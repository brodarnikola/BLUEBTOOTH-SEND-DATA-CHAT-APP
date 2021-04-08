package com.vjezba.bluebtoothtest.ui.activities

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.vjezba.bluebtoothtest.BluebtoothSecondServices
import com.vjezba.bluebtoothtest.BluebtoothSecondServices.Companion.STATE_CONNECTED
import com.vjezba.bluebtoothtest.BluebtoothSecondServices.Companion.STATE_CONNECTING
import com.vjezba.bluebtoothtest.BluebtoothSecondServices.Companion.STATE_LISTEN
import com.vjezba.bluebtoothtest.BluebtoothSecondServices.Companion.STATE_NONE
import com.vjezba.bluebtoothtest.R
import kotlinx.android.synthetic.main.activity_first_chat.*
import kotlinx.android.synthetic.main.activity_second_chat.*

class SecondChatActivity : AppCompatActivity() {

    private var mContext: Context? = null
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var chatUtils: BluebtoothSecondServices? = null

    private var adapterMainChat: ArrayAdapter<String>? = null

    private val LOCATION_PERMISSION_REQUEST = 101
    private val SELECT_DEVICE = 102

    companion object {
        val MESSAGE_STATE_CHANGED = 0
        val MESSAGE_READ = 1
        val MESSAGE_WRITE = 2
        val MESSAGE_DEVICE_NAME = 3
        val MESSAGE_TOAST = 4
        val DEVICE_NAME = "deviceName"
        val TOAST = "toast"
    }

    private var connectedDevice: String? = null

    private val handler = object :  Handler(Looper.getMainLooper() ) {
        override fun handleMessage(message: Message) {
            when (message.what) {
                MESSAGE_STATE_CHANGED -> when (message.arg1) {
                    STATE_NONE -> setState("Not Connected")
                    STATE_LISTEN -> setState("Not Connected")
                    STATE_CONNECTING -> setState("Connecting...")
                    STATE_CONNECTED -> setState("Connected: $connectedDevice")
                }
                MESSAGE_WRITE -> {
                    val buffer1 = message.obj as ByteArray
                    val outputBuffer = String(buffer1)
                    adapterMainChat!!.add("Me: $outputBuffer")
                }
                MESSAGE_READ -> {
                    val buffer = message.obj as ByteArray
                    val inputBuffer = String(buffer, 0, message.arg1)
                    adapterMainChat!!.add("$connectedDevice: $inputBuffer")
                }
                MESSAGE_DEVICE_NAME -> {
                    connectedDevice = message.data.getString(DEVICE_NAME)
                    Toast.makeText(mContext, connectedDevice, Toast.LENGTH_SHORT).show()
                }
                MESSAGE_TOAST -> Toast.makeText(mContext, message.data.getString(TOAST), Toast.LENGTH_SHORT).show()
            }
            false
        }
    }
    
    private fun setState(subTitle: CharSequence) {
        supportActionBar!!.subtitle = subTitle
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_second_chat)

        mContext = this

        init()
        initBluetooth()
        chatUtils = BluebtoothSecondServices(mContext!!, handler)
        
    }

    private fun init() {
        adapterMainChat = ArrayAdapter(mContext!!, R.layout.message_layout)
        list_conversation?.setAdapter(adapterMainChat)
        btn_send_msg?.setOnClickListener({
            val message = ed_enter_message.getText().toString()
            if (!message.isEmpty()) {
                ed_enter_message.setText("")
                chatUtils!!.write(message.toByteArray())
            }
        })
    }

    private fun initBluetooth() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null) {
            Toast.makeText(mContext, "No bluetooth found", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main_activity, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_search_devices -> {
                checkPermissions()
                true
            }
            R.id.menu_enable_bluetooth -> {
                enableBluetooth()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(mContext!!, Manifest.permission.ACCESS_FINE_LOCATION) !== PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this@SecondChatActivity, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST)
        } else {
            val intent = Intent(mContext, DeviceListActivity::class.java)
            startActivityForResult(intent, SELECT_DEVICE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == SELECT_DEVICE && resultCode == RESULT_OK) {
            val address = data?.getStringExtra("deviceAddress")
            Log.d("Mac address", "Is this correct mac address: ${address}")
            chatUtils!!.connect(bluetoothAdapter!!.getRemoteDevice(address))
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                val intent = Intent(mContext, DeviceListActivity::class.java)
                startActivityForResult(intent, SELECT_DEVICE)
            } else {
                AlertDialog.Builder(mContext!!)
                        .setCancelable(false)
                        .setMessage("Location permission is required.\n Please grant")
                        .setPositiveButton("Grant", DialogInterface.OnClickListener { dialogInterface, i -> checkPermissions() })
                        .setNegativeButton("Deny", DialogInterface.OnClickListener { dialogInterface, i -> this@SecondChatActivity.finish() }).show()
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions!!, grantResults)
        }
    }

    private fun enableBluetooth() {
        if (!bluetoothAdapter!!.isEnabled) {
            bluetoothAdapter!!.enable()
        }
        if (bluetoothAdapter!!.scanMode != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            val discoveryIntent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
            discoveryIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
            startActivity(discoveryIntent)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (chatUtils != null) {
            chatUtils!!.stop()
        }
    }


}