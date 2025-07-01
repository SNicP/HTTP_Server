package ru.netology;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

public class Handler implements Runnable {
    private final Socket socket;
    private final BufferedReader in;
    private final BufferedOutputStream out;
    final List<String> validPaths = List.of("/index.html", "/spring.svg", "/spring.png", "/resources.html","/styles.css", "/app.js", "/links.html", "/forms.html", "/classic.html", "/events.html", "/events.js");

    public Handler(Socket socket) throws IOException {
        this.socket = socket;
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.out = new BufferedOutputStream(socket.getOutputStream()) ;
    }

    @Override
    public void run(){
        try{
            while (true){
                final String requestLine = in.readLine();
                final String[] parts = requestLine.split(" ");
                System.out.println(requestLine);

                if (parts.length != 3){
                    //просто закрываем socket
                    continue;
                }

                final String path = parts[1];
                if (!validPaths.contains(path)){
                    out.write((
                            "HTTP/1.1 404 Not Found \r\n" +
                                    "Content-Length: 0\r\n" +
                                    "Connection: close\r\n" +
                                    "\r\n"
                    ).getBytes());
                    out.flush();
                    continue;
                }
                final Path filePath = Path.of(".","public", path);
                final String mimeType = Files.probeContentType(filePath);

                //special case for classic
                if (path.equals("/classic.html")){
                    final String template = Files.readString(filePath);
                    final byte[] content = template.replace(
                            "{time}",
                            LocalDateTime.now().toString()
                    ).getBytes();
                    out.write((
                            "HTTP/1.1 200 OK\r\n" +
                                    "Content-Type:" + mimeType + "\r\n" +
                                    "Content-Length: " + content.length + "\r\n" +
                                    "Connection: close\r\n" +
                                    "\r\n"
                    ).getBytes());
                    out.write(content);
                    out.flush();
                    continue;
                }
                final long length = Files.size(filePath);
                out.write((
                        "HTTP/1.1 200 OK\r\n" +
                                "Content-Type: " + mimeType + "\r\n" +
                                "Content-Length: " + length + "\r\n" +
                                "Connection: close\r\n" +
                                "\r\n"
                ).getBytes());
                Files.copy(filePath,out);
                out.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
