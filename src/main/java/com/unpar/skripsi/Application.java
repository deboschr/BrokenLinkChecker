package com.unpar.skripsi;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Application extends javafx.application.Application {
    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("views/main.fxml"));
        Scene scene = new Scene(fxmlLoader.load());

        stage.setTitle("BrokenLink Checker");
        stage.setScene(scene);

        stage.setMinWidth(900);
        stage.setMinHeight(600);
        stage.setMaximized(true);

        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
