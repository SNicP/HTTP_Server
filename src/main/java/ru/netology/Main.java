package ru.netology;

public class Main {
    final static int PORT = 9999;

    public static void main(String[] args) {

        Server server = new Server();

        server.addHandler("GET", "/messages", (request, responseStream) -> {
            String response = "HTTP/1.1 200 OK\r\n" +
                    "Content-Type: text/plain\r\n" +
                    "Content-Length: 18\r\n" +
                    "Connection: close\r\n" +
                    "\r\n";
            responseStream.write(response.getBytes());
            responseStream.flush();
        });

        server.addHandler("POST", "/messages", (request, responseStream) -> {
            String response = "HTTP/1.1 201 Created\r\n" +
                    "Content-Type: text/plain\r\n" +
                    "Content-Length: 19\r\n" +
                    "Connection: close\r\n" +
                    "\r\n";
            responseStream.write(response.getBytes());
            responseStream.flush();
        });

        server.listen(PORT);
    }
}
