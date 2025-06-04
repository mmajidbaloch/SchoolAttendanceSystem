package com.yourapp.attendance.controllers;

import com.yourapp.attendance.models.Principal;
import com.yourapp.attendance.utils.DatabaseConnection;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;

public class addPrincipalController {

    @FXML private TextField nameField;
    @FXML private TextField usernameField;
    @FXML private TextField passwordField;
    @FXML private Button addButton;
    @FXML private Button updateButton;

    private boolean isUpdateMode = false;

    public void initData(AdminTeacherManageController parentController, Principal existingPrincipal) {
        if (existingPrincipal != null) {
            nameField.setText(existingPrincipal.getName());
            usernameField.setText(existingPrincipal.getUsername());
            passwordField.setText("");

            usernameField.setDisable(false);
            addButton.setVisible(false);
            updateButton.setVisible(true);
            isUpdateMode = true;
        } else {
            addButton.setVisible(true);
            updateButton.setVisible(false);
        }
    }

    @FXML
    private void handleAddPrincipal(ActionEvent event) {
        String name = nameField.getText().trim();
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        if (name.isEmpty() || username.isEmpty() || password.isEmpty()) {
            showAlert("All fields are required.");
            return;
        }

        String hashedPassword = hashPassword(password);

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("INSERT INTO principals (name, username, password) VALUES (?, ?, ?)")) {

            ps.setString(1, name);
            ps.setString(2, username);
            ps.setString(3, hashedPassword);
            ps.executeUpdate();

            showAlert("Principal added successfully.");
            closeWindow(event);

        } catch (SQLIntegrityConstraintViolationException e) {
            showAlert("A principal already exists. Only one allowed.");
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error adding principal.");
        }
    }

    @FXML
    private void handleUpdatePrincipal(ActionEvent event) {
        String name = nameField.getText().trim();
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        if (name.isEmpty() || username.isEmpty() || password.isEmpty()) {
            showAlert("All fields are required.");
            return;
        }

        String hashedPassword = hashPassword(password);

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE principals SET name = ?, username = ?, password = ?")) {

            ps.setString(1, name);
            ps.setString(2, username);
            ps.setString(3, hashedPassword);

            ps.executeUpdate();

            showAlert("Principal updated successfully.");
            closeWindow(event);

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error updating principal.");
        }
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Message");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    private void closeWindow(ActionEvent event) {
        Stage stage = (Stage) ((Button) event.getSource()).getScene().getWindow();
        stage.close();
    }

    // Hash password with SHA-256
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
}
