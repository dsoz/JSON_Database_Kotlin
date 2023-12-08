package jsondatabase.server

import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket

const val address = "127.0.0.1"
const val port = 23456

class Server {
    private val server = ServerSocket(port, 50, InetAddress.getByName(address))
    private var socket: Socket = Socket()

    fun run(){
        socket = server.accept()
    }
    fun close(){
        socket.close()
    }

    fun closeServer(){
        server.close()
    }

    fun sendServerData(message: String){
        val output = DataOutputStream(socket.getOutputStream())
        output.writeUTF(message)
    }

    fun receiveServerData(): String{
        val input = DataInputStream(socket.getInputStream())
        return input.readUTF()
    }
}