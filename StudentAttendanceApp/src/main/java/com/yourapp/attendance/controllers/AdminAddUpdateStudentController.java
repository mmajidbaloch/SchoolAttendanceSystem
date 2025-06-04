package com.yourapp.attendance.controllers;

import com.yourapp.attendance.models.Student;
import com.yourapp.attendance.utils.DatabaseConnection;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.*;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.sql.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;

public class AdminAddUpdateStudentController {

    @FXML private TextField student_id, student_firstname, student_lastname, student_fathername,
            student_username, student_password;
    @FXML private ComboBox<String> student_class, student_classId, student_gender;
    @FXML private ComboBox<Integer> enrolled_year;

    private AdminStudentManageController parentController;
    private Student studentToEdit;

    @FXML
    public void initialize() {
        student_gender.setItems(FXCollections.observableArrayList("Male", "Female", "Other"));
        enrolled_year.setItems(FXCollections.observableArrayList(
                IntStream.rangeClosed(2005, 2025).boxed().collect(Collectors.toList())
        ));

        loadClassInfo();
    }

    private void loadClassInfo() {
        ObservableList<String> classNames = FXCollections.observableArrayList();
        ObservableList<String> classIds = FXCollections.observableArrayList();

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM classes");) {
            while (rs.next()) {
                classNames.add(rs.getString("class_name"));
                classIds.add(rs.getString("class_id"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        student_class.setItems(classNames);
        student_classId.setItems(classIds);
    }

    public void initData(AdminStudentManageController controller, Student student) {
        this.parentController = controller;
        this.studentToEdit = student;

        if (student != null) {
            student_id.setText(student.getStudentId());
            student_firstname.setText(student.getFirstName());
            student_lastname.setText(student.getLastName());
            student_fathername.setText(student.getFatherName());
            student_class.setValue(student.getClassName());
            student_classId.setValue(student.getClassId());
            student_gender.setValue(student.getGender());
            enrolled_year.setValue(student.getEnrolledYear());
            student_username.setText(student.getUsername());
            student_password.setText(student.getPassword());

            student_id.setDisable(true);
        }
    }

    private boolean validateInputs() {
        if (student_id.getText().isEmpty() ||
                student_firstname.getText().isEmpty() ||
                student_lastname.getText().isEmpty() ||
                student_fathername.getText().isEmpty() ||
                student_username.getText().isEmpty() ||
                student_password.getText().isEmpty() ||
                student_class.getValue() == null ||
                student_classId.getValue() == null ||
                student_gender.getValue() == null ||
                enrolled_year.getValue() == null) {

            showAlert(Alert.AlertType.WARNING, "Please fill all fields and select all dropdowns.");
            return false;
        }
        return true;
    }

    private void showAlert(Alert.AlertType alertType, String message) {
        Alert alert = new Alert(alertType, message, ButtonType.OK);
        alert.showAndWait();
    }

    private String hashPassword(String plainPassword) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashedBytes = md.digest(plainPassword.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hashedBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error hashing password", e);
        }
    }

    @FXML
    private void student_addBtn() {
        if (!validateInputs()) {
            return;
        }
        String modifiedUsername = student_username.getText().toLowerCase() + "@stud.ghstaunsa.edu.pk";
        String modifiedId = "00" + student_id.getText();

        try (Connection conn = DatabaseConnection.getConnection()) {
            // Check if username already exists
            String checkQuery = "SELECT COUNT(*) FROM students WHERE username = ?";
            try (PreparedStatement checkStmt = conn.prepareStatement(checkQuery)) {
                checkStmt.setString(1, modifiedUsername);
                ResultSet rs = checkStmt.executeQuery();
                if (rs.next() && rs.getInt(1) > 0) {
                    showAlert(Alert.AlertType.WARNING, "Username already exists. Please choose a different username.");
                    return;
                }
            }

            // If username doesn't exist, proceed to insert
            String insertQuery = "INSERT INTO students (student_id, first_name, last_name, father_name, class_name, class_id, gender, enrolled_year, username, password) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            try (PreparedStatement ps = conn.prepareStatement(insertQuery)) {
                ps.setString(1, modifiedId);
                ps.setString(2, student_firstname.getText());
                ps.setString(3, student_lastname.getText());
                ps.setString(4, student_fathername.getText());
                ps.setString(5, student_class.getValue());
                ps.setString(6, student_classId.getValue());
                ps.setString(7, student_gender.getValue());
                ps.setInt(8, enrolled_year.getValue());
                ps.setString(9, modifiedUsername);
                ps.setString(10, hashPassword(student_password.getText()));

                ps.executeUpdate();
                showAlert(Alert.AlertType.INFORMATION, "Student added successfully!");
                closeWindow();
            }

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Failed to add student: " + e.getMessage());
        }
    }


    @FXML
    private void student_updateBtn() {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE students SET first_name=?, last_name=?, father_name=?, class_name=?, class_id=?, gender=?, enrolled_year=?, username=?, password=? WHERE student_id=?")) {

            ps.setString(1, student_firstname.getText());
            ps.setString(2, student_lastname.getText());
            ps.setString(3, student_fathername.getText());
            ps.setString(4, student_class.getValue());
            ps.setString(5, student_classId.getValue());
            ps.setString(6, student_gender.getValue());
            ps.setInt(7, enrolled_year.getValue());
            ps.setString(8, student_username.getText());
            ps.setString(9, hashPassword(student_password.getText()));
            ps.setString(10, student_id.getText());

            ps.executeUpdate();
            closeWindow();

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Failed to update student: " + e.getMessage());
        }
    }

    @FXML
    private void cancel() {
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) student_id.getScene().getWindow();
        stage.close();
    }
}
