package ru.netology;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private final int MAX_THREADS = 64; // максимальное количество потоков

    private final int port; // переменная дл хранения порта
    private final List<String> validPaths; // переменная для хранения путей
    private final ExecutorService executorService; //

    public Server(int port, List<String> validPaths) {
        this.port = port;
        this.validPaths = validPaths;
        this.executorService = Executors.newFixedThreadPool(MAX_THREADS);
    }

    public void start() {
        try (final ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                final Socket socket = serverSocket.accept();
                Handler handler = new Handler(socket);
                executorService.submit(handler);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
