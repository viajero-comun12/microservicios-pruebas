package com.example.auth;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class App {
    private static final String DB_PATH = System.getenv().getOrDefault("DB_PATH", "/data/auth.db");
    private static final Gson gson = new Gson();

    public static void main(String[] args) throws IOException {
        initDatabase();

        int port = 8001;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/", new RootHandler());
        server.createContext("/health", new HealthHandler());
        server.createContext("/status", new HealthHandler());
        server.createContext("/login", new LoginHandler());
        server.createContext("/register", new RegisterHandler());
        server.createContext("/logs", new LogsHandler());

        server.setExecutor(null);
        System.out.println("Auth API listening on port " + port);
        server.start();
    }

    private static Connection getConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + DB_PATH);
    }

    private static void initDatabase() {
        try {
            File dbFile = new File(DB_PATH);
            if (dbFile.getParentFile() != null) {
                dbFile.getParentFile().mkdirs();
            }

            try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
                // Table for credentials
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS credentials (
                        username TEXT PRIMARY KEY,
                        password TEXT NOT NULL
                    );
                """);

                // Table for authentication records/sessions
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS auth_logs (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        username TEXT NOT NULL,
                        success INTEGER NOT NULL,
                        timestamp DATETIME DEFAULT CURRENT_TIMESTAMP
                    );
                """);

                // Pre-seed default user if table is empty
                ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM credentials");
                if (rs.next() && rs.getInt(1) == 0) {
                    stmt.execute("INSERT INTO credentials (username, password) VALUES ('admin', 'admin123')");
                    stmt.execute("INSERT INTO credentials (username, password) VALUES ('user1', 'password123')");
                    System.out.println("Default credentials seeded: admin/admin123, user1/password123");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void sendJsonResponse(HttpExchange exchange, int statusCode, Object data) throws IOException {
        String json = gson.toJson(data);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static String readRequestBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    static class RootHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestURI().getPath().equals("/")) {
                exchange.sendResponseHeaders(404, -1);
                return;
            }
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }
            Map<String, Object> resp = new HashMap<>();
            resp.put("service", "Auth API (Java)");
            resp.put("status", "running");
            Map<String, String> endpoints = new HashMap<>();
            endpoints.put("GET /", "Informacion del servicio");
            endpoints.put("GET /health", "Verificar estado de la API");
            endpoints.put("POST /login", "Autenticacion y validacion de credenciales");
            endpoints.put("POST /register", "Registrar nuevas credenciales");
            endpoints.put("GET /logs", "Consultar historial de autenticaciones");
            resp.put("endpoints", endpoints);
            sendJsonResponse(exchange, 200, resp);
        }
    }

    static class HealthHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }
            Map<String, Object> resp = new HashMap<>();
            resp.put("status", "UP");
            resp.put("service", "Auth API (Java)");
            resp.put("port", 8001);
            sendJsonResponse(exchange, 200, resp);
        }
    }

    static class LoginHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            try {
                String body = readRequestBody(exchange);
                JsonObject json = JsonParser.parseString(body).getAsJsonObject();
                String username = json.has("username") ? json.get("username").getAsString() : null;
                String password = json.has("password") ? json.get("password").getAsString() : null;

                if (username == null || password == null) {
                    Map<String, Object> err = new HashMap<>();
                    err.put("error", "Missing username or password");
                    sendJsonResponse(exchange, 400, err);
                    return;
                }

                boolean valid = false;
                try (Connection conn = getConnection()) {
                    String query = "SELECT password FROM credentials WHERE username = ?";
                    try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                        pstmt.setString(1, username);
                        ResultSet rs = pstmt.executeQuery();
                        if (rs.next()) {
                            String storedPassword = rs.getString("password");
                            if (storedPassword.equals(password)) {
                                valid = true;
                            }
                        }
                    }

                    // Log the authentication attempt into SQLite
                    String logQuery = "INSERT INTO auth_logs (username, success) VALUES (?, ?)";
                    try (PreparedStatement logStmt = conn.prepareStatement(logQuery)) {
                        logStmt.setString(1, username);
                        logStmt.setInt(2, valid ? 1 : 0);
                        logStmt.executeUpdate();
                    }
                }

                Map<String, Object> resp = new HashMap<>();
                if (valid) {
                    resp.put("valid", true);
                    resp.put("status", "success");
                    resp.put("message", "Authentication successful");
                    resp.put("username", username);
                    sendJsonResponse(exchange, 200, resp);
                } else {
                    resp.put("valid", false);
                    resp.put("status", "error");
                    resp.put("message", "Invalid credentials");
                    sendJsonResponse(exchange, 401, resp);
                }
            } catch (Exception e) {
                Map<String, Object> err = new HashMap<>();
                err.put("error", "Internal server error: " + e.getMessage());
                sendJsonResponse(exchange, 500, err);
            }
        }
    }

    static class RegisterHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            try {
                String body = readRequestBody(exchange);
                JsonObject json = JsonParser.parseString(body).getAsJsonObject();
                String username = json.has("username") ? json.get("username").getAsString() : null;
                String password = json.has("password") ? json.get("password").getAsString() : null;

                if (username == null || password == null) {
                    Map<String, Object> err = new HashMap<>();
                    err.put("error", "Missing username or password");
                    sendJsonResponse(exchange, 400, err);
                    return;
                }

                try (Connection conn = getConnection()) {
                    String query = "INSERT INTO credentials (username, password) VALUES (?, ?)";
                    try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                        pstmt.setString(1, username);
                        pstmt.setString(2, password);
                        pstmt.executeUpdate();
                    }
                }

                Map<String, Object> resp = new HashMap<>();
                resp.put("message", "User credentials registered successfully");
                resp.put("username", username);
                sendJsonResponse(exchange, 201, resp);
            } catch (SQLException e) {
                Map<String, Object> err = new HashMap<>();
                err.put("error", "Username already exists");
                sendJsonResponse(exchange, 409, err);
            } catch (Exception e) {
                Map<String, Object> err = new HashMap<>();
                err.put("error", "Internal server error: " + e.getMessage());
                sendJsonResponse(exchange, 500, err);
            }
        }
    }

    static class LogsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            List<Map<String, Object>> logs = new ArrayList<>();
            try (Connection conn = getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT id, username, success, timestamp FROM auth_logs ORDER BY id DESC")) {
                while (rs.next()) {
                    Map<String, Object> log = new HashMap<>();
                    log.put("id", rs.getInt("id"));
                    log.put("username", rs.getString("username"));
                    log.put("success", rs.getInt("success") == 1);
                    log.put("timestamp", rs.getString("timestamp"));
                    logs.add(log);
                }
                sendJsonResponse(exchange, 200, logs);
            } catch (SQLException e) {
                Map<String, Object> err = new HashMap<>();
                err.put("error", "Database error: " + e.getMessage());
                sendJsonResponse(exchange, 500, err);
            }
        }
    }
}
