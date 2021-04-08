import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.os.Handler
import android.os.Message
import android.util.Log
import androidx.fragment.app.FragmentManager
import com.vjezba.bluebtoothtest.ChatActivity
import com.vjezba.bluebtoothtest.DisableUserActionsDialog
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.Charset
import java.util.*


class BluetoothConnectionService(context: Context, val handler: Handler, val supportFragmentManager: FragmentManager, mChatActivity: ChatActivity) {

    val STATE_CONNECTING = 2
    val STATE_CONNECTED = 3
    val STATE_CONNECTION_FAILED = 4
    val STATE_MESSAGE_RECEIVED = 5

    private val mBluetoothAdapter: BluetoothAdapter
    var mContext: Context
    private var mInsecureAcceptThread: AcceptThread? = null
    private var mConnectThread: ConnectThread? = null
    private var mmDevice: BluetoothDevice? = null
    private var deviceUUID: UUID? = null
    var disableUserActionDialog: DisableUserActionsDialog? = null
    private var mConnectedThread: ConnectedThread? = null

    var inputString: String = ""
    var chatActivity: ChatActivity? = null


    companion object {
        private const val TAG = "BluetoothConnectionServ"
        private const val appName = "MYAPP"
        private val MY_UUID_INSECURE: UUID = UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66")
    }

    init {
        mContext = context
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        chatActivity = mChatActivity
        start()
    }

    private var currentState = 0

    @Synchronized
    fun setState(state: Int) {
        val message = Message.obtain()
        message.what = state
        handler.sendMessage(message)
        currentState = state
    }

    /**
     * This thread runs while listening for incoming connections. It behaves
     * like a server-side client. It runs until a connection is accepted
     * (or until cancelled).
     */
    private inner class AcceptThread : Thread() {
        // The local server socket
        private val mmServerSocket: BluetoothServerSocket?
        override fun run() {

            Log.d(TAG, "run: AcceptThread Running.")
            var socket: BluetoothSocket? = null
            while (socket == null) {
                try {

                    setState(STATE_CONNECTING)

                    // This is a blocking call and will only return on a
                    // successful connection or an exception
                    Log.d(
                            TAG,
                            "run: RFCOM server socket start....."
                    )
                    socket = mmServerSocket!!.accept()
                    Log.d(
                            TAG,
                            "run: RFCOM server socket accepted connection."
                    )

                } catch (e: IOException) {
                    Log.e(
                            TAG,
                            "AcceptThread: IOException: " + e.message
                    )
                    mmServerSocket!!.close()
                    setState(STATE_CONNECTION_FAILED)
                }

                if( socket != null ) {

                        when (currentState) {
                            STATE_CONNECTING -> connected(socket, mmDevice)
                            STATE_CONNECTED -> try {
                                mmServerSocket?.close()
                            } catch (e: IOException) {
                                Log.e("Accept->CloseSocket", e.toString())
                            }
                        }

//                    val message = Message.obtain()
//                    message.what = STATE_CONNECTED
//                    handler.sendMessage(message)

                    //talk about this is in the 3rd

                    //connected(socket, mmDevice)
                    //mmServerSocket?.close()
                    Log.i(TAG, "END mAcceptThread ")
                    break
                }

            }
        }

        fun cancel() {
            Log.d(TAG, "cancel: Canceling AcceptThread.")
            try {
                mmServerSocket!!.close()
                setState(STATE_CONNECTION_FAILED)
            } catch (e: IOException) {
                Log.e(
                        TAG,
                        "cancel: Close of AcceptThread ServerSocket failed. " + e.message
                )
            }
        }

        init {
            var tmp: BluetoothServerSocket? = null

            // Create a new listening server socket
            try {
                tmp = mBluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(
                        appName,
                        MY_UUID_INSECURE
                )
                Log.d(
                        TAG,
                        "AcceptThread: Setting up Server using: $MY_UUID_INSECURE"
                )
            } catch (e: IOException) {
                Log.e(
                        TAG,
                        "AcceptThread: IOException: " + e.message
                )
            }
            mmServerSocket = tmp
        }
    }

    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    // The local client socket
    private inner class ConnectThread(device: BluetoothDevice?, uuid: UUID?) : Thread() {
        private var mmSocket: BluetoothSocket? = null
        override fun run() {
            var tmp: BluetoothSocket? = null
            Log.i(TAG, "RUN mConnectThread ")

            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            try {
                Log.d(
                        TAG,
                        "ConnectThread: Trying to create InsecureRfcommSocket using UUID: "
                                + MY_UUID_INSECURE
                )
                tmp = mmDevice!!.createRfcommSocketToServiceRecord(deviceUUID)
            } catch (e: IOException) {
                Log.e(
                        TAG,
                        "ConnectThread: Could not create InsecureRfcommSocket " + e.message
                )
            }
            mmSocket = tmp

            // Always cancel discovery because it will slow down a connection
            mBluetoothAdapter.cancelDiscovery()

            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                mmSocket!!.connect()
                val message = Message.obtain()
                message.what = STATE_CONNECTED
                handler.sendMessage(message)

                Log.d(TAG, "run: ConnectThread connected.")
            } catch (e: IOException) {
                // Close the socket
                try {
                    mmSocket!!.close()
                    setState(STATE_CONNECTION_FAILED)
                    Log.d(TAG, "run: Closed Socket.")
                } catch (e1: IOException) {
                    Log.e(
                            TAG,
                            "mConnectThread: run: Unable to close connection in socket " + e1.message
                    )
                }
                Log.d(
                        TAG,
                        "run: ConnectThread: Could not connect to UUID: $MY_UUID_INSECURE"
                )
            }

            //will talk about this in the 3rd video
            connected(mmSocket, mmDevice)
        }

        fun cancel() {
            try {
                Log.d(TAG, "cancel: Closing Client Socket.")
                mmSocket!!.close()
                setState(STATE_CONNECTION_FAILED)
            } catch (e: IOException) {
                Log.e(
                        TAG,
                        "cancel: close() of mmSocket in Connectthread failed. " + e.message
                )
            }
        }

        init {
            Log.d(TAG, "ConnectThread: started.")
            mmDevice = device
            deviceUUID = uuid
        }
    }

    /**
     * Start the chat service. Specifically start AcceptThread to begin a
     * session in listening (server) mode. Called by the Activity onResume()
     */
    @Synchronized
    fun start() {
        Log.d(TAG, "start")

        // Cancel any thread attempting to make a connection
        if (mConnectThread != null) {
            mConnectThread!!.cancel()
            mConnectThread = null
        }
        if (mInsecureAcceptThread == null) {
            mInsecureAcceptThread = AcceptThread()
            mInsecureAcceptThread!!.start()
        }
    }

    /**
     * AcceptThread starts and sits waiting for a connection.
     * Then ConnectThread starts and attempts to make a connection with the other devices AcceptThread.
     */
    fun startClient(device: BluetoothDevice?, uuid: UUID?, mDisableUserActionDialog: DisableUserActionsDialog?) {
        Log.d(TAG, "startClient: Started.")

        disableUserActionDialog = mDisableUserActionDialog
        disableUserActionDialog?.show(supportFragmentManager, "")
        //initprogress dialog
//        mProgressDialog =  ProgressDialog.show(
//            mContext, "Connecting Bluetooth"
//            , "Please Wait...", true
//        )
        mConnectThread = ConnectThread(device, uuid)
        mConnectThread!!.start()
    }

    /**
     * Finally the ConnectedThread which is responsible for maintaining the BTConnection, Sending the data, and
     * receiving incoming data through input/output streams respectively.
     */
    private inner class ConnectedThread(socket: BluetoothSocket?) : Thread() {
        private val mmSocket: BluetoothSocket?
        private val mmInStream: InputStream?
        private val mmOutStream: OutputStream?
        override fun run() {
            val buffer = ByteArray(1024) // buffer store for the stream
            var bytes = 0 // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs
            while (true) {
                // Read from the InputStream
                try {

                    bytes = mmInStream?.read(buffer) ?: 0

                    val incomingMessage = String(buffer, 0, bytes)
                    inputString = incomingMessage

                    //chatActivity?.displayText(inputString)

                    handler.obtainMessage(STATE_MESSAGE_RECEIVED, bytes, -1, buffer).sendToTarget()
                    inputString = ""
                    Log.d(
                            TAG,
                            "InputStream: $incomingMessage"
                    )
                } catch (e: IOException) {
                    Log.e(
                            TAG,
                            "write: Error reading Input Stream. " + e.message
                    )
                    break
                }
            }
        }

        //Call this from the main activity to send data to the remote device
        fun write(bytes: ByteArray?) {
            val text = String(bytes!!, Charset.defaultCharset())
            Log.d(
                    TAG,
                    "write: Writing to outputstream: $text"
            )
            try {
                mmOutStream?.write(bytes)

                chatActivity?.displayOnMyPhoneMessage()
                //( activity as ChatActivity ).displayOnMyPhoneMessage()
                Log.d(
                        TAG,
                        "da li ce uci sim, pokrenuti updajte adaptera: "
                )
            } catch (e: IOException) {
                Log.e(
                        TAG,
                        "write: Error writing to output stream. " + e.message
                )
            }
        }

        /* Call this from the main activity to shutdown the connection */
        fun cancel() {
            try {
                mmSocket!!.close()
            } catch (e: IOException) {
            }
        }

        init {
            Log.d(TAG, "ConnectedThread: Starting.")
            mmSocket = socket
            var tmpIn: InputStream? = null
            var tmpOut: OutputStream? = null

            //dismiss the progressdialog when connection is established
            try {
                chatActivity?.hideProgressDialog(disableUserActionDialog)
                //chatActivity?.hideProgressDialog(progressDialog)
                //progressDialog!!.dismiss()
            } catch (e: NullPointerException) {
                e.printStackTrace()
            }
            try {
                tmpIn = mmSocket!!.inputStream
                tmpOut = mmSocket.outputStream
            } catch (e: IOException) {
                e.printStackTrace()
            }
            mmInStream = tmpIn
            mmOutStream = tmpOut
        }
    }

    private fun connected(mmSocket: BluetoothSocket?, mmDevice: BluetoothDevice?) {
        Log.d(TAG, "connected: Starting.")

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = ConnectedThread(mmSocket)
        mConnectedThread!!.start()

        setState(STATE_CONNECTED)
    }

    /**
     * Write to the ConnectedThread in an unsynchronized manner
     *
     * @param out The bytes to write
     * @see ConnectedThread.write
     */
    fun write(out: ByteArray?) {
        // Create temporary object
        var r: ConnectedThread

        // Synchronize a copy of the ConnectedThread
        Log.d(TAG, "write: Write Called.")
        //perform the write
        mConnectedThread!!.write(out)
    }

}