package com.yourapp.attendance.controllers;

import com.yourapp.attendance.utils.DatabaseConnection;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.event.ActionEvent;
import javafx.stage.Stage;
import javafx.scene.Node;

import java.io.IOException;
import java.sql.*;
import java.time.Month;
import java.util.HashMap;
import java.util.Map;

public class AdminDashboardController {

    @FXML private Label totalUserCount;
    @FXML private Label totalStudentsCount;
    @FXML private Label totalTeacherCount;

    @FXML private LineChart<String, Number> studentDataLineGraph;
    @FXML private LineChart<String, Number> teachersDataLineGraph1;

    @FXML
    public void initialize() {
        loadCounts();
        loadStudentLineChart();
        loadTeacherLineChart();
    }

    private void loadCounts() {
        int studentCount = getCountFromTable("students");
        int teacherCount = getCountFromTable("teachers");
        int totalUsers = studentCount + teacherCount;

        totalStudentsCount.setText(String.valueOf(studentCount));
        totalTeacherCount.setText(String.valueOf(teacherCount));
        totalUserCount.setText(String.valueOf(totalUsers));
    }

    private int getCountFromTable(String tableName) {
        String query = "SELECT COUNT(*) FROM " + tableName;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                return rs.getInt(1);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private void loadStudentLineChart() {
        XYChart.Series<String, Number> dataSeries = new XYChart.Series<>();
        dataSeries.setName("Student Attendance %");

        Map<Integer, double[]> monthlyAttendance = new HashMap<>();

        String query = "SELECT MONTH(date) AS month, " +
                "SUM(CASE WHEN status = 'Present' THEN 1 ELSE 0 END) AS presents, " +
                "COUNT(*) AS total " +
                "FROM attendance " +
                "GROUP BY MONTH(date)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                int month = rs.getInt("month");
                int present = rs.getInt("presents");
                int total = rs.getInt("total");

                double percentage = total > 0 ? ((double) present / total) * 100 : 0;
                monthlyAttendance.put(month, new double[]{percentage});
            }

            for (int month = 1; month <= 12; month++) {
                double[] data = monthlyAttendance.getOrDefault(month, new double[]{0});
                dataSeries.getData().add(new XYChart.Data<>(Month.of(month).name().substring(0, 3), data[0]));
            }

            studentDataLineGraph.getData().add(dataSeries);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadTeacherLineChart() {
        XYChart.Series<String, Number> dataSeries = new XYChart.Series<>();
        dataSeries.setName("Employees Attendance %");

        Map<Integer, Integer> monthlyTeacherAttendance = new HashMap<>();

        String query = "SELECT MONTH(date) AS month, COUNT(DISTINCT taken_by, date) AS days_taken " +
                "FROM attendance " +
                "GROUP BY MONTH(date)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                int month = rs.getInt("month");
                int daysTaken = rs.getInt("days_taken");

                monthlyTeacherAttendance.put(month, daysTaken);
            }

            for (int month = 1; month <= 12; month++) {
                int daysPresent = monthlyTeacherAttendance.getOrDefault(month, 0);
                // Assume each month has around 22 working days (simplification)
                double percentage = (daysPresent / 22.0) * 100;

                dataSeries.getData().add(new XYChart.Data<>(Month.of(month).name().substring(0, 3), percentage));
            }

            teachersDataLineGraph1.getData().add(dataSeries);

        } catch (SQLException e) {
            e.printStackTrace();
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
            stage.setTitle("Presenz");
            stage.show();
        }
    }
}
