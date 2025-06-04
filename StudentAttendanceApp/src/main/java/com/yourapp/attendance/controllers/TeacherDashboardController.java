package com.yourapp.attendance.controllers;

import com.yourapp.attendance.utils.DatabaseConnection;
import com.yourapp.attendance.utils.SessionManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import com.yourapp.attendance.models.StudentRow;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.cell.PropertyValueFactory;


import java.sql.*;

public class TeacherDashboardController {

    @FXML private Label greetingUserMessage;
    @FXML private Label fetchRelatedTeacherId;
    @FXML private Label fetchRelatedTeacherName;
    @FXML private Label fetchAssignedClassName;
    @FXML private Button takeAttendenceOfStudents;
    @FXML private Button teacherLogout;
    @FXML private TableView<StudentRow> showStudentData;
    @FXML private TableColumn<StudentRow, String> fetchRelatedStudIdOfTeacher;
    @FXML private TableColumn<StudentRow, String> fetchRelatedStudFNameOfTeacher;
    @FXML private TableColumn<StudentRow, String> fetchRelatedStudLNameOfTeacher;
    @FXML private TableColumn<StudentRow, String> fetchRelatedStudFatherOfTeacher;
    @FXML private TableColumn<StudentRow, String> fetchRelatedStudGenderOfTeacher;
    @FXML private TableColumn<StudentRow, String> fetchRelatedStudTodayAttend;
    @FXML private TableColumn<StudentRow, Integer> calAttendClassesofRelatedS;
    @FXML private TableColumn<StudentRow, Integer> calLeaveClassesofRelatedS;


    private String teacherUsername;

    @FXML
    public void initialize() {
        teacherUsername = SessionManager.getLoggedInUsername();
        fetchTeacherDetails(teacherUsername);
        loadAssignedClassStudents();
    }

    private void fetchTeacherDetails(String username) {
        String query = "SELECT t.teacher_id, t.first_name, t.last_name, c.class_name, c.class_id " +
                "FROM teachers t " +
                "JOIN class_teacher_assignments ca ON t.teacher_id = ca.teacher_id " +
                "JOIN classes c ON ca.class_id = c.class_id " +
                "WHERE t.username = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String teacherId = rs.getString("teacher_id");
                String firstName = rs.getString("first_name");
                String fullName = rs.getString("first_name") + " " + rs.getString("last_name");
                String className = rs.getString("class_name");

                fetchRelatedTeacherId.setText("Teacher Id: " + teacherId);
                fetchRelatedTeacherName.setText("Teacher Name: " + fullName);
                fetchAssignedClassName.setText("Class: " + className);
                greetingUserMessage.setText("Hi, " + firstName + "!");

                SessionManager.set("teacherId", teacherId);
                SessionManager.set("teacherFullName", fullName);
                SessionManager.set("classId", rs.getString("class_id"));
                SessionManager.set("className", className);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void refreshTable() {
        loadAssignedClassStudents();
    }


    private void loadAssignedClassStudents() {
        String classId = SessionManager.get("classId");
        if (classId == null) return;

        ObservableList<StudentRow> studentList = FXCollections.observableArrayList();

        String query = "SELECT s.student_id, s.first_name, s.last_name, s.father_name, s.gender, " +
                "SUM(CASE WHEN a.status = 'Present' THEN 1 ELSE 0 END) AS present_days, " +
                "SUM(CASE WHEN a.status = 'Absent' THEN 1 ELSE 0 END) AS absent_days, " +
                "MAX(CASE WHEN a.date = CURDATE() THEN a.status ELSE NULL END) AS today_status " +
                "FROM students s " +
                "LEFT JOIN attendance a ON s.student_id = a.student_id " +
                "WHERE s.class_id = ? " +
                "GROUP BY s.student_id";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, classId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                studentList.add(new StudentRow(
                        rs.getString("student_id"),
                        rs.getString("first_name"),
                        rs.getString("last_name"),
                        rs.getString("father_name"),
                        rs.getString("gender"),
                        rs.getString("today_status") != null ? rs.getString("today_status") : "Not Marked",
                        rs.getInt("present_days"),
                        rs.getInt("absent_days")
                ));
            }

            fetchRelatedStudIdOfTeacher.setCellValueFactory(new PropertyValueFactory<>("studentId"));
            fetchRelatedStudFNameOfTeacher.setCellValueFactory(new PropertyValueFactory<>("firstName"));
            fetchRelatedStudLNameOfTeacher.setCellValueFactory(new PropertyValueFactory<>("lastName"));
            fetchRelatedStudFatherOfTeacher.setCellValueFactory(new PropertyValueFactory<>("fatherName"));
            fetchRelatedStudGenderOfTeacher.setCellValueFactory(new PropertyValueFactory<>("gender"));
            fetchRelatedStudTodayAttend.setCellValueFactory(new PropertyValueFactory<>("todayStatus"));
            calAttendClassesofRelatedS.setCellValueFactory(new PropertyValueFactory<>("presentDays"));
            calLeaveClassesofRelatedS.setCellValueFactory(new PropertyValueFactory<>("absentDays"));

            showStudentData.setItems(studentList);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
        private void handleTakeAttendance(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/studentAttendance.fxml"));
            Parent root = loader.load();

            StudentAttendanceController studentAttendanceController = loader.getController();
            studentAttendanceController.setTeacherDashboardController(this);
            // New stage for student attendance window
            Stage newStage = new Stage();
            newStage.setTitle("Mark Student Attendance");
            newStage.setScene(new Scene(root));
            newStage.setResizable(false);
            newStage.initOwner(takeAttendenceOfStudents.getScene().getWindow());

            // 👉 Refresh table when studentAttendance window is closed
            newStage.setOnHidden(e -> loadAssignedClassStudents());

            newStage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @FXML
    private void handleSidebarNavigation(ActionEvent event) {
        Button clicked = (Button) event.getSource();
        String fxmlPath = null;

        switch (clicked.getId()) {
            case "teacherDashboard":
                fxmlPath = "/fxml/teacher_dashboard.fxml";
                break;
            case "myDataRecord":
                fxmlPath = "/fxml/teacherHimselfData.fxml";
                break;
            case "studentsRecord":
                fxmlPath = "/fxml/studentRecords.fxml";
                break;
            case "teacherSettings":
                fxmlPath = "/fxml/teacherSettings.fxml";
                break;
            case "teacherLogout":
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
