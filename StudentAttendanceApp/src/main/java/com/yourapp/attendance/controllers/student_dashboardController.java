package com.yourapp.attendance.controllers;

import com.yourapp.attendance.utils.DatabaseConnection;
import com.yourapp.attendance.utils.SessionManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.fxml.FXMLLoader;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.net.URL;
import java.sql.*;
import java.time.LocalDate;
import java.util.ResourceBundle;

public class student_dashboardController implements Initializable {

    @FXML private Label greetingUserMessage;
    @FXML private Label fetchRelatedStudentId, fetchRelatedStudentName, className;
    @FXML private Label totalConductedClasses, totalAttendClasses, totalAbsentClasses, attendancePercentage;
    @FXML private ProgressBar showpresentPercentage;
    @FXML private DatePicker fromDate, toDate;
    @FXML private TableView<AttendanceRecord> showStudentData;
    @FXML private TableColumn<AttendanceRecord, Integer> srNo;
    @FXML private TableColumn<AttendanceRecord, String> attendanceDate;
    @FXML private TableColumn<AttendanceRecord, String> attendanceStatus;

    private String studentId;
    private String classId;

    private int totalPresentAllTime = 0;
    private int totalAbsentAllTime = 0;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupDatePickers();
        setupTable();

        String username = SessionManager.getLoggedInUser();
        if (username != null) {
            fetchStudentDetails(username);
            calculateAllTimeStats();  // Set attendance percentage & bar only once
        }

        // Date filter listeners only update the table
        fromDate.valueProperty().addListener((obs, oldVal, newVal) -> updateAttendanceTable());
        toDate.valueProperty().addListener((obs, oldVal, newVal) -> updateAttendanceTable());
    }

    private void setupDatePickers() {
        LocalDate minDate = LocalDate.of(2025, 3, 1);
        LocalDate today = LocalDate.now();

        fromDate.setValue(minDate);
        toDate.setValue(today);

        Callback<DatePicker, DateCell> dayCellFactory = dp -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(date.isAfter(today));
            }
        };

        fromDate.setDayCellFactory(dayCellFactory);
        toDate.setDayCellFactory(dayCellFactory);
    }

    private void setupTable() {
        srNo.setCellValueFactory(new PropertyValueFactory<>("srNo"));
        attendanceDate.setCellValueFactory(new PropertyValueFactory<>("date"));
        attendanceStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        // Custom cell factory for status color
        attendanceStatus.setCellFactory(column -> new TableCell<AttendanceRecord, String>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(status);
                    if (status.equalsIgnoreCase("Absent")) {
                        setTextFill(Color.RED);
                        setStyle("-fx-font-weight: bold;");
                    } else {
                        setTextFill(Color.GREEN);
                        setStyle("-fx-font-weight: normal;");
                    }
                }
            }
        });
    }


    private void fetchStudentDetails(String username) {
        String query = "SELECT student_id, first_name, last_name, class_id, class_name FROM students WHERE username = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                studentId = rs.getString("student_id");
                classId = rs.getString("class_id");

                String firstName = rs.getString("first_name");
                String lastName = rs.getString("last_name");

                fetchRelatedStudentName.setText("Name: " + firstName + " " + lastName);
                fetchRelatedStudentId.setText("Id: " + studentId);
                className.setText("Class: " + rs.getString("class_name"));

                greetingUserMessage.setText("Hi, " + firstName + "!");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Called only once to calculate attendance percentage and update progress bar
    private void calculateAllTimeStats() {
        String query = "SELECT status FROM attendance WHERE student_id = ? AND class_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, studentId);
            stmt.setString(2, classId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                String status = rs.getString("status");
                if ("Present".equalsIgnoreCase(status)) totalPresentAllTime++;
                else if ("Absent".equalsIgnoreCase(status)) totalAbsentAllTime++;
            }

            int total = totalPresentAllTime + totalAbsentAllTime;
            double percent = total == 0 ? 0 : (totalPresentAllTime * 100.0) / total;

            totalConductedClasses.setText("Conducted Classes: " + total);
            totalAttendClasses.setText("Total Attend Classes: " + totalPresentAllTime);
            totalAbsentClasses.setText("Total Absent Classes: " + totalAbsentAllTime);
            attendancePercentage.setText(String.format("Attendance Percentage: %.2f%%", percent));

            if (percent < 75) {
                attendancePercentage.setTextFill(Color.RED);
                showpresentPercentage.setStyle("-fx-accent: red;");
            } else if (percent < 80) {
                attendancePercentage.setTextFill(Color.ORANGE);
                showpresentPercentage.setStyle("-fx-accent: orange;");
            } else {
                attendancePercentage.setTextFill(Color.GREEN);
                showpresentPercentage.setStyle("-fx-accent: green;");
            }

            showpresentPercentage.setProgress(percent / 100.0);

            updateAttendanceTable();  // Load table with filtered records
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void updateAttendanceTable() {
        ObservableList<AttendanceRecord> records = FXCollections.observableArrayList();

        LocalDate from = fromDate.getValue();
        LocalDate to = toDate.getValue();

        String query = "SELECT date, status FROM attendance WHERE student_id = ? AND class_id = ? AND date BETWEEN ? AND ? ORDER BY date";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, studentId);
            stmt.setString(2, classId);
            stmt.setDate(3, Date.valueOf(from));
            stmt.setDate(4, Date.valueOf(to));

            ResultSet rs = stmt.executeQuery();
            int sr = 1;

            while (rs.next()) {
                String status = rs.getString("status");
                LocalDate date = rs.getDate("date").toLocalDate();
                records.add(new AttendanceRecord(sr++, date.toString(), status));
            }

            showStudentData.setItems(records);

        } catch (SQLException e) {
            e.printStackTrace();
        }
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

    // Table row data model
    public static class AttendanceRecord {
        private final int srNo;
        private final String date;
        private final String status;

        public AttendanceRecord(int srNo, String date, String status) {
            this.srNo = srNo;
            this.date = date;
            this.status = status;
        }

        public int getSrNo() { return srNo; }
        public String getDate() { return date; }
        public String getStatus() { return status; }
    }
}
