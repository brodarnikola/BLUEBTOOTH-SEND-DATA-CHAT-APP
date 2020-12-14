package com.vjezba.bluebtoothtest

import BluetoothConnectionService
import android.app.AlertDialog
import android.app.ProgressDialog
import android.app.ProgressDialog.show
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.EditText
import android.widget.Toast
import androidx.annotation.MainThread
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.android.synthetic.main.activity_chat2.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.nio.charset.Charset
import java.util.*
import kotlin.collections.ArrayList


class ChatActivity : AppCompatActivity(), AdapterView.OnItemClickListener {

    private val TAG = "MainActivity"

    var mBluetoothAdapter: BluetoothAdapter? = null

    var mBluetoothConnection: BluetoothConnectionService? = null

    private val MY_UUID_INSECURE = UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66")

    var mBTDevice: BluetoothDevice? = null

    var mBTDevices: MutableList<BluetoothDevice?> = mutableListOf()

    var mDeviceListAdapter: DeviceListAdapter? = null

    // Create a BroadcastReceiver for ACTION_FOUND
    private val mBroadcastReceiver1: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            // When discovery finds a device
            if (action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                val state =
                    intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                when (state) {
                    BluetoothAdapter.STATE_OFF -> Log.d(
                        TAG,
                        "onReceive: STATE OFF"
                    )
                    BluetoothAdapter.STATE_TURNING_OFF -> Log.d(
                        TAG,
                        "mBroadcastReceiver1: STATE TURNING OFF"
                    )
                    BluetoothAdapter.STATE_ON -> Log.d(
                        TAG,
                        "mBroadcastReceiver1: STATE ON"
                    )
                    BluetoothAdapter.STATE_TURNING_ON -> Log.d(
                        TAG,
                        "mBroadcastReceiver1: STATE TURNING ON"
                    )
                }
            }
        }
    }

    /**
     * Broadcast Receiver for changes made to bluetooth states such as:
     * 1) Discoverability mode on/off or expire.
     */
    private val mBroadcastReceiver2: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (action == BluetoothAdapter.ACTION_SCAN_MODE_CHANGED) {
                val mode =
                    intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, BluetoothAdapter.ERROR)
                when (mode) {
                    BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE -> Log.d(
                        TAG,
                        "mBroadcastReceiver2: Discoverability Enabled."
                    )
                    BluetoothAdapter.SCAN_MODE_CONNECTABLE -> Log.d(
                        TAG,
                        "mBroadcastReceiver2: Discoverability Disabled. Able to receive connections."
                    )
                    BluetoothAdapter.SCAN_MODE_NONE -> Log.d(
                        TAG,
                        "mBroadcastReceiver2: Discoverability Disabled. Not able to receive connections."
                    )
                    BluetoothAdapter.STATE_CONNECTING -> Log.d(
                        TAG,
                        "mBroadcastReceiver2: Connecting...."
                    )
                    BluetoothAdapter.STATE_CONNECTED -> Log.d(
                        TAG,
                        "mBroadcastReceiver2: Connected."
                    )
                }
            }
        }
    }


    /**
     * Broadcast Receiver for listing devices that are not yet paired
     * -Executed by btnDiscover() method.
     */
    private val mBroadcastReceiver3: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            Log.d(TAG, "onReceive: ACTION FOUND.")
            if (action == BluetoothDevice.ACTION_FOUND) {
                val device =
                    intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                mBTDevices.add(device)
                Log.d(
                    TAG,
                    "onReceive: " + device!!.name + ": " + device.address
                )
                mDeviceListAdapter =
                    DeviceListAdapter(context, R.layout.device_adapter_view, mBTDevices, 0)
                lvNewDevices?.setAdapter(mDeviceListAdapter)
            }
        }
    }

    /**
     * Broadcast Receiver that detects bond state changes (Pairing status changes)
     */
    private val mBroadcastReceiver4: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (action == BluetoothDevice.ACTION_BOND_STATE_CHANGED) {
                val mDevice =
                    intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                //3 cases:
                //case1: bonded already
                if (mDevice!!.bondState == BluetoothDevice.BOND_BONDED) {
                    Log.d(TAG, "BroadcastReceiver: BOND_BONDED.")
                    //inside BroadcastReceiver4
                    mBTDevice = mDevice
                }
                //case2: creating a bone
                if (mDevice.bondState == BluetoothDevice.BOND_BONDING) {
                    Log.d(TAG, "BroadcastReceiver: BOND_BONDING.")
                }
                //case3: breaking a bond
                if (mDevice.bondState == BluetoothDevice.BOND_NONE) {
                    Log.d(TAG, "BroadcastReceiver: BOND_NONE.")
                }
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat2)

        mBTDevices = ArrayList()

        //Broadcasts when bond state changes (ie:pairing)

        //Broadcasts when bond state changes (ie:pairing)
        val filter = IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        registerReceiver(mBroadcastReceiver4, filter)

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        lvNewDevices!!.onItemClickListener = this

        btnONOFF.setOnClickListener {
            Log.d(TAG, "onClick: enabling/disabling bluetooth.")
            enableDisableBT()
        }

        btnStartConnection!!.setOnClickListener {
            startConnection()
        }

        btnSend!!.setOnClickListener {
            val bytes: ByteArray = editText!!.text.toString().toByteArray(Charset.defaultCharset()) // .getBytes(Charset.defaultCharset()); //.byteInputStream(Charset.defaultCharset())
            mBluetoothConnection!!.write(bytes)
        }

    }

    val listOFChat: MutableList<String> = mutableListOf()
    var chatListAdapter: ChatListAdapter? = null
    fun displayText(inputString: String) {

        lifecycleScope.launch {
            listOFChat.add(inputString)

            Log.d(
                TAG,
                "onReceive: " + inputString
            )
            chatListAdapter =
                ChatListAdapter(this@ChatActivity.baseContext, R.layout.chat_list_view, listOFChat)
            lvNewChat?.setAdapter(chatListAdapter)
        }
    }

    fun hideProgressDialog(disableUserActionDialog: DisableUserActionsDialog?) {
        disableUserActionDialog?.dismiss()
    }

    //create method for starting connection
    //***remember the conncction will fail and app will crash if you haven't paired first
    fun startConnection() {
        startBTConnection(mBTDevice, MY_UUID_INSECURE)
    }


    var disableUserActionDialog: DisableUserActionsDialog? = null
    /**
     * starting chat service method
     */
    fun startBTConnection(device: BluetoothDevice?, uuid: UUID?) {
        Log.d(TAG, "startBTConnection: Initializing RFCOM Bluetooth Connection.")

        disableUserActionDialog = DisableUserActionsDialog()
        disableUserActionDialog?.isCancelable = false
        disableUserActionDialog?.show( this.supportFragmentManager, ""
        )

        mBluetoothConnection!!.startClient(device, uuid, this, disableUserActionDialog)
    }


    fun enableDisableBT() {
        if (mBluetoothAdapter == null) {
            Log.d(TAG, "enableDisableBT: Does not have BT capabilities.")
        }
        if (!mBluetoothAdapter!!.isEnabled) {
            Log.d(TAG, "enableDisableBT: enabling BT.")
            val enableBTIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivity(enableBTIntent)
            val BTIntent = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
            registerReceiver(mBroadcastReceiver1, BTIntent)
        }
        else if (mBluetoothAdapter!!.isEnabled) {
            Log.d(TAG, "enableDisableBT: disabling BT.")
            mBluetoothAdapter!!.disable()
            val BTIntent = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
            registerReceiver(mBroadcastReceiver1, BTIntent)
        }
    }


    fun btnEnableDisable_Discoverable(view: View?) {
        Log.d(
            TAG,
            "btnEnableDisable_Discoverable: Making device discoverable for 300 seconds."
        )
        val discoverableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
        startActivity(discoverableIntent)
        val intentFilter = IntentFilter(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED)
        registerReceiver(mBroadcastReceiver2, intentFilter)
    }

    fun btnDiscover(view: View?) {
        Log.d(TAG, "btnDiscover: Looking for unpaired devices.")
        if (mBluetoothAdapter!!.isDiscovering) {
            mBluetoothAdapter!!.cancelDiscovery()
            Log.d(TAG, "btnDiscover: Canceling discovery.")

            //check BT permissions in manifest
            checkBTPermissions()
            mBluetoothAdapter!!.startDiscovery()
            val discoverDevicesIntent = IntentFilter(BluetoothDevice.ACTION_FOUND)
            registerReceiver(mBroadcastReceiver3, discoverDevicesIntent)
        }
        if (!mBluetoothAdapter!!.isDiscovering) {

            //check BT permissions in manifest
            checkBTPermissions()
            mBluetoothAdapter!!.startDiscovery()
            val discoverDevicesIntent = IntentFilter(BluetoothDevice.ACTION_FOUND)
            registerReceiver(mBroadcastReceiver3, discoverDevicesIntent)
        }
    }

    /**
     * This method is required for all devices running API23+
     * Android must programmatically check the permissions for bluetooth. Putting the proper permissions
     * in the manifest is not enough.
     *
     * NOTE: This will only execute on versions > LOLLIPOP because it is not needed otherwise.
     */
    private fun checkBTPermissions() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            var permissionCheck =
                checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION")
            permissionCheck += checkSelfPermission("Manifest.permission.ACCESS_COARSE_LOCATION")
            if (permissionCheck != 0) {
                requestPermissions(
                    arrayOf(
                        android.Manifest.permission.ACCESS_FINE_LOCATION,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION
                    ), 1001
                ) //Any number
            }
        } else {
            Log.d(
                TAG,
                "checkBTPermissions: No need to check permissions. SDK version < LOLLIPOP."
            )
        }
    }

    override fun onItemClick(
        adapterView: AdapterView<*>?,
        view: View?,
        i: Int,
        l: Long
    ) {
        //first cancel discovery because its very memory intensive.
        mBluetoothAdapter!!.cancelDiscovery()
        Log.d(TAG, "onItemClick: You Clicked on a device.")
        val deviceName = mBTDevices[i]!!.name
        val deviceAddress = mBTDevices[i]!!.address
        Log.d(TAG, "onItemClick: deviceName = $deviceName")
        Log.d(TAG, "onItemClick: deviceAddress = $deviceAddress")

        //create the bond.
        //NOTE: Requires API 17+? I think this is JellyBean
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2) {
            Log.d(TAG, "Trying to pair with $deviceName")
            mBTDevices[i]!!.createBond()
            mBTDevice = mBTDevices[i]
            mBluetoothConnection = BluetoothConnectionService(this@ChatActivity.baseContext)
        }
        mDeviceListAdapter?.updatePositionInDeviceListAdapter(i)
    }


    override fun onDestroy() {
        if( mBroadcastReceiver1.isOrderedBroadcast )
        unregisterReceiver(mBroadcastReceiver1)
        if( mBroadcastReceiver2.isOrderedBroadcast )
        unregisterReceiver(mBroadcastReceiver2)
        if( mBroadcastReceiver3.isOrderedBroadcast )
        unregisterReceiver(mBroadcastReceiver3)
        if( mBroadcastReceiver4.isOrderedBroadcast )
        unregisterReceiver(mBroadcastReceiver4)
        Log.d(TAG, "onDestroy: called.")
        super.onDestroy()
    }

}

