// app/java/your/package/name/SocketHandler.kt
package com.example.collegechatapp// Change this

import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket

object SocketHandler {
    private var socket: Socket? = null
    var writer: PrintWriter? = null
    var reader: BufferedReader? = null

    @Throws(Exception::class)
    fun connect(ip: String, port: Int) {
        // This will throw an exception if it fails, which is caught in the Activity
        socket = Socket(ip, port)
        writer = PrintWriter(socket!!.getOutputStream(), true)
        reader = BufferedReader(InputStreamReader(socket!!.getInputStream()))
    }

    fun disconnect() {
        writer?.close()
        reader?.close()
        socket?.close()
    }
}