package com.example.integradora;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class AgendarCitaController implements Initializable, ConCuenta {

    private static final String API_URL = "https://yjfzidepdnusqufayeli.supabase.co/rest/v1/";
    private static final String API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InlqZnppZGVwZG51c3F1ZmF5ZWxpIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTEyNTUyMDMsImV4cCI6MjA2NjgzMTIwM30.Kq7hVKEWbYWObRpiZ2yxgSWpRQS3V0MBGRIZxji09lU";

    private int idCuenta;
    private int idPaciente;
    private final Map<String, Integer> nombreDoctorToId = new HashMap<>();

    private static final DateTimeFormatter FMT_OUT = DateTimeFormatter.ofPattern("HH:mm");

    @FXML private Label nombrePacienteLabel;
    @FXML private ComboBox<String> comboDoctor;
    @FXML private DatePicker fechaPicker;
    @FXML private ComboBox<String> comboHora;
    @FXML private TextArea malestarArea;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        configurarFechaPicker();
        comboDoctor.setOnAction(e -> verificarYActualizarHoras());
        fechaPicker.setOnAction(e -> verificarYActualizarHoras());
    }

    // ===== ConCuenta =====
    @Override
    public void setIdCuenta(int idCuenta) {
        this.idCuenta = idCuenta;
        obtenerIdPaciente();
        cargarDoctoresActivos();
    }

    // ===== Paciente =====
    private void obtenerIdPaciente() {
        try {
            String urlStr = API_URL + "paciente?select=" + enc("id_paciente,nombre,id_cuenta")
                    + "&id_cuenta=eq." + enc(String.valueOf(idCuenta));
            HttpURLConnection conn = crearConexionGET(new URL(urlStr));
            String respuesta = leerRespuesta(conn);

            JSONArray array = new JSONArray(respuesta);
            if (array.length() == 0) {
                mostrarAlerta("⚠️ No se encontró paciente.");
                return;
            }
            JSONObject p = array.getJSONObject(0);
            idPaciente = p.getInt("id_paciente");
            nombrePacienteLabel.setText(p.getString("nombre"));
        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta("❌ Error cargando paciente.");
        }
    }

    // ===== Doctores (rol doctor, estado ACTIVO) =====
    private void cargarDoctoresActivos() {
        try {
            String urlStr = API_URL + "empleado?"
                    + "select=" + enc("id_empleado,nombre,especialidad")
                    + "&rol=eq." + enc("doctor")
                    + "&estado=eq." + enc("ACTIVO")
                    + "&order=" + enc("nombre.asc");
            HttpURLConnection conn = crearConexionGET(new URL(urlStr));
            String respuesta = leerRespuesta(conn);

            JSONArray array = new JSONArray(respuesta);
            List<String> doctores = new ArrayList<>();

            for (int i = 0; i < array.length(); i++) {
                JSONObject emp = array.getJSONObject(i);
                int id = emp.getInt("id_empleado");
                String nombre = emp.optString("nombre", "Sin nombre");
                String esp = emp.optString("especialidad", "");
                String etiqueta = esp.isBlank() ? nombre : (nombre + " - " + esp);
                doctores.add(etiqueta);
                nombreDoctorToId.put(etiqueta, id);
            }

            comboDoctor.setItems(FXCollections.observableArrayList(doctores));
        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta("❌ Error cargando doctores.");
        }
    }

    // ===== Fecha: no antes de hoy, no domingos, máximo 2 semanas =====
    private void configurarFechaPicker() {
        final LocalDate hoy = LocalDate.now();
        final LocalDate max = hoy.plusWeeks(2);

        fechaPicker.setDayCellFactory(dp -> new DateCell() {
            @Override public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                boolean invalida = empty
                        || date.isBefore(hoy)
                        || date.isAfter(max)
                        || date.getDayOfWeek() == DayOfWeek.SUNDAY;
                setDisable(invalida);
            }
        });
    }

    // ===== Horas disponibles considerando horario del doctor + choques doctor/paciente =====
    private void verificarYActualizarHoras() {
        String doctor = comboDoctor.getValue();
        LocalDate fecha = fechaPicker.getValue();
        if (doctor != null && fecha != null) {
            int idEmpleado = nombreDoctorToId.getOrDefault(doctor, 0);
            if (idEmpleado > 0) {
                cargarHorasDisponibles(idEmpleado, fecha);
            }
        }
    }

    private void cargarHorasDisponibles(int idEmpleado, LocalDate fecha) {
        try {
            // 1) obtener horario del doctor
            LocalTime[] rango = obtenerHorarioDoctor(idEmpleado);
            LocalTime inicio = rango[0], fin = rango[1];

            // 2) generar slots cada 30 min dentro del rango [inicio, fin)
            List<String> horasTodas = new ArrayList<>();
            for (LocalTime t = inicio; t.isBefore(fin); t = t.plusMinutes(30)) {
                horasTodas.add(t.format(FMT_OUT));
            }

            // 3) horas ocupadas por ese DOCTOR en esa fecha (estado=AGENDADA)
            Set<String> horasOcupadas = new HashSet<>();
            String urlDoc = API_URL + "cita?"
                    + "select=" + enc("hora")
                    + "&id_empleado=eq." + enc(String.valueOf(idEmpleado))
                    + "&fecha=eq." + enc(fecha.toString())
                    + "&estado=eq." + enc("AGENDADA");
            horasOcupadas.addAll(obtenerHoras(urlDoc));

            // 4) horas ocupadas por el PACIENTE en esa fecha (cualquier doctor), evita doble booking
            String urlPac = API_URL + "cita?"
                    + "select=" + enc("hora")
                    + "&id_paciente=eq." + enc(String.valueOf(idPaciente))
                    + "&fecha=eq." + enc(fecha.toString())
                    + "&estado=eq." + enc("AGENDADA");
            horasOcupadas.addAll(obtenerHoras(urlPac));

            // 5) remover ocupadas
            horasTodas.removeAll(horasOcupadas);

            // 6) (Opcional) si fecha es hoy, eliminar horas pasadas
            // if (fecha.equals(LocalDate.now())) {
            //     LocalTime ahora = LocalTime.now().withSecond(0).withNano(0);
            //     horasTodas.removeIf(h -> LocalTime.parse(h, FMT_OUT).isBefore(ahora));
            // }

            comboHora.setItems(FXCollections.observableArrayList(horasTodas));

            if (horasTodas.isEmpty()) {
                mostrarAlerta("⚠️ No hay horas disponibles para este doctor en esa fecha.");
            }

        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta("❌ Error cargando horas.");
        }
    }

    private Set<String> obtenerHoras(String endpoint) throws Exception {
        HttpURLConnection conn = crearConexionGET(new URL(endpoint));
        String respuesta = leerRespuesta(conn);
        JSONArray array = new JSONArray(respuesta);
        Set<String> horas = new HashSet<>();
        for (int i = 0; i < array.length(); i++) {
            String raw = array.getJSONObject(i).getString("hora");
            horas.add(normalizarHora(raw));
        }
        return horas;
    }

    /** horario del doctor desde 'empleado' (hora_inicio/hora_fin pueden venir con o sin segundos) */
    private LocalTime[] obtenerHorarioDoctor(int idEmpleado) throws Exception {
        String urlStr = API_URL + "empleado?"
                + "select=" + enc("hora_inicio,hora_fin")
                + "&id_empleado=eq." + enc(String.valueOf(idEmpleado))
                + "&limit=1";
        HttpURLConnection conn = crearConexionGET(new URL(urlStr));
        String respuesta = leerRespuesta(conn);

        JSONArray arr = new JSONArray(respuesta);
        if (arr.length() == 0) throw new IllegalStateException("El doctor no tiene horario definido.");

        JSONObject o = arr.getJSONObject(0);
        LocalTime inicio = parseHoraFlexible(o.optString("hora_inicio", "09:00").trim());
        LocalTime fin    = parseHoraFlexible(o.optString("hora_fin", "17:00").trim());
        if (!fin.isAfter(inicio)) throw new IllegalStateException("Horario inválido: fin <= inicio.");
        return new LocalTime[]{inicio, fin};
    }

    // ===== Agendar =====
    @FXML
    private void agendarCita() {
        String doctor = comboDoctor.getValue();
        LocalDate fecha = fechaPicker.getValue();
        String hora = comboHora.getValue();
        String motivo = malestarArea.getText();

        if (doctor == null || fecha == null || hora == null || motivo == null || motivo.isBlank()) {
            mostrarAlerta("⚠️ Por favor completa todos los campos.");
            return;
        }

        // validación extra contra doble booking del paciente
        if (pacienteTieneCitaEn(fecha, hora)) {
            mostrarAlerta("⚠️ Ya tienes una cita AGENDADA a esa hora en esa fecha.");
            return;
        }

        try {
            int idEmpleado = nombreDoctorToId.getOrDefault(doctor, 0);

            JSONObject cita = new JSONObject();
            cita.put("id_paciente", idPaciente);
            cita.put("id_empleado", idEmpleado);
            cita.put("fecha", fecha.toString());
            cita.put("hora", normalizarHora(hora)); // guardar HH:mm
            cita.put("motivo", motivo);
            cita.put("estado", "AGENDADA");
            cita.put("duracion", 30);

            HttpURLConnection conn = (HttpURLConnection) new URL(API_URL + "cita").openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("apikey", API_KEY);
            conn.setRequestProperty("Authorization", "Bearer " + API_KEY);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(cita.toString().getBytes(StandardCharsets.UTF_8));
            }

            int code = conn.getResponseCode();
            if (code == 201) {
                mostrarAlerta("✅ Cita agendada correctamente.");
                limpiarCampos();
            } else {
                String error = leerRespuesta(conn);
                System.err.println("❌ Error al guardar cita: " + error);
                mostrarAlerta("❌ No se pudo guardar la cita.");
            }

        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta("❌ Error al enviar cita.");
        }
    }

    private boolean pacienteTieneCitaEn(LocalDate fecha, String horaHHmm) {
        try {
            String url = API_URL + "cita?"
                    + "select=" + enc("id_cita")
                    + "&id_paciente=eq." + enc(String.valueOf(idPaciente))
                    + "&fecha=eq." + enc(fecha.toString())
                    + "&hora=eq." + enc(normalizarHora(horaHHmm))
                    + "&estado=eq." + enc("AGENDADA");
            HttpURLConnection conn = crearConexionGET(new URL(url));
            String respuesta = leerRespuesta(conn);
            return new JSONArray(respuesta).length() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            // ante error, prevenir doble booking
            return true;
        }
    }

    // ===== Utilidades HTTP / hora =====
    private HttpURLConnection crearConexionGET(URL url) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("apikey", API_KEY);
        conn.setRequestProperty("Authorization", "Bearer " + API_KEY);
        conn.setRequestProperty("Accept", "application/json");
        return conn;
    }

    private String leerRespuesta(HttpURLConnection conn) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                conn.getResponseCode() < 400 ? conn.getInputStream() : conn.getErrorStream(),
                StandardCharsets.UTF_8
        ));
        StringBuilder response = new StringBuilder();
        String linea;
        while ((linea = reader.readLine()) != null) response.append(linea);
        reader.close();
        return response.toString();
    }

    // 🔧 Encodificador: compatible con todos los JDKs (evita “Expected 2 arguments but found 1”)
    private String enc(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8"); // <- usa String encoding, no Charset
        } catch (UnsupportedEncodingException e) {
            return s;
        }
    }

    /** Parsea '09:00', '9:00' o '09:00:00' */
    private LocalTime parseHoraFlexible(String s) {
        if (s == null) throw new IllegalArgumentException("Hora nula");
        String t = s.trim();
        try { return LocalTime.parse(t); } catch (Exception ignored) {}
        try { return LocalTime.parse(t, DateTimeFormatter.ofPattern("H:mm")); } catch (Exception ignored) {}
        try { return LocalTime.parse(t, DateTimeFormatter.ofPattern("H:mm:ss")); } catch (Exception ignored) {}
        try { return LocalTime.parse(t, DateTimeFormatter.ofPattern("HH:mm")); } catch (Exception ignored) {}
        try { return LocalTime.parse(t, DateTimeFormatter.ofPattern("HH:mm:ss")); } catch (Exception ignored) {}
        throw new IllegalArgumentException("Formato de hora no válido: " + s);
    }

    /** Normaliza a 'HH:mm' */
    private String normalizarHora(String s) {
        try { return parseHoraFlexible(s).format(FMT_OUT); }
        catch (Exception ex) { return s; }
    }

    // ===== Limpieza =====
    private void limpiarCampos() {
        comboDoctor.getSelectionModel().clearSelection();
        comboHora.getItems().clear();
        fechaPicker.setValue(null);
        malestarArea.clear();
    }

    // ===== Navegación =====
    @FXML private void irAVistaAgendar(ActionEvent e)    { cambiarVista("/com/example/integradora/AgendarCita.fxml", e); }
    @FXML private void irAVistaPerfil(ActionEvent e)     { cambiarVista("/com/example/integradora/Usuario.fxml", e); }
    @FXML private void irAVistaHistorial(ActionEvent e)  { cambiarVista("/com/example/integradora/HistorialCitas.fxml", e); }
    @FXML private void irAVistaAgendadas(ActionEvent e)  { cambiarVista("/com/example/integradora/CitasAgendadas.fxml", e); }
    @FXML private void cerrarSesion(ActionEvent event)       { cambiarVista("Sesion.fxml", event); }


    private void cambiarVista(String rutaFXML, ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(rutaFXML));
            Parent root = loader.load();
            Object controller = loader.getController();
            if (controller instanceof ConCuenta cc) cc.setIdCuenta(idCuenta);
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("GECIME");
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta("❌ No se pudo cambiar de vista.");
        }
    }

    private void mostrarAlerta(String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.INFORMATION);
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }
}
