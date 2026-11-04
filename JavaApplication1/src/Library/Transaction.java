/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JPanel.java to edit this template
 */
package Library;

/**
 *
 * @author Administrator
 */
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.Color;
import java.awt.Component;
import java.sql.*;
import java.util.Date;
import java.time.Instant;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class Transaction extends javax.swing.JPanel {

    /**
     * Creates new form Transaction
     */
    private class OverdueRenderer extends DefaultTableCellRenderer {
        // Index of the Due Date column (Index 4)
        private static final int DUE_DATE_COLUMN = 4;
        // Index of the Status column (Index 6)
        private static final int STATUS_COLUMN = 6;

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            // Get the default component (label) from the parent
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            
            // Get the Due Date and Status values for the current row
            Object dueDateObj = table.getValueAt(row, DUE_DATE_COLUMN);
            Object statusObj = table.getValueAt(row, STATUS_COLUMN);
            
            String status = statusObj != null ? statusObj.toString() : "";
            
            // Default colors (handles selection logic automatically)
            if (isSelected) {
                // Keep the default selection color set by JTable
            } else {
                c.setBackground(table.getBackground());
                c.setForeground(table.getForeground());
            }

            // Check for Overdue condition
            if ("Borrowed".equalsIgnoreCase(status) || "Overdue".equalsIgnoreCase(status)) {
                
                if (dueDateObj instanceof Timestamp) {
                    Timestamp dueDate = (Timestamp) dueDateObj;
                    // Check if the Due Date is before the current time
                    if (dueDate.before(Timestamp.from(Instant.now()))) {
                        // Apply the red background ONLY if it's overdue and not returned
                        c.setBackground(new Color(255, 153, 153)); // Light Red/Salmon
                        c.setForeground(Color.BLACK);
                    }
                }
            }
            
            return c;
        }
    }
    
    private DefaultTableModel getTableModel() {
        return (DefaultTableModel) transactiontable.getModel();
    }
    public void logOverdueBooks() {
        // Query to select ONLY transactions that are NOT returned AND where the due_date is past NOW()
        String sql = "SELECT t.transaction_id, b.title, r.name AS borrower_name, t.due_date " +
                     "FROM transactions t " +
                     "JOIN books b ON t.book_id = b.book_id " +
                     "JOIN borrowers r ON t.borrower_id = r.borrower_id " +
                     "WHERE t.return_date IS NULL AND t.due_date < NOW()"; // The filter for overdue
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) 
        {
            int overdueCount = 0;
            while (rs.next()) {
                overdueCount++;
                String title = rs.getString("title");
                String borrower = rs.getString("borrower_name");
                Timestamp dueDate = rs.getTimestamp("due_date");
                
                // Log the overdue entry with the "OVERDUE_BOOK" activity type
                ActivityLog.log("OVERDUE_BOOK", 
                    String.format("Book '%s' (Transaction ID: %d) is overdue. Borrower: %s. Due Date: %s.", 
                                  title, rs.getInt("transaction_id"), borrower, dueDate.toString().substring(0, 10)));
            }
            
            // Add a summary log if any overdue books were found
            if (overdueCount > 0) {
                 ActivityLog.log("SYSTEM_ALERT", 
                    String.format("Detected %d overdue book(s) during transaction panel load.", overdueCount));
            }

        } catch (SQLException e) {
            System.err.println("Error checking or logging overdue books: " + e.getMessage());
        }
    }
    
    // Call loadTransactions in the constructor
    public Transaction() {
    initComponents();
    
    for (int i = 0; i < transactiontable.getColumnCount(); i++) {
        transactiontable.getColumnModel().getColumn(i).setCellRenderer(new OverdueRenderer());
    }
    loadTransactions();
    logOverdueBooks();
    
    // --- NEW CODE FOR DYNAMIC SEARCH BEHAVIOR ---
    // 1. Clear placeholder text on focus
    searchtransact.addFocusListener(new java.awt.event.FocusAdapter() {
        @Override
        public void focusGained(java.awt.event.FocusEvent evt) {
            if (searchtransact.getText().equals("Search")) {
                searchtransact.setText("");
            }
        }
        @Override
        public void focusLost(java.awt.event.FocusEvent evt) {
            if (searchtransact.getText().isEmpty()) {
                searchtransact.setText("Search");
            }
        }
    });
    
    // 2. Dynamic search on key press using DocumentListener
    searchtransact.getDocument().addDocumentListener(new DocumentListener() {
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
            // Get text, replace "Search" with empty string if it's the default
            String query = searchtransact.getText();
            if (query.equals("Search")) {
                query = "";
            }
            searchTransactions(query);
        }
    });
    // --- END NEW CODE ---
}
    
    /**
     * Fetches transaction data by joining books, borrowers, and transactions.
     */
    public void loadTransactions() {
        searchTransactions("");
        // SQL query to join necessary tables to get the display columns:
        // Transaction ID, Title, Borrower Name, Borrow Date, Due Date, Return Date, Status
        String sql = "SELECT t.transaction_id, b.title, r.name AS borrower_name, t.borrow_date, t.due_date, t.return_date, " +
                     // CASE statement determines the status for display
                     "CASE " +
                         "WHEN t.return_date IS NOT NULL THEN 'Returned' " +
                         "WHEN t.due_date < NOW() THEN 'Overdue' " +
                         "ELSE 'Borrowed' " +
                     "END AS status " +
                     "FROM transactions t " +
                     "JOIN books b ON t.book_id = b.book_id " +
                     "JOIN borrowers r ON t.borrower_id = r.borrower_id " +
                     "ORDER BY t.transaction_id DESC";
        
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) 
        {
            DefaultTableModel model = getTableModel();
            model.setRowCount(0); // Clear existing data

            while(rs.next()) {
                Object[] row = {
                    rs.getInt("transaction_id"),
                    rs.getString("title"),
                    rs.getString("borrower_name"),
                    rs.getTimestamp("borrow_date"),
                    rs.getTimestamp("due_date"),
                    rs.getTimestamp("return_date"), // This can be null
                    rs.getString("status")
                };
                model.addRow(row);
            }
        } catch(SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading transaction data: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            
        }
    }
    public void searchTransactions(String query) {
    // Prepare the query for SQL LIKE operator (e.g., searches for "h" become "%h%")
    String filter = "%" + query.trim() + "%";
    
    // SQL query to join necessary tables and filter by Transaction ID, Title, or Borrower Name
    String sql = "SELECT t.transaction_id, b.title, r.name AS borrower_name, t.borrow_date, t.due_date, t.return_date, " +
                 "CASE " +
                 "WHEN t.return_date IS NOT NULL THEN 'Returned' " +
                 "WHEN t.due_date < NOW() THEN 'Overdue' " +
                 "ELSE 'Borrowed' " +
                 "END AS status " +
                 "FROM transactions t " +
                 "JOIN books b ON t.book_id = b.book_id " +
                 "JOIN borrowers r ON t.borrower_id = r.borrower_id " +
                 // The main search filter
                 "WHERE CAST(t.transaction_id AS CHAR) LIKE ? OR b.title LIKE ? OR r.name LIKE ? " +
                 "ORDER BY t.transaction_id DESC";
    
    try (Connection conn = DBConnection.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) 
    {
        // Set the three parameters for the WHERE clause filter
        ps.setString(1, filter);
        ps.setString(2, filter);
        ps.setString(3, filter);

        try (ResultSet rs = ps.executeQuery()) {
            DefaultTableModel model = getTableModel();
            model.setRowCount(0); // Clear existing data

            while(rs.next()) {
                Object[] row = {
                    rs.getInt("transaction_id"),
                    rs.getString("title"),
                    rs.getString("borrower_name"),
                    rs.getTimestamp("borrow_date"),
                    rs.getTimestamp("due_date"),
                    rs.getTimestamp("return_date"), // This can be null
                    rs.getString("status")
                };
                model.addRow(row);
            }
        }
    } catch(SQLException e) {
        System.err.println("Error searching transaction data: " + e.getMessage());
    }
}

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        transactiontable = new javax.swing.JTable();
        returnbook = new javax.swing.JButton();
        removetransact = new javax.swing.JButton();
        searchtransact = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();

        jPanel1.setBackground(new java.awt.Color(204, 204, 204));
        jPanel1.setPreferredSize(new java.awt.Dimension(600, 400));

        transactiontable.setBackground(new java.awt.Color(204, 204, 204));
        transactiontable.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        transactiontable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null}
            },
            new String [] {
                "Transaction ID", "Title", "Borrower", "Borrow Date", "Due Date", "Return Date", "Status"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        transactiontable.setGridColor(new java.awt.Color(51, 51, 51));
        transactiontable.setIntercellSpacing(new java.awt.Dimension(1, 1));
        transactiontable.setPreferredSize(new java.awt.Dimension(600, 160));
        transactiontable.setRowHeight(30);
        transactiontable.setSelectionBackground(new java.awt.Color(204, 255, 255));
        transactiontable.setShowGrid(true);
        transactiontable.setSurrendersFocusOnKeystroke(true);
        jScrollPane1.setViewportView(transactiontable);

        returnbook.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        returnbook.setText("RETURN");
        returnbook.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                returnbookActionPerformed(evt);
            }
        });

        removetransact.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        removetransact.setText("REMOVE");
        removetransact.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removetransactActionPerformed(evt);
            }
        });

        searchtransact.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        searchtransact.setText("Search");
        searchtransact.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                searchtransactActionPerformed(evt);
            }
        });

        jLabel1.setFont(new java.awt.Font("Arial", 1, 36)); // NOI18N
        jLabel1.setText("T R A N S A C T I O N S");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(105, 105, 105)
                .addComponent(returnbook, javax.swing.GroupLayout.PREFERRED_SIZE, 89, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(removetransact, javax.swing.GroupLayout.PREFERRED_SIZE, 89, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(117, 117, 117))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jLabel1)
                .addGap(18, 18, 18)
                .addComponent(searchtransact, javax.swing.GroupLayout.PREFERRED_SIZE, 169, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(30, 30, 30))
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 600, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addGap(40, 40, 40)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(searchtransact, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 47, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 8, Short.MAX_VALUE)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 248, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(returnbook)
                    .addComponent(removetransact, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap(16, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );
    }// </editor-fold>//GEN-END:initComponents

    private void removetransactActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removetransactActionPerformed
    int selectedRow = transactiontable.getSelectedRow();
    
    if (selectedRow == -1) {
        JOptionPane.showMessageDialog(this, "Please select a transaction to remove.", "Selection Required", JOptionPane.WARNING_MESSAGE);
        return;
    }

    DefaultTableModel model = (DefaultTableModel) transactiontable.getModel();
    
    // Retrieve necessary data from the table (using the CORRECT indices):
    int transactionId = (int) model.getValueAt(selectedRow, 0); // Index 0: Transaction ID (Integer)
    String bookTitle = (String) model.getValueAt(selectedRow, 1); // Index 1: Book Title (String)
    String borrowerName = (String) model.getValueAt(selectedRow, 2); // Index 2: Borrower Name (String)
    String status = (String) model.getValueAt(selectedRow, 6); // Index 6: Status (String)
    
    // 🛑 NEW CHECK: Only allow removal of transactions that are 'Returned' 🛑
    if (!"Returned".equalsIgnoreCase(status)) {
        JOptionPane.showMessageDialog(this, 
                "Transaction ID " + transactionId + " cannot be deleted. " + 
                "Only transactions with a 'Returned' status can be permanently removed from the record.", 
                "Deletion Blocked", JOptionPane.ERROR_MESSAGE);
        
        // Log the attempted deletion of an active transaction
        ActivityLog.log("DELETE_BLOCKED", 
            String.format("Attempted to delete active Transaction ID: %d (%s) with status: %s.", 
                            transactionId, bookTitle, status));
        return;
    }
    // 🛑 END OF NEW CHECK 🛑
    
    // We must retrieve book_id from the database since it's not in the table model.
    int bookId = -1;
    
    // Safety Confirmation
    int confirmation = JOptionPane.showConfirmDialog(this, 
            "⚠️ WARNING: Are you sure you want to PERMANENTLY DELETE Transaction ID: " + transactionId + " (" + bookTitle + ")?\n" + 
            "The book is already returned and will remain 'Available' in the book inventory.", 
            "Confirm Transaction Deletion", JOptionPane.YES_NO_OPTION);

    if (confirmation == JOptionPane.YES_OPTION) {
        // We only need to delete the transaction; the book status is already 'Available'
        String getBookIdSql = "SELECT book_id FROM transactions WHERE transaction_id=?";
        String deleteTransactionSql = "DELETE FROM transactions WHERE transaction_id=?";
        // NOTE: We DO NOT need to run the UPDATE books query here because 'Returned' status means the book is already 'Available'.
        
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false); // Start transaction block
            
            // A. Get the Book ID first (for logging)
            try (PreparedStatement psGetBookId = conn.prepareStatement(getBookIdSql)) {
                psGetBookId.setInt(1, transactionId);
                try (ResultSet rs = psGetBookId.executeQuery()) {
                    if (rs.next()) {
                        bookId = rs.getInt("book_id");
                    }
                }
            }
            
            if (bookId == -1) {
                // If bookId can't be found, log an error but still attempt to delete the transact.
                System.err.println("Warning: Could not find associated book ID for transaction " + transactionId + " for logging.");
            }

            // B. Delete the Transaction
            try (PreparedStatement psDelete = conn.prepareStatement(deleteTransactionSql)) {
                psDelete.setInt(1, transactionId);
                int rowsAffected = psDelete.executeUpdate();
                if (rowsAffected == 0) {
                    throw new SQLException("Transaction deletion failed, no rows affected.");
                }
            }

            conn.commit(); // Commit changes if all steps succeeded

            // C. Logging
            ActivityLog.log("TRANSACTION_DELETED", 
                String.format("Transaction ID: %d (Book ID: %d, %s) permanently deleted. Status was: %s.", 
                              transactionId, bookId, bookTitle, status));
            
            JOptionPane.showMessageDialog(this, "Transaction ID " + transactionId + " permanently deleted.", "Success", JOptionPane.INFORMATION_MESSAGE);
            loadTransactions(); // Refresh the table
            
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback(); // Rollback on error
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            JOptionPane.showMessageDialog(this, "Database error during transaction removal: " + e.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }
    }//GEN-LAST:event_removetransactActionPerformed

    private void searchtransactActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_searchtransactActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_searchtransactActionPerformed

    private void returnbookActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_returnbookActionPerformed
    int selectedRow = transactiontable.getSelectedRow();
    if (selectedRow == -1) {
        JOptionPane.showMessageDialog(this, "Please select a borrowed book to return.", "Selection Required", JOptionPane.WARNING_MESSAGE);
        return;
    }
    
    DefaultTableModel model = getTableModel();
    
    // Retrieve necessary data from the table (using the CORRECT indices):
    int transactionId = (int) model.getValueAt(selectedRow, 0); // Index 0: Transaction ID (Integer)
    String bookTitle = (String) model.getValueAt(selectedRow, 1); // Index 1: Book Title (String)
    String status = (String) model.getValueAt(selectedRow, 6); // Index 6: Status (String)
    
    // bookId and borrowerId will be retrieved from the database.
    
    if ("Returned".equalsIgnoreCase(status)) {
        JOptionPane.showMessageDialog(this, 
            bookTitle + " is already marked as returned.", 
            "Already Returned", JOptionPane.INFORMATION_MESSAGE);
        return;
    }

    int confirmation = JOptionPane.showConfirmDialog(this, 
            "Confirm return of book: " + bookTitle + "?", 
            "Confirm Return", JOptionPane.YES_NO_OPTION);

    if (confirmation == JOptionPane.YES_OPTION) {
        // Current date and time for return_date
        java.sql.Timestamp currentTimestamp = new java.sql.Timestamp(System.currentTimeMillis());
        
        String updateTransactionSql = "UPDATE transactions SET status='Returned', return_date=? WHERE transaction_id=?";
        // Query to get both IDs needed for the book update and the log
        String getIdsSql = "SELECT book_id, borrower_id FROM transactions WHERE transaction_id=?"; 
        String updateBookSql = "UPDATE books SET status='Available' WHERE book_id=?";
        
        Connection conn = null; 
        
        try {
            conn = DBConnection.getConnection(); 
            conn.setAutoCommit(false); // Start transaction block

            int bookId = -1;
            int borrowerId = -1; 
            
            // 1. Get the book_id and borrower_id from the database
            try(PreparedStatement psGetIds = conn.prepareStatement(getIdsSql)) {
                psGetIds.setInt(1, transactionId);
                try(ResultSet rs = psGetIds.executeQuery()) {
                    if(rs.next()) {
                        bookId = rs.getInt("book_id");
                        borrowerId = rs.getInt("borrower_id");
                    }
                }
            }
            
            if (bookId == -1) {
                throw new SQLException("Could not find associated book ID for transaction " + transactionId);
            }

            // 2. Update the transaction record
            try(PreparedStatement psUpdateTransact = conn.prepareStatement(updateTransactionSql)) {
                psUpdateTransact.setTimestamp(1, currentTimestamp);
                psUpdateTransact.setInt(2, transactionId);
                psUpdateTransact.executeUpdate();
            }

            // 3. Update the book status
            try(PreparedStatement psUpdateBook = conn.prepareStatement(updateBookSql)) {
                psUpdateBook.setInt(1, bookId);
                psUpdateBook.executeUpdate();
            }

            conn.commit(); // Commit all changes if successful
            
            // 🔥 LOGGING: Using the correct, retrieved bookId and borrowerId 🔥
            ActivityLog.log("BOOK_RETURNED", 
                String.format("Book ID: %d (%s) returned successfully by Borrower ID: %d. Transaction ID: %d.", 
                              bookId, bookTitle, borrowerId, transactionId));


            JOptionPane.showMessageDialog(this, 
                bookTitle + " successfully marked as RETURNED. Book status updated to Available.", 
                "Success", JOptionPane.INFORMATION_MESSAGE);
            
            loadTransactions(); // Refresh the table
            
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback(); // Rollback if any part failed
                } catch (SQLException ex) {
                    // Ignore rollback errors
                }
            }
            JOptionPane.showMessageDialog(this, 
                "Error processing book return: " + e.getMessage(), 
                "Database Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true); // Reset auto-commit mode
                    conn.close();
                } catch (SQLException ex) {
                    // Ignore close errors
                }
            }
        }
    }
    }//GEN-LAST:event_returnbookActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JButton removetransact;
    private javax.swing.JButton returnbook;
    private javax.swing.JTextField searchtransact;
    private javax.swing.JTable transactiontable;
    // End of variables declaration//GEN-END:variables
}
