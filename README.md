# # Library Management System

### Project Overview
The **Library Management System** is a desktop-based application designed to streamline library administration, record-keeping, and book transaction workflows. Built specifically for librarian operations, the system automates book inventory tracking, borrow/return workflows, barcode validation, and record archiving to maintain strict data integrity.

---

## 🚀 Key Features

* **Duplicate Barcode Prevention:** Automatically validates barcodes upon entry and alerts the librarian if a duplicate code is detected, preventing record conflicts.
* **Safe Archiving System:** Replaces permanent record deletion with an archiving feature to keep historical data intact and accessible.
* **Borrow & Return Management:** Accurately logs borrowing transactions, tracking borrow dates, return dates, and borrower details in real time.
* **Inventory Search & Filtering:** Fast lookup capabilities enabling librarians to query books by title, author, or genre.
* **Comprehensive Reporting:** Generates system reports for active book lists, currently borrowed items, returned books, and archived records.

---

## 🛠️ Technical Stack & Tools

* **Programming Languages:** Java, Python
* **User Interface:** Java Swing / NetBeans GUI Builder
* **Database Management:** MySQL / SQLite
* **Development Environment:** NetBeans IDE, Visual Studio Code, XAMPP

---

## 👥 Project Team & Leadership

### Project Manager & Lead Developer
* **Mr. Christian Sam Bolo** — Leader | Project Manager | Primary Developer

### Project Team Members
* **James Charly B. Antes**
* **Clifford D. Custodio**
* **King Rusian A. Denoyo**
* **Jaryll C. Edmilao**
* **Mark Bryan B. Gupit**
* **Porferio C. Ravidas III**

---

## 📊 Database Design (3rd Normal Form)

### Data Schema Overview

#### 1. `Book` Table
* `book_id` (INT, Primary Key, Auto Increment) — Unique identifier for each book
* `title` (VARCHAR 255, Not Null) — Book title
* `author` (VARCHAR 255, Not Null) — Book author
* `genre` (VARCHAR 100, Not Null) — Category/Genre
* `barcode` (VARCHAR 50, Unique, Not Null) — Unique barcode identifier
* `status` (ENUM: `'available'`, `'borrowed'`, `'archived'`) — Current availability state

#### 2. `Borrower` Table
* `borrower_id` (INT, Primary Key, Auto Increment) — Unique identifier for borrowers
* `name` (VARCHAR 255, Not Null) — Full name of the student/borrower
* `address` (VARCHAR 255) — Borrower address
* `contact` (VARCHAR 100) — Contact number or email address

#### 3. `Transaction` Table
* `transaction_id` (INT, Primary Key, Auto Increment) — Unique transaction ID
* `book_id` (INT, Foreign Key referencing `Book.book_id`) — Target book ID
* `borrower_id` (INT, Foreign Key referencing `Borrower.borrower_id`) — Target borrower ID
* `borrow_date` (DATE, Not Null) — Date book was checked out
* `return_date` (DATE, Nullable) — Date book was returned
* `status` (ENUM: `'borrowed'`, `'returned'`) — Transaction state

---

## 💻 Database Initialization (SQL)

```sql
CREATE TABLE Book (
  book_id INT AUTO_INCREMENT PRIMARY KEY,
  title VARCHAR(255) NOT NULL,
  author VARCHAR(255) NOT NULL,
  genre VARCHAR(100) NOT NULL,
  barcode VARCHAR(50) UNIQUE NOT NULL,
  status ENUM('available', 'borrowed', 'archived') NOT NULL DEFAULT 'available'
);

CREATE TABLE Borrower (
  borrower_id INT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  address VARCHAR(255),
  contact VARCHAR(100)
);

CREATE TABLE Transaction (
  transaction_id INT AUTO_INCREMENT PRIMARY KEY,
  book_id INT NOT NULL,
  borrower_id INT NOT NULL,
  borrow_date DATE NOT NULL,
  return_date DATE DEFAULT NULL,
  status ENUM('borrowed', 'returned') NOT NULL,
  FOREIGN KEY (book_id) REFERENCES Book(book_id),
  FOREIGN KEY (borrower_id) REFERENCES Borrower(borrower_id)
);
