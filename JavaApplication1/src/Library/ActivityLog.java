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
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class ActivityLog {

    private static final String USER_CONTEXT = "Librarian"; 

    /**
     * Logs an activity into the database.
     * @param activityType A constant defining the type of action (e.g., "BOOK_ADDED").
     * @param details A human-readable string describing the specific action.
     */
    public static void log(String activityType, String details) {
        String sql = "INSERT INTO activity_log (timestamp, user_context, activity_type, details) "
                   + "VALUES (NOW(), ?, ?, ?)";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, USER_CONTEXT);
            pstmt.setString(2, activityType);
            pstmt.setString(3, details);
            
            pstmt.executeUpdate();
            
        } catch (RuntimeException | SQLException e) {
            System.err.println("CRITICAL ERROR: Failed to log activity: " + e.getMessage());
        }
    }
}