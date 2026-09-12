package com.rsps.interfacemaker;

import com.rsps.interfacemaker.view.MainView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class InterfaceMakerApp extends Application {
    @Override
    public void start(Stage primaryStage) {
        MainView mainView = new MainView(primaryStage);
        Scene scene = new Scene(mainView, 1400, 900);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        primaryStage.setTitle("RSPS Interface Maker");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
