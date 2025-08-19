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
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ResourceBundle;

public class Encursocontroller implements Initializable, ConCuenta {

    public TableView<CitaAgendada> tablaCitas;
    public TableColumn<CitaAgendada, String> colMatricula;
    public TableColumn<CitaAgendada, String> colNombre;
    public TableColumn<CitaAgendada, String> colApellidos;
    public TableColumn<CitaAgendada, String> colFecha;
    public TableColumn<CitaAgendada, String> colHora;
    public TableColumn<CitaAgendada, String> colMalestar;
    public TableColumn<CitaAgendada, Button> colBoton;

    private int idCuenta;
    private int idEmpleado;

    private static final String API_URL = "https://yjfzidepdnusqufayeli.supabase.co/rest/v1/";
    private static final String API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InlqZnppZGVwZG51c3F1ZmF5ZWxpIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTEyNTUyMDMsImV4cCI6MjA2NjgzMTIwM30.Kq7hVKEWbYWObRpiZ2yxgSWpRQS3V0MBGRIZxji09lU"; // su clave completa aquí

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        colMatricula.setCellValueFactory(new PropertyValueFactory<>("matricula"));
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colFecha.setCellValueFactory(new PropertyValueFactory<>("fecha"));
        colHora.setCellValueFactory(new PropertyValueFactory<>("hora"));
        colMalestar.setCellValueFactory(new PropertyValueFactory<>("malestar"));
        colBoton.setCellValueFactory(new PropertyValueFactory<>("boton"));
    }

    @Override
    public void setIdCuenta(int idCuenta) {
        this.idCuenta = idCuenta;
        cargarCitas();
    }

    private void cargarCitas() {
        try {
            JSONArray empleados = obtenerDatosDesdeAPI(API_URL + "empleado");
            for (int i = 0; i < empleados.length(); i++) {
                JSONObject empleado = empleados.getJSONObject(i);
                if (empleado.getInt("id_cuenta") == idCuenta) {
                    idEmpleado = empleado.getInt("id_empleado");
                    break;
                }
            }

            JSONArray pacientes = obtenerDatosDesdeAPI(API_URL + "paciente");
            JSONArray citas = obtenerDatosDesdeAPI(API_URL + "cita");
            ObservableList<CitaAgendada> lista = FXCollections.observableArrayList();

            for (int i = 0; i < citas.length(); i++) {
                JSONObject cita = citas.getJSONObject(i);
                if (!"AGENDADA".equals(cita.getString("estado"))) continue;
                if (cita.getInt("id_empleado") != idEmpleado) continue;

                int idCita = cita.getInt("id_cita");
                int idPaciente = cita.getInt("id_paciente");
                JSONObject paciente = buscarPacientePorId(pacientes, idPaciente);
                if (paciente == null) continue;

                String matricula = paciente.optString("matricula", "");
                String nombre = paciente.optString("nombre", "");
                String fecha = cita.optString("fecha", "");
                String hora = cita.optString("hora", "");
                String malestar = cita.optString("motivo", "");

                Button boton = new Button("Tomar cita");
                int finalIdCita = idCita;
                int finalIdPaciente = idPaciente;
                boton.setOnAction(e -> irAVistaTomarCita(e, finalIdCita, finalIdPaciente, nombre, malestar, fecha, hora));

                lista.add(new CitaAgendada(matricula, nombre, fecha, hora, malestar, boton));
            }

            if (lista.isEmpty()) {
                mostrarAlerta("Sin citas", "No hay citas agendadas para mostrar.");
            }

            tablaCitas.setItems(lista);

        } catch (Exception e) {
            System.out.println("Error al cargar citas:");
            e.printStackTrace();
        }
    }

    private JSONObject buscarPacientePorId(JSONArray pacientes, int idPaciente) {
        for (int i = 0; i < pacientes.length(); i++) {
            JSONObject paciente = pacientes.getJSONObject(i);
            if (paciente.getInt("id_paciente") == idPaciente) {
                return paciente;
            }
        }
        return null;
    }

    private JSONArray obtenerDatosDesdeAPI(String urlString) throws Exception {
        URL url = new URL(urlString + "?select=*");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestProperty("apikey", API_KEY);
        conn.setRequestProperty("Authorization", "Bearer " + API_KEY);
        conn.setRequestMethod("GET");
        conn.connect();

        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
        StringBuilder resultado = new StringBuilder();
        String linea;
        while ((linea = reader.readLine()) != null) {
            resultado.append(linea);
        }
        return new JSONArray(resultado.toString());
    }

    private void mostrarAlerta(String titulo, String contenido) {
        Alert alerta = new Alert(Alert.AlertType.INFORMATION);
        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        alerta.setContentText(contenido);
        alerta.showAndWait();
    }

    private void irAVistaTomarCita(ActionEvent event, int idCita, int idPaciente, String nombre, String malestar, String fecha, String hora) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("TomarCita.fxml"));
            Parent root = loader.load();

            TomarCitaController controller = loader.getController();
            controller.setDatos(idCuenta, idEmpleado, idCita, idPaciente, nombre, malestar, fecha, hora);

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta("Error", "No se pudo abrir la vista de Tomar Cita.");
        }
    }

    public void irAVistaAgendar(ActionEvent event) {
        cambiarVista("HistorialPacientes.fxml", event);
    }

    public void irAVistaAgendadas(ActionEvent event) {
        cambiarVista("EnCurso.fxml", event);
    }

    public void irAVistaHistorial(ActionEvent event) {
        cambiarVista("HistorialDoctor.fxml", event);
    }

    public void irAVistaPerfil(ActionEvent event) {
        cambiarVista("PerfilDoctor.fxml", event);
    }

    @FXML
    private void cerrarSesion(ActionEvent event) {
        cambiarVista("Sesion.fxml", event);
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

    // Clase interna como lo solicitó, sin .java externos
    public static class CitaAgendada {
        private final String matricula;
        private final String nombre;
        private final String fecha;
        private final String hora;
        private final String malestar;
        private final Button boton;

        public CitaAgendada(String matricula, String nombre, String fecha, String hora, String malestar, Button boton) {
            this.matricula = matricula;
            this.nombre = nombre;
            this.fecha = fecha;
            this.hora = hora;
            this.malestar = malestar;
            this.boton = boton;
        }

        public String getMatricula() { return matricula; }
        public String getNombre() { return nombre; }
        public String getFecha() { return fecha; }
        public String getHora() { return hora; }
        public String getMalestar() { return malestar; }
        public Button getBoton() { return boton; }
    }
}
