package com.example.integradora;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ResourceBundle;

public class TomarCitaController implements Initializable {

    @FXML private Label nombrePacienteLabel;
    @FXML private Label malestarLabel;
    @FXML private Label fechaLabel;
    @FXML private Label horaLabel;
    @FXML private TextArea observacionesArea;

    private int idCuenta;
    private int idEmpleado;
    private int idCita;
    private int idPaciente;

    private static final String API_URL = "https://yjfzidepdnusqufayeli.supabase.co/rest/v1/";
    private static final String API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InlqZnppZGVwZG51c3F1ZmF5ZWxpIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTEyNTUyMDMsImV4cCI6MjA2NjgzMTIwM30.Kq7hVKEWbYWObRpiZ2yxgSWpRQS3V0MBGRIZxji09lU";  // no modifiques

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {}

    public void setDatos(int idCuenta, int idEmpleado, int idCita, int idPaciente, String nombre, String malestar, String fecha, String hora) {
        this.idCuenta = idCuenta;
        this.idEmpleado = idEmpleado;
        this.idCita = idCita;
        this.idPaciente = idPaciente;
        nombrePacienteLabel.setText(nombre);
        malestarLabel.setText(malestar);
        fechaLabel.setText(fecha);
        horaLabel.setText(hora);
    }

    public void finalizarCita(ActionEvent event) {
        try {
            String observaciones = observacionesArea.getText();
            String body = String.format("{\"estado\": \"ATENDIDA\", \"observaciones\": \"%s\"}", escapeJson(observaciones));

            URL url = new URL(API_URL + "cita?id_cita=eq." + idCita);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("PATCH");
            conn.setRequestProperty("apikey", API_KEY);
            conn.setRequestProperty("Authorization", "Bearer " + API_KEY);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Prefer", "return=representation,resolution=merge-duplicates"); // 💡 evita duplicado
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = body.getBytes(StandardCharsets.UTF_8);
                os.write(input);
            }

            int response = conn.getResponseCode();
            if (response == 200 || response == 204) {
                mostrarAlerta("Cita finalizada", "La cita ha sido marcada como ATENDIDA.");
                irAVistaPerfil(event);
            } else {
                mostrarAlerta("Error", "Código: " + response);
            }

        } catch (Exception e) {
            mostrarAlerta("Excepción", e.getMessage());
        }
    }

    public void irAVistaPerfil(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("PerfilDoctor.fxml"));
            Parent root = loader.load();

            if (loader.getController() instanceof ConCuenta) {
                ((ConCuenta) loader.getController()).setIdCuenta(idCuenta);
            }

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();

        } catch (Exception e) {
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

    private String escapeJson(String texto) {
        return texto.replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }
}
