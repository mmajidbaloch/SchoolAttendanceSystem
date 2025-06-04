package com.yourapp.attendance.controllers;

import com.yourapp.attendance.utils.DatabaseConnection;
import com.yourapp.attendance.utils.SessionManager;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
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
import javafx.stage.Stage;
import javafx.util.Callback;

import java.sql.*;
import java.time.LocalDate;

public class studentRecordController {

    @FXML private Label greetingUserMessage;
    @FXML private Label fetchRelatedTeacherId;
    @FXML private Label fetchRelatedTeacherName;
    @FXML private Label fetchClassName;

    @FXML private DatePicker fromDate;
    @FXML private DatePicker toDate;

    @FXML private TableView<StudentAttendanceSummary> showStudentData;
    @FXML private TableColumn<StudentAttendanceSummary, String> srNo;
    @FXML private TableColumn<StudentAttendanceSummary, String> studentFirstName;
    @FXML private TableColumn<StudentAttendanceSummary, String> studentLastName;
    @FXML private TableColumn<StudentAttendanceSummary, Integer> totalAttendDays;
    @FXML private TableColumn<StudentAttendanceSummary, Integer> totalAbsentDays;
    @FXML private TableColumn<StudentAttendanceSummary, Integer> totalClassesOverall;
    @FXML private TableColumn<StudentAttendanceSummary, String> presentPercentage;
    @FXML private TableColumn<StudentAttendanceSummary, Double> progressBar;

    private String classId;

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

        setupDatePickers();
        loadTeacherDetails();
        loadStudentData(fromDate.getValue(), toDate.getValue());

        fromDate.valueProperty().addListener((obs, oldVal, newVal) -> loadStudentData(newVal, toDate.getValue()));
        toDate.valueProperty().addListener((obs, oldVal, newVal) -> loadStudentData(fromDate.getValue(), newVal));
    }

    private void setupDatePickers() {
        LocalDate today = LocalDate.now();
        LocalDate marchStart = LocalDate.of(2025, 3, 1);

        fromDate.setValue(marchStart);
        toDate.setValue(today);

        Callback<DatePicker, DateCell> disableFutureDates = dp -> new DateCell() {
            @Override
            public void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                setDisable(empty || item.isAfter(LocalDate.now()));
            }
        };

        fromDate.setDayCellFactory(disableFutureDates);
        toDate.setDayCellFactory(disableFutureDates);
    }

    private void loadTeacherDetails() {
        String username = SessionManager.getLoggedInUsername();
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
                String fullName = rs.getString("first_name") + " " + rs.getString("last_name");
                classId = rs.getString("class_id");

                fetchRelatedTeacherId.setText("Id: " + teacherId);
                fetchRelatedTeacherName.setText("Name: " + fullName);
                fetchClassName.setText("Class: " + rs.getString("class_name"));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadStudentData(LocalDate from, LocalDate to) {
        if (classId == null) return;

        ObservableList<StudentAttendanceSummary> data = FXCollections.observableArrayList();

        StringBuilder query = new StringBuilder(
                "SELECT s.first_name, s.last_name, " +
                        "SUM(CASE WHEN a.status = 'Present' THEN 1 ELSE 0 END) AS present_days, " +
                        "SUM(CASE WHEN a.status = 'Absent' THEN 1 ELSE 0 END) AS absent_days " +
                        "FROM students s " +
                        "LEFT JOIN attendance a ON s.student_id = a.student_id AND s.class_id = a.class_id " +
                        "WHERE s.class_id = ? "
        );

        if (from != null) query.append("AND a.date >= ? ");
        if (to != null) query.append("AND a.date <= ? ");
        query.append("GROUP BY s.student_id");

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query.toString())) {

            int paramIndex = 1;
            stmt.setString(paramIndex++, classId);
            if (from != null) stmt.setDate(paramIndex++, Date.valueOf(from));
            if (to != null) stmt.setDate(paramIndex, Date.valueOf(to));

            ResultSet rs = stmt.executeQuery();
            int serial = 1;

            while (rs.next()) {
                int present = rs.getInt("present_days");
                int absent = rs.getInt("absent_days");
                int total = present + absent;
                double percent = total > 0 ? (double) present / total : 0;

                data.add(new StudentAttendanceSummary(
                        String.valueOf(serial++),
                        rs.getString("first_name"),
                        rs.getString("last_name"),
                        present,
                        absent,
                        total,
                        String.format("%.2f%%", percent * 100),
                        percent
                ));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        srNo.setCellValueFactory(new PropertyValueFactory<>("srNo"));
        studentFirstName.setCellValueFactory(new PropertyValueFactory<>("firstName"));
        studentLastName.setCellValueFactory(new PropertyValueFactory<>("lastName"));
        totalAttendDays.setCellValueFactory(new PropertyValueFactory<>("attendDays"));
        totalAbsentDays.setCellValueFactory(new PropertyValueFactory<>("absentDays"));
        totalClassesOverall.setCellValueFactory(new PropertyValueFactory<>("totalDays"));

        // Color-coded percentage column
        presentPercentage.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String percentText, boolean empty) {
                super.updateItem(percentText, empty);
                if (empty || percentText == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(percentText);
                    double percent = Double.parseDouble(percentText.replace("%", ""));
                    if (percent < 75.0) {
                        setTextFill(javafx.scene.paint.Color.RED);
                    } else if (percent < 80.0) {
                        setTextFill(javafx.scene.paint.Color.ORANGE);
                    } else {
                        setTextFill(javafx.scene.paint.Color.GREEN);
                    }
                }
            }
        });

        // Color-coded progress bar
        progressBar.setCellFactory(column -> new TableCell<>() {
            private final ProgressBar bar = new ProgressBar();

            @Override
            protected void updateItem(Double progress, boolean empty) {
                super.updateItem(progress, empty);
                if (empty || progress == null) {
                    setGraphic(null);
                } else {
                    bar.setProgress(progress);
                    if (progress < 0.75) {
                        bar.setStyle("-fx-accent: red;");
                    } else if (progress < 0.80) {
                        bar.setStyle("-fx-accent: orange;");
                    } else {
                        bar.setStyle("-fx-accent: green;");
                    }
                    setGraphic(bar);
                }
            }
        });

        presentPercentage.setCellValueFactory(new PropertyValueFactory<>("percentText"));
        progressBar.setCellValueFactory(new PropertyValueFactory<>("progressValue"));
        showStudentData.setItems(data);
    }

    @FXML
    private void handleSidebarNavigation(ActionEvent event) {
        Button clicked = (Button) event.getSource();
        String fxmlPath = null;

        switch (clicked.getId()) {
            case "teacherDashboard":
                fxmlPath = "/fxml/teacher_dashboard.fxml"; break;
            case "myDataRecord":
                fxmlPath = "/fxml/teacherHimselfData.fxml"; break;
            case "studentsRecord":
                fxmlPath = "/fxml/studentRecords.fxml"; break;
            case "teacherSettings":
                fxmlPath = "/fxml/teacherSettings.fxml"; break;
            case "teacherLogout":
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

    // Inner helper class for table rows
    public static class StudentAttendanceSummary {
        private final SimpleStringProperty srNo;
        private final SimpleStringProperty firstName;
        private final SimpleStringProperty lastName;
        private final SimpleIntegerProperty attendDays;
        private final SimpleIntegerProperty absentDays;
        private final SimpleIntegerProperty totalDays;
        private final SimpleStringProperty percentText;
        private final SimpleDoubleProperty progressValue;

        public StudentAttendanceSummary(String srNo, String firstName, String lastName, int attend, int absent, int total, String percentText, double progressValue) {
            this.srNo = new SimpleStringProperty(srNo);
            this.firstName = new SimpleStringProperty(firstName);
            this.lastName = new SimpleStringProperty(lastName);
            this.attendDays = new SimpleIntegerProperty(attend);
            this.absentDays = new SimpleIntegerProperty(absent);
            this.totalDays = new SimpleIntegerProperty(total);
            this.percentText = new SimpleStringProperty(percentText);
            this.progressValue = new SimpleDoubleProperty(progressValue);
        }

        public String getSrNo() { return srNo.get(); }
        public String getFirstName() { return firstName.get(); }
        public String getLastName() { return lastName.get(); }
        public int getAttendDays() { return attendDays.get(); }
        public int getAbsentDays() { return absentDays.get(); }
        public int getTotalDays() { return totalDays.get(); }
        public String getPercentText() { return percentText.get(); }
        public double getProgressValue() { return progressValue.get(); }
    }
}
