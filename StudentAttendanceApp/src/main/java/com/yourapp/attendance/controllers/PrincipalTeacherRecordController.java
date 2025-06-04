package com.yourapp.attendance.controllers;

import com.yourapp.attendance.models.TeacherRow;
import com.yourapp.attendance.utils.DatabaseConnection;
import com.yourapp.attendance.utils.SessionManager;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.*;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.StringConverter;
import javafx.event.*;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class PrincipalTeacherRecordController {

    @FXML private Label fetchPrincipalName;
    @FXML private Label overallTotalAttendance;
    @FXML private DatePicker fromDate, toDate;
    @FXML private TableView<TeacherRow> teacherDataTable;
    @FXML private TableColumn<TeacherRow, Number> srNo;
    @FXML private TableColumn<TeacherRow, String> fetchTeacherId, fetchFirstName, fetchLastName;
    @FXML private TableColumn<TeacherRow, Integer> calculateAttendDays, calculateAbsentDays, calculateOnLeaveClasses;
    @FXML private TableColumn<TeacherRow, String> calculatePercentage;
    @FXML private TableColumn<TeacherRow, ProgressBar> progressBar;
    @FXML private TableColumn<TeacherRow, Void> statusChecker;

    private final ObservableList<TeacherRow> teacherList = FXCollections.observableArrayList();

    public void initialize() {
        setupDatePickers();
        setupTableColumns();
        loadPrincipalName();
        loadTeacherData();
        fromDate.valueProperty().addListener((obs, oldV, newV) -> loadTeacherData());
        toDate.valueProperty().addListener((obs, oldV, newV) -> loadTeacherData());
    }

    private void setupDatePickers() {
        fromDate.setValue(LocalDate.of(2025, 3, 1));
        toDate.setValue(LocalDate.now());

        Callback<DatePicker, DateCell> dayCellFactory = dp -> new DateCell() {
            @Override public void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                if (item.isAfter(LocalDate.now())) setDisable(true);
            }
        };

        fromDate.setDayCellFactory(dayCellFactory);
        toDate.setDayCellFactory(dayCellFactory);
    }

    private void loadPrincipalName() {
        String username = SessionManager.getLoggedInUser();
        try (Connection conn = DatabaseConnection.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement("SELECT name FROM principals WHERE username = ?");
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                fetchPrincipalName.setText("Name: " + rs.getString("name"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupTableColumns() {
        srNo.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(teacherDataTable.getItems().indexOf(cellData.getValue()) + 1));
        fetchTeacherId.setCellValueFactory(new PropertyValueFactory<>("teacherId"));
        fetchFirstName.setCellValueFactory(new PropertyValueFactory<>("firstName"));
        fetchLastName.setCellValueFactory(new PropertyValueFactory<>("lastName"));
        calculateAttendDays.setCellValueFactory(new PropertyValueFactory<>("presentDays"));
        calculateAbsentDays.setCellValueFactory(new PropertyValueFactory<>("absentDays"));
        calculateOnLeaveClasses.setCellValueFactory(new PropertyValueFactory<>("leaveDays"));

        calculatePercentage.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                } else {
                    TeacherRow teacher = getTableRow().getItem();
                    int total = teacher.getPresentDays() + teacher.getAbsentDays() + teacher.getLeaveDays();
                    double percent = total == 0 ? 0 : (teacher.getPresentDays() * 100.0) / total;
                    String percentText = String.format("%.2f%%", percent);
                    setText(percentText);

                    if (percent < 75) setTextFill(Color.RED);
                    else if (percent < 80) setTextFill(Color.ORANGE);
                    else setTextFill(Color.GREEN);
                }
            }
        });

        progressBar.setCellFactory(column -> new TableCell<>() {
            private final ProgressBar bar = new ProgressBar();
            @Override protected void updateItem(ProgressBar item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    TeacherRow teacher = getTableRow().getItem();
                    int total = teacher.getPresentDays() + teacher.getAbsentDays() + teacher.getLeaveDays();
                    double percent = total == 0 ? 0 : teacher.getPresentDays() / (double) total;
                    bar.setProgress(percent);

                    if (percent < 0.75) bar.setStyle("-fx-accent: red;");
                    else if (percent < 0.8) bar.setStyle("-fx-accent: orange;");
                    else bar.setStyle("-fx-accent: green;");

                    setGraphic(bar);
                }
            }
        });

        statusChecker.setCellFactory(col -> new TableCell<>() {
            private final DatePicker datePicker = new DatePicker();
            {
                datePicker.setConverter(new StringConverter<>() {
                    final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
                    @Override public String toString(LocalDate date) {
                        return date == null ? "" : formatter.format(date);
                    }
                    @Override public LocalDate fromString(String str) {
                        return str == null || str.isEmpty() ? null : LocalDate.parse(str, formatter);
                    }
                });
                datePicker.setDayCellFactory(dp -> new DateCell() {
                    @Override public void updateItem(LocalDate item, boolean empty) {
                        super.updateItem(item, empty);
                        if (item.isAfter(LocalDate.now())) setDisable(true);
                    }
                });

                datePicker.setOnAction(e -> {
                    TeacherRow teacher = getTableRow().getItem();
                    LocalDate date = datePicker.getValue();
                    if (date == null) return;

                    try (Connection conn = DatabaseConnection.getConnection()) {
                        PreparedStatement stmt = conn.prepareStatement("SELECT status FROM teacher_attendance WHERE teacher_id = ? AND date = ?");
                        stmt.setString(1, teacher.getTeacherId());
                        stmt.setDate(2, Date.valueOf(date));
                        ResultSet rs = stmt.executeQuery();
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setHeaderText(null);
                        alert.setTitle("Status Info");

                        if (rs.next()) {
                            alert.setContentText(teacher.getFirstName() + " was " + rs.getString("status") + " on " + date);
                        } else {
                            alert.setContentText("No record found for " + date);
                        }
                        alert.showAndWait();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                });
            }

            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : datePicker);
            }
        });
    }

    private void loadTeacherData() {
        teacherList.clear();
        teacherDataTable.setItems(teacherList);
        LocalDate from = fromDate.getValue();
        LocalDate to = toDate.getValue();

        int totalPresent = 0;

        try (Connection conn = DatabaseConnection.getConnection()) {
            PreparedStatement teacherStmt = conn.prepareStatement("SELECT * FROM teachers");
            ResultSet rs = teacherStmt.executeQuery();

            while (rs.next()) {
                String id = rs.getString("teacher_id");
                String fname = rs.getString("first_name");
                String lname = rs.getString("last_name");
                String gender = rs.getString("gender");
                String className = rs.getString("class_name");

                int present = getAttendanceCount(id, "Present", from, to, conn);
                int absent = getAttendanceCount(id, "Absent", from, to, conn);
                int leave = getAttendanceCount(id, "On Leave", from, to, conn);
                int total = present + absent + leave;

                totalPresent = total;

                String percentage = total == 0 ? "0%" : String.format("%.2f%%", present * 100.0 / total);

                teacherList.add(new TeacherRow(id, fname, lname, gender, className, "", present, absent, leave, percentage));
            }
            int distinctAttendanceDays = 0;
            String distinctDatesQuery = "SELECT COUNT(DISTINCT date) FROM teacher_attendance WHERE date BETWEEN ? AND ?";
            try (PreparedStatement countStmt = conn.prepareStatement(distinctDatesQuery)) {
                countStmt.setDate(1, Date.valueOf(from));
                countStmt.setDate(2, Date.valueOf(to));
                ResultSet countRs = countStmt.executeQuery();
                if (countRs.next()) {
                    distinctAttendanceDays = countRs.getInt(1);
                }
            }

            overallTotalAttendance.setText("Total Attendance Days: " + distinctAttendanceDays);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int getAttendanceCount(String teacherId, String status, LocalDate from, LocalDate to, Connection conn) throws SQLException {
        String query = "SELECT COUNT(*) FROM teacher_attendance WHERE teacher_id = ? AND status = ? AND date BETWEEN ? AND ?";
        PreparedStatement stmt = conn.prepareStatement(query);
        stmt.setString(1, teacherId);
        stmt.setString(2, status);
        stmt.setDate(3, Date.valueOf(from));
        stmt.setDate(4, Date.valueOf(to));
        ResultSet rs = stmt.executeQuery();
        return rs.next() ? rs.getInt(1) : 0;
    }



@FXML
    private void handleSidebarNavigation(ActionEvent event) {
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
                Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                stage.setScene(new Scene(root));
                stage.show();
            } catch (Exception e) {
                e.printStackTrace();
                new Alert(Alert.AlertType.ERROR, "Error loading page").showAndWait();
            }
        }
    }
}