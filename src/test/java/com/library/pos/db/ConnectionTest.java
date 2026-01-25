package com.library.pos.db;

import org.junit.jupiter.api.Test;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConnectionTest {

    @Test
    public void testConnection() {
        String url = "jdbc:mysql://127.0.0.1:3306/library_pos?useSSL=false&allowPublicKeyRetrieval=true";
        String user = "root";
        String password = "";

        System.out.println("--- DIAGNOSTIC START ---");
        System.out.println("Attempting connection to: " + url);
        System.out.println("User: " + user);

        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            throw new RuntimeException("DIAG_SUCCESS: Connected to " + conn.getCatalog());
        } catch (SQLException e) {
            throw new RuntimeException("DIAG_FAILURE: " + e.getMessage());
        }
    }
}
