package com.example.integradora;

import javafx.fxml.*;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.event.ActionEvent;
import javafx.scene.Node;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;

public class AgendarCitaAdminController implements Initializable {

    @FXML private ComboBox<String> comboDoctor;     // muestra nombre
    @FXML private ComboBox<String> comboHora;       // "HH:mm"
    @FXML private DatePicker fechaPicker;
    @FXML private TextArea malestarArea;
    @FXML private TextField idPacienteField;        // aquí escriben la MATRÍCULA del paciente

    private static final String API_URL = "https://yjfzidepdnusqufayeli.supabase.co/rest/v1/";
    private static final String API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InlqZnppZGVwZG51c3F1ZmF5ZWxpIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTEyNTUyMDMsImV4cCI6MjA2NjgzMTIwM30.Kq7hVKEWbYWObRpiZ2yxgSWpRQS3V0MBGRIZxji09lU";

    // Configuración de fechas
    private static final int MAX_DIAS = 14; // solo 2 semanas
    private static final DateTimeFormatter HHMM = DateTimeFormatter.ofPattern("HH:mm");

    // Datos en memoria
    private final Map<String, Integer> doctorNombreToId = new HashMap<>();
    private final Map<Integer, LocalTime[]> doctorHorario = new HashMap<>(); // id -> [inicio, fin]
    private final Map<String, Integer> matriculaToPacienteId = new HashMap<>(); // matricula -> id_paciente

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        configurarDatePicker();
        cargarDoctoresActivos();
        cargarPacientesActivos();

        comboDoctor.setOnAction(e -> recargarHoras());
        fechaPicker.valueProperty().addListener((obs, a, b) -> recargarHoras());
    }

    private void configurarDatePicker() {
        LocalDate hoy = LocalDate.now();
        fechaPicker.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                boolean invalida = empty
                        || date.isBefore(hoy)                      // no pasadas
                        || date.isAfter(hoy.plusDays(MAX_DIAS))    // solo 2 semanas
                        || date.getDayOfWeek() == DayOfWeek.SUNDAY;// sin domingos
                setDisable(invalida);
                if (invalida) setStyle("-fx-opacity: 0.5;");
            }
        });
        fechaPicker.setValue(hoy); // default hoy (si hoy no es inválido el usuario puede cambiar)
    }

    // ================== Carga de catálogos ==================

    private void cargarDoctoresActivos() {
        try {
            // Traemos nombre, id, y si existen, hora_inicio/hora_fin
            String select = enc("id_empleado,nombre,estado,hora_inicio,hora_fin");
            String url = API_URL + "empleado?select=" + select + "&estado=eq.ACTIVO";
            JSONArray data = getArray(url);

            List<String> nombres = new ArrayList<>();
            for (int i = 0; i < data.length(); i++) {
                JSONObject obj = data.getJSONObject(i);
                int id = obj.getInt("id_empleado");
                String nombre = obj.optString("nombre", "Doctor " + id);
                doctorNombreToId.put(nombre, id);
                // horario opcional
                LocalTime hi = parseTime(obj.optString("hora_inicio", "09:00:00"), LocalTime.of(9,0));
                LocalTime hf = parseTime(obj.optString("hora_fin", "21:00:00"),   LocalTime.of(21,0));
                doctorHorario.put(id, new LocalTime[]{hi, hf});
                nombres.add(nombre);
            }
            comboDoctor.getItems().setAll(nombres);
        } catch (Exception e) {
            e.printStackTrace();
            alerta("Error", "No se pudieron cargar los doctores activos.");
        }
    }

    private void cargarPacientesActivos() {
        try {
            String select = enc("id_paciente,matricula,estado");
            String url = API_URL + "paciente?select=" + select + "&estado=eq.ACTIVO";
            JSONArray data = getArray(url);

            matriculaToPacienteId.clear();
            for (int i = 0; i < data.length(); i++) {
                JSONObject obj = data.getJSONObject(i);
                String matricula = obj.optString("matricula", "");
                if (!matricula.isBlank()) {
                    matriculaToPacienteId.put(matricula, obj.getInt("id_paciente"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            alerta("Error", "No se pudieron cargar los pacientes activos.");
        }
    }

    // ================== Horas disponibles ==================

    private void recargarHoras() {
        comboHora.getItems().clear();

        String nombreDoctor = comboDoctor.getValue();
        LocalDate fecha = fechaPicker.getValue();

        if (nombreDoctor == null || fecha == null) return;

        // Reglas de fecha (doble validación)
        LocalDate hoy = LocalDate.now();
        if (fecha.isBefore(hoy)) return;
        if (fecha.isAfter(hoy.plusDays(MAX_DIAS))) return;
        if (fecha.getDayOfWeek() == DayOfWeek.SUNDAY) return;

        Integer idDoctor = doctorNombreToId.get(nombreDoctor);
        if (idDoctor == null) return;

        LocalTime[] rango = doctorHorario.getOrDefault(idDoctor, new LocalTime[]{LocalTime.of(9,0), LocalTime.of(21,0)});
        LocalTime inicio = rango[0];
        LocalTime fin    = rango[1];

        // Generar slots de 30min en el rango
        List<String> slots = new ArrayList<>();
        for (LocalTime t = inicio; !t.isAfter(fin); t = t.plusMinutes(30)) {
            slots.add(t.format(HHMM));
        }

        // Si la fecha es hoy, no permitir horas ya pasadas (buffer al minuto)
        if (fecha.isEqual(hoy)) {
            LocalTime ahora = LocalTime.now().withSecond(0).withNano(0);
            slots.removeIf(h -> LocalTime.parse(h, HHMM).isBefore(ahora));
        }

        // Quitar horas ya ocupadas por ese doctor en esa fecha
        try {
            String url = API_URL + "cita?select=" + enc("hora,estado")
                    + "&id_empleado=eq." + idDoctor
                    + "&fecha=eq." + fecha.toString();
            JSONArray citas = getArray(url);

            Set<String> ocupadas = new HashSet<>();
            for (int i = 0; i < citas.length(); i++) {
                JSONObject c = citas.getJSONObject(i);
                String estado = c.optString("estado", "AGENDADA");
                // considera ocupadas todas menos, por ejemplo, CANCELADA (ajuste si maneja más estados)
                if (!estado.equalsIgnoreCase("CANCELADA")) {
                    String h = c.optString("hora", "");
                    if (h.length() >= 5) ocupadas.add(h.substring(0,5));
                }
            }
            slots.removeIf(ocupadas::contains);
        } catch (Exception e) {
            e.printStackTrace();
            // si falla, mostramos todos los slots (ya sin ocupadas)
        }

        comboHora.getItems().setAll(slots);
    }

    // ================== Acción: agendar ==================

    @FXML
    private void agendarCita(ActionEvent event) {
        String matricula = safe(idPacienteField.getText());
        String nombreDoctor = comboDoctor.getValue();
        String hora = comboHora.getValue();
        LocalDate fecha = fechaPicker.getValue();
        String malestar = safe(malestarArea.getText());

        // Validaciones
        if (matricula.isEmpty()) { alerta("Validación", "Ingrese la matrícula del paciente."); return; }
        Integer idPaciente = matriculaToPacienteId.get(matricula);
        if (idPaciente == null) { alerta("Validación", "Paciente no encontrado o inactivo."); return; }

        if (nombreDoctor == null) { alerta("Validación", "Seleccione un doctor."); return; }
        Integer idDoctor = doctorNombreToId.get(nombreDoctor);
        if (idDoctor == null) { alerta("Validación", "Doctor no válido."); return; }

        if (fecha == null) { alerta("Validación", "Seleccione una fecha."); return; }
        // reglas de fecha
        LocalDate hoy = LocalDate.now();
        if (fecha.isBefore(hoy)) { alerta("Validación", "No puede agendar en fechas pasadas."); return; }
        if (fecha.isAfter(hoy.plusDays(MAX_DIAS))) { alerta("Validación", "Solo puede agendar dentro de las próximas 2 semanas."); return; }
        if (fecha.getDayOfWeek() == DayOfWeek.SUNDAY) { alerta("Validación", "No se agendan citas en domingo."); return; }

        if (hora == null) { alerta("Validación", "Seleccione una hora disponible."); return; }
        if (malestar.isEmpty()) { alerta("Validación", "Describa el motivo o malestar."); return; }

        // Revalidar que la hora aún esté libre (condición de carrera)
        if (!horaDisponible(idDoctor, fecha, hora)) {
            alerta("Validación", "Esa hora ya no está disponible. Elija otra, por favor.");
            recargarHoras();
            return;
        }

        // POST cita
        try {
            JSONObject cita = new JSONObject()
                    .put("id_paciente", idPaciente)
                    .put("id_empleado", idDoctor)
                    .put("fecha", fecha.toString())
                    .put("hora", hora)
                    .put("motivo", malestar)
                    .put("estado", "AGENDADA")
                    .put("duracion", 30);

            int code = postJson(API_URL + "cita", cita);
            if (code == 201) {
                alerta("Éxito", "Cita agendada correctamente.");
                // limpiar campos mínimos y refrescar horas
                malestarArea.clear();
                recargarHoras();
            } else {
                alerta("Error", "No se pudo agendar la cita. Código: " + code);
            }
        } catch (Exception e) {
            e.printStackTrace();
            alerta("Error", "Fallo al agendar la cita.");
        }
    }

    private boolean horaDisponible(int idDoctor, LocalDate fecha, String hora) {
        try {
            String url = API_URL + "cita?select=" + enc("id_cita,estado")
                    + "&id_empleado=eq." + idDoctor
                    + "&fecha=eq." + fecha.toString()
                    + "&hora=eq." + hora;
            JSONArray arr = getArray(url);
            for (int i = 0; i < arr.length(); i++) {
                String estado = arr.getJSONObject(i).optString("estado", "AGENDADA");
                if (!estado.equalsIgnoreCase("CANCELADA")) return false;
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            // si hay error al consultar, por seguridad, diga no disponible
            return false;
        }
    }

    // ================== HTTP helpers ==================

    private JSONArray getArray(String urlStr) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("apikey", API_KEY);
        conn.setRequestProperty("Authorization", "Bearer " + API_KEY);
        conn.setRequestProperty("Accept", "application/json");
        String body = leer(conn);
        return new JSONArray(body);
    }

    private int postJson(String urlStr, JSONObject body) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("apikey", API_KEY);
        conn.setRequestProperty("Authorization", "Bearer " + API_KEY);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Prefer", "return=minimal");
        conn.setDoOutput(true);
        try (OutputStream os = conn.getOutputStream()) {
            byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
            os.write(bytes);
        }
        // forzar lectura para que Supabase cierre bien
        try { leer(conn); } catch (Exception ignored) {}
        return conn.getResponseCode();
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

    private LocalTime parseTime(String t, LocalTime def) {
        try {
            if (t == null || t.isBlank()) return def;
            // admite "HH:mm:ss" o "HH:mm"
            if (t.length() >= 5) return LocalTime.parse(t.substring(0,5), HHMM);
            return def;
        } catch (Exception ex) { return def; }
    }

    private String safe(String s) { return s == null ? "" : s.trim(); }

    private void alerta(String titulo, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(titulo);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    // ================== Navegación ==================

    @FXML void irAVistaAgendar(ActionEvent event)    { cambiarVista("/com/example/integradora/AgendarCitaAdmin.fxml", event); }
    @FXML void irAVistaAgendadas(ActionEvent event)  { cambiarVista("/com/example/integradora/CitasAgendidasAdmin.fxml", event); }
    @FXML void irAVistaPacientes(ActionEvent event)  { cambiarVista("/com/example/integradora/Pacientes.fxml", event); }
    @FXML void irAVistaDoctores(ActionEvent event)   { cambiarVista("/com/example/integradora/Doctores.fxml", event); }
    @FXML void irAVistaPerfil(ActionEvent event)     { cambiarVista("/com/example/integradora/Admin.fxml", event); }
    @FXML private void irAVistaConsultorios(ActionEvent event) {
        cambiarVista("Consultorios.fxml", event);
    }
    @FXML private void irAVistaHistorial(ActionEvent event) { cambiarVista("HistorialDoctor.fxml", event); }
    @FXML private void cerrarSesion(ActionEvent event)       { cambiarVista("Sesion.fxml", event); }


    private void cambiarVista(String ruta, ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(ruta));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
