package ru.netology;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;
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
        try (socket;
             var in = new BufferedInputStream(socket.getInputStream());
             var out = new BufferedOutputStream(socket.getOutputStream())) {

            Request request = Request.parse(in);

            if (request == null) {
                respondBadRequest(out);
                return;
            }

            System.out.println("Request received: " + request.getMethod() + " " + request.getPath());

            Map<String, Handler> methodHandlers = handlers.get(request.getMethod().toUpperCase());
            if (methodHandlers != null) {
                Handler handler = methodHandlers.get(request.getPath());
                if (handler != null) {
                    handler.handle(request, out);
                    return;
                }
            }

            if (Request.validPaths.contains(request.getPath())) {
                serveStaticFile(request.getPath(), out);
                return;
            }

            respondNotFound(out);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void serveStaticFile(String path, BufferedOutputStream out) throws IOException {
        String relativePath = path.substring(1);

        Path filePath = Path.of(".", "public", relativePath);

        if (!Files.exists(filePath)) {
            respondNotFound(out);
            return;
        }

        byte[] content;
        String contentType = Files.probeContentType(filePath);

        if ("/classic.html".equals(path)) {
            String template = Files.readString(filePath);
            String replaced = template.replace("{time}", LocalDateTime.now().toString());
            content = replaced.getBytes();
            contentType = "text/html";
        } else {
            content = Files.readAllBytes(filePath);
        }

        sendResponse(out, "200 OK", content, contentType);
    }

    private void sendResponse(BufferedOutputStream out, String status, byte[] body, String contentType) throws IOException {
        String response = "HTTP/1.1 " + status + "\r\n" +
                "Content-Type: " + (contentType != null ? contentType : "application/octet-stream") + "\r\n" +
                "Content-Length: " + body.length + "\r\n" +
                "Connection: close\r\n" +
                "\r\n";

        out.write(response.getBytes());
        out.write(body);
        out.flush();
    }

    private void respondNotFound(BufferedOutputStream out) throws IOException {
        String response =
                "HTTP/1.1 404 Not Found\r\n" +
                        "Content-Length: 0\r\n" +
                        "Connection: close\r\n" +
                        "\r\n";
        out.write(response.getBytes());
        out.flush();
    }

    private void respondBadRequest(BufferedOutputStream out) throws IOException {
        String response =
                "HTTP/1.1 400 Bad Request\r\n" +
                        "Content-Length: 0\r\n" +
                        "Connection: close\r\n" +
                        "\r\n";
        out.write(response.getBytes());
        out.flush();
    }
}
