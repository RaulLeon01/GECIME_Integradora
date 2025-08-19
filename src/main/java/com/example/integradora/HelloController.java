package com.example.integradora;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.scene.Node;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class HelloController {

    @FXML private TextField usuarioField;
    @FXML private PasswordField contraseñaField;

    private static final String API_URL = "https://yjfzidepdnusqufayeli.supabase.co/rest/v1/";
    private static final String API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InlqZnppZGVwZG51c3F1ZmF5ZWxpIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTEyNTUyMDMsImV4cCI6MjA2NjgzMTIwM30.Kq7hVKEWbYWObRpiZ2yxgSWpRQS3V0MBGRIZxji09lU";

    @FXML
    private void onLoginButtonClick(ActionEvent event) {
        String usuarioIngresado = usuarioField.getText().trim();
        String contrasenaIngresada = contraseñaField.getText().trim();

        if (usuarioIngresado.isEmpty() || contrasenaIngresada.isEmpty()) {
            mostrarAlerta("Campos vacíos", "Por favor ingrese usuario y contraseña.");
            return;
        }

        try {
            URL url = new URL(API_URL + "cuenta?select=*");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("apikey", API_KEY);
            conn.setRequestProperty("Authorization", "Bearer " + API_KEY);

            int responseCode = conn.getResponseCode();
            System.out.println("📡 Código HTTP: " + responseCode);

            if (responseCode == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) response.append(line);
                reader.close();

                JSONArray jsonArray = new JSONArray(response.toString());
                String[][] cuentas = new String[jsonArray.length()][5];

                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject obj = jsonArray.getJSONObject(i);
                    cuentas[i][0] = obj.get("id_cuenta").toString();
                    cuentas[i][1] = obj.getString("usuario");
                    cuentas[i][2] = obj.getString("contrasena");
                    cuentas[i][3] = obj.getString("rol");
                    cuentas[i][4] = obj.getString("estado");  // solo se usa si es ADMIN
                }

                boolean encontrado = false;
                for (int i = 0; i < cuentas.length; i++) {
                    String usuario = cuentas[i][1];
                    String contrasena = cuentas[i][2];
                    String rol = cuentas[i][3];
                    int idCuenta = Integer.parseInt(cuentas[i][0]);

                    if (usuario.equals(usuarioIngresado) && contrasena.equals(contrasenaIngresada)) {
                        System.out.println("🔎 Coincidencia encontrada en cuenta, verificando estado según rol...");

                        boolean activo;

                        if (rol.equalsIgnoreCase("ADMIN")) {
                            String estado = cuentas[i][4];
                            System.out.println("🛡 Estado desde cuenta (ADMIN): " + estado);
                            activo = estado.equalsIgnoreCase("ACTIVO");
                        } else {
                            activo = verificarEstadoDesdeTablaSecundaria(idCuenta, rol);
                        }

                        if (!activo) {
                            mostrarAlerta("Cuenta inactiva", "Su cuenta ha sido desactivada.");
                            return;
                        }

                        System.out.println("✅ Login correcto - ID: " + idCuenta + " | Rol: " + rol.toUpperCase());

                        switch (rol.toUpperCase()) {
                            case "PACIENTE":
                                cargarVista("Usuario.fxml", "Perfil - GECIME", event, idCuenta);
                                break;
                            case "DOCTOR":
                                cargarVista("PerfilDoctor.fxml", "Médico - GECIME", event, idCuenta);
                                break;
                            case "ADMIN":
                                cargarVista("Admin.fxml", "Administrador - GECIME", event, idCuenta);
                                break;
                            default:
                                mostrarAlerta("Rol desconocido", "El rol de esta cuenta no es válido.");
                        }

                        encontrado = true;
                        break;
                    }
                }

                if (!encontrado) {
                    mostrarAlerta("Credenciales inválidas", "Usuario o contraseña incorrectos.");
                }

            } else {
                mostrarAlerta("Error", "No se pudo validar la cuenta. Código: " + responseCode);
            }

        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta("Error", "Ocurrió un problema al iniciar sesión.");
        }
    }

    private boolean verificarEstadoDesdeTablaSecundaria(int idCuenta, String rol) {
        try {
            String tabla = rol.equalsIgnoreCase("DOCTOR") ? "empleado" : "paciente";
            String urlStr = API_URL + tabla + "?id_cuenta=eq." + idCuenta + "&select=estado";
            URL url = new URL(urlStr);

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("apikey", API_KEY);
            conn.setRequestProperty("Authorization", "Bearer " + API_KEY);

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) return false;

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) response.append(line);
            reader.close();

            JSONArray array = new JSONArray(response.toString());
            if (array.length() > 0) {
                String estado = array.getJSONObject(0).getString("estado");
                System.out.println("📋 Estado verificado en tabla " + tabla + ": " + estado);
                return estado.equalsIgnoreCase("ACTIVO");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private void cargarVista(String fxml, String titulo, ActionEvent event, int idCuenta) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            Parent root = loader.load();

            Object controller = loader.getController();
            if (controller instanceof ConCuenta controladorConCuenta) {
                controladorConCuenta.setIdCuenta(idCuenta);
                System.out.println("🧾 ID pasado a la vista: " + idCuenta);
            }

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle(titulo);
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta("Error", "No se pudo cargar la vista: " + fxml);
        }
    }

    @FXML
    private void onIrARegistroClick(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("Registro.fxml"));
            Scene registroScene = new Scene(loader.load());
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(registroScene);
            stage.setTitle("Registrarse - GECIME");
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta("Error", "No se pudo cargar la vista de registro.");
        }
    }

    @FXML
    private void onOlvidasteContrasenaClick() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("RecuperarContrasena.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Recuperar Contraseña");
            stage.setScene(new Scene(root));
            stage.show();

            Stage currentStage = (Stage) usuarioField.getScene().getWindow();
            currentStage.close();

        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta("Error", "No se pudo abrir la ventana de recuperación.");
        }
    }

    private void mostrarAlerta(String titulo, String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.ERROR);
        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }
}
