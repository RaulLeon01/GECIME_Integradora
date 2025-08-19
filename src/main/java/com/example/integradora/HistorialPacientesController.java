package com.example.integradora;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import org.json.*;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class HistorialPacientesController implements Initializable, ConCuenta {

    @FXML private TextField campoMatricula;
    @FXML private TableView<CitaAgendada> tablaCitas;
    @FXML private TableColumn<CitaAgendada, String> colMatricula, colNombre, colDoctor, colFecha, colHora, colMalestar, colObservaciones;

    private int idCuenta;
    private int idEmpleado; // doctor logueado

    private static final String API_URL = "https://yjfzidepdnusqufayeli.supabase.co/rest/v1/";
    private static final String API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InlqZnppZGVwZG51c3F1ZmF5ZWxpIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTEyNTUyMDMsImV4cCI6MjA2NjgzMTIwM30.Kq7hVKEWbYWObRpiZ2yxgSWpRQS3V0MBGRIZxji09lU";

    // cachés simples
    private final Map<Integer, String> nombreDoctorById = new HashMap<>();
    private final Map<Integer, PacienteInfo> pacienteById = new HashMap<>();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        colMatricula.setCellValueFactory(new PropertyValueFactory<>("matricula"));
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colDoctor.setCellValueFactory(new PropertyValueFactory<>("doctor"));
        colFecha.setCellValueFactory(new PropertyValueFactory<>("fecha"));
        colHora.setCellValueFactory(new PropertyValueFactory<>("hora"));
        colMalestar.setCellValueFactory(new PropertyValueFactory<>("malestar"));
        colObservaciones.setCellValueFactory(new PropertyValueFactory<>("observaciones"));
    }

    @Override
    public void setIdCuenta(int idCuenta) {
        this.idCuenta = idCuenta;
        try {
            // 1) resolver id_empleado del doctor logueado
            String urlEmp = API_URL + "empleado?select=" + enc("id_empleado") +
                    "&id_cuenta=eq." + enc(String.valueOf(idCuenta)) + "&limit=1";
            JSONArray empArr = httpGetJsonArray(urlEmp);
            if (empArr.length() == 0) {
                mostrarAlerta("Sesión", "No se encontró empleado asociado a la cuenta.");
                return;
            }
            idEmpleado = empArr.getJSONObject(0).getInt("id_empleado");

            // 2) precargar catálogos mínimos (nombres)
            precargarCatalogos();

            // 3) mostrar TODAS las ATENDIDA del doctor
            cargarHistorialAtendidas(null);

        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta("Error", "No se pudo inicializar el historial.");
        }
    }

    // ====== Buscar / Filtrar ======

    @FXML
    public void buscarCitasPorMatricula(ActionEvent event) {
        String matricula = campoMatricula.getText().trim();
        try {
            if (matricula.isEmpty()) {
                // sin filtro: todas las ATENDIDA del doctor
                cargarHistorialAtendidas(null);
            } else {
                // filtrar por matrícula exacta
                Integer idPac = resolverIdPacientePorMatricula(matricula);
                if (idPac == null) {
                    mostrarAlerta("Paciente no encontrado", "No existe un paciente con esa matrícula.");
                    tablaCitas.getItems().clear();
                    return;
                }
                cargarHistorialAtendidas(idPac);
            }
        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta("Error", "No se pudo buscar el historial.");
        }
    }

    // Carga historial ATENDIDA del doctor; si idPaciente != null, aplica filtro por paciente
    private void cargarHistorialAtendidas(Integer idPacienteFiltro) throws Exception {
        StringBuilder q = new StringBuilder(API_URL)
                .append("cita?select=").append(enc("id_paciente,id_empleado,fecha,hora,motivo,observaciones,estado"))
                .append("&id_empleado=eq.").append(enc(String.valueOf(idEmpleado)))
                .append("&estado=eq.").append(enc("ATENDIDA"))
                .append("&order=").append(enc("fecha.desc"))
                .append("&order=").append(enc("hora.asc"));

        if (idPacienteFiltro != null) {
            q.append("&id_paciente=eq.").append(enc(String.valueOf(idPacienteFiltro)));
        }

        JSONArray citasArr = httpGetJsonArray(q.toString());
        ObservableList<CitaAgendada> lista = FXCollections.observableArrayList();

        for (int i = 0; i < citasArr.length(); i++) {
            JSONObject c = citasArr.getJSONObject(i);
            int idPac = c.getInt("id_paciente");
            int idDoc = c.getInt("id_empleado");

            PacienteInfo p = pacienteById.get(idPac);
            if (p == null) {
                // fallback puntual si no está en caché
                p = fetchPacienteInfo(idPac);
                if (p != null) pacienteById.put(idPac, p);
            }
            String nombreDoc = nombreDoctorById.getOrDefault(idDoc, "Doctor");

            if (p != null) {
                lista.add(new CitaAgendada(
                        p.matricula,
                        p.nombre,
                        nombreDoc,
                        c.optString("fecha", ""),
                        c.optString("hora", ""),
                        c.optString("motivo", ""),
                        c.optString("observaciones", "")
                ));
            }
        }

        tablaCitas.setItems(lista);

        if (lista.isEmpty()) {
            if (idPacienteFiltro == null) {
                mostrarAlerta("Sin resultados", "No hay citas ATENDIDA en su historial.");
            } else {
                mostrarAlerta("Sin resultados", "No hay citas ATENDIDA para esa matrícula.");
            }
        }
    }

    // ====== Catálogos mínimos ======

    private void precargarCatalogos() throws Exception {
        // nombres de empleados (al menos el propio)
        String urlEmps = API_URL + "empleado?select=" + enc("id_empleado,nombre");
        JSONArray emps = httpGetJsonArray(urlEmps);
        for (int i = 0; i < emps.length(); i++) {
            JSONObject e = emps.getJSONObject(i);
            nombreDoctorById.put(e.getInt("id_empleado"), e.optString("nombre", "Doctor"));
        }

        // pacientes con matricula/nombre (puede ser grande; si quisiera optimizar, podría filtrarse)
        String urlPacs = API_URL + "paciente?select=" + enc("id_paciente,matricula,nombre");
        JSONArray pacs = httpGetJsonArray(urlPacs);
        for (int i = 0; i < pacs.length(); i++) {
            JSONObject p = pacs.getJSONObject(i);
            PacienteInfo info = new PacienteInfo(
                    p.getInt("id_paciente"),
                    p.optString("matricula", ""),
                    p.optString("nombre", "")
            );
            pacienteById.put(info.id, info);
        }
    }

    private Integer resolverIdPacientePorMatricula(String matricula) throws Exception {
        // intentar desde caché
        for (PacienteInfo p : pacienteById.values()) {
            if (p.matricula.equalsIgnoreCase(matricula)) return p.id;
        }
        // fallback puntual
        String url = API_URL + "paciente?select=" + enc("id_paciente,matricula,nombre") +
                "&matricula=eq." + enc(matricula) + "&limit=1";
        JSONArray arr = httpGetJsonArray(url);
        if (arr.length() == 0) return null;
        JSONObject p = arr.getJSONObject(0);
        PacienteInfo info = new PacienteInfo(
                p.getInt("id_paciente"),
                p.optString("matricula", ""),
                p.optString("nombre", "")
        );
        pacienteById.put(info.id, info);
        return info.id;
    }

    private PacienteInfo fetchPacienteInfo(int idPaciente) throws Exception {
        String url = API_URL + "paciente?select=" + enc("id_paciente,matricula,nombre") +
                "&id_paciente=eq." + enc(String.valueOf(idPaciente)) + "&limit=1";
        JSONArray arr = httpGetJsonArray(url);
        if (arr.length() == 0) return null;
        JSONObject p = arr.getJSONObject(0);
        return new PacienteInfo(
                p.getInt("id_paciente"),
                p.optString("matricula", ""),
                p.optString("nombre", "")
        );
    }

    // ====== HTTP helpers ======

    private JSONArray httpGetJsonArray(String endpoint) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(endpoint).openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("apikey", API_KEY);
        conn.setRequestProperty("Authorization", "Bearer " + API_KEY);
        conn.setRequestProperty("Accept", "application/json");
        int code = conn.getResponseCode();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                (code < 400 ? conn.getInputStream() : conn.getErrorStream()),
                StandardCharsets.UTF_8
        ))) {
            StringBuilder sb = new StringBuilder();
            for (String line; (line = br.readLine()) != null; ) sb.append(line);
            if (code >= 200 && code < 300) return new JSONArray(sb.toString());
            throw new RuntimeException("GET " + endpoint + " -> " + code + ": " + sb);
        }
    }

    private String enc(String s) throws UnsupportedEncodingException {
        return URLEncoder.encode(s, "UTF-8");
    }

    private void mostrarAlerta(String titulo, String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.INFORMATION);
        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }

    // ====== Navegación ======

    @FXML public void irAVistaHistorialPacientes(ActionEvent event) { cambiarVista("HistorialPacientes.fxml", event); }
    @FXML public void irAVistaEnCurso(ActionEvent event) { cambiarVista("EnCurso.fxml", event); }
    @FXML public void irAVistaHistorial(ActionEvent event) { cambiarVista("HistorialDoctor.fxml", event); }
    @FXML public void irAVistaPerfil(ActionEvent event) { cambiarVista("PerfilDoctor.fxml", event); }
    @FXML private void cerrarSesion(ActionEvent event)       { cambiarVista("Sesion.fxml", event); }


    private void cambiarVista(String fxml, ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/integradora/" + fxml));
            Parent root = loader.load();
            Object controller = loader.getController();
            if (controller instanceof ConCuenta conCuenta) {
                conCuenta.setIdCuenta(idCuenta);
            }
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            System.out.println("❌ Error al cambiar vista:");
            e.printStackTrace();
        }
    }

    // ====== Modelos ======

    private static class PacienteInfo {
        final int id;
        final String matricula;
        final String nombre;
        PacienteInfo(int id, String matricula, String nombre) {
            this.id = id; this.matricula = matricula; this.nombre = nombre;
        }
    }

    // Clase interna sin apellidos
    public static class CitaAgendada {
        private final String matricula, nombre, doctor, fecha, hora, malestar, observaciones;

        public CitaAgendada(String matricula, String nombre, String doctor,
                            String fecha, String hora, String malestar, String observaciones) {
            this.matricula = matricula;
            this.nombre = nombre;
            this.doctor = doctor;
            this.fecha = fecha;
            this.hora = hora;
            this.malestar = malestar;
            this.observaciones = observaciones;
        }

        public String getMatricula() { return matricula; }
        public String getNombre() { return nombre; }
        public String getDoctor() { return doctor; }
        public String getFecha() { return fecha; }
        public String getHora() { return hora; }
        public String getMalestar() { return malestar; }
        public String getObservaciones() { return observaciones; }
    }
}
