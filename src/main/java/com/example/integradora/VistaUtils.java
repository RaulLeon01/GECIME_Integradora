package com.example.integradora;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.IOException;

public class VistaUtils {

    public static void cambiarVista(String fxmlNombre) {
        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(VistaUtils.class.getResource(fxmlNombre));
                Scene scene = new Scene(loader.load());
                Stage stage = (Stage) Stage.getWindows().stream().filter(Window::isShowing).findFirst().get();
                stage.setScene(scene);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}
