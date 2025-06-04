package com.yourapp.attendance;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class Main extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
        Scene scene = new Scene(loader.load());
        stage.setScene(scene);
        stage.setTitle("GHS Taunsa Sharif");
        stage.getIcons().add(new Image(getClass().getResourceAsStream("/images/Presenz.png")));

        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
