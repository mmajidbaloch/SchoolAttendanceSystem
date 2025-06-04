package com.yourapp.attendance.controllers;

import com.yourapp.attendance.utils.DatabaseConnection;
import com.yourapp.attendance.utils.PasswordUtils;
import com.yourapp.attendance.utils.SessionManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.Node;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class studentSettingsController {

    @FXML
    private Label greetingUserMessage;

    @FXML
    private PasswordField oldPassword;

    @FXML
    private PasswordField newPassword;

    @FXML
    private PasswordField confirmPassword;

    @FXML
    private Button updatePasswordBtn;

    @FXML
    private Button studentLogout;

    @FXML
    private Button studentDashboard;

    @FXML
    public void initialize() {
        String username = SessionManager.getLoggedInUser();
        if (username != null) {
            try (Connection conn = DatabaseConnection.getConnection()) {
                String query = "SELECT first_name FROM students WHERE username = ?";
                PreparedStatement stmt = conn.prepareStatement(query);
                stmt.setString(1, username);
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    String firstName = rs.getString("first_name");
                    greetingUserMessage.setText("Hi, " + firstName + "!");
                } else {
                    greetingUserMessage.setText("Welcome!");
                }
            } catch (Exception e) {
                e.printStackTrace();
                greetingUserMessage.setText("Welcome!");
            }
        }
    }

    @FXML
    private void updatePassword(ActionEvent event) {
        String username = SessionManager.getLoggedInUser();
        String oldPass = oldPassword.getText();
        String newPass = newPassword.getText();
        String confirmPass = confirmPassword.getText();

        if (oldPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "All fields are required.");
            return;
        }

        if (!newPass.equals(confirmPass)) {
            showAlert(Alert.AlertType.ERROR, "New passwords do not match.");
            return;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            // Hash the old password to verify
            String hashedOldPass = PasswordUtils.hashPassword(oldPass);

            String checkQuery = "SELECT * FROM students WHERE username = ? AND password = ?";
            PreparedStatement checkStmt = conn.prepareStatement(checkQuery);
            checkStmt.setString(1, username);
            checkStmt.setString(2, hashedOldPass);
            ResultSet rs = checkStmt.executeQuery();

            if (!rs.next()) {
                showAlert(Alert.AlertType.ERROR, "Old password is incorrect.");
                return;
            }

            // Hash the new password before updating
            String hashedNewPass = PasswordUtils.hashPassword(newPass);

            // Update password
            String updateQuery = "UPDATE students SET password = ? WHERE username = ?";
            PreparedStatement updateStmt = conn.prepareStatement(updateQuery);
            updateStmt.setString(1, hashedNewPass);
            updateStmt.setString(2, username);
            int rowsUpdated = updateStmt.executeUpdate();

            if (rowsUpdated > 0) {
                showAlert(Alert.AlertType.INFORMATION, "Password updated successfully.");
                oldPassword.clear();
                newPassword.clear();
                confirmPassword.clear();
            } else {
                showAlert(Alert.AlertType.ERROR, "Password update failed.");
            }

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Database error occurred.");
        }
    }

    private void showAlert(Alert.AlertType type, String message) {
        Alert alert = new Alert(type);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void handleSidebarNavigation(ActionEvent event) {
        Button clicked = (Button) event.getSource();
        String fxmlPath = null;

        switch (clicked.getId()) {
            case "studentDashboard":
                fxmlPath = "/fxml/student_dashboard.fxml"; break;
            case "studentSettings":
                fxmlPath = "/fxml/studentSettings.fxml"; break;
            case "studentLogout":
                SessionManager.clearSession();
                fxmlPath = "/fxml/login.fxml"; break;
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
