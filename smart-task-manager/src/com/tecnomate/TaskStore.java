package com.tecnomate;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Thread-safe in-memory task store. */
public class TaskStore {

    private final ConcurrentHashMap<String, Task> store = new ConcurrentHashMap<>();
    private final PriorityService priorityService;

    public TaskStore(PriorityService priorityService) {
        this.priorityService = priorityService;
    }

    public Task create(String title, String description) {
        Task task = new Task(title, description);

        // AI enrichment — if it fails, task is still saved with null priority
        PriorityService.Suggestion s = priorityService.suggest(title, description);
        task.setPriority(s.priority());
        task.setPriorityReasoning(s.reasoning());

        store.put(task.getId(), task);
        return task;
    }

    public Optional<Task> markComplete(String id) {
        Task task = store.get(id);
        if (task == null) return Optional.empty();
        task.setStatus(Task.Status.completed);
        return Optional.of(task);
    }

    public Collection<Task> listAll() {
        return store.values();
    }

    public Optional<Task> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }
}
