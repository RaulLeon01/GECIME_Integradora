package com.example.integradora;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class PasswordRecoveryConnection {

    private static final String API_URL = "https://yjfzidepdnusqufayeli.supabase.co/rest/v1/";
    private static final String API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InlqZnppZGVwZG51c3F1ZmF5ZWxpIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTEyNTUyMDMsImV4cCI6MjA2NjgzMTIwM30.Kq7hVKEWbYWObRpiZ2yxgSWpRQS3V0MBGRIZxji09lU"; // Aquí coloca tu API KEY

    public static boolean correoExiste(String correo) {
        try {
            String correoLimpio = correo.trim();
            String correoCodificado = java.net.URLEncoder.encode(correoLimpio, "UTF-8");
            String urlFinal = API_URL + "paciente?correo=ilike." + correoCodificado;

            System.out.println("🔍 URL generada: " + urlFinal); // ✅ Verificar URL

            URL url = new URL(urlFinal);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("apikey", API_KEY);
            conn.setRequestProperty("Authorization", "Bearer " + API_KEY);

            int responseCode = conn.getResponseCode();
            System.out.println("📡 Código de respuesta: " + responseCode); // ✅ Verificar respuesta

            if (responseCode == 200) {
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String inputLine;

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                System.out.println("📦 Respuesta del servidor: " + response.toString()); // ✅ Verificar datos

                JSONArray jsonArray = new JSONArray(response.toString());
                return jsonArray.length() > 0;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }



    public static int obtenerIdCuentaPorCorreo(String correo) {
        try {
            URL url = new URL(API_URL + "paciente?correo=eq." + correo);
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
                    return jsonArray.getJSONObject(0).getInt("id_cuenta");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    public static boolean guardarToken(int idCuenta, String token) {
        try {
            URL url = new URL(API_URL + "recuperacion_contrasena");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("apikey", API_KEY);
            conn.setRequestProperty("Authorization", "Bearer " + API_KEY);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            JSONObject json = new JSONObject();
            json.put("id_cuenta", idCuenta);
            json.put("token", token);

            // Sumar 5 minutos a la fecha actual
            String fechaExpiracion = java.time.LocalDateTime.now().plusMinutes(5).toString();
            json.put("fecha_solicitud", fechaExpiracion);
            json.put("usado", false);

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


    public static TokenData validarTokenDisponible(String token) {
        try {
            URL url = new URL(API_URL + "recuperacion_contrasena?token=eq." + token + "&usado=eq.false");
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
                    JSONObject objeto = jsonArray.getJSONObject(0);
                    int idCuenta = objeto.getInt("id_cuenta");
                    String fechaSolicitud = objeto.getString("fecha_solicitud");
                    return new TokenData(idCuenta, fechaSolicitud);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean tokenVigente(String fechaExpiracion) {
        try {
            java.time.LocalDateTime fechaToken = java.time.LocalDateTime.parse(fechaExpiracion);
            // Ahora solo revisamos si la fecha actual es antes de la fecha de expiración guardada
            return java.time.LocalDateTime.now().isBefore(fechaToken);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }



    public static boolean actualizarContrasena(int idCuenta, String nuevaContrasena) {
        try {
            String urlString = API_URL + "cuenta?id_cuenta=eq." + idCuenta;
            System.out.println("🌐 URL actualización: " + urlString);

            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST"); // Cambiamos PATCH por POST
            conn.setRequestProperty("apikey", API_KEY);
            conn.setRequestProperty("Authorization", "Bearer " + API_KEY);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Prefer", "resolution=merge-duplicates"); // IMPORTANTE
            conn.setDoOutput(true);

            JSONObject json = new JSONObject();
            json.put("id_cuenta", idCuenta); // ID para que supabase sepa qué actualizar
            json.put("contrasena", nuevaContrasena);

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = json.toString().getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = conn.getResponseCode();
            System.out.println("📡 Código de respuesta al actualizar: " + responseCode);

            return responseCode == 204 || responseCode == 200 || responseCode == 201;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }


    public static void marcarTokenComoUsado(String token) {
        try {
            URL url = new URL(API_URL + "recuperacion_contrasena?token=eq." + token);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("PATCH");
            conn.setRequestProperty("apikey", API_KEY);
            conn.setRequestProperty("Authorization", "Bearer " + API_KEY);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            JSONObject json = new JSONObject();
            json.put("usado", true);

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = json.toString().getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

