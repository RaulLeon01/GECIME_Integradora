package com.example.integradora;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class BaseDatosRegistro {

    private static final String API_URL = "https://yjfzidepdnusqufayeli.supabase.co/rest/v1/";
    private static final String API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InlqZnppZGVwZG51c3F1ZmF5ZWxpIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTEyNTUyMDMsImV4cCI6MjA2NjgzMTIwM30.Kq7hVKEWbYWObRpiZ2yxgSWpRQS3V0MBGRIZxji09lU";

    public static boolean registrar(String nombre, String apellidos, String curp, String telefono,
                                    String matricula, String fechaNacimiento, String correo,
                                    String telefonoEmergencia, String contrasena) {

        int idCuenta = obtenerSiguienteIdCuenta();
        if (idCuenta == -1) {
            System.out.println("❌ No se pudo generar el ID de cuenta.");
            return false;
        }

        if (!insertarCuenta(idCuenta, matricula, contrasena)) {
            System.out.println("❌ No se pudo registrar en la tabla cuenta.");
            return false;
        }

        String nombreCompleto = nombre + " " + apellidos;

        if (!insertarPaciente(idCuenta, nombreCompleto, curp, telefono, matricula, fechaNacimiento, correo, telefonoEmergencia)) {
            System.out.println("❌ No se pudo registrar en la tabla paciente.");
            return false;
        }

        System.out.println("✅ Registro completado con éxito.");
        return true;
    }

    private static boolean insertarCuenta(int idCuenta, String usuario, String contrasena) {
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
            json.put("rol", "Paciente");
            json.put("estado", "ACTIVO");

            try (OutputStream os = conn.getOutputStream()) {
                os.write(json.toString().getBytes(StandardCharsets.UTF_8));
            }

            int responseCode = conn.getResponseCode();
            if (responseCode != 201 && responseCode != 200) {
                imprimirError(conn);
                return false;
            }

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static boolean insertarPaciente(int idCuenta, String nombre, String curp, String telefono,
                                            String matricula, String fechaNacimiento, String correo, String telEmergencia) {
        try {
            URL url = new URL(API_URL + "paciente");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("apikey", API_KEY);
            conn.setRequestProperty("Authorization", "Bearer " + API_KEY);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            JSONObject json = new JSONObject();
            json.put("nombre", nombre);
            json.put("curp", curp);
            json.put("telefono", telefono);
            json.put("matricula", matricula);
            json.put("fecha_nac", fechaNacimiento);
            json.put("correo", correo);
            json.put("telefono_emergencia", telEmergencia);
            json.put("estado", "ACTIVO");
            json.put("id_cuenta", idCuenta);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(json.toString().getBytes(StandardCharsets.UTF_8));
            }

            int responseCode = conn.getResponseCode();
            if (responseCode != 201 && responseCode != 200) {
                imprimirError(conn);
                return false;
            }

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static int obtenerSiguienteIdCuenta() {
        try {
            URL url = new URL(API_URL + "cuenta?select=id_cuenta&order=id_cuenta.asc");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("apikey", API_KEY);
            conn.setRequestProperty("Authorization", "Bearer " + API_KEY);

            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder respuesta = new StringBuilder();
            String linea;
            while ((linea = in.readLine()) != null) {
                respuesta.append(linea);
            }
            in.close();

            // Buscar ID más bajo disponible
            org.json.JSONArray jsonArray = new org.json.JSONArray(respuesta.toString());
            boolean[] ocupados = new boolean[jsonArray.length() + 2];

            for (int i = 0; i < jsonArray.length(); i++) {
                int id = jsonArray.getJSONObject(i).getInt("id_cuenta");
                if (id > 0 && id < ocupados.length) {
                    ocupados[id] = true;
                }
            }

            for (int i = 1; i < ocupados.length; i++) {
                if (!ocupados[i]) return i;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    private static void imprimirError(HttpURLConnection conn) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getErrorStream()))) {
            StringBuilder respuesta = new StringBuilder();
            String linea;
            while ((linea = in.readLine()) != null) {
                respuesta.append(linea);
            }
            System.out.println("💥 Error Supabase: " + respuesta);
        } catch (Exception e) {
            System.out.println("❌ No se pudo leer el error.");
        }
    }
}
