package com.yourapp.attendance.controllers;

import com.yourapp.attendance.models.Principal;
import com.yourapp.attendance.models.Teacher;
import com.yourapp.attendance.utils.DatabaseConnection;
import javafx.collections.*;
import javafx.event.ActionEvent;
import javafx.fxml.*;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.*;
import javafx.scene.Node;

import java.io.IOException;
import java.sql.*;

public class AdminTeacherManageController {
    @FXML private TableView<Teacher> teacherTableView;
    @FXML private TableColumn<Teacher, String> columnAddTeacherId;
    @FXML private TableColumn<Teacher, String> columnAddTeacherFirstName;
    @FXML private TableColumn<Teacher, String> columnAddTeacherLastName;
    @FXML private TableColumn<Teacher, String> columnAddTeacherClass;
    @FXML private TableColumn<Teacher, String> columnAddTeacherClassId;
    @FXML private TableColumn<Teacher, String> columnAddTeacherGendre;
    @FXML private TableColumn<Teacher, String> columnAddTeacherContact;
    @FXML private TableColumn<Teacher, String> columnAddTeacherUsername;
    @FXML private TableColumn<Teacher, String> columnAddTeacherPassword;

    private ObservableList<Teacher> teacherList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        columnAddTeacherId.setCellValueFactory(new PropertyValueFactory<>("teacherId"));
        columnAddTeacherFirstName.setCellValueFactory(new PropertyValueFactory<>("firstName"));
        columnAddTeacherLastName.setCellValueFactory(new PropertyValueFactory<>("lastName"));
        columnAddTeacherClass.setCellValueFactory(new PropertyValueFactory<>("className"));
        columnAddTeacherClassId.setCellValueFactory(new PropertyValueFactory<>("classId"));
        columnAddTeacherGendre.setCellValueFactory(new PropertyValueFactory<>("gender"));
        columnAddTeacherContact.setCellValueFactory(new PropertyValueFactory<>("contact"));
        columnAddTeacherUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
        columnAddTeacherPassword.setCellValueFactory(new PropertyValueFactory<>("password"));

        loadTeacherData();
    }

    @FXML
    private void handlePrincipalDetail(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/addPrincipal.fxml"));
            Parent root = loader.load();

            addPrincipalController controller = loader.getController();

            Principal existingPrincipal = fetchPrincipalFromDB();
            controller.initData(this, existingPrincipal); // Will be null if no principal exists

            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Principal Details");
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Principal fetchPrincipalFromDB() {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM principals LIMIT 1")) {

            if (rs.next()) {
                return new Principal(
                        rs.getString("name"),
                        rs.getString("username"),
                        rs.getString("password")
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }


    private void loadTeacherData() {
        teacherList.clear();
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM teachers")) {
            while (rs.next()) {
                teacherList.add(new Teacher(
                        rs.getString("teacher_id"),
                        rs.getString("first_name"),
                        rs.getString("last_name"),
                        rs.getString("class_name"),
                        rs.getString("class_id"),
                        rs.getString("gender"),
                        rs.getString("contact"),
                        rs.getString("username"),
                        rs.getString("password")
                ));
            }
            teacherTableView.setItems(teacherList);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleAddTeacherPopup(ActionEvent event) {
        openTeacherPopup(event, null);
    }

    @FXML
    private void handleUpdateTeacherPopup(ActionEvent event) {
        Teacher selected = teacherTableView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            openTeacherPopup(event, selected);
        } else {
            showAlert("Please select a teacher to update.");
        }
    }

    @FXML
    private void handleDeleteTeacher(ActionEvent event) {
        Teacher selected = teacherTableView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement("DELETE FROM teachers WHERE teacher_id=?")) {
                ps.setString(1, selected.getTeacherId());
                ps.executeUpdate();
                loadTeacherData();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } else {
            showAlert("Please select a teacher to delete.");
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

    private void openTeacherPopup(ActionEvent event, Teacher teacherToEdit) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/adminAddUpdateTeacher.fxml"));
            Scene scene = new Scene(loader.load());

            AdminAddUpdateTeacherController controller = loader.getController();
            controller.initData(this, teacherToEdit);

            Stage popupStage = new Stage();
            popupStage.initModality(Modality.APPLICATION_MODAL);
            popupStage.initOwner(((Node) event.getSource()).getScene().getWindow());
            popupStage.setTitle(teacherToEdit == null ? "Add Teacher" : "Update Teacher");
            popupStage.setScene(scene);
            popupStage.showAndWait();

            loadTeacherData();

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
