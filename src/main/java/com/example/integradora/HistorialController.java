package com.example.integradora;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
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
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ResourceBundle;

public class HistorialController implements Initializable, ConCuenta {

    private static final String API_URL = "https://yjfzidepdnusqufayeli.supabase.co/rest/v1/";
    private static final String API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InlqZnppZGVwZG51c3F1ZmF5ZWxpIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTEyNTUyMDMsImV4cCI6MjA2NjgzMTIwM30.Kq7hVKEWbYWObRpiZ2yxgSWpRQS3V0MBGRIZxji09lU";

    private int idCuenta;

    @FXML private TableView<CitaHistorial> tablaCitas;
    @FXML private TableColumn<CitaHistorial, String> colMatricula;
    @FXML private TableColumn<CitaHistorial, String> colNombre;
    @FXML private TableColumn<CitaHistorial, String> colDoctor;
    @FXML private TableColumn<CitaHistorial, String> colFecha;
    @FXML private TableColumn<CitaHistorial, String> colHora;
    @FXML private TableColumn<CitaHistorial, String> colMalestar;
    @FXML private TableColumn<CitaHistorial, String> colObservaciones;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        configurarTabla();
    }

    @Override
    public void setIdCuenta(int idCuenta) {
        this.idCuenta = idCuenta;
        cargarCitasAtendidas();
    }

    private void configurarTabla() {
        colMatricula.setCellValueFactory(new PropertyValueFactory<>("matricula"));
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colDoctor.setCellValueFactory(new PropertyValueFactory<>("doctor"));
        colFecha.setCellValueFactory(new PropertyValueFactory<>("fecha"));
        colHora.setCellValueFactory(new PropertyValueFactory<>("hora"));
        colMalestar.setCellValueFactory(new PropertyValueFactory<>("malestar"));
        colObservaciones.setCellValueFactory(new PropertyValueFactory<>("observaciones"));
    }

    private void cargarCitasAtendidas() {
        try {
            JSONArray pacientesArray = getJSONArrayFromURL(API_URL + "paciente?select=id_paciente,id_cuenta,nombre,matricula");
            JSONArray citasArray = getJSONArrayFromURL(API_URL + "cita?select=id_cita,id_paciente,id_empleado,fecha,hora,motivo,estado,observaciones");
            JSONArray empleadosArray = getJSONArrayFromURL(API_URL + "empleado?select=id_empleado,nombre");

            // Buscar id_paciente correspondiente al idCuenta
            int idPaciente = -1;
            String nombrePaciente = "";
            String matricula = "";

            for (int i = 0; i < pacientesArray.length(); i++) {
                JSONObject paciente = pacientesArray.getJSONObject(i);
                if (paciente.getInt("id_cuenta") == idCuenta) {
                    idPaciente = paciente.getInt("id_paciente");
                    nombrePaciente = paciente.getString("nombre");
                    matricula = paciente.getString("matricula");
                    break;
                }
            }

            if (idPaciente == -1) {
                System.out.println("⚠️ No se encontró paciente con idCuenta=" + idCuenta);
                return;
            }

            ObservableList<CitaHistorial> lista = FXCollections.observableArrayList();

            for (int i = 0; i < citasArray.length(); i++) {
                JSONObject cita = citasArray.getJSONObject(i);
                if (!cita.getString("estado").equalsIgnoreCase("ATENDIDA")) continue;
                if (cita.getInt("id_paciente") != idPaciente) continue;

                int idEmpleado = cita.getInt("id_empleado");
                String nombreDoctor = "Desconocido";

                for (int j = 0; j < empleadosArray.length(); j++) {
                    JSONObject doctor = empleadosArray.getJSONObject(j);
                    if (doctor.getInt("id_empleado") == idEmpleado) {
                        nombreDoctor = doctor.getString("nombre");
                        break;
                    }
                }

                lista.add(new CitaHistorial(
                        matricula,
                        nombrePaciente,
                        nombreDoctor,
                        cita.getString("fecha"),
                        cita.getString("hora"),
                        cita.optString("motivo", ""),
                        cita.optString("observaciones", "")
                ));
            }

            tablaCitas.setItems(lista);
            System.out.println("✅ Historial cargado correctamente: " + lista.size() + " cita(s)");

        } catch (Exception e) {
            System.err.println("❌ Error al cargar historial:");
            e.printStackTrace();
        }
    }

    private JSONArray getJSONArrayFromURL(String urlStr) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr.replace(" ", "%20")).openConnection();
        conn.setRequestProperty("apikey", API_KEY);
        conn.setRequestProperty("Authorization", "Bearer " + API_KEY);
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder(); String line;
        while ((line = reader.readLine()) != null) sb.append(line);
        return new JSONArray(sb.toString());
    }

    private void cambiarVista(String fxml, ActionEvent event) {
        try {
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

    @FXML private void irAVistaAgendar(ActionEvent event) {
        cambiarVista("AgendarCita.fxml", event);
    }

    @FXML private void irAVistaHistorial(ActionEvent event) {
        cambiarVista("HistorialCitas.fxml", event);
    }

    @FXML private void irAVistaAgendadas(ActionEvent event) {
        cambiarVista("CitasAgendadas.fxml", event);
    }

    @FXML private void irAVistaPerfil(ActionEvent event) {
        cambiarVista("Usuario.fxml", event);
    }

    @FXML private void cerrarSesion(ActionEvent event)       { cambiarVista("Sesion.fxml", event); }


    // Clase interna como modelo (no se separa)
    public static class CitaHistorial {
        private final String matricula, nombre, doctor, fecha, hora, malestar, observaciones;

        public CitaHistorial(String matricula, String nombre, String doctor, String fecha, String hora, String malestar, String observaciones) {
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
