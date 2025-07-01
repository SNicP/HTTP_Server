package ru.netology;

import java.util.List;

public class Main {
    public static void main(String[] args) {
        final int PORT = 9999;
        final var validPaths = List.of("/index.html", "/spring.svg", "/spring.png", "/resources.html", "/styles.css", "/app.js", "/links.html", "/forms.html", "/classic.html", "/events.html", "/events.js");

        final var server = new Server(PORT, validPaths);

        server.start();
    }
}