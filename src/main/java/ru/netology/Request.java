package ru.netology;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Request {
    private final String method;
    private final String path;
    private final InputStream body;
    private final Map<String, String> headers;

    static final List<String> validPaths = List.of(
            "/index.html",
            "/spring.svg",
            "/spring.png",
            "/resources.html",
            "/styles.css",
            "/app.js",
            "/links.html",
            "/forms.html",
            "/classic.html",
            "/events.html",
            "/events.js"
    );

    public Request(String method, String path, InputStream body, Map<String, String> headers) {
        this.method = method;
        this.path = path;
        this.body = body;
        this.headers = headers;
    }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public InputStream getBody() {
        return body;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    @Override
    public String toString() {
        return "Request{" +
                "method='" + method + '\'' +
                ", path='" + path + '\'' +
                ", headers=" + headers +
                '}';
    }

    public static Request parse(InputStream inStream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inStream));

        // GET /path HTTP/1.1
        String requestLine = reader.readLine();
        if (requestLine == null || requestLine.isEmpty()) {
            return null;
        }

        String[] parts = requestLine.split(" ");
        if (parts.length != 3) {
            return null;
        }

        String method = parts[0];

        String path = parts[1];

        Map<String, String> headers = new HashMap<>();
        String line;
        while ((line = reader.readLine()) != null && !line.isEmpty()) {
            int colonPos = line.indexOf(":");
            if (colonPos != -1) {
                String headerName = line.substring(0, colonPos).trim();
                String headerValue = line.substring(colonPos + 1).trim();
                headers.put(headerName, headerValue);
            }
        }

        InputStream bodyStream = InputStream.nullInputStream();

        return new Request(method, path, bodyStream, headers);
    }
}
