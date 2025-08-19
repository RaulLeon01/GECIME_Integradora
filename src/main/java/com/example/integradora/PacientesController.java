package com.example.integradora;

import javafx.beans.property.ReadOnlyObjectWrapper;
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
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;
import java.util.ResourceBundle;

public class PacientesController implements Initializable {

    @FXML private TableView<Paciente> tablaPacientes;
    // Eliminadas: colId, colApellidos
    @FXML private TableColumn<Paciente, String> colNombre;
    @FXML private TableColumn<Paciente, String> colEstado;
    @FXML private TableColumn<Paciente, HBox>   colOpciones;

    private static final String API_URL = "https://yjfzidepdnusqufayeli.supabase.co/rest/v1/";
    private static final String API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InlqZnppZGVwZG51c3F1ZmF5ZWxpIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTEyNTUyMDMsImV4cCI6MjA2NjgzMTIwM30.Kq7hVKEWbYWObRpiZ2yxgSWpRQS3V0MBGRIZxji09lU";

    private final ObservableList<Paciente> listaPacientes = FXCollections.observableArrayList();
    private final HttpClient http = HttpClient.newHttpClient();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        colNombre.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getNombre()));
        colEstado.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getEstado()));
        colOpciones.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getOpciones()));
        cargarPacientes();
    }

    private void cargarPacientes() {
        listaPacientes.clear();
        try {
            HttpURLConnection conn = (HttpURLConnection)
                    new URL(API_URL + "paciente").openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("apikey", API_KEY);
            conn.setRequestProperty("Authorization", "Bearer " + API_KEY);

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder resultado = new StringBuilder();
            String linea;
            while ((linea = reader.readLine()) != null) resultado.append(linea);
            reader.close();

            JSONArray pacientes = new JSONArray(resultado.toString());

            for (int i = 0; i < pacientes.length(); i++) {
                JSONObject pac = pacientes.getJSONObject(i);
                int idPaciente = pac.getInt("id_paciente");
                String nombre = pac.optString("nombre", "SinNombre");
                String estado = pac.optString("estado", "DESCONOCIDO");

                // Botón Activar/Inactivar con confirmación
                Button btnEstado = new Button(estado.equalsIgnoreCase("ACTIVO") ? "Inactivar" : "Activar");
                btnEstado.setStyle("-fx-background-color: #2F4F76; -fx-text-fill: white; -fx-font-weight: bold;");
                String nuevoEstado = estado.equalsIgnoreCase("ACTIVO") ? "INACTIVO" : "ACTIVO";
                btnEstado.setOnAction(e -> confirmarCambioEstado(idPaciente, nuevoEstado));

                // Botón Ver información
                Button btnVer = new Button("Ver información");
                btnVer.setOnAction(e -> abrirVerInformacion(idPaciente, e));

                HBox opciones = new HBox(8, btnEstado, btnVer);

                listaPacientes.add(new Paciente(idPaciente, nombre, estado, opciones));
            }

            tablaPacientes.setItems(listaPacientes);
            System.out.println("✅ Pacientes cargados correctamente.");

        } catch (Exception e) {
            System.err.println("❌ Error al cargar pacientes:");
            e.printStackTrace();
        }
    }

    private void confirmarCambioEstado(int idPaciente, String nuevoEstado) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "¿Estás seguro de cambiar el estado de la cuenta a \"" + nuevoEstado + "\"?",
                ButtonType.YES, ButtonType.NO);
        alert.setTitle("Confirmar cambio de estado");
        alert.setHeaderText(null);

        Optional<ButtonType> res = alert.showAndWait();
        if (res.isPresent() && res.get() == ButtonType.YES) {
            cambiarEstadoPaciente(idPaciente, nuevoEstado);
        } else {
            System.out.println("↩ Cambio de estado cancelado por el usuario.");
        }
    }

    private void cambiarEstadoPaciente(int idPaciente, String nuevoEstado) {
        try {
            String url = API_URL + "paciente?id_paciente=eq." + idPaciente;
            String jsonBody = "{\"estado\": \"" + nuevoEstado + "\"}";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(url))
                    .method("PATCH", HttpRequest.BodyPublishers.ofString(jsonBody))
                    .header("apikey", API_KEY)
                    .header("Authorization", "Bearer " + API_KEY)
                    .header("Content-Type", "application/json")
                    .header("Prefer", "return=minimal")
                    .build();

            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 204) {
                System.out.println("✔ Estado actualizado correctamente a " + nuevoEstado);
                cargarPacientes();
            } else {
                System.out.println("✘ Error al actualizar estado. Código: " + response.statusCode());
                System.out.println("Respuesta: " + response.body());
            }
        } catch (Exception e) {
            System.err.println("❌ Error al cambiar estado:");
            e.printStackTrace();
        }
    }

    private void abrirVerInformacion(int idPaciente, ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/integradora/VerInformacion.fxml"));
            Parent root = loader.load();

            // Pasar idPaciente si el controller lo soporta
            Object controller = loader.getController();
            try {
                controller.getClass().getMethod("setIdPaciente", int.class).invoke(controller, idPaciente);
            } catch (NoSuchMethodException ignored) {
                System.out.println("ℹ VerInformacionController no tiene setIdPaciente(int).");
            }

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Ver Información del Paciente");
            stage.show();
        } catch (IOException ex) {
            System.err.println("❌ Error al abrir VerInformacion.fxml");
            ex.printStackTrace();
        } catch (Exception ex) {
            System.err.println("❌ Error al pasar parámetros a VerInformacionController");
            ex.printStackTrace();
        }
    }

    // Navegación entre vistas existentes
    private void cambiarVista(String fxml, ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/integradora/" + fxml));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Administrador - GECIME");
            stage.show();
        } catch (IOException e) {
            System.err.println("❌ Error al cambiar vista a: " + fxml);
            e.printStackTrace();
        }
    }

    @FXML private void irAVistaAgendar(ActionEvent event)   { cambiarVista("AgendarCitaAdmin.fxml", event); }
    @FXML private void irAVistaAgendadas(ActionEvent event) { cambiarVista("CitasAgendidasAdmin.fxml", event); }
    @FXML private void irAVistaPacientes(ActionEvent event) { cambiarVista("Pacientes.fxml", event); }
    @FXML private void irAVistaDoctores(ActionEvent event)  { cambiarVista("Doctores.fxml", event); }
    @FXML private void irAVistaPerfil(ActionEvent event)    { cambiarVista("Admin.fxml", event); }
    @FXML private void cerrarSesion(ActionEvent event)      { cambiarVista("Sesion.fxml", event); }
    @FXML private void irARegistrarPaciente(ActionEvent event) { cambiarVista("AgregarPacientesAdmin.fxml", event); }
    @FXML private void irAVistaHistorial(ActionEvent event) { cambiarVista("HistorialDoctor.fxml", event); }
    @FXML private void irAVistaConsultorios(ActionEvent event) {
        cambiarVista("Consultorios.fxml", event);
    }

    // ===== Modelo =====
    public static class Paciente {
        private final int idPaciente;
        private final String nombre;
        private final String estado;
        private final HBox opciones;

        public Paciente(int idPaciente, String nombre, String estado, HBox opciones) {
            this.idPaciente = idPaciente;
            this.nombre = nombre;
            this.estado = estado;
            this.opciones = opciones;
        }

        public int getIdPaciente() { return idPaciente; }
        public String getNombre() { return nombre; }
        public String getEstado() { return estado; }
        public HBox getOpciones() { return opciones; }
    }
}
