package Formularios.controller;

import Compartido.helper.AutoCompleteComboBoxListener;
import Consultas.clientes.model.cliente;
import Formularios.model.modelNuevoCliente;
import conexion.conexionFTP;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.io.ByteArrayInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

public class controllerNuevoCliente {

    private static final int CP_LONGITUD = 5;

    private static final String SEPOMEX_FILE_NAME = "CPdescarga.txt";

    private static final Object SEPOMEX_LOCK = new Object();
    private static volatile boolean sepomexCargado = false;
    private static volatile String sepomexErrorCarga = null;
    private static Map<String, CpInfo> sepomexCache = new HashMap<>();

    private static final String SEPOMEX_CP_HEADER = "d_codigo";
    private static final String SEPOMEX_COLONIA_HEADER = "d_asenta";
    private static final String SEPOMEX_MUNICIPIO_HEADER = "d_mnpio";
    private static final String SEPOMEX_ESTADO_HEADER = "d_estado";
    private static final String SEPOMEX_CIUDAD_HEADER = "d_ciudad";

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

        CompletableFuture
                .supplyAsync(() -> buscarEnSepomex(cp))
                .thenAccept(info -> {
                    if (solicitudCpId.get() != solicitudActual) return;
                    Platform.runLater(() -> aplicarAutocompletado(info, coloniaPreferida));
                })
                .exceptionally(ex -> {
                    if (solicitudCpId.get() == solicitudActual) {
                        Platform.runLater(() -> {
                            limpiarAutocompletado();
                            mostrarErrorUnaVez("Error consultando SEPOMEX", ex.getMessage());
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
    // ✅ CARGA SEPOMEX (CPdescarga.txt)
    // =========================
    private CpInfo buscarEnSepomex(String cp) {
        try {
            cargarSepomexSiNecesario();
        } catch (IOException e) {
            sepomexErrorCarga = e.getMessage();
        }

        if (sepomexErrorCarga != null) {
            throw new IllegalStateException(sepomexErrorCarga);
        }

        return sepomexCache.get(cp);
    }

    private void cargarSepomexSiNecesario() throws IOException {
        if (sepomexCargado || sepomexErrorCarga != null) return;

        synchronized (SEPOMEX_LOCK) {
            if (sepomexCargado || sepomexErrorCarga != null) return;

            conexionFTP ftp = new conexionFTP();
            byte[] contenido = ftp.getExtraFileBytes(SEPOMEX_FILE_NAME);
            if (contenido == null || contenido.length == 0) {
                sepomexErrorCarga = "No se pudo descargar el archivo SEPOMEX desde el FTP.";
                return;
            }

            Map<String, CpInfoBuilder> acumulado = new HashMap<>();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(new ByteArrayInputStream(contenido), StandardCharsets.ISO_8859_1))) {
                String headerLine = reader.readLine();
                if (headerLine == null || headerLine.isBlank()) {
                    sepomexErrorCarga = "El archivo SEPOMEX no contiene encabezados.";
                    return;
                }

                Map<String, Integer> headers = obtenerHeaders(headerLine);
                int idxCp = obtenerIndice(headers, SEPOMEX_CP_HEADER, "codigo", "cp");
                int idxColonia = obtenerIndice(headers, SEPOMEX_COLONIA_HEADER, "asentamiento", "colonia");
                int idxMunicipio = obtenerIndice(headers, SEPOMEX_MUNICIPIO_HEADER, "municipio");
                int idxEstado = obtenerIndice(headers, SEPOMEX_ESTADO_HEADER, "estado");
                int idxCiudad = obtenerIndice(headers, SEPOMEX_CIUDAD_HEADER, "ciudad");

                if (idxCp == -1) idxCp = 0;
                if (idxColonia == -1) idxColonia = 1;
                if (idxMunicipio == -1) idxMunicipio = 3;
                if (idxEstado == -1) idxEstado = 4;
                if (idxCiudad == -1) idxCiudad = 5;

                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isBlank()) continue;
                    String[] campos = line.split("\\|", -1);

                    String cp = obtenerCampo(campos, idxCp);
                    cp = normalizarCp(cp);
                    if (cp.isBlank() || cp.length() != CP_LONGITUD) continue;

                    String colonia = limpiarTexto(obtenerCampo(campos, idxColonia));
                    String municipio = limpiarTexto(obtenerCampo(campos, idxMunicipio));
                    String estado = limpiarTexto(obtenerCampo(campos, idxEstado));
                    String ciudad = limpiarTexto(obtenerCampo(campos, idxCiudad));

                    CpInfoBuilder builder = acumulado.computeIfAbsent(cp, key -> new CpInfoBuilder());
                    builder.agregarColonia(colonia);
                    builder.setEstadoSiVacio(estado);
                    builder.setLocalidadSiVacio(municipio);
                    builder.setCiudadSiVacio(ciudad);
                }
            } catch (IOException e) {
                sepomexErrorCarga = "No se pudo leer el archivo SEPOMEX.";
                return;
            }

            Map<String, CpInfo> nuevoCache = new HashMap<>();
            for (Map.Entry<String, CpInfoBuilder> entry : acumulado.entrySet()) {
                nuevoCache.put(entry.getKey(), entry.getValue().build());
            }

            sepomexCache = nuevoCache;
            sepomexCargado = true;
        }
    }

    private Map<String, Integer> obtenerHeaders(String headerLine) {
        Map<String, Integer> headers = new HashMap<>();
        String[] parts = headerLine.split("\\|", -1);
        for (int i = 0; i < parts.length; i++) {
            String header = parts[i];
            if (header == null) continue;
            String normalizado = header.trim().toLowerCase(Locale.ROOT);
            if (!normalizado.isBlank()) headers.put(normalizado, i);
        }
        return headers;
    }

    private String obtenerCampo(String[] campos, int indice) {
        if (indice < 0 || indice >= campos.length) return "";
        return campos[indice];
    }

    private int obtenerIndice(Map<String, Integer> headers, String... keys) {
        for (String key : keys) {
            if (key == null) continue;
            Integer idx = headers.get(key.toLowerCase(Locale.ROOT));
            if (idx != null) return idx;
        }
        return -1;
    }

    private String normalizarCp(String valor) {
        if (valor == null) return "";
        String limpio = valor.trim();
        if (limpio.isEmpty()) return "";
        if (limpio.matches("\\d+")) {
            if (limpio.length() < CP_LONGITUD) {
                return String.format("%0" + CP_LONGITUD + "d", Long.parseLong(limpio));
            }
        }
        return limpio;
    }

    private String limpiarTexto(String valor) {
        if (valor == null) return "";
        return valor.trim();
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

    private static class CpInfoBuilder {
        private String estado;
        private String localidad;
        private String ciudad;
        private final Set<String> colonias = new LinkedHashSet<>();

        private void agregarColonia(String colonia) {
            if (colonia != null && !colonia.isBlank()) {
                colonias.add(colonia.trim());
            }
        }

        private void setEstadoSiVacio(String valor) {
            if ((estado == null || estado.isBlank()) && valor != null && !valor.isBlank()) {
                estado = valor.trim();
            }
        }

        private void setLocalidadSiVacio(String valor) {
            if ((localidad == null || localidad.isBlank()) && valor != null && !valor.isBlank()) {
                localidad = valor.trim();
            }
        }

        private void setCiudadSiVacio(String valor) {
            if ((ciudad == null || ciudad.isBlank()) && valor != null && !valor.isBlank()) {
                ciudad = valor.trim();
            }
        }

        private CpInfo build() {
            return new CpInfo(
                    "México",
                    estado,
                    localidad,
                    ciudad,
                    new ArrayList<>(colonias)
            );
        }
    }
}
