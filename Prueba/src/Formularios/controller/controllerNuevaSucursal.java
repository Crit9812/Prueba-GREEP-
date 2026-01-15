package Formularios.controller;

import Consultas.sucursales.model.sucursal;
import Formularios.model.modelNuevaSucursal;
import conexion.conexionFTP;
import Compartido.helper.AutoCompleteComboBoxListener;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.stage.Modality;
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
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

public class controllerNuevaSucursal {

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
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    @FXML private Label titulo;
    @FXML private TextField txtNombre;
    @FXML private TextField txtDomicilio;
    @FXML private TextField txtCP;
    @FXML private ComboBox<String> cmbColonia;
    @FXML private TextField txtNumeroExt;
    @FXML private TextField txtNumeroInt;
    @FXML private TextField txtCiudad;
    @FXML private TextField txtEstado;
    @FXML private TextField txtLocalidad;
    @FXML private TextField txtPais;
    @FXML private TextField txtCorreo;
    @FXML private TextField txtTelefono;
    @FXML private Button btnGuardar;

    private boolean modoEdicion = false;
    private int idSucursalEdicion = -1;

    private final modelNuevaSucursal model = new modelNuevaSucursal();
    private Runnable onSaved = null;
    private final AtomicLong solicitudCpId = new AtomicLong(0);
    private String coloniaPendiente = null;
    private String ultimoCpBuscado = null;
    private boolean avisoErrorMostrado = false;
    private boolean avisoInfoMostrado = false;

    @FXML
    private void initialize() {
        btnGuardar.setOnAction(e -> guardarSucursal());
        configurarValidaciones();
        configurarCamposAutocompletado();
        configurarAutocompletadoCp();

        // Activar guardar con ENTER
        setEnterAction(txtNombre);
        setEnterAction(txtDomicilio);
        setEnterAction(txtCP);
        if (cmbColonia != null && cmbColonia.getEditor() != null) {
            setEnterAction(cmbColonia.getEditor());
        }
        setEnterAction(txtNumeroExt);
        setEnterAction(txtNumeroInt);
        setEnterAction(txtCiudad);
        setEnterAction(txtEstado);
        setEnterAction(txtLocalidad);
        setEnterAction(txtPais);
        setEnterAction(txtCorreo);
        setEnterAction(txtTelefono);
    }

    private void setEnterAction(TextField field) {
        if (field != null) field.setOnAction(e -> guardarSucursal());
    }

    public void setOnSaved(Runnable r) { this.onSaved = r; }

    // 👉 Se llama desde la ventana anterior cuando se quiere EDITAR
    public void cargarSucursal(sucursal s) {
        if (s == null) return;

        modoEdicion = true;
        idSucursalEdicion = s.getId();

        txtNombre.setText(s.getNombre());
        txtDomicilio.setText(s.getDomicilio());
        txtCP.setText(String.valueOf(s.getCp()));
        if (cmbColonia != null) cmbColonia.setValue(s.getColonia());
        txtNumeroExt.setText(String.valueOf(s.getNumeroExt()));
        txtNumeroInt.setText(String.valueOf(s.getNumeroInt()));
        txtCiudad.setText(s.getCiudad());
        txtEstado.setText(s.getEstado());
        txtLocalidad.setText(s.getLocalidad());
        txtPais.setText(s.getPais());
        txtCorreo.setText(s.getCorreo());
        txtTelefono.setText(String.valueOf(s.getTelefono()));

        coloniaPendiente = s.getColonia();
        buscarCpSiValido(txtCP.getText());

        btnGuardar.setText("Actualizar");
        titulo.setText("Actualizar sucursal");
    }

    // 👉 Se llama al abrir para indicar que es NUEVO
    @FXML
    public void prepararNuevaSucursal() {
        modoEdicion = false;
        btnGuardar.setText("Guardar");
        titulo.setText("Agregar sucursal");
    }

    @FXML
    public void guardarSucursal() {

        try {
            if (!validarFormulario()) return;

            // Crear / editar objeto
            sucursal s = new sucursal();
            if (modoEdicion) s.setId(idSucursalEdicion);

            s.setNombre(txtNombre.getText().trim());
            s.setDomicilio(txtDomicilio.getText().trim());
            s.setCp(Integer.parseInt(txtCP.getText().trim()));
            s.setColonia(obtenerColoniaSeleccionada());
            s.setNumeroExt(Integer.parseInt(txtNumeroExt.getText().trim()));
            s.setNumeroInt(txtNumeroInt.getText().trim().isEmpty() ? 0 :
                    Integer.parseInt(txtNumeroInt.getText().trim()));
            s.setCiudad(txtCiudad.getText().trim());
            s.setEstado(txtEstado.getText().trim());
            s.setLocalidad(txtLocalidad.getText().trim());
            s.setPais(txtPais.getText().trim());
            s.setCorreo(txtCorreo.getText().trim());
            s.setTelefono(Integer.parseInt(txtTelefono.getText().trim()));

            boolean exito = modoEdicion ?
                    model.modificarSucursal(s) :
                    model.guardarSucursal(s);

            if (exito) {
                mostrarAlerta(Alert.AlertType.INFORMATION,
                        modoEdicion ? "Sucursal actualizada correctamente"
                                : "Sucursal agregada correctamente"
                );

                if (onSaved != null) onSaved.run();
                Stage stage = (Stage) btnGuardar.getScene().getWindow();
                stage.close();

            } else {
                mostrarAlerta(Alert.AlertType.ERROR,
                        "Error al guardar la sucursal en la BD");
            }

        } catch (NumberFormatException e) {
            mostrarAlerta(Alert.AlertType.ERROR,
                    "Revisa CP, N° Interior, N° Exterior y Teléfono: deben ser numéricos");
        }
    }

    private void configurarValidaciones() {
        aplicarFiltro(txtNombre, permitirTexto(100));
        aplicarFiltro(txtDomicilio, permitirAlfanumericoConSimbolos(180));
        aplicarFiltro(txtCP, permitirNumeros(CP_LONGITUD));
        if (cmbColonia != null && cmbColonia.getEditor() != null) {
            aplicarFiltro(cmbColonia.getEditor(), permitirTexto(120));
        }
        aplicarFiltro(txtNumeroExt, permitirNumeros(10));
        aplicarFiltro(txtNumeroInt, permitirNumeros(10));
        aplicarFiltro(txtCorreo, permitirEmail(120));
        aplicarFiltro(txtTelefono, permitirNumeros(10));
    }

    private void aplicarFiltro(TextField field, UnaryOperator<TextFormatter.Change> filter) {
        if (field == null) return;
        field.setTextFormatter(new TextFormatter<>(filter));
    }

    private UnaryOperator<TextFormatter.Change> permitirNumeros(int maxLength) {
        return change -> {
            String nuevo = change.getControlNewText();
            if (!nuevo.matches("\\d*")) return null;
            return nuevo.length() <= maxLength ? change : null;
        };
    }

    private UnaryOperator<TextFormatter.Change> permitirTexto(int maxLength) {
        return change -> {
            String nuevo = change.getControlNewText();
            if (!nuevo.matches("[A-Za-zÁÉÍÓÚÜÑáéíóúüñ\\s'.-]*")) return null;
            return nuevo.length() <= maxLength ? change : null;
        };
    }

    private UnaryOperator<TextFormatter.Change> permitirAlfanumericoConSimbolos(int maxLength) {
        return change -> {
            String nuevo = change.getControlNewText();
            if (!nuevo.matches("[A-Za-z0-9ÁÉÍÓÚÜÑáéíóúüñ\\s#.,'/-]*")) return null;
            return nuevo.length() <= maxLength ? change : null;
        };
    }

    private UnaryOperator<TextFormatter.Change> permitirEmail(int maxLength) {
        return change -> {
            String nuevo = change.getControlNewText();
            if (!nuevo.matches("[A-Za-z0-9._%+\\-@]*")) return null;
            return nuevo.length() <= maxLength ? change : null;
        };
    }

    private boolean validarFormulario() {
        if (!validarCampoObligatorio(txtNombre, "Nombre")) return false;
        if (!validarCampoObligatorio(txtDomicilio, "Domicilio")) return false;
        if (!validarCampoObligatorio(txtCP, "Código Postal")) return false;
        if (!validarCampoObligatorio(txtNumeroExt, "Número Exterior")) return false;
        if (!validarCampoObligatorio(txtCiudad, "Ciudad")) return false;
        if (!validarCampoObligatorio(txtEstado, "Estado")) return false;
        if (!validarCampoObligatorio(txtLocalidad, "Localidad")) return false;
        if (!validarCampoObligatorio(txtPais, "País")) return false;
        if (!validarCampoObligatorio(txtCorreo, "Correo electrónico")) return false;
        if (!validarCampoObligatorio(txtTelefono, "Teléfono")) return false;

        if (!validarCampoNumerico(txtCP, "Código Postal")) return false;
        if (!validarCampoNumerico(txtNumeroExt, "Número Exterior")) return false;
        if (!validarCampoNumericoOpcional(txtNumeroInt, "Número Interior")) return false;
        if (!validarCampoNumerico(txtTelefono, "Teléfono")) return false;

        String cp = txtCP.getText().trim();
        if (cp.length() != CP_LONGITUD) {
            mostrarAlerta(Alert.AlertType.ERROR, "El código postal debe tener 5 dígitos.");
            return false;
        }

        String correo = txtCorreo.getText().trim();
        if (!correo.isBlank() && !EMAIL_PATTERN.matcher(correo).matches()) {
            mostrarAlerta(Alert.AlertType.ERROR, "El correo electrónico no tiene un formato válido.");
            return false;
        }

        String telefono = txtTelefono.getText().trim();
        if (!telefono.isBlank() && telefono.length() != 10) {
            mostrarAlerta(Alert.AlertType.ERROR, "El teléfono debe tener 10 dígitos.");
            return false;
        }

        return true;
    }

    private boolean validarCampoObligatorio(TextField campo, String nombreCampo) {
        if (campo == null) return false;
        if (campo.getText() == null || campo.getText().trim().isEmpty()) {
            mostrarAlerta(Alert.AlertType.ERROR,
                    "El campo '" + nombreCampo + "' es obligatorio.");
            campo.requestFocus();
            return false;
        }
        return true;
    }

    private boolean validarCampoNumerico(TextField campo, String nombreCampo) {
        if (campo == null) return false;
        if (!campo.getText().matches("\\d+")) {
            mostrarAlerta(Alert.AlertType.ERROR,
                    "El campo '" + nombreCampo + "' debe contener solo números.");
            campo.requestFocus();
            campo.selectAll();
            return false;
        }
        return true;
    }

    private boolean validarCampoNumericoOpcional(TextField campo, String nombreCampo) {
        if (campo == null) return false;
        String valor = campo.getText();
        if (valor == null || valor.trim().isEmpty()) return true;
        return validarCampoNumerico(campo, nombreCampo);
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
        mostrarAlerta(Alert.AlertType.ERROR, titulo + "\n\n" + msg);
    }

    private void mostrarInfoUnaVez(String titulo, String detalle) {
        if (avisoInfoMostrado) return;
        avisoInfoMostrado = true;

        String msg = (detalle == null || detalle.isBlank()) ? "" : ("\n\n" + detalle);
        mostrarAlerta(Alert.AlertType.INFORMATION, titulo + msg);
    }

    private void mostrarAlerta(Alert.AlertType type, String mensaje) {
        Alert alert = new Alert(type, mensaje);
        Stage stage = btnGuardar != null && btnGuardar.getScene() != null
                ? (Stage) btnGuardar.getScene().getWindow()
                : null;
        if (stage != null) {
            alert.initOwner(stage);
            alert.initModality(Modality.WINDOW_MODAL);
        }
        alert.showAndWait();
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
