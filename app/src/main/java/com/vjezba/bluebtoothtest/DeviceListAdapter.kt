package com.vjezba.bluebtoothtest

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.core.content.ContextCompat


class DeviceListAdapter(
    context: Context,
    tvResourceId: Int,
    private val mDevices: MutableList<BluetoothDevice?>,
    private var selectedBLuebtoothDevice: Int
) :
    ArrayAdapter<BluetoothDevice?>(context, tvResourceId, mDevices) {
    private val mLayoutInflater: LayoutInflater
    private val mViewResourceId: Int

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var convertView: View? = convertView
        convertView = mLayoutInflater.inflate(mViewResourceId, null)
        val device = mDevices[position]
        if (device != null) {

            val deviceName = convertView.findViewById(R.id.tvDeviceName) as TextView
            val deviceAdress =
                convertView.findViewById(R.id.tvDeviceAddress) as TextView

            if( position == selectedBLuebtoothDevice ) {
                convertView.setBackgroundColor(ContextCompat.getColor(context, android.R.color.holo_red_dark))
                deviceName.setTextColor(ContextCompat.getColor(context, android.R.color.white))
                deviceAdress.setTextColor(ContextCompat.getColor(context, android.R.color.white))
            }
            else {
                convertView.setBackgroundColor(ContextCompat.getColor(context, android.R.color.white))
                deviceName.setTextColor(ContextCompat.getColor(context, android.R.color.black))
                deviceAdress.setTextColor(ContextCompat.getColor(context, android.R.color.black))
            }

            if (deviceName != null) {
                deviceName.text = if( device.name != "" && device.name != null )  device.name else "Nepoznato ime bluebtooth uredaja"
            }
            if (deviceAdress != null) {
                deviceAdress.text = device.address
            }
        }
        return convertView
    }

    fun updatePositionInDeviceListAdapter(position: Int) {
        selectedBLuebtoothDevice = position
        notifyDataSetChanged()
    }

    init {
        mLayoutInflater =
            context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        mViewResourceId = tvResourceId
    }
}