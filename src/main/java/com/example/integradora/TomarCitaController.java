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
import org.json.JSONArray;
import org.json.JSONObject;

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

    private int idCuenta, idEmpleado, idCita, idPaciente;

    private static final String API_URL = "https://yjfzidepdnusqufayeli.supabase.co/rest/v1/";
    private static final String API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InlqZnppZGVwZG51c3F1ZmF5ZWxpIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTEyNTUyMDMsImV4cCI6MjA2NjgzMTIwM30.Kq7hVKEWbYWObRpiZ2yxgSWpRQS3V0MBGRIZxji09lU"; // clave completa

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
            JSONObject citaOriginal = obtenerCitaPorId(idCita);
            if (citaOriginal == null) {
                mostrarAlerta("Error", "No se encontró la cita.");
                return;
            }

            // Guardamos todos los campos y los modificamos
            citaOriginal.put("estado", "ATENDIDA");
            citaOriginal.put("observaciones", observacionesArea.getText());

            // Eliminamos la cita original
            eliminarCita(idCita);

            // Insertamos la nueva cita con observaciones y estado actualizado
            insertarCita(citaOriginal);

            mostrarAlerta("Cita finalizada", "La cita fue marcada como ATENDIDA correctamente.");
            irAVistaPerfil(event);

        } catch (Exception e) {
            mostrarAlerta("Error al finalizar cita", e.getMessage());
        }
    }

    private JSONObject obtenerCitaPorId(int idCita) throws Exception {
        URL url = new URL(API_URL + "cita?id_cita=eq." + idCita);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestProperty("apikey", API_KEY);
        conn.setRequestProperty("Authorization", "Bearer " + API_KEY);
        conn.setRequestProperty("Accept", "application/json");

        if (conn.getResponseCode() == 200) {
            StringBuilder response = new StringBuilder();
            try (var reader = new java.io.BufferedReader(new java.io.InputStreamReader(conn.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) response.append(line);
            }
            JSONArray array = new JSONArray(response.toString());
            return array.length() > 0 ? array.getJSONObject(0) : null;
        }
        return null;
    }

    private void eliminarCita(int idCita) throws Exception {
        URL url = new URL(API_URL + "cita?id_cita=eq." + idCita);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("DELETE");
        conn.setRequestProperty("apikey", API_KEY);
        conn.setRequestProperty("Authorization", "Bearer " + API_KEY);
        conn.connect();
        conn.getResponseCode(); // Esperamos respuesta
    }

    private void insertarCita(JSONObject cita) throws Exception {
        URL url = new URL(API_URL + "cita");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("apikey", API_KEY);
        conn.setRequestProperty("Authorization", "Bearer " + API_KEY);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = cita.toString().getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        int responseCode = conn.getResponseCode();
        if (responseCode != 201) {
            throw new RuntimeException("Error al insertar nueva cita. Código: " + responseCode);
        }
    }

    public void irAVistaPerfil(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("EnCurso.fxml"));
            Parent root = loader.load();

            if (loader.getController() instanceof ConCuenta controller) {
                controller.setIdCuenta(idCuenta);
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
}
