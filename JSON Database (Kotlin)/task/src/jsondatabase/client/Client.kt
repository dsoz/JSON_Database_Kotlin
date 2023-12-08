package jsondatabase.client

import jsondatabase.server.toJsonFromMutableMap
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.InetAddress
import java.net.Socket
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import java.io.File
import java.util.concurrent.locks.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

const val address = "127.0.0.1"
const val port = 23456
val socket = Socket(InetAddress.getByName(address), port)
val input = DataInputStream(socket.getInputStream())
val output = DataOutputStream(socket.getOutputStream())
val filePath = "C:\\Users\\Sigon\\IdeaProjects\\JSON Database (Kotlin)\\JSON Database (Kotlin)\\task\\src\\jsondatabase\\client\\data\\"


fun main(args: Array<String>){
    println("Client started!")

    sendClientData(readInput(args))
    receiveClientData()
}
fun readInput(args: Array<String>): String{
    return if (args.indexOf("-in") != -1){
        val fileName = args[args.indexOf("-in") + 1]
        readFile(fileName)

    } else{
        prepareDataAsJson(parseInput(args))
    }
}

/*fun readInputB(args: Array<String>): ByteArray{
    return if (args.indexOf("-in") != -1){
        val fileName = args[args.indexOf("-in") + 1]
        readFile(fileName)

    } else{
        prepareDataAsJsonB(parseInput(args))
    }
}*/

fun parseInput(args: Array<String>): MutableList<String>{
    val list = mutableListOf<String>()
    if (args.indexOf("-t") != -1) {
        list.add(args[args.indexOf("-t") + 1])
        }
    if (args.indexOf("-k") != -1) {
        list.add(args[args.indexOf("-k") + 1])
    }
    if (args.contains("-v")) {
        list.add(args[args.indexOf("-v") + 1].trim())
    }
    return list
}

fun sendClientData(message: String){
    output.writeUTF(message)
    println("Sent: $message")
}

fun sendClientData(messageList: MutableList<String>){
    sendClientData(messageList.reduce { acc, s -> acc.plus(" $s") })
}

fun prepareDataAsJson(messageList: MutableList<String>): String{
    val message = mutableMapOf<String, String>()

    if (messageList.size >= 1) {
        message["type"] = messageList[0]
        if (messageList.size >= 2){
            message["key"] = messageList[1]
            if (messageList.size == 3){
                message["value"] = messageList[2]
            }
        }
    }
    return Json.encodeToString(message)
}

fun receiveClientData(): String{
    val result = input.readUTF()

    println("Received: $result")
    return result
}

fun readFile(fileName: String): String{
    val fileToRead = File(filePath + fileName)
    return fileToRead.readText()
}