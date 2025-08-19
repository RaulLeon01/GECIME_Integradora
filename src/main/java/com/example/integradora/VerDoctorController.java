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

public class VerDoctorController implements Initializable {

    @FXML private Label nombreLabel;
    @FXML private Label rolLabel;
    @FXML private Label correoLabel;
    @FXML private Label especialidadLabel;
    @FXML private Label consultorioLabel;

    private static final String API_URL = "https://yjfzidepdnusqufayeli.supabase.co/rest/v1/";
    private static final String API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InlqZnppZGVwZG51c3F1ZmF5ZWxpIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTEyNTUyMDMsImV4cCI6MjA2NjgzMTIwM30.Kq7hVKEWbYWObRpiZ2yxgSWpRQS3V0MBGRIZxji09lU";

    private int idEmpleado = -1;

    @Override
    public void initialize(java.net.URL url, ResourceBundle rb) {
        rolLabel.setText("Doctor");
        nombreLabel.setText("—");
        correoLabel.setText("—");
        especialidadLabel.setText("—");
        consultorioLabel.setText("—");
    }

    // Llamado desde DoctoresController
    public void setIdEmpleado(int idEmpleado) {
        this.idEmpleado = idEmpleado;
        cargarDetalle();
    }

    private void cargarDetalle() {
        if (idEmpleado <= 0) return;
        try {
            // select=* para evitar 400 si alguna columna no existe
            String endpoint = API_URL + "empleado?"
                    + "select=" + enc("*")
                    + "&id_empleado=eq." + enc(String.valueOf(idEmpleado))
                    + "&limit=1";

            HttpURLConnection conn = (HttpURLConnection) new URL(endpoint).openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("apikey", API_KEY);
            conn.setRequestProperty("Authorization", "Bearer " + API_KEY);
            conn.setRequestProperty("Accept", "application/json");

            int code = conn.getResponseCode();
            String body;
            try (BufferedReader br = new BufferedReader(new InputStreamReader(
                    code < 400 ? conn.getInputStream() : conn.getErrorStream(),
                    StandardCharsets.UTF_8
            ))) {
                StringBuilder sb = new StringBuilder();
                for (String line; (line = br.readLine()) != null; ) sb.append(line);
                body = sb.toString();
            }
            if (code >= 400) {
                System.err.println("VerDoctor GET empleado -> " + code + ": " + body);
                return;
            }

            JSONArray arr = new JSONArray(body);
            if (arr.length() == 0) return;
            JSONObject d = arr.getJSONObject(0);

            // Solo estos campos (con posibles alias)
            String nombre = pick(d, "nombre");
            String correo = pick(d, "correo", "email", "correo_electronico");
            String esp    = pick(d, "especialidad", "area_especialidad");
            String cons   = pick(d, "consultorio", "consultorio_nombre", "consultorio_id");

            nombreLabel.setText(blank(nombre) ? "—" : nombre);
            correoLabel.setText(blank(correo) ? "—" : correo);
            especialidadLabel.setText(blank(esp) ? "—" : esp);
            consultorioLabel.setText(blank(cons) ? "—" : cons);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Botones
    @FXML private void onVolverClick(ActionEvent e) {
        cambiarVista("/com/example/integradora/Doctores.fxml", e);
    }
    @FXML private void onModificarInfoClick(ActionEvent e) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/integradora/ModificarDoctor.fxml"));
            Parent root = loader.load();
            Object c = loader.getController();
            try { c.getClass().getMethod("setIdEmpleado", int.class).invoke(c, idEmpleado); } catch (NoSuchMethodException ignore) {}
            Stage stage = (Stage) ((Node) e.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Modificar doctor");
            stage.show();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    // Utils
    private void cambiarVista(String ruta, ActionEvent e) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(ruta));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) e.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("GECIME");
            stage.show();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private String pick(JSONObject o, String... keys) {
        for (String k : keys) {
            String v = o.optString(k, "");
            if (!blank(v)) return v;
        }
        return "";
    }
    private boolean blank(String s) { return s == null || s.trim().isEmpty(); }
    private String enc(String s) {
        try { return URLEncoder.encode(s, "UTF-8"); }
        catch (Exception e) { return s; }
    }
}
