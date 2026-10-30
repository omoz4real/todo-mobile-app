package com.todo.dao;

import com.todo.model.Todo;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TodoDaoTest {

    private static TodoDao todoDao;

    @BeforeAll
    public static void setUp() {
        DatabaseManager.initializeDatabase();
        todoDao = new TodoDao();
    }

    @Test
    public void testTodoCrudLifecycle() {
        // 1. Insert
        Todo newTodo = new Todo("Test Task", "Testing CRUD lifecycle", "High", "2026-12-31", false);
        Todo inserted = todoDao.insert(newTodo);
        assertNotNull(inserted, "Inserted todo should not be null");
        assertTrue(inserted.getId() > 0, "Inserted todo should have a positive generated ID");

        // 2. Read / List
        List<Todo> allTodos = todoDao.getAllTodos();
        assertFalse(allTodos.isEmpty(), "Todo list should not be empty");
        boolean found = allTodos.stream().anyMatch(t -> t.getId() == inserted.getId());
        assertTrue(found, "Inserted todo should be in the returned list");

        // 3. Update
        inserted.setTitle("Updated Test Task");
        inserted.setPriority("Low");
        boolean updated = todoDao.update(inserted);
        assertTrue(updated, "Update should return true");

        // 4. Toggle completion
        boolean toggled = todoDao.toggleCompleted(inserted.getId(), true);
        assertTrue(toggled, "Toggle completion should return true");

        // 5. Delete
        boolean deleted = todoDao.delete(inserted.getId());
        assertTrue(deleted, "Delete should return true");

        // Verify deleted
        List<Todo> afterDelete = todoDao.getAllTodos();
        boolean stillExists = afterDelete.stream().anyMatch(t -> t.getId() == inserted.getId());
        assertFalse(stillExists, "Deleted todo should no longer be in the list");
    }
}
