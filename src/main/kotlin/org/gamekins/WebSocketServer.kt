package org.gamekins

import io.ktor.application.*
import io.ktor.http.cio.websocket.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.consumeEach
import java.net.ServerSocket
import java.util.concurrent.CopyOnWriteArrayList

object WebSocketServer {
    private var server: ApplicationEngine? = null
    private val connections = CopyOnWriteArrayList<SendChannel<Frame>>()

    fun startServer() {
        if (server != null) {
            println("Server is already running")
            return
        }

        try {
            val socket = ServerSocket(8443)
            socket.close()

            server = embeddedServer(Netty, port = 8443) {
                extracted()
            }
            server?.start(wait = true)
            println("server has started")
            println(server)
        } catch (e: Exception) {
            println("Error starting server: ${e.message}")
            if (server != null) {
                println("Using the running server")
            } else {
                println("No running server found, consider resolving port conflict")
            }
        }
    }

    private fun Application.extracted() {
        install(WebSockets)
        routing {
            webSocket("/jenkins/send") {
                println("WebSocket connection established")
                connections.add(this.outgoing)

                try {
                    incoming.consumeEach { frame ->
                        if (frame is Frame.Text) {
                            println("Received: ${frame.readText()}")
                        }
                    }
                } catch (e: ClosedReceiveChannelException) {
                    println("Connection closed")
                } finally {
                    connections.remove(this.outgoing)
                }
            }
        }
    }

    fun stopServer() {
        server?.stop(0, 0)
        server = null
    }

    suspend fun sendMessage(message: String) {
        connections.forEach { channel ->
            try {
                channel.send(Frame.Text(message))
            } catch (e: Exception) {
                println("Error sending message: ${e.message}")
            }
        }
    }
}
