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
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class controllerNuevoCliente {

    private static final int CP_LONGITUD = 5;
    private static final String CP_API_URL = "https://api.copomex.com/query/info_cp/%s?token=pruebas";
    private static final Pattern PATRON_CAMPO = Pattern.compile("\"%s\"\\s*:\\s*\"(.*?)\"");
    private static final Pattern PATRON_ASENTAMIENTO = Pattern.compile("\"asentamiento\"\\s*:\\s*\"(.*?)\"");
    private static final Pattern PATRON_COLONIA = Pattern.compile("\"colonia\"\\s*:\\s*\"(.*?)\"");
    private static final Pattern PATRON_COLONIAS_ARRAY = Pattern.compile("\"colonias\"\\s*:\\s*\\[(.*?)]");

    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

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

    @FXML
    private void initialize() {
        btnGuardar.setOnAction(e -> guardarCliente());
        configurarCamposAutocompletado();
        configurarAutocompletadoCp();

        // 👉 Al presionar ENTER en cualquier campo, se guarda automáticamente
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
        coloniaPendiente = c.getColonia();
        cmbColonia.setValue(c.getColonia());
        txtDomicilio.setText(c.getDomicilio());
        txtNoExt.setText(String.valueOf(c.getNumeroExt()));
        txtNoInt.setText(String.valueOf(c.getNumeroInt()));
        txtCorreoElectronico.setText(c.getCorreo());
        txtTelefono.setText(String.valueOf(c.getTelefono()));

        buscarCpSiValido(txtCP.getText());

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
        if (campo == null) {
            return;
        }
        campo.setEditable(false);
        campo.setFocusTraversable(false);
    }

    private void configurarAutocompletadoCp() {
        if (txtCP == null) {
            return;
        }
        txtCP.textProperty().addListener((obs, oldVal, newVal) -> buscarCpSiValido(newVal));
    }

    private void buscarCpSiValido(String cpTexto) {
        if (cpTexto == null) {
            limpiarAutocompletado();
            return;
        }
        String cp = cpTexto.trim();
        if (!cp.matches("\\d{" + CP_LONGITUD + "}")) {
            limpiarAutocompletado();
            return;
        }
        if (cp.equals(ultimoCpBuscado)) {
            return;
        }
        ultimoCpBuscado = cp;
        buscarDatosPorCp(cp, coloniaPendiente);
    }

    private void buscarDatosPorCp(String cp, String coloniaPreferida) {
        long solicitudActual = solicitudCpId.incrementAndGet();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(String.format(CP_API_URL, cp)))
                .GET()
                .build();

        HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                .thenApply(HttpResponse::body)
                .thenApply(this::parsearRespuestaCp)
                .thenAccept(info -> {
                    if (solicitudCpId.get() != solicitudActual) {
                        return;
                    }
                    Platform.runLater(() -> aplicarAutocompletado(info, coloniaPreferida));
                })
                .exceptionally(ex -> {
                    if (solicitudCpId.get() == solicitudActual) {
                        Platform.runLater(this::limpiarAutocompletado);
                    }
                    return null;
                });
    }

    private void aplicarAutocompletado(CpInfo info, String coloniaPreferida) {
        if (info == null) {
            limpiarAutocompletado();
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
                if (cmbColonia.getEditor() != null) {
                    cmbColonia.getEditor().clear();
                }
            }
        }
        coloniaPendiente = null;
    }

    private void limpiarAutocompletado() {
        txtPais.clear();
        txtEstado.clear();
        txtLocalidad.clear();
        txtCiudad.clear();
        if (cmbColonia != null) {
            cmbColonia.setItems(FXCollections.observableArrayList());
            cmbColonia.setValue(null);
            if (cmbColonia.getEditor() != null) {
                cmbColonia.getEditor().clear();
            }
        }
    }

    private String obtenerColoniaSeleccionada() {
        if (cmbColonia == null) {
            return "";
        }
        String valor = cmbColonia.getValue();
        if ((valor == null || valor.isBlank()) && cmbColonia.getEditor() != null) {
            valor = cmbColonia.getEditor().getText();
        }
        return valor != null ? valor.trim() : "";
    }

    private CpInfo parsearRespuestaCp(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }
        Set<String> colonias = new LinkedHashSet<>();
        agregarCoincidencias(colonias, body, PATRON_ASENTAMIENTO);
        agregarCoincidencias(colonias, body, PATRON_COLONIA);
        agregarColoniasDeArreglo(colonias, body);

        String estado = obtenerPrimerCampo(body, "estado");
        String ciudad = obtenerPrimerCampo(body, "ciudad");
        String pais = obtenerPrimerCampo(body, "pais");
        String localidad = obtenerPrimerCampo(body, "localidad");
        String municipio = obtenerPrimerCampo(body, "municipio");
        if ((localidad == null || localidad.isBlank()) && municipio != null && !municipio.isBlank()) {
            localidad = municipio;
        }

        if ((estado == null || estado.isBlank())
                && (ciudad == null || ciudad.isBlank())
                && (pais == null || pais.isBlank())
                && colonias.isEmpty()) {
            return null;
        }
        return new CpInfo(pais, estado, localidad, ciudad, new ArrayList<>(colonias));
    }

    private void agregarCoincidencias(Set<String> destino, String body, Pattern pattern) {
        Matcher matcher = pattern.matcher(body);
        while (matcher.find()) {
            String valor = limpiarTextoJson(matcher.group(1));
            if (valor != null && !valor.isBlank()) {
                destino.add(valor.trim());
            }
        }
    }

    private void agregarColoniasDeArreglo(Set<String> destino, String body) {
        Matcher matcher = PATRON_COLONIAS_ARRAY.matcher(body);
        if (!matcher.find()) {
            return;
        }
        String contenido = matcher.group(1);
        String[] partes = contenido.split(",");
        for (String parte : partes) {
            String valor = parte.trim();
            if (valor.startsWith("\"")) {
                valor = valor.substring(1);
            }
            if (valor.endsWith("\"")) {
                valor = valor.substring(0, valor.length() - 1);
            }
            valor = limpiarTextoJson(valor);
            if (valor != null && !valor.isBlank()) {
                destino.add(valor.trim());
            }
        }
    }

    private String obtenerPrimerCampo(String body, String campo) {
        Pattern pattern = Pattern.compile(String.format(PATRON_CAMPO.pattern(), Pattern.quote(campo)));
        Matcher matcher = pattern.matcher(body);
        if (matcher.find()) {
            return limpiarTextoJson(matcher.group(1));
        }
        return null;
    }

    private String limpiarTextoJson(String valor) {
        if (valor == null) {
            return null;
        }
        StringBuilder resultado = new StringBuilder();
        for (int i = 0; i < valor.length(); i++) {
            char ch = valor.charAt(i);
            if (ch == '\\' && i + 1 < valor.length()) {
                char siguiente = valor.charAt(i + 1);
                switch (siguiente) {
                    case '"':
                        resultado.append('"');
                        i++;
                        break;
                    case '\\':
                        resultado.append('\\');
                        i++;
                        break;
                    case '/':
                        resultado.append('/');
                        i++;
                        break;
                    case 'b':
                        resultado.append('\b');
                        i++;
                        break;
                    case 'f':
                        resultado.append('\f');
                        i++;
                        break;
                    case 'n':
                        resultado.append('\n');
                        i++;
                        break;
                    case 'r':
                        resultado.append('\r');
                        i++;
                        break;
                    case 't':
                        resultado.append('\t');
                        i++;
                        break;
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
