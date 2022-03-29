package com.jetbrains.handson.chat.server

import io.ktor.application.*
import io.ktor.http.cio.websocket.*
import io.ktor.routing.*
import io.ktor.websocket.*
import java.util.*
import kotlin.collections.LinkedHashSet

// args are collected from HODEL and passed to Netty server
fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused")
fun Application.module() {
    // use KTOR websockets
    install(WebSockets)
    // for all client connection create a KTOR thread
    routing {
        // set of all client connections
        val connections = Collections.synchronizedSet<Connection?>(LinkedHashSet())
        webSocket("/chat") {
            // add connection to set of connections
            val thisConnection = Connection(this)
            connections += thisConnection
            try {
                send("You are connected! There are ${connections.count()} users here.")

                // if only 1 user, that's the coordinator
                if (connections.count() == 1) {
                    send("You are the coordinator my friend!")
                    thisConnection.name += "-COORD"
                    thisConnection.coord = 1
                }
                // when user connects log
                println("Adding ${thisConnection.name}")

                for (frame in incoming) {
                    frame as? Frame.Text ?: continue
                    val receivedText = frame.readText()
                    // member requests the server to know the existing members
                    var listOfExistingMembers = ""
                    if (receivedText == "/members") {
                        connections.forEach {
                            listOfExistingMembers += "[name: ${it.name}, " +
                                    "coord: ${it.coord}, id: ciccio99, IP: 000, Port: 000]\n"
                        }
                        thisConnection.session.send(listOfExistingMembers)
                        thisConnection.session.send("End of list!")
                    } else {
                        // text to be sent
                        val textWithUsername = "[${thisConnection.name}]: $receivedText"
                        connections.forEach {
                            it.session.send(textWithUsername)
                        }
                    }
                }
            } catch (e: Exception) {
                println(e.localizedMessage)
            } finally {
                // when client disconnects, log client leaving
                println("Removing ${thisConnection.name}")
                // remove that client's connection from the connections hashset
                connections -= thisConnection
                // if the COORD disconnects, then someone else has to take that role
                setNewCoord(connections)
            }
        }
    }
}


suspend fun setNewCoord(connections : MutableSet<Connection>) {
    /*
    Checks the presence of a coordinator in the connections set.
    If there is not, sets that role to the first connection in the set.
     */
    var counter = 0
    connections.forEach { connection: Connection -> if (connection.coord == 1) counter++ }
    if (counter == 0) {
        println("Setting ${connections.elementAt(0).name} as the new COORD...")
        connections.elementAt(0).coord = 1
        connections.forEach { it.session.send("${connections.elementAt(0).name} is the new coordinator!")}
        connections.elementAt(0).name += "-COORD"
    }
}