College Chat App
A simple real-time chat application designed for college students to connect and communicate. This project consists of an Android client and a Python server, enabling basic text-based chat functionality over a local network.

Features
Real-time Messaging: Send and receive messages instantly.

Nickname Support: Users can set a unique nickname before joining the chat.

Multi-Client Support: Multiple users can connect to the same server and chat.

Connection Handling: Basic error handling for network disconnections.

Technologies Used
Client (Android)
Language: Kotlin

IDE: Android Studio

Libraries:

AndroidX Lifecycle (for CoroutineScope)

Kotlin Coroutines (for asynchronous network operations)

AndroidX RecyclerView (for displaying chat messages)

AndroidX ConstraintLayout

Server
Language: Python

Modules: socket, threading, sys

Setup Instructions
1. Server Setup
To run the chat server, you will need Python installed on your machine.

Save the Server Code:
Save the provided Python server code (from python_server_code artifact) into a file named server.py.

Open a Terminal/Command Prompt:
Navigate to the directory where you saved server.py.

Run the Server:
Execute the following command:

python server.py

You should see the message: Server is listening on 0.0.0.0:5050 (or your specific IP address if configured differently).

Keep this terminal window open while you are using the Android app, as it is the chat server.

2. Client Setup (Android)
To run the Android client, you will need Android Studio installed.

Open Project in Android Studio:
Open your CollegeChatApp project in Android Studio.

Update Gradle (if necessary):
Ensure your app/build.gradle.kts and gradle/libs.versions.toml files are up-to-date with the latest dependencies (as provided in previous corrections). Sync your project with Gradle files (File > Sync Project with Gradle Files).

Verify Android Manifest:
Confirm that your AndroidManifest.xml includes the INTERNET permission and usesCleartextTraffic="true" for local network testing:

<uses-permission android:name="android.permission.INTERNET" />
<application
    android:usesCleartextTraffic="true"
    ...>
    <!-- ... -->
</application>

Clean and Rebuild Project:
In Android Studio, go to Build > Clean Project and then Build > Rebuild Project.

Run the App:
Connect an Android device or start an emulator. Click the Run button (green triangle) in Android Studio to deploy the app.

Usage
Start the Python Server as described in the "Server Setup" section.

Launch the Android App on your device or emulator.

Enter Server IP Address:

If running the server on your local machine and using an Android Emulator, the IP address is typically 10.0.2.2.

If running the server on your local machine and using a physical Android device connected to the same Wi-Fi network, you'll need to find your computer's local IP address (e.g., 192.168.1.X). You can usually find this by running ipconfig (Windows) or ifconfig/ip addr show (Linux/macOS) in your terminal.

Enter Your Nickname:
Type your desired nickname in the "Your Nickname" field.

Connect:
Tap the "Connect to Chat" button.

Chat:
If the connection is successful, you will be taken to the chat screen. Type your message in the input field at the bottom and tap "Send". Your message should appear in the chat list, and if other clients are connected, they will also see your message.

Troubleshooting
"Disconnected from chat" error on Android:

Ensure your Python server is running.

Double-check the IP address entered in the Android app. It must be the correct IP of the machine running the server.

Verify that your device/emulator and server machine are on the same network and not blocked by a firewall.

Check the server terminal for any error messages.

Messages not appearing:

Confirm that the server is broadcasting messages correctly (check the broadcast function in server.py).

Ensure the Android client's message receiving loop is active and not encountering exceptions (check Logcat for ChatActivity logs).

Nickname issues (e.g., first message becomes nickname): This was addressed in the latest server code. Ensure you are running the most up-to-date server.py file.
