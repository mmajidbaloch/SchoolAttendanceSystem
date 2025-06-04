package com.yourapp.attendance.controllers;

import com.yourapp.attendance.utils.DatabaseConnection;
import com.yourapp.attendance.utils.SessionManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Button;
import javafx.scene.Node;
import javafx.stage.Stage;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class PrincipalSettingsController {

    @FXML
    private PasswordField oldPassword;

    @FXML
    private PasswordField newPassword;

    @FXML
    private PasswordField confirmPassword;

    @FXML
    private Label greetingUserMessage;

    @FXML
    private Button updatePassword;

    @FXML
    private Button principalDashboard;

    @FXML
    private Button principalLogout;

    @FXML
    public void initialize() {
        String username = SessionManager.getLoggedInUser();
        if (username != null) {
            greetingUserMessage.setText("Welcome Back!");
        }
    }

    @FXML
    private void handleUpdatePassword(ActionEvent event) {
        String username = SessionManager.getLoggedInUser();
        String oldPass = oldPassword.getText();
        String newPass = newPassword.getText();
        String confirmPass = confirmPassword.getText();

        if (oldPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "All fields are required.");
            return;
        }

        if (!newPass.equals(confirmPass)) {
            showAlert(Alert.AlertType.ERROR, "New password and confirm password do not match.");
            return;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            String query = "SELECT password FROM principals WHERE username = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String currentHashedPassword = rs.getString("password");
                String oldPassHashed = hashPassword(oldPass);

                if (!currentHashedPassword.equals(oldPassHashed)) {
                    showAlert(Alert.AlertType.ERROR, "Old password is incorrect.");
                    return;
                }

                String newPassHashed = hashPassword(newPass);

                String updateQuery = "UPDATE principals SET password = ? WHERE username = ?";
                PreparedStatement updateStmt = conn.prepareStatement(updateQuery);
                updateStmt.setString(1, newPassHashed);
                updateStmt.setString(2, username);
                int rows = updateStmt.executeUpdate();

                if (rows > 0) {
                    showAlert(Alert.AlertType.INFORMATION, "Password updated successfully.");
                    oldPassword.clear();
                    newPassword.clear();
                    confirmPassword.clear();
                } else {
                    showAlert(Alert.AlertType.ERROR, "Failed to update password.");
                }
            } else {
                showAlert(Alert.AlertType.ERROR, "User not found.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Database error: " + e.getMessage());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Hashing error: " + e.getMessage());
        }
    }

    private String hashPassword(String password) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hashedBytes = md.digest(password.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : hashedBytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private void showAlert(Alert.AlertType type, String message) {
        Alert alert = new Alert(type);
        alert.setTitle("Alert");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    public void handleSidebarNavigation(ActionEvent event) {
        Button clicked = (Button) event.getSource();
        String fxmlPath = null;

        switch (clicked.getId()) {
            case "principalDashboard":
                fxmlPath = "/fxml/principal_dashboard.fxml";
                break;
            case "teachersRecordInPrincipal":
                fxmlPath = "/fxml/principalTeachersRecord.fxml";
                break;
            case "studentsRecordInPrincipal":
                fxmlPath = "/fxml/principalStudentsRecord.fxml";
                break;
            case "principalSettings":
                fxmlPath = "/fxml/principalSettings.fxml";
                break;
            case "principalLogout":
                SessionManager.clearSession();
                fxmlPath = "/fxml/login.fxml";
                break;
        }

        if (fxmlPath != null) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
                Parent root = loader.load();
                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                stage.setScene(new Scene(root));
                stage.show();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
