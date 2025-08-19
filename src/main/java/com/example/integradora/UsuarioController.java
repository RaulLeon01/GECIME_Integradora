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
import java.util.ResourceBundle;

public class UsuarioController implements Initializable, ConCuenta {

    private static final String API_URL = "https://yjfzidepdnusqufayeli.supabase.co/rest/v1/";
    private static final String API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InlqZnppZGVwZG51c3F1ZmF5ZWxpIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTEyNTUyMDMsImV4cCI6MjA2NjgzMTIwM30.Kq7hVKEWbYWObRpiZ2yxgSWpRQS3V0MBGRIZxji09lU";
    private int idCuenta;

    @FXML private Label nombreLabel;
    @FXML private Label matriculaLabel;
    @FXML private Label emailLabel;
    @FXML private Label curpLabel;
    @FXML private Label telefonoLabel;
    @FXML private Label telefonoEmergenciaLabel;
    @FXML private Label tipoLabel;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Espera setIdCuenta
    }

    @Override
    public void setIdCuenta(int idCuenta) {
        this.idCuenta = idCuenta;
        System.out.println("✅ UsuarioController recibió idCuenta: " + idCuenta);
        cargarDatosPaciente();
    }

    private void cargarDatosPaciente() {
        try {
            String query = String.format("paciente?id_cuenta=eq.%d&select=*", idCuenta);
            URL url = new URL(API_URL + query);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("apikey", API_KEY);
            conn.setRequestProperty("Authorization", "Bearer " + API_KEY);

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder json = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) json.append(line);
                reader.close();

                JSONArray jsonArray = new JSONArray(json.toString());
                if (!jsonArray.isEmpty()) {
                    JSONObject paciente = jsonArray.getJSONObject(0);
                    nombreLabel.setText(paciente.optString("nombre", ""));
                    matriculaLabel.setText(paciente.optString("matricula", ""));
                    emailLabel.setText(paciente.optString("correo", ""));
                    curpLabel.setText(paciente.optString("curp", ""));
                    telefonoLabel.setText(paciente.optString("telefono", ""));
                    telefonoEmergenciaLabel.setText(paciente.optString("telefono_emergencia", ""));
                    tipoLabel.setText(paciente.optString("rol", ""));
                } else {
                    System.out.println("⚠️ No se encontró paciente con idCuenta=" + idCuenta);
                }
            } else {
                System.out.println("❌ Error al consultar paciente. Código: " + responseCode);
            }
        } catch (Exception e) {
            System.err.println("❌ Error al cargar datos del paciente:");
            e.printStackTrace();
        }
    }

    private void cambiarVista(String fxml, ActionEvent event) {
        try {
            System.out.println("🔁 Cambiando a: " + fxml);
            System.out.println("🎯 idCuenta actual: " + idCuenta);

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/integradora/" + fxml));
            Parent root = loader.load();

            Object controller = loader.getController();
            System.out.println("🎮 Controller: " + controller.getClass());

            if (controller instanceof ConCuenta cuentaController) {
                System.out.println("✅ Controller implementa ConCuenta");
                cuentaController.setIdCuenta(idCuenta);
            } else {
                System.err.println("❌ Controller NO implementa ConCuenta");
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

    // Métodos vinculados a los botones del menú lateral
    @FXML private void irAVistaAgendar(ActionEvent event) { cambiarVista("AgendarCita.fxml", event); }
    @FXML private void irAVistaHistorial(ActionEvent event) { cambiarVista("HistorialCitas.fxml", event); }
    @FXML private void irAVistaAgendadas(ActionEvent event) { cambiarVista("CitasAgendadas.fxml", event); }
    @FXML private void irAVistaPerfil(ActionEvent event) { cambiarVista("Usuario.fxml", event); }
    @FXML private void cerrarSesion(ActionEvent event) {
        cambiarVista("Sesion.fxml", event);
    }
}
