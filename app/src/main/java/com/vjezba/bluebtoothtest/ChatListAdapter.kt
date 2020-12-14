package com.vjezba.bluebtoothtest

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView


class ChatListAdapter(
    context: Context,
    tvResourceId: Int,
    private val mDevices: MutableList<String>
) :
    ArrayAdapter<String>(context, tvResourceId, mDevices) {
    private val mLayoutInflater: LayoutInflater
    private val mViewResourceId: Int

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var convertView: View? = convertView
        convertView = mLayoutInflater.inflate(mViewResourceId, null)
        val device = mDevices[position]
        if (device != null) {
            val deviceName = convertView.findViewById(R.id.tvChat) as TextView
            if (deviceName != null) {
                deviceName.text = device
            }
        }
        return convertView
    }

    init {
        mLayoutInflater =
            context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        mViewResourceId = tvResourceId
    }
}