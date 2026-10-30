package com.todo;

import com.gluonhq.attach.util.Platform;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public class App extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            URL fxmlLocation = getClass().getResource("/com/todo/main-view.fxml");
            if (fxmlLocation == null) {
                throw new IOException("Cannot find FXML file: /com/todo/main-view.fxml");
            }

            FXMLLoader loader = new FXMLLoader(fxmlLocation);
            Parent root = loader.load();

            Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
            Scene scene;

            boolean isMobile = Platform.isAndroid() || Platform.isIOS();
            if (isMobile) {
                scene = new Scene(root, bounds.getWidth(), bounds.getHeight());
            } else {
                scene = new Scene(root, Math.min(900, bounds.getWidth() - 40), Math.min(720, bounds.getHeight() - 40));
                primaryStage.setMinWidth(400);
                primaryStage.setMinHeight(500);
            }

            URL cssLocation = getClass().getResource("/com/todo/styles.css");
            if (cssLocation != null) {
                scene.getStylesheets().add(cssLocation.toExternalForm());
            }

            primaryStage.setTitle("TaskFlow - Todo & Task Manager");
            primaryStage.setScene(scene);
            primaryStage.show();
        } catch (Exception e) {
            System.err.println("Failed to start application: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}

