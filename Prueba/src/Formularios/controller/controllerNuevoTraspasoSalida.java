package Formularios.controller;

import Compartido.controller.productoCboxController;
import Compartido.model.DAO.GenericDAO;
import Formularios.model.modelNuevoTraspasoSalida;
import Operaciones.compra.model.UbicacionCompra;
import Operaciones.traspasoSalida.model.traspasoSalida;
import conexion.conexionFTP;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import Compartido.helper.AutoCompleteComboBoxListener;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
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
import javafx.stage.Stage;
import javafx.util.Duration;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Objects;

public class controllerNuevoTraspasoSalida {

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
    @FXML private TextField txtPrecioEntrada;
    @FXML private TextField txtPrecioIVA;
    @FXML private TextField txtPrecioBruto;
    @FXML private TextField txtPrecioTotal;
    @FXML private Button btnGuardar;
    @FXML private Button btnLimpiar;
    @FXML private TabPane tabPaneModo;
    @FXML private Tab tabNormal;
    @FXML private Tab tabRapido;
    @FXML private TextField txtCantidadRapida;
    @FXML private TextField txtPrecioEntradaRapida;
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
    private ObservableList<traspasoSalida> itemsTraspaso;
    private Operaciones.traspasoSalida.controller.MainController mainController;

    private BigDecimal precioEntradaBase = BigDecimal.ZERO;
    private BigDecimal precioIvaBase = BigDecimal.ZERO;
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
    private boolean seleccionarClaveAlternaPendiente = false;
    private boolean cantidadRapidaValida = false;
    private final Map<String, Image> cacheImagenes = new HashMap<>();
    private traspasoSalida itemParaEditar;
    private boolean modoEdicion = false;
    private boolean inicializado = false;

    @FXML
    public void initialize() {
        productoController = new productoCboxController();
        productoController.inicializarDisponibles(cbClaveProducto, cbProductoNombre, cbClaveAlterna);

        configurarPresentaciones();
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
        configurarSeleccionClaveAlternaPorDefecto();
        configurarCampoCantidadUbicacion(txtCantidadUbicacion, comboUbicacion);
        configurarComboUbicacion(comboUbicacion, txtCantidadUbicacion);
        ubicacionesCapturadas.put(comboUbicacion, new ArrayList<>());

        inicializado = true;  // AGREGAR ESTA LÍNEA

        // Si hay item para editar, cargarlo
        if (itemParaEditar != null) {
            cargarItemParaEditar();
        }

        Platform.runLater(() -> cbClaveProducto.requestFocus());
    }

    private void cargarItemParaEditar() {
        if (itemParaEditar == null) {
            return;
        }

        // Cargar datos del item en los campos del formulario
        cbClaveProducto.setValue(itemParaEditar.getClaveProducto());
        cbProductoNombre.setValue(itemParaEditar.getProducto());
        txtDescripcion.setText(itemParaEditar.getDescripcion());

        txtLote.setText(itemParaEditar.getLote());

        // Configurar caducidad
        if (itemParaEditar.getCaducidad() != null && !itemParaEditar.getCaducidad().isBlank()) {
            try {
                dpCaducidad.setValue(java.time.LocalDate.parse(itemParaEditar.getCaducidad()));
            } catch (Exception e) {
                dpCaducidad.setValue(null);
            }
        }

        txtCantidad.setText(String.valueOf(itemParaEditar.getCantidad()));
        cbPresentacion.setValue(itemParaEditar.getPresentacion());
        txtFactor.setText(String.valueOf(itemParaEditar.getFactor()));

        // Cargar precios
        txtPrecioEntrada.setText(itemParaEditar.getPrecioEntrada());
        txtPrecioIVA.setText(itemParaEditar.getPrecioIva());
        txtPrecioBruto.setText(itemParaEditar.getPrecioBruto());
        txtPrecioTotal.setText(itemParaEditar.getPrecioTotal());

        // Cargar ubicaciones
        cargarUbicacionesParaEdicion(itemParaEditar.getUbicaciones());

        // Actualizar validaciones
        loteValidado = true;
        caducidadValidada = true;
        presentacionValida = true;
        factorValido = true;
        cantidadTotalValida = true;

        // Actualizar imagen del producto
        actualizarImagenProducto();

        // Establecer foco
        Platform.runLater(() -> txtLote.requestFocus());
    }
    private void cargarUbicacionesParaEdicion(List<UbicacionCompra> ubicacionesLista) {
        if (ubicacionesLista == null || ubicacionesLista.isEmpty()) {
            return;
        }

        // Limpiar ubicaciones existentes
        while (contenedorUbicaciones.getChildren().size() > 1) {
            contenedorUbicaciones.getChildren().remove(1);
        }
        contadorFilas = 1;

        // Cargar primera ubicación en el combo principal
        if (!ubicacionesLista.isEmpty()) {
            UbicacionCompra primeraUbicacion = ubicacionesLista.get(0);
            comboUbicacion.setValue(primeraUbicacion.getUbicacion());
            txtCantidadUbicacion.setText(String.valueOf(primeraUbicacion.getCantidad()));

            // Registrar en el mapa de ubicaciones capturadas
            List<UbicacionCompra> lista = new ArrayList<>();
            lista.add(primeraUbicacion);
            ubicacionesCapturadas.put(comboUbicacion, lista);
        }

        // Cargar ubicaciones restantes
        for (int i = 1; i < ubicacionesLista.size(); i++) {
            UbicacionCompra ubicacion = ubicacionesLista.get(i);
            agregarUbicacionParaEdicion(ubicacion);
        }
    }

    private void agregarUbicacionParaEdicion(UbicacionCompra ubicacion) {
        if (contadorFilas >= MAX_FILAS) {
            return;
        }

        HBox nuevaFila = new HBox(20);

        VBox vboxUbicacion = new VBox(5);
        Label lblUbicacion = new Label("Ubicación:");
        ComboBox<String> nuevoCombo = new ComboBox<>(this.ubicaciones);
        nuevoCombo.setEditable(false);
        nuevoCombo.setPromptText("Selecciona una ubicación");
        configurarAutocompletadoUbicacion(nuevoCombo);
        configurarComboUbicacion(nuevoCombo, txtCantidad);
        vboxUbicacion.getChildren().addAll(lblUbicacion, nuevoCombo);
        HBox.setHgrow(vboxUbicacion, Priority.ALWAYS);

        VBox vboxCantidad = new VBox(5);
        Label lblCantidad = new Label("Cantidad en ubicación:");
        TextField txtCantidad = new TextField();
        configurarCampoCantidadUbicacion(txtCantidad, nuevoCombo);
        vboxCantidad.getChildren().addAll(lblCantidad, txtCantidad);
        HBox.setHgrow(vboxCantidad, Priority.ALWAYS);

        VBox vboxBoton = new VBox(5);
        Button botonEliminar = new Button();
        String styleV = "-fx-background-color: #d3d3d3; -fx-border-color: #999; -fx-font-weight: bold; -fx-cursor: hand; -fx-border-radius: 5; -fx-max-width: 25; -fx-max-height: 25; -fx-background-radius: 5; -fx-text-fill: black;";
        botonEliminar.setStyle(styleV);
        botonEliminar.setText("-");
        vboxBoton.setAlignment(Pos.BOTTOM_CENTER);
        vboxBoton.getChildren().addAll(botonEliminar);
        HBox.setHgrow(vboxBoton, Priority.ALWAYS);
        botonEliminar.setOnAction(this::manejarEliminar);

        nuevaFila.getChildren().addAll(vboxUbicacion, vboxCantidad, vboxBoton);
        contenedorUbicaciones.getChildren().add(nuevaFila);

        // Establecer valores
        nuevoCombo.setValue(ubicacion.getUbicacion());
        txtCantidad.setText(String.valueOf(ubicacion.getCantidad()));

        // Registrar en el mapa de ubicaciones capturadas
        List<UbicacionCompra> lista = new ArrayList<>();
        lista.add(ubicacion);
        ubicacionesCapturadas.put(nuevoCombo, lista);

        contadorFilas++;
    }

    public void setItemParaEditar(traspasoSalida item) {
        this.itemParaEditar = item;
        this.modoEdicion = true;

        if (itemParaEditar != null && inicializado) {
            cargarItemParaEditar();
        }
    }

    private void configurarPresentaciones() {
        cbPresentacion.setItems(presentaciones);
        cbPresentacion.setValue(null);
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
                cargarPreciosDesdeProducto();
                cargarPreciosRapidosDesdeUltimaEntrada();
                actualizarEstadoCascada();
                seleccionarClaveAlternaPendiente = true;
            }
            actualizarImagenProducto();
        });

        cbProductoNombre.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                actualizarDescripcionDesdeProducto();
                cargarPreciosDesdeProducto();
                cargarPreciosRapidosDesdeUltimaEntrada();
                actualizarEstadoCascada();
                seleccionarClaveAlternaPendiente = true;
            }
            actualizarImagenProducto();
        });

        cbClaveAlterna.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                actualizarDescripcionDesdeProducto();
                cargarPreciosDesdeProducto();
                cargarPreciosRapidosDesdeUltimaEntrada();
                actualizarEstadoCascada();
            }
            actualizarImagenProducto();
        });

        cbPresentacion.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.isBlank()) {
                String cantidadTexto = txtCantidad.getText() != null ? txtCantidad.getText().trim() : "";
                if (cantidadTexto.isBlank()) {
                    cbPresentacion.setValue(null);
                    mostrarAlertaCascada("Debe capturar la cantidad antes de la presentación.");
                    return;
                }
            }
            if (newVal != null && !newVal.equals(oldVal)) {
                txtFactor.clear();
                ultimoFactorValidado = "";
                factorValido = false;
            }
            if (newVal != null && newVal.equalsIgnoreCase("pz")) {
                txtFactor.setText("1");
            }
            presentacionValida = false;
            factorValido = false;
            validarPresentacion();
            actualizarEstadoCascada();
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
        if (comboBox == null || comboBox.getEditor() == null) {
            return;
        }
        comboBox.getEditor().focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                String texto = comboBox.getEditor().getText();
                if (texto == null || texto.isBlank()) {
                    limpiarSeleccionProducto();
                }
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

    private void limpiarSeleccionProducto() {
        productoController.limpiarSeleccion();
        txtDescripcion.clear();
        limpiarPrecios();
        limpiarValidacionesInventario();
        limpiarUbicacionPrimaria();
        cbPresentacion.setValue(null);
        txtFactor.clear();
        if (txtCantidadRapida != null) {
            txtCantidadRapida.clear();
        }
        if (txtPrecioEntradaRapida != null) {
            txtPrecioEntradaRapida.clear();
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
        actualizarEstadoCascada();
    }

    private void configurarCalculoPrecios() {
        txtCantidad.textProperty().addListener((obs, oldVal, newVal) -> {
            validarCantidadTotalDisponible();
            cargarPreciosDesdeProducto();
            recalcularPrecios();
            actualizarEstadoCascada();
        });

        if (txtCantidadRapida != null) {
            txtCantidadRapida.textProperty().addListener((obs, oldVal, newVal) -> {
                programarValidacionCantidadRapida();
                recalcularPreciosRapido();
            });
        }

        if (txtPrecioEntradaRapida != null) {
            txtPrecioEntradaRapida.textProperty().addListener((obs, oldVal, newVal) -> recalcularPreciosRapido());
        }

        if (txtPrecioIVARapida != null) {
            txtPrecioIVARapida.textProperty().addListener((obs, oldVal, newVal) -> recalcularPreciosRapido());
        }

        txtFactor.textProperty().addListener((obs, oldVal, newVal) -> {
            programarValidacionFactor(newVal);
        });
    }

    private void configurarCamposLectura() {
        txtPrecioEntrada.setEditable(false);
        txtPrecioIVA.setEditable(false);
        txtPrecioBruto.setEditable(false);
        txtPrecioTotal.setEditable(false);
        if (txtPrecioEntradaRapida != null) {
            txtPrecioEntradaRapida.setEditable(false);
        }
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
        if (txtCantidadRapida != null) {
            validarNumerosEnteros(txtCantidadRapida);
        }
    }

    private void validarNumerosEnteros(TextField campo) {
        campo.textProperty().addListener((obs, old, val) -> {
            if (!val.matches("\\d*")) {
                campo.setText(val.replaceAll("[^\\d]", ""));
            }
        });
    }


    private void actualizarDescripcionDesdeProducto() {
        String descripcion = productoController.getDescripcionSeleccionada();
        txtDescripcion.setText(descripcion);
    }

    private void cargarPreciosDesdeProducto() {
        String idProducto = productoController.getIdSeleccionado();
        if (idProducto == null || idProducto.isBlank()) {
            limpiarPrecios();
            return;
        }

        if (!datosCompletosParaPrecio()) {
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
                    aplicarPrecios(resultado.get());
                } else {
                    //limpiarPrecios();
                }
            }

            @Override
            protected void failed() {
                //limpiarPrecios();
            }
        };

        Thread hilo = new Thread(task);
        hilo.setDaemon(true);
        hilo.start();
    }

    private void cargarPreciosRapidosDesdeUltimaEntrada() {
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
        BigDecimal precioIvaRapida = precios.getPrecioIva() != null
                ? precios.getPrecioIva()
                : BigDecimal.ZERO;

        if (txtPrecioEntradaRapida != null) {
            txtPrecioEntradaRapida.setText(formatearDecimal(precioEntradaRapida));
        }
        if (txtPrecioIVARapida != null) {
            txtPrecioIVARapida.setText(formatearDecimal(precioIvaRapida));
        }
        recalcularPreciosRapido();
    }

    private void limpiarPreciosRapidos() {
        if (txtPrecioEntradaRapida != null) {
            txtPrecioEntradaRapida.clear();
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

    private void aplicarPrecios(modelNuevoTraspasoSalida.PreciosProducto precios) {
        if (precios == null) {
            limpiarPrecios();
            return;
        }
        precioEntradaBase = precios.getPrecioUnitario() != null ? precios.getPrecioUnitario() : BigDecimal.ZERO;
        precioIvaBase = precios.getPrecioIva() != null ? precios.getPrecioIva() : BigDecimal.ZERO;

        txtPrecioEntrada.setText(formatearDecimal(precioEntradaBase));
        txtPrecioIVA.setText(formatearDecimal(precioIvaBase));
        recalcularPrecios();
    }

    private void limpiarPrecios() {
        txtPrecioEntrada.clear();
        txtPrecioIVA.clear();
        txtPrecioBruto.clear();
        txtPrecioTotal.clear();
        precioEntradaBase = BigDecimal.ZERO;
        precioIvaBase = BigDecimal.ZERO;
    }

    private void guardarItemEditado() {
        // Validar que todos los campos estén completos
        String clave = productoController.getIdSeleccionado();
        String nombre = productoController.getNombreSeleccionado();
        String descripcion = txtDescripcion.getText() != null ? txtDescripcion.getText().trim() : "";
        String lote = txtLote.getText() != null ? txtLote.getText().trim() : "";
        java.time.LocalDate caducidad = dpCaducidad.getValue();
        String cantidadTexto = txtCantidad.getText() != null ? txtCantidad.getText().trim() : "";
        String presentacion = cbPresentacion.getValue();
        String factorTexto = txtFactor.getText() != null ? txtFactor.getText().trim() : "";
        String precioEntrada = txtPrecioEntrada.getText() != null ? txtPrecioEntrada.getText().trim() : "";
        String precioIva = txtPrecioIVA.getText() != null ? txtPrecioIVA.getText().trim() : "";
        String precioBruto = txtPrecioBruto.getText() != null ? txtPrecioBruto.getText().trim() : "";
        String precioTotal = txtPrecioTotal.getText() != null ? txtPrecioTotal.getText().trim() : "";

        // Validaciones (igual que en guardarItem normal)
        if (clave == null || clave.isBlank()
                || nombre == null || nombre.isBlank()
                || descripcion.isBlank()
                || lote.isBlank()
                || cantidadTexto.isBlank()
                || presentacion == null || presentacion.isBlank()
                || factorTexto.isBlank()
                || precioEntrada.isBlank()
                || precioIva.isBlank()
                || precioBruto.isBlank()
                || precioTotal.isBlank()) {
            mostrarAlerta("Advertencia", "Debe completar todos los campos antes de guardar.");
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

        // Verificar si el lote cambió y si ya existe
        if (!lote.equals(itemParaEditar.getLote())) {
            if (existeProductoLoteEnTraspaso(clave, lote)) {
                mostrarAlerta("Advertencia",
                        "Ya se agregó este producto con el mismo lote. Finaliza el traspaso para poder repetirlo.");
                return;
            }
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

        // Actualizar el item existente
        itemParaEditar.setClaveProducto(clave);
        itemParaEditar.setProducto(nombre);
        itemParaEditar.setDescripcion(descripcion);
        itemParaEditar.setLote(lote);
        itemParaEditar.setCaducidad(caducidad.toString());
        itemParaEditar.setCantidad(cantidad);
        itemParaEditar.setPresentacion(presentacion);
        itemParaEditar.setFactor(factor);
        itemParaEditar.setUbicaciones(ubicacionesSeleccionadas);
        itemParaEditar.setPrecioEntrada(precioEntrada);
        itemParaEditar.setPrecioIva(precioIva);
        itemParaEditar.setPrecioBruto(precioBruto);
        itemParaEditar.setPrecioTotal(precioTotal);

        // Refrescar la tabla
        if (mainController != null) {
            mainController.refrescarTabla();
        }

        mostrarAlertaSinEspera("Éxito", "Producto actualizado en el traspaso de salida.");
        cerrarFormulario();
    }

    private void guardarItem() {
        if (modoEdicion && itemParaEditar != null) {
            guardarItemEditado();
            return;
        }
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
        String presentacion = cbPresentacion.getValue();
        String factorTexto = txtFactor.getText() != null ? txtFactor.getText().trim() : "";
        String precioEntrada = txtPrecioEntrada.getText() != null ? txtPrecioEntrada.getText().trim() : "";
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
                || precioIva.isBlank()
                || precioBruto.isBlank()
                || precioTotal.isBlank()) {
            mostrarAlerta("Advertencia", "Debe completar todos los campos antes de guardar.");
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
        if (existeProductoLoteEnTraspaso(clave, lote)) {
            mostrarAlerta("Advertencia",
                    "Ya se agregó este producto con el mismo lote. Finaliza el traspaso para poder repetirlo.");
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

        if (itemsTraspaso == null) {
            mostrarAlerta("Error", "No se pudo registrar el traspaso en la tabla.");
            return;
        }

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
                precioEntrada,
                precioIva,
                precioBruto,
                precioTotal
        );
        itemsTraspaso.add(item);

        if (mainController != null) {
            mainController.refrescarTabla();
        }

        mostrarAlertaSinEspera("Éxito", "Producto agregado al traspaso de salida.");
        limpiarFormularioParaNuevo();
    }

    private void guardarItemRapido() {
        String clave = productoController.getIdSeleccionado();
        String nombre = productoController.getNombreSeleccionado();
        String descripcion = txtDescripcion.getText() != null ? txtDescripcion.getText().trim() : "";
        String cantidadTexto = txtCantidadRapida != null && txtCantidadRapida.getText() != null
                ? txtCantidadRapida.getText().trim()
                : "";
        String precioEntrada = txtPrecioEntradaRapida != null && txtPrecioEntradaRapida.getText() != null
                ? txtPrecioEntradaRapida.getText().trim()
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

        if (clave == null || clave.isBlank()
                || nombre == null || nombre.isBlank()
                || descripcion.isBlank()
                || cantidadTexto.isBlank()
                || precioEntrada.isBlank()
                || precioIva.isBlank()
                || precioBruto.isBlank()
                || precioTotal.isBlank()) {
            mostrarAlerta("Advertencia", "Debe completar todos los campos antes de guardar.");
            return;
        }

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

        int disponible = modelo.obtenerCantidadDisponibleProductoPresentacionFactor(clave, "pz", 1);
        if (cantidad > disponible) {
            mostrarAlerta("Advertencia",
                    "La cantidad supera la disponible para la presentación pz con factor 1.");
            return;
        }

        if (itemsTraspaso == null) {
            mostrarAlerta("Error", "No se pudo registrar el traspaso en la tabla.");
            return;
        }

        List<modelNuevoTraspasoSalida.DisponibilidadRapida> disponibles =
                modelo.obtenerDisponibilidadesRapidas(clave, "pz", 1);
        List<AsignacionRapida> asignaciones = construirAsignacionesRapidas(disponibles, cantidad);
        if (asignaciones.isEmpty()) {
            mostrarAlerta("Error", "No se pudo distribuir la cantidad solicitada con la disponibilidad actual.");
            return;
        }
        if (!confirmarRevisionUbicacionesRapidas()) {
            return;
        }
        if (!mostrarResumenUbicacionesRapidas(asignaciones)) {
            return;
        }
        mostrarAlerta("Aviso", "Revisión de ubicaciones confirmada.");

        List<traspasoSalida> itemsGenerados = construirItemsRapidosTraspaso(clave, nombre, descripcion, asignaciones);
        if (itemsGenerados.isEmpty()) {
            mostrarAlerta("Error", "No se pudo distribuir la cantidad solicitada con la disponibilidad actual.");
            return;
        }

        BigDecimal precioEntradaDecimal = parseDecimal(precioEntrada);
        BigDecimal precioIvaDecimal = parseDecimal(precioIva);
        for (traspasoSalida item : itemsGenerados) {
            if (existeProductoLoteEnTraspaso(clave, item.getLote())) {
                mostrarAlerta("Advertencia",
                        "Ya se agregó este producto con el mismo lote. Finaliza el traspaso para poder repetirlo.");
                return;
            }
            int cantidadItem = item.getCantidad();
            BigDecimal brutoItem = precioEntradaDecimal.multiply(BigDecimal.valueOf(cantidadItem));
            BigDecimal totalItem = precioIvaDecimal.multiply(BigDecimal.valueOf(cantidadItem));
            item.setPrecioEntrada(formatearDecimal(precioEntradaDecimal));
            item.setPrecioIva(formatearDecimal(precioIvaDecimal));
            item.setPrecioBruto(formatearDecimal(brutoItem));
            item.setPrecioTotal(formatearDecimal(totalItem));
            itemsTraspaso.add(item);
        }

        if (mainController != null) {
            mainController.refrescarTabla();
        }

        mostrarAlertaSinEspera("Éxito", "Producto agregado al traspaso de salida.");
        limpiarFormularioParaNuevo();
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

        HBox encabezado = new HBox(10);
        encabezado.setStyle("-fx-padding: 6 8 6 8; -fx-background-color: #f0f0f0; -fx-border-color: #cccccc;");
        encabezado.setAlignment(Pos.CENTER_LEFT);
        Label tituloUbicacion = new Label("Lote / Ubicación");
        tituloUbicacion.setStyle("-fx-font-weight: bold;");
        Label tituloCantidad = new Label("Cantidad");
        tituloCantidad.setStyle("-fx-font-weight: bold;");
        HBox.setHgrow(tituloUbicacion, Priority.ALWAYS);
        encabezado.getChildren().addAll(tituloUbicacion, tituloCantidad);
        contenido.getChildren().add(encabezado);

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

    private List<traspasoSalida> construirItemsRapidosTraspaso(
            String clave,
            String nombre,
            String descripcion,
            List<AsignacionRapida> asignaciones
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
                    "pz",
                    1,
                    entry.getValue(),
                    "",
                    "",
                    "",
                    ""
            );
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

            VBox vboxUbicacion = (VBox) fila.getChildren().get(0);
            VBox vboxCantidad = (VBox) fila.getChildren().get(1);

            ComboBox<?> combo = null;
            TextField cantidadField = null;

            for (javafx.scene.Node child : vboxUbicacion.getChildren()) {
                if (child instanceof ComboBox) {
                    combo = (ComboBox<?>) child;
                    break;
                }
            }

            for (javafx.scene.Node child : vboxCantidad.getChildren()) {
                if (child instanceof TextField) {
                    cantidadField = (TextField) child;
                    break;
                }
            }

            if (combo == null || cantidadField == null) {
                continue;
            }

            String ubicacion = combo.getValue() != null ? combo.getValue().toString().trim() : "";
            String cantidadTexto = cantidadField.getText() != null ? cantidadField.getText().trim() : "";

            if (ubicacion.isBlank() || cantidadTexto.isBlank()) {
                mostrarAlerta("Advertencia", "Debe completar todas las ubicaciones y cantidades.");
                return new ArrayList<>();
            }

            try {
                int cantidad = Integer.parseInt(cantidadTexto);
                if (cantidad <= 0) {
                    mostrarAlerta("Advertencia", "La cantidad por ubicación debe ser mayor a 0.");
                    return new ArrayList<>();
                }
                resultado.add(new UbicacionCompra(ubicacion, cantidad));
            } catch (NumberFormatException e) {
                mostrarAlerta("Error", "La cantidad por ubicación debe ser un número válido.");
                return new ArrayList<>();
            }
        }

        return resultado;
    }

    private boolean existeProductoLoteEnTraspaso(String clave, String lote) {
        if (clave == null || clave.isBlank() || lote == null || lote.isBlank()) {
            return false;
        }
        if (itemsTraspaso == null) {
            return false;
        }
        String claveNormalizada = clave.trim();
        String loteNormalizado = lote.trim();
        return itemsTraspaso.stream()
                .anyMatch(item -> item != null
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
        modoEdicion = false;
        itemParaEditar = null;

        if (productoController != null) {
            productoController.limpiarSeleccion();
        }
        txtDescripcion.clear();
        txtLote.clear();
        dpCaducidad.setValue(null);
        txtCantidad.clear();
        cbPresentacion.setValue(null);
        txtFactor.clear();
        txtCantidadUbicacion.clear();
        limpiarPrecios();
        limpiarValidacionesInventario();
        limpiarUbicacionPrimaria();
        seleccionarClaveAlternaPendiente = false;
        ubicacionesCapturadas.clear();
        debounceCantidadUbicacion.clear();
        ultimaCantidadUbicacionValidada.clear();
        ubicacionesCapturadas.put(comboUbicacion, new ArrayList<>());

        while (contenedorUbicaciones.getChildren().size() > 1) {
            contenedorUbicaciones.getChildren().remove(1);
        }
        contadorFilas = 1;
        limpiarComboUbicacion(comboUbicacion);

        if (txtCantidadRapida != null) {
            txtCantidadRapida.clear();
        }
        if (txtPrecioEntradaRapida != null) {
            txtPrecioEntradaRapida.clear();
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

        cbClaveProducto.requestFocus();
        actualizarEstadoCascada();
    }

    @FXML
    private void agregarUbicacion() {
        if (contadorFilas >= MAX_FILAS) {
            Alert alerta = new Alert(AlertType.INFORMATION);
            alerta.setTitle("Límite alcanzado");
            alerta.setHeaderText(null);
            alerta.setContentText("Solo se pueden agregar hasta " + MAX_FILAS + " ubicaciones.");
            alerta.showAndWait();
            return;
        }

        HBox nuevaFila = new HBox(20);

        VBox vboxUbicacion = new VBox(5);
        Label lblUbicacion = new Label("Ubicación:");
        ComboBox<String> nuevoCombo = new ComboBox<>(ubicaciones);
        nuevoCombo.setEditable(false);
        nuevoCombo.setPromptText("Selecciona una ubicación");
        configurarAutocompletadoUbicacion(nuevoCombo);
        configurarComboUbicacion(nuevoCombo, txtCantidad);
        vboxUbicacion.getChildren().addAll(lblUbicacion, nuevoCombo);
        HBox.setHgrow(vboxUbicacion, Priority.ALWAYS);

        VBox vboxCantidad = new VBox(5);
        Label lblCantidad = new Label("Cantidad en ubicación:");
        TextField txtCantidad = new TextField();
        configurarCampoCantidadUbicacion(txtCantidad, nuevoCombo);
        vboxCantidad.getChildren().addAll(lblCantidad, txtCantidad);
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
        ubicacionesCapturadas.put(nuevoCombo, new ArrayList<>());

        contadorFilas++;
    }

    private void manejarEliminar(ActionEvent event) {
        Button botonPresionado = (Button) event.getSource();
        VBox contenedorBoton = (VBox) botonPresionado.getParent();
        HBox fila = (HBox) contenedorBoton.getParent();

        contenedorUbicaciones.getChildren().remove(fila);
        limpiarCapturasCombo((ComboBox<String>) ((VBox) fila.getChildren().get(0)).getChildren().stream()
                .filter(node -> node instanceof ComboBox)
                .findFirst()
                .orElse(null));
        ComboBox<String> combo = (ComboBox<String>) ((VBox) fila.getChildren().get(0)).getChildren().stream()
                .filter(node -> node instanceof ComboBox)
                .findFirst()
                .orElse(null);
        limpiarCapturasCombo(combo);
        ubicacionesCapturadas.remove(combo);
        contadorFilas--;
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
            return;
        }

        BigDecimal precioBruto = precioEntradaBase.multiply(BigDecimal.valueOf(cantidad));
        BigDecimal precioTotal = precioIvaBase.multiply(BigDecimal.valueOf(cantidad));

        txtPrecioBruto.setText(formatearDecimal(precioBruto));
        txtPrecioTotal.setText(formatearDecimal(precioTotal));
    }

    private void recalcularPreciosRapido() {
        if (txtCantidadRapida == null) {
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
            return;
        }

        BigDecimal precioEntrada = parseDecimal(txtPrecioEntradaRapida != null
                ? txtPrecioEntradaRapida.getText()
                : null);
        BigDecimal precioIva = parseDecimal(txtPrecioIVARapida != null
                ? txtPrecioIVARapida.getText()
                : null);

        BigDecimal precioBruto = precioEntrada.multiply(BigDecimal.valueOf(cantidad));
        BigDecimal precioTotal = precioIva.multiply(BigDecimal.valueOf(cantidad));

        if (txtPrecioBrutoRapida != null) {
            txtPrecioBrutoRapida.setText(formatearDecimal(precioBruto));
        }
        if (txtPrecioTotalRapida != null) {
            txtPrecioTotalRapida.setText(formatearDecimal(precioTotal));
        }
    }

    private void actualizarPreciosPorUbicaciones() {
        cargarPreciosDesdeProducto();
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

    private void cerrarFormulario() {
        if (btnGuardar == null || btnGuardar.getScene() == null) {
            return;
        }
        Stage stage = (Stage) btnGuardar.getScene().getWindow();
        stage.close();
    }

    public void setItemsTraspaso(ObservableList<traspasoSalida> itemsTraspaso) {
        this.itemsTraspaso = itemsTraspaso;
    }

    public void setMainController(Operaciones.traspasoSalida.controller.MainController mainController) {
        this.mainController = mainController;
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
                }
                ubicacionValidada = false;
                cantidadTotalValida = false;
                presentacionValida = false;
                factorValido = false;
                actualizarEstadoCascada();
                //limpiarPrecios();
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
        if (cbPresentacion.getValue() == null || cbPresentacion.getValue().isBlank()) {
            if (txtFactor.getText() != null && !txtFactor.getText().isBlank()) {
                txtFactor.clear();
                mostrarAlertaCascada("Debe capturar la presentación antes del factor.");
                return;
            }
        }
        String cantidadTexto = txtCantidad.getText() != null ? txtCantidad.getText().trim() : "";
        if (cantidadTexto.isBlank() && txtFactor.getText() != null && !txtFactor.getText().isBlank()) {
            txtFactor.clear();
            mostrarAlertaCascada("Debe capturar la cantidad antes del factor.");
            return;
        }
    }

    private void validarCamposDesdePresentacion() {
        validarCamposDesdeLote();
        if (!loteValidado) {
            return;
        }
        String cantidadTexto = txtCantidad.getText() != null ? txtCantidad.getText().trim() : "";
        if (cantidadTexto.isBlank() && cbPresentacion.getValue() != null && !cbPresentacion.getValue().isBlank()) {
            cbPresentacion.setValue(null);
            mostrarAlertaCascada("Debe capturar la cantidad antes de la presentación.");
            return;
        }
        validarPresentacion();
    }

    private void validarCamposDesdeFactor() {
        validarCamposDesdeCantidad();
        if (!cantidadTotalValida) {
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
                //limpiarPrecios();
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
                //limpiarPrecios();
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
        String idProducto = productoController.getIdSeleccionado();
        String lote = txtLote.getText() != null ? txtLote.getText().trim() : "";
        String presentacion = cbPresentacion.getValue();
        int factor = parseEntero(txtFactor.getText());
        java.time.LocalDate caducidad = dpCaducidad.getValue();
        String ubicacion = combo.getValue() != null ? combo.getValue().trim() : "";
        if (idProducto == null || idProducto.isBlank()
                || lote.isBlank()
                || presentacion == null
                || presentacion.isBlank()
                || factor <= 0
                || ubicacion.isBlank()) {
            campoCantidad.clear();
            mostrarAlerta("Advertencia", "Debe completar las características del producto antes de la cantidad.");
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
                int factorActual = parseEntero(txtFactor.getText());
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
                            "No hay existencia en esa ubicación con las características indicadas.");
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
        javafx.concurrent.Task<Integer> task = new javafx.concurrent.Task<>() {
            @Override
            protected Integer call() {
                return modelo.obtenerCantidadDisponibleProductoPresentacionFactor(idSnapshot, "pz", 1);
            }

            @Override
            protected void succeeded() {
                String textoActual = txtCantidadRapida.getText() != null ? txtCantidadRapida.getText().trim() : "";
                if (!textoActual.equals(String.valueOf(cantidadSnapshot))) {
                    return;
                }
                int disponible = getValue() != null ? getValue() : 0;
                if (cantidadSnapshot > disponible) {
                    cantidadRapidaValida = false;
                    mostrarAlertaSinEspera("Advertencia",
                            "La cantidad supera la disponible para la presentación pz con factor 1.");
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
        String cantidadTexto = txtCantidad.getText() != null ? txtCantidad.getText().trim() : "";
        if (cantidadTexto.isBlank()) {
            txtFactor.clear();
            mostrarAlertaCascada("Debe capturar la cantidad antes del factor.");
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
        String presentacion = cbPresentacion.getValue();
        String factorTexto = txtFactor.getText();
        if (presentacion == null || presentacion.isBlank()
                || factorTexto == null || factorTexto.isBlank()) {
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
        String presentacionSnapshot = presentacion;
        int factorSnapshot = factor;
        int cantidadSnapshot = cantidad;

        javafx.concurrent.Task<Integer> task = new javafx.concurrent.Task<>() {
            @Override
            protected Integer call() {
                return modelo.obtenerCantidadDisponibleProductoLoteCaducidadPresentacionFactor(
                        idSnapshot, loteSnapshot, null, presentacionSnapshot, factorSnapshot);
            }

            @Override
            protected void succeeded() {
                String loteActual = txtLote.getText() != null ? txtLote.getText().trim() : "";
                String idActual = productoController.getIdSeleccionado();
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
        if (cantidadTexto.isBlank()) {
            presentacionValida = false;
            if (presentacion != null && !presentacion.isBlank()) {
                cbPresentacion.setValue(null);
                mostrarAlertaSinEspera("Advertencia", "Debe capturar la cantidad antes de la presentación.");
            }
            return;
        }
        int cantidad = parseEntero(cantidadTexto);
        if (cantidad <= 0) {
            presentacionValida = false;
            if (presentacion != null && !presentacion.isBlank()) {
                cbPresentacion.setValue(null);
                mostrarAlertaSinEspera("Advertencia", "La cantidad debe ser mayor a 0 antes de la presentación.");
            }
            return;
        }
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
                }
            }
        };

        Thread hilo = new Thread(task);
        hilo.setDaemon(true);
        hilo.start();
    }

    private boolean datosCompletosParaPrecio() {
        return productoController.getIdSeleccionado() != null
                && !productoController.getIdSeleccionado().isBlank()
                && loteValidado
                && caducidadValidada
                && cantidadTotalValida;
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
        for (ComboBox<String> combo : ubicacionesCapturadas.keySet()) {
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
}
