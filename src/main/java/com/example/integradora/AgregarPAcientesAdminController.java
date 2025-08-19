package com.example.integradora;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

// HttpClient para PATCH del rol
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;

public class AgregarPAcientesAdminController {

    @FXML private TextField nombreField;
    @FXML private TextField curpField;
    @FXML private TextField apellidosField;
    @FXML private TextField telefonoField;
    @FXML private TextField matriculaField;
    @FXML private TextField fechaNacimientoField; // YYYY-MM-DD
    @FXML private TextField emailField;
    @FXML private TextField telefonoEmergenciaField;
    @FXML private PasswordField contrasenaField;
    @FXML private PasswordField confirmarContrasenaField;
    @FXML private ComboBox<String> tipoUsuarioCombo; // ESTUDIANTE / PERSONAL

    private static final Pattern SOLO_DIGITOS_10 = Pattern.compile("^\\d{10}$");
    private static final Pattern EMAIL_UTEZ = Pattern.compile("^[A-Za-z0-9._%+-]+@utez\\.edu\\.mx$", Pattern.CASE_INSENSITIVE);
    private static final Pattern CURP_OFICIAL = Pattern.compile("^[A-Z]{4}\\d{6}[HM][A-Z]{5}[A-Z0-9]{2}$");
    private static final Pattern PASS_ALFANUM_8 = Pattern.compile("^[A-Za-z0-9]{8,}$");
    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // Supabase para el PATCH del rol
    private static final String API_URL = "https://yjfzidepdnusqufayeli.supabase.co/rest/v1/";
    private static final String API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InlqZnppZGVwZG51c3F1ZmF5ZWxpIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTEyNTUyMDMsImV4cCI6MjA2NjgzMTIwM30.Kq7hVKEWbYWObRpiZ2yxgSWpRQS3V0MBGRIZxji09lU";
    private final HttpClient http = HttpClient.newHttpClient();

    @FXML
    public void initialize() {
        telefonoField.setTextFormatter(new TextFormatter<>(c -> c.getControlNewText().matches("\\d{0,10}") ? c : null));
        telefonoEmergenciaField.setTextFormatter(new TextFormatter<>(c -> c.getControlNewText().matches("\\d{0,10}") ? c : null));
        curpField.setTextFormatter(new TextFormatter<>(c -> { c.setText(c.getText().toUpperCase()); return c.getControlNewText().length() <= 18 ? c : null; }));
        if (tipoUsuarioCombo.getItems().isEmpty()) {
            tipoUsuarioCombo.getItems().addAll("ESTUDIANTE", "PERSONAL");
        }
    }

    @FXML
    public void onVolverClick() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("Pacientes.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) nombreField.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Pacientes");
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("No se pudo cargar la vista de pacientes.");
        }
    }

    @FXML
    private void onRegistrarClick() {
        String nombre = safe(nombreField.getText());
        String curp = safe(curpField.getText()).toUpperCase();
        String apellidos = safe(apellidosField.getText());
        String telefono = safe(telefonoField.getText());
        String matricula = safe(matriculaField.getText());
        String fechaNacimiento = safe(fechaNacimientoField.getText());
        String email = safe(emailField.getText());
        String telefonoEmergencia = safe(telefonoEmergenciaField.getText());
        String contrasena = safe(contrasenaField.getText());
        String confirmarContrasena = safe(confirmarContrasenaField.getText());
        String rolSel = (tipoUsuarioCombo.getValue() == null) ? "" : tipoUsuarioCombo.getValue().trim().toUpperCase();

        // Validaciones una por una con alerta
        if (nombre.isEmpty()) { mostrarError("El nombre es obligatorio."); return; }
        if (apellidos.isEmpty()) { mostrarError("Los apellidos son obligatorios."); return; }

        if (curp.isEmpty()) { mostrarError("La CURP es obligatoria."); return; }
        if (curp.length() != 18) { mostrarError("La CURP debe tener 18 caracteres."); return; }
        if (!CURP_OFICIAL.matcher(curp).matches()) {
            mostrarError("La CURP no cumple el formato esperado (18 caracteres en mayúsculas).");
            return;
        }

        if (matricula.isEmpty()) { mostrarError("La matrícula es obligatoria."); return; }
        if (matricula.length() < 5) { mostrarError("La matrícula debe tener al menos 5 caracteres."); return; }

        if (telefono.isEmpty()) { mostrarError("El teléfono es obligatorio."); return; }
        if (!SOLO_DIGITOS_10.matcher(telefono).matches()) {
            mostrarError("El teléfono debe tener exactamente 10 dígitos.");
            return;
        }

        if (telefonoEmergencia.isEmpty()) { mostrarError("El teléfono de emergencia es obligatorio."); return; }
        if (!SOLO_DIGITOS_10.matcher(telefonoEmergencia).matches()) {
            mostrarError("El teléfono de emergencia debe tener exactamente 10 dígitos.");
            return;
        }

        if (email.isEmpty()) { mostrarError("El correo es obligatorio."); return; }
        if (!EMAIL_UTEZ.matcher(email).matches()) {
            mostrarError("El correo debe ser del dominio @utez.edu.mx");
            return;
        }

        if (fechaNacimiento.isEmpty()) { mostrarError("La fecha de nacimiento es obligatoria."); return; }
        LocalDate fnac;
        try {
            fnac = LocalDate.parse(fechaNacimiento, ISO_DATE);
        } catch (DateTimeParseException e) {
            mostrarError("La fecha de nacimiento debe tener el formato YYYY-MM-DD.");
            return;
        }
        if (fnac.isAfter(LocalDate.now())) {
            mostrarError("La fecha de nacimiento no puede ser futura.");
            return;
        }
        int edad = Period.between(fnac, LocalDate.now()).getYears();
        if (edad < 17 || edad > 80) {
            mostrarError("La edad debe estar entre 17 y 80 años.");
            return;
        }

        if (contrasena.isEmpty()) { mostrarError("La contraseña es obligatoria."); return; }
        if (!PASS_ALFANUM_8.matcher(contrasena).matches()) {
            mostrarError("La contraseña debe tener al menos 8 caracteres (letras o números).");
            return;
        }
        if (!contrasena.equals(confirmarContrasena)) {
            mostrarError("Las contraseñas no coinciden.");
            return;
        }

        if (rolSel.isEmpty()) {
            mostrarError("Seleccione el tipo de usuario (ESTUDIANTE o PERSONAL).");
            return;
        }
        if (!rolSel.equals("ESTUDIANTE") && !rolSel.equals("PERSONAL")) {
            mostrarError("Rol inválido. Debe ser ESTUDIANTE o PERSONAL.");
            return;
        }

        // Insert con su clase existente
        boolean exito = BaseDatosRegistro.registrar(
                nombre, apellidos, curp, telefono,
                matricula, fechaNacimiento, email,
                telefonoEmergencia, contrasena
        );

        if (!exito) {
            mostrarError("No se pudo completar el registro.");
            return;
        }

        // PATCH del rol a la tabla paciente (por matrícula)
        boolean rolOk = patchRolPacientePorMatricula(matricula, rolSel);
        if (rolOk) {
            mostrarInfo("Registro exitoso.");
            limpiarCampos();
        } else {
            mostrarInfo("Paciente registrado, pero no se pudo actualizar el rol.");
        }
    }

    // ===== PATCH rol =====
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

    // ===== Helpers =====
    private String safe(String s) { return s == null ? "" : s.trim(); }

    private void limpiarCampos() {
        nombreField.clear();
        curpField.clear();
        apellidosField.clear();
        telefonoField.clear();
        matriculaField.clear();
        fechaNacimientoField.clear();
        emailField.clear();
        telefonoEmergenciaField.clear();
        contrasenaField.clear();
        confirmarContrasenaField.clear();
        if (tipoUsuarioCombo != null) tipoUsuarioCombo.getSelectionModel().clearSelection();
    }

    private void mostrarError(String mensaje) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Validación");
        a.setHeaderText(null);
        a.setContentText(mensaje);
        a.showAndWait();
    }

    private void mostrarInfo(String mensaje) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Información");
        a.setHeaderText(null);
        a.setContentText(mensaje);
        a.showAndWait();
    }
}
