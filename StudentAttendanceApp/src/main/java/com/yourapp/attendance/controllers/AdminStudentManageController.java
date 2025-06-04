package com.yourapp.attendance.controllers;

import com.yourapp.attendance.models.Student;
import com.yourapp.attendance.utils.DatabaseConnection;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.*;
import javafx.scene.Node;

import java.io.IOException;
import java.sql.*;

public class AdminStudentManageController {

    @FXML private TableView<Student> studentTableView;
    @FXML private TableColumn<Student, String> columnAddStudentId;
    @FXML private TableColumn<Student, String> columnAddStudentFirstName;
    @FXML private TableColumn<Student, String> columnAddStudentLastName;
    @FXML private TableColumn<Student, String> columnAddFatherName;
    @FXML private TableColumn<Student, String> columnAddStudentClass;
    @FXML private TableColumn<Student, String> columnAddStudentClassId;
    @FXML private TableColumn<Student, String> columnAddStudentGendre;
    @FXML private TableColumn<Student, Integer> columnAddStudentEnrolledYear;
    @FXML private TableColumn<Student, String> columnAddStudentUsername;
    @FXML private TableColumn<Student, String> columnAddStudentPassword;

    private ObservableList<Student> studentList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        columnAddStudentId.setCellValueFactory(new PropertyValueFactory<>("studentId"));
        columnAddStudentFirstName.setCellValueFactory(new PropertyValueFactory<>("firstName"));
        columnAddStudentLastName.setCellValueFactory(new PropertyValueFactory<>("lastName"));
        columnAddFatherName.setCellValueFactory(new PropertyValueFactory<>("fatherName"));
        columnAddStudentClass.setCellValueFactory(new PropertyValueFactory<>("className"));
        columnAddStudentClassId.setCellValueFactory(new PropertyValueFactory<>("classId"));
        columnAddStudentGendre.setCellValueFactory(new PropertyValueFactory<>("gender"));
        columnAddStudentEnrolledYear.setCellValueFactory(new PropertyValueFactory<>("enrolledYear"));
        columnAddStudentUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
        columnAddStudentPassword.setCellValueFactory(new PropertyValueFactory<>("password"));

        loadStudentData();
    }

    private void loadStudentData() {
        studentList.clear();
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM students")) {
            while (rs.next()) {
                studentList.add(new Student(
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
                ));
            }
            studentTableView.setItems(studentList);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleAddStudentPopup(ActionEvent event) {
        openStudentPopup(event, null);
    }

    @FXML
    private void handleUpdateStudentPopup(ActionEvent event) {
        Student selected = studentTableView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            openStudentPopup(event, selected);
        } else {
            showAlert("Please select a student to update.");
        }
    }

    @FXML
    private void handleDeleteStudent(ActionEvent event) {
        Student selected = studentTableView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement("DELETE FROM students WHERE student_id=?")) {
                ps.setString(1, selected.getStudentId());
                ps.executeUpdate();
                loadStudentData();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } else {
            showAlert("Please select a student to delete.");
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

        private void openStudentPopup(ActionEvent event, Student studentToEdit) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/adminAddUpdateStudent.fxml"));
                Scene scene = new Scene(loader.load());

                AdminAddUpdateStudentController controller = loader.getController();
                controller.initData(this, studentToEdit); // Pass back ref

                Stage popupStage = new Stage();
                popupStage.initModality(Modality.APPLICATION_MODAL);
                popupStage.initOwner(((Node) event.getSource()).getScene().getWindow());
                popupStage.setTitle(studentToEdit == null ? "Add Student" : "Update Student");
                popupStage.setScene(scene);
                popupStage.showAndWait();

                loadStudentData();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void showAlert(String msg) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setHeaderText(null);
            alert.setContentText(msg);
            alert.showAndWait();
        }
    }
