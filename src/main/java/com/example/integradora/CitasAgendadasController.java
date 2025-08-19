package com.example.integradora;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class CitasAgendadasController implements ConCuenta {

    @FXML private TableView<CitaAgendada> tablaAgendadas;
    @FXML private TableColumn<CitaAgendada, String> colMatricula;
    @FXML private TableColumn<CitaAgendada, String> colNombre;
    @FXML private TableColumn<CitaAgendada, String> colDoctor;
    @FXML private TableColumn<CitaAgendada, String> colFecha;
    @FXML private TableColumn<CitaAgendada, String> colHora;
    @FXML private TableColumn<CitaAgendada, String> colMalestar;
    @FXML private TableColumn<CitaAgendada, Button> colModificar;

    private static final String API_URL = "https://yjfzidepdnusqufayeli.supabase.co/rest/v1/";
    private static final String API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InlqZnppZGVwZG51c3F1ZmF5ZWxpIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTEyNTUyMDMsImV4cCI6MjA2NjgzMTIwM30.Kq7hVKEWbYWObRpiZ2yxgSWpRQS3V0MBGRIZxji09lU";
    private int idCuenta;

    @Override
    public void setIdCuenta(int idCuenta) {
        this.idCuenta = idCuenta;
        obtenerDatosYMostrar();
    }

    @FXML
    public void initialize() {
        colMatricula.setCellValueFactory(new PropertyValueFactory<>("matricula"));
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colDoctor.setCellValueFactory(new PropertyValueFactory<>("doctor"));
        colFecha.setCellValueFactory(new PropertyValueFactory<>("fecha"));
        colHora.setCellValueFactory(new PropertyValueFactory<>("hora"));
        colMalestar.setCellValueFactory(new PropertyValueFactory<>("malestar"));
        colModificar.setCellValueFactory(new PropertyValueFactory<>("btnModificar"));
    }

    private void obtenerDatosYMostrar() {
        try {
            String json = httpGet(API_URL + "paciente");
            JSONArray pacientes = new JSONArray(json);

            String idPacienteEncontrado = "";
            String nombre = "";
            String matricula = "";

            for (int i = 0; i < pacientes.length(); i++) {
                JSONObject p = pacientes.getJSONObject(i);
                if (p.optInt("id_cuenta") == idCuenta) {
                    idPacienteEncontrado = String.valueOf(p.optInt("id_paciente"));
                    nombre = p.optString("nombre");
                    matricula = p.optString("matricula");
                    break;
                }
            }

            if (!idPacienteEncontrado.isEmpty()) {
                cargarCitas(Integer.parseInt(idPacienteEncontrado), nombre, matricula);
            } else {
                System.out.println("⚠️ No se encontró ningún paciente con ese ID de cuenta.");
            }

        } catch (Exception e) {
            System.out.println("❌ Error al obtener datos de pacientes:");
            e.printStackTrace();
        }
    }

    private void cargarCitas(int idPaciente, String nombre, String matricula) {
        try {
            String json = httpGet(API_URL + "cita");
            JSONArray citas = new JSONArray(json);

            List<CitaAgendada> lista = new ArrayList<>();

            for (int i = 0; i < citas.length(); i++) {
                JSONObject c = citas.getJSONObject(i);

                int idPacienteCita = c.optInt("id_paciente");
                String estado = c.optString("estado");
                if (idPacienteCita == idPaciente && "AGENDADA".equals(estado)) {

                    int idCita = c.optInt("id_cita");
                    int idEmpleado = c.optInt("id_empleado"); // <<< AQUÍ
                    String fecha = c.optString("fecha");
                    String hora = c.optString("hora");
                    String motivo = c.optString("motivo");

                    String doctor = obtenerNombreEmpleado(idEmpleado);

                    Button btnModificar = new Button("Modificar");
                    btnModificar.setOnAction(e -> abrirVistaModificar(idCita, idPaciente, idEmpleado, fecha, hora));

                    System.out.println("✅ Cita encontrada y mostrada → id_cita: " + idCita);

                    // Constructor de 11 args (se mantiene el placeholder "")
                    lista.add(new CitaAgendada(
                            matricula,
                            nombre,
                            "",        // placeholder (p.ej. consultorio)
                            doctor,    // nombre del médico/empleado
                            fecha,
                            hora,
                            motivo,
                            btnModificar,
                            idCita,
                            idPaciente,
                            idEmpleado
                    ));
                }
            }

            tablaAgendadas.getItems().setAll(lista);
        } catch (Exception e) {
            System.out.println("❌ Error al cargar citas:");
            e.printStackTrace();
        }
    }

    // Consulta el nombre en la tabla empleado por id_empleado
    private String obtenerNombreEmpleado(int idEmpleado) {
        try {
            // Pide solo columnas que EXISTAN en tu tabla.
            String url = API_URL + "empleado?select=nombre&id_empleado=eq." + idEmpleado;
            String json = httpGet(url);

            JSONArray arr = new JSONArray(json);
            if (arr.length() > 0) {
                String nombre = arr.getJSONObject(0).optString("nombre", "").trim();
                if (!nombre.isEmpty()) return nombre;
            }

        } catch (Exception e) {
            System.out.println("⚠️ No se pudo obtener el nombre del médico (id_empleado=" + idEmpleado + "):");
            e.printStackTrace();
        }
        return "Desconocido";
    }

    private void abrirVistaModificar(int idCita, int idPaciente, int idEmpleado, String fecha, String hora) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/integradora/ModificarCita.fxml"));
            Parent root = loader.load();

            ModificarCitaController controller = loader.getController();
            controller.setIdCuenta(idCuenta);
            // Si tu otro controller espera "idMedico", pásale idEmpleado sin problema.
            controller.setCita(idCita, idPaciente, idEmpleado, fecha, hora);

            Stage stage = (Stage) tablaAgendadas.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Modificar Cita");

        } catch (Exception e) {
            System.out.println("❌ Error al cambiar a vista ModificarCita:");
            e.printStackTrace();
        }
    }

    // ===== NAVEGACIÓN LATERAL =====
    @FXML private void irAVistaAgendar(ActionEvent e)   { cambiarVista("AgendarCita.fxml", e); }
    @FXML private void irAVistaHistorial(ActionEvent e) { cambiarVista("HistorialCitas.fxml", e); }
    @FXML private void irAVistaAgendadas(ActionEvent e) { cambiarVista("CitasAgendadas.fxml", e); }
    @FXML private void irAVistaPerfil(ActionEvent e)    { cambiarVista("Usuario.fxml", e); }
    @FXML private void cerrarSesion(ActionEvent event) {
        cambiarVista("Sesion.fxml", event);
    }
    private void cambiarVista(String fxml, ActionEvent e) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/integradora/" + fxml));
            Parent root = loader.load();

            Object controller = loader.getController();
            if (controller instanceof ConCuenta c) {
                c.setIdCuenta(idCuenta);
            }

            Stage stage = (Stage) ((Node) e.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (Exception ex) {
            System.out.println("❌ Error al cambiar de vista:");
            ex.printStackTrace();
        }
    }

    // ===== Utilidad HTTP con mejor logging de errores =====
    private String httpGet(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestProperty("apikey", API_KEY);
        conn.setRequestProperty("Authorization", "Bearer " + API_KEY);

        int code = conn.getResponseCode();
        InputStream is = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line; while ((line = reader.readLine()) != null) sb.append(line);
            if (code >= 400) {
                System.out.println("HTTP " + code + " → " + urlStr);
                System.out.println("Body: " + sb);
                throw new RuntimeException("HTTP " + code + " en " + urlStr);
            }
            return sb.toString();
        }
    }
}
