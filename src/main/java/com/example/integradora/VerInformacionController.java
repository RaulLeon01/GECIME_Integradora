package com.example.integradora;

import javafx.event.ActionEvent;
import javafx.fxml.*;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.ResourceBundle;

public class VerInformacionController implements Initializable {

    // UI (coinciden con el FXML)
    @FXML private Label nombreLabel;
    @FXML private Label matriculaLabel;           // paciente.matricula
    @FXML private Label emailLabel;               // paciente.correo
    @FXML private Label curpLabel;                // paciente.curp
    @FXML private Label telefonoEmergenciaLabel;  // paciente.telefono_emergencia
    @FXML private Label telefonoLabel;            // paciente.telefono
    @FXML private Label tipoLabel;


    // API
    private static final String API_URL = "https://yjfzidepdnusqufayeli.supabase.co/rest/v1/";
    private static final String API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InlqZnppZGVwZG51c3F1ZmF5ZWxpIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTEyNTUyMDMsImV4cCI6MjA2NjgzMTIwM30.Kq7hVKEWbYWObRpiZ2yxgSWpRQS3V0MBGRIZxji09lU";

    // Estado
    private int idPaciente = -1;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // se carga al llamar setIdPaciente(...)
    }

    /** Llamar desde PacientesController antes de mostrar la vista */
    public void setIdPaciente(int idPaciente) {
        this.idPaciente = idPaciente;
        cargarInfoPaciente();
    }

    // ================== Carga de datos ==================

    private void cargarInfoPaciente() {
        if (idPaciente <= 0) {
            alerta("Aviso", "Paciente no especificado.");
            return;
        }
        try {
            // Traer SOLO de paciente (según tu esquema actual)
            String select = enc("id_paciente,nombre,matricula,correo,curp,telefono,fecha_nac,telefono_emergencia,estado,rol");
            String urlPac = API_URL + "paciente?select=" + select
                    + "&id_paciente=eq." + idPaciente + "&limit=1";

            JSONObject pac = getOne(urlPac);
            if (pac == null) { alerta("Aviso", "No se encontró el paciente."); return; }

            // Mapear a UI
            nombreLabel.setText(nullToEmpty(pac.optString("nombre", "")));
            matriculaLabel.setText(nullToEmpty(pac.optString("matricula", "")));
            emailLabel.setText(nullToEmpty(pac.optString("correo", "")));
            curpLabel.setText(nullToEmpty(pac.optString("curp", "")));
            telefonoEmergenciaLabel.setText(nullToEmpty(pac.optString("telefono_emergencia", "")));
            telefonoLabel.setText(nullToEmpty(pac.optString("telefono", "")));
            tipoLabel.setText(nullToEmpty(pac.optString("rol", "")));

            System.out.println("✅ Info paciente cargada (id=" + idPaciente + ").");

        } catch (Exception ex) {
            ex.printStackTrace();
            alerta("Error", "No se pudo cargar la información del paciente.");
        }
    }

    // ================== Acciones ==================

    @FXML
    private void onVolverClick(ActionEvent e) {
        cambiarVista("/com/example/integradora/Pacientes.fxml", e);
    }

    @FXML
    private void onModificarInfoClick(ActionEvent e) {
        // Abre ModificarInformacion.fxml con el id_paciente actual
        abrirVistaConId("/com/example/integradora/ModificarInformacion.fxml",
                "setIdPaciente", idPaciente, e);
    }

    // ================== HTTP helpers ==================

    private JSONObject getOne(String url) throws Exception {
        HttpURLConnection conn = crearGET(new URL(url));
        String body = leer(conn);
        JSONArray arr = new JSONArray(body);
        return arr.isEmpty() ? null : arr.getJSONObject(0);
    }

    private HttpURLConnection crearGET(URL url) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("apikey", API_KEY);
        conn.setRequestProperty("Authorization", "Bearer " + API_KEY);
        conn.setRequestProperty("Accept", "application/json");
        return conn;
    }

    private String leer(HttpURLConnection conn) throws Exception {
        int code = conn.getResponseCode();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                code < 400 ? conn.getInputStream() : conn.getErrorStream(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            for (String line; (line = br.readLine()) != null; ) sb.append(line);
            if (code >= 400) throw new RuntimeException("HTTP " + code + " -> " + sb);
            return sb.toString();
        }
    }

    private String enc(String s) {
        try { return URLEncoder.encode(s, StandardCharsets.UTF_8.toString()); }
        catch (Exception e) { return s; }
    }

    // ================== UI/Navegación ==================

    private void cambiarVista(String rutaFXML, ActionEvent e) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(rutaFXML));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) e.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("GECIME");
            stage.show();
        } catch (Exception ex) {
            ex.printStackTrace();
            alerta("Error", "No se pudo cambiar de vista.");
        }
    }

    private void abrirVistaConId(String rutaFXML, String metodoSetter, int id, ActionEvent e) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(rutaFXML));
            Parent root = loader.load();

            Object controller = loader.getController();
            try {
                controller.getClass().getMethod(metodoSetter, int.class).invoke(controller, id);
            } catch (NoSuchMethodException nsme) {
                System.out.println("ℹ La vista " + rutaFXML + " no expone " + metodoSetter + "(int). Se abrirá sin parámetro.");
            }

            Stage stage = (Stage) ((Node) e.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Modificar Información del Paciente");
            stage.show();
        } catch (Exception ex) {
            ex.printStackTrace();
            alerta("Error", "No se pudo abrir ModificarInformacion.");
        }
    }

    // ================== Helpers ==================

    private String formatearFecha(String f) {
        if (f == null || f.isBlank()) return "";
        // si viene como "YYYY-MM-DD..." corta a 10
        return f.length() >= 10 ? f.substring(0, 10) : f;
    }

    private String nullToEmpty(String s) { return (s == null) ? "" : s; }

    private void alerta(String titulo, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(titulo);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}
