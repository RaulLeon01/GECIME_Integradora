package com.example.integradora;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Random;

public class RecuperarController {

    @FXML
    private TextField correoField;

    private String correo;

    @FXML
    private void onEnviarTokenClick() {
        correo = correoField.getText().trim();

        if (correo.isEmpty()) {
            mostrarAlerta("Error", "Debe ingresar un correo.");
            return;
        }

        if (!PasswordRecoveryConnection.correoExiste(correo)) {
            mostrarAlerta("Error", "El correo no está registrado.");
            return;
        }

        String token = generarToken();

        int idCuenta = PasswordRecoveryConnection.obtenerIdCuentaPorCorreo(correo);

        if (PasswordRecoveryConnection.guardarToken(idCuenta, token)) {
            EmailSender.enviarCorreo(correo, "Código de Recuperación GECIME", "Su código es: " + token);

            // Cambio de vista automático
            abrirVistaToken();

        } else {
            mostrarAlerta("Error", "No se pudo guardar el token.");
        }
    }

    private String generarToken() {
        Random random = new Random();
        int numero = 1000 + random.nextInt(9000); // 4 dígitos
        return String.valueOf(numero);
    }

    private void abrirVistaToken() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("TokenView.fxml"));
            Scene scene = new Scene(loader.load());

            // Pasar el correo a la nueva vista
            IngresarTokenController controller = loader.getController();
            controller.setCorreo(correo);


            Stage stage = (Stage) correoField.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Validar Token");
            stage.show();

        } catch (IOException e) {
            mostrarAlerta("Error", "No se pudo abrir la vista de token.");
            e.printStackTrace();
        }
    }

    private void mostrarAlerta(String titulo, String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.INFORMATION);
        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }
    @FXML
    private void onVolverClick() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("Sesion.fxml")); // Asegúrese que el nombre sea correcto
            Scene scene = new Scene(loader.load());

            Stage stage = (Stage) correoField.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Inicio de Sesión");
            stage.show();

        } catch (IOException e) {
            mostrarAlerta("Error", "No se pudo abrir la vista de sesión.");
            e.printStackTrace();
        }
    }

}
