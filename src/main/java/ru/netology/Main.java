package ru.netology;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class Main {
    public static final String GET = "GET";
    public static final String POST = "POST";
    final static int PORT = 9999;
    static final List<String> allowedMethods = List.of(GET, POST);

    public static void main(String[] args) {


        Server server = new Server();

        server.addHandler("GET", "/messages", (request, responseStream) -> {
            if (checkAllowedMethod(request.getMethod(), responseStream)) {
                return;
            }
            String response = "HTTP/1.1 200 OK\r\n" +
                    "Content-Type: text/plain\r\n" +
                    "Content-Length: 18\r\n" +
                    "Connection: close\r\n" +
                    "\r\n";
            responseStream.write(response.getBytes());
            responseStream.flush();
        });

        server.addHandler("POST", "/messages", (request, responseStream) -> {
            if (checkAllowedMethod(request.getMethod(), responseStream)) {
                return;
            }
            String response = "HTTP/1.1 201 Created\r\n" +
                    "Content-Type: text/plain\r\n" +
                    "Content-Length: 19\r\n" +
                    "Connection: close\r\n" +
                    "\r\n";
            responseStream.write(response.getBytes());
            responseStream.flush();
        });

        server.addHandler("POST", "/", (request, responseStream) -> {
            if (checkAllowedMethod(request.getMethod(), responseStream)) {
                return;
            }
            String value = request.getBodyParam("value");
            String responseBody = value != null ? value : "<no value>";
            String response = "HTTP/1.1 200 OK\r\n" +
                    "Content-Type: text/plain; charset=UTF-8\r\n" +
                    "Content-Length: " + responseBody.getBytes().length + "\r\n" +
                    "Connection: close\r\n" +
                    "\r\n" +
                    responseBody;
            responseStream.write(response.getBytes());
            responseStream.flush();
        });

        server.listen(PORT);
    }

    private static boolean checkAllowedMethod(String method, BufferedOutputStream responseStream) throws IOException {
        if (!allowedMethods.contains(method)) {
            String response = "HTTP/1.1 405 Method Not Allowed\r\n" +
                    "Content-Length: 0\r\n" +
                    "Connection: close\r\n" +
                    "\r\n";
            responseStream.write(response.getBytes(StandardCharsets.UTF_8));
            responseStream.flush();
            return true;  // метод запрещён — сигнал прерывания
        }
        return false; // метод разрешён — продолжать
    }
}

