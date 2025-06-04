package com.yourapp.attendance.controllers;

import com.yourapp.attendance.models.StudentRow;
import com.yourapp.attendance.utils.DatabaseConnection;
import com.yourapp.attendance.utils.SessionManager;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.StringConverter;

import javafx.event.ActionEvent;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class PrincipalStudentRecordController {

    @FXML private Label fetchPrincipalName;
    @FXML private ComboBox<String> listAllClasses;
    @FXML private DatePicker fromDate, toDate;
    @FXML private TableView<StudentRow> studentDataShowTable;
    @FXML private TableColumn<StudentRow, Number> srNo;
    @FXML private TableColumn<StudentRow, String> fetchStudentId, fetchFirstName, fetchLastName;
    @FXML private TableColumn<StudentRow, Integer> calculateAttendDays, calculateAbsentDays;
    @FXML private TableColumn<StudentRow, Integer> calculateAllConductedClasses;
    @FXML private TableColumn<StudentRow, String> calculatePercentage;
    @FXML private TableColumn<StudentRow, ProgressBar> progressBar;
    @FXML private TableColumn<StudentRow, Void> statusChecker;

    private final ObservableList<StudentRow> studentList = FXCollections.observableArrayList();

    public void initialize() {
        String username = SessionManager.getLoggedInUser();
        String fullName = "";

        try (Connection conn = DatabaseConnection.getConnection()) {
            String query = "SELECT name FROM principals WHERE username = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                fullName = rs.getString("name");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        fetchPrincipalName.setText("Name: " + fullName);

        setupDatePickers();
        loadClassNames();
        setupTableColumns();
        listAllClasses.setOnAction(e -> loadStudentData());
        fromDate.valueProperty().addListener((obs, oldVal, newVal) -> loadStudentData());
        toDate.valueProperty().addListener((obs, oldVal, newVal) -> loadStudentData());
    }

    private void setupDatePickers() {
        LocalDate today = LocalDate.now();
        fromDate.setValue(LocalDate.of(2025, 3, 1));
        toDate.setValue(today);

        Callback<DatePicker, DateCell> dayCellFactory = dp -> new DateCell() {
            @Override public void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                if (item.isAfter(LocalDate.now())) setDisable(true);
            }
        };
        fromDate.setDayCellFactory(dayCellFactory);
        toDate.setDayCellFactory(dayCellFactory);
    }

    private void setupTableColumns() {
        srNo.setCellValueFactory(col -> new ReadOnlyObjectWrapper<>(studentDataShowTable.getItems().indexOf(col.getValue()) + 1));
        fetchStudentId.setCellValueFactory(new PropertyValueFactory<>("studentId"));
        fetchFirstName.setCellValueFactory(new PropertyValueFactory<>("firstName"));
        fetchLastName.setCellValueFactory(new PropertyValueFactory<>("lastName"));
        calculateAttendDays.setCellValueFactory(new PropertyValueFactory<>("presentDays"));
        calculateAbsentDays.setCellValueFactory(new PropertyValueFactory<>("absentDays"));

        calculateAllConductedClasses.setCellValueFactory(cell -> {
            int present = cell.getValue().getPresentDays();
            int absent = cell.getValue().getAbsentDays();
            return new ReadOnlyObjectWrapper<>(present + absent);
        });

        calculatePercentage.setCellFactory(column -> new TableCell<>() {
            @Override protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null); setStyle("");
                } else {
                    StudentRow student = getTableRow().getItem();
                    int total = student.getPresentDays() + student.getAbsentDays();
                    double percent = total == 0 ? 0 : (student.getPresentDays() * 100.0) / total;
                    String percentStr = String.format("%.2f%%", percent);
                    setText(percentStr);
                    setTextFill(percent < 75.0 ? Color.RED : Color.BLACK);
                }
            }
        });

        progressBar.setCellFactory(column -> new TableCell<>() {
            private final ProgressBar bar = new ProgressBar(0);

            @Override protected void updateItem(ProgressBar progress, boolean empty) {
                super.updateItem(progress, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    StudentRow student = getTableRow().getItem();
                    int total = student.getPresentDays() + student.getAbsentDays();
                    double percent = total == 0 ? 0 : (student.getPresentDays() * 1.0) / total;
                    bar.setProgress(percent);
                    if (percent < 0.75) {
                        bar.setStyle("-fx-accent: red;");
                    } else if (percent < 0.80) {
                        bar.setStyle("-fx-accent: yellow;");
                    } else {
                        bar.setStyle("-fx-accent: green;");
                    }
                    setGraphic(bar);
                }
            }
        });

        statusChecker.setCellFactory(column -> new TableCell<>() {
            private final DatePicker datePicker = new DatePicker();

            {

                datePicker.setConverter(new StringConverter<>() {
                    final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd-MM-yyyy");
                    @Override public String toString(LocalDate date) {
                        return date == null ? "" : dtf.format(date);
                    }
                    @Override public LocalDate fromString(String string) {
                        return (string == null || string.isEmpty()) ? null : LocalDate.parse(string, dtf);
                    }
                });

                datePicker.setOnAction(event -> {
                    StudentRow student = getTableView().getItems().get(getIndex());
                    LocalDate selectedDate = datePicker.getValue();
                    if (selectedDate == null) return;

                    try (Connection conn = DatabaseConnection.getConnection()) {
                        String sql = "SELECT status FROM attendance WHERE student_id = ? AND date = ?";
                        PreparedStatement stmt = conn.prepareStatement(sql);
                        stmt.setString(1, student.getStudentId());
                        stmt.setDate(2, Date.valueOf(selectedDate));
                        ResultSet rs = stmt.executeQuery();

                        if (rs.next()) {
                            String status = rs.getString("status");
                            Alert alert = new Alert(Alert.AlertType.INFORMATION);
                            alert.setTitle("Attendance Status");
                            alert.setHeaderText(null);
                            alert.setContentText(student.getFirstName() + " was " + status + " on " + selectedDate);
                            alert.showAndWait();
                        } else {
                            Alert alert = new Alert(Alert.AlertType.INFORMATION);
                            alert.setTitle("Attendance Status");
                            alert.setHeaderText(null);
                            alert.setContentText("No attendance record for " + selectedDate);
                            alert.showAndWait();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }

            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(datePicker);
                }
            }
        });
    }

    private void loadClassNames() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT class_name FROM classes");
            while (rs.next()) {
                listAllClasses.getItems().add(rs.getString("class_name"));
            }

            // Set the default selected class to "Class One" if it's available
            if (listAllClasses.getItems().contains("One")) {
                listAllClasses.setValue("One");
                loadStudentData();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void loadStudentData() {
        studentList.clear();
        studentDataShowTable.setItems(studentList);

        String className = listAllClasses.getValue();
        LocalDate from = fromDate.getValue();
        LocalDate to = toDate.getValue();
        if (className == null || from == null || to == null) return;

        try (Connection conn = DatabaseConnection.getConnection()) {
            String studentSql = "SELECT * FROM students WHERE class_name = ?";
            PreparedStatement stmt = conn.prepareStatement(studentSql);
            stmt.setString(1, className);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                String studentId = rs.getString("student_id");
                String fname = rs.getString("first_name");
                String lname = rs.getString("last_name");
                String fatherName = rs.getString("father_name");
                String gender = rs.getString("gender");

                int present = getAttendanceCount(studentId, from, to, "present", conn);
                int absent = getAttendanceCount(studentId, from, to, "absent", conn);

                studentList.add(new StudentRow(studentId, fname, lname, fatherName, gender, "", present, absent));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int getAttendanceCount(String studentId, LocalDate from, LocalDate to, String status, Connection conn) throws SQLException {
        String sql = "SELECT COUNT(*) FROM attendance WHERE student_id = ? AND status = ? AND date BETWEEN ? AND ?";
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setString(1, studentId);
        stmt.setString(2, status);
        stmt.setDate(3, Date.valueOf(from));
        stmt.setDate(4, Date.valueOf(to));
        ResultSet rs = stmt.executeQuery();
        return rs.next() ? rs.getInt(1) : 0;
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
