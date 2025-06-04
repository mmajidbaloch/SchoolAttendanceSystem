package com.yourapp.attendance.controllers;
import com.yourapp.attendance.utils.DatabaseConnection;
import com.yourapp.attendance.models.ClassSection;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.event.ActionEvent;
import javafx.stage.Stage;
import javafx.scene.Node;

import java.sql.*;
import java.io.IOException;




public class AdminAddClassesController {

    @FXML
    private TextField classId, className, classCapacity;
    @FXML
    private TableView<ClassSection> classTableView;
    @FXML
    private TableColumn<ClassSection, String> columnClassId;
    @FXML
    private TableColumn<ClassSection, String> columnClassName;
    @FXML
    private TableColumn<ClassSection, Integer> columnClassCapacity;

    private ObservableList<ClassSection> classList = FXCollections.observableArrayList();

    private Connection getConnection() throws SQLException {
        return DatabaseConnection.getConnection();
    }

    @FXML
    public void initialize() {
        columnClassId.setCellValueFactory(new PropertyValueFactory<>("classId"));
        columnClassName.setCellValueFactory(new PropertyValueFactory<>("className"));
        columnClassCapacity.setCellValueFactory(new PropertyValueFactory<>("capacity"));
        loadClassData();
    }

    private void loadClassData() {
        classList.clear();
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM classes")) {
            while (rs.next()) {
                classList.add(new ClassSection(
                        rs.getString("class_id"),
                        rs.getString("class_name"),
                        rs.getInt("capacity")));
            }
            classTableView.setItems(classList);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleAddClass(ActionEvent event) {
        if (classId.getText().isEmpty() || className.getText().isEmpty() || classCapacity.getText().isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "All fields are required!");
            return;
        }
        try (Connection conn = getConnection();

             PreparedStatement ps = conn.prepareStatement("INSERT INTO classes VALUES (?, ?, ?)")) {
            ps.setString(1, classId.getText());
            ps.setString(2, className.getText());
            ps.setInt(3, Integer.parseInt(classCapacity.getText()));
            ps.executeUpdate();
            loadClassData();
            clearFields();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleUpdateClass(ActionEvent event) {
        if (classId.getText().isEmpty() || className.getText().isEmpty() || classCapacity.getText().isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "All fields are required!");
            return;
        }
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE classes SET class_name=?, capacity=? WHERE class_id=?")) {
            ps.setString(1, className.getText());
            ps.setInt(2, Integer.parseInt(classCapacity.getText()));
            ps.setString(3, classId.getText());
            ps.executeUpdate();
            loadClassData();
            clearFields();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleDeleteClass(ActionEvent event) {
        if (classId.getText().isEmpty() || className.getText().isEmpty() || classCapacity.getText().isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "All fields are required!");
            return;
        }
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM classes WHERE class_id=?")) {
            ps.setString(1, classId.getText());
            ps.executeUpdate();
            loadClassData();
            clearFields();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void showAlert(Alert.AlertType alertType, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle("Validation Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void clearFields() {
        classId.clear();
        className.clear();
        classCapacity.clear();
    }

    // Optional: add sidebar nav handler like in AdminDashboardController
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
}
