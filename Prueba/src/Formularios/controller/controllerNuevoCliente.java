package Formularios.controller;

import Formularios.model.modelNuevoCliente;
import Consultas.clientes.model.cliente;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class controllerNuevoCliente {

    @FXML private Label titulo;
    @FXML private TextField txtNombre;
    @FXML private TextField txtRFC;
    @FXML private TextField txtCURP;
    @FXML private TextField txtRazonSocial;
    @FXML private TextField txtCP;
    @FXML private TextField txtPais;
    @FXML private TextField txtEstado;
    @FXML private TextField txtLocalidad;
    @FXML private TextField txtCiudad;
    @FXML private ComboBox<String> cmbColonia;
    @FXML private TextField txtDomicilio;
    @FXML private TextField txtNoExt;
    @FXML private TextField txtNoInt;
    @FXML private TextField txtCorreoElectronico;
    @FXML private TextField txtTelefono;
    @FXML private Button btnGuardar;

    private boolean modoEdicion = false;
    private int idClienteEdicion = -1;

    private final modelNuevoCliente model = new modelNuevoCliente();
    private Runnable onSaved = null;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private String ultimoCpConsultado = "";

    @FXML
    private void initialize() {
        btnGuardar.setOnAction(e -> guardarCliente());

        // 👉 Al presionar ENTER en cualquier campo, se guarda automáticamente
        setEnterAction(txtNombre);
        setEnterAction(txtRFC);
        setEnterAction(txtCURP);
        setEnterAction(txtRazonSocial);
        setEnterAction(txtCP);
        setEnterAction(txtPais);
        setEnterAction(txtEstado);
        setEnterAction(txtLocalidad);
        setEnterAction(txtCiudad);
        setEnterAction(txtDomicilio);
        setEnterAction(txtNoExt);
        setEnterAction(txtNoInt);
        setEnterAction(txtCorreoElectronico);
        setEnterAction(txtTelefono);

        configurarAutocompletadoCP();
    }

    private void setEnterAction(TextField field) {
        field.setOnAction(e -> guardarCliente());
    }

    public void setOnSaved(Runnable r) { this.onSaved = r; }

    // 👉 Ejecutar cuando es edición
    public void cargarCliente(cliente c) {
        if (c == null) return;

        modoEdicion = true;
        idClienteEdicion = c.getId();

        txtNombre.setText(c.getNombre());
        txtRFC.setText(c.getRfc());
        txtCURP.setText(c.getCurp());
        txtRazonSocial.setText(c.getRazonSocial());
        txtCP.setText(String.valueOf(c.getCp()));
        txtPais.setText(c.getPais());
        txtEstado.setText(c.getEstado());
        txtLocalidad.setText(c.getLocalidad());
        txtCiudad.setText(c.getCiudad());
        cmbColonia.getItems().setAll(c.getColonia());
        cmbColonia.getSelectionModel().select(c.getColonia());
        txtDomicilio.setText(c.getDomicilio());
        txtNoExt.setText(String.valueOf(c.getNumeroExt()));
        txtNoInt.setText(String.valueOf(c.getNumeroInt()));
        txtCorreoElectronico.setText(c.getCorreo());
        txtTelefono.setText(String.valueOf(c.getTelefono()));

        btnGuardar.setText("Actualizar");
        titulo.setText("Actualizar cliente");
    }

    // 👉 Ejecutar al abrir formulario "nuevo"
    @FXML
    public void prepararNuevoCliente() {
        modoEdicion = false;
        btnGuardar.setText("Guardar");
        titulo.setText("Agregar cliente");
    }

    // En controllerNuevoCliente.java, modifica el método guardarCliente():
    @FXML
    public void guardarCliente() {

        if (!validarNumericos()) return;

        try {
            cliente c = new cliente();
            if (modoEdicion) c.setId(idClienteEdicion);

            c.setNombre(txtNombre.getText());
            c.setRfc(txtRFC.getText());
            c.setCurp(txtCURP.getText());
            c.setRazonSocial(txtRazonSocial.getText());
            c.setCp(Integer.parseInt(txtCP.getText()));
            c.setPais(txtPais.getText());
            c.setEstado(txtEstado.getText());
            c.setLocalidad(txtLocalidad.getText());
            c.setCiudad(txtCiudad.getText());
            c.setColonia(cmbColonia.getValue());
            c.setDomicilio(txtDomicilio.getText());
            c.setNumeroExt(Integer.parseInt(txtNoExt.getText()));
            c.setNumeroInt(Integer.parseInt(txtNoInt.getText()));
            c.setCorreo(txtCorreoElectronico.getText());
            c.setTelefono(Integer.parseInt(txtTelefono.getText()));

            boolean exito = modoEdicion ?
                    model.modificarCliente(c) :
                    model.agregarCliente(c);

            if (exito) {
                new Alert(Alert.AlertType.INFORMATION,
                        modoEdicion ? "Cliente actualizado correctamente" :
                                "Cliente agregado correctamente"
                ).showAndWait();

                // 👉 IMPORTANTE: Ejecutar callback ANTES de cerrar
                if (onSaved != null) {
                    onSaved.run();
                }

                // 👉 Cerrar después de un breve retardo (igual que proveedores)
                javafx.application.Platform.runLater(() -> {
                    Stage stage = (Stage) btnGuardar.getScene().getWindow();
                    stage.close();
                });

            } else {
                new Alert(Alert.AlertType.ERROR,
                        "No se pudo guardar el cliente."
                ).showAndWait();
            }

        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR,
                    "Ocurrió un error inesperado al guardar el cliente."
            ).showAndWait();
        }
    }

    private boolean validarNumericos() {
        return validarCampoNumerico(txtCP, "Código Postal") &&
                validarCampoNumerico(txtNoExt, "Número Exterior") &&
                validarCampoNumerico(txtNoInt, "Número Interior") &&
                validarCampoNumerico(txtTelefono, "Teléfono");
    }

    private boolean validarCampoNumerico(TextField campo, String nombreCampo) {
        if (!campo.getText().matches("\\d+")) {
            new Alert(Alert.AlertType.ERROR,
                    "El campo '" + nombreCampo + "' debe contener solo números."
            ).showAndWait();
            campo.requestFocus();
            campo.selectAll();
            return false;
        }
        return true;
    }

    // Añade este método a la clase controllerNuevoCliente (después de setOnSaved)
    public String getNombreCliente() {
        return txtNombre.getText().trim();
    }

    private void configurarAutocompletadoCP() {
        txtPais.setEditable(false);
        txtEstado.setEditable(false);
        txtLocalidad.setEditable(false);
        txtCiudad.setEditable(false);
        cmbColonia.setDisable(true);

        txtCP.textProperty().addListener((obs, oldVal, newVal) -> {
            String cp = newVal == null ? "" : newVal.trim();
            if (!cp.matches("\\d{5}")) {
                limpiarDatosAutocompletados();
                return;
            }
            if (cp.equals(ultimoCpConsultado)) {
                return;
            }
            ultimoCpConsultado = cp;
            consultarDatosPorCp(cp);
        });
    }

    private void limpiarDatosAutocompletados() {
        ultimoCpConsultado = "";
        txtPais.clear();
        txtEstado.clear();
        txtLocalidad.clear();
        txtCiudad.clear();
        cmbColonia.getItems().clear();
        cmbColonia.getSelectionModel().clearSelection();
        cmbColonia.setDisable(true);
    }

    private void consultarDatosPorCp(String cp) {
        Task<ResultadoCp> task = new Task<>() {
            @Override
            protected ResultadoCp call() throws Exception {
                String infoUrl = "https://api.copomex.com/query/info_cp/" + cp + "?token=pruebas";
                String coloniasUrl = "https://api.copomex.com/query/get_colonia_por_cp/" + cp + "?token=pruebas";

                String infoBody = enviarSolicitud(infoUrl);
                String coloniasBody = enviarSolicitud(coloniasUrl);

                if (respuestaConError(infoBody)) {
                    return new ResultadoCp();
                }

                ResultadoCp resultado = new ResultadoCp();
                resultado.pais = extraerCampoEnRespuesta(infoBody, "pais");
                resultado.estado = extraerCampoEnRespuesta(infoBody, "estado");
                resultado.ciudad = extraerCampoEnRespuesta(infoBody, "ciudad");
                resultado.localidad = extraerCampoEnRespuesta(infoBody, "localidad");
                if (resultado.localidad == null || resultado.localidad.isBlank()) {
                    resultado.localidad = extraerCampoEnRespuesta(infoBody, "municipio");
                }
                resultado.colonias = extraerListaEnRespuesta(coloniasBody, "colonia");
                if (resultado.colonias.isEmpty()) {
                    String coloniaDirecta = extraerCampoEnRespuesta(infoBody, "colonia");
                    if (coloniaDirecta != null && !coloniaDirecta.isBlank()) {
                        resultado.colonias.add(coloniaDirecta);
                    }
                }
                resultado.cp = cp;
                return resultado;
            }
        };

        task.setOnSucceeded(e -> {
            ResultadoCp resultado = task.getValue();
            if (resultado == null || !cp.equals(txtCP.getText().trim())) {
                return;
            }
            aplicarResultadoCp(resultado);
        });

        task.setOnFailed(e -> Platform.runLater(this::limpiarDatosAutocompletados));

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private String enviarSolicitud(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString()).body();
    }

    private void aplicarResultadoCp(ResultadoCp resultado) {
        txtPais.setText(valorSeguro(resultado.pais));
        txtEstado.setText(valorSeguro(resultado.estado));
        txtLocalidad.setText(valorSeguro(resultado.localidad));
        txtCiudad.setText(valorSeguro(resultado.ciudad));

        Set<String> coloniasUnicas = new LinkedHashSet<>(resultado.colonias);
        cmbColonia.getItems().setAll(coloniasUnicas);
        if (!cmbColonia.getItems().isEmpty()) {
            cmbColonia.getSelectionModel().select(0);
            cmbColonia.setDisable(false);
        } else {
            cmbColonia.setDisable(true);
        }
    }

    private String valorSeguro(String valor) {
        return valor == null ? "" : valor;
    }

    private String extraerCampoEnRespuesta(String json, String campo) {
        if (json == null || json.isBlank()) {
            return "";
        }
        String respuesta = extraerBloqueRespuesta(json);
        String contenido = respuesta == null || respuesta.isBlank() ? json : respuesta;
        Pattern pattern = Pattern.compile("\"" + Pattern.quote(campo) + "\"\\s*:\\s*\"(.*?)\"", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(contenido);
        if (matcher.find()) {
            return desescapeJson(matcher.group(1));
        }
        return "";
    }

    private List<String> extraerListaEnRespuesta(String json, String campo) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        String respuesta = extraerBloqueRespuesta(json);
        String contenido = respuesta == null || respuesta.isBlank() ? json : respuesta;
        Pattern pattern = Pattern.compile("\"" + Pattern.quote(campo) + "\"\\s*:\\s*\\[(.*?)\\]", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher matcher = pattern.matcher(contenido);
        if (!matcher.find()) {
            return extraerListaDesdeObjetos(contenido, campo);
        }
        String listaContenido = matcher.group(1);
        Pattern itemPattern = Pattern.compile("\"(.*?)\"");
        Matcher itemMatcher = itemPattern.matcher(listaContenido);
        List<String> items = new ArrayList<>();
        while (itemMatcher.find()) {
            items.add(desescapeJson(itemMatcher.group(1)));
        }
        return items;
    }

    private String extraerBloqueRespuesta(String json) {
        int idx = json.indexOf("\"response\"");
        if (idx == -1) {
            return "";
        }
        int start = json.indexOf('{', idx);
        if (start == -1) {
            start = json.indexOf('[', idx);
        }
        if (start == -1) {
            return "";
        }
        return extraerEstructura(json, start);
    }

    private List<String> extraerListaDesdeObjetos(String json, String campo) {
        Pattern pattern = Pattern.compile("\"" + Pattern.quote(campo) + "\"\\s*:\\s*\"(.*?)\"", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(json);
        List<String> items = new ArrayList<>();
        while (matcher.find()) {
            items.add(desescapeJson(matcher.group(1)));
        }
        return items;
    }

    private String desescapeJson(String valor) {
        if (valor == null || valor.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < valor.length(); i++) {
            char c = valor.charAt(i);
            if (c == '\\' && i + 1 < valor.length()) {
                char next = valor.charAt(i + 1);
                if (next == 'u' && i + 5 < valor.length()) {
                    String hex = valor.substring(i + 2, i + 6);
                    try {
                        sb.append((char) Integer.parseInt(hex, 16));
                        i += 5;
                        continue;
                    } catch (NumberFormatException ignored) {
                    }
                } else {
                    switch (next) {
                        case '"':
                            sb.append('"');
                            i++;
                            continue;
                        case '\\':
                            sb.append('\\');
                            i++;
                            continue;
                        case '/':
                            sb.append('/');
                            i++;
                            continue;
                        case 'b':
                            sb.append('\b');
                            i++;
                            continue;
                        case 'f':
                            sb.append('\f');
                            i++;
                            continue;
                        case 'n':
                            sb.append('\n');
                            i++;
                            continue;
                        case 'r':
                            sb.append('\r');
                            i++;
                            continue;
                        case 't':
                            sb.append('\t');
                            i++;
                            continue;
                        default:
                            break;
                    }
                }
            }
            sb.append(c);
        }
        return sb.toString();
    }

    private boolean respuestaConError(String json) {
        if (json == null || json.isBlank()) {
            return true;
        }
        Pattern pattern = Pattern.compile("\"error\"\\s*:\\s*(true|false|\"true\"|\"false\")", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            return "true".equalsIgnoreCase(matcher.group(1).replace("\"", ""));
        }
        return false;
    }

    private String extraerEstructura(String json, int start) {
        char open = json.charAt(start);
        char close = open == '{' ? '}' : ']';
        int depth = 0;
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == open) {
                depth++;
            } else if (c == close) {
                depth--;
                if (depth == 0) {
                    return json.substring(start, i + 1);
                }
            }
        }
        return "";
    }

    private static class ResultadoCp {
        private String cp;
        private String pais;
        private String estado;
        private String localidad;
        private String ciudad;
        private List<String> colonias = new ArrayList<>();
    }
}
