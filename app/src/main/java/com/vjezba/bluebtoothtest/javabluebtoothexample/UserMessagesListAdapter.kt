package com.vjezba.bluebtoothtest.javabluebtoothexample

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.vjezba.bluebtoothtest.R

class UserMessagesListAdapter(var listMessages: MutableList<SenderReceiverBLEDevice>) : RecyclerView.Adapter<UserMessagesListAdapter.UserMessageHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserMessageHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.java_bluebtooth_chat_messages_lits, parent, false)
        return UserMessageHolder(view)
    }

    override fun onBindViewHolder(holder: UserMessageHolder, position: Int) {
        holder.display(listMessages[position])
    }

    override fun getItemCount() = listMessages.size

    inner class UserMessageHolder(item: View) : RecyclerView.ViewHolder(item) {
        private val messageFromMe = item.findViewById<TextView>(R.id.messageFromMe)
        private val messageFromFriend = item.findViewById<TextView>(R.id.messageFromFriend)

        fun display(userMessage: SenderReceiverBLEDevice) {
            if( userMessage.receiverDevice ) {
                messageFromFriend.visibility = View.VISIBLE
                messageFromMe.visibility = View.GONE
                messageFromFriend.text = userMessage.chatMessage
            }
            else {
                messageFromFriend.visibility = View.GONE
                messageFromMe.visibility = View.VISIBLE
                messageFromMe.text = userMessage.chatMessage
            }
        }
    }

    fun updateUserMessages(newUserMessage: SenderReceiverBLEDevice) {
        listMessages.add(newUserMessage)
        notifyDataSetChanged()
    }
}