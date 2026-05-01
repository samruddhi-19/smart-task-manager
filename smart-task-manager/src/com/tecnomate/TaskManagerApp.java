package com.tecnomate;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

/**
 * Smart Task Manager API
 *
 * Zero external dependencies — uses JDK's built-in HTTP server.
 *
 * Endpoints:
 *   POST  /tasks       → create task (with AI priority suggestion)
 *   PATCH /tasks/{id}  → mark complete
 *   GET   /tasks       → list all tasks (?status=pending|completed)
 */
public class TaskManagerApp {

    public static void main(String[] args) throws IOException {
        String apiKey = System.getenv("ANTHROPIC_API_KEY");
        int    port   = 8080;

        PriorityService priorityService = new PriorityService(apiKey);
        TaskStore       store           = new TaskStore(priorityService);

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/tasks", exchange -> {
            try {
                handle(exchange, store);
            } catch (Exception e) {
                sendJson(exchange, 500, "{\"error\":\"Internal server error\"}");
            }
        });

        server.start();
        System.out.println("✅ Smart Task Manager running on http://localhost:" + port);
        System.out.println("   ANTHROPIC_API_KEY " + (apiKey != null ? "detected ✓" : "not set — priority defaults to medium"));
    }

    // ── Router ────────────────────────────────────────────────────────────────

    static void handle(HttpExchange ex, TaskStore store) throws IOException {
        String method = ex.getRequestMethod();
        String path   = ex.getRequestURI().getPath();   // e.g. /tasks or /tasks/abc-123
        String query  = ex.getRequestURI().getQuery();  // e.g. status=pending

        // POST /tasks
        if ("POST".equals(method) && "/tasks".equals(path)) {
            handleCreate(ex, store);

        // GET /tasks
        } else if ("GET".equals(method) && "/tasks".equals(path)) {
            handleList(ex, store, query);

        // PATCH /tasks/{id}
        } else if ("PATCH".equals(method) && path.startsWith("/tasks/")) {
            String id = path.substring("/tasks/".length());
            handleComplete(ex, store, id);

        } else {
            sendJson(ex, 404, "{\"error\":\"Not found\"}");
        }
    }

    // ── Handlers ──────────────────────────────────────────────────────────────

    private static void handleCreate(HttpExchange ex, TaskStore store) throws IOException {
        String body = readBody(ex);

        String title       = extractJsonString(body, "title");
        String description = extractJsonString(body, "description");

        if (title == null || title.isBlank()) {
            sendJson(ex, 400, "{\"error\":\"title is required\"}");
            return;
        }
        if (title.length() > 200) {
            sendJson(ex, 400, "{\"error\":\"title must be under 200 characters\"}");
            return;
        }

        Task task = store.create(title, description);
        sendJson(ex, 201, task.toJson());
    }

    private static void handleList(HttpExchange ex, TaskStore store, String query) throws IOException {
        String statusFilter = null;
        if (query != null) {
            for (String param : query.split("&")) {
                if (param.startsWith("status=")) {
                    statusFilter = param.substring("status=".length());
                }
            }
        }

        final String filter = statusFilter;
        String json = store.listAll().stream()
            .filter(t -> filter == null || t.getStatus().name().equalsIgnoreCase(filter))
            .map(Task::toJson)
            .collect(Collectors.joining(",\n", "[\n", "\n]"));

        sendJson(ex, 200, json);
    }

    private static void handleComplete(HttpExchange ex, TaskStore store, String id) throws IOException {
        if (id.isBlank()) {
            sendJson(ex, 400, "{\"error\":\"Task ID is required\"}");
            return;
        }

        store.markComplete(id)
            .ifPresentOrElse(
                task -> { try { sendJson(ex, 200, task.toJson()); } catch (IOException e) { throw new RuntimeException(e); } },
                ()   -> { try { sendJson(ex, 404, "{\"error\":\"Task not found\"}"); } catch (IOException e) { throw new RuntimeException(e); } }
            );
    }

    // ── Utilities ─────────────────────────────────────────────────────────────

    private static void sendJson(HttpExchange ex, int status, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(bytes); }
    }

    private static String readBody(HttpExchange ex) throws IOException {
        try (InputStream is = ex.getRequestBody()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /**
     * Minimal JSON string extractor — handles the simple flat JSON we receive.
     * Works for "title": "...", "description": "..." keys.
     */
    static String extractJsonString(String json, String key) {
        if (json == null) return null;
        String search = "\"" + key + "\"";
        int idx = json.indexOf(search);
        if (idx < 0) return null;
        int colon = json.indexOf(':', idx + search.length());
        if (colon < 0) return null;
        // Skip whitespace
        int i = colon + 1;
        while (i < json.length() && json.charAt(i) == ' ') i++;
        if (i >= json.length()) return null;
        // null value
        if (json.startsWith("null", i)) return null;
        // String value
        if (json.charAt(i) == '"') {
            int start = i + 1;
            int end   = json.indexOf('"', start);
            return end < 0 ? null : json.substring(start, end);
        }
        return null;
    }
}
