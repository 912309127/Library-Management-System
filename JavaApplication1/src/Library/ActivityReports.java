/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JPanel.java to edit this template
 */
package Library;

/**
 *
 * @author Admin
 */
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Stack;
import javax.swing.JOptionPane;
import javax.swing.GroupLayout.Alignment;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class ActivityReports extends javax.swing.JPanel {

    /**
     * Creates new form ActivityReports
     */
    private Stack<String> undoStack;
    private Stack<String> redoStack;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public ActivityReports() {
    initComponents();
    initializeStacks();
    loadAllActivities();

    // --- NEW CODE: Focus and Document Listeners ---
    // 1. Clear placeholder text on focus (optional but recommended)
    searchField.addFocusListener(new java.awt.event.FocusAdapter() {
        @Override
        public void focusGained(java.awt.event.FocusEvent evt) {
            if (searchField.getText().equals("Search")) {
                searchField.setText("");
            }
        }
        @Override
        public void focusLost(java.awt.event.FocusEvent evt) {
            if (searchField.getText().isEmpty()) {
                searchField.setText("Search");
            }
        }
    });

    // 2. Dynamic search on key press using DocumentListener (REQUIRED for dynamic search)
    searchField.getDocument().addDocumentListener(new DocumentListener() {
        public void changedUpdate(DocumentEvent e) {
            searchUpdate();
        }
        public void removeUpdate(DocumentEvent e) {
            searchUpdate();
        }
        public void insertUpdate(DocumentEvent e) {
            searchUpdate();
        }

        private void searchUpdate() {
            String query = searchField.getText();
            if (query.equals("Search")) {
                query = "";
            }
            // Call the new search method
            loadActivities(query);
        }
    });
    // --- END NEW CODE ---
}
    
    private void initializeStacks() {
        undoStack = new Stack<>();
        redoStack = new Stack<>();
    }
    
    public void loadAllActivities() {
        initializeStacks(); 
        logTxtArea.setText("");
        
        loadActivities("");
    }
    
    public void loadActivities(String query) {
    initializeStacks(); // Clear stacks before loading new data
    logTxtArea.setText("");
    
    String filter = "%" + query.trim() + "%";
    
    // Base SQL query
    String sql = "SELECT timestamp, user_context, activity_type, details FROM activity_log ";
    
    // Add WHERE clause for filtering
    if (!query.trim().isEmpty()) {
        sql += "WHERE user_context LIKE ? OR activity_type LIKE ? OR details LIKE ? ";
    }
    
    sql += "ORDER BY log_id DESC";

    try (Connection conn = DBConnection.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {

        // Set parameters only if a query is present
        if (!query.trim().isEmpty()) {
            ps.setString(1, filter);
            ps.setString(2, filter);
            ps.setString(3, filter);
        }

        try (ResultSet rs = ps.executeQuery()) {
            Stack<String> tempDisplayStack = new Stack<>();
            
            while (rs.next()) {
                Timestamp ts = rs.getTimestamp("timestamp");
                String formattedTime = dateFormat.format(ts);
                String user = rs.getString("user_context");
                String type = rs.getString("activity_type");
                String detail = rs.getString("details");

                String logEntry = String.format("[%s] | %s | %s: %s\n", formattedTime, user, type, detail);
                
                undoStack.push(logEntry); // Always push to undo stack
                tempDisplayStack.push(logEntry);
            }
            
            // Rebuild display from the temp stack (in reverse order)
            StringBuilder sb = new StringBuilder();
            while(!tempDisplayStack.isEmpty()){
                sb.append(tempDisplayStack.pop());
            }
            logTxtArea.setText(sb.toString());
            
            updateButtonStates();

        }
    } catch (RuntimeException | SQLException e) {
        // Show an error only if there's no query, otherwise print to console
        if (query.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "Error loading activity logs: " + e.getMessage(), 
                "Database Error", 
                JOptionPane.ERROR_MESSAGE
            );
        } else {
             System.err.println("Error during dynamic log search: " + e.getMessage());
        }
    }
}
    
    private void rebuildDisplay() {
        StringBuilder sb = new StringBuilder();
        Stack<String> tempDisplayStack = new Stack<>();
        tempDisplayStack.addAll(undoStack); 
        
        while (!tempDisplayStack.isEmpty()) {
            sb.append(tempDisplayStack.pop());
        }
        
        logTxtArea.setText(sb.toString());
    }
    
    private void updateButtonStates() {
        undologs.setEnabled(undoStack.size() > 0); 
        redologs.setEnabled(!redoStack.isEmpty());
        
        undologs.setText("UNDO (Hide Last Log)");
        redologs.setText("REDO (Show Next Log)");
    }


    /**
     * Handles the UNDO button click. 
     * Moves the top entry from the undoStack to the redoStack and updates display.
     */                                  

    /**
     * Handles the REDO button click. 
     * Moves the top entry from the redoStack back to the undoStack and updates display.
     */
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        logTxtArea = new javax.swing.JTextArea();
        jLabel1 = new javax.swing.JLabel();
        undologs = new javax.swing.JButton();
        redologs = new javax.swing.JButton();
        searchField = new javax.swing.JTextField();

        setBackground(new java.awt.Color(204, 204, 204));
        setPreferredSize(new java.awt.Dimension(600, 400));

        logTxtArea.setEditable(false);
        logTxtArea.setBackground(new java.awt.Color(204, 204, 204));
        logTxtArea.setColumns(20);
        logTxtArea.setRows(5);
        jScrollPane1.setViewportView(logTxtArea);

        jLabel1.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel1.setText("REPORTS (Activity Log)");

        undologs.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        undologs.setText("UNDO");
        undologs.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                undologsActionPerformed(evt);
            }
        });

        redologs.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        redologs.setText("REDO");
        redologs.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                redologsActionPerformed(evt);
            }
        });

        searchField.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        searchField.setText("Search");
        searchField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                searchFieldActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(85, 85, 85)
                .addComponent(undologs)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 287, Short.MAX_VALUE)
                .addComponent(redologs)
                .addGap(84, 84, 84))
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1)
                .addContainerGap())
            .addGroup(layout.createSequentialGroup()
                .addGap(189, 189, 189)
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(searchField, javax.swing.GroupLayout.PREFERRED_SIZE, 159, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(21, 21, 21))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(13, 13, 13)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(searchField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 298, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 11, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(redologs)
                    .addComponent(undologs))
                .addGap(25, 25, 25))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void undologsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_undologsActionPerformed
    if (!undoStack.isEmpty()) { 
            String lastEntry = undoStack.pop();
            redoStack.push(lastEntry);
            rebuildDisplay();
        }
        updateButtonStates();
    }//GEN-LAST:event_undologsActionPerformed

    private void redologsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_redologsActionPerformed
    if (!redoStack.isEmpty()) {
            String nextEntry = redoStack.pop(); 
            undoStack.push(nextEntry); 
            rebuildDisplay(); 
        }
        updateButtonStates();
    }//GEN-LAST:event_redologsActionPerformed

    private void searchFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_searchFieldActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_searchFieldActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTextArea logTxtArea;
    private javax.swing.JButton redologs;
    private javax.swing.JTextField searchField;
    private javax.swing.JButton undologs;
    // End of variables declaration//GEN-END:variables
}
