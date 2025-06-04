package com.yourapp.attendance.controllers;

import com.yourapp.attendance.utils.DatabaseConnection;
import com.yourapp.attendance.utils.PasswordUtils;
import com.yourapp.attendance.utils.SessionManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.Node;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class LoginController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label loginInformation;

    @FXML
    private Hyperlink forgotPassword;

    @FXML
    private void handleLogin(ActionEvent event) {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        if (isValidLogin(username, password)) {
            SessionManager.setLoggedInUser(username);

            String fxmlToLoad;
            if (username.equals("admin")) {
                fxmlToLoad = "/fxml/admin_dashboard.fxml";
            } else if (username.contains("@emp.ghstaunsa.edu.pk")) {
                fxmlToLoad = "/fxml/teacher_dashboard.fxml";
            } else if (username.contains("@head.ghstaunsa.edu.pk")) {
                fxmlToLoad = "/fxml/principal_dashboard.fxml";
            } else if (username.contains("@stud.ghstaunsa.edu.pk")) {
                fxmlToLoad = "/fxml/student_dashboard.fxml";
            } else {
                loginInformation.setText("Invalid login or password");
                return;
            }

            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlToLoad));
                Scene dashboardScene = new Scene(loader.load());
                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                stage.setScene(dashboardScene);
                stage.setTitle("Presenz");
                stage.show();
            } catch (IOException e) {
                e.printStackTrace();
                loginInformation.setText("Error loading dashboard.");
            }

        } else {
            loginInformation.setText("Invalid login or password");
        }
    }

    @FXML
    private void handleForgotPasswordClick() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/forgotPassword.fxml"));
            Parent root = loader.load();
            Stage newWindow = new Stage();
            newWindow.setScene(new Scene(root));
            newWindow.setTitle("Recover Password");
            newWindow.centerOnScreen();
            newWindow.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean isValidLogin(String username, String password) {
        String hashedInputPassword = PasswordUtils.hashPassword(password);

        try (Connection conn = DatabaseConnection.getConnection()) {
            // Admin check
            if (username.equals("admin")) {
                String query = "SELECT password FROM admins WHERE username = ?";
                try (PreparedStatement stmt = conn.prepareStatement(query)) {
                    stmt.setString(1, username);
                    ResultSet rs = stmt.executeQuery();
                    if (rs.next()) {
                        String storedHash = rs.getString("password");
                        return storedHash.equals(hashedInputPassword);
                    }
                }
            }

            // Teacher check
            String teacherQuery = "SELECT password FROM teachers WHERE username = ?";
            try (PreparedStatement stmt = conn.prepareStatement(teacherQuery)) {
                stmt.setString(1, username);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    String storedHash = rs.getString("password");
                    if (storedHash.equals(hashedInputPassword)) {
                        return true;
                    }
                }
            }

            // Principal check
            String principalQuery = "SELECT password FROM principals WHERE username = ?";
            try (PreparedStatement stmt = conn.prepareStatement(principalQuery)) {
                stmt.setString(1, username);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    String storedHash = rs.getString("password");
                    if (storedHash.equals(hashedInputPassword)) {
                        return true;
                    }
                }
            }

            // Student check
            String studentQuery = "SELECT password FROM students WHERE username = ?";
            try (PreparedStatement stmt = conn.prepareStatement(studentQuery)) {
                stmt.setString(1, username);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    String storedHash = rs.getString("password");
                    if (storedHash.equals(hashedInputPassword)) {
                        return true;
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    // for enter press
    @FXML
    private void handleKeyPressed(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER) {
            ActionEvent fakeEvent = new ActionEvent(event.getSource(), null);
            handleLogin(fakeEvent);
        }
    }
}
