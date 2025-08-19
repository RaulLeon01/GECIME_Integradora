package com.example.integradora;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.scene.Node;
import javafx.beans.property.ReadOnlyObjectWrapper;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.ResourceBundle;

public class DoctoresController implements Initializable {

    @FXML private TableView<Doctor> tablaDoctores;
    @FXML private TableColumn<Doctor, String> colNombre;
    @FXML private TableColumn<Doctor, String> colEstado;
    @FXML private TableColumn<Doctor, HBox>   colOpciones;

    private static final String API_URL = "https://yjfzidepdnusqufayeli.supabase.co/rest/v1/";
    private static final String API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InlqZnppZGVwZG51c3F1ZmF5ZWxpIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTEyNTUyMDMsImV4cCI6MjA2NjgzMTIwM30.Kq7hVKEWbYWObRpiZ2yxgSWpRQS3V0MBGRIZxji09lU";

    private final ObservableList<Doctor> listaDoctores = FXCollections.observableArrayList();
    private final HttpClient http = HttpClient.newHttpClient();

    @Override
    public void initialize(java.net.URL url, ResourceBundle resourceBundle) {
        colNombre.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getNombre()));
        colEstado.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getEstado()));
        colOpciones.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getOpciones()));
        cargarDoctores();
    }

    private void cargarDoctores() {
        listaDoctores.clear();
        try {
            String endpoint = API_URL + "empleado?rol=eq.doctor&order=nombre.asc";
            HttpURLConnection conn = (HttpURLConnection) new URL(endpoint).openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("apikey", API_KEY);
            conn.setRequestProperty("Authorization", "Bearer " + API_KEY);

            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                for (String line; (line = br.readLine()) != null; ) sb.append(line);

                JSONArray arr = new JSONArray(sb.toString());
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject emp = arr.getJSONObject(i);
                    int idEmpleado  = emp.getInt("id_empleado");
                    String nombre   = emp.optString("nombre", "Sin nombre");
                    String estado   = emp.optString("estado", "DESCONOCIDO");

                    // Botón: Ver doctor -> VerDoctor.fxml
                    Button btnVer = new Button("Ver doctor");
                    btnVer.setStyle("-fx-background-color:#2F4F76; -fx-text-fill:white; -fx-font-weight:bold;");
                    btnVer.setOnAction(e -> abrirVerDoctor(idEmpleado));

                    // Botón: Activar/Inactivar (con confirmación)
                    Button btnToggle = new Button(estado.equalsIgnoreCase("ACTIVO") ? "Inactivar" : "Activar");
                    btnToggle.setStyle("-fx-background-color:#6b7a99; -fx-text-fill:white; -fx-font-weight:bold;");
                    btnToggle.setOnAction(e -> {
                        String nuevo = estado.equalsIgnoreCase("ACTIVO") ? "INACTIVO" : "ACTIVO";
                        if (confirmarCambio(nombre, nuevo)) {
                            cambiarEstadoEmpleado(idEmpleado, nuevo);
                        }
                    });

                    HBox opciones = new HBox(8, btnVer, btnToggle);
                    opciones.setStyle("-fx-alignment:CENTER_LEFT;");

                    listaDoctores.add(new Doctor(idEmpleado, nombre, estado, opciones));
                }
            }

            tablaDoctores.setItems(listaDoctores);
        } catch (Exception ex) {
            ex.printStackTrace();
            alerta("Error", "No se pudieron cargar los doctores.");
        }
    }

    private boolean confirmarCambio(String nombreDoctor, String nuevoEstado) {
        String accion = nuevoEstado.equalsIgnoreCase("ACTIVO") ? "ACTIVAR" : "INACTIVAR";
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmar cambio");
        alert.setHeaderText(null);
        alert.setContentText("¿Está seguro de " + accion + " la cuenta del doctor \"" + nombreDoctor + "\"?");
        ButtonType si  = new ButtonType("Sí", ButtonBar.ButtonData.OK_DONE);
        ButtonType no  = new ButtonType("No", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(si, no);
        Optional<ButtonType> res = alert.showAndWait();
        return res.isPresent() && res.get() == si;
    }

    private void cambiarEstadoEmpleado(int idEmpleado, String nuevoEstado) {
        try {
            String url = API_URL + "empleado?id_empleado=eq." + idEmpleado;
            String json = "{\"estado\":\"" + nuevoEstado + "\"}";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(url))
                    .method("PATCH", HttpRequest.BodyPublishers.ofString(json))
                    .header("apikey", API_KEY)
                    .header("Authorization", "Bearer " + API_KEY)
                    .header("Content-Type", "application/json")
                    .header("Prefer", "return=minimal")
                    .build();

            HttpResponse<String> resp = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 204) {
                cargarDoctores(); // refresca la tabla y texto del botón
            } else {
                System.err.println("PATCH empleado -> " + resp.statusCode() + ": " + resp.body());
                alerta("Error", "No se pudo cambiar el estado (código " + resp.statusCode() + ").");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            alerta("Error", "Ocurrió un problema al cambiar el estado.");
        }
    }

    private void abrirVerDoctor(int idEmpleado) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/integradora/VerDoctor.fxml"));
            Parent root = loader.load();

            Object controller = loader.getController();
            if (controller != null) {
                try { controller.getClass().getMethod("setIdEmpleado", int.class).invoke(controller, idEmpleado); }
                catch (NoSuchMethodException ignored) {}
            }

            Stage stage = (Stage) tablaDoctores.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Ver doctor");
            stage.show();
        } catch (Exception ex) {
            ex.printStackTrace();
            alerta("Error", "No se pudo abrir la vista VerDoctor.");
        }
    }

    // Navegación (si la usas)
    private void cambiarVista(String fxml, ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/integradora/" + fxml));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Administrador - GECIME");
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            alerta("Error", "No se pudo cambiar de vista.");
        }
    }

    @FXML private void irAVistaAgendar(ActionEvent e)    { cambiarVista("AgendarCitaAdmin.fxml", e); }
    @FXML private void irAVistaAgendadas(ActionEvent e)  { cambiarVista("CitasAgendidasAdmin.fxml", e); }
    @FXML private void irAVistaPacientes(ActionEvent e)  { cambiarVista("Pacientes.fxml", e); }
    @FXML private void irAVistaDoctores(ActionEvent e)   { cambiarVista("Doctores.fxml", e); }
    @FXML private void irAVistaPerfil(ActionEvent e)     { cambiarVista("Admin.fxml", e); }
    @FXML private void cerrarSesion(ActionEvent e)       { cambiarVista("Sesion.fxml", e); }
    @FXML private void registrarDoctor(ActionEvent e)    { cambiarVista("RegistrarDoctores.fxml", e); }
    @FXML private void irAVistaHistorial(ActionEvent event) { cambiarVista("HistorialDoctor.fxml", event); }
    @FXML private void irAVistaConsultorios(ActionEvent event) {
        cambiarVista("Consultorios.fxml", event);
    }

    // ===== Modelo =====
    public static class Doctor {
        private final int idEmpleado;
        private final String nombre;
        private final String estado;
        private final HBox opciones;

        public Doctor(int idEmpleado, String nombre, String estado, HBox opciones) {
            this.idEmpleado = idEmpleado;
            this.nombre = nombre;
            this.estado = estado;
            this.opciones = opciones;
        }

        public int  getIdEmpleado() { return idEmpleado; }
        public String getNombre()   { return nombre; }
        public String getEstado()   { return estado; }
        public HBox  getOpciones()  { return opciones; }
    }

    private void alerta(String titulo, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(titulo);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}
