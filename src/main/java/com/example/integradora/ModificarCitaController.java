package com.example.integradora;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.DateCell;
import javafx.stage.Stage;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ModificarCitaController implements ConCuenta, Initializable {

    @FXML private ComboBox<String> comboDoctor; // "123 - Nombre"
    @FXML private ComboBox<String> comboHora;   // "HH:mm"
    @FXML private DatePicker fechaPicker;

    private int idCuenta;
    private int idCita;
    private int idPaciente;
    private int idMedico;          // médico original de la cita
    private String horaOriginal;   // "HH:mm"
    private LocalDate fechaOriginal;

    private static final String API_URL = "https://yjfzidepdnusqufayeli.supabase.co/rest/v1/";
    private static final String API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InlqZnppZGVwZG51c3F1ZmF5ZWxpIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTEyNTUyMDMsImV4cCI6MjA2NjgzMTIwM30.Kq7hVKEWbYWObRpiZ2yxgSWpRQS3V0MBGRIZxji09lU";

    private static final DateTimeFormatter FMT_OUT = DateTimeFormatter.ofPattern("HH:mm");

    private final HttpClient http = HttpClient.newHttpClient();

    @Override
    public void initialize(java.net.URL location, java.util.ResourceBundle resources) {
        configurarFechaPicker();
        fechaPicker.setOnAction(e -> cargarHorasDisponibles());
    }

    @Override
    public void setIdCuenta(int idCuenta) { this.idCuenta = idCuenta; }

    /** Llamar al abrir la vista con los datos actuales de la cita */
    public void setCita(int idCita, int idPaciente, int idMedico, String fecha, String hora) {
        this.idCita = idCita;
        this.idPaciente = idPaciente;
        this.idMedico = idMedico;
        this.horaOriginal = normalizarHora(hora);
        this.fechaOriginal = LocalDate.parse(fecha);
        fechaPicker.setValue(fechaOriginal);

        cargarDoctores(() -> {
            for (String item : comboDoctor.getItems()) {
                if (item.startsWith(idMedico + " - ")) {
                    comboDoctor.getSelectionModel().select(item);
                    break;
                }
            }
            cargarHorasDisponibles();
        });
    }

    // ------------------ Fechas válidas ------------------

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

    // ------------------ Doctores activos ------------------

    private void cargarDoctores(Runnable afterLoad) {
        try {
            String endpoint = API_URL + "empleado?"
                    + "select=" + enc("id_empleado,nombre")
                    + "&rol=eq." + enc("doctor")
                    + "&estado=eq." + enc("ACTIVO")
                    + "&order=" + enc("nombre.asc");

            JSONArray arr = httpGetJsonArray(endpoint);
            List<String> doctores = new ArrayList<>();
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                int id = o.has("id_empleado") ? o.getInt("id_empleado") : o.getInt("id");
                String nombre = o.optString("nombre", "Sin nombre");
                doctores.add(id + " - " + nombre);
            }
            comboDoctor.setItems(FXCollections.observableArrayList(doctores));
            comboDoctor.setOnAction(e -> cargarHorasDisponibles());

            if (afterLoad != null) afterLoad.run();
            if (doctores.isEmpty()) mostrarAlerta("No se encontraron doctores ACTIVO.");
        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta("Error al cargar doctores.");
        }
    }

    // ------------------ Horario y horas disponibles ------------------

    /** Lee hora_inicio y hora_fin (puede venir '09:00' o '09:00:00') del empleado */
    private LocalTime[] obtenerHorarioDoctor(int idDoctor) throws Exception {
        String url = API_URL + "empleado?"
                + "select=" + enc("hora_inicio,hora_fin")
                + "&id_empleado=eq." + enc(String.valueOf(idDoctor))
                + "&limit=1";

        JSONArray arr = httpGetJsonArray(url);
        if (arr.length() == 0) throw new IllegalStateException("El doctor no tiene horario definido.");

        JSONObject o = arr.getJSONObject(0);
        LocalTime inicio = parseHoraFlexible(o.optString("hora_inicio", "09:00").trim());
        LocalTime fin    = parseHoraFlexible(o.optString("hora_fin", "17:00").trim());
        if (!fin.isAfter(inicio)) throw new IllegalStateException("Horario inválido: fin <= inicio.");
        return new LocalTime[]{inicio, fin};
    }

    private void cargarHorasDisponibles() {
        comboHora.getItems().clear();
        String doctorSeleccionado = comboDoctor.getSelectionModel().getSelectedItem();
        LocalDate fechaSel = fechaPicker.getValue();
        if (doctorSeleccionado == null || fechaSel == null) return;

        int idDoctor = Integer.parseInt(doctorSeleccionado.split(" - ")[0]);

        try {
            // 1) rango horario del doctor
            LocalTime[] horario = obtenerHorarioDoctor(idDoctor);
            LocalTime inicio = horario[0], fin = horario[1];

            // 2) slots cada 30 min [inicio, fin)
            List<String> todasHoras = new ArrayList<>();
            for (LocalTime t = inicio; t.isBefore(fin); t = t.plusMinutes(30)) {
                todasHoras.add(t.format(FMT_OUT));
            }

            // 3) horas ocupadas por ese doctor en esa fecha
            String endpointDoc = API_URL + "cita?"
                    + "select=" + enc("hora")
                    + "&id_empleado=eq." + enc(String.valueOf(idDoctor))
                    + "&fecha=eq." + enc(fechaSel.toString())
                    + "&estado=eq." + enc("AGENDADA");

            JSONArray arrDoc = httpGetJsonArray(endpointDoc);
            Set<String> horasOcupadas = new HashSet<>();
            for (int i = 0; i < arrDoc.length(); i++) {
                horasOcupadas.add(normalizarHora(arrDoc.getJSONObject(i).getString("hora")));
            }

            // 4) horas ocupadas por el PACIENTE en esa fecha (cualquier doctor), excluyendo esta misma cita
            String endpointPac = API_URL + "cita?"
                    + "select=" + enc("hora")
                    + "&id_paciente=eq." + enc(String.valueOf(idPaciente))
                    + "&fecha=eq." + enc(fechaSel.toString())
                    + "&estado=eq." + enc("AGENDADA")
                    + "&id_cita=neq." + enc(String.valueOf(idCita));

            JSONArray arrPac = httpGetJsonArray(endpointPac);
            for (int i = 0; i < arrPac.length(); i++) {
                horasOcupadas.add(normalizarHora(arrPac.getJSONObject(i).getString("hora")));
            }

            // 5) liberar la hora original si es mismo doctor y fecha
            if (fechaOriginal != null && horaOriginal != null
                    && fechaSel.equals(fechaOriginal) && idDoctor == this.idMedico) {
                horasOcupadas.remove(normalizarHora(horaOriginal));
            }

            // 6) disponibles = todas - ocupadas
            todasHoras.removeAll(horasOcupadas);

            // (opcional) si fechaSel es hoy, quitar horas pasadas:
            // if (fechaSel.equals(LocalDate.now())) {
            //     LocalTime ahora = LocalTime.now().withSecond(0).withNano(0);
            //     todasHoras.removeIf(h -> LocalTime.parse(h, FMT_OUT).isBefore(ahora));
            // }

            comboHora.setItems(FXCollections.observableArrayList(todasHoras));
            if (todasHoras.isEmpty()) mostrarAlerta("No hay horarios disponibles para esa fecha.");
        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta("Error al cargar horarios disponibles.");
        }
    }

    // ------------------ Acciones ------------------

    @FXML
    private void modificarCita() {
        String doctorSeleccionado = comboDoctor.getSelectionModel().getSelectedItem();
        String nuevaHora = comboHora.getSelectionModel().getSelectedItem();
        LocalDate nuevaFecha = fechaPicker.getValue();

        if (doctorSeleccionado == null || nuevaHora == null || nuevaFecha == null) {
            mostrarAlerta("Faltan campos por llenar.");
            return;
        }

        int idDoctorNuevo = Integer.parseInt(doctorSeleccionado.split(" - ")[0]);

        try {
            // Validación extra: que el paciente no tenga otra cita a esa hora/fecha
            if (pacienteTieneCitaEn(nuevaFecha, nuevaHora)) {
                mostrarAlerta("El paciente ya tiene una cita a esa hora en esa fecha.");
                return;
            }

            String endpoint = API_URL + "cita?id_cita=eq." + enc(String.valueOf(idCita));

            JSONObject body = new JSONObject();
            body.put("fecha", nuevaFecha.toString());
            body.put("hora", nuevaHora);            // HH:mm
            body.put("id_empleado", idDoctorNuevo); // por si cambió el doctor

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .method("PATCH", HttpRequest.BodyPublishers.ofString(body.toString()))
                    .header("apikey", API_KEY)
                    .header("Authorization", "Bearer " + API_KEY)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .header("Prefer", "return=representation")
                    .build();

            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
                mostrarAlerta("✅ Cita modificada correctamente.");
                this.fechaOriginal = nuevaFecha;
                this.horaOriginal = nuevaHora;
                this.idMedico = idDoctorNuevo;
            } else {
                System.err.println("[DEBUG] PATCH cita RESP ("+resp.statusCode()+"): " + resp.body());
                mostrarAlerta("❌ Error al modificar la cita. Código: " + resp.statusCode());
            }
        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta("Error al modificar la cita.");
        }
    }

    @FXML
    private void eliminarCita() {
        try {
            String endpoint = API_URL + "cita?id_cita=eq." + enc(String.valueOf(idCita));
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .method("DELETE", HttpRequest.BodyPublishers.noBody())
                    .header("apikey", API_KEY)
                    .header("Authorization", "Bearer " + API_KEY)
                    .build();

            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 204) {
                mostrarAlerta("Cita eliminada correctamente.");
            } else {
                System.err.println("[DEBUG] DELETE cita RESP ("+resp.statusCode()+"): " + resp.body());
                mostrarAlerta("Error al eliminar la cita. Código: " + resp.statusCode());
            }
        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta("Error al eliminar la cita.");
        }
    }

    // ------------------ Navegación ------------------

    @FXML private void irAVistaAgendar(ActionEvent e)   { cambiarVista("AgendarCita.fxml", e); }
    @FXML private void irAVistaAgendadas(ActionEvent e){ cambiarVista("CitasAgendadas.fxml", e); }
    @FXML private void irAVistaHistorial(ActionEvent e){ cambiarVista("HistorialCitas.fxml", e); }
    @FXML private void irAVistaPerfil(ActionEvent e)   { cambiarVista("Usuario.fxml", e); }
    @FXML private void regresar(ActionEvent e)         { cambiarVista("CitasAgendadas.fxml", e); }
    @FXML private void cerrarSesion(ActionEvent event)       { cambiarVista("Sesion.fxml", event); }


    private void cambiarVista(String fxml, ActionEvent e) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/integradora/" + fxml));
            Parent root = loader.load();
            Object controller = loader.getController();
            if (controller instanceof ConCuenta c) c.setIdCuenta(idCuenta);
            Stage stage = (Stage) ((Node) e.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("GECIME");
            stage.show();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    // ------------------ HTTP helpers ------------------

    private JSONArray httpGetJsonArray(String endpoint) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .GET()
                .header("apikey", API_KEY)
                .header("Authorization", "Bearer " + API_KEY)
                .header("Accept", "application/json")
                .build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
            return new JSONArray(resp.body());
        }
        throw new RuntimeException("GET " + endpoint + " -> " + resp.statusCode() + ": " + resp.body());
    }

    private String enc(String s) { return URLEncoder.encode(s, StandardCharsets.UTF_8); }

    // ------------------ Hora helpers ------------------

    /** Parsea '09:00', '9:00' o '09:00:00' a LocalTime */
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

    /** Normaliza cualquier texto de hora a 'HH:mm' */
    private String normalizarHora(String s) {
        try { return parseHoraFlexible(s).format(FMT_OUT); }
        catch (Exception ex) { return s; }
    }

    // ------------------ Validación extra de conflicto paciente ------------------

    /** ¿El paciente ya tiene cita AGENDADA a esa fecha/hora (distinta a esta id_cita)? */
    private boolean pacienteTieneCitaEn(LocalDate fecha, String horaHHmm) {
        try {
            String endpoint = API_URL + "cita?"
                    + "select=" + enc("id_cita")
                    + "&id_paciente=eq." + enc(String.valueOf(idPaciente))
                    + "&fecha=eq." + enc(fecha.toString())
                    + "&hora=eq." + enc(horaHHmm)
                    + "&estado=eq." + enc("AGENDADA")
                    + "&id_cita=neq." + enc(String.valueOf(idCita));
            JSONArray arr = httpGetJsonArray(endpoint);
            return arr.length() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            // En caso de error, por seguridad no permitir:
            return true;
        }
    }

    private void mostrarAlerta(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Información");
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}
