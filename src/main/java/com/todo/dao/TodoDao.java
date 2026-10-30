package com.todo.dao;

import com.todo.model.Todo;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;

public class TodoDao {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String FALLBACK_FILE_NAME = "todos.dat";

    public List<Todo> getAllTodos() {
        if (!DatabaseManager.isJdbcAvailable()) {
            return getAllFromFile();
        }

        List<Todo> todos = new ArrayList<>();
        String sql = "SELECT id, title, description, priority, due_date, completed, created_at FROM todos ORDER BY completed ASC, due_date ASC, id DESC";

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                todos.add(mapResultSetToTodo(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching todos from JDBC: " + e.getMessage() + ". Using file fallback.");
            return getAllFromFile();
        }
        return todos;
    }

    public Todo insert(Todo todo) {
        String now = LocalDateTime.now().format(DATE_TIME_FORMATTER);
        if (todo.getCreatedAt() == null || todo.getCreatedAt().isBlank()) {
            todo.setCreatedAt(now);
        }

        if (!DatabaseManager.isJdbcAvailable()) {
            return insertToFile(todo);
        }

        String sql = "INSERT INTO todos (title, description, priority, due_date, completed, created_at) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, todo.getTitle());
            pstmt.setString(2, todo.getDescription());
            pstmt.setString(3, todo.getPriority() != null ? todo.getPriority() : "Medium");
            pstmt.setString(4, todo.getDueDate());
            pstmt.setInt(5, todo.isCompleted() ? 1 : 0);
            pstmt.setString(6, todo.getCreatedAt());

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        todo.setId(generatedKeys.getInt(1));
                    }
                }
            }
            return todo;
        } catch (SQLException e) {
            System.err.println("Error inserting todo via JDBC: " + e.getMessage() + ". Falling back to file.");
            return insertToFile(todo);
        }
    }

    public boolean update(Todo todo) {
        if (!DatabaseManager.isJdbcAvailable()) {
            return updateInFile(todo);
        }

        String sql = "UPDATE todos SET title = ?, description = ?, priority = ?, due_date = ?, completed = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, todo.getTitle());
            pstmt.setString(2, todo.getDescription());
            pstmt.setString(3, todo.getPriority() != null ? todo.getPriority() : "Medium");
            pstmt.setString(4, todo.getDueDate());
            pstmt.setInt(5, todo.isCompleted() ? 1 : 0);
            pstmt.setInt(6, todo.getId());

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating todo via JDBC: " + e.getMessage() + ". Falling back to file.");
            return updateInFile(todo);
        }
    }

    public boolean toggleCompleted(int id, boolean completed) {
        if (!DatabaseManager.isJdbcAvailable()) {
            List<Todo> all = getAllFromFile();
            for (Todo t : all) {
                if (t.getId() == id) {
                    t.setCompleted(completed);
                    return saveAllToFile(all);
                }
            }
            return false;
        }

        String sql = "UPDATE todos SET completed = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, completed ? 1 : 0);
            pstmt.setInt(2, id);

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error toggling completion via JDBC: " + e.getMessage());
            return false;
        }
    }

    public boolean delete(int id) {
        if (!DatabaseManager.isJdbcAvailable()) {
            return deleteFromFile(id);
        }

        String sql = "DELETE FROM todos WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error deleting todo via JDBC: " + e.getMessage());
            return deleteFromFile(id);
        }
    }

    private Todo mapResultSetToTodo(ResultSet rs) throws SQLException {
        return new Todo(
            rs.getInt("id"),
            rs.getString("title"),
            rs.getString("description"),
            rs.getString("priority"),
            rs.getString("due_date"),
            rs.getInt("completed") == 1,
            rs.getString("created_at")
        );
    }

    // --- File Storage Fallback for Mobile / Android runtime ---
    private File getFallbackFile() {
        return new File(DatabaseManager.getStorageDirectory(), FALLBACK_FILE_NAME);
    }

    private synchronized List<Todo> getAllFromFile() {
        List<Todo> list = new ArrayList<>();
        File file = getFallbackFile();
        if (!file.exists()) {
            return list;
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] parts = line.split("\t", -1);
                if (parts.length >= 7) {
                    int id = Integer.parseInt(parts[0]);
                    String title = decode(parts[1]);
                    String desc = decode(parts[2]);
                    String priority = parts[3];
                    String dueDate = parts[4].isEmpty() ? null : parts[4];
                    boolean completed = Boolean.parseBoolean(parts[5]);
                    String createdAt = parts[6];
                    list.add(new Todo(id, title, desc, priority, dueDate, completed, createdAt));
                }
            }
        } catch (Exception e) {
            System.err.println("Error reading fallback storage: " + e.getMessage());
        }

        list.sort(Comparator.comparing(Todo::isCompleted)
                .thenComparing(t -> t.getDueDate() == null ? "" : t.getDueDate())
                .thenComparing(Comparator.comparingInt(Todo::getId).reversed()));
        return list;
    }

    private synchronized Todo insertToFile(Todo todo) {
        List<Todo> all = getAllFromFile();
        int maxId = all.stream().mapToInt(Todo::getId).max().orElse(0);
        todo.setId(maxId + 1);
        all.add(todo);
        saveAllToFile(all);
        return todo;
    }

    private synchronized boolean updateInFile(Todo todo) {
        List<Todo> all = getAllFromFile();
        boolean found = false;
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).getId() == todo.getId()) {
                all.set(i, todo);
                found = true;
                break;
            }
        }
        return found && saveAllToFile(all);
    }

    private synchronized boolean deleteFromFile(int id) {
        List<Todo> all = getAllFromFile();
        boolean removed = all.removeIf(t -> t.getId() == id);
        return removed && saveAllToFile(all);
    }

    private synchronized boolean saveAllToFile(List<Todo> todos) {
        File file = getFallbackFile();
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
            for (Todo t : todos) {
                writer.write(t.getId() + "\t"
                        + encode(t.getTitle()) + "\t"
                        + encode(t.getDescription()) + "\t"
                        + (t.getPriority() != null ? t.getPriority() : "Medium") + "\t"
                        + (t.getDueDate() != null ? t.getDueDate() : "") + "\t"
                        + t.isCompleted() + "\t"
                        + (t.getCreatedAt() != null ? t.getCreatedAt() : ""));
                writer.newLine();
            }
            return true;
        } catch (Exception e) {
            System.err.println("Error writing fallback storage: " + e.getMessage());
            return false;
        }
    }

    private String encode(String s) {
        if (s == null) return "";
        return Base64.getEncoder().encodeToString(s.getBytes(StandardCharsets.UTF_8));
    }

    private String decode(String s) {
        if (s == null || s.isEmpty()) return "";
        try {
            return new String(Base64.getDecoder().decode(s), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return s;
        }
    }
}
