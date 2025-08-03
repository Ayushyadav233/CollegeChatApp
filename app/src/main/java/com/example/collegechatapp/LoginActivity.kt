// app/src/main/java/com/example/collegechatapp/LoginActivity.kt
package com.example.collegechatapp

import android.content.Intent
import android.os.Bundle
import android.util.Log // Import Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "LoginActivity" // Tag for logging
        private const val SERVER_PORT = 5050
        const val NICKNAME_EXTRA = "NICKNAME"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val etIpAddress = findViewById<EditText>(R.id.etIpAddress)
        val etNickname = findViewById<EditText>(R.id.etNickname)
        val btnConnect = findViewById<Button>(R.id.btnConnect)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)

        btnConnect.setOnClickListener {
            val ipAddress = etIpAddress.text.toString().trim()
            val nickname = etNickname.text.toString().trim()

            Log.d(TAG, "Entered IP Address: $ipAddress") // Log entered IP
            Log.d(TAG, "Entered Nickname: $nickname") // Log entered nickname

            if (ipAddress.isEmpty() || nickname.isEmpty()) {
                Toast.makeText(this, "IP and Nickname are required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnConnect.visibility = View.INVISIBLE
            progressBar.visibility = View.VISIBLE

            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    SocketHandler.connect(ipAddress, SERVER_PORT)
                    Log.d(TAG, "Socket connected successfully.")

                    withContext(Dispatchers.Main) {
                        val intent = Intent(this@LoginActivity, ChatActivity::class.java)
                        intent.putExtra(NICKNAME_EXTRA, nickname)
                        Log.d(TAG, "Putting nickname '$nickname' into Intent with key '$NICKNAME_EXTRA'") // Log intent extra
                        startActivity(intent)
                        finish()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Log.e(TAG, "Connection Failed: ${e.message}", e) // Log connection error
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@LoginActivity,
                            "Connection Failed: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                        btnConnect.visibility = View.VISIBLE
                        progressBar.visibility = View.GONE
                    }
                }
            }
        }
    }
}