package com.inkwell.app;

import com.inkwell.database.DatabaseInitializer;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class InkwellApplication extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        DatabaseInitializer databaseInitializer = new DatabaseInitializer();
        databaseInitializer.initialize();

        FXMLLoader loader = new FXMLLoader(
                InkwellApplication.class.getResource("/fxml/main-view.fxml")
        );
        Parent root = loader.load();

        Scene scene = new Scene(root, 980, 640);
        scene.getStylesheets().add(
                InkwellApplication.class.getResource("/styles/theme.css").toExternalForm()
        );

        stage.setTitle("Inkwell");
        stage.setScene(scene);
        stage.setMinWidth(860);
        stage.setMinHeight(560);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
