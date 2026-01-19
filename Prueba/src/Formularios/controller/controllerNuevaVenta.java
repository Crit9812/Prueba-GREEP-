package Formularios.controller;

import Compartido.controller.productoCboxController;
import Compartido.helper.AutoCompleteComboBoxListener;
import Compartido.model.DAO.GenericDAO;
import Formularios.model.modelNuevoTraspasoSalida;
import Operaciones.compra.model.UbicacionCompra;
import Operaciones.traspasoSalida.model.traspasoSalida;
import conexion.conexionFTP;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.concurrent.Task;
import javafx.util.Duration;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Objects;

public class controllerNuevaVenta {

    private static final BigDecimal IVA_TASA = new BigDecimal("0.16");

    @FXML private VBox contenedorUbicaciones;
    @FXML private ComboBox<String> comboUbicacion;

    @FXML private ComboBox<String> cbClaveProducto;
    @FXML private ComboBox<String> cbClaveAlterna;
    @FXML private ComboBox<String> cbProductoNombre;
    @FXML private TextField txtDescripcion;
    @FXML private ImageView previewImage;
    @FXML private TextField txtLote;
    @FXML private DatePicker dpCaducidad;
    @FXML private TextField txtCantidad;
    @FXML private ComboBox<String> cbPresentacion;
    @FXML private TextField txtFactor;
    @FXML private TextField txtCantidadUbicacion;
    @FXML private TextField txtNota;
    @FXML private TextField txtPrecioEntrada;
    @FXML private TextField txtPrecioEntradaIva;
    @FXML private TextField txtPrecioSalida;
    @FXML private CheckBox checkBoxIVA;
    @FXML private TextField txtPrecioIVA;
    @FXML private TextField txtPrecioBruto;
    @FXML private TextField txtPrecioTotal;
    @FXML private Button btnGuardar;
    @FXML private Button btnLimpiar;
    @FXML private Label lblTitulo;
    @FXML private TabPane tabPaneModo;
    @FXML private Tab tabNormal;
    @FXML private Tab tabRapido;
    @FXML private ComboBox<String> cbPresentacionRapida;
    @FXML private TextField txtFactorRapido;
    @FXML private TextField txtCantidadRapida;
    @FXML private TextField txtPrecioEntradaRapida;
    @FXML private TextField txtPrecioEntradaIvaRapida;
    @FXML private TextField txtPrecioSalidaRapida;
    @FXML private CheckBox checkBoxIVARapida;
    @FXML private TextField txtPrecioIVARapida;
    @FXML private TextField txtPrecioBrutoRapida;
    @FXML private TextField txtPrecioTotalRapida;

    private int contadorFilas = 1;
    private static final int MAX_FILAS = 10;

    private final ObservableList<String> ubicaciones = FXCollections.observableArrayList();
    private final ObservableList<String> presentaciones = FXCollections.observableArrayList(
            "paquete", "pz", "caja", "bolsa", "pieza", "rollo", "litro", "kilogramo", "metro", "unidad"
    );

    private final modelNuevoTraspasoSalida modelo = new modelNuevoTraspasoSalida();
    private productoCboxController productoController;
    private ObservableList<traspasoSalida> itemsVenta;
    private Operaciones.venta.controller.MainController mainController;
    private traspasoSalida itemParaEditar;

    private BigDecimal precioEntradaBase = BigDecimal.ZERO;
    private BigDecimal precioEntradaIvaBase = BigDecimal.ZERO;
    private boolean loteValidado = false;
    private boolean caducidadValidada = false;
    private boolean presentacionValida = false;
    private boolean factorValido = false;
    private boolean ubicacionValidada = false;
    private int cantidadDisponibleUbicacion = 0;
    private boolean cantidadTotalValida = false;
    private static final Duration DEBOUNCE_TIEMPO = Duration.millis(300);
    private final PauseTransition loteDebounce = new PauseTransition(DEBOUNCE_TIEMPO);
    private final PauseTransition factorDebounce = new PauseTransition(DEBOUNCE_TIEMPO);
    private final PauseTransition cantidadRapidaDebounce = new PauseTransition(DEBOUNCE_TIEMPO);
    private String ultimoLoteValidado = "";
    private String ultimoFactorValidado = "";
    private String ultimaPresentacionValidada = "";
    private final Map<TextField, PauseTransition> debounceCantidadUbicacion = new HashMap<>();
    private final Map<TextField, String> ultimaCantidadUbicacionValidada = new HashMap<>();
    private final Map<ComboBox<String>, List<UbicacionCompra>> ubicacionesCapturadas = new HashMap<>();
    private boolean inicializado = false;
    private boolean seleccionarClaveAlternaPendiente = false;
    private String tituloFormulario = "Venta";
    private boolean cantidadRapidaValida = false;
    private final Map<String, Image> cacheImagenes = new HashMap<>();
    private boolean modoSoloNormal = false;

    @FXML
    public void initialize() {
        if (lblTitulo != null) {
            lblTitulo.setText(tituloFormulario);
        }
        productoController = new productoCboxController();
        productoController.inicializarDisponibles(cbClaveProducto, cbProductoNombre, cbClaveAlterna);

        configurarPresentaciones();
        configurarPresentacionesRapidas();
        configurarAutocompletadoUbicacion(comboUbicacion);
        configurarEventos();
        configurarValidaciones();
        configurarCalculoPrecios();
        configurarCamposLectura();
        cargarUbicacionesDesdeBD();
        configurarLimpiezaPorCampoVacio();
        configurarManejoEnter();
        configurarCascada();
        configurarModoRapido();
        aplicarModoSoloNormal();
        configurarSeleccionClaveAlternaPorDefecto();
        configurarCampoCantidadUbicacion(txtCantidadUbicacion, comboUbicacion);
        configurarComboUbicacion(comboUbicacion, txtCantidadUbicacion);
        ubicacionesCapturadas.put(comboUbicacion, new ArrayList<>());

        inicializado = true;

        if (itemParaEditar != null) {
            if (tabRapido != null) {
                tabRapido.setDisable(true);
                if (tabPaneModo != null && tabNormal != null) {
                    tabPaneModo.getSelectionModel().select(tabNormal);
                }
            }
            cargarItemParaEditar();
        }

        Platform.runLater(() -> cbClaveProducto.requestFocus());
    }

    private void configurarPresentacionesRapidas() {
        if (cbPresentacionRapida != null) {
            cbPresentacionRapida.setItems(presentaciones);
            cbPresentacionRapida.setValue(null);

            // Listener para cuando se selecciona presentación en modo rápido
            cbPresentacionRapida.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null && !newVal.isBlank()) {
                    // Si es "pz", establecer factor 1 automáticamente y hacerlo no editable
                    if (newVal.equalsIgnoreCase("pz")) {
                        if (txtFactorRapido != null) {
                            txtFactorRapido.setText("1");
                            txtFactorRapido.setEditable(false);
                            txtFactorRapido.setStyle("-fx-background-color: #f0f0f0;");
                        }
                    } else {
                        if (txtFactorRapido != null) {
                            txtFactorRapido.setEditable(true);
                            txtFactorRapido.setStyle("");
                            txtFactorRapido.clear();

                            // Si hay cantidad capturada, limpiarla porque necesita factor
                            if (txtCantidadRapida != null && !txtCantidadRapida.getText().isBlank()) {
                                txtCantidadRapida.clear();
                                mostrarAlertaSinEspera("Advertencia",
                                        "Debe capturar el factor antes de la cantidad para esta presentación.");
                            }
                        }
                    }

                    // Validar presentación en modo rápido
                    validarPresentacionRapida();

                    // Recalcular disponibilidad si hay cantidad
                    if (txtCantidadRapida != null && !txtCantidadRapida.getText().isBlank()) {
                        programarValidacionCantidadRapida();
                    }
                }
            });
        }

        // Configurar validación de números enteros para factor rápido
        if (txtFactorRapido != null) {
            validarNumerosEnteros(txtFactorRapido);

            // Listener para cambios en factor rápido
            txtFactorRapido.textProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null && !newVal.isBlank()) {
                    // Validar factor en modo rápido
                    validarFactorRapidoCompleto(newVal);

                    // Si hay cantidad capturada, revalidarla
                    if (txtCantidadRapida != null && !txtCantidadRapida.getText().isBlank()) {
                        programarValidacionCantidadRapida();
                    }
                } else if (newVal != null && newVal.isBlank()) {
                    // Si se borra el factor y hay cantidad, limpiar la cantidad
                    if (txtCantidadRapida != null && !txtCantidadRapida.getText().isBlank()) {
                        txtCantidadRapida.clear();
                        mostrarAlertaSinEspera("Advertencia",
                                "Debe capturar el factor antes de la cantidad.");
                    }
                }
            });
        }

        // Configurar listener para cantidad rápida
        if (txtCantidadRapida != null) {
            txtCantidadRapida.textProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null && !newVal.isBlank()) {
                    // Verificar que se haya capturado el factor primero
                    String presentacion = cbPresentacionRapida != null ? cbPresentacionRapida.getValue() : null;

                    if (presentacion != null && !presentacion.isBlank()) {
                        // Si no es "pz", verificar que haya factor
                        if (!presentacion.equalsIgnoreCase("pz")) {
                            if (txtFactorRapido != null &&
                                    (txtFactorRapido.getText() == null || txtFactorRapido.getText().isBlank())) {
                                txtCantidadRapida.clear();
                                mostrarAlertaSinEspera("Advertencia",
                                        "Debe capturar el factor antes de la cantidad para la presentación " + presentacion + ".");
                                return;
                            }
                        }
                    } else {
                        // Si no hay presentación seleccionada, limpiar cantidad
                        txtCantidadRapida.clear();
                        mostrarAlertaSinEspera("Advertencia",
                                "Debe seleccionar una presentación antes de la cantidad.");
                        return;
                    }

                    // Programar validación de cantidad
                    programarValidacionCantidadRapida();
                }
            });
        }
    }

    private void validarPresentacionRapida() {
        String presentacion = cbPresentacionRapida != null ? cbPresentacionRapida.getValue() : null;
        if (presentacion == null || presentacion.isBlank()) {
            return;
        }

        String idProducto = productoController.getIdSeleccionado();
        if (idProducto == null || idProducto.isBlank()) {
            mostrarAlertaSinEspera("Advertencia", "Seleccione un producto antes de la presentación.");
            if (cbPresentacionRapida != null) {
                cbPresentacionRapida.setValue(null);
            }
            return;
        }

        String presentacionSnapshot = presentacion;
        String idProductoSnapshot = idProducto;

        javafx.concurrent.Task<Boolean> task = new javafx.concurrent.Task<>() {
            @Override
            protected Boolean call() {
                return modelo.existePresentacionParaProducto(idProductoSnapshot, presentacionSnapshot);
            }

            @Override
            protected void succeeded() {
                String presentacionActual = cbPresentacionRapida != null ? cbPresentacionRapida.getValue() : null;
                String idProductoActual = productoController.getIdSeleccionado();
                if (!presentacionSnapshot.equals(presentacionActual)
                        || !idProductoSnapshot.equals(idProductoActual)) {
                    return;
                }
                boolean existe = getValue();
                if (!existe) {
                    if (cbPresentacionRapida != null) {
                        cbPresentacionRapida.setValue(null);
                    }
                    mostrarAlertaSinEspera("Advertencia",
                            "La presentación no existe para el producto seleccionado.");
                }
            }
        };

        Thread hilo = new Thread(task);
        hilo.setDaemon(true);
        hilo.start();
    }

    private void validarFactorRapidoCompleto(String factorTexto) {
        String presentacion = cbPresentacionRapida != null ? cbPresentacionRapida.getValue() : null;
        if (presentacion == null || presentacion.isBlank()) {
            if (!factorTexto.isBlank()) {
                mostrarAlertaSinEspera("Advertencia", "Debe capturar la presentación antes del factor.");
                if (txtFactorRapido != null) {
                    txtFactorRapido.clear();
                }
            }
            return;
        }

        if (factorTexto.isBlank()) {
            return;
        }

        int factor;
        try {
            factor = Integer.parseInt(factorTexto);
            if (factor <= 0) {
                mostrarAlertaSinEspera("Advertencia", "El factor debe ser un número mayor a 0.");
                if (txtFactorRapido != null) {
                    txtFactorRapido.clear();
                }
                return;
            }
        } catch (NumberFormatException e) {
            mostrarAlertaSinEspera("Advertencia", "El factor debe ser un número válido.");
            if (txtFactorRapido != null) {
                txtFactorRapido.clear();
            }
            return;
        }

        String idProducto = productoController.getIdSeleccionado();
        if (idProducto == null || idProducto.isBlank()) {
            mostrarAlertaSinEspera("Advertencia", "Seleccione un producto antes del factor.");
            if (txtFactorRapido != null) {
                txtFactorRapido.clear();
            }
            return;
        }

        String presentacionSnapshot = presentacion;
        String idProductoSnapshot = idProducto;
        int factorSnapshot = factor;
        String factorTextoSnapshot = factorTexto;

        javafx.concurrent.Task<Boolean> task = new javafx.concurrent.Task<>() {
            @Override
            protected Boolean call() {
                return modelo.existeFactorParaProductoPresentacion(
                        idProductoSnapshot, presentacionSnapshot, factorSnapshot);
            }

            @Override
            protected void succeeded() {
                String presentacionActual = cbPresentacionRapida != null ? cbPresentacionRapida.getValue() : null;
                String idProductoActual = productoController.getIdSeleccionado();
                String factorActual = txtFactorRapido != null ? txtFactorRapido.getText() : null;
                if (!presentacionSnapshot.equals(presentacionActual)
                        || !idProductoSnapshot.equals(idProductoActual)
                        || !factorTextoSnapshot.equals(factorActual)) {
                    return;
                }
                boolean existe = getValue();
                if (!existe) {
                    if (txtFactorRapido != null) {
                        txtFactorRapido.clear();
                    }
                    mostrarAlertaSinEspera("Advertencia",
                            "El factor no corresponde con la presentación seleccionada para este producto.");
                }
            }
        };

        Thread hilo = new Thread(task);
        hilo.setDaemon(true);
        hilo.start();
    }

    private boolean validarCombinacionProductoPresentacionFactorRapido(String clave, String presentacion, int factor) {
        if (clave == null || clave.isBlank() || presentacion == null || presentacion.isBlank() || factor <= 0) {
            return false;
        }

        String claveSnapshot = clave;
        String presentacionSnapshot = presentacion;
        int factorSnapshot = factor;

        javafx.concurrent.Task<Boolean> task = new javafx.concurrent.Task<>() {
            @Override
            protected Boolean call() {
                return modelo.existeCombinacionProductoPresentacionFactor(
                        claveSnapshot, presentacionSnapshot, factorSnapshot);
            }

            @Override
            protected void succeeded() {
                boolean existe = getValue();
                if (!existe) {
                    Platform.runLater(() -> {
                        mostrarAlerta("Error",
                                "La combinación de producto, presentación y factor no existe en inventario.");
                        if (cbPresentacionRapida != null) {
                            cbPresentacionRapida.setValue(null);
                        }
                        if (txtFactorRapido != null) {
                            txtFactorRapido.clear();
                        }
                    });
                }
            }
        };

        // Ejecutar en segundo plano
        Thread hilo = new Thread(task);
        hilo.setDaemon(true);
        hilo.start();

        // Como es asíncrono, retornamos true y la validación mostrará alerta si falla
        return true;
    }

    private void aplicarModoSoloNormal() {
        if (!modoSoloNormal || tabPaneModo == null) {
            return;
        }

        if (tabRapido != null) {
            tabPaneModo.getTabs().remove(tabRapido);
        }
        if (tabNormal != null) {
            tabPaneModo.getSelectionModel().select(tabNormal);
        }
        tabPaneModo.getStyleClass().add("modo-tabs-sin-header");
    }

    public void setModoSoloNormal(boolean modoSoloNormal) {
        this.modoSoloNormal = modoSoloNormal;
        aplicarModoSoloNormal();
    }

    private void configurarPresentaciones() {
        cbPresentacion.setItems(presentaciones);
        cbPresentacion.setValue(null);

        // Agregar listener para cuando se selecciona presentación en modo normal
        cbPresentacion.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.isBlank()) {
                // Validar que haya lote capturado antes de permitir seleccionar presentación
                String lote = txtLote.getText() != null ? txtLote.getText().trim() : "";
                if (lote.isBlank()) {
                    cbPresentacion.setValue(null);
                    mostrarAlertaCascada("Debe capturar el lote antes de seleccionar la presentación.");
                    return;
                }

                // Si es "pz", establecer factor 1 automáticamente
                if (newVal.equalsIgnoreCase("pz")) {
                    txtFactor.setText("1");
                    txtFactor.setEditable(false);
                    txtFactor.setStyle("-fx-background-color: #f0f0f0;");
                    factorValido = true;  // Marcar como válido ya que es automático
                    ultimoFactorValidado = "1";  // Establecer el factor validado
                } else {
                    txtFactor.setEditable(true);
                    txtFactor.setStyle("");
                    txtFactor.clear();
                    factorValido = false;  // Resetear validez
                    ultimoFactorValidado = "";  // Limpiar factor validado

                    // Si hay cantidad capturada, limpiarla porque necesita factor (como en modo rápido)
                    if (!txtCantidad.getText().isBlank()) {
                        txtCantidad.clear();
                        mostrarAlertaCascada("Debe capturar el factor antes de la cantidad para esta presentación.");
                    }
                }
            }

            // SOLO limpiar factor si no es "pz" o si se cambia de "pz" a otra cosa
            if (newVal != null && !newVal.equals(oldVal)) {
                if (oldVal != null && oldVal.equalsIgnoreCase("pz") &&
                        (newVal == null || !newVal.equalsIgnoreCase("pz"))) {
                    txtFactor.clear();
                    ultimoFactorValidado = "";
                    factorValido = false;
                }
            }

            presentacionValida = false;
            validarPresentacion();
            actualizarEstadoCascada();
        });
    }

    private void cargarUbicacionesDesdeBD() {
        javafx.concurrent.Task<List<String>> task = new javafx.concurrent.Task<>() {
            @Override
            protected List<String> call() {
                return modelo.obtenerNombresUbicaciones();
            }

            @Override
            protected void succeeded() {
                List<String> resultados = getValue();
                ubicaciones.setAll(resultados != null ? resultados : List.of());
            }

            @Override
            protected void failed() {
                ubicaciones.clear();
            }
        };

        Thread hilo = new Thread(task);
        hilo.setDaemon(true);
        hilo.start();
    }

    private void configurarEventos() {
        cbClaveProducto.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                actualizarDescripcionDesdeProducto();
                cargarPrecioEntradaDesdeProducto();
                cargarPrecioRapidoDesdeUltimaEntrada();
                actualizarEstadoCascada();
                seleccionarClaveAlternaPendiente = true;
            }
            actualizarImagenProducto();
        });

        cbProductoNombre.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                actualizarDescripcionDesdeProducto();
                cargarPrecioEntradaDesdeProducto();
                cargarPrecioRapidoDesdeUltimaEntrada();
                actualizarEstadoCascada();
                seleccionarClaveAlternaPendiente = true;
            }
            actualizarImagenProducto();
        });

        cbClaveAlterna.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                actualizarDescripcionDesdeProducto();
                cargarPrecioEntradaDesdeProducto();
                cargarPrecioRapidoDesdeUltimaEntrada();
                actualizarEstadoCascada();
            }
            actualizarImagenProducto();
        });

        // Listener para cantidad en modo normal
        txtCantidad.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.isBlank()) {
                // Verificar que haya presentación seleccionada
                String presentacion = cbPresentacion.getValue();
                if (presentacion == null || presentacion.isBlank()) {
                    txtCantidad.clear();
                    mostrarAlertaCascada("Debe seleccionar una presentación antes de la cantidad.");
                    return;
                }

                // Verificar que haya factor para presentaciones que no sean "pz"
                if (!presentacion.equalsIgnoreCase("pz")) {
                    if (txtFactor.getText() == null || txtFactor.getText().isBlank()) {
                        txtCantidad.clear();
                        mostrarAlertaCascada("Debe capturar el factor antes de la cantidad para la presentación " + presentacion + ".");
                        return;
                    }
                }
            }
        });

        // Listener para factor en modo normal
        txtFactor.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.isBlank()) {
                // Verificar que haya presentación seleccionada
                String presentacion = cbPresentacion.getValue();
                if (presentacion == null || presentacion.isBlank()) {
                    txtFactor.clear();
                    mostrarAlertaCascada("Debe seleccionar una presentación antes del factor.");
                    return;
                }
            }
        });

        txtLote.textProperty().addListener((obs, oldVal, newVal) -> {
            if (oldVal != null && !oldVal.equals(newVal)) {
                loteValidado = false;
                caducidadValidada = false;
                presentacionValida = false;
                factorValido = false;
                ubicacionValidada = false;
                cantidadDisponibleUbicacion = 0;
                cantidadTotalValida = false;
                dpCaducidad.setValue(null);
                cbPresentacion.setValue(null);
                txtFactor.clear();
                limpiarUbicacionPrimaria();
                limpiarPrecios();
                actualizarEstadoCascada();
            }
            programarValidacionLote(newVal);
        });

        btnGuardar.setOnAction(e -> guardarItem());
        if (btnLimpiar != null) {
            btnLimpiar.setOnAction(e -> limpiarFormularioParaNuevo());
        }
    }

    private void configurarLimpiezaPorCampoVacio() {
        configurarLimpiezaCombo(cbClaveProducto);
        configurarLimpiezaCombo(cbProductoNombre);
        configurarLimpiezaCombo(cbClaveAlterna);
    }

    private void configurarLimpiezaCombo(ComboBox<String> comboBox) {
        comboBox.getEditor().textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.isBlank()) {
                comboBox.setValue(null);
                limpiarValidacionesInventario();
                limpiarFormularioDependiente();
            }
        });
    }

    private void actualizarImagenProducto() {
        if (previewImage == null) {
            return;
        }

        previewImage.setImage(null);

        if (productoController == null) {
            return;
        }

        String urlImagen = productoController.getUrlImagenSeleccionada();
        if (urlImagen == null || urlImagen.isBlank()) {
            return;
        }

        if (cacheImagenes.containsKey(urlImagen)) {
            previewImage.setImage(cacheImagenes.get(urlImagen));
            return;
        }

        Task<Image> task = new Task<>() {
            @Override
            protected Image call() throws Exception {
                conexionFTP ftp = new conexionFTP();
                return ftp.getImageFromFTP(urlImagen);
            }
        };
        task.setOnSucceeded(e -> {
            Image img = task.getValue();
            cacheImagenes.put(urlImagen, img);
            previewImage.setImage(img);
        });
        task.setOnFailed(e -> previewImage.setImage(null));
        new Thread(task).start();
    }

    private void limpiarFormularioDependiente() {
        txtDescripcion.clear();
        txtLote.clear();
        dpCaducidad.setValue(null);
        txtCantidad.clear();
        cbPresentacion.setValue(null);
        txtFactor.clear();
        txtFactor.setEditable(true); // Restaurar editable por defecto
        txtFactor.setStyle("");
        limpiarUbicacionPrimaria();
        limpiarPrecios();
        if (txtNota != null) {
            txtNota.clear();
        }
        if (txtCantidadRapida != null) {
            txtCantidadRapida.clear();
        }
        if (cbPresentacionRapida != null) {
            cbPresentacionRapida.setValue(null);
        }
        if (txtFactorRapido != null) {
            txtFactorRapido.clear();
        }
        if (txtPrecioEntradaRapida != null) {
            txtPrecioEntradaRapida.clear();
        }
        if (txtPrecioEntradaIvaRapida != null) {
            txtPrecioEntradaIvaRapida.clear();
        }
        if (txtPrecioSalidaRapida != null) {
            txtPrecioSalidaRapida.clear();
        }
        if (checkBoxIVARapida != null) {
            checkBoxIVARapida.setSelected(false);
        }
        if (txtPrecioIVARapida != null) {
            txtPrecioIVARapida.clear();
        }
        if (txtPrecioBrutoRapida != null) {
            txtPrecioBrutoRapida.clear();
        }
        if (txtPrecioTotalRapida != null) {
            txtPrecioTotalRapida.clear();
        }
    }

    private void actualizarDescripcionDesdeProducto() {
        String descripcion = productoController.getDescripcionSeleccionada();
        txtDescripcion.setText(descripcion);
    }

    private void cargarPrecioEntradaDesdeProducto() {
        String idProducto = productoController.getIdSeleccionado();
        if (idProducto == null || idProducto.isBlank()) {
            limpiarPrecios();
            return;
        }

        if (!datosCompletosParaPrecioEntrada()) {
            limpiarPrecios();
            return;
        }

        String lote = txtLote.getText() != null ? txtLote.getText().trim() : "";
        java.time.LocalDate caducidad = dpCaducidad.getValue();

        javafx.concurrent.Task<Optional<modelNuevoTraspasoSalida.PreciosProducto>> task = new javafx.concurrent.Task<>() {
            @Override
            protected Optional<modelNuevoTraspasoSalida.PreciosProducto> call() {
                return modelo.obtenerPreciosProductoPorLoteCaducidad(idProducto, lote, caducidad);
            }

            @Override
            protected void succeeded() {
                Optional<modelNuevoTraspasoSalida.PreciosProducto> resultado = getValue();
                if (resultado.isPresent()) {
                    aplicarPrecioEntrada(resultado.get());
                }
            }
        };

        Thread hilo = new Thread(task);
        hilo.setDaemon(true);
        hilo.start();
    }

    private void cargarPrecioRapidoDesdeUltimaEntrada() {
        String idProducto = productoController.getIdSeleccionado();
        if (idProducto == null || idProducto.isBlank()) {
            limpiarPreciosRapidos();
            return;
        }
        String idSnapshot = idProducto;
        javafx.concurrent.Task<Optional<modelNuevoTraspasoSalida.PreciosProducto>> task = new javafx.concurrent.Task<>() {
            @Override
            protected Optional<modelNuevoTraspasoSalida.PreciosProducto> call() {
                return modelo.obtenerPreciosProductoUltimaEntrada(idSnapshot);
            }

            @Override
            protected void succeeded() {
                String idActual = productoController.getIdSeleccionado();
                if (!idSnapshot.equals(idActual)) {
                    return;
                }
                Optional<modelNuevoTraspasoSalida.PreciosProducto> resultado = getValue();
                if (resultado.isPresent()) {
                    aplicarPreciosRapidos(resultado.get());
                } else {
                    limpiarPreciosRapidos();
                }
            }

            @Override
            protected void failed() {
                limpiarPreciosRapidos();
            }
        };

        Thread hilo = new Thread(task);
        hilo.setDaemon(true);
        hilo.start();
    }

    private void aplicarPreciosRapidos(modelNuevoTraspasoSalida.PreciosProducto precios) {
        if (precios == null) {
            limpiarPreciosRapidos();
            return;
        }
        BigDecimal precioEntradaRapida = precios.getPrecioUnitario() != null
                ? precios.getPrecioUnitario()
                : BigDecimal.ZERO;
        BigDecimal precioEntradaIvaRapida = precios.getPrecioIva() != null
                ? precios.getPrecioIva()
                : BigDecimal.ZERO;

        if (txtPrecioEntradaRapida != null) {
            txtPrecioEntradaRapida.setText(formatearDecimal(precioEntradaRapida));
        }
        if (txtPrecioEntradaIvaRapida != null) {
            txtPrecioEntradaIvaRapida.setText(formatearDecimal(precioEntradaIvaRapida));
        }
        if (txtPrecioSalidaRapida != null) {
            txtPrecioSalidaRapida.setText(formatearDecimal(precioEntradaRapida));
        }
        recalcularPreciosRapido();
    }

    private void limpiarPreciosRapidos() {
        if (txtPrecioEntradaRapida != null) {
            txtPrecioEntradaRapida.clear();
        }
        if (txtPrecioEntradaIvaRapida != null) {
            txtPrecioEntradaIvaRapida.clear();
        }
        if (txtPrecioSalidaRapida != null) {
            txtPrecioSalidaRapida.clear();
        }
        if (txtPrecioIVARapida != null) {
            txtPrecioIVARapida.clear();
        }
        if (txtPrecioBrutoRapida != null) {
            txtPrecioBrutoRapida.clear();
        }
        if (txtPrecioTotalRapida != null) {
            txtPrecioTotalRapida.clear();
        }
    }

    private void aplicarPrecioEntrada(modelNuevoTraspasoSalida.PreciosProducto precios) {
        if (precios == null) {
            limpiarPrecios();
            return;
        }
        precioEntradaBase = precios.getPrecioUnitario() != null ? precios.getPrecioUnitario() : BigDecimal.ZERO;
        precioEntradaIvaBase = precios.getPrecioIva() != null ? precios.getPrecioIva() : BigDecimal.ZERO;

        txtPrecioEntrada.setText(formatearDecimal(precioEntradaBase));
        if (txtPrecioEntradaIva != null) {
            txtPrecioEntradaIva.setText(formatearDecimal(precioEntradaIvaBase));
        }

        txtPrecioSalida.setText(formatearDecimal(precioEntradaBase));

        recalcularPrecios();
    }

    private void limpiarPrecios() {
        txtPrecioEntrada.clear();
        if (txtPrecioEntradaIva != null) {
            txtPrecioEntradaIva.clear();
        }
        txtPrecioSalida.clear();
        txtPrecioIVA.clear();
        txtPrecioBruto.clear();
        txtPrecioTotal.clear();
        precioEntradaBase = BigDecimal.ZERO;
        precioEntradaIvaBase = BigDecimal.ZERO;
    }

    private void guardarItem() {
        if (esModoRapido()) {
            guardarItemRapido();
            return;
        }
        String clave = productoController.getIdSeleccionado();
        String nombre = productoController.getNombreSeleccionado();
        String descripcion = txtDescripcion.getText() != null ? txtDescripcion.getText().trim() : "";
        String lote = txtLote.getText() != null ? txtLote.getText().trim() : "";
        java.time.LocalDate caducidad = dpCaducidad.getValue();
        String cantidadTexto = txtCantidad.getText() != null ? txtCantidad.getText().trim() : "";
        String nota = txtNota != null && txtNota.getText() != null ? txtNota.getText().trim() : "";
        String presentacion = cbPresentacion.getValue();
        String factorTexto = txtFactor.getText() != null ? txtFactor.getText().trim() : "";
        String precioEntrada = txtPrecioEntrada.getText() != null ? txtPrecioEntrada.getText().trim() : "";
        String precioEntradaIva = txtPrecioEntradaIva != null && txtPrecioEntradaIva.getText() != null
                ? txtPrecioEntradaIva.getText().trim()
                : "";
        String precioSalida = txtPrecioSalida.getText() != null ? txtPrecioSalida.getText().trim() : "";
        String precioIva = txtPrecioIVA.getText() != null ? txtPrecioIVA.getText().trim() : "";
        String precioBruto = txtPrecioBruto.getText() != null ? txtPrecioBruto.getText().trim() : "";
        String precioTotal = txtPrecioTotal.getText() != null ? txtPrecioTotal.getText().trim() : "";

        if (clave == null || clave.isBlank()
                || nombre == null || nombre.isBlank()
                || descripcion.isBlank()
                || lote.isBlank()
                || cantidadTexto.isBlank()
                || presentacion == null || presentacion.isBlank()
                || factorTexto.isBlank()
                || precioEntrada.isBlank()
                || precioEntradaIva.isBlank()
                || precioSalida.isBlank()
                || precioIva.isBlank()
                || precioBruto.isBlank()
                || precioTotal.isBlank()) {
            mostrarAlerta("Advertencia", "Debe completar todos los campos antes de guardar, excepto el comentario.");
            return;
        }

        int cantidad;
        int factor;
        try {
            cantidad = Integer.parseInt(cantidadTexto);
            if (cantidad <= 0) {
                mostrarAlerta("Advertencia", "La cantidad debe ser mayor a 0.");
                return;
            }
        } catch (NumberFormatException e) {
            mostrarAlerta("Error", "La cantidad debe ser un número válido.");
            return;
        }

        try {
            factor = Integer.parseInt(factorTexto);
            if (factor <= 0) {
                mostrarAlerta("Advertencia", "El factor debe ser mayor a 0.");
                return;
            }
        } catch (NumberFormatException e) {
            mostrarAlerta("Error", "El factor debe ser un número válido.");
            return;
        }

        List<UbicacionCompra> ubicacionesSeleccionadas = obtenerUbicacionesSeleccionadas();
        if (ubicacionesSeleccionadas.isEmpty()) {
            mostrarAlerta("Advertencia", "Debe capturar las ubicaciones con cantidad.");
            return;
        }
        if (existeProductoLoteEnVenta(clave, lote)) {
            mostrarAlerta("Advertencia",
                    "Ya se agregó este producto con el mismo lote. Finaliza la venta para poder repetirlo.");
            return;
        }
        if (tieneUbicacionesDuplicadas(ubicacionesSeleccionadas)) {
            mostrarAlerta("Advertencia", "No se puede seleccionar la misma ubicación más de una vez.");
            return;
        }
        int sumaUbicaciones = ubicacionesSeleccionadas.stream()
                .mapToInt(UbicacionCompra::getCantidad)
                .sum();
        if (sumaUbicaciones != cantidad) {
            mostrarAlerta("Advertencia", "La suma de cantidades por ubicación debe ser igual a la cantidad total.");
            return;
        }

        BigDecimal precioEntradaDecimal = parseDecimal(precioEntrada);
        BigDecimal precioSalidaDecimal = parseDecimal(precioSalida);
        if (precioSalidaDecimal.compareTo(precioEntradaDecimal) < 0) {
            Alert confirmacion = new Alert(AlertType.CONFIRMATION);
            confirmacion.setTitle("Advertencia");
            confirmacion.setHeaderText("El precio de salida es menor al precio de entrada.");
            confirmacion.setContentText("Esto representa una pérdida de dinero. ¿Deseas continuar?");
            Optional<javafx.scene.control.ButtonType> respuesta = confirmacion.showAndWait();
            if (respuesta.isEmpty() || respuesta.get() != javafx.scene.control.ButtonType.OK) {
                return;
            }
        }

        if (itemsVenta == null) {
            mostrarAlerta("Error", "No se pudo registrar la venta en la tabla.");
            return;
        }

        if (itemParaEditar != null) {
            itemParaEditar.setClaveProducto(clave);
            itemParaEditar.setProducto(nombre);
            itemParaEditar.setDescripcion(descripcion);
            itemParaEditar.setLote(lote);
            itemParaEditar.setCaducidad(caducidad.toString());
            itemParaEditar.setCantidad(cantidad);
            itemParaEditar.setPresentacion(presentacion);
            itemParaEditar.setFactor(factor);
            itemParaEditar.setUbicaciones(ubicacionesSeleccionadas);
            itemParaEditar.setNota(nota);
            itemParaEditar.setPrecioEntrada(precioSalida);
            itemParaEditar.setPrecioIva(precioIva);
            itemParaEditar.setPrecioBruto(precioBruto);
            itemParaEditar.setPrecioTotal(precioTotal);
        } else {
            traspasoSalida item = new traspasoSalida(
                    clave,
                    nombre,
                    descripcion,
                    lote,
                    caducidad.toString(),
                    cantidad,
                    presentacion,
                    factor,
                    ubicacionesSeleccionadas,
                    precioSalida,
                    precioIva,
                    precioBruto,
                    precioTotal
            );
            item.setNota(nota);
            itemsVenta.add(item);
        }

        if (mainController != null) {
            mainController.refrescarTabla();
        }

        if (itemParaEditar != null) {
            mostrarAlertaSinEspera("Éxito", "Producto actualizado.");
            cerrarFormulario();
        } else {
            mostrarAlertaSinEspera("Éxito", "Producto agregado a la venta.");
            limpiarFormularioParaNuevo();
        }
    }

    private void guardarItemRapido() {
        if (itemParaEditar != null) {
            mostrarAlerta("Advertencia", "La edición está disponible solo en el modo normal.");
            return;
        }

        String clave = productoController.getIdSeleccionado();
        String nombre = productoController.getNombreSeleccionado();
        String descripcion = txtDescripcion.getText() != null ? txtDescripcion.getText().trim() : "";
        String cantidadTexto = txtCantidadRapida != null && txtCantidadRapida.getText() != null
                ? txtCantidadRapida.getText().trim()
                : "";

        // Obtener presentación y factor del modo rápido
        String presentacionRapida = "pz"; // Valor por defecto
        int factorRapido = 1; // Valor por defecto

        if (cbPresentacionRapida != null && cbPresentacionRapida.getValue() != null
                && !cbPresentacionRapida.getValue().isBlank()) {
            presentacionRapida = cbPresentacionRapida.getValue().trim();
        }

        if (txtFactorRapido != null && txtFactorRapido.getText() != null && !txtFactorRapido.getText().isBlank()) {
            try {
                factorRapido = Integer.parseInt(txtFactorRapido.getText().trim());
                if (factorRapido <= 0) {
                    mostrarAlerta("Advertencia", "El factor debe ser mayor a 0.");
                    return;
                }
            } catch (NumberFormatException e) {
                mostrarAlerta("Error", "El factor debe ser un número válido.");
                return;
            }
        } else if (!presentacionRapida.equalsIgnoreCase("pz")) {
            // Si no es "pz" y no tiene factor, mostrar error
            mostrarAlerta("Advertencia", "Debe especificar un factor para la presentación seleccionada.");
            return;
        }

        // VALIDACIÓN 1: Verificar que el producto esté seleccionado
        if (clave == null || clave.isBlank() || nombre == null || nombre.isBlank()) {
            mostrarAlerta("Advertencia", "Debe seleccionar un producto.");
            return;
        }

        // VALIDACIÓN 2: Verificar que la presentación esté seleccionada
        if (presentacionRapida.isBlank()) {
            mostrarAlerta("Advertencia", "Debe seleccionar una presentación.");
            return;
        }

        // VALIDACIÓN 3: Verificar que la cantidad esté capturada
        if (cantidadTexto.isBlank()) {
            mostrarAlerta("Advertencia", "Debe capturar la cantidad.");
            return;
        }

        // VALIDACIÓN 4: Verificar que los precios estén completos
        String precioSalida = txtPrecioSalidaRapida != null && txtPrecioSalidaRapida.getText() != null
                ? txtPrecioSalidaRapida.getText().trim()
                : "";
        String precioIva = txtPrecioIVARapida != null && txtPrecioIVARapida.getText() != null
                ? txtPrecioIVARapida.getText().trim()
                : "";
        String precioBruto = txtPrecioBrutoRapida != null && txtPrecioBrutoRapida.getText() != null
                ? txtPrecioBrutoRapida.getText().trim()
                : "";
        String precioTotal = txtPrecioTotalRapida != null && txtPrecioTotalRapida.getText() != null
                ? txtPrecioTotalRapida.getText().trim()
                : "";

        if (descripcion.isBlank() || precioSalida.isBlank() || precioIva.isBlank()
                || precioBruto.isBlank() || precioTotal.isBlank()) {
            mostrarAlerta("Advertencia", "Debe completar todos los campos antes de guardar.");
            return;
        }

        // VALIDACIÓN 5: Validar cantidad numérica
        int cantidad;
        try {
            cantidad = Integer.parseInt(cantidadTexto);
            if (cantidad <= 0) {
                mostrarAlerta("Advertencia", "La cantidad debe ser mayor a 0.");
                return;
            }
        } catch (NumberFormatException e) {
            mostrarAlerta("Error", "La cantidad debe ser un número válido.");
            return;
        }

        // VALIDACIÓN 6: Confirmar pérdida si aplica
        if (!confirmarPerdidaRapidaSiAplica(cantidad)) {
            return;
        }

        // VALIDACIÓN 7: Verificar disponibilidad del producto con presentación y factor específicos
        int disponible = modelo.obtenerCantidadDisponibleProductoPresentacionFactor(
                clave, presentacionRapida, factorRapido);
        if (cantidad > disponible) {
            mostrarAlerta("Advertencia",
                    "La cantidad supera la disponible para la presentación " + presentacionRapida
                            + " con factor " + factorRapido + ".");
            return;
        }

        // VALIDACIÓN 8: Verificar que la combinación producto-presentación-factor exista en inventario
        // Esta es una validación asíncrona que se ejecuta en segundo plano
        String claveSnapshot = clave;
        String presentacionSnapshot = presentacionRapida;
        int factorSnapshot = factorRapido;
        int cantidadSnapshot = cantidad;

        javafx.concurrent.Task<Boolean> validacionTask = new javafx.concurrent.Task<>() {
            @Override
            protected Boolean call() {
                // Primero verificar si la combinación existe
                boolean combinacionExiste = modelo.existeCombinacionProductoPresentacionFactor(
                        claveSnapshot, presentacionSnapshot, factorSnapshot);

                if (!combinacionExiste) {
                    return false;
                }

                // Luego verificar disponibilidad específica
                return modelo.obtenerCantidadDisponibleProductoPresentacionFactor(
                        claveSnapshot, presentacionSnapshot, factorSnapshot) >= cantidadSnapshot;
            }

            @Override
            protected void succeeded() {
                boolean validacionExitosa = getValue();
                if (!validacionExitosa) {
                    Platform.runLater(() -> {
                        mostrarAlerta("Error",
                                "La combinación de producto, presentación y factor no existe en inventario " +
                                        "o no hay suficiente cantidad disponible.");
                    });
                } else {
                    // Si la validación es exitosa, continuar con el resto del proceso
                    Platform.runLater(() -> continuarGuardadoRapido(claveSnapshot, nombre, descripcion,
                            presentacionSnapshot, factorSnapshot, cantidadSnapshot,
                            precioSalida, precioIva, precioBruto, precioTotal));
                }
            }

            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    mostrarAlerta("Error", "Error al validar la disponibilidad del producto.");
                });
            }
        };

        // Ejecutar la validación en segundo plano
        Thread hiloValidacion = new Thread(validacionTask);
        hiloValidacion.setDaemon(true);
        hiloValidacion.start();
    }

    // NUEVO MÉTODO: Continuar con el guardado después de la validación exitosa
    private void continuarGuardadoRapido(String clave, String nombre, String descripcion,
                                         String presentacionRapida, int factorRapido, int cantidad,
                                         String precioSalida, String precioIva,
                                         String precioBruto, String precioTotal) {

        if (itemsVenta == null) {
            mostrarAlerta("Error", "No se pudo registrar la venta en la tabla.");
            return;
        }

        // Obtener disponibilidades rápidas
        List<modelNuevoTraspasoSalida.DisponibilidadRapida> disponibles =
                modelo.obtenerDisponibilidadesRapidas(clave, presentacionRapida, factorRapido);

        if (disponibles == null || disponibles.isEmpty()) {
            mostrarAlerta("Error", "No hay disponibilidad para el producto con las características especificadas.");
            return;
        }

        // Construir asignaciones
        List<AsignacionRapida> asignaciones = construirAsignacionesRapidas(disponibles, cantidad);
        if (asignaciones.isEmpty()) {
            mostrarAlerta("Error", "No se pudo distribuir la cantidad solicitada con la disponibilidad actual.");
            return;
        }

        // Confirmaciones del usuario
        if (!confirmarRevisionUbicacionesRapidas()) {
            return;
        }
        if (!mostrarResumenUbicacionesRapidas(asignaciones)) {
            return;
        }
        mostrarAlerta("Aviso", "Revisión de ubicaciones confirmada.");

        // Construir items de venta
        List<traspasoSalida> itemsGenerados = construirItemsRapidosVenta(
                clave, nombre, descripcion, asignaciones, presentacionRapida, factorRapido);

        if (itemsGenerados.isEmpty()) {
            mostrarAlerta("Error", "No se pudo distribuir la cantidad solicitada con la disponibilidad actual.");
            return;
        }

        // Procesar cada item generado
        BigDecimal precioSalidaDecimal = parseDecimal(precioSalida);
        BigDecimal precioIvaDecimal = parseDecimal(precioIva);

        for (traspasoSalida item : itemsGenerados) {
            // Verificar si ya existe en la venta
            if (existeProductoLoteEnVenta(clave, item.getLote())) {
                mostrarAlerta("Advertencia",
                        "Ya se agregó este producto con el mismo lote. Finaliza la venta para poder repetirlo.");
                return;
            }

            // Calcular precios para este item
            int cantidadItem = item.getCantidad();
            BigDecimal brutoItem = precioSalidaDecimal.multiply(BigDecimal.valueOf(cantidadItem));
            BigDecimal totalItem = precioIvaDecimal.multiply(BigDecimal.valueOf(cantidadItem));

            // Establecer precios en el item
            item.setPrecioEntrada(formatearDecimal(precioSalidaDecimal));
            item.setPrecioIva(formatearDecimal(precioIvaDecimal));
            item.setPrecioBruto(formatearDecimal(brutoItem));
            item.setPrecioTotal(formatearDecimal(totalItem));

            // Agregar a la lista de ventas
            itemsVenta.add(item);
        }

        // Actualizar interfaz si hay controlador principal
        if (mainController != null) {
            mainController.refrescarTabla();
        }

        // Mostrar mensaje de éxito y limpiar formulario
        mostrarAlertaSinEspera("Éxito", "Producto agregado a la venta.");
        limpiarFormularioParaNuevo();
    }


    private boolean confirmarPerdidaRapidaSiAplica(int cantidad) {
        if (txtPrecioEntradaRapida == null || txtPrecioSalidaRapida == null) {
            return true;
        }
        String precioEntradaTexto = txtPrecioEntradaRapida.getText() != null
                ? txtPrecioEntradaRapida.getText().trim()
                : "";
        String precioSalidaTexto = txtPrecioSalidaRapida.getText() != null
                ? txtPrecioSalidaRapida.getText().trim()
                : "";
        if (precioEntradaTexto.isBlank() || precioSalidaTexto.isBlank()) {
            return true;
        }
        BigDecimal precioEntrada = parseDecimal(precioEntradaTexto);
        BigDecimal precioSalida = parseDecimal(precioSalidaTexto);
        if (precioSalida.compareTo(precioEntrada) >= 0) {
            return true;
        }
        BigDecimal perdidaUnit = precioEntrada.subtract(precioSalida);
        BigDecimal perdidaTotal = perdidaUnit.multiply(BigDecimal.valueOf(cantidad));

        Alert alerta = new Alert(AlertType.WARNING);
        alerta.setTitle("Advertencia de pérdida");
        alerta.setHeaderText("El precio de salida es menor al precio de entrada.");
        alerta.getButtonTypes().setAll(javafx.scene.control.ButtonType.OK, javafx.scene.control.ButtonType.CANCEL);
        VBox contenido = new VBox(6);
        Label mensaje = new Label("Puede haber posibles pérdidas con este precio.");
        Label perdidaLabel = new Label("Pérdida estimada: " + formatearDecimal(perdidaTotal));
        perdidaLabel.setStyle("-fx-text-fill: #d9534f; -fx-font-weight: bold;");
        contenido.getChildren().addAll(mensaje, perdidaLabel);
        alerta.getDialogPane().setContent(contenido);
        Optional<javafx.scene.control.ButtonType> respuesta = alerta.showAndWait();
        return respuesta.isPresent() && respuesta.get() == javafx.scene.control.ButtonType.OK;
    }

    private List<AsignacionRapida> construirAsignacionesRapidas(
            List<modelNuevoTraspasoSalida.DisponibilidadRapida> disponibles,
            int cantidad
    ) {
        if (disponibles == null || disponibles.isEmpty()) {
            return List.of();
        }
        List<AsignacionRapida> resultado = new ArrayList<>();
        int restante = cantidad;
        for (modelNuevoTraspasoSalida.DisponibilidadRapida disp : disponibles) {
            if (restante <= 0) {
                break;
            }
            int asignar = Math.min(restante, disp.getTotal());
            if (asignar <= 0) {
                continue;
            }
            resultado.add(new AsignacionRapida(disp.getUbicacion(), disp.getLote(), disp.getCaducidad(), asignar));
            restante -= asignar;
        }
        if (restante > 0) {
            return List.of();
        }
        return resultado;
    }

    private boolean confirmarRevisionUbicacionesRapidas() {
        Alert confirmacion = new Alert(AlertType.CONFIRMATION);
        confirmacion.setTitle("Revisión de ubicaciones");
        confirmacion.setHeaderText("Revisa bien las ubicaciones de donde se sacan los productos.");
        confirmacion.setContentText("Presiona Aceptar para continuar o Cancelar para detener el guardado.");
        Optional<javafx.scene.control.ButtonType> respuesta = confirmacion.showAndWait();
        return respuesta.isPresent() && respuesta.get() == javafx.scene.control.ButtonType.OK;
    }

    private boolean mostrarResumenUbicacionesRapidas(List<AsignacionRapida> asignaciones) {
        if (asignaciones == null || asignaciones.isEmpty()) {
            return false;
        }
        Map<String, Map<String, Integer>> lotesPorUbicacion = new java.util.LinkedHashMap<>();
        Map<String, Integer> totalesPorUbicacion = new java.util.LinkedHashMap<>();
        for (AsignacionRapida asignacion : asignaciones) {
            if (asignacion == null) {
                continue;
            }
            String ubicacion = asignacion.ubicacion != null ? asignacion.ubicacion : "";
            String lote = asignacion.lote != null && !asignacion.lote.isBlank() ? asignacion.lote : "Sin lote";
            lotesPorUbicacion.computeIfAbsent(ubicacion, k -> new java.util.LinkedHashMap<>())
                    .merge(lote, asignacion.cantidad, Integer::sum);
            totalesPorUbicacion.merge(ubicacion, asignacion.cantidad, Integer::sum);
        }

        VBox contenido = new VBox(10);
        contenido.setFillWidth(true);
        contenido.setAlignment(Pos.TOP_LEFT);

        for (Map.Entry<String, Map<String, Integer>> entry : lotesPorUbicacion.entrySet()) {
            String ubicacion = entry.getKey();
            int total = totalesPorUbicacion.getOrDefault(ubicacion, 0);
            HBox filaUbicacion = new HBox(10);
            filaUbicacion.setStyle("-fx-padding: 6 8 6 8; -fx-background-color: #000000;");
            filaUbicacion.setAlignment(Pos.CENTER_LEFT);
            Label ubicacionLabel = new Label("Ubicación " + ubicacion + ":");
            ubicacionLabel.setStyle("-fx-text-fill: #ffffff; -fx-font-weight: bold;");
            Label totalLabel = new Label(String.valueOf(total));
            totalLabel.setStyle("-fx-text-fill: #ffffff; -fx-font-weight: bold;");
            HBox.setHgrow(ubicacionLabel, Priority.ALWAYS);
            filaUbicacion.getChildren().addAll(ubicacionLabel, totalLabel);
            contenido.getChildren().add(filaUbicacion);

            for (Map.Entry<String, Integer> loteEntry : entry.getValue().entrySet()) {
                HBox filaLote = new HBox(10);
                filaLote.setStyle("-fx-padding: 6 8 6 8; -fx-background-color: #ffffff; "
                        + "-fx-border-color: #cccccc; -fx-border-width: 1;");
                filaLote.setAlignment(Pos.CENTER_LEFT);
                Label loteLabel = new Label("Lote " + loteEntry.getKey() + ":");
                Label cantidadLabel = new Label(String.valueOf(loteEntry.getValue()));
                HBox.setHgrow(loteLabel, Priority.ALWAYS);
                filaLote.getChildren().addAll(loteLabel, cantidadLabel);
                contenido.getChildren().add(filaLote);
            }
        }

        Alert resumenAlert = new Alert(AlertType.CONFIRMATION);
        resumenAlert.setTitle("Ubicaciones sugeridas");
        resumenAlert.setHeaderText("Primeras ubicaciones encontradas");
        resumenAlert.setContentText("Presiona Aceptar para continuar o Cancelar para volver al formulario.");
        resumenAlert.getButtonTypes().setAll(javafx.scene.control.ButtonType.OK, javafx.scene.control.ButtonType.CANCEL);
        resumenAlert.getDialogPane().setContent(contenido);
        Optional<javafx.scene.control.ButtonType> respuesta = resumenAlert.showAndWait();
        return respuesta.isPresent() && respuesta.get() == javafx.scene.control.ButtonType.OK;
    }

    private List<traspasoSalida> construirItemsRapidosVenta(
            String clave,
            String nombre,
            String descripcion,
            List<AsignacionRapida> asignaciones,
            String presentacion,  // NUEVO parámetro
            int factor            // NUEVO parámetro
    ) {
        List<traspasoSalida> resultado = new ArrayList<>();
        if (asignaciones == null || asignaciones.isEmpty()) {
            return resultado;
        }

        Map<LoteCaducidadKey, List<UbicacionCompra>> ubicacionesPorLote = new java.util.LinkedHashMap<>();
        Map<LoteCaducidadKey, Integer> cantidadesPorLote = new java.util.LinkedHashMap<>();

        for (AsignacionRapida asignacion : asignaciones) {
            if (asignacion == null || asignacion.cantidad <= 0) {
                continue;
            }
            LoteCaducidadKey key = new LoteCaducidadKey(asignacion.lote, asignacion.caducidad);
            ubicacionesPorLote.computeIfAbsent(key, k -> new ArrayList<>())
                    .add(new UbicacionCompra(asignacion.ubicacion, asignacion.cantidad));
            cantidadesPorLote.merge(key, asignacion.cantidad, Integer::sum);
        }

        for (Map.Entry<LoteCaducidadKey, List<UbicacionCompra>> entry : ubicacionesPorLote.entrySet()) {
            LoteCaducidadKey key = entry.getKey();
            int cantidadItem = cantidadesPorLote.getOrDefault(key, 0);
            if (cantidadItem <= 0) {
                continue;
            }
            String caducidad = key.caducidad != null ? key.caducidad.toString() : "";
            traspasoSalida item = new traspasoSalida(
                    clave,
                    nombre,
                    descripcion,
                    key.lote,
                    caducidad,
                    cantidadItem,
                    presentacion,  // Usar presentación del modo rápido
                    factor,        // Usar factor del modo rápido
                    entry.getValue(),
                    "",
                    "",
                    "",
                    ""
            );
            item.setNota("");
            resultado.add(item);
        }

        return resultado;
    }

    private static class AsignacionRapida {
        private final String ubicacion;
        private final String lote;
        private final java.time.LocalDate caducidad;
        private final int cantidad;

        private AsignacionRapida(String ubicacion, String lote, java.time.LocalDate caducidad, int cantidad) {
            this.ubicacion = ubicacion;
            this.lote = lote != null ? lote : "";
            this.caducidad = caducidad;
            this.cantidad = cantidad;
        }
    }

    private List<UbicacionCompra> obtenerUbicacionesSeleccionadas() {
        List<UbicacionCompra> resultado = new ArrayList<>();

        for (javafx.scene.Node nodo : contenedorUbicaciones.getChildren()) {
            if (!(nodo instanceof HBox)) {
                continue;
            }
            HBox fila = (HBox) nodo;
            if (fila.getChildren().size() < 2) {
                continue;
            }

            VBox contenedorUbicacion = (VBox) fila.getChildren().get(0);
            VBox contenedorCantidad = (VBox) fila.getChildren().get(1);

            ComboBox<String> combo = null;
            TextField campoCantidad = null;

            if (contenedorUbicacion != null && !contenedorUbicacion.getChildren().isEmpty()) {
                javafx.scene.Node nodoCombo = contenedorUbicacion.getChildren().get(1);
                if (nodoCombo instanceof ComboBox) {
                    combo = (ComboBox<String>) nodoCombo;
                }
            }

            if (contenedorCantidad != null && !contenedorCantidad.getChildren().isEmpty()) {
                javafx.scene.Node nodoCantidad = contenedorCantidad.getChildren().get(1);
                if (nodoCantidad instanceof TextField) {
                    campoCantidad = (TextField) nodoCantidad;
                }
            }

            if (combo == null || campoCantidad == null) {
                continue;
            }

            String ubicacion = combo.getValue();
            String cantidadTexto = campoCantidad.getText() != null ? campoCantidad.getText().trim() : "";
            if (ubicacion == null || ubicacion.isBlank() || cantidadTexto.isBlank()) {
                continue;
            }
            int cantidad;
            try {
                cantidad = Integer.parseInt(cantidadTexto);
            } catch (NumberFormatException e) {
                continue;
            }
            if (cantidad <= 0) {
                continue;
            }
            resultado.add(new UbicacionCompra(ubicacion, cantidad));
        }

        return resultado;
    }

    private boolean existeProductoLoteEnVenta(String clave, String lote) {
        return existeProductoLoteEnVenta(clave, lote, itemParaEditar);
    }

    private boolean existeProductoLoteEnVenta(String clave, String lote, traspasoSalida itemExcluir) {
        if (clave == null || clave.isBlank() || lote == null || lote.isBlank()) {
            return false;
        }
        if (mainController != null && mainController.existeProductoLote(clave, lote, itemExcluir)) {
            return true;
        }
        if (itemsVenta == null) {
            return false;
        }
        String claveNormalizada = clave.trim();
        String loteNormalizado = lote.trim();
        return itemsVenta.stream()
                .anyMatch(item -> item != null
                        && item != itemExcluir
                        && claveNormalizada.equals(item.getClaveProducto())
                        && loteNormalizado.equals(item.getLote()));
    }

    private boolean tieneUbicacionesDuplicadas(List<UbicacionCompra> ubicacionesSeleccionadas) {
        java.util.Set<String> ubicacionesUnicas = new java.util.HashSet<>();
        for (UbicacionCompra ubicacionCompra : ubicacionesSeleccionadas) {
            if (ubicacionCompra == null || ubicacionCompra.getUbicacion() == null) {
                continue;
            }
            String ubicacion = ubicacionCompra.getUbicacion().trim();
            if (ubicacion.isBlank()) {
                continue;
            }
            if (!ubicacionesUnicas.add(ubicacion)) {
                return true;
            }
        }
        return false;
    }

    private void limpiarFormularioParaNuevo() {
        limpiarValidacionesInventario();
        seleccionarClaveAlternaPendiente = false;
        if (productoController != null) {
            productoController.limpiarSeleccion();
        }
        limpiarFormularioDependiente();
        txtPrecioSalida.clear();
        if (checkBoxIVA != null) {
            checkBoxIVA.setSelected(false);
        }
        ubicacionesCapturadas.clear();
        debounceCantidadUbicacion.clear();
        limpiarFilasAdicionales();
        Platform.runLater(() -> cbClaveProducto.requestFocus());
    }

    private void cerrarFormulario() {
        if (btnGuardar != null && btnGuardar.getScene() != null) {
            javafx.stage.Stage stage = (javafx.stage.Stage) btnGuardar.getScene().getWindow();
            if (stage != null) {
                stage.close();
            }
        }
    }

    private void limpiarFilasAdicionales() {
        if (contenedorUbicaciones == null) {
            return;
        }
        ubicacionesCapturadas.clear();
        debounceCantidadUbicacion.clear();
        while (contenedorUbicaciones.getChildren().size() > 1) {
            contenedorUbicaciones.getChildren().remove(contenedorUbicaciones.getChildren().size() - 1);
        }
        contadorFilas = 1;
        if (!contenedorUbicaciones.getChildren().isEmpty()) {
            HBox fila = (HBox) contenedorUbicaciones.getChildren().get(0);
            VBox contenedorUbicacion = (VBox) fila.getChildren().get(0);
            VBox contenedorCantidad = (VBox) fila.getChildren().get(1);
            ComboBox<String> combo = (ComboBox<String>) contenedorUbicacion.getChildren().get(1);
            TextField campoCantidad = (TextField) contenedorCantidad.getChildren().get(1);
            limpiarComboUbicacion(combo);
            campoCantidad.clear();
        }
    }

    private void limpiarComboUbicacion(ComboBox<String> comboBox) {
        if (comboBox == null) {
            return;
        }
        comboBox.setValue(null);
        if (comboBox.getEditor() != null) {
            comboBox.getEditor().clear();
        }
    }

    private void mostrarAlerta(String titulo, String mensaje) {
        Platform.runLater(() -> {
            Alert alert = new Alert(AlertType.INFORMATION);
            alert.setTitle(titulo);
            alert.setHeaderText(null);
            alert.setContentText(mensaje);
            alert.showAndWait();
        });
    }

    private void mostrarAlertaSinEspera(String titulo, String mensaje) {
        Platform.runLater(() -> {
            Alert alert = new Alert(AlertType.INFORMATION);
            alert.setTitle(titulo);
            alert.setHeaderText(null);
            alert.setContentText(mensaje);
            alert.show();

            new Thread(() -> {
                try {
                    Thread.sleep(2000);
                    if (alert.isShowing()) {
                        Platform.runLater(alert::close);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        });
    }

    private void recalcularPrecios() {
        int cantidad = parseEntero(txtCantidad.getText());
        if (cantidad <= 0) {
            txtPrecioBruto.clear();
            txtPrecioTotal.clear();
            txtPrecioIVA.clear();
            return;
        }

        BigDecimal precioSalida = parseDecimal(txtPrecioSalida.getText());
        BigDecimal precioConIva = precioSalida;
        if (checkBoxIVA != null && checkBoxIVA.isSelected()) {
            BigDecimal iva = precioSalida.multiply(IVA_TASA);
            precioConIva = precioSalida.add(iva);
        }

        BigDecimal precioBruto = precioSalida.multiply(BigDecimal.valueOf(cantidad));
        BigDecimal precioTotal = precioConIva.multiply(BigDecimal.valueOf(cantidad));

        txtPrecioIVA.setText(formatearDecimal(precioConIva));
        txtPrecioBruto.setText(formatearDecimal(precioBruto));
        txtPrecioTotal.setText(formatearDecimal(precioTotal));
    }

    private void recalcularPreciosRapido() {
        if (txtCantidadRapida == null || txtPrecioSalidaRapida == null) {
            return;
        }
        int cantidad = parseEntero(txtCantidadRapida.getText());
        if (cantidad <= 0) {
            if (txtPrecioBrutoRapida != null) {
                txtPrecioBrutoRapida.clear();
            }
            if (txtPrecioTotalRapida != null) {
                txtPrecioTotalRapida.clear();
            }
            if (txtPrecioIVARapida != null) {
                txtPrecioIVARapida.clear();
            }
            return;
        }

        BigDecimal precioSalida = parseDecimal(txtPrecioSalidaRapida.getText());
        BigDecimal precioConIva = precioSalida;
        if (checkBoxIVARapida != null && checkBoxIVARapida.isSelected()) {
            BigDecimal iva = precioSalida.multiply(IVA_TASA);
            precioConIva = precioSalida.add(iva);
        }

        BigDecimal precioBruto = precioSalida.multiply(BigDecimal.valueOf(cantidad));
        BigDecimal precioTotal = precioConIva.multiply(BigDecimal.valueOf(cantidad));

        if (txtPrecioIVARapida != null) {
            txtPrecioIVARapida.setText(formatearDecimal(precioConIva));
        }
        if (txtPrecioBrutoRapida != null) {
            txtPrecioBrutoRapida.setText(formatearDecimal(precioBruto));
        }
        if (txtPrecioTotalRapida != null) {
            txtPrecioTotalRapida.setText(formatearDecimal(precioTotal));
        }
    }

    private void actualizarPreciosPorUbicaciones() {
        recalcularPrecios();
    }

    private int parseEntero(String texto) {
        try {
            return Integer.parseInt(texto);
        } catch (Exception e) {
            return 0;
        }
    }

    private BigDecimal parseDecimal(String texto) {
        if (texto == null || texto.isBlank()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(texto.trim());
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private String formatearDecimal(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private void configurarCascada() {
        txtDescripcion.setEditable(false);
        dpCaducidad.setEditable(false);
        dpCaducidad.setMouseTransparent(true);
        dpCaducidad.setFocusTraversable(false);
        actualizarEstadoCascada();

        txtLote.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                validarCamposDesdeLote();
            }
        });

        cbPresentacion.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                validarCamposDesdePresentacion();
            }
        });

        txtCantidad.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                validarCamposDesdeCantidad();
            }
        });

        txtCantidadUbicacion.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                validarCamposDesdeCantidadUbicacion();
            }
        });

        txtFactor.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                validarCamposDesdeFactor();
            }
        });
    }

    private void configurarModoRapido() {
        if (tabPaneModo != null) {
            tabPaneModo.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
                if (newTab == tabRapido) {
                    recalcularPreciosRapido();
                } else {
                    recalcularPrecios();
                }
            });
        }
    }

    private boolean esModoRapido() {
        return tabPaneModo != null && tabRapido != null
                && tabPaneModo.getSelectionModel().getSelectedItem() == tabRapido;
    }

    private void configurarSeleccionClaveAlternaPorDefecto() {
        if (cbClaveAlterna == null) {
            return;
        }

        cbClaveAlterna.getItems().addListener((javafx.collections.ListChangeListener<String>) change -> {
            if (cbClaveAlterna.getItems().isEmpty()) {
                return;
            }
            if (!seleccionarClaveAlternaPendiente) {
                return;
            }
            seleccionarClaveAlternaPendiente = false;
            Platform.runLater(this::seleccionarPrimerClaveAlternaDisponible);
        });
    }

    private void seleccionarPrimerClaveAlternaDisponible() {
        if (cbClaveAlterna == null || cbClaveAlterna.getItems().isEmpty()) {
            return;
        }

        String primeraClave = cbClaveAlterna.getItems().stream()
                .filter(item -> item != null && !item.isBlank())
                .findFirst()
                .orElse("");

        if (primeraClave.isBlank()) {
            cbClaveAlterna.setValue("");
            return;
        }

        cbClaveAlterna.setValue(primeraClave);
    }

    private void actualizarEstadoCascada() {
        txtLote.setDisable(false);
        txtCantidad.setDisable(false);
        cbPresentacion.setDisable(false);
        txtFactor.setDisable(false);
        comboUbicacion.setDisable(false);
        txtCantidadUbicacion.setDisable(false);
    }

    private void validarLoteCompleto(String lote) {
        if (lote.isBlank()) {
            loteValidado = false;
            actualizarEstadoCascada();
            return;
        }
        String idProducto = productoController.getIdSeleccionado();
        if (idProducto == null || idProducto.isBlank()) {
            loteValidado = false;
            txtLote.clear();
            mostrarAlertaSinEspera("Advertencia", "Seleccione un producto antes de validar el lote.");
            actualizarEstadoCascada();
            return;
        }

        // Solo cargar precio de entrada si el campo está vacío
        boolean precioSalidaEstaVacio = txtPrecioSalida.getText() == null || txtPrecioSalida.getText().isBlank();

        String loteSnapshot = lote;
        String productoSnapshot = idProducto;
        javafx.concurrent.Task<ResultadoValidacionLote> task = new javafx.concurrent.Task<>() {
            @Override
            protected ResultadoValidacionLote call() {
                GenericDAO.ValidacionDisponibilidadSalida validacion =
                        modelo.validarEntradaYDisponibilidadLoteProducto(loteSnapshot, productoSnapshot);
                if (!validacion.isEntradaCompletada()) {
                    return ResultadoValidacionLote.entradaPendiente();
                }
                if (validacion.getDisponiblesSinSalida() <= 0) {
                    return ResultadoValidacionLote.salidaEnProceso();
                }
                Optional<java.time.LocalDate> caducidad = modelo.obtenerCaducidadParaLoteProducto(
                        loteSnapshot, productoSnapshot);
                return ResultadoValidacionLote.ok(caducidad.orElse(null));
            }

            @Override
            protected void succeeded() {
                String loteActual = txtLote.getText() != null ? txtLote.getText().trim() : "";
                String idActual = productoController.getIdSeleccionado();
                if (!loteSnapshot.equals(loteActual) || !productoSnapshot.equals(idActual)) {
                    return;
                }
                ResultadoValidacionLote resultado = getValue();
                if (resultado.estado == EstadoValidacionLote.ENTRADA_PENDIENTE) {
                    loteValidado = false;
                    txtLote.clear();
                    dpCaducidad.setValue(null);
                    limpiarUbicacionPrimaria();
                    mostrarAlertaSinEspera("Advertencia",
                            "El producto no esta en stock, posiblemente este en tus traspasos de entrada");
                } else if (resultado.estado == EstadoValidacionLote.SALIDA_EN_PROCESO) {
                    loteValidado = false;
                    txtLote.clear();
                    dpCaducidad.setValue(null);
                    limpiarUbicacionPrimaria();
                    mostrarAlertaSinEspera("Advertencia",
                            "El producto esta en proceso de salida a una sucursal");
                } else if (resultado.estado == EstadoValidacionLote.LOTE_INVALIDO) {
                    loteValidado = false;
                    txtLote.clear();
                    dpCaducidad.setValue(null);
                    limpiarUbicacionPrimaria();
                    mostrarAlertaSinEspera("Advertencia", "El lote no corresponde al producto seleccionado.");
                } else {
                    loteValidado = true;
                    dpCaducidad.setValue(resultado.caducidad);
                    caducidadValidada = true;

                    // SOLO cargar precio de entrada si el campo de precio de salida está vacío
                    if (precioSalidaEstaVacio) {
                        cargarPrecioEntradaDesdeProducto();
                    }
                }
                ubicacionValidada = false;
                cantidadTotalValida = false;
                presentacionValida = false;
                factorValido = false;
                actualizarEstadoCascada();
            }

            @Override
            protected void failed() {
                loteValidado = false;
            }
        };

        Thread hilo = new Thread(task);
        hilo.setDaemon(true);
        hilo.start();
    }

    private enum EstadoValidacionLote {
        OK,
        ENTRADA_PENDIENTE,
        SALIDA_EN_PROCESO,
        LOTE_INVALIDO
    }

    private static class ResultadoValidacionLote {
        private final EstadoValidacionLote estado;
        private final java.time.LocalDate caducidad;

        private ResultadoValidacionLote(EstadoValidacionLote estado, java.time.LocalDate caducidad) {
            this.estado = estado;
            this.caducidad = caducidad;
        }

        private static ResultadoValidacionLote ok(java.time.LocalDate caducidad) {
            return new ResultadoValidacionLote(EstadoValidacionLote.OK, caducidad);
        }

        private static ResultadoValidacionLote entradaPendiente() {
            return new ResultadoValidacionLote(EstadoValidacionLote.ENTRADA_PENDIENTE, null);
        }

        private static ResultadoValidacionLote salidaEnProceso() {
            return new ResultadoValidacionLote(EstadoValidacionLote.SALIDA_EN_PROCESO, null);
        }

        private static ResultadoValidacionLote loteInvalido() {
            return new ResultadoValidacionLote(EstadoValidacionLote.LOTE_INVALIDO, null);
        }
    }

    private void validarCamposDesdeLote() {
        String lote = txtLote.getText() != null ? txtLote.getText().trim() : "";
        if (!lote.isBlank()) {
            validarLoteCompleto(lote);
            ultimoLoteValidado = loteValidado ? lote : "";
        } else if (txtCantidad.getText() != null && !txtCantidad.getText().isBlank()) {
            txtCantidad.clear();
            mostrarAlertaCascada("Debe capturar el lote antes de la cantidad.");
            return;
        }
        if (!loteValidado) {
            return;
        }
        validarCantidadTotalDisponible();
    }

    private void validarCamposDesdeCantidad() {
        validarCamposDesdeLote();
        if (!loteValidado) {
            return;
        }

        String cantidadTexto = txtCantidad.getText() != null ? txtCantidad.getText().trim() : "";
        if (cantidadTexto.isBlank()) {
            return;
        }

        // Verificar que haya presentación seleccionada
        String presentacion = cbPresentacion.getValue();
        if (presentacion == null || presentacion.isBlank()) {
            txtCantidad.clear();
            mostrarAlertaCascada("Debe seleccionar una presentación antes de la cantidad.");
            return;
        }

        // Verificar que haya factor para presentaciones que no sean "pz"
        if (!presentacion.equalsIgnoreCase("pz")) {
            if (txtFactor.getText() == null || txtFactor.getText().isBlank()) {
                txtCantidad.clear();
                mostrarAlertaCascada("Debe capturar el factor antes de la cantidad para la presentación " + presentacion + ".");
                return;
            }
        }

        if (cbPresentacion.getValue() == null || cbPresentacion.getValue().isBlank()) {
            if (txtFactor.getText() != null && !txtFactor.getText().isBlank()) {
                txtFactor.clear();
                mostrarAlertaCascada("Debe capturar la presentación antes del factor.");
                return;
            }
        }

    }

    private void validarCamposDesdePresentacion() {
        validarCamposDesdeLote();
        if (!loteValidado) {
            return;
        }
        String cantidadTexto = txtCantidad.getText() != null ? txtCantidad.getText().trim() : "";

        validarPresentacion();
    }

    private void validarCamposDesdeFactor() {
        validarCamposDesdeCantidad();
        if (!cantidadTotalValida) {
            return;
        }

        String factorTexto = txtFactor.getText() != null ? txtFactor.getText().trim() : "";
        if (factorTexto.isBlank()) {
            return;
        }



        // Verificar que haya presentación seleccionada
        String presentacion = cbPresentacion.getValue();
        if (presentacion == null || presentacion.isBlank()) {
            txtFactor.clear();
            mostrarAlertaCascada("Debe seleccionar una presentación antes del factor.");
            return;
        }

        validarPresentacion();
        if (txtFactor.getText() != null && !txtFactor.getText().isBlank()) {
            validarFactorCompleto(txtFactor.getText().trim());
        }
        if ((comboUbicacion.getValue() != null && !comboUbicacion.getValue().isBlank())
                && (txtFactor.getText() == null || txtFactor.getText().isBlank())) {
            comboUbicacion.setValue(null);
            if (comboUbicacion.getEditor() != null) {
                comboUbicacion.getEditor().clear();
            }
            mostrarAlertaCascada("Debe capturar el factor antes de la ubicación.");
            return;
        }
    }

    private void validarCamposDesdeCantidadUbicacion() {
        validarCamposDesdeFactor();
        if (!factorValido) {
            return;
        }
        validarUbicacion();
        validarCantidadDisponible(txtCantidadUbicacion, comboUbicacion);
    }

    private void mostrarAlertaCascada(String mensaje) {
        mostrarAlertaSinEspera("Advertencia", mensaje);
    }

    private void validarUbicacion() {
        if (!caducidadValidada) {
            ubicacionValidada = false;
            return;
        }
        String idProducto = productoController.getIdSeleccionado();
        String lote = txtLote.getText() != null ? txtLote.getText().trim() : "";
        java.time.LocalDate caducidad = dpCaducidad.getValue();
        String ubicacion = comboUbicacion.getValue() != null ? comboUbicacion.getValue().trim() : "";
        if (idProducto == null || idProducto.isBlank() || ubicacion.isBlank()) {
            ubicacionValidada = false;
            return;
        }
        String idProductoSnapshot = idProducto;
        String loteSnapshot = lote;
        java.time.LocalDate caducidadSnapshot = caducidad;
        String ubicacionSnapshot = ubicacion;

        javafx.concurrent.Task<Boolean> task = new javafx.concurrent.Task<>() {
            @Override
            protected Boolean call() {
                boolean existe = modelo.existeLoteCaducidadUbicacionProducto(
                        idProductoSnapshot, loteSnapshot, caducidadSnapshot, ubicacionSnapshot);
                if (existe) {
                    cantidadDisponibleUbicacion = modelo.obtenerCantidadDisponibleProductoUbicacion(
                            idProductoSnapshot, loteSnapshot, caducidadSnapshot, ubicacionSnapshot);
                }
                return existe;
            }

            @Override
            protected void succeeded() {
                String idProductoActual = productoController.getIdSeleccionado();
                String loteActual = txtLote.getText() != null ? txtLote.getText().trim() : "";
                java.time.LocalDate caducidadActual = dpCaducidad.getValue();
                String ubicacionActual = comboUbicacion.getValue() != null ? comboUbicacion.getValue().trim() : "";
                if (!idProductoSnapshot.equals(idProductoActual)
                        || !loteSnapshot.equals(loteActual)
                        || !Objects.equals(caducidadSnapshot, caducidadActual)
                        || !ubicacionSnapshot.equals(ubicacionActual)) {
                    return;
                }
                boolean existe = getValue();
                if (!existe) {
                    ubicacionValidada = false;
                    comboUbicacion.setValue(null);
                    if (comboUbicacion.getEditor() != null) {
                        comboUbicacion.getEditor().clear();
                    }
                    txtCantidadUbicacion.clear();
                    mostrarAlertaSinEspera("Advertencia",
                            "No hay productos en esa ubicación para el lote y caducidad indicados.");
                } else {
                    ubicacionValidada = true;
                }
                actualizarEstadoCascada();
            }
        };

        Thread hilo = new Thread(task);
        hilo.setDaemon(true);
        hilo.start();
    }

    private void validarUbicacionParaCombo(ComboBox<String> combo, TextField campoCantidad) {
        if (combo == null) {
            return;
        }
        if (!caducidadValidada) {
            if (combo == comboUbicacion) {
                ubicacionValidada = false;
            }
            return;
        }
        String idProducto = productoController.getIdSeleccionado();
        String lote = txtLote.getText() != null ? txtLote.getText().trim() : "";
        java.time.LocalDate caducidad = dpCaducidad.getValue();
        String ubicacion = combo.getValue() != null ? combo.getValue().trim() : "";
        if (idProducto == null || idProducto.isBlank() || ubicacion.isBlank()) {
            if (combo == comboUbicacion) {
                ubicacionValidada = false;
            }
            return;
        }
        String idProductoSnapshot = idProducto;
        String loteSnapshot = lote;
        java.time.LocalDate caducidadSnapshot = caducidad;
        String ubicacionSnapshot = ubicacion;

        javafx.concurrent.Task<Boolean> task = new javafx.concurrent.Task<>() {
            @Override
            protected Boolean call() {
                boolean existe = modelo.existeLoteCaducidadUbicacionProducto(
                        idProductoSnapshot, loteSnapshot, caducidadSnapshot, ubicacionSnapshot);
                if (existe) {
                    cantidadDisponibleUbicacion = modelo.obtenerCantidadDisponibleProductoUbicacion(
                            idProductoSnapshot, loteSnapshot, caducidadSnapshot, ubicacionSnapshot);
                }
                return existe;
            }

            @Override
            protected void succeeded() {
                String idProductoActual = productoController.getIdSeleccionado();
                String loteActual = txtLote.getText() != null ? txtLote.getText().trim() : "";
                java.time.LocalDate caducidadActual = dpCaducidad.getValue();
                String ubicacionActual = combo.getValue() != null ? combo.getValue().trim() : "";
                if (!idProductoSnapshot.equals(idProductoActual)
                        || !loteSnapshot.equals(loteActual)
                        || !Objects.equals(caducidadSnapshot, caducidadActual)
                        || !ubicacionSnapshot.equals(ubicacionActual)) {
                    return;
                }
                boolean existe = getValue();
                if (!existe) {
                    if (combo == comboUbicacion) {
                        ubicacionValidada = false;
                    }
                    combo.setValue(null);
                    if (combo.getEditor() != null) {
                        combo.getEditor().clear();
                    }
                    mostrarAlertaSinEspera("Advertencia",
                            "No hay productos en esa ubicación para el lote y caducidad indicados.");
                } else if (combo == comboUbicacion) {
                    ubicacionValidada = true;
                }
                actualizarEstadoCascada();
            }
        };

        Thread hilo = new Thread(task);
        hilo.setDaemon(true);
        hilo.start();
    }

    private void validarCantidadDisponible(TextField campoCantidad, ComboBox<String> combo) {
        if (campoCantidad == null || combo == null) {
            return;
        }
        String texto = campoCantidad.getText() != null ? campoCantidad.getText().trim() : "";
        if (texto.isBlank()) {
            return;
        }
        if (combo.getValue() == null || combo.getValue().isBlank()) {
            campoCantidad.clear();
            mostrarAlerta("Advertencia", "Debe capturar la ubicación antes de la cantidad en ubicación.");
            return;
        }
        if (!loteValidado || !caducidadValidada) {
            campoCantidad.clear();
            mostrarAlerta("Advertencia", "Debe capturar un lote y caducidad válidos antes de la cantidad.");
            return;
        }

        // AHORA VALIDAMOS PRESENTACIÓN Y FACTOR TAMBIÉN
        String presentacion = cbPresentacion.getValue();
        String factorTexto = txtFactor.getText();

        if (presentacion == null || presentacion.isBlank() ||
                factorTexto == null || factorTexto.isBlank()) {
            campoCantidad.clear();
            mostrarAlerta("Advertencia", "Debe completar presentación y factor antes de la cantidad.");
            return;
        }

        int factor;
        try {
            factor = Integer.parseInt(factorTexto);
            if (factor <= 0) {
                campoCantidad.clear();
                mostrarAlerta("Advertencia", "El factor debe ser mayor a 0.");
                return;
            }
        } catch (NumberFormatException e) {
            campoCantidad.clear();
            mostrarAlerta("Error", "El factor debe ser un número válido.");
            return;
        }

        String idProducto = productoController.getIdSeleccionado();
        String lote = txtLote.getText() != null ? txtLote.getText().trim() : "";
        java.time.LocalDate caducidad = dpCaducidad.getValue();
        String ubicacion = combo.getValue() != null ? combo.getValue().trim() : "";

        if (idProducto == null || idProducto.isBlank()
                || lote.isBlank()
                || presentacion == null
                || presentacion.isBlank()
                || factor <= 0
                || ubicacion.isBlank()) {
            campoCantidad.clear();
            mostrarAlerta("Advertencia", "Debe completar todas las características del producto antes de la cantidad.");
            return;
        }

        String idProductoSnapshot = idProducto;
        String loteSnapshot = lote;
        String presentacionSnapshot = presentacion;
        int factorSnapshot = factor;
        java.time.LocalDate caducidadSnapshot = caducidad;
        String ubicacionSnapshot = ubicacion;
        String cantidadTextoSnapshot = texto;

        javafx.concurrent.Task<Integer> task = new javafx.concurrent.Task<>() {
            @Override
            protected Integer call() {
                return modelo.obtenerCantidadDisponibleDetalle(
                        idProductoSnapshot, loteSnapshot, caducidadSnapshot, presentacionSnapshot, factorSnapshot,
                        ubicacionSnapshot);
            }

            @Override
            protected void succeeded() {
                String idProductoActual = productoController.getIdSeleccionado();
                String loteActual = txtLote.getText() != null ? txtLote.getText().trim() : "";
                String presentacionActual = cbPresentacion.getValue();
                String factorActualText = txtFactor.getText();
                int factorActual = 0;
                try {
                    factorActual = factorActualText != null ? Integer.parseInt(factorActualText) : 0;
                } catch (NumberFormatException e) {
                    factorActual = 0;
                }
                java.time.LocalDate caducidadActual = dpCaducidad.getValue();
                String ubicacionActual = combo.getValue() != null ? combo.getValue().trim() : "";
                String cantidadActual = campoCantidad.getText() != null ? campoCantidad.getText().trim() : "";

                if (!idProductoSnapshot.equals(idProductoActual)
                        || !loteSnapshot.equals(loteActual)
                        || !presentacionSnapshot.equals(presentacionActual)
                        || factorSnapshot != factorActual
                        || !Objects.equals(caducidadSnapshot, caducidadActual)
                        || !ubicacionSnapshot.equals(ubicacionActual)
                        || !cantidadTextoSnapshot.equals(cantidadActual)) {
                    return;
                }

                cantidadDisponibleUbicacion = getValue();
                if (cantidadDisponibleUbicacion <= 0) {
                    campoCantidad.clear();
                    mostrarAlerta("Advertencia",
                            "No hay existencia en esa ubicación con las características indicadas (lote, presentación y factor).");
                    return;
                }

                int cantidad = parseEntero(cantidadActual);
                if (cantidad <= 0) {
                    campoCantidad.clear();
                    mostrarAlerta("Advertencia", "La cantidad debe ser mayor a 0.");
                    return;
                }
                if (cantidad > cantidadDisponibleUbicacion) {
                    campoCantidad.clear();
                    mostrarAlerta("Advertencia", "La cantidad supera la disponible en esa ubicación.");
                    return;
                }
                registrarCantidadUbicacion(combo, cantidad);
            }
        };

        Thread hilo = new Thread(task);
        hilo.setDaemon(true);
        hilo.start();
    }

    private void programarValidacionLote(String nuevoValor) {
        loteDebounce.stop();
        if (nuevoValor == null || nuevoValor.isBlank()) {
            ultimoLoteValidado = "";
            return;
        }
        loteDebounce.setOnFinished(event -> {
            String loteActual = txtLote.getText() != null ? txtLote.getText().trim() : "";
            if (loteActual.isBlank()) {
                return;
            }
            if (loteActual.equals(ultimoLoteValidado) && loteValidado) {
                return;
            }
            validarLoteCompleto(loteActual);
            if (loteValidado) {
                ultimoLoteValidado = loteActual;
            }
        });
        loteDebounce.playFromStart();
    }

    private void programarValidacionCantidadRapida() {
        if (txtCantidadRapida == null) {
            return;
        }

        cantidadRapidaDebounce.stop();
        String nuevoValor = txtCantidadRapida.getText();

        if (nuevoValor == null || nuevoValor.isBlank()) {
            cantidadRapidaValida = false;
            return;
        }

        // Verificar que haya producto seleccionado
        String idProducto = productoController.getIdSeleccionado();
        if (idProducto == null || idProducto.isBlank()) {
            txtCantidadRapida.clear();
            mostrarAlertaSinEspera("Advertencia", "Seleccione un producto antes de capturar la cantidad.");
            return;
        }

        // Verificar que haya presentación seleccionada
        String presentacion = cbPresentacionRapida != null ? cbPresentacionRapida.getValue() : null;
        if (presentacion == null || presentacion.isBlank()) {
            txtCantidadRapida.clear();
            mostrarAlertaSinEspera("Advertencia", "Seleccione una presentación antes de capturar la cantidad.");
            return;
        }

        // Verificar que haya factor para presentaciones que no sean "pz"
        if (!presentacion.equalsIgnoreCase("pz")) {
            if (txtFactorRapido == null || txtFactorRapido.getText() == null || txtFactorRapido.getText().isBlank()) {
                txtCantidadRapida.clear();
                mostrarAlertaSinEspera("Advertencia",
                        "Debe capturar el factor antes de la cantidad para la presentación " + presentacion + ".");
                return;
            }
        }

        cantidadRapidaDebounce.setOnFinished(event -> validarCantidadRapidaDisponible());
        cantidadRapidaDebounce.playFromStart();
    }

    private void validarCantidadRapidaDisponible() {
        if (txtCantidadRapida == null) {
            return;
        }
        String cantidadTexto = txtCantidadRapida.getText() != null ? txtCantidadRapida.getText().trim() : "";
        if (cantidadTexto.isBlank()) {
            cantidadRapidaValida = false;
            return;
        }

        // Obtener presentación y factor del modo rápido
        String presentacionRapida = "pz"; // Valor por defecto
        int factorRapido = 1; // Valor por defecto

        if (cbPresentacionRapida != null && cbPresentacionRapida.getValue() != null) {
            presentacionRapida = cbPresentacionRapida.getValue().trim();
        }

        if (txtFactorRapido != null && txtFactorRapido.getText() != null && !txtFactorRapido.getText().isBlank()) {
            try {
                factorRapido = Integer.parseInt(txtFactorRapido.getText().trim());
                if (factorRapido <= 0) {
                    cantidadRapidaValida = false;
                    mostrarAlertaSinEspera("Advertencia", "El factor debe ser mayor a 0.");
                    return;
                }
            } catch (NumberFormatException e) {
                cantidadRapidaValida = false;
                mostrarAlertaSinEspera("Advertencia", "El factor debe ser un número válido.");
                return;
            }
        } else if (cbPresentacionRapida != null && cbPresentacionRapida.getValue() != null
                && !cbPresentacionRapida.getValue().equalsIgnoreCase("pz")) {
            // Si no es "pz" y no tiene factor, mostrar error
            cantidadRapidaValida = false;
            mostrarAlertaSinEspera("Advertencia", "Debe especificar un factor para la presentación seleccionada.");
            return;
        }

        int cantidad;
        try {
            cantidad = Integer.parseInt(cantidadTexto);
        } catch (NumberFormatException e) {
            cantidadRapidaValida = false;
            mostrarAlertaSinEspera("Advertencia", "La cantidad debe ser un número válido.");
            return;
        }
        if (cantidad <= 0) {
            cantidadRapidaValida = false;
            mostrarAlertaSinEspera("Advertencia", "La cantidad debe ser mayor a 0.");
            return;
        }
        String idProducto = productoController.getIdSeleccionado();
        if (idProducto == null || idProducto.isBlank()) {
            cantidadRapidaValida = false;
            mostrarAlertaSinEspera("Advertencia", "Seleccione un producto antes de la cantidad.");
            return;
        }

        String idSnapshot = idProducto;
        int cantidadSnapshot = cantidad;
        String presentacionSnapshot = presentacionRapida;
        int factorSnapshot = factorRapido;

        javafx.concurrent.Task<Integer> task = new javafx.concurrent.Task<>() {
            @Override
            protected Integer call() {
                return modelo.obtenerCantidadDisponibleProductoPresentacionFactor(
                        idSnapshot, presentacionSnapshot, factorSnapshot);
            }

            @Override
            protected void succeeded() {
                String textoActual = txtCantidadRapida.getText() != null ? txtCantidadRapida.getText().trim() : "";
                String presentacionActual = cbPresentacionRapida != null && cbPresentacionRapida.getValue() != null
                        ? cbPresentacionRapida.getValue().trim() : "pz";
                String factorActual = txtFactorRapido != null && txtFactorRapido.getText() != null
                        ? txtFactorRapido.getText().trim() : "1";
                int factorNum = 1;
                try {
                    factorNum = Integer.parseInt(factorActual);
                } catch (NumberFormatException e) {
                    factorNum = 1;
                }

                if (!textoActual.equals(String.valueOf(cantidadSnapshot))
                        || !presentacionActual.equals(presentacionSnapshot)
                        || factorNum != factorSnapshot) {
                    return;
                }
                int disponible = getValue() != null ? getValue() : 0;
                if (cantidadSnapshot > disponible) {
                    cantidadRapidaValida = false;
                    mostrarAlertaSinEspera("Advertencia",
                            "La cantidad supera la disponible para la presentación " + presentacionSnapshot
                                    + " con factor " + factorSnapshot + ".");
                } else {
                    cantidadRapidaValida = true;
                }
            }
        };

        Thread hilo = new Thread(task);
        hilo.setDaemon(true);
        hilo.start();
    }

    private void programarValidacionCantidadUbicacion(TextField campoCantidad, ComboBox<String> combo) {
        PauseTransition debounce = debounceCantidadUbicacion.computeIfAbsent(campoCantidad,
                key -> new PauseTransition(DEBOUNCE_TIEMPO));
        debounce.stop();
        String nuevoValor = campoCantidad.getText();
        if (nuevoValor == null || nuevoValor.isBlank()) {
            ultimaCantidadUbicacionValidada.remove(campoCantidad);
            return;
        }
        if (combo.getValue() == null || combo.getValue().isBlank()) {
            campoCantidad.clear();
            mostrarAlertaCascada("Debe capturar la ubicación antes de la cantidad en ubicación.");
            return;
        }
        debounce.setOnFinished(event -> {
            String cantidadActual = campoCantidad.getText() != null
                    ? campoCantidad.getText().trim()
                    : "";
            if (cantidadActual.isBlank()) {
                ultimaCantidadUbicacionValidada.remove(campoCantidad);
                return;
            }
            if (cantidadActual.equals(ultimaCantidadUbicacionValidada.get(campoCantidad))) {
                return;
            }
            validarCantidadDisponible(campoCantidad, combo);
            actualizarEstadoCascada();
            ultimaCantidadUbicacionValidada.put(campoCantidad, cantidadActual);
        });
        debounce.playFromStart();
    }

    private void programarValidacionFactor(String nuevoValor) {
        factorDebounce.stop();
        if (nuevoValor == null || nuevoValor.isBlank()) {
            ultimoFactorValidado = "";
            return;
        }
        if (cbPresentacion.getValue() == null || cbPresentacion.getValue().isBlank()) {
            txtFactor.clear();
            mostrarAlertaCascada("Debe capturar la presentación antes del factor.");
            return;
        }

        factorDebounce.setOnFinished(event -> {
            String factorActual = txtFactor.getText() != null ? txtFactor.getText().trim() : "";
            if (factorActual.isBlank()) {
                ultimoFactorValidado = "";
                return;
            }
            if (factorActual.equals(ultimoFactorValidado)) {
                return;
            }
            validarFactorCompleto(factorActual);
            if (factorValido) {
                ultimoFactorValidado = factorActual;
            }
        });
        factorDebounce.playFromStart();
    }

    private void validarCantidadTotalDisponible() {
        if (!caducidadValidada) {
            cantidadTotalValida = false;
            return;
        }
        String texto = txtCantidad.getText() != null ? txtCantidad.getText().trim() : "";
        if (texto.isBlank()) {
            cantidadTotalValida = false;
            return;
        }
        int cantidad = parseEntero(texto);
        if (cantidad <= 0) {
            cantidadTotalValida = false;
            return;
        }

        // AHORA VALIDAMOS PRESENTACIÓN Y FACTOR TAMBIÉN
        String presentacion = cbPresentacion.getValue();
        String factorTexto = txtFactor.getText();

        if (presentacion == null || presentacion.isBlank() ||
                factorTexto == null || factorTexto.isBlank()) {
            cantidadTotalValida = false;
            return;
        }

        int factor;
        try {
            factor = Integer.parseInt(factorTexto);
            if (factor <= 0) {
                txtCantidad.clear();
                cantidadTotalValida = false;
                mostrarAlertaSinEspera("Advertencia", "El factor debe ser mayor a 0.");
                return;
            }
        } catch (NumberFormatException e) {
            txtCantidad.clear();
            cantidadTotalValida = false;
            mostrarAlertaSinEspera("Error", "El factor debe ser un número válido.");
            return;
        }

        String lote = txtLote.getText() != null ? txtLote.getText().trim() : "";
        String idProducto = productoController.getIdSeleccionado();
        if (idProducto == null || idProducto.isBlank()) {
            cantidadTotalValida = false;
            return;
        }

        String loteSnapshot = lote;
        String idSnapshot = idProducto;
        java.time.LocalDate caducidadSnapshot = dpCaducidad.getValue();
        String presentacionSnapshot = presentacion;
        int factorSnapshot = factor;
        int cantidadSnapshot = cantidad;

        javafx.concurrent.Task<Integer> task = new javafx.concurrent.Task<>() {
            @Override
            protected Integer call() {
                // Necesitamos una nueva consulta que incluya presentación y factor
                return modelo.obtenerCantidadDisponibleProductoLoteCaducidadPresentacionFactor(
                        idSnapshot, loteSnapshot, caducidadSnapshot, presentacionSnapshot, factorSnapshot);
            }

            @Override
            protected void succeeded() {
                String loteActual = txtLote.getText() != null ? txtLote.getText().trim() : "";
                String idActual = productoController.getIdSeleccionado();
                java.time.LocalDate caducidadActual = dpCaducidad.getValue();
                String presentacionActual = cbPresentacion.getValue();
                String factorActualText = txtFactor.getText();
                int factorActual = 0;
                try {
                    factorActual = factorActualText != null ? Integer.parseInt(factorActualText) : 0;
                } catch (NumberFormatException e) {
                    factorActual = 0;
                }
                int cantidadActual = parseEntero(txtCantidad.getText());

                if (!loteSnapshot.equals(loteActual)
                        || !idSnapshot.equals(idActual)
                        || !Objects.equals(caducidadSnapshot, caducidadActual)
                        || !presentacionSnapshot.equals(presentacionActual)
                        || factorSnapshot != factorActual
                        || cantidadActual != cantidadSnapshot) {
                    return;
                }

                int disponible = getValue();
                if (cantidadSnapshot > disponible) {
                    txtCantidad.clear();
                    cantidadTotalValida = false;
                    mostrarAlertaSinEspera("Advertencia",
                            "La cantidad supera la disponible para el lote, presentación y factor seleccionados.");
                } else {
                    cantidadTotalValida = true;
                    actualizarPreciosPorUbicaciones();
                }
            }
        };

        Thread hilo = new Thread(task);
        hilo.setDaemon(true);
        hilo.start();
    }

    private void validarPresentacion() {
        String presentacion = cbPresentacion.getValue();
        if (!loteValidado || !caducidadValidada) {
            presentacionValida = false;
            if (presentacion != null && !presentacion.isBlank()) {
                cbPresentacion.setValue(null);
                mostrarAlertaSinEspera("Advertencia", "Debe capturar un lote válido antes de la presentación.");
            }
            return;
        }
        String cantidadTexto = txtCantidad.getText() != null ? txtCantidad.getText().trim() : "";

        String lote = txtLote.getText() != null ? txtLote.getText().trim() : "";
        String idProducto = productoController.getIdSeleccionado();
        if (presentacion == null || presentacion.isBlank() || idProducto == null || idProducto.isBlank()) {
            presentacionValida = false;
            return;
        }
        String presentacionSnapshot = presentacion;
        String loteSnapshot = lote;
        String idProductoSnapshot = idProducto;
        javafx.concurrent.Task<Boolean> task = new javafx.concurrent.Task<>() {
            @Override
            protected Boolean call() {
                return modelo.existePresentacionParaProductoLote(idProductoSnapshot, loteSnapshot, presentacionSnapshot);
            }

            @Override
            protected void succeeded() {
                String presentacionActual = cbPresentacion.getValue();
                String loteActual = txtLote.getText() != null ? txtLote.getText().trim() : "";
                String idProductoActual = productoController.getIdSeleccionado();
                if (!presentacionSnapshot.equals(presentacionActual)
                        || !loteSnapshot.equals(loteActual)
                        || !idProductoSnapshot.equals(idProductoActual)) {
                    return;
                }
                boolean existe = getValue();
                if (!existe) {
                    presentacionValida = false;
                    cbPresentacion.setValue(null);
                    mostrarAlertaSinEspera("Advertencia",
                            "La presentación no existe para el lote y producto seleccionados.");
                } else {
                    presentacionValida = true;
                    ultimaPresentacionValidada = presentacionSnapshot;
                }
                factorValido = false;
            }
        };

        Thread hilo = new Thread(task);
        hilo.setDaemon(true);
        hilo.start();
    }

    private void validarFactorCompleto(String factorTexto) {
        String presentacion = cbPresentacion.getValue();
        if (presentacion == null || presentacion.isBlank()) {
            factorValido = false;
            if (!factorTexto.isBlank()) {
                txtFactor.clear();
                mostrarAlertaSinEspera("Advertencia", "Debe capturar la presentación antes del factor.");
            }
            return;
        }

        // Si es "pz" y tiene factor 1, validar automáticamente
        if (presentacion.equalsIgnoreCase("pz") && "1".equals(factorTexto)) {
            factorValido = true;
            ultimoFactorValidado = "1";
            return;
        }

        validarPresentacion();
        if (!presentacionValida) {
            factorValido = false;
            return;
        }
        if (!presentacion.equals(ultimaPresentacionValidada)) {
            factorValido = false;
            txtFactor.clear();
            mostrarAlerta("Advertencia", "Seleccione la presentación válida antes de capturar el factor.");
            return;
        }
        if (factorTexto.isBlank()) {
            factorValido = false;
            return;
        }
        int factor = parseEntero(factorTexto);
        if (factor <= 0) {
            factorValido = false;
            txtFactor.clear();
            mostrarAlerta("Advertencia", "El factor debe ser un número mayor a 0.");
            return;
        }
        String lote = txtLote.getText() != null ? txtLote.getText().trim() : "";
        String idProducto = productoController.getIdSeleccionado();
        if (idProducto == null || idProducto.isBlank() || presentacion == null || presentacion.isBlank()) {
            factorValido = false;
            return;
        }
        String presentacionSnapshot = presentacion;
        String loteSnapshot = lote;
        String idProductoSnapshot = idProducto;
        int factorSnapshot = factor;
        String factorTextoSnapshot = factorTexto;
        javafx.concurrent.Task<Boolean> task = new javafx.concurrent.Task<>() {
            @Override
            protected Boolean call() {
                return modelo.existeFactorParaProductoLotePresentacion(
                        idProductoSnapshot, loteSnapshot, presentacionSnapshot, factorSnapshot);
            }

            @Override
            protected void succeeded() {
                String presentacionActual = cbPresentacion.getValue();
                String loteActual = txtLote.getText() != null ? txtLote.getText().trim() : "";
                String idProductoActual = productoController.getIdSeleccionado();
                String factorActual = txtFactor.getText() != null ? txtFactor.getText().trim() : "";
                if (!presentacionSnapshot.equals(presentacionActual)
                        || !loteSnapshot.equals(loteActual)
                        || !idProductoSnapshot.equals(idProductoActual)
                        || !factorTextoSnapshot.equals(factorActual)) {
                    return;
                }
                boolean existe = getValue();
                if (!existe) {
                    factorValido = false;
                    txtFactor.clear();
                    mostrarAlertaSinEspera("Advertencia",
                            "El factor no corresponde con la presentación y lote seleccionados.");
                } else {
                    factorValido = true;
                    ultimoFactorValidado = factorTextoSnapshot;
                }
            }
        };

        Thread hilo = new Thread(task);
        hilo.setDaemon(true);
        hilo.start();
    }

    private boolean datosCompletosParaPrecioEntrada() {
        return productoController.getIdSeleccionado() != null
                && !productoController.getIdSeleccionado().isBlank()
                && loteValidado
                && caducidadValidada;
    }

    private void limpiarValidacionesInventario() {
        loteValidado = false;
        caducidadValidada = false;
        presentacionValida = false;
        factorValido = false;
        ubicacionValidada = false;
        cantidadDisponibleUbicacion = 0;
        cantidadTotalValida = false;
        cantidadRapidaValida = false;
        ultimoLoteValidado = "";
        ultimoFactorValidado = "";
        ultimaPresentacionValidada = "";
        ultimaCantidadUbicacionValidada.clear();

        // Restaurar campo factor a editable
        txtFactor.setEditable(true);
        txtFactor.setStyle("");
        txtFactor.clear();

        // También limpiar campos del modo rápido
        if (cbPresentacionRapida != null) {
            cbPresentacionRapida.setValue(null);
        }
        if (txtFactorRapido != null) {
            txtFactorRapido.clear();
            txtFactorRapido.setEditable(true);
            txtFactorRapido.setStyle("");
        }
        if (txtCantidadRapida != null) {
            txtCantidadRapida.clear();
        }
    }

    private void configurarAutocompletadoUbicacion(ComboBox<String> comboBox) {
        if (comboBox == null) {
            return;
        }
        comboBox.setItems(ubicaciones);
        comboBox.setEditable(false);
        if (comboBox.isEditable()) {
            new AutoCompleteComboBoxListener<>(comboBox);
        }

        if (comboBox.isEditable()) {
            comboBox.focusedProperty().addListener((obs, oldVal, newVal) -> {
                if (!newVal) {
                    validarTextoUbicacion(comboBox);
                }
            });
        }

        comboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (comboBox.isEditable() && newVal != null && !newVal.isBlank()) {
                comboBox.getEditor().setText(newVal);
            }
            if (oldVal != null && !oldVal.equals(newVal)) {
                limpiarCapturasCombo(comboBox);
            }
        });
    }

    private void validarTextoUbicacion(ComboBox<String> comboBox) {
        String valor = comboBox.getEditor() != null ? comboBox.getEditor().getText() : null;
        if (valor == null || valor.isBlank()) {
            return;
        }
        if (!ubicaciones.contains(valor)) {
            comboBox.setValue(null);
            if (comboBox.getEditor() != null) {
                comboBox.getEditor().clear();
            }
            mostrarAlerta("Advertencia", "La ubicación no existe. Seleccione una válida.");
        }
    }

    private void limpiarUbicacionPrimaria() {
        if (comboUbicacion != null) {
            comboUbicacion.setValue(null);
            if (comboUbicacion.getEditor() != null) {
                comboUbicacion.getEditor().clear();
            }
        }
        if (txtCantidadUbicacion != null) {
            txtCantidadUbicacion.clear();
        }
        ultimaCantidadUbicacionValidada.clear();
        limpiarCapturasCombo(comboUbicacion);
    }

    private void configurarCampoCantidadUbicacion(TextField campoCantidad, ComboBox<String> combo) {
        if (campoCantidad == null || combo == null) {
            return;
        }
        validarNumerosEnteros(campoCantidad);
        campoCantidad.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.isBlank()
                    && (combo.getValue() == null || combo.getValue().isBlank())) {
                campoCantidad.clear();
                mostrarAlertaCascada("Debe capturar la ubicación antes de la cantidad en ubicación.");
                return;
            }
            programarValidacionCantidadUbicacion(campoCantidad, combo);
        });
    }

    private void configurarComboUbicacion(ComboBox<String> combo, TextField campoCantidad) {
        if (combo == null) {
            return;
        }
        combo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.isBlank()
                    && (txtFactor.getText() == null || txtFactor.getText().isBlank())) {
                combo.setValue(null);
                if (combo.getEditor() != null) {
                    combo.getEditor().clear();
                }
                if (campoCantidad != null) {
                    campoCantidad.clear();
                }
                mostrarAlertaCascada("Debe capturar el factor antes de la ubicación.");
                return;
            }
            if (newVal != null && !newVal.isBlank() && ubicacionDuplicada(combo, newVal)) {
                combo.setValue(null);
                if (combo.getEditor() != null) {
                    combo.getEditor().clear();
                }
                mostrarAlertaCascada("No se puede seleccionar la misma ubicación más de una vez.");
                return;
            }
            limpiarCapturasCombo(combo);
            if (combo == comboUbicacion) {
                validarUbicacion();
            } else {
                validarUbicacionParaCombo(combo, campoCantidad);
            }
            actualizarEstadoCascada();
        });
    }

    private void registrarCantidadUbicacion(ComboBox<String> combo, int cantidad) {
        String ubicacion = combo.getValue();
        if (ubicacion == null || ubicacion.isBlank() || cantidad <= 0) {
            return;
        }
        List<UbicacionCompra> lista = ubicacionesCapturadas.computeIfAbsent(combo, key -> new ArrayList<>());
        lista.add(new UbicacionCompra(ubicacion, cantidad));
    }

    private void limpiarCapturasCombo(ComboBox<String> combo) {
        if (combo == null) {
            return;
        }
        List<UbicacionCompra> lista = ubicacionesCapturadas.get(combo);
        if (lista != null) {
            lista.clear();
        }
    }

    private boolean ubicacionDuplicada(ComboBox<String> comboActual, String ubicacion) {
        if (ubicacion == null || ubicacion.isBlank()) {
            return false;
        }
        String ubicacionNormalizada = ubicacion.trim();
        for (javafx.scene.Node nodo : contenedorUbicaciones.getChildren()) {
            if (!(nodo instanceof HBox)) {
                continue;
            }
            HBox fila = (HBox) nodo;
            if (fila.getChildren().isEmpty()) {
                continue;
            }
            VBox contenedorUbicacion = (VBox) fila.getChildren().get(0);
            if (contenedorUbicacion.getChildren().size() < 2) {
                continue;
            }
            ComboBox<String> combo = (ComboBox<String>) contenedorUbicacion.getChildren().get(1);
            if (combo == null || combo == comboActual) {
                continue;
            }
            String valor = combo.getValue();
            if (valor != null && !valor.isBlank() && ubicacionNormalizada.equals(valor.trim())) {
                return true;
            }
        }
        return false;
    }

    private void configurarCalculoPrecios() {
        txtCantidad.textProperty().addListener((obs, oldVal, newVal) -> {
            validarCantidadTotalDisponible();
            recalcularPrecios();
            actualizarEstadoCascada();
        });

        txtPrecioSalida.textProperty().addListener((obs, oldVal, newVal) -> recalcularPrecios());

        if (checkBoxIVA != null) {
            checkBoxIVA.selectedProperty().addListener((obs, oldVal, newVal) -> recalcularPrecios());
        }

        if (txtCantidadRapida != null) {
            txtCantidadRapida.textProperty().addListener((obs, oldVal, newVal) -> {
                programarValidacionCantidadRapida();
                recalcularPreciosRapido();
            });
        }

        if (txtPrecioSalidaRapida != null) {
            txtPrecioSalidaRapida.textProperty().addListener((obs, oldVal, newVal) -> recalcularPreciosRapido());
        }

        if (checkBoxIVARapida != null) {
            checkBoxIVARapida.selectedProperty().addListener((obs, oldVal, newVal) -> recalcularPreciosRapido());
        }

        txtFactor.textProperty().addListener((obs, oldVal, newVal) -> {
            programarValidacionFactor(newVal);
        });
    }

    private void configurarCamposLectura() {
        txtPrecioEntrada.setEditable(false);
        if (txtPrecioEntradaIva != null) {
            txtPrecioEntradaIva.setEditable(false);
        }
        if (txtPrecioEntradaRapida != null) {
            txtPrecioEntradaRapida.setEditable(false);
        }
        if (txtPrecioEntradaIvaRapida != null) {
            txtPrecioEntradaIvaRapida.setEditable(false);
        }
        txtPrecioIVA.setEditable(false);
        txtPrecioBruto.setEditable(false);
        txtPrecioTotal.setEditable(false);
        if (txtPrecioIVARapida != null) {
            txtPrecioIVARapida.setEditable(false);
        }
        if (txtPrecioBrutoRapida != null) {
            txtPrecioBrutoRapida.setEditable(false);
        }
        if (txtPrecioTotalRapida != null) {
            txtPrecioTotalRapida.setEditable(false);
        }
    }

    private void configurarManejoEnter() {
        cbClaveProducto.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                cbProductoNombre.requestFocus();
                event.consume();
            }
        });

        cbProductoNombre.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                cbClaveAlterna.requestFocus();
                event.consume();
            }
        });

        cbClaveAlterna.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                txtCantidad.requestFocus();
                event.consume();
            }
        });

        txtCantidad.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                cbPresentacion.requestFocus();
                event.consume();
            }
        });

        cbPresentacion.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                txtFactor.requestFocus();
                event.consume();
            }
        });

        txtFactor.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                guardarItem();
                event.consume();
            }
        });

        txtLote.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                dpCaducidad.requestFocus();
                event.consume();
            }
        });

        dpCaducidad.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                txtCantidad.requestFocus();
                event.consume();
            }
        });

        txtCantidadUbicacion.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                guardarItem();
                event.consume();
            }
        });
    }

    private void configurarValidaciones() {
        validarNumerosEnteros(txtCantidad);
        validarNumerosEnteros(txtFactor);
        validarNumerosEnteros(txtCantidadUbicacion);
        if (txtCantidadRapida != null) {
            validarNumerosEnteros(txtCantidadRapida);
        }
        // NUEVO: Validar factor rápido
        if (txtFactorRapido != null) {
            validarNumerosEnteros(txtFactorRapido);
        }
    }

    private void validarNumerosEnteros(TextField campo) {
        campo.textProperty().addListener((obs, old, val) -> {
            if (!val.matches("\\d*")) {
                campo.setText(val.replaceAll("[^\\d]", ""));
            }
        });
    }

    private void agregarUbicacionCombo() {
        if (contadorFilas >= MAX_FILAS) {
            mostrarAlerta("Límite alcanzado", "Solo se pueden agregar hasta " + MAX_FILAS + " ubicaciones.");
            return;
        }

        HBox nuevaFila = new HBox(20);

        VBox vboxUbicacion = new VBox(5);
        ComboBox<String> nuevoCombo = new ComboBox<>(ubicaciones);
        nuevoCombo.setEditable(false);
        nuevoCombo.setPromptText("Selecciona una ubicación");
        vboxUbicacion.getChildren().addAll(new javafx.scene.control.Label("Ubicación:"), nuevoCombo);
        HBox.setHgrow(vboxUbicacion, Priority.ALWAYS);

        VBox vboxCantidad = new VBox(5);
        TextField txtCantidad = new TextField();
        vboxCantidad.getChildren().addAll(new javafx.scene.control.Label("Cantidad en ubicación:"), txtCantidad);
        HBox.setHgrow(vboxCantidad, Priority.ALWAYS);

        VBox vboxBoton = new VBox(5);
        Button botonEliminar = new Button();
        String styleV = "-fx-background-color: #d3d3d3; -fx-border-color: #999; -fx-font-weight: bold; -fx-cursor: hand; -fx-border-radius: 5;  -fx-max-width: 25; -fx-max-height: 25; -fx-background-radius: 5; -fx-text-fill: black;";
        botonEliminar.setStyle(styleV);
        botonEliminar.setText("-");
        vboxBoton.setAlignment(Pos.BOTTOM_CENTER);
        vboxBoton.getChildren().addAll(botonEliminar);
        HBox.setHgrow(vboxBoton, Priority.ALWAYS);
        botonEliminar.setOnAction(this::manejarEliminar);

        nuevaFila.getChildren().addAll(vboxUbicacion, vboxCantidad, vboxBoton);
        contenedorUbicaciones.getChildren().add(nuevaFila);

        configurarCampoCantidadUbicacion(txtCantidad, nuevoCombo);
        configurarComboUbicacion(nuevoCombo, txtCantidad);
        ubicacionesCapturadas.put(nuevoCombo, new ArrayList<>());

        contadorFilas++;
    }

    @FXML
    private void agregarUbicacion() {
        agregarUbicacionCombo();
    }

    private void manejarEliminar(ActionEvent event) {
        Button botonPresionado = (Button) event.getSource();
        VBox contenedorBoton = (VBox) botonPresionado.getParent();
        HBox fila = (HBox) contenedorBoton.getParent();

        ComboBox<String> combo = null;
        TextField campoCantidad = null;
        if (fila.getChildren().size() >= 2) {
            VBox contenedorUbicacion = (VBox) fila.getChildren().get(0);
            VBox contenedorCantidad = (VBox) fila.getChildren().get(1);
            combo = (ComboBox<String>) contenedorUbicacion.getChildren().get(1);
            campoCantidad = (TextField) contenedorCantidad.getChildren().get(1);
        }
        if (combo != null) {
            ubicacionesCapturadas.remove(combo);
        }
        if (campoCantidad != null) {
            ultimaCantidadUbicacionValidada.remove(campoCantidad);
        }

        contenedorUbicaciones.getChildren().remove(fila);
        contadorFilas--;
    }

    public void setItemsVenta(ObservableList<traspasoSalida> itemsVenta) {
        this.itemsVenta = itemsVenta;
    }

    public void setMainController(Operaciones.venta.controller.MainController mainController) {
        this.mainController = mainController;
    }

    public void setTituloFormulario(String tituloFormulario) {
        if (tituloFormulario == null || tituloFormulario.isBlank()) {
            return;
        }
        this.tituloFormulario = tituloFormulario;
        if (lblTitulo != null) {
            lblTitulo.setText(tituloFormulario);
        }
    }

    public void setItemParaEditar(traspasoSalida item) {
        this.itemParaEditar = item;
        if (itemParaEditar != null && inicializado) {
            cargarItemParaEditar();
        }
    }

    private void cargarItemParaEditar() {
        if (itemParaEditar == null) {
            return;
        }

        limpiarFormularioParaNuevo();
        cbClaveProducto.setValue(itemParaEditar.getClaveProducto());
        cbProductoNombre.setValue(itemParaEditar.getProducto());
        txtDescripcion.setText(itemParaEditar.getDescripcion());
        if (txtNota != null) {
            txtNota.setText(itemParaEditar.getNota());
        }
        txtLote.setText(itemParaEditar.getLote());
        configurarCaducidadDesdeTexto(itemParaEditar.getCaducidad());
        loteValidado = true;
        caducidadValidada = true;
        txtCantidad.setText(String.valueOf(itemParaEditar.getCantidad()));
        cbPresentacion.setValue(itemParaEditar.getPresentacion());
        txtFactor.setText(String.valueOf(itemParaEditar.getFactor()));
        presentacionValida = true;
        factorValido = true;
        cargarPrecioEntradaDesdeProducto();

        String precioSalida = itemParaEditar.getPrecioEntrada();
        txtPrecioSalida.setText(precioSalida);
        txtPrecioIVA.setText(itemParaEditar.getPrecioIva());
        txtPrecioBruto.setText(itemParaEditar.getPrecioBruto());
        txtPrecioTotal.setText(itemParaEditar.getPrecioTotal());
        if (checkBoxIVA != null) {
            BigDecimal base = parseDecimal(precioSalida);
            BigDecimal conIva = parseDecimal(itemParaEditar.getPrecioIva());
            checkBoxIVA.setSelected(conIva.compareTo(base) > 0);
        }
        cargarUbicacionesParaEdicion(itemParaEditar.getUbicaciones());
        recalcularPrecios();
    }

    private void cargarUbicacionesParaEdicion(List<UbicacionCompra> ubicacionesLista) {
        limpiarFilasAdicionales();
        ubicacionesCapturadas.clear();
        ultimaCantidadUbicacionValidada.clear();
        if (ubicacionesLista == null || ubicacionesLista.isEmpty()) {
            return;
        }
        for (int i = 0; i < ubicacionesLista.size(); i++) {
            UbicacionCompra ubicacion = ubicacionesLista.get(i);
            if (i > 0) {
                agregarUbicacionCombo();
            }
            HBox fila = (HBox) contenedorUbicaciones.getChildren().get(i);
            VBox contenedorUbicacion = (VBox) fila.getChildren().get(0);
            VBox contenedorCantidad = (VBox) fila.getChildren().get(1);
            ComboBox<String> combo = (ComboBox<String>) contenedorUbicacion.getChildren().get(1);
            TextField campoCantidad = (TextField) contenedorCantidad.getChildren().get(1);
            combo.setValue(ubicacion.getUbicacion());
            if (combo.getEditor() != null) {
                combo.getEditor().setText(ubicacion.getUbicacion());
            }
            campoCantidad.setText(String.valueOf(ubicacion.getCantidad()));
        }
    }

    private static class LoteCaducidadKey {
        private final String lote;
        private final java.time.LocalDate caducidad;

        private LoteCaducidadKey(String lote, java.time.LocalDate caducidad) {
            this.lote = lote != null ? lote : "";
            this.caducidad = caducidad;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            LoteCaducidadKey other = (LoteCaducidadKey) obj;
            return java.util.Objects.equals(lote, other.lote)
                    && java.util.Objects.equals(caducidad, other.caducidad);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(lote, caducidad);
        }
    }

    private void configurarCaducidadDesdeTexto(String caducidadTexto) {
        if (caducidadTexto == null || caducidadTexto.isBlank()) {
            dpCaducidad.setValue(null);
            return;
        }
        try {
            dpCaducidad.setValue(java.time.LocalDate.parse(caducidadTexto));
        } catch (java.time.format.DateTimeParseException e) {
            dpCaducidad.setValue(null);
        }
    }
}
