package com.example.collegechatapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.Socket

class LoginActivity : AppCompatActivity() {

    // This companion object allows us to define constants that can be
    // accessed directly from the class name, like LoginActivity.NICKNAME_EXTRA
    companion object {
        const val NICKNAME_EXTRA = "com.example.collegechatapp.NICKNAME"
    }

    private lateinit var etServerIp: EditText
    private lateinit var etNickname: EditText
    private lateinit var btnConnect: Button
    private lateinit var progressBar: ProgressBar

    // Constants for Firebase and the chat server
    private val serverChatPort = 7070
    private val dbCollectionPath = "artifacts"
    private val dbAppId = "collegechatapp"
    private val dbServerDocId = "server_info"
    private val db = Firebase.firestore

    private var job: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        etServerIp = findViewById(R.id.etServerIp)
        etNickname = findViewById(R.id.etNickname)
        btnConnect = findViewById(R.id.btnConnect)
        progressBar = findViewById(R.id.progressBar)

        btnConnect.setOnClickListener {
            val serverIp = etServerIp.text.toString().trim()
            val nickname = etNickname.text.toString().trim()

            if (serverIp.isNotBlank() && nickname.isNotBlank()) {
                connectToServer(serverIp, nickname)
            } else {
                Toast.makeText(this, "Server IP and Nickname cannot be empty.", Toast.LENGTH_SHORT).show()
            }
        }

        // Start listening for the server IP from Firestore
        startIpDiscoveryFromFirestore()
    }

    private fun startIpDiscoveryFromFirestore() {
        Log.d("LoginActivity", "Starting Firestore IP listener...")

        val serverDocRef = db.collection(dbCollectionPath)
            .document(dbAppId)
            .collection("public")
            .document("data")
            .collection("server_info_collection")
            .document(dbServerDocId)

        serverDocRef.addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.e("LoginActivity", "Firestore listener failed.", e)
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                val ip = snapshot.getString("serverIp")
                if (ip != null && ip.isNotBlank()) {
                    Log.d("LoginActivity", "Received server IP from Firestore: $ip")
                    etServerIp.setText(ip)
                    Toast.makeText(this, "Server IP found!", Toast.LENGTH_SHORT).show()
                    progressBar.visibility = View.GONE
                }
            } else {
                Log.d("LoginActivity", "Server document does not exist yet. Still waiting...")
                etServerIp.setText(R.string.searching)
                progressBar.visibility = View.VISIBLE
            }
        }
    }

    private fun connectToServer(serverIp: String, nickname: String) {
        btnConnect.isEnabled = false
        progressBar.visibility = View.VISIBLE

        job = CoroutineScope(Dispatchers.IO).launch {
            try {
                val serverAddress = InetAddress.getByName(serverIp)
                val socket = Socket(serverAddress, serverChatPort)

                Log.d("LoginActivity", "TCP socket connection successful!")
                socket.close()

                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    val intent = Intent(this@LoginActivity, ChatActivity::class.java).apply {
                        // Use the newly defined constant here
                        putExtra(NICKNAME_EXTRA, nickname)
                    }
                    startActivity(intent)
                    finish()
                }
            } catch (e: Exception) {
                Log.e("LoginActivity", "Error connecting to server: ${e.message}")
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    btnConnect.isEnabled = true
                    Toast.makeText(this@LoginActivity, "Connection failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job?.cancel()
    }
}
