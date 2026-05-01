package com.tecnomate;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.logging.Logger;

/**
 * Smart Feature: Auto-priority suggestions.
 * Calls Claude Haiku to classify task priority as low / medium / high.
 * Falls back to MEDIUM on any failure — never blocks task creation.
 */
public class PriorityService {

    private static final Logger LOG   = Logger.getLogger(PriorityService.class.getName());
    private static final String MODEL = "claude-haiku-4-5-20251001";
    private static final String URL   = "https://api.anthropic.com/v1/messages";

    private final String     apiKey;
    private final HttpClient http;

    public PriorityService(String apiKey) {
        this.apiKey = apiKey;
        this.http   = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .build();
    }

    public record Suggestion(Task.Priority priority, String reasoning) {}

    /** Never throws. Falls back to medium on any error. */
    public Suggestion suggest(String title, String description) {
        if (apiKey == null || apiKey.isBlank()) {
            return new Suggestion(Task.Priority.medium, "No API key set — defaulted to medium.");
        }
        try {
            String body = buildRequestBody(title, description);
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(URL))
                .header("x-api-key",          apiKey)
                .header("anthropic-version",   "2023-06-01")
                .header("content-type",        "application/json")
                .timeout(Duration.ofSeconds(10))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());

            if (res.statusCode() != 200) {
                LOG.warning("Anthropic API returned " + res.statusCode());
                return new Suggestion(Task.Priority.medium, "AI error — defaulted to medium.");
            }

            return parseResponse(res.body());

        } catch (Exception e) {
            LOG.warning("Priority suggestion failed: " + e.getMessage());
            return new Suggestion(Task.Priority.medium, "AI unavailable — defaulted to medium.");
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private String buildRequestBody(String title, String description) {
        String desc   = description != null ? description : "Not provided.";
        String prompt = "You are a task prioritization assistant. Respond with ONLY a JSON object — no markdown, no extra text.\\n"
            + "Format: {\\\"priority\\\": \\\"high\\\", \\\"reasoning\\\": \\\"One sentence.\\\"}\\n\\n"
            + "Priority rules:\\n"
            + "- high: urgent, deadline-sensitive, blocking others, or safety-critical\\n"
            + "- medium: important but not immediately time-pressured\\n"
            + "- low: nice-to-have, low business impact\\n\\n"
            + "Task title: " + title.replace("\"", "\\\"") + "\\n"
            + "Task description: " + desc.replace("\"", "\\\"");

        return """
            {
              "model": "%s",
              "max_tokens": 150,
              "messages": [{"role": "user", "content": "%s"}]
            }""".formatted(MODEL, prompt);
    }

    /**
     * Parses: {"content":[{"type":"text","text":"{\"priority\":\"high\",\"reasoning\":\"...\"}"}]}
     * Done with simple string extraction — no JSON lib needed for this structure.
     */
    private Suggestion parseResponse(String raw) {
        // Extract the text value from content[0].text
        int textStart = raw.indexOf("\"text\":") + 7;
        // Skip whitespace and opening quote
        while (textStart < raw.length() && (raw.charAt(textStart) == ' ' || raw.charAt(textStart) == '"')) textStart++;
        int textEnd = raw.lastIndexOf("\"}"  );
        String text = raw.substring(textStart, textEnd).replace("\\\"", "\"").replace("\\n", " ");

        String priority  = extractJsonString(text, "priority");
        String reasoning = extractJsonString(text, "reasoning");

        Task.Priority level;
        try {
            level = Task.Priority.valueOf(priority.toLowerCase().trim());
        } catch (Exception e) {
            LOG.warning("Unexpected priority from AI: " + priority);
            level = Task.Priority.medium;
        }
        return new Suggestion(level, reasoning != null ? reasoning : "No reasoning provided.");
    }

    /** Extracts string value for a key from a flat JSON object. */
    private String extractJsonString(String json, String key) {
        String search = "\"" + key + "\"";
        int idx = json.indexOf(search);
        if (idx < 0) return null;
        int colon = json.indexOf(':', idx + search.length());
        int start = json.indexOf('"', colon) + 1;
        int end   = json.indexOf('"', start);
        return json.substring(start, end);
    }
}
