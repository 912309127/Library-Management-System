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
import java.awt.event.FocusListener;
import java.awt.event.FocusEvent;
import javax.swing.table.DefaultTableModel;
import java.sql.*;
import java.awt.Dimension;
import Library.BorrowBook; 
import Library.AddBook; 
import Library.UpdateBook;
import javax.swing.event.DocumentListener; // New dependency
import javax.swing.SwingUtilities;

public class ManageBooks extends javax.swing.JPanel {

    /**
     * Creates new form ManageBooks
     */
    
   // or searchfield, etc.
// Assuming the table model method exists:
   public ManageBooks() { 
    initComponents();
    loadBooks(); // Initial load of all data
    setupSearchFocus(); // Set up the clear-on-focus logic
}
    public void loadBooks() {
    loadBooks(""); 
}
private DefaultTableModel getTableModel() {
    // Uses the generated component name BooksTable
    return (DefaultTableModel) BooksTable.getModel();
}


// --- Database and Utility Methods ---

/**
 * Loads and filters the book data based on the search term.
 * @param searchTerm The title, author, or barcode to search for.
 */
private void archiveBook(int bookId) {
        // First, get the book title for the log
        String titleToLog = "Unknown Book"; 
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement psGetTitle = conn.prepareStatement("SELECT title FROM books WHERE book_id = ?")) {
            
            psGetTitle.setInt(1, bookId);
            try (ResultSet rs = psGetTitle.executeQuery()) {
                if (rs.next()) {
                    titleToLog = rs.getString("title");
                }
            }
        } catch (SQLException e) {
            // Log this internal error, but don't stop the archive attempt
            System.err.println("Error fetching title for logging: " + e.getMessage());
        }


        // SQL to set the IsArchived flag to 1 (True) and update the status
        String sql = "UPDATE books SET IsArchived = 1, status = 'Archived', archived_date = NOW() WHERE book_id = ?";
        
        try (Connection conn = DBConnection.getConnection(); 
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, bookId);
            int affectedRows = pstmt.executeUpdate();
            
            if (affectedRows > 0) {
                JOptionPane.showMessageDialog(this, "Book successfully archived.");
                
                // LOGGING ADDED
                ActivityLog.log("BOOK_ARCHIVED", String.format("Archived Book ID: %d, Title: %s", bookId, titleToLog));
                
                loadBooks(); 
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, 
                "Database Error during archive: " + e.getMessage(), 
                "Error", 
                JOptionPane.ERROR_MESSAGE
            );
        }
    }

public void loadBooks(String searchTerm) {
    // If the search term is the default text, treat it as empty.
    if ("Search".equalsIgnoreCase(searchTerm)) {
        searchTerm = "";
    }
    
    // Base SQL query. CRITICAL FILTER: IsArchived = 0 to only show active books
    String sql = "SELECT book_id, title, author, genre, barcode, status FROM books WHERE IsArchived = 0"; // <--- Only show active books
    
    if (!searchTerm.isEmpty()) {
        // If searching, we need to add the search criteria using AND
        sql += " AND (title LIKE ? OR author LIKE ? OR genre LIKE ? OR barcode LIKE ?)"; // <--- NOTE THE AND
    }
    sql += " ORDER BY book_id DESC";
    
    // The rest of the prepared statement setup must also be adjusted:
    try (Connection conn = DBConnection.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) 
    {
        // ADJUSTED PARAMETER BINDING: Start indexing at 1 if a search term exists
        if (!searchTerm.isEmpty()) {
            String searchPattern = "%" + searchTerm + "%";
            // Start binding at 1 since we have WHERE IsArchived=0
            ps.setString(1, searchPattern); 
            ps.setString(2, searchPattern);
            ps.setString(3, searchPattern);
            ps.setString(4, searchPattern);
        }
        
        try (ResultSet rs = ps.executeQuery()) {
            DefaultTableModel model = getTableModel();
            model.setRowCount(0); // Clear existing data

            while(rs.next()) {
                Object[] row = {
                    rs.getInt("book_id"),
                    rs.getString("title"),
                    rs.getString("author"),
                    rs.getString("genre"),
                    rs.getString("barcode"),
                    rs.getString("status")
                };
                model.addRow(row);
            }
        }
    } catch(SQLException e) {
        JOptionPane.showMessageDialog(this, "Error loading book data: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        e.printStackTrace();
    }
}

// Sets up the focus listener using the CORRECT VARIABLE NAME: SearchBooks
    private void setupSearchFocus() {
    final String defaultSearchText = "Search"; // Match this to your initial text!
    
    // --- Focus Listener (For clearing the default text) ---
    SearchBooks.addFocusListener(new FocusListener() {
        @Override
        public void focusGained(FocusEvent e) {
            if (SearchBooks.getText().equals(defaultSearchText)) {
                SearchBooks.setText("");
            }
        }

        @Override
        public void focusLost(FocusEvent e) {
            if (SearchBooks.getText().isEmpty()) {
                SearchBooks.setText(defaultSearchText);
                loadBooks(); // Re-load all books if search box is cleared
            }
        }
    });

    // --- Document Listener (For real-time search) ---
    SearchBooks.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
        private void search() {
            String keyword = SearchBooks.getText().trim();
            // Do not search if the text is the default placeholder
            if (!defaultSearchText.equalsIgnoreCase(keyword)) {
                // We use a separate thread to prevent UI freezing during fast typing
                SwingUtilities.invokeLater(() -> loadBooks(keyword));
            }
        }

        @Override
        public void insertUpdate(javax.swing.event.DocumentEvent e) {
            search();
        }

        @Override
        public void removeUpdate(javax.swing.event.DocumentEvent e) {
            search();
        }

        @Override
        public void changedUpdate(javax.swing.event.DocumentEvent e) {
            // Not typically used for plain text fields
        }
    });
}
    
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPopupMenu1 = new javax.swing.JPopupMenu();
        manageBooksPanel = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        BooksTable = new javax.swing.JTable();
        addbook = new javax.swing.JButton();
        updatebook = new javax.swing.JButton();
        movetoarchive = new javax.swing.JButton();
        SearchBooks = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();
        borrowbook = new javax.swing.JButton();

        manageBooksPanel.setBackground(new java.awt.Color(204, 204, 204));
        manageBooksPanel.setPreferredSize(new java.awt.Dimension(600, 400));

        BooksTable.setBackground(new java.awt.Color(204, 204, 204));
        BooksTable.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        BooksTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null}
            },
            new String [] {
                "BOOK ID", "TITLE", "AUTHOR", "GENRE", "BARCODE", "STATUS"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        BooksTable.setToolTipText("");
        BooksTable.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        BooksTable.setGridColor(new java.awt.Color(51, 51, 51));
        BooksTable.setIntercellSpacing(new java.awt.Dimension(1, 1));
        BooksTable.setMinimumSize(new java.awt.Dimension(450, 300));
        BooksTable.setPreferredSize(new java.awt.Dimension(600, 300));
        BooksTable.setRowHeight(30);
        BooksTable.setSelectionBackground(new java.awt.Color(204, 255, 255));
        BooksTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        BooksTable.setShowGrid(true);
        BooksTable.setSurrendersFocusOnKeystroke(true);
        jScrollPane1.setViewportView(BooksTable);

        addbook.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        addbook.setText("ADD");
        addbook.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addbookActionPerformed(evt);
            }
        });

        updatebook.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        updatebook.setText("UPDATE");
        updatebook.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                updatebookActionPerformed(evt);
            }
        });

        movetoarchive.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        movetoarchive.setText("ARCHIVE");
        movetoarchive.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                movetoarchiveActionPerformed(evt);
            }
        });

        SearchBooks.setFont(new java.awt.Font("Segoe UI", 3, 12)); // NOI18N
        SearchBooks.setText("Search");
        SearchBooks.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                SearchBooksActionPerformed(evt);
            }
        });

        jLabel1.setFont(new java.awt.Font("Arial", 1, 36)); // NOI18N
        jLabel1.setText("M A N A G E  B O O K S");

        borrowbook.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        borrowbook.setText("BORROW");
        borrowbook.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                borrowbookActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout manageBooksPanelLayout = new javax.swing.GroupLayout(manageBooksPanel);
        manageBooksPanel.setLayout(manageBooksPanelLayout);
        manageBooksPanelLayout.setHorizontalGroup(
            manageBooksPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, manageBooksPanelLayout.createSequentialGroup()
                .addGap(23, 23, 23)
                .addComponent(borrowbook)
                .addGap(79, 79, 79)
                .addComponent(addbook)
                .addGap(72, 72, 72)
                .addComponent(updatebook)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(movetoarchive)
                .addGap(61, 61, 61))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, manageBooksPanelLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(manageBooksPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 588, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(manageBooksPanelLayout.createSequentialGroup()
                        .addGap(19, 19, 19)
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(SearchBooks)))
                .addGap(46, 46, 46))
        );
        manageBooksPanelLayout.setVerticalGroup(
            manageBooksPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, manageBooksPanelLayout.createSequentialGroup()
                .addContainerGap(46, Short.MAX_VALUE)
                .addGroup(manageBooksPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(SearchBooks, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 241, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(manageBooksPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(updatebook)
                    .addComponent(movetoarchive)
                    .addComponent(addbook)
                    .addComponent(borrowbook))
                .addGap(18, 18, 18))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(manageBooksPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(manageBooksPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void borrowbookActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_borrowbookActionPerformed
    int selectedRow = BooksTable.getSelectedRow();
    if (selectedRow == -1) {
        JOptionPane.showMessageDialog(this, "Please select a book to borrow.", "Selection Required", JOptionPane.WARNING_MESSAGE);
        return;
    }

    DefaultTableModel model = getTableModel();
    
    // Get Book Details from the table (ensure column indices are correct)
    int bookId = (int) model.getValueAt(selectedRow, 0); // BOOK ID (Column 0)
    String title = (String) model.getValueAt(selectedRow, 1); // TITLE (Column 1)
    String barcode = (String) model.getValueAt(selectedRow, 4); // BARCODE (Column 4)
    String status = (String) model.getValueAt(selectedRow, 5); // STATUS (Column 5)

    // A book can only be borrowed if its status is 'Available'
    if (!"Available".equalsIgnoreCase(status)) {
        JOptionPane.showMessageDialog(this, 
            title + " cannot be borrowed because its status is: " + status, 
            "Book Not Available", 
            JOptionPane.WARNING_MESSAGE);
        return;
    }

    // Pass the book ID and title to the BorrowBook form
    BorrowBook borrowForm = new BorrowBook(this, bookId, title, barcode);
    borrowForm.setVisible(true);
    borrowForm.setLocationRelativeTo(null);
    }//GEN-LAST:event_borrowbookActionPerformed

    private void movetoarchiveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_movetoarchiveActionPerformed
    int selectedRow = BooksTable.getSelectedRow(); // Use BooksTable
    
    if (selectedRow == -1) {
        JOptionPane.showMessageDialog(this, "Please select a book to archive.");
        return;
    }
    
    // Get the Book ID from the selected row (assuming it's column 0)
    int bookId = (int) BooksTable.getValueAt(selectedRow, 0); 
    
    // Also, check the book's STATUS column (e.g., column 5)
    String status = BooksTable.getValueAt(selectedRow, 5).toString();

    // CRITICAL: Check if the book is currently borrowed/out
    if (status.equalsIgnoreCase("Borrowed")) { 
        JOptionPane.showMessageDialog(this, 
            "Cannot archive: This book is currently borrowed.", 
            "Error", 
            JOptionPane.ERROR_MESSAGE
        );
        return;
    }

    int choice = JOptionPane.showConfirmDialog(this, 
        "Are you sure you want to archive this book?", 
        "Confirm Archive", 
        JOptionPane.YES_NO_OPTION
    );

    if (choice == JOptionPane.YES_OPTION) {
        archiveBook(bookId); // Call the utility method defined above
    }
    }//GEN-LAST:event_movetoarchiveActionPerformed

    private void updatebookActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_updatebookActionPerformed
    int selected = BooksTable.getSelectedRow();
        if(selected == -1) {
            JOptionPane.showMessageDialog(this, "Please select a book first!");
            return;
        }

        DefaultTableModel model = (DefaultTableModel) BooksTable.getModel();
        int bookId = (int) model.getValueAt(selected, 0);
        String title = (String) model.getValueAt(selected, 1);
        String author = (String) model.getValueAt(selected, 2);
        String genre = (String) model.getValueAt(selected, 3);
        String barcode = (String) model.getValueAt(selected, 4);

        
        UpdateBook updateFrame = new UpdateBook(this, bookId, title, author, genre, barcode);
        updateFrame.setVisible(true);
    }//GEN-LAST:event_updatebookActionPerformed

    private void addbookActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addbookActionPerformed
    AddBook addFrame = new AddBook(this);
    addFrame.setVisible(true);
    }//GEN-LAST:event_addbookActionPerformed

    private void SearchBooksActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_SearchBooksActionPerformed
    String keyword = SearchBooks.getText().trim();
    loadBooks(keyword);
    }//GEN-LAST:event_SearchBooksActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTable BooksTable;
    private javax.swing.JTextField SearchBooks;
    private javax.swing.JButton addbook;
    private javax.swing.JButton borrowbook;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPopupMenu jPopupMenu1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JPanel manageBooksPanel;
    private javax.swing.JButton movetoarchive;
    private javax.swing.JButton updatebook;
    // End of variables declaration//GEN-END:variables
}
