package com.example.integradora;

import javafx.event.ActionEvent;
import javafx.fxml.*;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ResourceBundle;

public class ModificarInfoController implements Initializable {

    // UI (coinciden con el FXML)
    @FXML private Label     matriculaLabel;  // paciente.matricula (solo lectura)
    @FXML private TextField nombreField;     // paciente.nombre
    @FXML private TextField telefonoField;   // paciente.telefono (10 dígitos)

    // API
    private static final String API_URL = "https://yjfzidepdnusqufayeli.supabase.co/rest/v1/";
    private static final String API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InlqZnppZGVwZG51c3F1ZmF5ZWxpIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTEyNTUyMDMsImV4cCI6MjA2NjgzMTIwM30.Kq7hVKEWbYWObRpiZ2yxgSWpRQS3V0MBGRIZxji09lU";
    private final HttpClient http = HttpClient.newHttpClient();

    // Estado
    private int idPaciente = -1;

    // A dónde regresar
    private static final String BACK_FXML = "/com/example/integradora/VerInformacion.fxml";

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Solo números y máximo 10 en teléfono mientras escribe
        telefonoField.setTextFormatter(new TextFormatter<>(c ->
                c.getControlNewText().matches("\\d{0,10}") ? c : null
        ));
    }

    /** Llamar justo después de cargar el FXML */
    public void setIdPaciente(int idPaciente) {
        this.idPaciente = idPaciente;
        cargarPaciente();
    }

    // ================== Carga ==================

    private void cargarPaciente() {
        if (idPaciente <= 0) return;
        try {
            String select = enc("id_paciente,matricula,nombre,telefono");
            String url = API_URL + "paciente?select=" + select + "&id_paciente=eq." + idPaciente + "&limit=1";

            HttpURLConnection conn = crearGET(new URL(url));
            String body = leer(conn);
            JSONArray arr = new JSONArray(body);
            if (arr.isEmpty()) { alerta("Aviso", "Paciente no encontrado."); return; }

            JSONObject p = arr.getJSONObject(0);
            matriculaLabel.setText(p.optString("matricula", ""));
            nombreField.setText(p.optString("nombre", ""));
            telefonoField.setText(p.optString("telefono", ""));

        } catch (Exception ex) {
            ex.printStackTrace();
            alerta("Error", "No se pudieron cargar los datos del paciente.");
        }
    }

    // ================== Guardar ==================

    @FXML
    private void onGuardarClick(ActionEvent e) {
        String nombre   = safe(nombreField.getText());
        String telefono = safe(telefonoField.getText());

        if (nombre.isEmpty()) { alerta("Validación", "El nombre es obligatorio."); return; }
        if (telefono.isEmpty()) { alerta("Validación", "El teléfono es obligatorio."); return; }
        if (!telefono.matches("\\d{10}")) {
            alerta("Validación", "El teléfono debe tener exactamente 10 dígitos.");
            return;
        }

        try {
            JSONObject patch = new JSONObject()
                    .put("nombre", nombre)
                    .put("telefono", telefono);

            String url = API_URL + "paciente?id_paciente=eq." + idPaciente;

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .method("PATCH", HttpRequest.BodyPublishers.ofString(patch.toString()))
                    .header("apikey", API_KEY)
                    .header("Authorization", "Bearer " + API_KEY)
                    .header("Content-Type", "application/json")
                    .header("Prefer", "return=minimal")
                    .build();

            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());

            if (resp.statusCode() != 204) {
                System.err.println("PATCH paciente -> " + resp.statusCode() + " : " + resp.body());
                alerta("Error", "No se pudo actualizar la información.\nCódigo " + resp.statusCode());
                return;
            }

            alerta("Éxito", "Información actualizada correctamente.");
            // Volver a VerInformacion con el mismo idPaciente
            volverConId(e);

        } catch (Exception ex) {
            ex.printStackTrace();
            alerta("Error", "Fallo al actualizar la información.");
        }
    }

    @FXML
    private void onCancelarClick(ActionEvent e) {
        volverConId(e);
    }

    private void volverConId(ActionEvent e) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(BACK_FXML));
            Parent root = loader.load();

            Object controller = loader.getController();
            try {
                controller.getClass().getMethod("setIdPaciente", int.class).invoke(controller, idPaciente);
            } catch (NoSuchMethodException ignored) {}

            Stage stage = (Stage) ((Node) e.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Información del Paciente");
            stage.show();
        } catch (Exception ex) {
            ex.printStackTrace();
            alerta("Error", "No se pudo regresar a la vista anterior.");
        }
    }

    // ================== Utils HTTP/UI ==================

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

    private String safe(String s) { return s == null ? "" : s.trim(); }

    private void alerta(String titulo, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(titulo);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}
