package com.yourapp.attendance.controllers;

import com.yourapp.attendance.models.StudentRow;
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

public class StudentAttendanceController {

    @FXML private Label fetchRelatedTeacherUsername;
    @FXML private Label fetchRelatedClassId;
    @FXML private DatePicker attendenceDate;

    @FXML private TableView<StudentRow> studentTable;
    @FXML private TableColumn<StudentRow, String> fetchRelatedStudId;
    @FXML private TableColumn<StudentRow, String> fetchRelatedStudFNameOfTeacher;
    @FXML private TableColumn<StudentRow, String> fetchRelatedLtudLNameOfStudent;
    @FXML private TableColumn<StudentRow, String> fetchRelatedStudFatherName;
    @FXML private TableColumn<StudentRow, String> fetchRelatedStudGender;
    @FXML private TableColumn<StudentRow, String> fetchRelatedStudClassName;
    @FXML private TableColumn<StudentRow, String> studentAttendMarkedByTeacher;

    @FXML private Button saveStudentAttendence;
    @FXML private Button updateStudentAttendenceOnly;

    private ObservableList<StudentRow> studentList = FXCollections.observableArrayList();
    private TeacherDashboardController teacherDashboardController;

    public void setTeacherDashboardController(TeacherDashboardController controller) {
        this.teacherDashboardController = controller;
    }



    @FXML
    public void initialize() {
        // Set labels from session
        fetchRelatedTeacherUsername.setText("Incharge: " + SessionManager.get("teacherFullName"));
        fetchRelatedClassId.setText("Class Id: " + SessionManager.get("classId"));

        attendenceDate.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || date.isAfter(LocalDate.now())); // Disable future dates
            }
        });

        // Set default date to today
        attendenceDate.setValue(LocalDate.now());

        // Listen to date change
        attendenceDate.valueProperty().addListener((obs, oldDate, newDate) -> {
            if (newDate != null) {
                loadStudentsForAttendance(newDate);
            }
        });

        // Table columns setup
        fetchRelatedStudId.setCellValueFactory(new PropertyValueFactory<>("studentId"));
        fetchRelatedStudFNameOfTeacher.setCellValueFactory(new PropertyValueFactory<>("firstName"));
        fetchRelatedLtudLNameOfStudent.setCellValueFactory(new PropertyValueFactory<>("lastName"));
        fetchRelatedStudFatherName.setCellValueFactory(new PropertyValueFactory<>("fatherName"));
        fetchRelatedStudGender.setCellValueFactory(new PropertyValueFactory<>("gender"));
        fetchRelatedStudClassName.setCellValueFactory(new PropertyValueFactory<>("className"));
        studentAttendMarkedByTeacher.setCellFactory(getAttendanceCellFactory());
        studentAttendMarkedByTeacher.setCellValueFactory(new PropertyValueFactory<>("todayStatus"));

        // ✅ Correct method call with LocalDate
        loadStudentsForAttendance(LocalDate.now());

        saveStudentAttendence.setOnAction(e -> saveAttendance(false));
        updateStudentAttendenceOnly.setOnAction(e -> saveAttendance(true));
    }


    private Callback<TableColumn<StudentRow, String>, TableCell<StudentRow, String>> getAttendanceCellFactory() {
        return column -> new TableCell<>() {
            private final ComboBox<String> comboBox = new ComboBox<>(FXCollections.observableArrayList("Present", "Absent"));

            {
                comboBox.setOnAction(event -> {
                    StudentRow student = getTableView().getItems().get(getIndex());
                    student.setTodayStatus(comboBox.getValue());
                    student.setEdited(true);
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

    private void loadStudentsForAttendance(LocalDate selectedDate) {
        studentList.clear();
        String classId = SessionManager.get("classId");

        String query =
                "SELECT s.student_id, s.first_name, s.last_name, s.father_name, s.gender, s.class_name, " +
                        "a.status " +
                        "FROM students s " +
                        "LEFT JOIN attendance a ON s.student_id = a.student_id " +
                        "AND a.class_id = ? AND a.date = ? " +
                        "WHERE s.class_id = ?";


        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, classId);
            stmt.setDate(2, java.sql.Date.valueOf(selectedDate));
            stmt.setString(3, classId);

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                String status = rs.getString("status");
                if (status == null) {
                    status = "Present"; // default if not marked
                }

                StudentRow student = new StudentRow(
                        rs.getString("student_id"),
                        rs.getString("first_name"),
                        rs.getString("last_name"),
                        rs.getString("father_name"),
                        rs.getString("gender"),
                        status,
                        0,
                        0
                );
                student.setClassName(rs.getString("class_name"));
                studentList.add(student);
            }

            studentTable.setItems(studentList);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    private void saveAttendance(boolean isUpdate) {
        if (attendenceDate.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "Please select a date first.");
            return;
        }

        if (attendenceDate.getValue().isAfter(LocalDate.now())) {
            showAlert(Alert.AlertType.WARNING, "You cannot take attendance for future dates.");
            return;
        }

        String date = attendenceDate.getValue().toString();
        String classId = SessionManager.get("classId");
        String teacherId = SessionManager.get("teacherId");

        String checkQuery = "SELECT COUNT(*) FROM attendance WHERE student_id = ? AND class_id = ? AND date = ?";
        String insertQuery = "INSERT INTO attendance (student_id, class_id, date, status, taken_by) VALUES (?, ?, ?, ?, ?)";
        String updateQuery = "UPDATE attendance SET status = ?, taken_by = ? WHERE student_id = ? AND date = ?";

        boolean attendanceSaved = false;
        boolean attendanceAlreadyTaken = false;

        try (Connection conn = DatabaseConnection.getConnection()) {
            for (StudentRow student : studentList) {
                if (isUpdate && !student.isEdited()) {
                    continue; // Skip students who were not edited
                }
                if (isUpdate) {
                    // Update mode
                    try (PreparedStatement stmt = conn.prepareStatement(updateQuery)) {
                        stmt.setString(1, student.getTodayStatus());
                        stmt.setString(2, teacherId);
                        stmt.setString(3, student.getStudentId());
                        stmt.setString(4, date);
                        int updatedRows = stmt.executeUpdate();
                        if (updatedRows > 0) {
                            attendanceSaved = true;
                        }
                    }
                } else {
                    // Save new attendance
                    try (PreparedStatement checkStmt = conn.prepareStatement(checkQuery)) {
                        checkStmt.setString(1, student.getStudentId());
                        checkStmt.setString(2, classId);
                        checkStmt.setString(3, date);

                        ResultSet rs = checkStmt.executeQuery();
                        if (rs.next() && rs.getInt(1) > 0) {
                            attendanceAlreadyTaken = true;
                            continue; // Skip inserting for this student
                        }
                    }

                    try (PreparedStatement insertStmt = conn.prepareStatement(insertQuery)) {
                        insertStmt.setString(1, student.getStudentId());
                        insertStmt.setString(2, classId);
                        insertStmt.setString(3, date);
                        insertStmt.setString(4, student.getTodayStatus());
                        insertStmt.setString(5, teacherId);
                        insertStmt.executeUpdate();
                        attendanceSaved = true;
                    }
                }
            }

            if (isUpdate) {
                showAlert(Alert.AlertType.INFORMATION, attendanceSaved ? "Attendance updated!" : "No attendance records were updated.");
            } else {
                if (attendanceSaved) {
                    showAlert(Alert.AlertType.INFORMATION, "Attendance saved successfully!");
                } else if (attendanceAlreadyTaken) {
                    showAlert(Alert.AlertType.WARNING, "Attendance already taken for that day!");
                } else {
                    showAlert(Alert.AlertType.ERROR, "No attendance was saved.");
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Database error occurred.");
        }

        if (teacherDashboardController != null) {
            teacherDashboardController.refreshTable();
        }

    }


    private void showAlert(Alert.AlertType type, String message) {
        Alert alert = new Alert(type, message, ButtonType.OK);
        alert.showAndWait();
    }
}
