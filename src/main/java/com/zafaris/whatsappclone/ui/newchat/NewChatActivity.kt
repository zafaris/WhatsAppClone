package com.zafaris.whatsappclone.ui.newchat

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import com.zafaris.whatsappclone.R
import com.zafaris.whatsappclone.model.Chat
import com.zafaris.whatsappclone.model.User
import com.zafaris.whatsappclone.ui.chat.ChatActivity
import com.zafaris.whatsappclone.ui.home.HomeActivity
import kotlinx.android.synthetic.main.activity_home.*
import kotlinx.android.synthetic.main.activity_new_chat.*

class NewChatActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var databaseReference: DatabaseReference
    private lateinit var prefs: SharedPreferences
    private var name = ""

    private lateinit var selectUsersAdapter: SelectUsersAdapter
    private val userIdsList: MutableList<String> = ArrayList()
    private val usersList: MutableList<User> = ArrayList()
    private val usersSelectedList: MutableList<Int> = ArrayList()

        override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_chat)
        title = "Create new chat"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        auth = FirebaseAuth.getInstance()
        databaseReference = Firebase.database.reference

        prefs = getSharedPreferences("com.zafaris.whatsappclone", Context.MODE_PRIVATE)
        name = prefs.getString("name", "")!!

        setupRv()

        getUsers()

        button_new_chat.setOnClickListener {
            if (usersSelectedList.size == 1) {
                Toast.makeText(this, "Creating new chat with ${usersList[usersSelectedList[0]]}", Toast.LENGTH_SHORT).show()

                userIdsList.add(auth.uid!!)
                val chat = Chat()
                createNewChat(chat)
            } else if (usersSelectedList.size > 1) {
                if (edittext_new_chat_name.text.isNotEmpty()) {
                    val chatName = edittext_new_chat_name.text.toString()
                    Toast.makeText(this, "Creating new chat: $chatName", Toast.LENGTH_SHORT).show()

                    userIdsList.add(auth.uid!!)
                    val chat = Chat(name = chatName)
                    createNewChat(chat)
                } else {
                    Toast.makeText(this, "Please enter a chat name", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Select at least one user", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getUsers() {
        val userReference = databaseReference.child("users")
        userReference.addListenerForSingleValueEvent(object : ValueEventListener {

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (userSnapshot in dataSnapshot.children) {
                    val userId = userSnapshot.key!!
                    if (userId != auth.uid!!) {
                        userIdsList.add(userId)

                        val user = userSnapshot.getValue<User>()!!
                        usersList.add(user)
                    }
                }
                selectUsersAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(this@NewChatActivity, "Error getting users from server...", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun createNewChat(chat: Chat) {
        val chatId = databaseReference.push().key

        val userNames: HashMap<String, Boolean> = HashMap()
        val updatesMap: HashMap<String, Any> = HashMap()

        userNames[name] = true
        updatesMap["users/${auth.uid!!}/chats/$chatId"] = true

        for (index in usersSelectedList) {
            userNames[usersList[index].name] = true
            val userId = userIdsList[index]
            updatesMap["users/$userId/chats/$chatId"] = true
        }
        chat.userNames = userNames

        updatesMap["chats/$chatId"] = chat
        databaseReference.updateChildren(updatesMap)

        val intent = Intent(this, ChatActivity::class.java)
        intent.putExtra("chatId", chatId)
        startActivity(intent)
    }

    private fun setupRv() {
        selectUsersAdapter = SelectUsersAdapter(usersList, usersSelectedList)
        selectUsersAdapter.setOnCheckedChangedListener { position, isChecked -> checkBoxOnCheckedChange(position, isChecked) }
        recyclerview_select_users.adapter = selectUsersAdapter

        val divider = DividerItemDecoration(applicationContext, LinearLayoutManager.VERTICAL)
        recyclerview_select_users.addItemDecoration(divider)
        recyclerview_select_users.layoutManager = LinearLayoutManager(this)
    }

    private fun checkBoxOnCheckedChange(position: Int, isChecked: Boolean) {
        if (isChecked) {
            usersSelectedList.add(position)
        } else {
            usersSelectedList.removeAt(usersSelectedList.indexOf(position))
        }
        if (usersSelectedList.size > 1) {
            edittext_new_chat_name.visibility = View.VISIBLE
        } else {
            edittext_new_chat_name.visibility = View.GONE
        }
    }
}
