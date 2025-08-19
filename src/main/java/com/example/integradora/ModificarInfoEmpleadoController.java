package com.example.integradora;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.*;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ModificarInfoEmpleadoController implements Initializable {

    // UI (coinciden con tu FXML)
    @FXML private TextField cedulaField;         // ← cuenta.usuario (editable)
    @FXML private TextField nombreField;         // empleado.nombre
    @FXML private TextField especialidadField;   // empleado.especialidad
    @FXML private TextField telefonoField;       // empleado.telefono
    @FXML private ComboBox<String> consultorioCombo; // empleado.id_consultorio (elige desde lista)
    @FXML private TextField horaInicioField;     // empleado.hora_inicio (HH:mm)
    @FXML private TextField horaFinField;        // empleado.hora_fin   (HH:mm)

    // API
    private static final String API_URL = "https://yjfzidepdnusqufayeli.supabase.co/rest/v1/";
    private static final String API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InlqZnppZGVwZG51c3F1ZmF5ZWxpIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTEyNTUyMDMsImV4cCI6MjA2NjgzMTIwM30.Kq7hVKEWbYWObRpiZ2yxgSWpRQS3V0MBGRIZxji09lU";
    private final HttpClient http = HttpClient.newHttpClient();

    // Estado
    private int idEmpleadoOriginal = -1;   // id_empleado actual (para filtrar en PATCH)
    private int idCuenta = -1;             // para actualizar usuario en cuenta
    private Integer consultorioSeleccionadoId = null;
    private String consultorioSeleccionadoNombre = null;
    private final Map<String, Integer> etiquetaToIdConsultorio = new HashMap<>();

    private static final DateTimeFormatter HHMM   = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter HHMMSS = DateTimeFormatter.ofPattern("HH:mm:ss");

    // A dónde regresar
    private String backFxml = "/com/example/integradora/Doctores.fxml";

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        consultorioCombo.setItems(FXCollections.observableArrayList());
        consultorioCombo.setOnAction(ev -> {
            String et = consultorioCombo.getSelectionModel().getSelectedItem();
            if (et == null) {
                consultorioSeleccionadoId = null;
                consultorioSeleccionadoNombre = null;
            } else {
                consultorioSeleccionadoId = etiquetaToIdConsultorio.get(et);
                consultorioSeleccionadoNombre = extraerNombreDeEtiqueta(et);
            }
        });
    }

    /** Llamar justo después de cargar el FXML */
    public void setIdEmpleado(int idEmpleado) {
        this.idEmpleadoOriginal = idEmpleado;
        cargarConsultorios();
        cargarEmpleadoYCuenta();
    }

    /** (Opcional) vista a la que vuelve Guardar/Cancelar */
    public void setBackFxml(String rutaFxml) {
        if (rutaFxml != null && !rutaFxml.isBlank()) backFxml = rutaFxml;
    }

    // ================== Carga ==================

    private void cargarEmpleadoYCuenta() {
        if (idEmpleadoOriginal <= 0) return;
        try {
            // Empleado: ahora traemos también 'telefono' y 'consultorio' (varchar)
            String urlEmp = API_URL + "empleado?select=" + enc("id_empleado,nombre,especialidad,id_cuenta,hora_inicio,hora_fin,id_consultorio,telefono,consultorio")
                    + "&id_empleado=eq." + enc(String.valueOf(idEmpleadoOriginal)) + "&limit=1";
            HttpURLConnection connEmp = crearGET(new URL(urlEmp));
            String bodyEmp = leer(connEmp);

            JSONArray arr = new JSONArray(bodyEmp);
            if (arr.isEmpty()) { alerta("Aviso", "Empleado no encontrado."); return; }

            JSONObject e = arr.getJSONObject(0);
            String nombre = e.optString("nombre", "");
            String especialidad = e.optString("especialidad", "");
            String hInicio = e.isNull("hora_inicio") ? "" : e.getString("hora_inicio");
            String hFin    = e.isNull("hora_fin")    ? "" : e.getString("hora_fin");
            String telEmp  = e.optString("telefono", "");
            String consNom = e.optString("consultorio", ""); // varchar en empleado
            idCuenta       = e.isNull("id_cuenta")   ? -1 : e.getInt("id_cuenta");
            Integer consId = e.isNull("id_consultorio") ? null : e.getInt("id_consultorio");

            nombreField.setText(nombre);
            especialidadField.setText(especialidad);
            telefonoField.setText(telEmp);
            if (!hInicio.isBlank()) horaInicioField.setText(toHHmm(hInicio));
            if (!hFin.isBlank())    horaFinField.setText(toHHmm(hFin));

            // Preseleccionar consultorio
            if (consId != null) {
                seleccionarConsultorioEnCombo(consId);
            } else if (!consNom.isBlank()) {
                // Si no hay id pero hay nombre textual, intenta encontrarlo por nombre
                for (String et : consultorioCombo.getItems()) {
                    if (extraerNombreDeEtiqueta(et).equalsIgnoreCase(consNom.trim())) {
                        consultorioCombo.getSelectionModel().select(et);
                        consultorioSeleccionadoId = etiquetaToIdConsultorio.get(et);
                        consultorioSeleccionadoNombre = consNom.trim();
                        break;
                    }
                }
            }

            // CUENTA: usuario
            if (idCuenta > 0) {
                String urlCta = API_URL + "cuenta?select=" + enc("usuario")
                        + "&id_cuenta=eq." + idCuenta + "&limit=1";
                HttpURLConnection connCta = crearGET(new URL(urlCta));
                String bodyCta = leer(connCta);
                JSONArray arrC = new JSONArray(bodyCta);
                if (!arrC.isEmpty()) {
                    JSONObject cta = arrC.getJSONObject(0);
                    String usuario = cta.optString("usuario", "");
                    cedulaField.setText(usuario); // “cédula” = usuario
                }
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            alerta("Error", "No se pudieron cargar los datos.");
        }
    }

    private void cargarConsultorios() {
        try {
            String url = API_URL + "consultorio?select=" + enc("id,nombre,ubicacion,estado")
                    + "&estado=eq.ACTIVO&order=" + enc("nombre.asc");
            HttpURLConnection conn = crearGET(new URL(url));
            String body = leer(conn);

            JSONArray arr = new JSONArray(body);
            ObservableList<String> items = FXCollections.observableArrayList();
            etiquetaToIdConsultorio.clear();

            for (int i = 0; i < arr.length(); i++) {
                JSONObject c = arr.getJSONObject(i);
                int id = c.getInt("id");
                String nombre = c.optString("nombre", "Consultorio " + id);
                String ubicacion = c.optString("ubicacion", "");
                String etiqueta = id + " - " + nombre + (ubicacion.isBlank() ? "" : " (" + ubicacion + ")");
                etiquetaToIdConsultorio.put(etiqueta, id);
                items.add(etiqueta);
            }

            consultorioCombo.setItems(items);
            if (consultorioSeleccionadoId != null) seleccionarConsultorioEnCombo(consultorioSeleccionadoId);

        } catch (Exception ex) {
            ex.printStackTrace();
            alerta("Error", "No se pudieron cargar los consultorios.");
        }
    }

    private void seleccionarConsultorioEnCombo(int id) {
        for (String et : consultorioCombo.getItems()) {
            Integer v = etiquetaToIdConsultorio.get(et);
            if (v != null && v == id) {
                consultorioCombo.getSelectionModel().select(et);
                consultorioSeleccionadoNombre = extraerNombreDeEtiqueta(et);
                break;
            }
        }
    }

    // ================== Guardar ==================

    @FXML
    private void onGuardarClick(ActionEvent e) {
        // Validaciones
        String usuario   = safe(cedulaField.getText()); // CUENTA.usuario
        String nombre    = safe(nombreField.getText());
        String espec     = safe(especialidadField.getText());
        String telefono  = safe(telefonoField.getText()); // EMPLEADO.telefono
        String hi        = safe(horaInicioField.getText());
        String hf        = safe(horaFinField.getText());

        if (usuario.isBlank()) { alerta("Validación", "El usuario no puede estar vacío."); return; }
        if (usuario.length() < 5) { alerta("Validación", "El usuario debe tener al menos 5 caracteres."); return; }

        if (nombre.isBlank()) { alerta("Validación", "El nombre no puede estar vacío."); return; }

        if (telefono.isBlank()) { alerta("Validación", "El teléfono es obligatorio."); return; }
        if (!telefono.matches("\\d{10}")) {
            alerta("Validación", "El teléfono debe tener exactamente 10 dígitos (solo números).");
            return;
        }

        if (consultorioSeleccionadoId == null || consultorioSeleccionadoNombre == null) {
            alerta("Validación", "Debe seleccionar un consultorio.");
            return;
        }

        String horaInicio = null, horaFin = null;
        try {
            if (!hi.isBlank()) horaInicio = toHHmmss(hi);
            if (!hf.isBlank()) horaFin    = toHHmmss(hf);
        } catch (Exception exx) {
            alerta("Validación", "Formato de hora inválido. Usa HH:mm (ej. 08:00).");
            return;
        }
        if (horaInicio != null && horaFin != null) {
            if (!LocalTime.parse(horaFin, HHMMSS).isAfter(LocalTime.parse(horaInicio, HHMMSS))) {
                alerta("Validación", "La hora fin debe ser mayor que la hora inicio.");
                return;
            }
        }

        // JSON para empleado (incluye telefono, id_consultorio y consultorio nombre)
        JSONObject emp = new JSONObject();
        emp.put("nombre", nombre);
        emp.put("especialidad", espec);
        emp.put("telefono", telefono);
        emp.put("id_consultorio", consultorioSeleccionadoId);
        emp.put("consultorio", consultorioSeleccionadoNombre); // <-- varchar en empleado
        if (horaInicio != null) emp.put("hora_inicio", horaInicio);
        if (horaFin    != null) emp.put("hora_fin",    horaFin);

        try {
            // PATCH empleado
            String urlEmp = API_URL + "empleado?id_empleado=eq." + idEmpleadoOriginal;
            HttpResponse<String> respEmp = http.send(
                    HttpRequest.newBuilder()
                            .uri(URI.create(urlEmp))
                            .method("PATCH", HttpRequest.BodyPublishers.ofString(emp.toString()))
                            .header("apikey", API_KEY)
                            .header("Authorization", "Bearer " + API_KEY)
                            .header("Content-Type", "application/json")
                            .header("Prefer", "return=minimal")
                            .build(),
                    HttpResponse.BodyHandlers.ofString()
            );

            if (respEmp.statusCode() != 204) {
                System.err.println("PATCH empleado -> " + respEmp.statusCode() + " : " + respEmp.body());
                alerta("Error", "No se pudo actualizar el empleado.\nCódigo " + respEmp.statusCode());
                return;
            }

            // PATCH cuenta: usuario
            if (idCuenta > 0) {
                JSONObject cta = new JSONObject().put("usuario", usuario);
                String urlCta = API_URL + "cuenta?id_cuenta=eq." + idCuenta;
                HttpResponse<String> respCta = http.send(
                        HttpRequest.newBuilder()
                                .uri(URI.create(urlCta))
                                .method("PATCH", HttpRequest.BodyPublishers.ofString(cta.toString()))
                                .header("apikey", API_KEY)
                                .header("Authorization", "Bearer " + API_KEY)
                                .header("Content-Type", "application/json")
                                .header("Prefer", "return=minimal")
                                .build(),
                        HttpResponse.BodyHandlers.ofString()
                );
                if (respCta.statusCode() != 204) {
                    System.err.println("PATCH cuenta -> " + respCta.statusCode() + " : " + respCta.body());
                    alerta("Error", "Empleado actualizado, pero el usuario de la cuenta no se guardó.\nCódigo " + respCta.statusCode());
                    return;
                }
            }

            alerta("Éxito", "Información actualizada correctamente.");
            onCancelarClick(e);

        } catch (Exception ex) {
            ex.printStackTrace();
            alerta("Error", "Fallo al actualizar la información.");
        }
    }

    @FXML
    private void onCancelarClick(ActionEvent e) {
        cambiarVista(backFxml, e);
    }

    // ================== Utils ==================

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
            for (String line; (line = br.readLine()) != null; ) sb.append(line);
            if (code >= 400) throw new RuntimeException("HTTP " + code + " -> " + sb);
            return sb.toString();
        }
    }

    private String enc(String s) {
        try { return URLEncoder.encode(s, "UTF-8"); }
        catch (Exception e) { return s; }
    }

    private String safe(String s) { return s == null ? "" : s.trim(); }

    private String toHHmm(String t) {
        String v = t.trim();
        if (v.matches("^\\d{2}:\\d{2}:\\d{2}$")) return v.substring(0,5);
        if (v.matches("^\\d{1}:\\d{2}$")) return "0" + v;
        return v;
    }

    private String toHHmmss(String t) {
        String v = t.trim();
        if (v.matches("^\\d{2}:\\d{2}:\\d{2}$")) return v;
        if (v.matches("^\\d{1}:\\d{2}$")) v = "0" + v;
        if (v.matches("^\\d{2}:\\d{2}$")) return v + ":00";
        LocalTime time = LocalTime.parse(v, HHMM); // valida
        return time.format(HHMMSS);
    }

    private void alerta(String titulo, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(titulo);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    private void cambiarVista(String rutaFXML, ActionEvent e) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(rutaFXML));
            Parent root = loader.load();

            // Si volvemos a VerDoctor, pásale el id para recargar
            Object controller = loader.getController();
            if (controller != null && rutaFXML.endsWith("VerDoctor.fxml")) {
                try { controller.getClass().getMethod("setIdEmpleado", int.class).invoke(controller, idEmpleadoOriginal); }
                catch (NoSuchMethodException ignored) {}
            }

            Stage stage = (Stage) ((Node) e.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("GECIME");
            stage.show();
        } catch (Exception ex) {
            ex.printStackTrace();
            alerta("Error", "No se pudo cambiar de vista.");
        }
    }

    // Extra: obtener nombre desde la etiqueta "id - Nombre (ubicación)"
    private String extraerNombreDeEtiqueta(String etiqueta) {
        if (etiqueta == null) return null;
        String s = etiqueta.contains(" - ") ? etiqueta.split(" - ", 2)[1] : etiqueta;
        int p = s.indexOf(" (");
        return (p >= 0 ? s.substring(0, p) : s).trim();
    }
}
