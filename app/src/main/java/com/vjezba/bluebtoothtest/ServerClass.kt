/*
package com.vjezba.bluebtoothtest

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.os.Handler
import android.os.Message
import java.io.IOException

class ServerClass constructor(handler: Handler, bluetoothAdapter: BluetoothAdapter, var sendReceive: ChatActivity.SendReceive) : Thread() {
    private var serverSocket: BluetoothServerSocket? = null

    private var handlerTest: Handler? = null

    override fun run() {
        var socket: BluetoothSocket? = null
        while (socket == null) {
            try {

                val message = Message.obtain()
                message.what = ChatsActivity.STATE_CONNECTING
                handlerTest?.sendMessage(message)
                socket = serverSocket!!.accept()
            } catch (e: IOException) {
                e.printStackTrace()
                val message = Message.obtain()
                message.what = ChatsActivity.STATE_CONNECTION_FAILED
                handlerTest?.sendMessage(message)
            }
            if (socket != null) {
                val message = Message.obtain()
                message.what = ChatsActivity.STATE_CONNECTED
                handlerTest?.sendMessage(message)
                sendReceive = ChatActivity.SendReceive(socket)
                sendReceive.start()
                break
            }
        }
    }

    init {
        try {
            handlerTest = handler
            serverSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord(ChatsActivity.APP_NAME, ChatsActivity.MY_UUID)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}*/
