package com.example.integradora;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import javafx.event.ActionEvent;
import javafx.scene.Node;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ResourceBundle;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;

public class PerfilAdminController implements Initializable {

    @FXML private Label nombreLabel;
    @FXML private Label usuarioLabel;
    @FXML private Label emailLabel;
    @FXML private Label telefonoLabel;
    @FXML private Label emergenciaLabel;

    private static final String API_URL = "https://yjfzidepdnusqufayeli.supabase.co/rest/v1/";
    private static final String API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InlqZnppZGVwZG51c3F1ZmF5ZWxpIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTEyNTUyMDMsImV4cCI6MjA2NjgzMTIwM30.Kq7hVKEWbYWObRpiZ2yxgSWpRQS3V0MBGRIZxji09lU";

    private static final int ID_CUENTA = 3000; // cuenta.id_cuenta
    private static final int ID_ADMIN = 1;     // administrador.id_admin

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        cargarDatosAdmin();
    }

    private void cargarDatosAdmin() {
        try {
            // cuenta
            String queryCuenta = "cuenta?id_cuenta=eq." + ID_CUENTA;
            HttpURLConnection connCuenta = (HttpURLConnection) new URL(API_URL + queryCuenta).openConnection();
            connCuenta.setRequestMethod("GET");
            connCuenta.setRequestProperty("apikey", API_KEY);
            connCuenta.setRequestProperty("Authorization", "Bearer " + API_KEY);

            StringBuilder responseCuenta = new StringBuilder();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(connCuenta.getInputStream()))) {
                for (String line; (line = r.readLine()) != null; ) responseCuenta.append(line);
            }
            JSONArray arrayCuenta = new JSONArray(responseCuenta.toString());
            if (arrayCuenta.length() > 0) {
                JSONObject cuenta = arrayCuenta.getJSONObject(0);
                nombreLabel.setText("Rem");
                usuarioLabel.setText(cuenta.optString("usuario", "N/D"));
            }

            // administrador
            String queryAdmin = "administrador?id_admin=eq." + ID_ADMIN;
            HttpURLConnection connAdmin = (HttpURLConnection) new URL(API_URL + queryAdmin).openConnection();
            connAdmin.setRequestMethod("GET");
            connAdmin.setRequestProperty("apikey", API_KEY);
            connAdmin.setRequestProperty("Authorization", "Bearer " + API_KEY);

            StringBuilder responseAdmin = new StringBuilder();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(connAdmin.getInputStream()))) {
                for (String line; (line = r.readLine()) != null; ) responseAdmin.append(line);
            }
            JSONArray arrayAdmin = new JSONArray(responseAdmin.toString());
            if (arrayAdmin.length() > 0) {
                JSONObject admin = arrayAdmin.getJSONObject(0);
                emailLabel.setText(admin.optString("correo", "N/D"));
                telefonoLabel.setText(admin.optString("telefono", "N/D"));
                emergenciaLabel.setText(admin.optString("telefono_emergencia", "N/D"));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Navegación genérica
    private void cambiarVista(String fxml, ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/integradora/" + fxml));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Administrador - GECIME");
            stage.show();
        } catch (IOException e) {
            System.err.println("❌ Error al cambiar vista a: " + fxml);
            e.printStackTrace();
        }
    }

    // Botones laterales
    @FXML private void irAVistaAgendar(ActionEvent event)    { cambiarVista("AgendarCitaAdmin.fxml", event); }
    @FXML private void irAVistaAgendadas(ActionEvent event)  { cambiarVista("CitasAgendidasAdmin.fxml", event); }
    @FXML private void irAVistaPacientes(ActionEvent event)  { cambiarVista("Pacientes.fxml", event); }
    @FXML private void irAVistaDoctores(ActionEvent event)   { cambiarVista("Doctores.fxml", event); }
    @FXML private void irAVistaPerfil(ActionEvent event)     { cambiarVista("Admin.fxml", event); }
    @FXML private void cerrarSesion(ActionEvent event)       { cambiarVista("Sesion.fxml", event); }
    @FXML private void irAVistaHistorial(ActionEvent event) { cambiarVista("HistorialDoctor.fxml", event); }



    // ⭐ Nuevo: llevar a Consultorios
    @FXML private void irAVistaConsultorios(ActionEvent event) {
        cambiarVista("Consultorios.fxml", event);
    }
}
