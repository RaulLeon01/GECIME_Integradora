package com.example.integradora;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class DatabaseConnection {

    private static final String API_URL = "https://yjfzidepdnusqufayeli.supabase.co/rest/v1/";
    private static final String API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InlqZnppZGVwZG51c3F1ZmF5ZWxpIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTEyNTUyMDMsImV4cCI6MjA2NjgzMTIwM30.Kq7hVKEWbYWObRpiZ2yxgSWpRQS3V0MBGRIZxji09lU";
    public static Paciente obtenerPacientePorMatricula(String matricula) {
        try {
            URL url = new URL(API_URL + "paciente?matricula=eq." + matricula);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("apikey", API_KEY);
            conn.setRequestProperty("Authorization", "Bearer " + API_KEY);

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String inputLine;

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                JSONArray jsonArray = new JSONArray(response.toString());
                if (jsonArray.length() > 0) {
                    JSONObject obj = jsonArray.getJSONObject(0);
                    return new Paciente(
                            obj.getString("nombre"),
                            obj.getString("matricula"),
                            obj.getString("correo"),
                            obj.getString("curp"),
                            obj.getString("telefono"),
                            obj.getString("telefono_emergencia"),
                            obj.getString("fecha_nac")
                    );
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }



    public static int obtenerSiguienteIdDisponible() {
        try {
            URL url = new URL(API_URL + "cuenta?select=id_cuenta&order=id_cuenta.asc");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("apikey", API_KEY);
            conn.setRequestProperty("Authorization", "Bearer " + API_KEY);

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String inputLine;

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                JSONArray jsonArray = new JSONArray(response.toString());
                if (jsonArray.length() == 0) return 1;

                int maxId = 0;
                for (int i = 0; i < jsonArray.length(); i++) {
                    int id = jsonArray.getJSONObject(i).getInt("id_cuenta");
                    if (id > maxId) maxId = id;
                }

                boolean[] ocupados = new boolean[maxId + 2];
                for (int i = 0; i < jsonArray.length(); i++) {
                    int id = jsonArray.getJSONObject(i).getInt("id_cuenta");
                    if (id > 0 && id < ocupados.length) {
                        ocupados[id] = true;
                    }
                }

                for (int i = 1; i < ocupados.length; i++) {
                    if (!ocupados[i]) {
                        return i;
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    public static boolean insertarCuentaConId(int idCuenta, String usuario, String contrasena, String rol) {
        try {
            URL url = new URL(API_URL + "cuenta");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("apikey", API_KEY);
            conn.setRequestProperty("Authorization", "Bearer " + API_KEY);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            JSONObject json = new JSONObject();
            json.put("id_cuenta", idCuenta);
            json.put("usuario", usuario);
            json.put("contrasena", contrasena);
            json.put("rol", rol);
            json.put("estado", "ACTIVO");

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = json.toString().getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = conn.getResponseCode();
            return responseCode == 201 || responseCode == 200;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean insertarPaciente(String nombre, String curp, String apellidos, String telefono,
                                           String matricula, String fechaNacimiento, String email,
                                           String telefonoEmergencia, String contrasena, int idCuenta) {

        try {
            URL url = new URL(API_URL + "paciente");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("apikey", API_KEY);
            conn.setRequestProperty("Authorization", "Bearer " + API_KEY);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            JSONObject json = new JSONObject();
            json.put("nombre", nombre + " " + apellidos);
            json.put("matricula", matricula);
            json.put("correo", email);
            json.put("curp", curp);
            json.put("telefono", telefono);
            json.put("fecha_nac", fechaNacimiento);
            json.put("telefono_emergencia", telefonoEmergencia);
            json.put("contrasena", contrasena);
            json.put("id_cuenta", idCuenta);
            json.put("estado", "ACTIVO");

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = json.toString().getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = conn.getResponseCode();
            return responseCode == 201 || responseCode == 200;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void mostrarAlerta(String titulo, String mensaje) {
        Platform.runLater(() -> {
            Alert alerta = new Alert(Alert.AlertType.INFORMATION);
            alerta.setTitle(titulo);
            alerta.setHeaderText(null);
            alerta.setContentText(mensaje);
            alerta.showAndWait();
        });
    }
}
