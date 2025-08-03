// app/src/main/java/com/example/collegechatapp/ChatActivity.kt
package com.example.collegechatapp

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException // Import IOException for specific error handling

/**
 * ChatActivity displays the chat messages and allows the user to send messages.
 * It handles receiving messages from the server and sending messages to the server.
 */
class ChatActivity : AppCompatActivity() {

    // Mutable list to hold chat messages that will be displayed in the RecyclerView
    private val messages = mutableListOf<String>()
    // Adapter for the RecyclerView, responsible for binding data to views
    private lateinit var chatAdapter: ChatAdapter
    // Stores the nickname of the current user, received from LoginActivity
    private lateinit var currentNickname: String

    // Companion object to define a constant tag for logging
    companion object {
        private const val TAG = "ChatActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Wrap the entire onCreate content in a try-catch for robust error handling during setup
        try {
            setContentView(R.layout.activity_chat)

            // Initialize UI elements by finding them by their respective IDs
            val rvChatMessages = findViewById<RecyclerView>(R.id.rvChatMessages)
            val etMessage = findViewById<EditText>(R.id.etMessage)
            val btnSend = findViewById<Button>(R.id.btnSend)

            // Retrieve the nickname passed from LoginActivity via Intent extras
            // Use '?: "Unknown"' as a fallback if the nickname is not found
            currentNickname = intent.getStringExtra(LoginActivity.NICKNAME_EXTRA) ?: "Unknown"
            Log.d(TAG, "ChatActivity received nickname: '$currentNickname'") // Log the received nickname

            // Initialize the ChatAdapter with the messages list
            chatAdapter = ChatAdapter(messages)
            // Set the adapter for the RecyclerView
            rvChatMessages.adapter = chatAdapter
            // Set a LinearLayoutManager to arrange items in a vertical list
            rvChatMessages.layoutManager = LinearLayoutManager(this)

            // Launch a coroutine in the lifecycleScope to listen for incoming messages from the server.
            // This coroutine will run on the Dispatchers.IO thread pool, which is suitable for network operations.
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    // Loop indefinitely to continuously read messages from the server
                    while (true) {
                        // Attempt to read a line from the socket. This is a blocking IO operation.
                        val message = SocketHandler.reader?.readLine()

                        // If readLine() returns null, it means the server has closed its end of the connection.
                        if (message == null) {
                            Log.d(TAG, "Socket reader returned null. Connection likely closed by server or client.")
                            break // Exit the loop to handle disconnection
                        }
                        Log.d(TAG, "Received message from server: '$message'") // Log the raw message received

                        // Check if the received message is the "NICK" prompt from the server
                        if (message == "NICK") {
                            // The server is asking for our nickname. Send it back.
                            // Ensure SocketHandler.writer is not null before attempting to write
                            if (SocketHandler.writer != null) {
                                // Send the current user's nickname to the server
                                SocketHandler.writer?.println(currentNickname)
                                // Explicitly flush the writer to ensure the data is sent immediately over the network
                                SocketHandler.writer?.flush()
                                Log.d(TAG, "Sent nickname '$currentNickname' to server as NICK response and flushed.")
                            } else {
                                // If writer is null, it indicates a critical issue with the socket connection
                                Log.e(TAG, "SocketHandler.writer is null when trying to send nickname! Cannot send.")
                                // Throw an exception to be caught by the outer try-catch for proper error handling
                                throw IllegalStateException("Socket writer is null, cannot send nickname.")
                            }
                        } else {
                            // If it's not "NICK", it's a regular chat message.
                            // Switch to the Main dispatcher to update the UI (RecyclerView).
                            withContext(Dispatchers.Main) {
                                messages.add(message) // Add the new message to the list
                                // Notify the adapter that a new item has been inserted at the end
                                chatAdapter.notifyItemInserted(messages.size - 1)
                                // Scroll the RecyclerView to show the latest message
                                rvChatMessages.scrollToPosition(messages.size - 1)
                                Log.d(TAG, "UI updated with message: '$message'")
                            }
                        }
                    }
                } catch (e: IOException) {
                    // Catch specific IOException for network-related errors (e.g., connection reset, broken pipe)
                    e.printStackTrace()
                    Log.e(TAG, "Network error in chat message listener: ${e.javaClass.simpleName}: ${e.message}", e)
                    withContext(Dispatchers.Main) { // Switch to Main thread to display a Toast message
                        Toast.makeText(
                            this@ChatActivity,
                            "Network error: ${e.javaClass.simpleName} - ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                        finish() // Close the activity on network error
                    }
                } catch (e: Exception) {
                    // Catch any other unexpected exceptions during message listening
                    e.printStackTrace()
                    Log.e(TAG, "Unexpected error in chat message listener: ${e.javaClass.simpleName}: ${e.message}", e)
                    withContext(Dispatchers.Main) { // Switch to Main thread to display a Toast message
                        Toast.makeText(
                            this@ChatActivity,
                            "Disconnected from chat: ${e.javaClass.simpleName} - ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                        finish() // Close the activity on error
                    }
                } finally {
                    // Ensure the socket is disconnected if the loop breaks or an error occurs
                    SocketHandler.disconnect()
                    Log.d(TAG, "Socket disconnected due to loop exit or error.")
                }
            }

            // Set an OnClickListener for the Send button
            btnSend.setOnClickListener {
                val message = etMessage.text.toString().trim() // Get the message from the input field
                if (message.isNotEmpty()) {
                    // Launch a coroutine to send the message to the server.
                    // This will also run on the Dispatchers.IO thread pool.
                    lifecycleScope.launch(Dispatchers.IO) {
                        val fullMessage = "$currentNickname: $message" // Prepend nickname to the message
                        try {
                            // Send the formatted message to the server
                            SocketHandler.writer?.println(fullMessage)
                            // Explicitly flush the writer to ensure the data is sent immediately
                            SocketHandler.writer?.flush()
                            Log.d(TAG, "Sent message to server: '$fullMessage' and flushed.")
                        } catch (writeException: Exception) {
                            // Handle errors during message sending
                            Log.e(TAG, "Error sending chat message: ${writeException.message}", writeException)
                            withContext(Dispatchers.Main) { // Switch to Main thread to show a Toast
                                Toast.makeText(
                                    this@ChatActivity,
                                    "Failed to send message: ${writeException.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                    etMessage.text.clear() // Clear the input field after sending (this is a UI operation, runs on Main thread)
                }
            }
        } catch (e: Exception) {
            // Catch any unexpected exceptions that occur during the initial setup of ChatActivity
            e.printStackTrace()
            Log.e(TAG, "Critical error during ChatActivity onCreate setup: ${e.javaClass.simpleName}: ${e.message}", e)
            Toast.makeText(this, "App error during chat setup: ${e.javaClass.simpleName} - ${e.message}", Toast.LENGTH_LONG).show()
            finish() // Close the activity if setup fails critically
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Ensure the socket is disconnected when the activity is destroyed to release resources.
        // This is launched on Dispatchers.IO to prevent NetworkOnMainThreadException.
        lifecycleScope.launch(Dispatchers.IO) {
            SocketHandler.disconnect()
            Log.d(TAG, "Socket disconnected in onDestroy.")
        }
    }
}
