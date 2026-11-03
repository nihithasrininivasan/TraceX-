
package com.traceroute.networking;

import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import java.net.ServerSocket;
import java.net.Socket;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
public class TCPDemoService {

    // Demonstrate TCP client-server communication
    public Map<String, Object> demonstrateTCP(String message) {

        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException(
                    "Message cannot be empty"
            );
        }

        try (ServerSocket serverSocket = new ServerSocket(0)) {

            // Get an available port
            int port = serverSocket.getLocalPort();

            CompletableFuture<String> receivedMessage =
                    new CompletableFuture<>();

            // Start TCP Server
            Thread serverThread = new Thread(() -> {

                try (
                    Socket serverConnection = serverSocket.accept();

                    BufferedReader input =
                        new BufferedReader(
                            new InputStreamReader(
                                serverConnection.getInputStream()
                            )
                        );

                    PrintWriter output =
                        new PrintWriter(
                            serverConnection.getOutputStream(),
                            true
                        )
                ) {

                    // Receive message from client
                    String received = input.readLine();

                    // Generate server response
                    String response = "TCP Server received: " + received;

                    // Send response to client
                    output.println(response);

                    receivedMessage.complete(received);

                } catch (IOException e) {

                    receivedMessage.completeExceptionally(e);
                }
            });

            serverThread.start();

            // Start TCP Client
            try (
                Socket clientSocket =
                    new Socket("127.0.0.1", port);

                PrintWriter output =
                    new PrintWriter(
                        clientSocket.getOutputStream(),
                        true
                    );

                BufferedReader input =
                    new BufferedReader(
                        new InputStreamReader(
                            clientSocket.getInputStream()
                        )
                    )
            ) {

                // Send message to server
                output.println(message);

                // Receive server response
                String response = input.readLine();

                String received = receivedMessage.get(
                        5, TimeUnit.SECONDS
                );

                // Return communication details
                return Map.of(
                    "protocol", "TCP",
                    "serverAddress", "127.0.0.1",
                    "serverPort", port,
                    "sentMessage", message,
                    "receivedMessage", received,
                    "serverResponse", response,
                    "status", "TCP communication successful"
                );
            }

        } catch (Exception e) {

            throw new RuntimeException(
                    "TCP communication failed: " + e.getMessage(),
                    e
            );
        }
    }
}
