package com.example.integradora;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import javafx.scene.Node;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ResourceBundle;

public class CitasAgendadasAdminController implements Initializable {

    private static final String API_URL = "https://yjfzidepdnusqufayeli.supabase.co/rest/v1/";
    private static final String API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InlqZnppZGVwZG51c3F1ZmF5ZWxpIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTEyNTUyMDMsImV4cCI6MjA2NjgzMTIwM30.Kq7hVKEWbYWObRpiZ2yxgSWpRQS3V0MBGRIZxji09lU";

    @FXML private TableView<CitaAgendada> tablaCitas;
    @FXML private TableColumn<CitaAgendada, String> colMatricula;
    @FXML private TableColumn<CitaAgendada, String> colNombre;
    @FXML private TableColumn<CitaAgendada, String> colDoctor;
    @FXML private TableColumn<CitaAgendada, String> colFecha;
    @FXML private TableColumn<CitaAgendada, String> colHora;
    @FXML private TableColumn<CitaAgendada, String> colMalestar;

    private final ObservableList<CitaAgendada> listaCitas = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        colMatricula.setCellValueFactory(new PropertyValueFactory<>("matricula"));
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colDoctor.setCellValueFactory(new PropertyValueFactory<>("doctor"));
        colFecha.setCellValueFactory(new PropertyValueFactory<>("fecha"));
        colHora.setCellValueFactory(new PropertyValueFactory<>("hora"));
        colMalestar.setCellValueFactory(new PropertyValueFactory<>("malestar"));
        cargarCitas();
    }

    private void cargarCitas() {
        listaCitas.clear();
        try {
            // Obtener todas las citas
            HttpURLConnection conn = (HttpURLConnection) new URL(API_URL + "cita").openConnection();
            conn.setRequestProperty("apikey", API_KEY);
            conn.setRequestProperty("Authorization", "Bearer " + API_KEY);
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder respuesta = new StringBuilder();
            String linea;
            while ((linea = reader.readLine()) != null) respuesta.append(linea);
            reader.close();
            JSONArray citas = new JSONArray(respuesta.toString());

            // Obtener todos los pacientes
            conn = (HttpURLConnection) new URL(API_URL + "paciente").openConnection();
            conn.setRequestProperty("apikey", API_KEY);
            conn.setRequestProperty("Authorization", "Bearer " + API_KEY);
            reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            respuesta = new StringBuilder();
            while ((linea = reader.readLine()) != null) respuesta.append(linea);
            reader.close();
            JSONArray pacientes = new JSONArray(respuesta.toString());

            // Obtener todos los doctores
            conn = (HttpURLConnection) new URL(API_URL + "empleado").openConnection();
            conn.setRequestProperty("apikey", API_KEY);
            conn.setRequestProperty("Authorization", "Bearer " + API_KEY);
            reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            respuesta = new StringBuilder();
            while ((linea = reader.readLine()) != null) respuesta.append(linea);
            reader.close();
            JSONArray doctores = new JSONArray(respuesta.toString());

            for (int i = 0; i < citas.length(); i++) {
                JSONObject cita = citas.getJSONObject(i);
                if (!cita.getString("estado").equalsIgnoreCase("AGENDADA")) continue;

                int idPaciente = cita.getInt("id_paciente");
                int idEmpleado = cita.getInt("id_empleado");

                String nombrePaciente = "";
                String matriculaPaciente = "";
                for (int j = 0; j < pacientes.length(); j++) {
                    JSONObject pac = pacientes.getJSONObject(j);
                    if (pac.getInt("id_paciente") == idPaciente) {
                        nombrePaciente = pac.getString("nombre");
                        matriculaPaciente = pac.getString("matricula");
                        break;
                    }
                }

                String nombreDoctor = "";
                for (int j = 0; j < doctores.length(); j++) {
                    JSONObject doc = doctores.getJSONObject(j);
                    if (doc.getInt("id_empleado") == idEmpleado) {
                        nombreDoctor = doc.getString("nombre");
                        break;
                    }
                }

                listaCitas.add(new CitaAgendada(
                        matriculaPaciente,
                        nombrePaciente,
                        nombreDoctor,
                        cita.getString("fecha"),
                        cita.getString("hora"),
                        cita.getString("motivo")
                ));
            }

            tablaCitas.setItems(listaCitas);
            System.out.println("✅ Citas cargadas correctamente.");
        } catch (Exception e) {
            System.err.println("❌ Error al cargar citas:");
            e.printStackTrace();
        }
    }

    @FXML private void irAVistaAgendar(ActionEvent event) { cambiarVista("/com/example/integradora/AgendarCitaAdmin.fxml", event); }
    @FXML private void irAVistaAgendadas(ActionEvent event) { cambiarVista("/com/example/integradora/CitasAgendadasAdmin.fxml", event); }
    @FXML private void irAVistaPacientes(ActionEvent event) { cambiarVista("/com/example/integradora/Pacientes.fxml", event); }
    @FXML private void irAVistaDoctores(ActionEvent event) { cambiarVista("/com/example/integradora/Doctores.fxml", event); }
    @FXML private void irAVistaPerfil(ActionEvent event) { cambiarVista("/com/example/integradora/Admin.fxml", event); }
    @FXML private void cerrarSesion(ActionEvent event) { cambiarVista("/com/example/integradora/Sesion.fxml", event); }
    @FXML private void irAVistaHistorial(ActionEvent event) { cambiarVista("HistorialDoctor.fxml", event); }
    @FXML private void irAVistaConsultorios(ActionEvent event) {
        cambiarVista("Consultorios.fxml", event);
    }

    private void cambiarVista(String ruta, ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(ruta));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Administrador - GECIME");
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static class CitaAgendada {
        private final String matricula, nombre, doctor, fecha, hora, malestar;

        public CitaAgendada(String matricula, String nombre, String doctor, String fecha, String hora, String malestar) {
            this.matricula = matricula;
            this.nombre = nombre;
            this.doctor = doctor;
            this.fecha = fecha;
            this.hora = hora;
            this.malestar = malestar;
        }

        public String getMatricula() { return matricula; }
        public String getNombre() { return nombre; }
        public String getDoctor() { return doctor; }
        public String getFecha() { return fecha; }
        public String getHora() { return hora; }
        public String getMalestar() { return malestar; }
    }
}
