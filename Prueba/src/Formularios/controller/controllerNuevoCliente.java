package Formularios.controller;

import Compartido.helper.AutoCompleteComboBoxListener;
import Consultas.clientes.model.cliente;
import Formularios.model.modelNuevoCliente;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class controllerNuevoCliente {

    private static final int CP_LONGITUD = 5;

    // ✅ TU TOKEN REAL DE COPOMEX:
    private static final String COPOMEX_TOKEN = "6b65ff9f-78ee-471e-b687-37ca49c48d43";

    // ✅ Endpoint COPOMEX
    private static final String CP_API_URL = "https://api.copomex.com/query/info_cp/%s?token=%s";

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .build();

    // Para detectar errores de COPOMEX (si los incluye)
    private static final Pattern P_ERROR_BOOL = Pattern.compile("\"error\"\\s*:\\s*(true|false)");
    private static final Pattern P_CODIGO_ERROR = Pattern.compile("\"codigo_error\"\\s*:\\s*\"(.*?)\"");
    private static final Pattern P_MENSAJE = Pattern.compile("\"mensaje\"\\s*:\\s*\"(.*?)\"");

    // Campos dentro de response
    private static final Pattern P_CAMPO = Pattern.compile("\"%s\"\\s*:\\s*\"(.*?)\"");
    private static final Pattern P_ASENTAMIENTO = Pattern.compile("\"asentamiento\"\\s*:\\s*\"(.*?)\"");
    private static final Pattern P_COLONIA = Pattern.compile("\"colonia\"\\s*:\\s*\"(.*?)\"");
    private static final Pattern P_COLONIAS_ARRAY = Pattern.compile("\"colonias\"\\s*:\\s*\\[(.*?)]", Pattern.DOTALL);

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

    private final AtomicLong solicitudCpId = new AtomicLong(0);
    private String coloniaPendiente = null;
    private String ultimoCpBuscado = null;

    private final modelNuevoCliente model = new modelNuevoCliente();
    private Runnable onSaved = null;

    // Para no spamear alertas
    private boolean avisoErrorMostrado = false;
    private boolean avisoInfoMostrado = false;

    @FXML
    private void initialize() {
        btnGuardar.setOnAction(e -> guardarCliente());
        configurarCamposAutocompletado();
        configurarAutocompletadoCp();

        setEnterAction(txtNombre);
        setEnterAction(txtRFC);
        setEnterAction(txtCURP);
        setEnterAction(txtRazonSocial);
        setEnterAction(txtCP);
        setEnterAction(txtDomicilio);
        setEnterAction(txtNoExt);
        setEnterAction(txtNoInt);
        setEnterAction(txtCorreoElectronico);
        setEnterAction(txtTelefono);
        if (cmbColonia != null && cmbColonia.getEditor() != null) {
            setEnterAction(cmbColonia.getEditor());
        }
    }

    private void setEnterAction(TextField field) {
        if (field != null) field.setOnAction(e -> guardarCliente());
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
        coloniaPendiente = c.getColonia();
        if (cmbColonia != null) cmbColonia.setValue(c.getColonia());
        txtDomicilio.setText(c.getDomicilio());
        txtNoExt.setText(String.valueOf(c.getNumeroExt()));
        txtNoInt.setText(String.valueOf(c.getNumeroInt()));
        txtCorreoElectronico.setText(c.getCorreo());
        txtTelefono.setText(String.valueOf(c.getTelefono()));

        buscarCpSiValido(txtCP.getText());

        btnGuardar.setText("Actualizar");
        titulo.setText("Actualizar cliente");
    }

    @FXML
    public void prepararNuevoCliente() {
        modoEdicion = false;
        btnGuardar.setText("Guardar");
        titulo.setText("Agregar cliente");
    }

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
            c.setColonia(obtenerColoniaSeleccionada());
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

                if (onSaved != null) onSaved.run();

                Platform.runLater(() -> {
                    Stage stage = (Stage) btnGuardar.getScene().getWindow();
                    stage.close();
                });

            } else {
                new Alert(Alert.AlertType.ERROR, "No se pudo guardar el cliente.").showAndWait();
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
        if (campo == null) return false;
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

    private void configurarCamposAutocompletado() {
        bloquearCampo(txtPais);
        bloquearCampo(txtEstado);
        bloquearCampo(txtLocalidad);
        bloquearCampo(txtCiudad);

        if (cmbColonia != null) {
            cmbColonia.setEditable(true);
            new AutoCompleteComboBoxListener<>(cmbColonia);
        }
    }

    private void bloquearCampo(TextField campo) {
        if (campo == null) return;
        campo.setEditable(false);
        campo.setFocusTraversable(false);
    }

    private void configurarAutocompletadoCp() {
        if (txtCP == null) return;
        txtCP.textProperty().addListener((obs, oldVal, newVal) -> buscarCpSiValido(newVal));
    }

    private void buscarCpSiValido(String cpTexto) {
        if (cpTexto == null) {
            limpiarAutocompletado();
            return;
        }

        String cp = cpTexto.trim();

        // Reset avisos cuando cambia CP
        if (ultimoCpBuscado == null || !ultimoCpBuscado.equals(cp)) {
            avisoErrorMostrado = false;
            avisoInfoMostrado = false;
        }

        if (!cp.matches("\\d{" + CP_LONGITUD + "}")) {
            limpiarAutocompletado();
            return;
        }
        if (cp.equals(ultimoCpBuscado)) return;

        ultimoCpBuscado = cp;
        buscarDatosPorCp(cp, coloniaPendiente);
    }

    private void buscarDatosPorCp(String cp, String coloniaPreferida) {
        long solicitudActual = solicitudCpId.incrementAndGet();

        String url = String.format(CP_API_URL, cp, COPOMEX_TOKEN);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(12))
                .header("Accept", "application/json")
                .GET()
                .build();

        HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                .thenAccept(resp -> {
                    if (solicitudCpId.get() != solicitudActual) return;

                    int status = resp.statusCode();
                    String body = resp.body();

                    if (status != 200) {
                        Platform.runLater(() -> {
                            limpiarAutocompletado();
                            mostrarErrorUnaVez("Error consultando CP (HTTP " + status + ")", extraerDetalleError(body));
                        });
                        return;
                    }

                    // Si COPOMEX incluye error=true
                    Boolean error = leerBoolean(body, P_ERROR_BOOL);
                    if (error != null && error) {
                        Platform.runLater(() -> {
                            limpiarAutocompletado();
                            mostrarErrorUnaVez("COPOMEX devolvió un error", extraerDetalleError(body));
                        });
                        return;
                    }

                    CpInfo info = parsearRespuestaCopomex(body);
                    Platform.runLater(() -> aplicarAutocompletado(info, coloniaPreferida));
                })
                .exceptionally(ex -> {
                    if (solicitudCpId.get() == solicitudActual) {
                        Platform.runLater(() -> {
                            limpiarAutocompletado();
                            mostrarErrorUnaVez("Error de red al consultar CP", ex.getMessage());
                        });
                    }
                    return null;
                });
    }

    private void mostrarErrorUnaVez(String titulo, String detalle) {
        if (avisoErrorMostrado) return;
        avisoErrorMostrado = true;

        String msg = (detalle == null || detalle.isBlank()) ? "Sin detalle." : detalle;
        new Alert(Alert.AlertType.ERROR, titulo + "\n\n" + msg).showAndWait();
    }

    private void mostrarInfoUnaVez(String titulo, String detalle) {
        if (avisoInfoMostrado) return;
        avisoInfoMostrado = true;

        String msg = (detalle == null || detalle.isBlank()) ? "" : ("\n\n" + detalle);
        new Alert(Alert.AlertType.INFORMATION, titulo + msg).showAndWait();
    }

    private String extraerDetalleError(String body) {
        if (body == null) return "";
        String codigo = limpiarTextoJson(leerPrimerGrupo(body, P_CODIGO_ERROR));
        String mensaje = limpiarTextoJson(leerPrimerGrupo(body, P_MENSAJE));

        if ((codigo == null || codigo.isBlank()) && (mensaje == null || mensaje.isBlank())) {
            return body.length() > 300 ? body.substring(0, 300) + "..." : body;
        }

        StringBuilder sb = new StringBuilder();
        if (codigo != null && !codigo.isBlank()) sb.append("Código: ").append(codigo).append("\n");
        if (mensaje != null && !mensaje.isBlank()) sb.append("Mensaje: ").append(mensaje);
        return sb.toString().trim();
    }

    private void aplicarAutocompletado(CpInfo info, String coloniaPreferida) {
        if (info == null) {
            limpiarAutocompletado();
            mostrarInfoUnaVez("CP no encontrado", "No se encontraron datos para ese código postal.");
            return;
        }

        if (info.pais != null) txtPais.setText(info.pais);
        if (info.estado != null) txtEstado.setText(info.estado);
        if (info.localidad != null) txtLocalidad.setText(info.localidad);
        if (info.ciudad != null) txtCiudad.setText(info.ciudad);

        if (cmbColonia != null) {
            cmbColonia.setItems(FXCollections.observableArrayList(info.colonias));

            String seleccion = coloniaPreferida;
            if (seleccion == null || seleccion.isBlank()) {
                seleccion = info.colonias.isEmpty() ? null : info.colonias.get(0);
            }

            if (seleccion != null && !seleccion.isBlank()) {
                if (!info.colonias.contains(seleccion)) {
                    cmbColonia.getItems().add(seleccion);
                }
                cmbColonia.setValue(seleccion);
                if (cmbColonia.getEditor() != null) {
                    cmbColonia.getEditor().setText(seleccion);
                }
            } else {
                cmbColonia.setValue(null);
                if (cmbColonia.getEditor() != null) cmbColonia.getEditor().clear();
            }
        }

        coloniaPendiente = null;
    }

    private void limpiarAutocompletado() {
        if (txtPais != null) txtPais.clear();
        if (txtEstado != null) txtEstado.clear();
        if (txtLocalidad != null) txtLocalidad.clear();
        if (txtCiudad != null) txtCiudad.clear();

        if (cmbColonia != null) {
            cmbColonia.setItems(FXCollections.observableArrayList());
            cmbColonia.setValue(null);
            if (cmbColonia.getEditor() != null) cmbColonia.getEditor().clear();
        }
    }

    private String obtenerColoniaSeleccionada() {
        if (cmbColonia == null) return "";
        String valor = cmbColonia.getValue();
        if ((valor == null || valor.isBlank()) && cmbColonia.getEditor() != null) {
            valor = cmbColonia.getEditor().getText();
        }
        return valor != null ? valor.trim() : "";
    }

    // =========================
    // ✅ PARSEO COPOMEX (ROBUSTO)
    // =========================
    private CpInfo parsearRespuestaCopomex(String body) {
        if (body == null || body.isBlank()) return null;

        // Extrae TODOS los bloques "response" (a veces viene array de objetos, etc.)
        List<String> responses = extraerTodosLosBloques(body, "response");
        if (responses.isEmpty()) {
            // Algunos formatos podrían ser "response": {...} pero igual cae aquí si existe
            return null;
        }

        Set<String> colonias = new LinkedHashSet<>();

        String pais = null;
        String estado = null;
        String ciudad = null;
        String localidad = null;

        for (String resp : responses) {
            if (resp == null || resp.isBlank()) continue;

            // colonias/asentamientos
            agregarCoincidencias(colonias, resp, P_ASENTAMIENTO);
            agregarCoincidencias(colonias, resp, P_COLONIA);
            agregarColoniasDeArreglo(colonias, resp);

            if (pais == null) pais = obtenerPrimerCampo(resp, "pais");
            if (estado == null) estado = obtenerPrimerCampo(resp, "estado");
            if (ciudad == null) ciudad = obtenerPrimerCampo(resp, "ciudad");

            String loc = obtenerPrimerCampo(resp, "localidad");
            String mun = obtenerPrimerCampo(resp, "municipio");

            if (localidad == null || localidad.isBlank()) {
                if (loc != null && !loc.isBlank()) localidad = loc;
                else if (mun != null && !mun.isBlank()) localidad = mun;
            }
        }

        if ((estado == null || estado.isBlank())
                && (ciudad == null || ciudad.isBlank())
                && (pais == null || pais.isBlank())
                && (localidad == null || localidad.isBlank())
                && colonias.isEmpty()) {
            return null;
        }

        return new CpInfo(
                pais != null ? pais : "México",
                estado,
                localidad,
                ciudad,
                new ArrayList<>(colonias)
        );
    }

    private void agregarCoincidencias(Set<String> destino, String body, Pattern pattern) {
        Matcher matcher = pattern.matcher(body);
        while (matcher.find()) {
            String valor = limpiarTextoJson(matcher.group(1));
            if (valor != null && !valor.isBlank()) destino.add(valor.trim());
        }
    }

    private void agregarColoniasDeArreglo(Set<String> destino, String body) {
        Matcher matcher = P_COLONIAS_ARRAY.matcher(body);
        if (!matcher.find()) return;

        String contenido = matcher.group(1);
        String[] partes = contenido.split(",");

        for (String parte : partes) {
            String valor = parte.trim();
            if (valor.startsWith("\"")) valor = valor.substring(1);
            if (valor.endsWith("\"")) valor = valor.substring(0, valor.length() - 1);

            valor = limpiarTextoJson(valor);
            if (valor != null && !valor.isBlank()) destino.add(valor.trim());
        }
    }

    private String obtenerPrimerCampo(String body, String campo) {
        Pattern pattern = Pattern.compile(String.format(P_CAMPO.pattern(), Pattern.quote(campo)));
        Matcher matcher = pattern.matcher(body);
        if (matcher.find()) return limpiarTextoJson(matcher.group(1));
        return null;
    }

    private Boolean leerBoolean(String body, Pattern p) {
        Matcher m = p.matcher(body);
        if (!m.find()) return null;
        return "true".equalsIgnoreCase(m.group(1));
    }

    private String leerPrimerGrupo(String body, Pattern p) {
        Matcher m = p.matcher(body);
        if (m.find()) return m.group(1);
        return null;
    }

    /**
     * Extrae TODOS los bloques JSON asociados a una clave (ej: "response").
     * Funciona aunque el JSON raíz sea un array [...] y aunque response sea {} o [].
     */
    private List<String> extraerTodosLosBloques(String json, String key) {
        List<String> out = new ArrayList<>();
        if (json == null || key == null) return out;

        String needle = "\"" + key + "\"";
        int from = 0;

        while (true) {
            int idx = json.indexOf(needle, from);
            if (idx < 0) break;

            int colon = json.indexOf(':', idx + needle.length());
            if (colon < 0) break;

            int i = colon + 1;
            while (i < json.length() && Character.isWhitespace(json.charAt(i))) i++;
            if (i >= json.length()) break;

            char start = json.charAt(i);
            if (start != '{' && start != '[') {
                from = i + 1;
                continue;
            }

            String bloque = extraerBloqueBalanceado(json, i, (start == '{') ? '}' : ']');
            if (bloque != null) out.add(bloque);

            from = i + 1;
        }

        return out;
    }

    private String extraerBloqueBalanceado(String s, int startIdx, char closeChar) {
        char openChar = (closeChar == '}') ? '{' : '[';

        StringBuilder sb = new StringBuilder();
        int depth = 0;
        boolean inString = false;
        boolean escape = false;

        for (int p = startIdx; p < s.length(); p++) {
            char ch = s.charAt(p);
            sb.append(ch);

            if (escape) { escape = false; continue; }

            if (ch == '\\') {
                if (inString) escape = true;
                continue;
            }

            if (ch == '"') {
                inString = !inString;
                continue;
            }

            if (inString) continue;

            if (ch == openChar) depth++;
            else if (ch == closeChar) {
                depth--;
                if (depth == 0) return sb.toString();
            }
        }

        return null;
    }

    private String limpiarTextoJson(String valor) {
        if (valor == null) return null;

        StringBuilder resultado = new StringBuilder();
        for (int i = 0; i < valor.length(); i++) {
            char ch = valor.charAt(i);

            if (ch == '\\' && i + 1 < valor.length()) {
                char siguiente = valor.charAt(i + 1);
                switch (siguiente) {
                    case '"': resultado.append('"'); i++; break;
                    case '\\': resultado.append('\\'); i++; break;
                    case '/': resultado.append('/'); i++; break;
                    case 'b': resultado.append('\b'); i++; break;
                    case 'f': resultado.append('\f'); i++; break;
                    case 'n': resultado.append('\n'); i++; break;
                    case 'r': resultado.append('\r'); i++; break;
                    case 't': resultado.append('\t'); i++; break;
                    case 'u':
                        if (i + 5 < valor.length()) {
                            String hex = valor.substring(i + 2, i + 6);
                            try {
                                resultado.append((char) Integer.parseInt(hex, 16));
                                i += 5;
                            } catch (NumberFormatException e) {
                                resultado.append("\\u").append(hex);
                                i += 5;
                            }
                        }
                        break;
                    default:
                        resultado.append(ch);
                        break;
                }
            } else {
                resultado.append(ch);
            }
        }
        return resultado.toString();
    }

    private static class CpInfo {
        private final String pais;
        private final String estado;
        private final String localidad;
        private final String ciudad;
        private final List<String> colonias;

        private CpInfo(String pais, String estado, String localidad, String ciudad, List<String> colonias) {
            this.pais = pais;
            this.estado = estado;
            this.localidad = localidad;
            this.ciudad = ciudad;
            this.colonias = colonias;
        }
    }
}
