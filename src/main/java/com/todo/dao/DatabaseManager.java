package com.todo.dao;

import com.gluonhq.attach.storage.StorageService;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {
    private static boolean jdbcAvailable = false;
    private static File storageDirectory = null;

    static {
        initializeStorage();
    }

    public static synchronized void initializeStorage() {
        try {
            storageDirectory = StorageService.create()
                    .flatMap(StorageService::getPrivateStorage)
                    .orElseGet(() -> new File(System.getProperty("user.home"), ".taskflow"));
        } catch (Throwable t) {
            storageDirectory = new File(System.getProperty("user.home"), ".taskflow");
        }

        if (!storageDirectory.exists()) {
            storageDirectory.mkdirs();
        }

        try {
            Class.forName("org.sqlite.JDBC");
            File dbFile = new File(storageDirectory, "todo.db");
            String dbUrl = "jdbc:sqlite:" + dbFile.getAbsolutePath();
            try (Connection conn = DriverManager.getConnection(dbUrl)) {
                initializeDatabaseSchema(conn);
                jdbcAvailable = true;
            }
        } catch (Throwable t) {
            System.err.println("JDBC SQLite driver not available or unsupported on this platform: " + t.getMessage());
            jdbcAvailable = false;
        }
    }

    public static boolean isJdbcAvailable() {
        return jdbcAvailable;
    }

    public static File getStorageDirectory() {
        if (storageDirectory == null) {
            initializeStorage();
        }
        return storageDirectory;
    }

    public static Connection getConnection() throws SQLException {
        if (!jdbcAvailable) {
            throw new SQLException("JDBC SQLite is unavailable on this platform.");
        }
        File dbFile = new File(getStorageDirectory(), "todo.db");
        return DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
    }

    private static void initializeDatabaseSchema(Connection conn) throws SQLException {
        String createTableSQL = """
            CREATE TABLE IF NOT EXISTS todos (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT NOT NULL,
                description TEXT,
                priority TEXT NOT NULL DEFAULT 'Medium',
                due_date TEXT,
                completed INTEGER NOT NULL DEFAULT 0,
                created_at TEXT NOT NULL
            );
        """;

        String createIndexSQL = """
            CREATE INDEX IF NOT EXISTS idx_todos_completed ON todos(completed);
        """;

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(createTableSQL);
            stmt.execute(createIndexSQL);
        }
    }

    public static void initializeDatabase() {
        initializeStorage();
    }
}

