package ru.netology;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Map;

public class ConnectionHandler implements Runnable {
    private final Socket socket;
    private final Map<String, Map<String, Handler>> handlers;

    public ConnectionHandler(Socket socket, Map<String, Map<String, Handler>> handlers) {
        this.socket = socket;
        this.handlers = handlers;
    }

    @Override
    public void run() {
        try (
                socket;
                var in = socket.getInputStream();
                var out = socket.getOutputStream();
                var bufferedOut = new BufferedOutputStream(out)
        ) {
            Request request = Request.parse(in);
            if (request == null) {
                respondBadRequest(bufferedOut);
                return;
            }

            // Ищем динамический обработчик для метода и пути
            Map<String, Handler> methodHandlers = handlers.get(request.getMethod().toUpperCase());
            if (methodHandlers != null) {
                Handler handler = methodHandlers.get(request.getPath());
                if (handler != null) {
                    handler.handle(request, bufferedOut);
                    return;
                }
            }

            if (Request.validPaths.contains(request.getPath())) {
                String relativePath = request.getPath().substring(1);
                Path filePath = Path.of(".", "public", relativePath);

                if (!Files.exists(filePath)) {
                    respondNotFound(bufferedOut);
                    return;
                }

                byte[] content;
                String contentType = Files.probeContentType(filePath);

                if (request.getPath().equals("/classic.html")) {
                    String template = Files.readString(filePath, StandardCharsets.UTF_8);
                    String replaced = template.replace("{time}", LocalDateTime.now().toString());
                    content = replaced.getBytes(StandardCharsets.UTF_8);
                    contentType = "text/html";
                } else {
                    content = Files.readAllBytes(filePath);
                }

                sendResponse(bufferedOut, "200 OK", content, contentType);
                return;
            }

            respondNotFound(bufferedOut);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendResponse(BufferedOutputStream out, String status, byte[] body, String contentType) throws IOException {
        String response = "HTTP/1.1 " + status + "\r\n" +
                "Content-Type: " + contentType + "\r\n" +
                "Content-Length: " + body.length + "\r\n" +
                "Connection: close\r\n" +
                "\r\n";

        out.write(response.getBytes(StandardCharsets.UTF_8));
        out.write(body);
        out.flush();
    }

    private void respondNotFound(BufferedOutputStream out) throws IOException {
        String response =
                "HTTP/1.1 404 Not Found\r\n" +
                        "Content-Length: 0\r\n" +
                        "Connection: close\r\n" +
                        "\r\n";
        out.write(response.getBytes(StandardCharsets.UTF_8));
        out.flush();
    }

    private void respondBadRequest(BufferedOutputStream out) throws IOException {
        String response =
                "HTTP/1.1 400 Bad Request\r\n" +
                        "Content-Length: 0\r\n" +
                        "Connection: close\r\n" +
                        "\r\n";
        out.write(response.getBytes(StandardCharsets.UTF_8));
        out.flush();
    }
}
