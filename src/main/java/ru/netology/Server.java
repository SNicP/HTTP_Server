package ru.netology;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    final static int MAX_THREADS = 64;

    private final Map<String, Map<String, Handler>> handlers = new ConcurrentHashMap<>();

    private final ExecutorService threadPool = Executors.newFixedThreadPool(MAX_THREADS);

    public void addHandler(String method, String path, Handler handler) {
        handlers.computeIfAbsent(method.toUpperCase(), k -> new ConcurrentHashMap<>())
                .put(path, handler);
    }

    public void listen(int port) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server started on port " + port);

            while (!Thread.currentThread().isInterrupted()) {
                Socket clientSocket = serverSocket.accept();
                threadPool.execute(new ConnectionHandler(clientSocket, handlers));
            }

        } catch (IOException exception) {
            exception.printStackTrace();
        } finally {
            threadPool.shutdown();
        }
    }

    private static void respondNotFound(java.io.BufferedOutputStream out) throws IOException {
        String response =
                "HTTP/1.1 404 Not Found\r\n" +
                        "Content-Length: 0\r\n" +
                        "Connection: close\r\n" +
                        "\r\n";
        out.write(response.getBytes());
        out.flush();
    }

    private static void respondBadRequest(java.io.BufferedOutputStream out) throws IOException {
        String response =
                "HTTP/1.1 400 Bad Request\r\n" +
                        "Content-Length: 0\r\n" +
                        "Connection: close\r\n" +
                        "\r\n";
        out.write(response.getBytes());
        out.flush();
    }
}
