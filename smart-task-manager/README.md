# Smart Task Manager API

A backend REST API for managing tasks with **AI-powered priority suggestions** via Claude.

---

## ✅ How to Run (Step by Step)

### Step 1 — Make sure Java is installed
Open a terminal and run:
```
java -version
```
You should see something like `openjdk version "17..."` or higher.
If not, download Java from: https://www.oracle.com/java/technologies/downloads/

---

### Step 2 — Set your Anthropic API key (optional)
This enables the AI priority feature. Skip this if you just want to test the API.

**Windows:**
```
set ANTHROPIC_API_KEY=sk-ant-your-key-here
```

**Mac / Linux:**
```
export ANTHROPIC_API_KEY=sk-ant-your-key-here
```

---

### Step 3 — Run the server

**Windows** — double-click `run.bat`, OR in terminal:
```
run.bat
```

**Mac / Linux** — in terminal:
```
./run.sh
```

You should see:
```
✅ Smart Task Manager running on http://localhost:8080
```

---

## 🧪 Testing the API

Open a **new terminal** (keep the server running) and run these commands:

### Create a task
```bash
curl -X POST http://localhost:8080/tasks \
  -H "Content-Type: application/json" \
  -d "{\"title\": \"Fix login bug\", \"description\": \"Users can not log in after update\"}"
```

**Expected response (201):**
```json
{
  "id": "abc-123",
  "title": "Fix login bug",
  "description": "Users can not log in after update",
  "status": "pending",
  "priority": "high",
  "priorityReasoning": "Production login failure affects all users and needs immediate attention.",
  "createdAt": "...",
  "updatedAt": "..."
}
```

---

### List all tasks
```bash
curl http://localhost:8080/tasks
```

### Filter by status
```bash
curl "http://localhost:8080/tasks?status=pending"
curl "http://localhost:8080/tasks?status=completed"
```

---

### Mark a task as complete
Copy the `id` from a previous response, then:
```bash
curl -X PATCH http://localhost:8080/tasks/PASTE-ID-HERE
```

---

## 📁 Project Structure

```
smart-task-manager/
├── run.bat                          ← Run on Windows
├── run.sh                           ← Run on Mac/Linux
├── DECISION_LOG.md                  ← Assignment requirement
├── README.md                        ← This file
└── src/com/tecnomate/
    ├── TaskManagerApp.java          ← Main server + router
    ├── Task.java                    ← Task model
    ├── TaskStore.java               ← In-memory storage
    └── PriorityService.java         ← AI priority suggestions
```

---

## ⚠️ Common Issues

| Problem | Fix |
|--------|-----|
| `java: not found` | Install Java JDK 17+ |
| `javac: not found` | Install JDK (not just JRE). Get it from oracle.com |
| Port 8080 in use | Run `netstat -ano \| findstr 8080` (Windows) and kill that process |
| Priority shows "medium" always | Your `ANTHROPIC_API_KEY` is not set — see Step 2 |
