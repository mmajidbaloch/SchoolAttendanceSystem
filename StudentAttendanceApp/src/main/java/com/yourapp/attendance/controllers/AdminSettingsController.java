package com.yourapp.attendance.controllers;

import com.yourapp.attendance.utils.DatabaseConnection;
import com.yourapp.attendance.utils.SessionManager;
import com.yourapp.attendance.utils.PasswordUtils;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.event.ActionEvent;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class AdminSettingsController {

    @FXML private PasswordField oldPassword;
    @FXML private PasswordField newPassword;
    @FXML private PasswordField confirmPassword;
    @FXML private Button updatePassword;
    @FXML private Label aboutPasswordUpdate;
    @FXML private TextField enterNicknameForMoreSecurity;
    @FXML private TextField enterPetnameForMoreSecurity;
    @FXML private TextField enterSecretWordForMoreSecurity;

    @FXML
    private void initialize() {
        updatePassword.setOnAction(this::handleUpdatePassword);
    }

    private void handleUpdatePassword(ActionEvent event) {
        String oldPass = oldPassword.getText().trim();
        String newPass = newPassword.getText().trim();
        String confirmPass = confirmPassword.getText().trim();

        String nickname = enterNicknameForMoreSecurity.getText().trim();
        String petname = enterPetnameForMoreSecurity.getText().trim();
        String secretWord = enterSecretWordForMoreSecurity.getText().trim();

        if (oldPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty() ||
                nickname.isEmpty() || petname.isEmpty() || secretWord.isEmpty()) {
            aboutPasswordUpdate.setText("⚠ Please fill in all password fields.");
            return;
        }

        if (!newPass.equals(confirmPass)) {
            aboutPasswordUpdate.setText("❌ New passwords do not match.");
            return;
        }

        String currentAdminUsername = SessionManager.getLoggedInUser();

        try (Connection conn = DatabaseConnection.getConnection()) {
            // Hash the old password entered by user
            String hashedOldPass = PasswordUtils.hashPassword(oldPass);

            String validateQuery = "SELECT password FROM admins WHERE username = ?";
            try (PreparedStatement stmt = conn.prepareStatement(validateQuery)) {
                stmt.setString(1, currentAdminUsername);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    String storedHashedPassword = rs.getString("password");
                    if (!storedHashedPassword.equals(hashedOldPass)) {
                        aboutPasswordUpdate.setText("❌ Old password is incorrect.");
                        return;
                    }
                } else {
                    aboutPasswordUpdate.setText("❌ User not found.");
                    return;
                }
            }

            // Hash the new password before storing
            String hashedNewPass = PasswordUtils.hashPassword(newPass);

            String updateQuery = "UPDATE admins SET password = ?, nickname = ?, petname = ?, secret_word = ? WHERE username = ?";
            try (PreparedStatement stmt = conn.prepareStatement(updateQuery)) {
                stmt.setString(1, hashedNewPass);
                stmt.setString(2, nickname);
                stmt.setString(3, petname);
                stmt.setString(4, secretWord);
                stmt.setString(5, currentAdminUsername);
                int rowsAffected = stmt.executeUpdate();
                if (rowsAffected > 0) {
                    aboutPasswordUpdate.setText("✅ Password and recovery info updated!");
                    showAlert(Alert.AlertType.INFORMATION, "Success", "Password and recovery information updated!");
                    oldPassword.clear();
                    newPassword.clear();
                    confirmPassword.clear();
                    enterNicknameForMoreSecurity.clear();
                    enterPetnameForMoreSecurity.clear();
                    enterSecretWordForMoreSecurity.clear();
                } else {
                    aboutPasswordUpdate.setText("❌ Update failed.");
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            aboutPasswordUpdate.setText("❌ Database error occurred.");
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void handleNavigation(ActionEvent event) throws IOException {
        Button clickedButton = (Button) event.getSource();
        String fxmlPath = null;

        switch (clickedButton.getId()) {
            case "adminDashboard":
                fxmlPath = "/fxml/admin_dashboard.fxml";
                break;
            case "adminManageTeachers":
                fxmlPath = "/fxml/adminTeacherManage.fxml";
                break;
            case "adminManageStudents":
                fxmlPath = "/fxml/adminStudentManage.fxml";
                break;
            case "adminAddClasses":
                fxmlPath = "/fxml/adminAddClasses.fxml";
                break;
            case "adminManageClasses":
                fxmlPath = "/fxml/adminAddManageDataOfClasses.fxml";
                break;
            case "adminSetting":
                fxmlPath = "/fxml/adminSettings.fxml";
                break;
            case "adminLogout":
                // Clear session if you want
                SessionManager.clearSession();
                fxmlPath = "/fxml/login.fxml";
                break;
        }

        if (fxmlPath != null) {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Scene newScene = new Scene(loader.load());

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(newScene);
            stage.setTitle("Presenz");
            stage.show();
        }
    }
}
