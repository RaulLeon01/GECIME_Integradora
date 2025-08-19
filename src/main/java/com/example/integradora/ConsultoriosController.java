package com.example.integradora;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.*;
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
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

// Para PATCH con HttpClient
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;

public class ConsultoriosController implements Initializable {

    @FXML private TableView<ConsultorioRow> tablaConsultorios;
    @FXML private TableColumn<ConsultorioRow, String> colNombre;
    @FXML private TableColumn<ConsultorioRow, String> colUbicacion;
    @FXML private TableColumn<ConsultorioRow, String> colEstado;
    @FXML private TableColumn<ConsultorioRow, HBox>  colOpciones;

    private static final String API_URL = "https://yjfzidepdnusqufayeli.supabase.co/rest/v1/";
    private static final String API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InlqZnppZGVwZG51c3F1ZmF5ZWxpIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTEyNTUyMDMsImV4cCI6MjA2NjgzMTIwM30.Kq7hVKEWbYWObRpiZ2yxgSWpRQS3V0MBGRIZxji09lU";

    private final ObservableList<ConsultorioRow> data = FXCollections.observableArrayList();
    private final HttpClient http = HttpClient.newHttpClient();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        colNombre.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getNombre()));
        colUbicacion.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getUbicacion()));
        colEstado.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getEstado()));
        colOpciones.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getOpciones()));
        cargarConsultorios();
    }

    private void cargarConsultorios() {
        data.clear();
        try {
            String select = enc("id,nombre,ubicacion,estado");
            String order  = enc("nombre.asc");
            URL url = new URL(API_URL + "consultorio?select=" + select + "&order=" + order);
            HttpURLConnection conn = crearGET(url);
            String body = leer(conn);

            JSONArray arr = new JSONArray(body);
            List<ConsultorioRow> rows = new ArrayList<>();

            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                int id = o.getInt("id");
                String nombre = o.optString("nombre", "Consultorio " + id);
                String ubic = o.optString("ubicacion", "");
                String estado = o.optString("estado", "DESCONOCIDO");

                Button toggle = new Button(estado.equalsIgnoreCase("ACTIVO") ? "Inactivar" : "Activar");
                toggle.setStyle("-fx-background-color:#2F4F76; -fx-text-fill:white; -fx-font-weight:bold;");
                final int idF = id;
                final String estadoActual = estado;
                toggle.setOnAction(e -> confirmarYActualizar(idF, estadoActual));

                HBox ops = new HBox(toggle);
                ops.setSpacing(8);

                rows.add(new ConsultorioRow(id, nombre, ubic, estado, ops));
            }

            data.setAll(rows);
            tablaConsultorios.setItems(data);

        } catch (Exception e) {
            e.printStackTrace();
            alerta("Error", "No se pudieron cargar los consultorios.");
        }
    }

    private void confirmarYActualizar(int id, String estadoActual) {
        String proximo = estadoActual.equalsIgnoreCase("ACTIVO") ? "INACTIVO" : "ACTIVO";
        Alert conf = new Alert(Alert.AlertType.CONFIRMATION,
                "¿Seguro que desea cambiar el estado a " + proximo + "?", ButtonType.YES, ButtonType.NO);
        conf.setHeaderText(null);
        conf.setTitle("Confirmar");
        conf.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.YES) cambiarEstado(id, proximo);
        });
    }

    // PATCH con HttpClient para evitar ProtocolException
    private void cambiarEstado(int id, String nuevoEstado) {
        try {
            String url = API_URL + "consultorio?id=eq." + id;
            String json = "{\"estado\":\"" + nuevoEstado + "\"}";

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("apikey", API_KEY)
                    .header("Authorization", "Bearer " + API_KEY)
                    .header("Content-Type", "application/json")
                    .header("Prefer", "return=minimal")
                    .method("PATCH", HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());

            if (resp.statusCode() == 204) {
                cargarConsultorios();
            } else {
                alerta("Error", "No se pudo actualizar. Código: " + resp.statusCode() + "\n" + resp.body());
            }
        } catch (Exception e) {
            e.printStackTrace();
            alerta("Error", "Falló la actualización de estado.");
        }
    }

    // ----- utilidades HTTP/UI -----
    private HttpURLConnection crearGET(URL url) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("apikey", API_KEY);
        conn.setRequestProperty("Authorization", "Bearer " + API_KEY);
        conn.setRequestProperty("Accept", "application/json");
        return conn;
    }

    private String leer(HttpURLConnection conn) throws Exception {
        int code = conn.getResponseCode();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                code < 400 ? conn.getInputStream() : conn.getErrorStream(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            for (String ln; (ln = br.readLine()) != null; ) sb.append(ln);
            if (code >= 400) throw new RuntimeException("HTTP " + code + " -> " + sb);
            return sb.toString();
        }
    }

    private String enc(String s) {
        try { return URLEncoder.encode(s, StandardCharsets.UTF_8.toString()); }
        catch (Exception e) { return s; }
    }

    private void alerta(String titulo, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(titulo);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    // ----- navegación lateral -----
    private void cambiarVista(String fxml, ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/integradora/" + fxml));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Administrador - GECIME");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML private void irAVistaPerfil(ActionEvent e)       { cambiarVista("Admin.fxml", e); }
    @FXML private void irAVistaAgendadas(ActionEvent e)    { cambiarVista("CitasAgendidasAdmin.fxml", e); }
    @FXML private void irAVistaAgendar(ActionEvent e)      { cambiarVista("AgendarCitaAdmin.fxml", e); }
    @FXML private void irAVistaPacientes(ActionEvent e)    { cambiarVista("Pacientes.fxml", e); }
    @FXML private void irAVistaDoctores(ActionEvent e)     { cambiarVista("Doctores.fxml", e); }
    @FXML private void irAVistaConsultorios(ActionEvent e) { cambiarVista("Consultorios.fxml", e); }
    @FXML private void cerrarSesion(ActionEvent e)         { cambiarVista("Sesion.fxml", e); }
    @FXML private void irARegistrarConsultorio(ActionEvent e) {
        cambiarVista("AgregarConsultorio.fxml", e);
    }
    @FXML private void irAVistaHistorial(ActionEvent event) { cambiarVista("HistorialDoctor.fxml", event); }

    // ----- modelo para la tabla -----
    public static class ConsultorioRow {
        private final int id;
        private final String nombre, ubicacion, estado;
        private final HBox opciones;

        public ConsultorioRow(int id, String nombre, String ubicacion, String estado, HBox opciones) {
            this.id = id; this.nombre = nombre; this.ubicacion = ubicacion; this.estado = estado; this.opciones = opciones;
        }
        public int getId() { return id; } // no se muestra, pero útil internamente
        public String getNombre() { return nombre; }
        public String getUbicacion() { return ubicacion; }
        public String getEstado() { return estado; }
        public HBox getOpciones() { return opciones; }
    }
}
