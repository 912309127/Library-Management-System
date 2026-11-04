/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Library;

/**
 *
 * @author Student
 */
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.sql.*;
import java.text.SimpleDateFormat;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class Archive extends javax.swing.JPanel {

    /**
     * Creates new form Archive
     */
    public Archive() {
    initComponents();
    loadArchivedBooks();

    // --- NEW CODE FOR DYNAMIC SEARCH BEHAVIOR ---
    // 1. Clear placeholder text on focus
    searcharchivebook.addFocusListener(new java.awt.event.FocusAdapter() {
        @Override
        public void focusGained(java.awt.event.FocusEvent evt) {
            if (searcharchivebook.getText().equals("Search")) {
                searcharchivebook.setText("");
            }
        }
        @Override
        public void focusLost(java.awt.event.FocusEvent evt) {
            if (searcharchivebook.getText().isEmpty()) {
                searcharchivebook.setText("Search");
            }
        }
    });

    // 2. Dynamic search on key press using DocumentListener
    searcharchivebook.getDocument().addDocumentListener(new DocumentListener() {
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
            String query = searcharchivebook.getText();
            if (query.equals("Search")) {
                query = "";
            }
            searchArchivedBooks(query);
        }
    });
    // --- END NEW CODE ---
}
    
    private DefaultTableModel getTableModel() {
        return (DefaultTableModel) archivetable.getModel();
    }

    /**
     * Loads books that have IsArchived = 1 from the database.
     */
    public void loadArchivedBooks() {
    // 1. CRITICAL: Select the new 'archived_date' column
    String sql = "SELECT book_id, title, author, genre, barcode, status, archived_date FROM books WHERE IsArchived = 1";

    DefaultTableModel model = getTableModel();
    model.setRowCount(0); // Clear existing rows
    
    // Date formatter for display
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm"); 

    try (Connection conn = DBConnection.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql);
         ResultSet rs = ps.executeQuery()) {

        while (rs.next()) {
            // Get the Timestamp object directly from the ResultSet
            Timestamp archiveTimestamp = rs.getTimestamp("archived_date");
            String displayDate;
            
            if (archiveTimestamp != null) {
                // Format the timestamp into a readable string
                displayDate = dateFormat.format(archiveTimestamp);
            } else {
                // Fallback for books archived before the column was added/updated
                displayDate = "N/A"; 
            }
            
            Object[] row = {
                rs.getInt("book_id"),
                rs.getString("title"),
                rs.getString("author"),
                rs.getString("genre"),
                rs.getString("barcode"),
                rs.getString("status"),
                // 2. CRITICAL: Use the formatted date string here
                displayDate
            };
            model.addRow(row);
        }
    } catch (SQLException e) {
        JOptionPane.showMessageDialog(this, 
            "Error loading archived books: " + e.getMessage(), 
            "Database Error", 
            JOptionPane.ERROR_MESSAGE
        );
        e.printStackTrace();
    }
}
    
    public void searchArchivedBooks(String query) {
    String filter = "%" + query.trim() + "%";
    
    // Select archived books (IsArchived = 1) and filter by ID, Title, Author, or Barcode
    String sql = "SELECT book_id, title, author, genre, barcode, status, archived_date FROM books WHERE IsArchived = 1 AND (CAST(book_id AS CHAR) LIKE ? OR title LIKE ? OR author LIKE ? OR barcode LIKE ?)";

    DefaultTableModel model = getTableModel();
    model.setRowCount(0); // Clear existing rows
    
    // Date formatter for display
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm"); 

    try (Connection conn = DBConnection.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {

        // Set the four parameters for the WHERE clause filter
        ps.setString(1, filter);
        ps.setString(2, filter);
        ps.setString(3, filter);
        ps.setString(4, filter);

        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                // Get the Timestamp object directly from the ResultSet
                Timestamp archiveTimestamp = rs.getTimestamp("archived_date");
                String displayDate;
                
                if (archiveTimestamp != null) {
                    // Format the timestamp into a readable string
                    displayDate = dateFormat.format(archiveTimestamp);
                } else {
                    // Fallback for books archived before the column was added/updated
                    displayDate = "N/A"; 
                }
                
                Object[] row = {
                    rs.getInt("book_id"),
                    rs.getString("title"),
                    rs.getString("author"),
                    rs.getString("genre"),
                    rs.getString("barcode"),
                    rs.getString("status"),
                    // Use the formatted date string here
                    displayDate
                };
                model.addRow(row);
            }
        }
    } catch (SQLException e) {
        // Only show a dialog on severe errors, avoid on every keystroke
        System.err.println("Error searching archived books: " + e.getMessage());
    }
}
    
    private void restoreBook(int bookId, String title) {
        // SQL: Update IsArchived to 0 and status to Available
        String sql = "UPDATE books SET IsArchived = 0, status = 'Available', archived_date = NULL WHERE book_id = ?"; 
        
        try (Connection conn = DBConnection.getConnection(); 
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, bookId);
            int affectedRows = pstmt.executeUpdate();
            
            if (affectedRows > 0) {
                // Log activity (assuming ActivityLog is available)
                ActivityLog.log("BOOK_RESTORED", "Book ID " + bookId + " ('" + title + "') was restored from archive.");
                
                JOptionPane.showMessageDialog(this, "Book successfully restored to main list.");
                
                // ⭐ THE CRITICAL FIX IS HERE ⭐
                // After successful update, reload the data to refresh the table immediately.
                loadArchivedBooks(); 
            }
        } catch (RuntimeException | SQLException e) {
            JOptionPane.showMessageDialog(this, 
                "Error restoring book: " + e.getMessage(), 
                "Database Error", 
                JOptionPane.ERROR_MESSAGE
            );
            e.printStackTrace();
        }
    }
    
    private void deleteBookPermanently(int bookId, String title) {
        String sql = "DELETE FROM books WHERE book_id = ?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, bookId);
            int affectedRows = pstmt.executeUpdate();
            
            if (affectedRows > 0) {
                // ---> ACTIVITY LOGGER CALL
                ActivityLog.log("BOOK_DELETED_PERMANENTLY", "Book ID " + bookId + " ('" + title + "') was permanently deleted from the archive.");
                // <---
                
                JOptionPane.showMessageDialog(this, "Book permanently deleted from the database.", "Success", JOptionPane.INFORMATION_MESSAGE);
                // No table reload here! The action listener handles the UI update.
            }
        } catch (RuntimeException | SQLException e) {
            JOptionPane.showMessageDialog(this, 
                "Error deleting book permanently: " + e.getMessage(), 
                "Database Error", 
                JOptionPane.ERROR_MESSAGE
            );
            e.printStackTrace();
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
        archivetable = new javax.swing.JTable();
        restorebook = new javax.swing.JButton();
        deletebookarch = new javax.swing.JButton();
        searcharchivebook = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();

        jPanel1.setBackground(new java.awt.Color(204, 204, 204));
        jPanel1.setPreferredSize(new java.awt.Dimension(600, 400));

        archivetable.setBackground(new java.awt.Color(204, 204, 204));
        archivetable.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        archivetable.setModel(new javax.swing.table.DefaultTableModel(
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
                "Book ID", "Title", "Author", "Genre", "Barcode", "Status", "Date Archive"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        archivetable.setGridColor(new java.awt.Color(0, 0, 0));
        archivetable.setIntercellSpacing(new java.awt.Dimension(1, 1));
        archivetable.setRowHeight(30);
        archivetable.setShowGrid(true);
        archivetable.setSurrendersFocusOnKeystroke(true);
        jScrollPane1.setViewportView(archivetable);

        restorebook.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        restorebook.setText("RESTORE");
        restorebook.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                restorebookActionPerformed(evt);
            }
        });

        deletebookarch.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        deletebookarch.setText("DELETE");
        deletebookarch.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deletebookarchActionPerformed(evt);
            }
        });

        searcharchivebook.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        searcharchivebook.setText("Search");
        searcharchivebook.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                searcharchivebookActionPerformed(evt);
            }
        });

        jLabel1.setFont(new java.awt.Font("Arial", 1, 36)); // NOI18N
        jLabel1.setText("A R C H I V E");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(108, 108, 108)
                .addComponent(restorebook, javax.swing.GroupLayout.PREFERRED_SIZE, 119, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 141, Short.MAX_VALUE)
                .addComponent(deletebookarch, javax.swing.GroupLayout.PREFERRED_SIZE, 129, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(103, 103, 103))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addGap(80, 80, 80)
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(searcharchivebook, javax.swing.GroupLayout.PREFERRED_SIZE, 160, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(41, 41, 41))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap(33, Short.MAX_VALUE)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(searcharchivebook, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 282, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(deletebookarch)
                    .addComponent(restorebook))
                .addGap(8, 8, 8))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );
    }// </editor-fold>//GEN-END:initComponents

    private void restorebookActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_restorebookActionPerformed
    int selectedRow = archivetable.getSelectedRow();
    if (selectedRow == -1) {
        JOptionPane.showMessageDialog(this, "Please select a book to restore.", "Selection Required", JOptionPane.WARNING_MESSAGE);
        return;
    }

    // 1. EXTRACT bookId (Column 0)
    int bookId = (int) archivetable.getValueAt(selectedRow, 0); 
    
    // 2. EXTRACT title (Column 1)
    String title = (String) archivetable.getValueAt(selectedRow, 1);
    
    int choice = JOptionPane.showConfirmDialog(this, 
        "Are you sure you want to restore the book: " + title + "?", 
        "Confirm Restore", 
        JOptionPane.YES_NO_OPTION
    );

    if (choice == JOptionPane.YES_OPTION) {
        // 3. CORRECT CALL: Pass both arguments
        restoreBook(bookId, title); 
    }
    }//GEN-LAST:event_restorebookActionPerformed

    private void deletebookarchActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deletebookarchActionPerformed
    int selectedRow = archivetable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a book to permanently delete.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // Get the Book ID and Title from the selected row (assuming ID is column 0 and Title is column 1)
        int bookId = (int) archivetable.getValueAt(selectedRow, 0);
        String title = (String) archivetable.getValueAt(selectedRow, 1);
        
        // Confirmation dialog for permanent delete
        int choice = JOptionPane.showConfirmDialog(this, 
            "WARNING: This will permanently delete the book '" + title + "' from the database.\nAre you sure you want to proceed?", 
            "Confirm Permanent Delete", 
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE // Use a warning icon for emphasis
        );
        
        if (choice == JOptionPane.YES_OPTION) {
            // 1. Execute the database deletion
            deleteBookPermanently(bookId, title);
            
            // 2. CRITICAL: Get the table model and remove the row from the UI
            DefaultTableModel model = (DefaultTableModel) archivetable.getModel();
            
            // 3. Check if the index is still valid (best practice)
            if (selectedRow < model.getRowCount() && selectedRow != -1) {
                // Remove the row from the visual table immediately for responsiveness
                model.removeRow(selectedRow); 
            }
        }
    }//GEN-LAST:event_deletebookarchActionPerformed

    private void searcharchivebookActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_searcharchivebookActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_searcharchivebookActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTable archivetable;
    private javax.swing.JButton deletebookarch;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JButton restorebook;
    private javax.swing.JTextField searcharchivebook;
    // End of variables declaration//GEN-END:variables
}
