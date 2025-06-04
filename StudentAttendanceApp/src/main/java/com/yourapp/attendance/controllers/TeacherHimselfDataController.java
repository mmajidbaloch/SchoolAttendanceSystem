package com.yourapp.attendance.controllers;

import com.yourapp.attendance.utils.DatabaseConnection;
import com.yourapp.attendance.utils.SessionManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.sql.*;
import java.time.LocalDate;

public class TeacherHimselfDataController {

    @FXML private Label fetchRelatedTeacherId;
    @FXML private Label fetchRelatedTeacherName;
    @FXML private Label greetingUserMessage;

    @FXML private Label totalConductedClasses;
    @FXML private Label totalAttendClasses;
    @FXML private Label totalAbsentClasses;
    @FXML private Label totalOnLeaveClasses;
    @FXML private Label attendancePercentage;

    @FXML private DatePicker fromDate;
    @FXML private DatePicker toDate;
    @FXML private ProgressBar showpresentPercentage;

    @FXML private TableView<ObservableList<String>> showStudentData;
    @FXML private TableColumn<ObservableList<String>, String> srNo;
    @FXML private TableColumn<ObservableList<String>, String> attendanceDate;
    @FXML private TableColumn<ObservableList<String>, String> attendanceStatus;

    @FXML private Button teacherDashboard;
    @FXML private Button teacherLogout;
    @FXML private Button myDataRecord;
    @FXML private Button studentsRecord;
    @FXML private Button teacherSettings;

    private String loggedInTeacherId;

    @FXML
    public void initialize() {
        String username = SessionManager.getLoggedInUser();
        if (username != null) {
            try (Connection conn = DatabaseConnection.getConnection()) {
                String query = "SELECT first_name FROM teachers WHERE username = ?";
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

        loggedInTeacherId = SessionManager.get("teacherId");
        String fullName = SessionManager.get("teacherFullName");

        fetchRelatedTeacherId.setText("Id: " + loggedInTeacherId);
        fetchRelatedTeacherName.setText("Name: " + fullName);

        fromDate.setValue(LocalDate.of(2025, 3, 1));
        toDate.setValue(LocalDate.now());

        Callback<DatePicker, DateCell> disableFuture = dp -> new DateCell() {
            @Override
            public void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                setDisable(item.isAfter(LocalDate.now()));
            }
        };
        fromDate.setDayCellFactory(disableFuture);
        toDate.setDayCellFactory(disableFuture);

        fromDate.valueProperty().addListener((obs, oldVal, newVal) -> loadTableOnly());
        toDate.valueProperty().addListener((obs, oldVal, newVal) -> loadTableOnly());

        // Setup columns
        srNo.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().get(0)));
        attendanceDate.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().get(1)));
        attendanceStatus.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().get(2)));

        // Highlight "Absent" status in red
        attendanceStatus.setCellFactory(column -> new TableCell<ObservableList<String>, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if (item.equalsIgnoreCase("Absent")) {
                        setTextFill(Color.RED);
                    } else {
                        setTextFill(Color.BLACK);
                    }
                }
            }
        });

        // Load stats and table
        loadFullStats();
        loadTableOnly();
    }

    private void loadTableOnly() {
        ObservableList<ObservableList<String>> data = FXCollections.observableArrayList();

        String sql = "SELECT date, status FROM teacher_attendance WHERE teacher_id = ? AND date BETWEEN ? AND ? ORDER BY date ASC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, loggedInTeacherId);
            stmt.setDate(2, Date.valueOf(fromDate.getValue()));
            stmt.setDate(3, Date.valueOf(toDate.getValue()));

            ResultSet rs = stmt.executeQuery();
            int count = 1;
            while (rs.next()) {
                ObservableList<String> row = FXCollections.observableArrayList();
                row.add(String.valueOf(count++));
                row.add(rs.getString("date"));
                row.add(rs.getString("status"));
                data.add(row);
            }

            showStudentData.setItems(data);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadFullStats() {
        int present = 0, absent = 0, leave = 0, total = 0;

        String sql = "SELECT status FROM teacher_attendance WHERE teacher_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, loggedInTeacherId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                String status = rs.getString("status");
                if ("Present".equalsIgnoreCase(status)) present++;
                else if ("Absent".equalsIgnoreCase(status)) absent++;
                else if ("On Leave".equalsIgnoreCase(status)) leave++;
                total++;
            }

            totalConductedClasses.setText("Conducted Classes: " + total);
            totalAttendClasses.setText("Total Attend Classes: " + present);
            totalAbsentClasses.setText("Total Absent Classes: " + absent);
            totalOnLeaveClasses.setText("On Leave Classes: " + leave);

            int percentage = total > 0 ? (present * 100 / total) : 0;
            attendancePercentage.setText("Attendance Percentage: " + percentage + "%");

            double progress = total == 0 ? 0 : (double) present / total;
            showpresentPercentage.setProgress(progress);

            if (progress < 0.75) {
                showpresentPercentage.setStyle("-fx-accent: red;");
                attendancePercentage.setTextFill(Color.RED);
            } else if (progress < 0.80) {
                showpresentPercentage.setStyle("-fx-accent: yellow;");
                attendancePercentage.setTextFill(Color.ORANGE);
            } else {
                showpresentPercentage.setStyle("-fx-accent: green;");
                attendancePercentage.setTextFill(Color.GREEN);
            }

        } catch (SQLException e) {
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
