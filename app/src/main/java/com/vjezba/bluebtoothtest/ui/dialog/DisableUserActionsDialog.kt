package com.vjezba.bluebtoothtest.ui.dialog

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.fragment.app.DialogFragment
import com.vjezba.bluebtoothtest.R
import kotlinx.android.synthetic.main.dialog_disable_user_actions.*

class DisableUserActionsDialog : DialogFragment() {


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val view = inflater.inflate(R.layout.dialog_disable_user_actions, container, false)
        if (dialog != null && dialog?.window != null) {
            dialog?.window?.setBackgroundDrawable( ColorDrawable(Color.TRANSPARENT))
            dialog?.window?.requestFeature(Window.FEATURE_NO_TITLE)
            dialog?.setCanceledOnTouchOutside(false)
        }
        return view
    }

    override fun onStart() {
        super.onStart()

        btnCancel.setOnClickListener {
            dismiss()
        }
    }

}