package com.yourapp.attendance.controllers;

import com.yourapp.attendance.utils.DatabaseConnection;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.scene.Node;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class AdminAddManageDataOfClassesController {

    @FXML private Label totalStudents101;
    @FXML private Label totalStudents102;
    @FXML private Label totalStudents103;
    @FXML private Label totalStudents104;
    @FXML private Label totalStudents105;
    @FXML private Label totalStudents106;
    @FXML private Label totalStudents107;
    @FXML private Label totalStudents108;
    @FXML private Label totalStudents109;
    @FXML private Label totalStudents110;

    @FXML
    public void initialize() {
        setTotalStudents("101", totalStudents101);
        setTotalStudents("102", totalStudents102);
        setTotalStudents("103", totalStudents103);
        setTotalStudents("104", totalStudents104);
        setTotalStudents("105", totalStudents105);
        setTotalStudents("106", totalStudents106);
        setTotalStudents("107", totalStudents107);
        setTotalStudents("108", totalStudents108);
        setTotalStudents("109", totalStudents109);
        setTotalStudents("110", totalStudents110);
    }

    private void setTotalStudents(String classId, Label label) {
        String query = "SELECT COUNT(*) AS total FROM students WHERE class_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, classId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                label.setText("Total Students: " + rs.getInt("total"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            label.setText("Total Students: Error");
        }
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
                fxmlPath = "/fxml/login.fxml";
                break;
        }

        if (fxmlPath != null) {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Scene newScene = new Scene(loader.load());

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(newScene);
            stage.setTitle("Admin Panel");
            stage.show();
        }
    }

    @FXML
    private void handleClassCardClick(ActionEvent event) {
        Button clickedButton = (Button) event.getSource();
        String buttonId = clickedButton.getId();

        String fxmlToLoad = null;

        switch (buttonId) {
            case "btnClass101":
                fxmlToLoad = "/fxml/adminClass1.fxml";
                break;
            case "btnClass102":
                fxmlToLoad = "/fxml/adminClass2.fxml";
                break;
            case "btnClass103":
                fxmlToLoad = "/fxml/adminClass3.fxml";
                break;
            case "btnClass104":
                fxmlToLoad = "/fxml/adminClass4.fxml";
                break;
            case "btnClass105":
                fxmlToLoad = "/fxml/adminClass5.fxml";
                break;
            case "btnClass106":
                fxmlToLoad = "/fxml/adminClass6.fxml";
                break;
            case "btnClass107":
                fxmlToLoad = "/fxml/adminClass7.fxml";
                break;
            case "btnClass108":
                fxmlToLoad = "/fxml/adminClass8.fxml";
                break;
            case "btnClass109":
                fxmlToLoad = "/fxml/adminClass9.fxml";
                break;
            case "btnClass110":
                fxmlToLoad = "/fxml/adminClass10.fxml";
                break;
        }

        if (fxmlToLoad != null) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlToLoad));
                Scene scene = new Scene(loader.load());
                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                stage.setScene(scene);
                stage.setTitle("Presenz");
                stage.show();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
