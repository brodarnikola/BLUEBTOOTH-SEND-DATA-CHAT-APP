package com.vjezba.bluebtoothtest.ui.activities

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.vjezba.bluebtoothtest.R
import kotlinx.android.synthetic.main.activity_device_list.*

class DeviceListActivity : AppCompatActivity() {


    private var adapterPairedDevices: ArrayAdapter<String>? = null
    private var adapterAvailableDevices:ArrayAdapter<String>? = null
    private var mContext: Context? = null
    private var bluetoothAdapter: BluetoothAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_list)

        mContext = this

        init()
    }

    private fun init() {
        adapterPairedDevices = ArrayAdapter(mContext!!, R.layout.device_list_item)
        adapterAvailableDevices = ArrayAdapter(mContext!!, R.layout.device_list_item)
        list_paired_devices.setAdapter(adapterPairedDevices)
        list_available_devices.setAdapter(adapterAvailableDevices)
        list_available_devices.setOnItemClickListener({ adapterView, view, i, l ->
            val info = (view as TextView).text.toString()
            val address = info.substring(info.length - 17)
            val intent = Intent()
            intent.putExtra("deviceAddress", address)
            setResult(RESULT_OK, intent)
            finish()
        })
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        val pairedDevices =
            bluetoothAdapter!!.getBondedDevices()
        if (pairedDevices != null && pairedDevices.size > 0) {
            for (device in pairedDevices) {
                adapterPairedDevices!!.add(
                    """
                        ${device.name}
                        ${device.address}
                        """.trimIndent()
                )
            }
        }
        val intentFilter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        registerReceiver(bluetoothDeviceListener, intentFilter)
        val intentFilter1 = IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        registerReceiver(bluetoothDeviceListener, intentFilter1)
        list_paired_devices.setOnItemClickListener({ adapterView, view, i, l ->
            bluetoothAdapter!!.cancelDiscovery()
            val info = (view as TextView).text.toString()
            val address = info.substring(info.length - 17)
            Log.d("Address", address)
            val intent = Intent()
            intent.putExtra("deviceAddress", address)
            setResult(Activity.RESULT_OK, intent)
            finish()
        })
    }

    private val bluetoothDeviceListener: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (BluetoothDevice.ACTION_FOUND == action) {
                val device =
                    intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                if (device!!.bondState != BluetoothDevice.BOND_BONDED) {
                    adapterAvailableDevices!!.add(
                        """
                            ${device!!.name}
                            ${device!!.address}
                            """.trimIndent()
                    )
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED == action) {
                progress_scan_devices!!.visibility = View.GONE
                if (adapterAvailableDevices!!.count == 0) {
                    Toast.makeText(context, "No new devices found", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(
                        context,
                        "Click on the device to start the chat",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_device_list, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_scan_devices -> {
                scanDevices()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun scanDevices() {
        progress_scan_devices!!.visibility = View.VISIBLE
        adapterAvailableDevices!!.clear()
        Toast.makeText(mContext, "Scan started", Toast.LENGTH_SHORT).show()
        if (bluetoothAdapter!!.isDiscovering) {
            bluetoothAdapter!!.cancelDiscovery()
        }
        bluetoothAdapter!!.startDiscovery()
    }

    override fun onDestroy() {
        super.onDestroy()
        bluetoothDeviceListener?.let { unregisterReceiver(it) }
    }





}