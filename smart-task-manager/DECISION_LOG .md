# DECISION_LOG.md

This file is supposed to carry more weight than the code itself — it's about
how I thought through the problem, not just what I ended up building.

---

## 1. Time Breakdown

| Phase | Time |
|-------|------|
| Reading the spec, figuring out the structure | 23 min |
| Setting up the project, packages, getting it to compile | 18 min |
| Task model + basic data design | 22 min |
| TaskStore (in-memory storage) | 14 min |
| PriorityService — the AI integration part | 44 min |
| TaskManagerApp — the HTTP server and routing | 21 min |
| README + run scripts | 16 min |
| This file | 28 min |
| **Total** | **~3h 6min** |

The PriorityService took the longest by far. Getting the request body right,
parsing the response without a JSON library, and handling failures gracefully
— that part had a few iterations.

---

## 2. Where I Used AI — and Why

### Project scaffolding and boilerplate
I used AI to get the initial project structure going — package layout, the
basic HttpServer setup, the HttpClient configuration in PriorityService. This
stuff is mostly mechanical and I didn't want to waste time looking up the
exact JDK API signatures.

The interesting decisions came after the scaffolding was done.

### First draft of the Anthropic API call
I asked AI to write the initial `buildRequestBody()` and the HttpClient send
call. The structure of the request — headers, body format, timeout setup —
is the kind of thing AI gets right quickly. I then edited it to add the
prompt I actually wanted and adjusted the timeout values.

### JavaDoc comments
AI wrote the first pass. I edited a couple that were inaccurate — one said
the method "throws IOException" when I'd specifically made it swallow all
exceptions.

---

## 3. Where I Didn't Use AI

### The fallback logic in PriorityService
The decision that task creation should never fail because of the AI call —
that was mine. I wrote the try/catch around the whole suggest() method myself
and decided `medium` was the right default. AI tends to write the happy path
and not think about what happens when an external service is slow or down.
A hanging POST /tasks would be a bad experience and I wanted to prevent that.

### Picking ConcurrentHashMap
I chose ConcurrentHashMap over a plain HashMap without asking AI. The reason
is straightforward — if two POST /tasks requests come in at the same time,
a plain HashMap can corrupt itself. The store is a single instance shared
across all requests, so thread safety matters here even though this is a
small project.

### The ?status filter
The spec only asked for listing all tasks. I added the optional
`?status=pending|completed` query param myself. It was only a few lines and
it makes the API actually useful — without it you'd have to filter client-side
which is annoying. This felt like an obvious small extension that was worth
doing.

### Decision to skip Spring Boot
I made a deliberate call to not use Spring Boot at all and just use the JDK's
built-in HttpServer and HttpClient. The spec didn't require a framework and
adding Spring Boot would mean managing pom.xml, auto-configuration,
application.properties, and a bunch of setup that adds no real value here.
Zero external dependencies keeps it simple — anyone with Java 17+ can just
run it.

---

## 4. AI Outputs I Had to Fix

### Wrong response parsing
The first version of parseResponse() AI wrote was trying to use regex to
extract the priority and reasoning from the Claude response. It broke as soon
as the reasoning text had any special characters in it. I rewrote it to use
simple indexOf-based extraction which is more predictable for the flat JSON
structure I'm getting back.

### Prompt wasn't specific enough
The first prompt AI suggested for the priority classification was pretty vague
— it just said "classify this task as high, medium, or low priority." The
responses were inconsistent. I rewrote the prompt to include explicit rules
for what each level means and told it to respond with ONLY JSON with no
markdown. That made the output reliable enough to parse.

---

## 5. Trade-offs I Made

### No database
The spec says in-memory is fine. Adding H2 or SQLite would mean setting up
JDBC, managing a schema, dealing with connection pooling — none of that adds
anything meaningful for evaluation. ConcurrentHashMap does the job.

### No pagination on GET /tasks
For an in-memory store used in evaluation there's no realistic scale concern.
Adding page/size params would take extra time for zero benefit here.

### No DELETE endpoint
The spec lists three endpoints. I built exactly those three. Adding DELETE
would be scope creep and might actually signal I didn't read the spec carefully.

### Synchronous AI call
The HttpClient.send() call blocks the request thread while waiting for Claude
to respond — potentially 1-3 seconds. For this project scope that's
acceptable. The alternative (firing the AI call async and returning
priority: null immediately) would be cleaner but significantly more complex
to implement correctly.

### Claude Haiku over Sonnet
Priority classification is a simple structured task. Haiku is faster and
cheaper and the quality difference for something this simple is negligible.
Sonnet would just add latency.

---

## 6. What I'd Improve With More Time

**Async AI call** — the biggest one. Return the task immediately with
`priority: null` and update it in the background. This decouples API response
time from AI latency entirely.

**H2 in-memory SQL** — ConcurrentHashMap loses everything on restart. H2
with Spring Data JPA would be a step up with minimal extra config.

**Integration tests** — at minimum I'd want tests for: task creation returns
201, PATCH on a missing ID returns 404, missing title returns 400, and
status filter returns only matching tasks.

**API key validation at startup** — right now if the key is wrong every
request silently falls back to medium. A startup check that logs a clear
warning would surface misconfiguration immediately instead of silently
degrading.
