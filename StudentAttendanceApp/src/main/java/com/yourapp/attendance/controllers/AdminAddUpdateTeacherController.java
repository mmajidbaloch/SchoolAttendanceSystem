package com.yourapp.attendance.controllers;

import com.yourapp.attendance.models.Teacher;
import com.yourapp.attendance.utils.DatabaseConnection;
import com.yourapp.attendance.utils.PasswordUtils;
import javafx.collections.FXCollections;
import javafx.fxml.*;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.sql.*;
import java.util.List;

public class AdminAddUpdateTeacherController {

    @FXML private TextField teacher_id, teacher_firstname, teacher_lastname,
            teacher_contact, teacher_username, teacher_password;

    @FXML private ComboBox<String> teacher_class, teacher_classId, teacher_gender;

    private AdminTeacherManageController parentController;
    private Teacher teacherToEdit;

    @FXML
    public void initialize() {
        teacher_gender.setItems(FXCollections.observableArrayList("Male", "Female", "Other"));
        loadClassInfo();
    }

    private void loadClassInfo() {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM classes")) {

            List<String> classNames = FXCollections.observableArrayList();
            List<String> classIds = FXCollections.observableArrayList();
            classNames.add("None");
            classIds.add("None");
            while (rs.next()) {
                classNames.add(rs.getString("class_name"));
                classIds.add(rs.getString("class_id"));
            }

            teacher_class.setItems(FXCollections.observableArrayList(classNames));
            teacher_classId.setItems(FXCollections.observableArrayList(classIds));

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void initData(AdminTeacherManageController controller, Teacher teacher) {
        this.parentController = controller;
        this.teacherToEdit = teacher;

        if (teacher != null) {
            teacher_id.setText(teacher.getTeacherId());
            teacher_firstname.setText(teacher.getFirstName());
            teacher_lastname.setText(teacher.getLastName());
            teacher_class.setValue(teacher.getClassName());
            teacher_classId.setValue(teacher.getClassId());
            teacher_gender.setValue(teacher.getGender());
            teacher_contact.setText(teacher.getContact());
            teacher_username.setText(teacher.getUsername());
            teacher_password.setText(teacher.getPassword());

            teacher_id.setDisable(true);
        }
    }

    private boolean validateFields() {
        if (teacher_id.getText().isEmpty() ||
                teacher_firstname.getText().isEmpty() ||
                teacher_lastname.getText().isEmpty() ||
                teacher_class.getValue() == null ||
                teacher_classId.getValue() == null ||
                teacher_gender.getValue() == null ||
                teacher_contact.getText().isEmpty() ||
                teacher_username.getText().isEmpty() ||
                teacher_password.getText().isEmpty()) {

            showAlert("All fields are required. Please fill out all fields.");
            return false;
        }
        return true;
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void teacher_addBtn() {
        if (!validateFields()) {
            return;
        }

        String originalUsername = teacher_username.getText().toLowerCase();
        String classValue = teacher_class.getValue();
        String modifiedUsername = originalUsername + "@emp.ghstaunsa.edu.pk";
        String modifiedId = "00" + teacher_id.getText();
        String hashedPassword = PasswordUtils.hashPassword(teacher_password.getText());

        try (Connection conn = DatabaseConnection.getConnection()) {

            String checkQuery = "SELECT COUNT(*) FROM teachers WHERE username = ?";
            try (PreparedStatement checkStmt = conn.prepareStatement(checkQuery)) {
                checkStmt.setString(1, modifiedUsername);
                ResultSet rs = checkStmt.executeQuery();
                if (rs.next() && rs.getInt(1) > 0) {
                    showAlert("Username already exists! Please choose a different username.");
                    return;
                }
            }

            try (PreparedStatement ps = conn.prepareStatement("INSERT INTO teachers VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                ps.setString(1, modifiedId);
                ps.setString(2, teacher_firstname.getText());
                ps.setString(3, teacher_lastname.getText());
                ps.setString(4, classValue);
                ps.setString(5, teacher_classId.getValue());
                ps.setString(6, teacher_gender.getValue());
                ps.setString(7, teacher_contact.getText());
                ps.setString(8, modifiedUsername);
                ps.setString(9, hashedPassword);

                ps.executeUpdate();
                closeWindow();
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void teacher_updateBtn() {
        if (!validateFields()) {
            return;
        }

        String hashedPassword = PasswordUtils.hashPassword(teacher_password.getText());

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE teachers SET first_name=?, last_name=?, class_name=?, class_id=?, gender=?, contact=?, username=?, password=? WHERE teacher_id=?")) {
            ps.setString(1, teacher_firstname.getText());
            ps.setString(2, teacher_lastname.getText());
            ps.setString(3, teacher_class.getValue());
            ps.setString(4, teacher_classId.getValue());
            ps.setString(5, teacher_gender.getValue());
            ps.setString(6, teacher_contact.getText());
            ps.setString(7, teacher_username.getText());
            ps.setString(8, hashedPassword);
            ps.setString(9, teacher_id.getText());

            ps.executeUpdate();
            closeWindow();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void cancel() {
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) teacher_id.getScene().getWindow();
        stage.close();
    }
}
