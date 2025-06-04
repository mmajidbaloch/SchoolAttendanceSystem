package com.yourapp.attendance.controllers;

import com.yourapp.attendance.models.TeacherRow;
import com.yourapp.attendance.utils.DatabaseConnection;
import com.yourapp.attendance.utils.SessionManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.Callback;

import java.sql.*;
import java.time.LocalDate;

public class TeacherAttendanceController {

    @FXML private Label fetchPrincipalfullName;
    @FXML private DatePicker attendenceDate;
    @FXML private TableView<TeacherRow> teacherTable;
    @FXML private TableColumn<TeacherRow, String> fetchRelatedTeacherId;
    @FXML private TableColumn<TeacherRow, String> fetchRelatedTeacherFName;
    @FXML private TableColumn<TeacherRow, String> fetchRelatedTeacherLName;
    @FXML private TableColumn<TeacherRow, String> fetchRelatedTeacherGender;
    @FXML private TableColumn<TeacherRow, String> fetchRelatedTeacherAssignedClass;
    @FXML private TableColumn<TeacherRow, String> teacherAttendMarkedByPrincipal;
    @FXML private Button saveTeacherAttendence;
    @FXML private Button updateTeacherAttendenceOnly;

    private final ObservableList<TeacherRow> teacherList = FXCollections.observableArrayList();
    private PrincipalDashboardController principalDashboardController;
    public void setPrincipalDashboardController(PrincipalDashboardController controller) {
        this.principalDashboardController = controller;
    }


    @FXML
    public void initialize() {
        fetchPrincipalfullName.setText("Name: " + SessionManager.get("principalFullName"));

        // Disable future dates
        attendenceDate.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || date.isAfter(LocalDate.now()));
            }
        });

        // Set default to today
        attendenceDate.setValue(LocalDate.now());

        // Load data when date is changed
        attendenceDate.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                loadTeacherList();
            }
        });

        // Set column bindings
        fetchRelatedTeacherId.setCellValueFactory(new PropertyValueFactory<>("teacherId"));
        fetchRelatedTeacherFName.setCellValueFactory(new PropertyValueFactory<>("firstName"));
        fetchRelatedTeacherLName.setCellValueFactory(new PropertyValueFactory<>("lastName"));
        fetchRelatedTeacherGender.setCellValueFactory(new PropertyValueFactory<>("gender"));
        fetchRelatedTeacherAssignedClass.setCellValueFactory(new PropertyValueFactory<>("className"));
        teacherAttendMarkedByPrincipal.setCellValueFactory(new PropertyValueFactory<>("todayStatus"));
        teacherAttendMarkedByPrincipal.setCellFactory(getAttendanceCellFactory());

        // Load initial list
        loadTeacherList();

        // Button actions
        saveTeacherAttendence.setOnAction(e -> saveAttendance(false));
        updateTeacherAttendenceOnly.setOnAction(e -> saveAttendance(true));
    }


    private Callback<TableColumn<TeacherRow, String>, TableCell<TeacherRow, String>> getAttendanceCellFactory() {
        return column -> new TableCell<>() {
            private final ComboBox<String> comboBox = new ComboBox<>(FXCollections.observableArrayList("Present", "Absent","On Leave"));

            {
                comboBox.setOnAction(event -> {
                    TeacherRow teacher = getTableView().getItems().get(getIndex());
                    teacher.setTodayStatus(comboBox.getValue());
                    teacher.setEdited(true);
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    comboBox.setValue(item);
                    setGraphic(comboBox);
                }
            }
        };
    }

    private void loadTeacherList() {
        teacherList.clear();

        LocalDate selectedDate = attendenceDate.getValue();
        String date = selectedDate != null ? selectedDate.toString() : LocalDate.now().toString();

        String query = "SELECT t.teacher_id, t.first_name, t.last_name, t.gender, " +
                "c.class_name, a.status " +
                "FROM teachers t " +
                "LEFT JOIN class_teacher_assignments ca ON t.teacher_id = ca.teacher_id " +
                "LEFT JOIN classes c ON ca.class_id = c.class_id " +
                "LEFT JOIN teacher_attendance a ON t.teacher_id = a.teacher_id AND a.date = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, date);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String attendanceStatus = rs.getString("status");
                    TeacherRow teacher = new TeacherRow(
                            rs.getString("teacher_id"),
                            rs.getString("first_name"),
                            rs.getString("last_name"),
                            rs.getString("gender"),
                            rs.getString("class_name") != null ? rs.getString("class_name") : "N/A",
                            attendanceStatus != null ? attendanceStatus : "Present",
                            0,
                            0,
                            0,
                            "N/A"
                    );
                    teacherList.add(teacher);
                }
            }

            teacherTable.setItems(teacherList);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    private void saveAttendance(boolean isUpdate) {
        LocalDate selectedDate = attendenceDate.getValue();
        if (selectedDate == null || selectedDate.isAfter(LocalDate.now())) {
            showAlert(Alert.AlertType.WARNING, "Please select a valid date.");
            return;
        }

        String date = selectedDate.toString();
        String principalId = SessionManager.get("principalId");

        String checkQuery = "SELECT COUNT(*) FROM teacher_attendance WHERE teacher_id = ? AND date = ?";
        String insertQuery = "INSERT INTO teacher_attendance (teacher_id, date, status) VALUES (?, ?, ?)";
        String updateQuery = "UPDATE teacher_attendance SET status = ? WHERE teacher_id = ? AND date = ?";


        boolean saved = false;
        boolean alreadyTaken = false;

        try (Connection conn = DatabaseConnection.getConnection()) {
            for (TeacherRow teacher : teacherList) {
                if (isUpdate && !teacher.isEdited()) continue;

                if (isUpdate) {
                    try (PreparedStatement stmt = conn.prepareStatement(updateQuery)) {
                        stmt.setString(1, teacher.getTodayStatus());
                        stmt.setString(2, teacher.getTeacherId());
                        stmt.setString(3, date);
                        int rows = stmt.executeUpdate();
                        if (rows > 0) saved = true;
                    }
                } else {
                    try (PreparedStatement checkStmt = conn.prepareStatement(checkQuery)) {
                        checkStmt.setString(1, teacher.getTeacherId());
                        checkStmt.setString(2, date);
                        ResultSet rs = checkStmt.executeQuery();
                        if (rs.next() && rs.getInt(1) > 0) {
                            alreadyTaken = true;
                            continue;
                        }
                    }

                    try (PreparedStatement insertStmt = conn.prepareStatement(insertQuery)) {
                        insertStmt.setString(1, teacher.getTeacherId());
                        insertStmt.setString(2, date);
                        insertStmt.setString(3, teacher.getTodayStatus());
                        insertStmt.executeUpdate();
                        saved = true;
                    }
                }
            }

            if (isUpdate) {
                showAlert(Alert.AlertType.INFORMATION, saved ? "Attendance updated!" : "No records updated.");
            } else {
                if (saved) {
                    showAlert(Alert.AlertType.INFORMATION, "Attendance saved successfully!");
                } else if (alreadyTaken) {
                    showAlert(Alert.AlertType.WARNING, "Attendance already taken for some teachers.");
                } else {
                    showAlert(Alert.AlertType.ERROR, "No attendance was saved.");
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Database error occurred.");
        }
        if (principalDashboardController != null) {
            principalDashboardController.refreshTable();
        }
        loadTeacherList();
    }

    private void showAlert(Alert.AlertType type, String message) {
        Alert alert = new Alert(type, message, ButtonType.OK);
        alert.showAndWait();
    }
}
