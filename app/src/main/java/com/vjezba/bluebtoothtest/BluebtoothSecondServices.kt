package com.vjezba.bluebtoothtest

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.util.Log
import com.vjezba.bluebtoothtest.ui.activities.SecondChatActivity
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID


class BluebtoothSecondServices(mContext: Context, mHandler: Handler) {

    private var context: Context? = null
    private var handler: Handler? = null
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var connectThread: ConnectThread? = null
    private var acceptThread: AcceptThread? = null
    private var connectedThread: ConnectedThread? = null

    private val APP_UUID =
        UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66")
    private val APP_NAME = "BluetoothChatApp59"

    companion object { 
        val STATE_NONE = 0
        val STATE_LISTEN = 1
        val STATE_CONNECTING = 2
        val STATE_CONNECTED = 3
    }

    private var currentState = 0
    
    init { 
        context = mContext
        handler = mHandler
        currentState = STATE_NONE
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    }
 
    fun getState(): Int {
        return currentState
    }

    @Synchronized
    fun setState(state: Int) {
        this.currentState = state
        handler!!.obtainMessage(SecondChatActivity.MESSAGE_STATE_CHANGED, state, -1).sendToTarget()
    }

    @Synchronized
    private fun start() {
        if (connectThread != null) {
            connectThread!!.cancel()
            connectThread = null
        }
        if (acceptThread == null) {
            acceptThread = AcceptThread()
            acceptThread!!.start()
        }
        if (connectedThread != null) {
            connectedThread!!.cancel()
            connectedThread = null
        }
        setState(STATE_LISTEN)
    }

    @Synchronized
    fun stop() {
        if (connectThread != null) {
            connectThread!!.cancel()
            connectThread = null
        }
        if (acceptThread != null) {
            acceptThread!!.cancel()
            acceptThread = null
        }
        if (connectedThread != null) {
            connectedThread!!.cancel()
            connectedThread = null
        }
        setState(STATE_NONE)
    }

    fun connect(device: BluetoothDevice) {
        if (currentState == STATE_CONNECTING) {
            connectThread!!.cancel()
            connectThread = null
        }
        connectThread = ConnectThread(device)
        connectThread!!.start()
        if (connectedThread != null) {
            connectedThread!!.cancel()
            connectedThread = null
        }
        setState(STATE_CONNECTING)
    }

    fun write(buffer: ByteArray?) {
        var connThread: ConnectedThread?
        synchronized(this) {
            if (currentState != STATE_CONNECTED) {
                return
            }
            connThread = connectedThread
        }
        connThread!!.write(buffer)
    }

   private inner class AcceptThread : Thread() {
        private val serverSocket: BluetoothServerSocket
        override fun run() {
            var socket: BluetoothSocket? = null
            try {
                socket = serverSocket.accept()
            } catch (e: IOException) {
                Log.e("Accept->Run", e.toString())
                try {
                    serverSocket.close()
                } catch (e1: IOException) {
                    Log.e("Accept->Close", e.toString())
                }
            } catch (e: Exception) {
                Log.e("Main Accept->Run", "Main accept run exception: $e")
            }
            if (socket != null) {
                when (currentState) {
                    // TODO: WHY DO I NEED TO HAVE HERE THIS STATE_LISTEN INSTEAD OF STATE_CONNECTING
                    STATE_LISTEN, STATE_CONNECTING -> connected(
                        socket,
                        socket.remoteDevice
                    )
                    STATE_CONNECTED -> try {
                        serverSocket.close()
                    } catch (e: IOException) {
                        Log.e("Accept->CloseSocket", e.toString())
                    }
                }
            }
        }

        fun cancel() {
            try {
                serverSocket.close()
            } catch (e: IOException) {
                Log.e("Accept->CloseServer", e.toString())
            }
        }

        init {
            var tmp: BluetoothServerSocket? = null
            try {
                tmp = bluetoothAdapter?.listenUsingRfcommWithServiceRecord(APP_NAME, APP_UUID)
            } catch (e: IOException) {
                Log.e("Accept->Constructor", e.toString())
            }
            serverSocket = tmp!!
        }
    }

    inner class ConnectThread(private val device: BluetoothDevice) : Thread() {
        private val socket: BluetoothSocket?
        override fun run() {
            try {
                socket!!.connect()
            } catch (e: IOException) {
                Log.e("Connect->Run", e.toString())
                try {
                    socket!!.close()
                } catch (e1: IOException) {
                    Log.e("Connect->CloseSocket", e.toString())
                }
                connectionFailed()
                return
            }
            synchronized(this@BluebtoothSecondServices) { connectThread = null }
            connected(socket, device)
        }

        fun cancel() {
            try {
                socket!!.close()
            } catch (e: IOException) {
                Log.e("Connect->Cancel", e.toString())
            }
        }

        init {
            var tmp: BluetoothSocket? = null
            try {
                tmp = device.createRfcommSocketToServiceRecord(APP_UUID)
            } catch (e: IOException) {
                Log.e("Connect->Constructor", e.toString())
            }
            socket = tmp

            // Always cancel discovery because it will slow down a connection
            bluetoothAdapter?.cancelDiscovery()
        }
    }

    private inner class ConnectedThread(private val socket: BluetoothSocket) : Thread() {
        private val inputStream: InputStream?
        private val outputStream: OutputStream?
        override fun run() {
            val buffer = ByteArray(1024)
            var bytes: Int
            while (true) {
                try {
                    bytes = inputStream!!.read(buffer)
                    handler?.obtainMessage(SecondChatActivity.MESSAGE_READ, bytes, -1, buffer)
                        ?.sendToTarget()
                } catch (e: IOException) {
                    break
                    //connectionLost();
                }
            }
        }

        fun write(buffer: ByteArray?) {
            try {
                outputStream!!.write(buffer)
                handler?.obtainMessage(SecondChatActivity.MESSAGE_WRITE, -1, -1, buffer)?.sendToTarget()
            } catch (e: IOException) {
            }
        }

        fun cancel() {
            try {
                socket.close()
            } catch (e: IOException) {
            }
        }

        init {
            var tmpIn: InputStream? = null
            var tmpOut: OutputStream? = null
            try {
                tmpIn = socket.inputStream
                tmpOut = socket.outputStream
            } catch (e: IOException) {
            }
            inputStream = tmpIn
            outputStream = tmpOut
        }
    }

    private fun connectionLost() {
        val message = handler!!.obtainMessage(SecondChatActivity.MESSAGE_TOAST)
        val bundle = Bundle()
        bundle.putString(SecondChatActivity.TOAST, "Connection Lost")
        message.data = bundle
        handler?.sendMessage(message)
        this@BluebtoothSecondServices.start()
    }

    @Synchronized
    private fun connectionFailed() {
        val message = handler!!.obtainMessage(SecondChatActivity.MESSAGE_TOAST)
        val bundle = Bundle()
        bundle.putString(SecondChatActivity.TOAST, "Cant connect to the device")
        message.data = bundle
        handler?.sendMessage(message)
        this@BluebtoothSecondServices.start()
    }

    @Synchronized
    private fun connected(socket: BluetoothSocket, device: BluetoothDevice) {
        if (connectThread != null) {
            connectThread!!.cancel()
            connectThread = null
        }
        if (connectedThread != null) {
            connectedThread!!.cancel()
            connectedThread = null
        }
        connectedThread = ConnectedThread(socket)
        connectedThread!!.start()
        val message = handler!!.obtainMessage(SecondChatActivity.MESSAGE_DEVICE_NAME)
        val bundle = Bundle()
        bundle.putString(SecondChatActivity.DEVICE_NAME, device.name)
        message.data = bundle
        handler?.sendMessage(message)
        setState(STATE_CONNECTED)
    }
}
