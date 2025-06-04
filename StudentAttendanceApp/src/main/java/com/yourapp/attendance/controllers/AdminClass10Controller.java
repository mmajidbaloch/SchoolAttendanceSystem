package com.yourapp.attendance.controllers;

import com.yourapp.attendance.utils.DatabaseConnection;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import com.yourapp.attendance.models.Student;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import javafx.scene.control.TableView;
import java.io.IOException;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

import javafx.beans.property.SimpleStringProperty;


public class AdminClass10Controller {

    @FXML private Label teacherNameWithClassId110;
    @FXML private ChoiceBox<String> assignAttendenceTakerTeacherC110;
    @FXML private Button updateStudentAttendenceRecordC110;
    @FXML private TableColumn<Student, String> takenByTeacherClass110;
    @FXML private TableColumn<Student, Integer> calculateStudentPresentDaysC110;
    @FXML private TableColumn<Student, Integer> calculateStudentAbsentDaysC110;

    // Table column references (optional hookup later if needed)
    @FXML private TableView<?> fetchStudentsTable; // Just a placeholder
    // Add other columns here...
    @FXML private TableView<Student> class10TableView;
    @FXML private TableColumn<Student, Integer> fetchStudentIdClassIdIs110;
    @FXML private TableColumn<Student, String> fetchStudFNameClassIdIs110;
    @FXML private TableColumn<Student, String> fetchStudLNameClassIdIs110;
    @FXML private TableColumn<Student, String> fetchStudGenderClassIdIs110;

    private ObservableList<Student> studentList110 = FXCollections.observableArrayList();
    private Map<String, AttendanceInfo> attendanceMap = new HashMap<>();

    private final String CLASS_ID = "110";

    @FXML
    public void initialize() {
        loadAssignedTeacher();
        loadTeacherOptions();
        loadClass10Students();
        loadAttendanceDataForClass10();
    }

    private static class AttendanceInfo {
        int presentDays;
        int absentDays;
        String lastTakenBy;

        AttendanceInfo(int present, int absent, String teacher) {
            this.presentDays = present;
            this.absentDays = absent;
            this.lastTakenBy = teacher;
        }
    }

    private void loadAssignedTeacher() {
        String query = "SELECT t.username "
                + "FROM class_teacher_assignments cta "
                + "JOIN teachers t ON cta.teacher_id = t.teacher_id "
                + "WHERE cta.class_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {

            ps.setString(1, CLASS_ID);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String username = rs.getString("username");
                teacherNameWithClassId110.setText("Assigned Teacher: " + username);
            } else {
                teacherNameWithClassId110.setText("Assigned Teacher: None");
            }

        } catch (SQLException e) {
            teacherNameWithClassId110.setText("Error loading teacher");
            e.printStackTrace();
        }
    }

    private void loadAttendanceDataForClass10() {
        String query = "SELECT student_id, " +
                "SUM(CASE WHEN status = 'Present' THEN 1 ELSE 0 END) AS present_days, " +
                "SUM(CASE WHEN status = 'Absent' THEN 1 ELSE 0 END) AS absent_days, " +
                "MAX(taken_by) AS last_teacher " +
                "FROM attendance " +
                "WHERE class_id = '110' " +
                "GROUP BY student_id";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String studentId = rs.getString("student_id");
                int present = rs.getInt("present_days");
                int absent = rs.getInt("absent_days");
                String teacher = rs.getString("last_teacher");

                attendanceMap.put(studentId, new AttendanceInfo(present, absent, teacher));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadClass10Students() {
        String query = "SELECT student_id, first_name, last_name, father_name, class_name, class_id, gender, enrolled_year, username, password FROM students WHERE class_id = '110'";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Student student = new Student(
                        rs.getString("student_id"),
                        rs.getString("first_name"),
                        rs.getString("last_name"),
                        rs.getString("father_name"),
                        rs.getString("class_name"),
                        rs.getString("class_id"),
                        rs.getString("gender"),
                        rs.getInt("enrolled_year"),
                        rs.getString("username"),
                        rs.getString("password")
                );
                studentList110.add(student);
                AttendanceInfo info = attendanceMap.get(student.getStudentId());
                if (info != null) {
                    student.setPresentDays(info.presentDays);
                    student.setAbsentDays(info.absentDays);
                    student.setTakenBy(info.lastTakenBy);
                }

            }

            fetchStudentIdClassIdIs110.setCellValueFactory(new PropertyValueFactory<>("studentId"));
            fetchStudFNameClassIdIs110.setCellValueFactory(new PropertyValueFactory<>("firstName"));
            fetchStudLNameClassIdIs110.setCellValueFactory(new PropertyValueFactory<>("lastName"));
            fetchStudGenderClassIdIs110.setCellValueFactory(new PropertyValueFactory<>("gender"));

            class10TableView.setItems(studentList110);
            takenByTeacherClass110.setCellValueFactory(cellData ->
                    new SimpleStringProperty(attendanceMap.getOrDefault(cellData.getValue().getStudentId(), new AttendanceInfo(0, 0, "N/A")).lastTakenBy));

            calculateStudentPresentDaysC110.setCellValueFactory(cellData ->
                    new ReadOnlyObjectWrapper<>(attendanceMap.getOrDefault(cellData.getValue().getStudentId(), new AttendanceInfo(0, 0, "")).presentDays));

            calculateStudentAbsentDaysC110.setCellValueFactory(cellData ->
                    new ReadOnlyObjectWrapper<>(attendanceMap.getOrDefault(cellData.getValue().getStudentId(), new AttendanceInfo(0, 0, "")).absentDays));

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadTeacherOptions() {
        ObservableList<String> usernames = FXCollections.observableArrayList();
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT username FROM teachers")) {

            while (rs.next()) {
                usernames.add(rs.getString("username"));
            }

            assignAttendenceTakerTeacherC110.setItems(usernames);

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

    @FXML
    private void updateTeacherAssignment() {
        String selectedUsername = assignAttendenceTakerTeacherC110.getValue();
        if (selectedUsername == null) {
            showAlert("Please select a teacher to assign.");
            return;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            // First, fetch teacher_id using username
            PreparedStatement findId = conn.prepareStatement("SELECT teacher_id FROM teachers WHERE username = ?");
            findId.setString(1, selectedUsername);
            ResultSet rs = findId.executeQuery();

            if (rs.next()) {
                String teacherId = rs.getString("teacher_id");

                // Check if an assignment exists
                PreparedStatement check = conn.prepareStatement("SELECT * FROM class_teacher_assignments WHERE class_id = ?");
                check.setString(1, CLASS_ID);
                ResultSet result = check.executeQuery();

                if (result.next()) {
                    // Update
                    PreparedStatement update = conn.prepareStatement("UPDATE class_teacher_assignments SET teacher_id = ? WHERE class_id = ?");
                    update.setString(1, teacherId);
                    update.setString(2, CLASS_ID);
                    update.executeUpdate();
                } else {
                    // Insert
                    PreparedStatement insert = conn.prepareStatement("INSERT INTO class_teacher_assignments (class_id, teacher_id) VALUES (?, ?)");
                    insert.setString(1, CLASS_ID);
                    insert.setString(2, teacherId);
                    insert.executeUpdate();
                }

                teacherNameWithClassId110.setText("Assigned Teacher: " + selectedUsername);
                showAlert("Teacher assignment updated successfully.");

            } else {
                showAlert("Teacher not found.");
            }

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error occurred while updating assignment.");
        }
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
