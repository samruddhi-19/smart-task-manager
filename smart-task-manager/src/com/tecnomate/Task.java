package com.tecnomate;

import java.time.Instant;
import java.util.UUID;

public class Task {

    public enum Status   { pending, completed }
    public enum Priority { low, medium, high }

    private final String  id;
    private final String  title;
    private final String  description;
    private Status        status;
    private Priority      priority;
    private String        priorityReasoning;
    private final Instant createdAt;
    private Instant       updatedAt;

    public Task(String title, String description) {
        this.id          = UUID.randomUUID().toString();
        this.title       = title;
        this.description = description;
        this.status      = Status.pending;
        this.createdAt   = Instant.now();
        this.updatedAt   = Instant.now();
    }

    // ── Getters ───────────────────────────────────────────────────────────────
    public String  getId()               { return id; }
    public String  getTitle()            { return title; }
    public String  getDescription()      { return description; }
    public Status  getStatus()           { return status; }
    public Priority getPriority()        { return priority; }
    public String  getPriorityReasoning(){ return priorityReasoning; }
    public Instant getCreatedAt()        { return createdAt; }
    public Instant getUpdatedAt()        { return updatedAt; }

    // ── Setters ───────────────────────────────────────────────────────────────
    public void setStatus(Status s)            { this.status = s; this.updatedAt = Instant.now(); }
    public void setPriority(Priority p)        { this.priority = p; }
    public void setPriorityReasoning(String r) { this.priorityReasoning = r; }

    /** Serialize to JSON string manually (no external lib needed). */
    public String toJson() {
        return String.format("""
            {
              "id": "%s",
              "title": "%s",
              "description": %s,
              "status": "%s",
              "priority": %s,
              "priorityReasoning": %s,
              "createdAt": "%s",
              "updatedAt": "%s"
            }""",
            id,
            escape(title),
            description != null ? "\"" + escape(description) + "\"" : "null",
            status.name(),
            priority != null ? "\"" + priority.name() + "\"" : "null",
            priorityReasoning != null ? "\"" + escape(priorityReasoning) + "\"" : "null",
            createdAt,
            updatedAt
        );
    }

    private static String escape(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"")
                                 .replace("\n", "\\n").replace("\r", "\\r");
    }
}
