package com.example.integradora;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.*;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.ResourceBundle;

public class HistorialDoctorController implements Initializable, ConCuenta {

    private int idCuenta; // solo para navegación

    private static final String API_URL = "https://yjfzidepdnusqufayeli.supabase.co/rest/v1/";
    private static final String API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InlqZnppZGVwZG51c3F1ZmF5ZWxpIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTEyNTUyMDMsImV4cCI6MjA2NjgzMTIwM30.Kq7hVKEWbYWObRpiZ2yxgSWpRQS3V0MBGRIZxji09lU";

    @FXML private TableView<CitaAgendada> tablaCitas;
    @FXML private TableColumn<CitaAgendada, String> colMatricula, colNombre, colFecha, colHora, colMalestar, colObservaciones;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // AHORA esta columna muestra el nombre del doctor (propiedad "doctor")
        colMatricula.setCellValueFactory(new PropertyValueFactory<>("doctor"));
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colFecha.setCellValueFactory(new PropertyValueFactory<>("fecha"));
        colHora.setCellValueFactory(new PropertyValueFactory<>("hora"));
        colMalestar.setCellValueFactory(new PropertyValueFactory<>("malestar"));
        colObservaciones.setCellValueFactory(new PropertyValueFactory<>("observaciones"));
    }

    @Override
    public void setIdCuenta(int idCuenta) {
        this.idCuenta = idCuenta;
        cargarCitas();
    }

    private void cargarCitas() {
        ObservableList<CitaAgendada> lista = FXCollections.observableArrayList();

        try {
            // Traemos solo citas ATENDIDAS e incluimos id_empleado para obtener el doctor
            String select = enc("id_paciente,id_empleado,fecha,hora,motivo,observaciones,estado");
            String urlStr = API_URL + "cita?select=" + select + "&estado=eq.ATENDIDA";
            HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("apikey", API_KEY);
            conn.setRequestProperty("Authorization", "Bearer " + API_KEY);

            StringBuilder response = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                for (String line; (line = reader.readLine()) != null; ) response.append(line);
            }

            JSONArray citas = new JSONArray(response.toString());
            for (int i = 0; i < citas.length(); i++) {
                JSONObject obj = citas.getJSONObject(i);
                String idPaciente = String.valueOf(obj.optInt("id_paciente"));
                int idEmpleado = obj.optInt("id_empleado", 0);
                String fecha = obj.optString("fecha", "");
                String hora = obj.optString("hora", "");
                String malestar = obj.optString("motivo", "");
                String observaciones = obj.optString("observaciones", "");

                // buscamos datos del paciente (activo)
                String[] paciente = buscarDatosPaciente(idPaciente);
                // buscamos nombre del doctor
                String doctor = (idEmpleado > 0) ? buscarNombreEmpleado(idEmpleado) : "Desconocido";

                if (paciente != null) {
                    lista.add(new CitaAgendada(
                            doctor,            // <- en la primera columna saldrá el doctor
                            paciente[1],       // nombre del paciente
                            fecha, hora, malestar, observaciones
                    ));
                }
            }

            tablaCitas.setItems(lista);
        } catch (Exception e) {
            System.out.println("❌ Error al cargar citas:");
            e.printStackTrace();
        }
    }

    private String[] buscarDatosPaciente(String idPaciente) {
        try {
            // Filtramos por id y estado ACTIVO para no traer toda la tabla
            String select = enc("matricula,nombre,estado");
            String urlStr = API_URL + "paciente?select=" + select
                    + "&id_paciente=eq." + enc(idPaciente)
                    + "&estado=eq.ACTIVO&limit=1";
            HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("apikey", API_KEY);
            conn.setRequestProperty("Authorization", "Bearer " + API_KEY);

            StringBuilder response = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                for (String line; (line = reader.readLine()) != null; ) response.append(line);
            }

            JSONArray arr = new JSONArray(response.toString());
            if (!arr.isEmpty()) {
                JSONObject p = arr.getJSONObject(0);
                String matricula = p.optString("matricula", "");
                String nombre = p.optString("nombre", "");
                return new String[]{matricula, nombre};
            }
        } catch (Exception e) {
            System.out.println("❌ Error al obtener datos del paciente:");
            e.printStackTrace();
        }
        return null;
    }

    private String buscarNombreEmpleado(int idEmpleado) {
        try {
            // Pide campos que existan en tu tabla. Si tienes apellidos, agrégalos aquí.
            String select = enc("nombre");
            String urlStr = API_URL + "empleado?select=" + select + "&id_empleado=eq." + idEmpleado + "&limit=1";
            HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("apikey", API_KEY);
            conn.setRequestProperty("Authorization", "Bearer " + API_KEY);

            StringBuilder response = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                for (String line; (line = reader.readLine()) != null; ) response.append(line);
            }

            JSONArray arr = new JSONArray(response.toString());
            if (!arr.isEmpty()) {
                JSONObject e = arr.getJSONObject(0);
                String nombre = e.optString("nombre", "").trim();
                if (!nombre.isEmpty()) return nombre;
            }
        } catch (Exception ex) {
            System.out.println("⚠️ No se pudo obtener el nombre del empleado (id_empleado=" + idEmpleado + "):");
            ex.printStackTrace();
        }
        return "Desconocido";
    }

    private String enc(String s) {
        try { return URLEncoder.encode(s, StandardCharsets.UTF_8.toString()); }
        catch (Exception e) { return s; }
    }

    private void cambiarVista(String fxml, ActionEvent event) {
        try {
            System.out.println("🔁 Cambiando a: " + fxml);
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/integradora/" + fxml));
            Parent root = loader.load();

            Object controller = loader.getController();
            if (controller instanceof ConCuenta cuentaController) {
                cuentaController.setIdCuenta(idCuenta);
            }

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("GECIME");
            stage.show();

        } catch (Exception e) {
            System.err.println("❌ Error al cambiar vista:");
            e.printStackTrace();
        }
    }


    // Botones laterales
    @FXML private void irAVistaAgendar(ActionEvent event)    { cambiarVista("AgendarCitaAdmin.fxml", event); }
    @FXML private void irAVistaAgendadas(ActionEvent event)  { cambiarVista("CitasAgendidasAdmin.fxml", event); }
    @FXML private void irAVistaPacientes(ActionEvent event)  { cambiarVista("Pacientes.fxml", event); }
    @FXML private void irAVistaDoctores(ActionEvent event)   { cambiarVista("Doctores.fxml", event); }
    @FXML private void irAVistaPerfil(ActionEvent event)     { cambiarVista("Admin.fxml", event); }
    @FXML private void cerrarSesion(ActionEvent event)       { cambiarVista("Sesion.fxml", event); }
    @FXML private void irAVistaHistorial(ActionEvent event) { cambiarVista("HistorialDoctor.fxml", event); }

    @FXML private void irAVistaConsultorios(ActionEvent event) {
        cambiarVista("Consultorios.fxml", event);
    }
    // ===== Modelo para la tabla (doctor en vez de matrícula) =====
    public static class CitaAgendada {
        private String doctor, nombre, fecha, hora, malestar, observaciones;
        public CitaAgendada(String doctor, String nombre, String fecha,
                            String hora, String malestar, String observaciones) {
            this.doctor = doctor; this.nombre = nombre; this.fecha = fecha;
            this.hora = hora; this.malestar = malestar; this.observaciones = observaciones;
        }
        public String getDoctor() { return doctor; }
        public String getNombre() { return nombre; }
        public String getFecha() { return fecha; }
        public String getHora() { return hora; }
        public String getMalestar() { return malestar; }
        public String getObservaciones() { return observaciones; }
    }
}
