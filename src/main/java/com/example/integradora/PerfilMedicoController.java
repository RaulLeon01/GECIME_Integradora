package com.example.integradora;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ResourceBundle;

public class PerfilMedicoController implements Initializable, ConCuenta {

    @FXML private Label nombreLabel;
    @FXML private Label matriculaLabel;
    @FXML private Label emailLabel;
    @FXML private Label especialidadLabel;
    @FXML private Label consultorioLabel;
    @FXML private Label areaMedicaLabel;

    private int idCuenta;

    private static final String API_URL = "https://yjfzidepdnusqufayeli.supabase.co/rest/v1/";
    private static final String API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InlqZnppZGVwZG51c3F1ZmF5ZWxpIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTEyNTUyMDMsImV4cCI6MjA2NjgzMTIwM30.Kq7hVKEWbYWObRpiZ2yxgSWpRQS3V0MBGRIZxji09lU";

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Esperando el idCuenta antes de cargar datos
    }

    @Override
    public void setIdCuenta(int idCuenta) {
        this.idCuenta = idCuenta;
        System.out.println("👨‍⚕️ ID del médico recibido: " + idCuenta);
        cargarDatosDoctor();
    }

    private void cargarDatosDoctor() {
        try {
            String query = API_URL + "empleado?id_cuenta=eq." + idCuenta;
            URL url = new URL(query);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("apikey", API_KEY);
            conn.setRequestProperty("Authorization", "Bearer " + API_KEY);

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder respuesta = new StringBuilder();
            String linea;
            while ((linea = reader.readLine()) != null) respuesta.append(linea);
            reader.close();

            JSONArray array = new JSONArray(respuesta.toString());
            if (!array.isEmpty()) {
                JSONObject doctor = array.getJSONObject(0);

                nombreLabel.setText(doctor.optString("nombre", ""));
                matriculaLabel.setText(doctor.optString("usuario", ""));
                emailLabel.setText(doctor.optString("correo", ""));
                especialidadLabel.setText(doctor.optString("especialidad", ""));
                consultorioLabel.setText(doctor.optString("consultorio", ""));
                areaMedicaLabel.setText(doctor.optString("tipo_area", ""));
            } else {
                System.out.println("❌ No se encontró el doctor con id_cuenta=" + idCuenta);
            }

        } catch (Exception e) {
            System.err.println("❌ Error al cargar datos del médico:");
            e.printStackTrace();
        }
    }

    private void cambiarVista(String fxml, ActionEvent event) {
        try {
            System.out.println("🔁 Cambiando a: " + fxml);
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/integradora/" + fxml));
            Parent root = loader.load();

            Object controller = loader.getController();
            if (controller instanceof ConCuenta cuentaController) {
                cuentaController.setIdCuenta(idCuenta);
            }

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("GECIME");
            stage.show();

        } catch (Exception e) {
            System.err.println("❌ Error al cambiar vista:");
            e.printStackTrace();
        }
    }

    // Botones del menú lateral
    @FXML private void irAVistaHistorialPacientes(ActionEvent event) {
        cambiarVista("HistorialPacientes.fxml", event);
    }

    @FXML private void irAVistaEnCurso(ActionEvent event) {
        cambiarVista("EnCurso.fxml", event);
    }

    @FXML private void irAVistaHistorial(ActionEvent event) {
        cambiarVista("HistorialDoctor.fxml", event);
    }

    @FXML private void cerrarSesion(ActionEvent event) {
        cambiarVista("Sesion.fxml", event);
    }

    @FXML private void irAVistaPerfil(ActionEvent event) {
        cambiarVista("PerfilDoctor.fxml", event);
    }
    /*@FXML
    private void cerrarSesion(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/integradora/Sesion.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }*/

}
