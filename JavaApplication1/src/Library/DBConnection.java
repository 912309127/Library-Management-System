/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Library;

/**
 *
 * @author Admin
 */
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import javax.swing.JOptionPane;

public class DBConnection {

    // IMPORTANT: Check your credentials here!
    private static final String URL = "jdbc:mysql://localhost:3306/library_dbs?serverTimezone=UTC";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    /**
     * Establishes and returns a NEW database connection.
     * Throws a RuntimeException on failure.
     */
    public static Connection getConnection() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver"); 
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("MySQL JDBC Driver not found. Check project libraries.", e);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to establish database connection. Check MySQL status.", e);
        }
    }
    
    /**
     * Attempts a connection for application startup verification.
     */
    public static boolean checkConnection() {
        try (Connection conn = getConnection()) {
             System.out.println("✅ Database connection successful!");
             return true;
         } catch (RuntimeException e) { 
             Throwable cause = e.getCause() != null ? e.getCause() : e;
             String detailMessage = cause.getMessage();
             
             JOptionPane.showMessageDialog(null, 
                 "Connection Error: Could not connect to the database.\nDetails: " + detailMessage, 
                 "Database Startup Failure", 
                 JOptionPane.ERROR_MESSAGE);
             
             e.printStackTrace();
             return false;
         } catch (SQLException e) { 
             // Catches the compiler-reported SQLException from the implicit conn.close()
             System.err.println("Database connection succeeded but failed to close cleanly: " + e.getMessage());
             return true; 
         }
    }
}

