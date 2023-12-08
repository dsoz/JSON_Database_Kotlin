package jsondatabase.server

import java.net.SocketException
import kotlin.system.exitProcess
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import java.io.File
import java.util.InvalidPropertiesFormatException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.ReentrantReadWriteLock


//private val arrayDB = Array<String>(1000){""}
private var arrayDB = mutableMapOf<String, String>()
private var arrayDBPro = mutableMapOf<String, Any>()
private val dbFile = File("C:\\Users\\Sigon\\IdeaProjects\\JSON Database (Kotlin)\\JSON Database (Kotlin)\\task\\src\\jsondatabase\\server\\data\\db.json")
val lock: ReadWriteLock = ReentrantReadWriteLock()
val readLock: Lock = lock.readLock()
val writeLock: Lock = lock.writeLock()

fun main(){
    val executor: ExecutorService = Executors.newFixedThreadPool(2)
    val server = Server()

    executor.submit{
        readJsonDataPro(server)
    }
}

fun inputCommand(input: String): String{
    while (true){
        val inputLine = input.split(" ")
        var index = ""
        val commandName = inputLine[0].lowercase()
        var entriesList = listOf<String>()

        if (inputLine.size > 1) {
            index = inputLine[1]
            if (commandName == "set")
                entriesList = inputLine.subList(2, inputLine.size)
        }

       return when(commandName.lowercase()){
            "set" -> setEntry(index, entriesList)
            "get" -> getEntry(index)
            "delete" -> deleteEntry(index)
            else -> return "The input action is invalid"
        }
    }
}

fun setEntry(index: String, entriesList: List<String>): String{
    val db = readDBFile()
    val sb = StringBuilder()

    for (str in entriesList)
        sb.append("$str ")

    db[index] = sb.trim().toString()
    saveDBFile(Json.encodeToString(db))
    return Json.encodeToString(mapOf("response" to "OK"))
}

fun getEntry(index: String): String{
    val db = readDBFile()
    if (!db.containsKey(index))
        return Json.encodeToString(mapOf("response" to "ERROR", "reason" to "No such key"))
    return if (db[index] == "")
        Json.encodeToString(mapOf("response" to "ERROR"))
    else
        Json.encodeToString(mapOf("response" to "OK", "value" to db[index].toString()))
}

fun deleteEntry(index: String): String{
    val db = readDBFile()

    if (!db.containsKey(index))
        return Json.encodeToString(mapOf("response" to "ERROR", "reason" to "No such key"))
    db.remove(index)
    saveDBFile(Json.encodeToString(db))
    return Json.encodeToString(mapOf("response" to "OK"))
}

fun readData(){
    val server = Server()
    println("Server started!")

    while (true){
        server.run()
        try {
            val input = server.receiveServerData()
            if (input == "exit") {
                server.sendServerData("OK")
                server.closeServer()
                exitProcess(1)
            }
            server.sendServerData(inputCommand(input))

        } catch (e: SocketException) {
            server.close()
        }
    }
}

fun readJsonData(server: Server){
    println("Server started!")

    while (true){
        server.run()
        try {
            val input = Json.decodeFromString<Map<String, String>>(server.receiveServerData())

            val commandString = StringBuilder()
            if (input.isNotEmpty()) {
                commandString.append(input["type"])
                if (input.size >= 2){
                    commandString.append(" ${input["key"]}")
                    if (input.size == 3){
                        commandString.append(" ${input["value"]}")
                    }
                }
            }
            val result = inputCommand(commandString.toString())
            if (input["type"] == "exit") {
                server.sendServerData(Json.encodeToString(mapOf("response" to "OK")))
                server.closeServer()
                exitProcess(1)
            }
            server.sendServerData(result)

        } catch (e: SocketException) {
            server.close()
        }
    }
}

fun readJsonDataPro(server: Server){
    println("Server started!")
    while (true){
        server.run()

        try {
            val inputJsonStr = server.receiveServerData().trimIndent()
            val inputObj = Json.parseToJsonElement(inputJsonStr) as JsonObject


            val type = inputObj["type"] as JsonPrimitive

            if (type.content == "exit") {
                server.sendServerData(Json.encodeToString(mapOf("response" to "OK")))
                server.closeServer()
                exitProcess(1)
            }

            var keysArray = arrayOf("")
            val keys = inputObj["key"]

            if (keys is JsonPrimitive)
                keysArray = arrayOf(keys.content)
            if (keys is JsonArray)
                keysArray = keys.map { it.jsonPrimitive.content }.toTypedArray()

            val mainDB = readDBFilePro()
            try{
                when (type.content) {
                    "set" -> {
                        val value = inputObj["value"]

                        val result: MutableMap<String, Any> =
                            if (value is JsonObject){
                                setMap(keysArray, toMutableMapFromJson(value), 0, mainDB)
                            } else{
                                if (value is JsonPrimitive){
                                    setMap(keysArray, value.content, 0, mainDB)
                                }
                                setMap(keysArray, value as Any, 0, mainDB)
                            }

                        val jsonStr = toJsonFromMutableMap(result)
                        saveDBFile(jsonStr.toString())
                        server.sendServerData(Json.encodeToString(mapOf("response" to "OK")))
                    }

                    "get" -> {
                        val result = getMap(keysArray, 0, mainDB)
                        val jsonStr = toJsonFromMutableMap(mutableMapOf("response" to "OK", "value" to result))
                        server.sendServerData(jsonStr.toString())
                    }

                    "delete" -> {
                        deleteMap(keysArray, 0, mainDB)
                        saveDBFile(toJsonFromMutableMap(mainDB).toString())
                        server.sendServerData(Json.encodeToString(mapOf("response" to "OK")))
                    }
                }
            } catch (e: InvalidPropertiesFormatException){
                server.sendServerData(Json.encodeToString(mapOf("response" to "ERROR")))
            }
        } catch (e: Exception) {
            println(e.printStackTrace())
            server.close()
        }
    }
}

fun saveDBFile(jsonDBString: String): String{
    writeLock.lock()
    dbFile.writeText(jsonDBString)
    writeLock.unlock()
    return jsonDBString
}

fun readDBFile(): MutableMap<String, String>{
    readLock.lock()
    val db = if (dbFile.exists())
        Json.decodeFromString<MutableMap<String, String>>(dbFile.readText())
    else
        arrayDB

    readLock.unlock()
    return db
}

fun readDBFilePro(): MutableMap<String, Any>{
    readLock.lock()
    val db = if (dbFile.exists()) {
        toMutableMapFromJson(Json.parseToJsonElement(dbFile.readText()))
       // Json.decodeFromString<MutableMap<String, Any>>(dbFile.readText())
    }
    else
        arrayDBPro

    readLock.unlock()
    return db as MutableMap<String, Any>
}

fun toMutableMapFromJson(json: JsonElement): Any{
    when (json) {
        is JsonPrimitive -> {
            return json.content
        }

        is JsonObject -> {
            val map = mutableMapOf<String, Any>()

            for (entry in json){
                map[entry.key] = toMutableMapFromJson(entry.value)
            }
            return map
        }

        is JsonArray -> {
            val list = mutableListOf<Any>()
            for(element in json){
                list.add(toMutableMapFromJson(element))
            }
            return list
        }

        else -> throw IllegalArgumentException("Unsupported JSON element type")
    }
}

fun toJsonFromMutableMap(map: MutableMap<String, Any>): JsonObject{
    val json = buildJsonObject {
        for (entry in map){
            if (entry.value is Map<*, *>){
                put(entry.key, toJsonFromMutableMap(entry.value as MutableMap<String, Any>))
            } else{
              //  println("toJsonFromMutableMap - ${entry.value.toString()}")
                val tmp = entry.value.toString().replace("\"", "")
                put(entry.key, JsonPrimitive(tmp))
            }
        }
    }
    return json
}

fun getMap(keysArray: Array<String>, index: Int, map: MutableMap<String, Any>): Any{
    if (map.containsKey(keysArray[index])){
        if (index < keysArray.lastIndex){
            val subMap = map[keysArray[index]] as MutableMap<String, Any>
            return getMap(keysArray, index + 1, subMap)
        } else{
            when(val result = map[keysArray[index]]){
                is String -> {
                    return result
                }
                is Map<*,*> -> {
                    return result
                }
                is Array<*> -> {
                    return result
                }
            }
        }
    } else{
        throw InvalidPropertiesFormatException("ERROR")
    }
    return Any()
}

fun setMap(keysArray: Array<String>, value: Any, index: Int, map: MutableMap<String, Any>): MutableMap<String, Any>{
    if (index < keysArray.lastIndex){
        if (!map.containsKey(keysArray[index])){
            map[keysArray[index]] = mutableMapOf<String, Any>()
        }
        val subMap = map[keysArray[index]] as MutableMap<String, Any>
        setMap(keysArray, value,index + 1, subMap)
    } else{
        map[keysArray[index]] = value
    }
    return map
}

fun deleteMap(keysArray: Array<String>, index: Int, map: MutableMap<String, Any>): MutableMap<String, Any>{
    if (map.containsKey(keysArray[index])){
        if (index < keysArray.lastIndex){
            val subMap = map[keysArray[index]] as MutableMap<String, Any>
            return deleteMap(keysArray, index + 1, subMap)
        } else{
            map.remove(keysArray[index])
        }
    } else {
        throw InvalidPropertiesFormatException("ERROR")
    }
    return map
}


fun test(){
    val result = """
        {
           "type":"set",
           "key":"person",
           "value":{
              "name":"Elon Musk",
              "car":{
                 "model":"Tesla Roadster",
                 "year":"2018"
              },
              "rocket":{
                 "name":"Falcon 9",
                 "launches":"87"
              }
           }
        }
    """.trimIndent()
    val map =  toMutableMapFromJson(Json.parseToJsonElement(result)) as MutableMap<String, Any>
    saveDBFile(toJsonFromMutableMap(map).toString())
}