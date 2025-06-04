package com.yourapp.attendance.controllers;

import com.yourapp.attendance.utils.DatabaseConnection;
import com.yourapp.attendance.utils.PasswordUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class ForgotPasswordController {

    @FXML
    private TextField usernameField;
    @FXML
    private TextField nicknameField;
    @FXML
    private TextField petNameField;
    @FXML
    private TextField secretWordField;
    @FXML
    private TextField newPasswordField;
    @FXML
    private TextField confirmPasswordField;
    @FXML
    private Label displayInfo;
    @FXML
    private Button validateButton;
    @FXML
    private Button cancelButton;

    @FXML
    private void onValidateClick() {
        String username = usernameField.getText().trim();
        String nickname = nicknameField.getText().trim();
        String petName = petNameField.getText().trim();
        String secretWord = secretWordField.getText().trim();
        String newPassword = newPasswordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        if (username.isEmpty() || nickname.isEmpty() || petName.isEmpty() || secretWord.isEmpty()
                || newPassword.isEmpty() || confirmPassword.isEmpty()) {
            displayInfo.setText("Please fill all fields.");
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            displayInfo.setText("Passwords do not match.");
            return;
        }

        if (verifyAdminDetails(username, nickname, petName, secretWord)) {
            if (updateAdminPassword(username, newPassword)) {
                displayInfo.setText("Password updated successfully.");
            } else {
                displayInfo.setText("Error updating password.");
            }
        } else {
            displayInfo.setText("Invalid credentials.");
        }
    }

    @FXML
    private void onCancelClick() {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }

    private boolean verifyAdminDetails(String username, String nickname, String petName, String secretWord) {
        String sql = "SELECT * FROM admins WHERE username = ? AND nickname = ? AND petname = ? AND secret_word = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            stmt.setString(2, nickname);
            stmt.setString(3, petName);
            stmt.setString(4, secretWord);

            ResultSet rs = stmt.executeQuery();
            return rs.next();

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean updateAdminPassword(String username, String newPassword) {
        String sql = "UPDATE admins SET password = ? WHERE username = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            // Hash the new password before storing
            String hashedPassword = PasswordUtils.hashPassword(newPassword);

            stmt.setString(1, hashedPassword);
            stmt.setString(2, username);

            return stmt.executeUpdate() > 0;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
