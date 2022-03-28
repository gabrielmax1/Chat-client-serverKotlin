package com.jetbrains.handson.chat.client

import io.ktor.client.*
import io.ktor.client.features.websocket.*
import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import kotlinx.coroutines.*

fun main(args: Array<String>) {
    // Parse CLI Parameters
    val operatingParameters = OperatingParameters().main(args)
    // TODO use operatingParameters
    // use KTOR client web sockets
    val client = HttpClient {
        install(WebSockets)
    }
    //run blocking tread/container/instance (nothing else at the same time) TODO check if comment correct
    runBlocking {
        //create client websocket instance TODO add client init parameters
        client.webSocket(method = HttpMethod.Get, host = "127.0.0.1", port = 8080, path = "/chat") {

            val messageOutputRoutine = launch { outputMessages() }
            val userInputRoutine = launch { inputMessages() }

            userInputRoutine.join() // Wait for completion; either "exit" or error
            messageOutputRoutine.cancelAndJoin()
        }
    }
    //release system resources TODO try with resources
    client.close()
    println("Connection closed. Goodbye!")
}

// Double check this part
suspend fun DefaultClientWebSocketSession.outputMessages() {
    try {
        //for each incoming message
        for (message in incoming) {
            //TODO change to any data type
            message as? Frame.Text ?: continue
            // print message parsed from server
            println(message.readText())
        }
    } catch (e: Exception) {
        //log error
        println("Error while receiving: " + e.localizedMessage)
    }
}

suspend fun DefaultClientWebSocketSession.inputMessages() {
    while (true) {
        //for each user input
        //TODO change
        val message = readLine() ?: ""
        if (message.equals("exit", true)) return
        // member can request existing members
        if (message.equals("/members")) {
            println("Returning existing members from server's set...")
            send("/members")
        } else {
            try {
                // send what you typed
                send(message)
            } catch (e: Exception) {
                println("Error while sending: " + e.localizedMessage)
                return
            }
        }
    }
}
