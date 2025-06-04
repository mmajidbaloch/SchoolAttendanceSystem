package com.yourapp.attendance.controllers;

import com.yourapp.attendance.models.TeacherRow;
import com.yourapp.attendance.utils.DatabaseConnection;
import com.yourapp.attendance.utils.SessionManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.*;

public class PrincipalDashboardController {

    @FXML private Label fetchPrincipalName;
    @FXML private Button takeAttendanceOfTeachers;
    @FXML private Button principalLogout;
    @FXML private Label greetingUserMessage;
    @FXML private Button principalDashboard;
    @FXML private Button teachersRecordInPrincipal;
    @FXML private Button studentsRecordInPrincipal;
    @FXML private Button principalSettings;
    @FXML private Label fetchTotalClasses;

    @FXML private TableView<TeacherRow> teacherDataShowTable;
    @FXML private TableColumn<TeacherRow, String> fetchTeachersId;
    @FXML private TableColumn<TeacherRow, String> fetchRelatedTeachersFName;
    @FXML private TableColumn<TeacherRow, String> fetchRelatedTeachersLName;
    @FXML private TableColumn<TeacherRow, String> fetchRelatedTeachersGender;
    @FXML private TableColumn<TeacherRow, String> fetchRelatedTeachersClassAssigned;
    @FXML private TableColumn<TeacherRow, String> fetchRelatedTeachersTodayAttend;
    @FXML private TableColumn<TeacherRow, Integer> calculateAttendClassesTeacher;
    @FXML private TableColumn<TeacherRow, Integer> calculateLeavedClassesTeacher;
    @FXML private TableColumn<TeacherRow, Integer> calculateOnLeavedClasses;
    @FXML private TableColumn<TeacherRow, String> calculatePresentPercentage;

    private String principalUsername;

    @FXML
    public void initialize() {
        principalUsername = SessionManager.getLoggedInUsername();
        fetchPrincipalDetails(principalUsername);
        loadTeacherAttendanceData();
        fetchTotalClassCount();
    }

    private void fetchTotalClassCount() {
        String query = "SELECT COUNT(DISTINCT date) AS total_classes FROM teacher_attendance";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                fetchTotalClasses.setText("Total Classes: " + rs.getInt("total_classes"));
            }

        } catch (SQLException e) {
            fetchTotalClasses.setText("Total Classes: N/A");
            e.printStackTrace();
        }
    }

    private void fetchPrincipalDetails(String username) {
        String query = "SELECT name FROM principals WHERE username = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String principalFullName = rs.getString("name");
                fetchPrincipalName.setText("Principal Name: " + principalFullName);
                SessionManager.set("principalFullName", principalFullName);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void refreshTable() {
        loadTeacherAttendanceData();
    }

    private void loadTeacherAttendanceData() {
        ObservableList<TeacherRow> teacherList = FXCollections.observableArrayList();

        String query = "SELECT t.teacher_id, t.first_name, t.last_name, t.gender, c.class_name, " +
                "SUM(CASE WHEN ta.status = 'Present' THEN 1 ELSE 0 END) AS present_days, " +
                "SUM(CASE WHEN ta.status = 'Absent' THEN 1 ELSE 0 END) AS absent_days, " +
                "SUM(CASE WHEN ta.status = 'On Leave' THEN 1 ELSE 0 END) AS leave_days, " +
                "MAX(CASE WHEN ta.date = CURDATE() THEN ta.status ELSE NULL END) AS today_status, " +
                "ROUND(CASE WHEN COUNT(ta.date) > 0 " +
                "THEN (SUM(CASE WHEN ta.status = 'Present' THEN 1 ELSE 0 END) * 100.0) / COUNT(ta.date) ELSE 0 END, 2) AS attendance_percentage " +
                "FROM teachers t " +
                "LEFT JOIN class_teacher_assignments ca ON t.teacher_id = ca.teacher_id " +
                "LEFT JOIN classes c ON ca.class_id = c.class_id " +
                "LEFT JOIN teacher_attendance ta ON t.teacher_id = ta.teacher_id " +
                "GROUP BY t.teacher_id, c.class_name, t.first_name, t.last_name, t.gender";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                teacherList.add(new TeacherRow(
                        rs.getString("teacher_id"),
                        rs.getString("first_name"),
                        rs.getString("last_name"),
                        rs.getString("gender"),
                        rs.getString("class_name") != null ? rs.getString("class_name") : "Not Assigned",
                        rs.getString("today_status") != null ? rs.getString("today_status") : "Not Marked",
                        rs.getInt("present_days"),
                        rs.getInt("absent_days"),
                        rs.getInt("leave_days"),
                        rs.getString("attendance_percentage") + "%"
                ));
            }

            fetchTeachersId.setCellValueFactory(new PropertyValueFactory<>("teacherId"));
            fetchRelatedTeachersFName.setCellValueFactory(new PropertyValueFactory<>("firstName"));
            fetchRelatedTeachersLName.setCellValueFactory(new PropertyValueFactory<>("lastName"));
            fetchRelatedTeachersGender.setCellValueFactory(new PropertyValueFactory<>("gender"));
            fetchRelatedTeachersClassAssigned.setCellValueFactory(new PropertyValueFactory<>("className"));
            fetchRelatedTeachersTodayAttend.setCellValueFactory(new PropertyValueFactory<>("todayStatus"));
            calculateAttendClassesTeacher.setCellValueFactory(new PropertyValueFactory<>("presentDays"));
            calculateLeavedClassesTeacher.setCellValueFactory(new PropertyValueFactory<>("absentDays"));
            calculateOnLeavedClasses.setCellValueFactory(new PropertyValueFactory<>("leaveDays"));
            calculatePresentPercentage.setCellValueFactory(new PropertyValueFactory<>("attendancePercentage"));

            teacherDataShowTable.setItems(teacherList);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleTakeAttendance(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/teacherAttendance.fxml"));
            Parent root = loader.load();

            TeacherAttendanceController teacherAttendanceController = loader.getController();
            teacherAttendanceController.setPrincipalDashboardController(this);
            Stage newStage = new Stage();
            newStage.setTitle("Mark Teacher Attendance");
            newStage.setScene(new Scene(root));
            newStage.setResizable(false);
            newStage.initOwner(takeAttendanceOfTeachers.getScene().getWindow());
            newStage.setOnHidden(e -> loadTeacherAttendanceData());
            newStage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
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
