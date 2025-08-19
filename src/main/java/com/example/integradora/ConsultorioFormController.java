package com.example.integradora;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.event.ActionEvent;
import javafx.scene.Node;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.net.URL;
import java.util.ResourceBundle;

public class ConsultorioFormController implements Initializable {

    @FXML private TextField nombreConsultorioField;
    @FXML private TextField ubicacionField;

    private static final String API_URL = "https://yjfzidepdnusqufayeli.supabase.co/rest/v1/";
    private static final String API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InlqZnppZGVwZG51c3F1ZmF5ZWxpIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTEyNTUyMDMsImV4cCI6MjA2NjgzMTIwM30.Kq7hVKEWbYWObRpiZ2yxgSWpRQS3V0MBGRIZxji09lU";
    private final HttpClient http = HttpClient.newHttpClient();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Sin ComboBox ni lógica de estado; se envía "ACTIVO" en el insert.
    }

    @FXML
    private void onGuardarClick(ActionEvent event) {
        String nombre = safe(nombreConsultorioField.getText());
        String ubicacion = safe(ubicacionField.getText());

        if (nombre.isEmpty()) {
            error("El nombre del consultorio es obligatorio.");
            return;
        }

        try {
            // 1) calcular nextId
            int nextId = getNextConsultorioId();

            // 2) intentar insertar con reintentos si hay colisión (409)
            int maxIntentos = 3;
            HttpResponse<String> resp = null;
            for (int intento = 0; intento < maxIntentos; intento++) {
                JSONObject body = new JSONObject()
                        .put("id", nextId)
                        .put("nombre", nombre)
                        .put("ubicacion", ubicacion)
                        .put("estado", "ACTIVO"); // se fija ACTIVO al crear

                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(API_URL + "consultorio"))
                        .header("apikey", API_KEY)
                        .header("Authorization", "Bearer " + API_KEY)
                        .header("Content-Type", "application/json")
                        .header("Prefer", "return=minimal")
                        .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                        .build();

                resp = http.send(req, HttpResponse.BodyHandlers.ofString());

                if (resp.statusCode() == 201) break;                 // éxito
                if (resp.statusCode() == 409) { nextId++; continue; } // id ocupado
                else break; // otro error
            }

            if (resp != null && resp.statusCode() == 201) {
                info("Consultorio registrado correctamente.");
                cambiarVista("/com/example/integradora/Consultorios.fxml", event);
            } else if (resp != null && resp.statusCode() == 409) {
                error("No se pudo registrar por colisiones repetidas de ID. Intente de nuevo.");
            } else {
                int code = (resp == null) ? -1 : resp.statusCode();
                String body = (resp == null) ? "" : resp.body();
                error("No se pudo registrar. Código: " + code + "\n" + body);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            error("Ocurrió un error al registrar el consultorio.");
        }
    }

    @FXML
    private void onCancelarClick(ActionEvent event) {
        cambiarVista("/com/example/integradora/Consultorios.fxml", event);
    }

    // ======= Helpers de ID =======
    /** Lee el mayor id actual y devuelve max+1 (o 1 si no hay registros). */
    private int getNextConsultorioId() throws Exception {
        String select = URLEncoder.encode("id", StandardCharsets.UTF_8);
        String order  = URLEncoder.encode("id.desc", StandardCharsets.UTF_8);
        String url = API_URL + "consultorio?select=" + select + "&order=" + order + "&limit=1";

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("apikey", API_KEY)
                .header("Authorization", "Bearer " + API_KEY)
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() >= 400)
            throw new RuntimeException("HTTP " + resp.statusCode() + " -> " + resp.body());

        JSONArray arr = new JSONArray(resp.body());
        if (arr.length() == 0) return 1;
        int maxId = arr.getJSONObject(0).getInt("id");
        return maxId + 1;
    }

    // ===== Utilidades UI/Navegación =====
    private String safe(String s) { return s == null ? "" : s.trim(); }

    private void info(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Información");
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    private void error(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Error");
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    private void cambiarVista(String fxmlAbs, ActionEvent e) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlAbs));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) e.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Administrador - GECIME");
            stage.show();
        } catch (Exception ex) {
            ex.printStackTrace();
            error("No se pudo cambiar de vista.");
        }
    }
}
