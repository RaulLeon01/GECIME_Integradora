package com.example.integradora;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

public class IngresarTokenController {

    @FXML
    private TextField tokenField;

    @FXML
    private PasswordField nuevaContrasenaField;

    @FXML
    private PasswordField confirmarContrasenaField;

    private String correo; // 🔑 Esta variable es necesaria para recibir el correo

    // ✅ Método para recibir el correo desde la vista anterior
    public void setCorreo(String correo) {
        this.correo = correo;
    }

    @FXML
    private void onActualizarClick() {
        String token = tokenField.getText().trim();
        String nuevaContrasena = nuevaContrasenaField.getText().trim();
        String confirmarContrasena = confirmarContrasenaField.getText().trim();

        if (token.isEmpty() || nuevaContrasena.isEmpty() || confirmarContrasena.isEmpty()) {
            mostrarAlerta("Error", "Debe llenar todos los campos.");
            return;
        }

        if (!nuevaContrasena.equals(confirmarContrasena)) {
            mostrarAlerta("Error", "Las contraseñas no coinciden.");
            return;
        }

        TokenData tokenData = PasswordRecoveryConnection.validarTokenDisponible(token);
        if (tokenData == null) {
            mostrarAlerta("Error", "El token no es válido o ya fue usado.");
            return;
        }

        if (!PasswordRecoveryConnection.tokenVigente(tokenData.getFechaSolicitud())) {
            mostrarAlerta("Error", "El token ha expirado.");
            return;
        }

        boolean actualizado = PasswordRecoveryConnection.actualizarContrasena(tokenData.getIdCuenta(), nuevaContrasena);
        if (actualizado) {
            PasswordRecoveryConnection.marcarTokenComoUsado(token);
            mostrarAlerta("Éxito", "La contraseña se actualizó correctamente.");
            volverAVistaSesion();
        } else {
            mostrarAlerta("Error", "No se pudo actualizar la contraseña.");
        }
    }

    @FXML
    private void onCancelarClick() {
        volverAVistaSesion();
    }

    private void volverAVistaSesion() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("Sesion.fxml"));
            Scene scene = new Scene(loader.load());
            Stage stage = (Stage) tokenField.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Iniciar Sesión");
            stage.show();
        } catch (IOException e) {
            mostrarAlerta("Error", "No se pudo abrir la vista de sesión.");
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
}
