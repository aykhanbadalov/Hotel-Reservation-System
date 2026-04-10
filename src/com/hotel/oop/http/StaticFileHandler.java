package com.hotel.oop.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Serves files from {@code frontend/public} (HTML, compiled {@code style.css}, {@code app.js}).
 * <p>
 * Path traversal is blocked by normalizing and checking the path stays under the document root.
 */
public class StaticFileHandler implements HttpHandler {

    private final Path documentRoot;

    public StaticFileHandler(Path documentRoot) {
        this.documentRoot = documentRoot.toAbsolutePath().normalize();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            exchange.close();
            return;
        }

        String path = exchange.getRequestURI().getPath();
        if (path.equals("/")) {
            path = "/auth.html";
        }

        Path requested = documentRoot.resolve(path.substring(1)).normalize();
        if (!requested.startsWith(documentRoot)) {
            exchange.sendResponseHeaders(403, -1);
            exchange.close();
            return;
        }

        if (!Files.isRegularFile(requested)) {
            exchange.sendResponseHeaders(404, -1);
            exchange.close();
            return;
        }

        byte[] bytes = Files.readAllBytes(requested);
        String mime = guessMime(requested.toString());
        exchange.getResponseHeaders().set("Content-Type", mime);
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static String guessMime(String name) {
        String lower = name.toLowerCase();
        if (lower.endsWith(".html")) {
            return "text/html; charset=UTF-8";
        }
        if (lower.endsWith(".css")) {
            return "text/css; charset=UTF-8";
        }
        if (lower.endsWith(".js")) {
            return "application/javascript; charset=UTF-8";
        }
        if (lower.endsWith(".ico")) {
            return "image/x-icon";
        }
        if (lower.endsWith(".svg")) {
            return "image/svg+xml";
        }
        return "application/octet-stream";
    }
}
