package com.vjezba.bluebtoothtest.ui.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.vjezba.bluebtoothtest.R
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onStart() {
        super.onStart()

        val divideDecimal = 5 / 2 .toDouble()
        Log.d("Divide value", "Divided value is: ${divideDecimal}")

        btnFirstExample.setOnClickListener {
            val intent = Intent(this, FirstChatActivity::class.java)
            startActivity(intent)
        }

        btnSecondExample.setOnClickListener {
            val intent = Intent(this, SecondChatActivity::class.java)
            startActivity(intent)
        }
    }


}