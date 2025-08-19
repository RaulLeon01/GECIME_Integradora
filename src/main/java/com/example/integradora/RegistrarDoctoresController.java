package com.example.integradora;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.scene.Node;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class RegistrarDoctoresController {

    // UI
    @FXML private TextField campoNombre;
    @FXML private TextField campoApellidos;
    @FXML private TextField campoMatricula;     // cuenta.usuario
    @FXML private TextField campoEmail;
    @FXML private TextField campoArea;
    @FXML private TextField campoCurp;
    @FXML private TextField campoTelefono;
    @FXML private ComboBox<String> comboConsultorio;
    @FXML private PasswordField campoContrasena;
    @FXML private PasswordField campoConfirmar;
    @FXML private ComboBox<String> comboHoraInicio;
    @FXML private ComboBox<String> comboHoraFin;

    // API
    private static final String API_URL = "https://yjfzidepdnusqufayeli.supabase.co/rest/v1/";
    private static final String API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InlqZnppZGVwZG51c3F1ZmF5ZWxpIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTEyNTUyMDMsImV4cCI6MjA2NjgzMTIwM30.Kq7hVKEWbYWObRpiZ2yxgSWpRQS3V0MBGRIZxji09lU";
    private final HttpClient http = HttpClient.newHttpClient();

    // Validaciones
    private static final DateTimeFormatter HHMM   = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter HHMMSS = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final String EMAIL_UTEZ_REGEX = "^[A-Za-z0-9._%+-]+@utez\\.edu\\.mx$";
    private static final String CURP_18_ALFANUM  = "^[A-Z0-9]{18}$";

    // Consultorios: etiqueta -> id / nombre
    private final Map<String, Integer> etiquetaToIdConsultorio = new HashMap<>();
    private final Map<String, String>  etiquetaToNombre        = new HashMap<>();

    @FXML
    public void initialize() {
        // Formateadores
        campoTelefono.setTextFormatter(new TextFormatter<>(c -> c.getControlNewText().matches("\\d{0,10}") ? c : null));
        campoCurp.setTextFormatter(new TextFormatter<>(c -> {
            String next = c.getControlNewText().toUpperCase();
            if (!next.matches("[A-Z0-9]{0,18}")) return null;
            c.setText(c.getText().toUpperCase());
            return c;
        }));

        cargarConsultoriosActivos();
        cargarHoras();
    }

    private void cargarConsultoriosActivos() {
        try {
            String url = API_URL + "consultorio?select=" +
                    enc("id,nombre,ubicacion,estado") +
                    "&estado=eq.ACTIVO&order=" + enc("nombre.asc");

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("apikey", API_KEY)
                    .header("Authorization", "Bearer " + API_KEY)
                    .header("Accept", "application/json")
                    .GET().build();

            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() >= 400) throw new RuntimeException(resp.body());

            JSONArray arr = new JSONArray(resp.body());
            ObservableList<String> items = FXCollections.observableArrayList();
            etiquetaToIdConsultorio.clear();
            etiquetaToNombre.clear();

            for (int i = 0; i < arr.length(); i++) {
                JSONObject c = arr.getJSONObject(i);
                int id = c.getInt("id");
                String nombre = c.optString("nombre", "Consultorio " + id);
                String ubic = c.optString("ubicacion", "");
                String etiqueta = id + " - " + nombre + (ubic.isBlank() ? "" : " (" + ubic + ")");
                etiquetaToIdConsultorio.put(etiqueta, id);
                etiquetaToNombre.put(etiqueta, nombre);
                items.add(etiqueta);
            }
            comboConsultorio.setItems(items);
        } catch (Exception ex) {
            ex.printStackTrace();
            alertaError("No se pudieron cargar los consultorios.");
        }
    }

    private void cargarHoras() {
        List<String> horas = new ArrayList<>();
        for (int h = 8; h <= 21; h++) {
            horas.add(String.format("%02d:00", h));
            horas.add(String.format("%02d:30", h));
        }
        comboHoraInicio.setItems(FXCollections.observableArrayList(horas));
        comboHoraFin.setItems(FXCollections.observableArrayList(horas));
    }

    @FXML
    private void registrarDoctor(ActionEvent e) {
        // Lectura
        String nombre     = safe(campoNombre.getText());
        String apellidos  = safe(campoApellidos.getText());
        String usuario    = safe(campoMatricula.getText());     // cuenta.usuario
        String email      = safe(campoEmail.getText());
        String area       = safe(campoArea.getText());
        String curp       = safe(campoCurp.getText()).toUpperCase();
        String telefono   = safe(campoTelefono.getText());
        String etCons     = comboConsultorio.getValue();
        String hi         = comboHoraInicio.getValue();
        String hf         = comboHoraFin.getValue();
        String pass       = safe(campoContrasena.getText());
        String pass2      = safe(campoConfirmar.getText());

        // Validaciones
        if (nombre.isBlank())                  { alertaError("El nombre es obligatorio."); return; }
        if (apellidos.isBlank())               { alertaError("Los apellidos son obligatorios."); return; }
        if (usuario.length() < 5)              { alertaError("La matrícula/usuario debe tener al menos 5 caracteres."); return; }
        if (!email.matches(EMAIL_UTEZ_REGEX))  { alertaError("El correo debe terminar en @utez.edu.mx."); return; }
        if (!curp.matches(CURP_18_ALFANUM))    { alertaError("La CURP debe tener 18 caracteres alfanuméricos (A–Z, 0–9)."); return; }
        if (!telefono.matches("\\d{10}"))      { alertaError("El teléfono debe tener 10 dígitos."); return; }
        if (etCons == null)                    { alertaError("Selecciona un consultorio."); return; }
        if (hi == null || hf == null)          { alertaError("Selecciona hora de inicio y fin."); return; }
        if (pass.length() < 8)                 { alertaError("La contraseña debe tener al menos 8 caracteres."); return; }
        if (!pass.equals(pass2))               { alertaError("Las contraseñas no coinciden."); return; }

        String horaInicio, horaFin;
        try {
            horaInicio = toHHmmss(hi);
            horaFin    = toHHmmss(hf);
            if (!LocalTime.parse(horaFin, HHMMSS).isAfter(LocalTime.parse(horaInicio, HHMMSS))) {
                alertaError("La hora fin debe ser mayor que la hora inicio.");
                return;
            }
        } catch (Exception ex) {
            alertaError("Formato de hora inválido. Usa HH:mm.");
            return;
        }

        Integer idConsultorio = etiquetaToIdConsultorio.get(etCons);
        String  nombreConsultorio = etiquetaToNombre.get(etCons);
        if (idConsultorio == null || nombreConsultorio == null) {
            alertaError("Consultorio inválido.");
            return;
        }

        try {
            // 1) Crear cuenta (doctor)
            JSONObject cuenta = new JSONObject()
                    .put("usuario", usuario)
                    .put("contrasena", pass)
                    .put("rol", "doctor")
                    .put("estado", "ACTIVO");

            HttpResponse<String> rCta = http.send(
                    HttpRequest.newBuilder()
                            .uri(URI.create(API_URL + "cuenta"))
                            .header("apikey", API_KEY)
                            .header("Authorization", "Bearer " + API_KEY)
                            .header("Content-Type", "application/json")
                            .header("Prefer", "return=minimal")
                            .POST(HttpRequest.BodyPublishers.ofString(cuenta.toString()))
                            .build(),
                    HttpResponse.BodyHandlers.ofString()
            );
            if (rCta.statusCode() != 201) {
                alertaError("No se pudo crear la cuenta (código " + rCta.statusCode() + ").\n" + rCta.body());
                return;
            }

            // 2) Obtener id_cuenta
            Integer idCuenta = obtenerIdCuentaPorUsuario(usuario);
            if (idCuenta == null) {
                alertaError("Cuenta creada, pero no se pudo recuperar id_cuenta.");
                return;
            }

            // 3) Crear empleado (incluye rol y correo + consultorio por nombre e id)
            String nombreCompleto = nombre + " " + apellidos;
            JSONObject emp = new JSONObject()
                    .put("nombre", nombreCompleto)
                    .put("especialidad", area)     // si tu columna se llama distinto, cámbiala aquí
                    .put("telefono", telefono)
                    .put("correo", email)          // ← guardar correo en empleado
                    .put("rol", "doctor")          // ← guardar rol en empleado
                    .put("estado", "ACTIVO")
                    .put("hora_inicio", horaInicio)
                    .put("hora_fin",    horaFin)
                    .put("id_consultorio", idConsultorio)
                    .put("consultorio",  nombreConsultorio) // ← nombre del consultorio
                    .put("id_cuenta",    idCuenta);

            HttpResponse<String> rEmp = http.send(
                    HttpRequest.newBuilder()
                            .uri(URI.create(API_URL + "empleado"))
                            .header("apikey", API_KEY)
                            .header("Authorization", "Bearer " + API_KEY)
                            .header("Content-Type", "application/json")
                            .header("Prefer", "return=minimal")
                            .POST(HttpRequest.BodyPublishers.ofString(emp.toString()))
                            .build(),
                    HttpResponse.BodyHandlers.ofString()
            );
            if (rEmp.statusCode() != 201) {
                alertaError("Cuenta creada, pero no se pudo registrar el empleado (código " + rEmp.statusCode() + ").\n" + rEmp.body());
                return;
            }

            alertaInfo("Doctor registrado correctamente.");
            regresar(e);

        } catch (Exception ex) {
            ex.printStackTrace();
            alertaError("Ocurrió un error al registrar.");
        }
    }

    @FXML
    private void regresar(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/integradora/Doctores.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Administrador - GECIME");
            stage.show();
        } catch (Exception ex) {
            ex.printStackTrace();
            alertaError("No se pudo regresar a la vista de Doctores.");
        }
    }

    // ===== Helpers =====
    private Integer obtenerIdCuentaPorUsuario(String usuario) {
        try {
            String url = API_URL + "cuenta?select=" + enc("id_cuenta") +
                    "&usuario=eq." + enc(usuario) + "&limit=1";
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("apikey", API_KEY)
                    .header("Authorization", "Bearer " + API_KEY)
                    .header("Accept", "application/json")
                    .GET().build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() >= 400) return null;
            JSONArray arr = new JSONArray(resp.body());
            if (arr.isEmpty()) return null;
            return arr.getJSONObject(0).getInt("id_cuenta");
        } catch (Exception e) {
            return null;
        }
    }

    private String toHHmmss(String t) {
        String v = t.trim();
        if (v.matches("^\\d{2}:\\d{2}:\\d{2}$")) return v;
        if (v.matches("^\\d{1}:\\d{2}$")) v = "0" + v;
        if (v.matches("^\\d{2}:\\d{2}$")) return v + ":00";
        LocalTime time = LocalTime.parse(v, HHMM);
        return time.format(HHMMSS);
    }

    private String enc(String s) {
        try { return URLEncoder.encode(s, StandardCharsets.UTF_8.toString()); }
        catch (Exception e) { return s; }
    }

    private String safe(String s) { return s == null ? "" : s.trim(); }

    private void alertaError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Error");
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    private void alertaInfo(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Información");
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}
