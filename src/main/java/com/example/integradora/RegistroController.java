package com.example.integradora;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

// HttpClient para PATCH
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;

public class RegistroController {

    @FXML private TextField nombreField;
    @FXML private TextField curpField;
    @FXML private TextField apellidosField;
    @FXML private TextField telefonoField;
    @FXML private TextField matriculaField;
    @FXML private DatePicker fechaNacimientoPicker;
    @FXML private TextField emailField;
    @FXML private TextField telefonoEmergenciaField;
    @FXML private PasswordField contrasenaField;
    @FXML private PasswordField confirmarContrasenaField;
    @FXML private ComboBox<String> tipoUsuarioCombo; // Personal / Estudiante

    // Validaciones
    private static final Pattern TEL10            = Pattern.compile("^\\d{10}$");
    private static final Pattern EMAIL_UTEZ       = Pattern.compile("^[A-Za-z0-9._%+-]+@utez\\.edu\\.mx$", Pattern.CASE_INSENSITIVE);
    private static final Pattern CURP_18_ALFANUM  = Pattern.compile("^[A-Z0-9]{18}$");
    private static final Pattern PASS_ALFANUM_8   = Pattern.compile("^[A-Za-z0-9]{8,}$");
    private static final DateTimeFormatter ISO    = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // Supabase
    private static final String API_URL = "https://yjfzidepdnusqufayeli.supabase.co/rest/v1/";
    private static final String API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InlqZnppZGVwZG51c3F1ZmF5ZWxpIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTEyNTUyMDMsImV4cCI6MjA2NjgzMTIwM30.Kq7hVKEWbYWObRpiZ2yxgSWpRQS3V0MBGRIZxji09lU";
    private final HttpClient http = HttpClient.newHttpClient();

    @FXML
    public void initialize() {
        // Limitar entradas
        telefonoField.setTextFormatter(new TextFormatter<>(c -> c.getControlNewText().matches("\\d{0,10}") ? c : null));
        telefonoEmergenciaField.setTextFormatter(new TextFormatter<>(c -> c.getControlNewText().matches("\\d{0,10}") ? c : null));
        curpField.setTextFormatter(new TextFormatter<>(change -> {
            String next = change.getControlNewText().toUpperCase();
            if (!next.matches("[A-Z0-9]{0,18}")) return null; // solo A–Z/0–9, máx 18
            change.setText(change.getText().toUpperCase());
            return change;
        }));

        if (tipoUsuarioCombo.getItems().isEmpty()) {
            tipoUsuarioCombo.getItems().addAll("Personal", "Estudiante");
        }
    }

    @FXML
    public void onVolverClick() {
        cambiarDeVista("Sesion.fxml", "Iniciar Sesión");
    }

    @FXML
    private void onRegistrarClick() {
        String nombre              = safe(nombreField.getText());
        String curp                = safe(curpField.getText()).toUpperCase();
        String apellidos           = safe(apellidosField.getText());
        String telefono            = safe(telefonoField.getText());
        String matricula           = safe(matriculaField.getText());
        LocalDate fechaNacimiento  = fechaNacimientoPicker.getValue();
        String email               = safe(emailField.getText());
        String telefonoEmergencia  = safe(telefonoEmergenciaField.getText());
        String contrasena          = safe(contrasenaField.getText());
        String confirmarContrasena = safe(confirmarContrasenaField.getText());
        String rolSel              = (tipoUsuarioCombo.getValue() == null) ? "" : tipoUsuarioCombo.getValue().trim();

        // Vacíos
        if (nombre.isEmpty() || curp.isEmpty() || apellidos.isEmpty() || telefono.isEmpty() ||
                matricula.isEmpty() || fechaNacimiento == null || email.isEmpty() ||
                telefonoEmergencia.isEmpty() || contrasena.isEmpty() || confirmarContrasena.isEmpty() ||
                rolSel.isEmpty()) {
            mostrarAlerta("Error", "Debe llenar todos los campos (incluido Tipo de usuario).");
            return;
        }

        // CURP: 18 alfanuméricos
        if (!CURP_18_ALFANUM.matcher(curp).matches()) {
            mostrarAlerta("Error", "La CURP debe tener exactamente 18 caracteres alfanuméricos (A–Z, 0–9).");
            return;
        }

        // Matrícula (mínimo 5)
        if (matricula.length() < 5) {
            mostrarAlerta("Error", "La matrícula debe tener al menos 5 caracteres.");
            return;
        }

        // Teléfonos
        if (!TEL10.matcher(telefono).matches()) {
            mostrarAlerta("Error", "El número de teléfono debe tener 10 dígitos.");
            return;
        }
        if (!TEL10.matcher(telefonoEmergencia).matches()) {
            mostrarAlerta("Error", "El teléfono de emergencia debe tener 10 dígitos.");
            return;
        }

        // Email institucional
        if (!EMAIL_UTEZ.matcher(email).matches()) {
            mostrarAlerta("Error", "El correo debe terminar en @utez.edu.mx.");
            return;
        }

        // Password
        if (!PASS_ALFANUM_8.matcher(contrasena).matches()) {
            mostrarAlerta("Error", "La contraseña debe tener al menos 8 caracteres (solo letras o números).");
            return;
        }
        if (!contrasena.equals(confirmarContrasena)) {
            mostrarAlerta("Error", "Las contraseñas no coinciden.");
            return;
        }

        // Fecha/edad
        LocalDate hoy = LocalDate.now();
        if (fechaNacimiento.isAfter(hoy)) {
            mostrarAlerta("Error", "La fecha de nacimiento no puede ser futura.");
            return;
        }
        int edad = Period.between(fechaNacimiento, hoy).getYears();
        if (edad < 17 || edad > 80) {
            mostrarAlerta("Error", "La edad debe estar entre 17 y 80 años.");
            return;
        }

        // Registro en su base (clase existente)
        boolean exito = BaseDatosRegistro.registrar(
                nombre, apellidos, curp, telefono,
                matricula, fechaNacimiento.format(ISO), email,
                telefonoEmergencia, contrasena
        );

        if (!exito) {
            mostrarAlerta("Error", "No se pudo completar el registro.");
            return;
        }

        // PATCH roles
        String rolPaciente = rolSel.equalsIgnoreCase("Estudiante") ? "ESTUDIANTE" : "PERSONAL";
        boolean okPac = patchRolPacientePorMatricula(matricula, rolPaciente);
        boolean okCta = patchRolCuentaPorUsuario(matricula, "paciente");

        if (okPac && okCta) {
            mostrarAlerta("Éxito", "Registro exitoso.");
            limpiarCampos();
        } else if (!okPac && okCta) {
            mostrarAlerta("Información", "Cuenta creada, pero no se pudo actualizar el rol del paciente (ESTUDIANTE/PERSONAL).");
        } else if (okPac && !okCta) {
            mostrarAlerta("Información", "Paciente registrado, pero no se pudo actualizar el rol de la cuenta a 'paciente'.");
        } else {
            mostrarAlerta("Información", "Registro ok, pero no se pudieron actualizar los roles. Intente más tarde.");
        }
    }

    // ===== PATCH paciente.rol (por matrícula) =====
    private boolean patchRolPacientePorMatricula(String matricula, String rol) {
        try {
            String url = API_URL + "paciente?matricula=eq." + URLEncoder.encode(matricula, StandardCharsets.UTF_8);
            String json = "{\"rol\":\"" + rol + "\"}";
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("apikey", API_KEY)
                    .header("Authorization", "Bearer " + API_KEY)
                    .header("Content-Type", "application/json")
                    .header("Prefer", "return=minimal")
                    .method("PATCH", HttpRequest.BodyPublishers.ofString(json))
                    .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            return resp.statusCode() == 204;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // ===== PATCH cuenta.rol = "paciente" (por usuario = matrícula) =====
    private boolean patchRolCuentaPorUsuario(String usuario, String rolCuenta) {
        try {
            String url = API_URL + "cuenta?usuario=eq." + URLEncoder.encode(usuario, StandardCharsets.UTF_8);
            String json = "{\"rol\":\"" + rolCuenta + "\"}";
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("apikey", API_KEY)
                    .header("Authorization", "Bearer " + API_KEY)
                    .header("Content-Type", "application/json")
                    .header("Prefer", "return=minimal")
                    .method("PATCH", HttpRequest.BodyPublishers.ofString(json))
                    .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            return resp.statusCode() == 204;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // ===== Helpers =====
    private void limpiarCampos() {
        nombreField.clear();
        curpField.clear();
        apellidosField.clear();
        telefonoField.clear();
        matriculaField.clear();
        if (fechaNacimientoPicker != null) fechaNacimientoPicker.setValue(null);
        emailField.clear();
        telefonoEmergenciaField.clear();
        contrasenaField.clear();
        confirmarContrasenaField.clear();
        if (tipoUsuarioCombo != null) tipoUsuarioCombo.getSelectionModel().clearSelection();
    }

    private void mostrarAlerta(String titulo, String mensaje) {
        Alert.AlertType tipo = titulo.equalsIgnoreCase("Error") ? Alert.AlertType.ERROR : Alert.AlertType.INFORMATION;
        Alert alerta = new Alert(tipo);
        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }

    private void cambiarDeVista(String fxml, String titulo) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            Parent root = loader.load();
            Stage stage = (Stage) nombreField.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle(titulo);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta("Error", "No se pudo cambiar de vista.");
        }
    }

    private String safe(String s) { return s == null ? "" : s.trim(); }
}
