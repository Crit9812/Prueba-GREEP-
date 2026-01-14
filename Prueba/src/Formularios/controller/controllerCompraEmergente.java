package Formularios.controller;

import Compartido.controller.productoCboxController;
import Formularios.utilities.helperCompraEmergente;
import Operaciones.compra.controller.MainController;
import Operaciones.compra.model.UbicacionCompra;
import Operaciones.compra.model.compra;
import Operaciones.compra.model.model;
import conexion.conexionFTP;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.input.KeyCode;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.concurrent.Task;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class controllerCompraEmergente {

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
    @FXML private CheckBox checkBoxIVA;
    @FXML private TextField txtPrecioIVA;
    @FXML private TextField txtPrecioBruto;
    @FXML private TextField txtPrecioTotal;
    @FXML private Button btnGuardar;
    @FXML private Button btnLimpiar;
    @FXML private Label lblTitulo;

    private static final int MAX_FILAS = 10;

    private final ObservableList<String> ubicaciones = FXCollections.observableArrayList();
    private final model modeloCompras = new model();

    private final ObservableList<String> presentaciones = FXCollections.observableArrayList(
            "paquete", "pz", "caja", "bolsa", "pieza", "rollo", "litro", "kilogramo", "metro", "unidad"
    );

    private ObservableList<compra> itemsCompra;
    private MainController mainController;
    private productoCboxController productoController;
    private String proveedorId;
    private String proveedorNombre;
    private boolean inicializado = false;
    private compra itemParaEditar;
    private static final DateTimeFormatter FECHA_FORMATO = DateTimeFormatter.ISO_LOCAL_DATE;
    private String ultimoIdProductoDescripcion = "";
    private boolean seleccionarClaveAlternaPendiente = false;
    private String tituloFormulario = "Compra";
    private final Map<String, Image> cacheImagenes = new HashMap<>();
    private final List<UbicacionRow> filasUbicaciones = new ArrayList<>();

    @FXML
    public void initialize() {
        if (lblTitulo != null) {
            lblTitulo.setText(tituloFormulario);
        }
        productoController = new productoCboxController();
        if (proveedorId != null && !proveedorId.isBlank()) {
            productoController.inicializarConProveedor(cbClaveProducto, cbProductoNombre, cbClaveAlterna, proveedorId);
        } else {
            productoController.inicializar(cbClaveProducto, cbProductoNombre, cbClaveAlterna);
        }

        configurarPresentaciones();
        configurarEventos();
        configurarValidaciones();
        configurarCalculoPrecios();
        configurarCamposLectura();
        cargarUbicacionesDesdeBD();
        configurarUbicacionesIniciales();
        configurarLimpiezaPorCampoVacio();
        configurarManejoEnter();
        configurarSeleccionClaveAlternaPorDefecto();

        inicializado = true;

        if (itemParaEditar != null) {
            cargarItemParaEditar();
        }

        Platform.runLater(() -> cbClaveProducto.requestFocus());
    }

    public void setItemsCompra(ObservableList<compra> itemsCompra) {
        this.itemsCompra = itemsCompra;
    }

    public void setMainController(MainController mainController) {
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

    public void setProveedorSeleccionado(String proveedorId, String proveedorNombre) {
        this.proveedorId = proveedorId;
        this.proveedorNombre = proveedorNombre;

        if (inicializado && productoController != null) {
            productoController.recargarConProveedor(proveedorId);
        }
    }

    public void setItemParaEditar(compra item) {
        this.itemParaEditar = item;
        if (inicializado) {
            cargarItemParaEditar();
        }
    }

    private void configurarPresentaciones() {
        cbPresentacion.setItems(presentaciones);
        cbPresentacion.setValue("pz");
        if (txtFactor != null) {
            txtFactor.setText("1");
        }
    }

    private void cargarUbicacionesDesdeBD() {
        javafx.concurrent.Task<List<String>> task = new javafx.concurrent.Task<>() {
            @Override
            protected List<String> call() {
                return modeloCompras.obtenerNombresUbicaciones();
            }

            @Override
            protected void succeeded() {
                List<String> resultados = getValue();
                ubicaciones.setAll(resultados != null ? resultados : List.of());
                actualizarCombosUbicacion();
            }

            @Override
            protected void failed() {
                ubicaciones.clear();
                actualizarCombosUbicacion();
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
                seleccionarClaveAlternaPendiente = true;
            }
            actualizarImagenProducto();
            cargarPrecioEntradaUltimoProducto();
        });

        cbProductoNombre.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                actualizarDescripcionDesdeProducto();
                seleccionarClaveAlternaPendiente = true;
            }
            actualizarImagenProducto();
            cargarPrecioEntradaUltimoProducto();
        });

        cbClaveAlterna.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                actualizarDescripcionDesdeProducto();
            }
            actualizarImagenProducto();
            cargarPrecioEntradaUltimoProducto();
        });

        cbPresentacion.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.equalsIgnoreCase("pz")) {
                txtFactor.setText("1");
                return;
            }
            if (oldVal != null && oldVal.equalsIgnoreCase("pz") && "1".equals(txtFactor.getText())) {
                txtFactor.clear();
            }
        });

        btnGuardar.setOnAction(e -> guardarItem());
        if (btnLimpiar != null) {
            btnLimpiar.setOnAction(e -> limpiarFormularioParaNuevo());
        }
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

    private void limpiarSeleccionProducto() {
        productoController.limpiarSeleccion();
        txtDescripcion.clear();
        limpiarImagenProducto();
    }

    private void limpiarImagenProducto() {
        if (previewImage != null) {
            previewImage.setImage(null);
        }
    }

    private void actualizarImagenProducto() {
        if (previewImage == null || productoController == null) {
            return;
        }

        previewImage.setImage(null);

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

    private void configurarCalculoPrecios() {
        txtCantidad.textProperty().addListener((obs, oldVal, newVal) -> recalcularPrecios());
        txtPrecioEntrada.textProperty().addListener((obs, oldVal, newVal) -> recalcularPrecios());
        if (checkBoxIVA != null) {
            checkBoxIVA.selectedProperty().addListener((obs, oldVal, newVal) -> recalcularPrecios());
        }
    }

    private void configurarCamposLectura() {
        txtPrecioIVA.setEditable(false);
        txtPrecioBruto.setEditable(false);
        txtPrecioTotal.setEditable(false);
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
    }

    private void configurarValidaciones() {
        // Usar los validadores del helper
        configurarValidadorCampo(txtCantidad, "entero");
        configurarValidadorCampo(txtCantidadUbicacion, "entero");
        configurarValidadorCampo(txtFactor, "decimal");
        configurarValidadorCampo(txtPrecioEntrada, "decimal");
        configurarValidadorCampo(txtPrecioIVA, "decimal");
        configurarValidadorCampo(txtPrecioBruto, "decimal");
        configurarValidadorCampo(txtPrecioTotal, "decimal");
    }

    private void configurarValidadorCampo(TextField campo, String tipo) {
        campo.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) return;

            if ("entero".equals(tipo)) {
                campo.setText(helperCompraEmergente.getValidadorEnteros().apply(newVal));
            } else if ("decimal".equals(tipo)) {
                campo.setText(helperCompraEmergente.getValidadorDecimales().apply(newVal));
            }
        });
    }

    private void actualizarDescripcionDesdeProducto() {
        String idProducto = productoController.getIdSeleccionado();
        if (idProducto != null && idProducto.equals(ultimoIdProductoDescripcion)) {
            return;
        }
        String descripcion = productoController.getDescripcionSeleccionada();
        txtDescripcion.setText(descripcion);
        ultimoIdProductoDescripcion = idProducto != null ? idProducto : "";
    }

    private void cargarPrecioEntradaUltimoProducto() {
        if (itemParaEditar != null) {
            return;
        }
        String idProducto = productoController.getIdSeleccionado();
        if (idProducto == null || idProducto.isBlank()) {
            return;
        }

        Task<java.util.Optional<BigDecimal>> task = new Task<>() {
            @Override
            protected java.util.Optional<BigDecimal> call() {
                return modeloCompras.obtenerPrecioEntradaUltimoProducto(idProducto);
            }

            @Override
            protected void succeeded() {
                java.util.Optional<BigDecimal> resultado = getValue();
                resultado.ifPresent(precio -> {
                    if (precio != null) {
                        txtPrecioEntrada.setText(formatearDecimal(precio));
                        recalcularPrecios();
                    }
                });
            }
        };

        Thread hilo = new Thread(task);
        hilo.setDaemon(true);
        hilo.start();
    }

    // Metodo para actualizar la clave alterna desde el formulario de claves
    public void actualizarClaveAlternaCreada(String claveAlterna) {
        Platform.runLater(() -> {
            if (claveAlterna != null && !claveAlterna.isEmpty() && cbClaveAlterna != null) {
                // Establecer directamente la clave en el ComboBox
                cbClaveAlterna.setValue(claveAlterna);
                cbClaveAlterna.getEditor().setText(claveAlterna);

                // Forzar la actualización desde la clave alterna
                actualizarDesdeClaveAlternaExterna(claveAlterna);

                // Enfocar el siguiente campo para continuar con la compra
                txtCantidad.requestFocus();

            }
        });
    }

    // Metodo auxiliar para actualizar desde clave alterna externa
    private void actualizarDesdeClaveAlternaExterna(String claveAlterna) {
        if (productoController != null) {
            // Usar el metodo existente del productoCboxController
            productoController.setSeleccionPorClaveAlterna(claveAlterna);

            // Actualizar descripción si es necesario
            actualizarDescripcionDesdeProducto();
        }
    }

    private void guardarItem() {
        if (itemsCompra == null) {
            mostrarAlerta("Error", "No se pudo conectar con la tabla principal");
            return;
        }

        String clave = productoController.getIdSeleccionado();
        String nombre = productoController.getNombreSeleccionado();
        String claveAlterna = productoController.getClaveAlternaSeleccionada();
        String descripcion = txtDescripcion.getText();
        String lote = txtLote.getText();
        String caducidad = obtenerCaducidadTexto();
        String cantidadTexto = txtCantidad.getText();
        String nota = txtNota != null ? txtNota.getText() : "";
        String presentacion = cbPresentacion.getValue();
        String factor = txtFactor.getText();
        String precioEntrada = txtPrecioEntrada.getText();
        String precioIVA = txtPrecioIVA.getText();
        String precioBruto = txtPrecioBruto.getText();
        String precioTotal = txtPrecioTotal.getText();

        // 1. Validar producto seleccionado (usando el controller existente)
        if (!productoController.validarSeleccion()) {
            mostrarAlerta("Error", "El ID y el nombre del producto no corresponden.\n" +
                    "Por favor, verifique la selección.");
            return;
        }

        String mensajeCampos = validarCamposObligatorios(clave, nombre, claveAlterna, descripcion, lote,
                cantidadTexto, presentacion, factor, precioEntrada, precioIVA, precioBruto, precioTotal);
        if (mensajeCampos != null) {
            mostrarAlerta("Advertencia", mensajeCampos);
            return;
        }

        // 2. Validar datos del formulario usando el helper
        List<UbicacionCompra> ubicacionesSeleccionadas = obtenerUbicacionesSeleccionadas();

        helperCompraEmergente.ResultadoValidacion validacion =
                helperCompraEmergente.validarFormularioCompleto(
                        clave, nombre, cantidadTexto,
                        ubicacionesSeleccionadas, precioEntrada
                );

        if (!validacion.isValido()) {
            mostrarAlerta("Advertencia", validacion.getMensaje());
            return;
        }

        if (esProductoReactivo() && (caducidad == null || caducidad.isBlank())) {
            mostrarAlerta("Advertencia", "La caducidad es forzosa para los reactivos.");
            return;
        }

        if (tieneUbicacionesDuplicadas(ubicacionesSeleccionadas)) {
            mostrarAlerta("Advertencia", "No se puede seleccionar la misma ubicación más de una vez.");
            return;
        }

        int cantidad = Integer.parseInt(cantidadTexto.trim());

        helperCompraEmergente.ResultadoValidacion validacionUbicaciones =
                validarUbicacionesSeleccionadas(cantidad);
        if (!validacionUbicaciones.isValido()) {
            mostrarAlerta("Advertencia", validacionUbicaciones.getMensaje());
            return;
        }

        // 3. Validación adicional de ubicaciones (ya incluida en validarFormularioCompleto)
        if (presentacion == null || presentacion.isBlank()) {
            presentacion = "pz";
        }

        // 4. Crear o actualizar el item
        if (itemParaEditar != null) {
            itemParaEditar.setClaveProducto(clave);
            itemParaEditar.setProducto(nombre);
            itemParaEditar.setDescripcion(descripcion);
            itemParaEditar.setLote(lote);
            itemParaEditar.setCaducidad(caducidad);
            itemParaEditar.setCantidad(cantidad);
            itemParaEditar.setClaveAlterna(claveAlterna);
            itemParaEditar.setPresentacion(presentacion);
            itemParaEditar.setFactor(factor);
            itemParaEditar.setUbicaciones(ubicacionesSeleccionadas);
            itemParaEditar.setNota(nota);
            itemParaEditar.setPrecioEntrada(precioEntrada);
            itemParaEditar.setPrecioIva(precioIVA);
            itemParaEditar.setPrecioBruto(precioBruto);
            itemParaEditar.setPrecioTotal(precioTotal);
            itemParaEditar.setAplicaIva(checkBoxIVA != null && checkBoxIVA.isSelected());
        } else {
            compra item = new compra(
                    clave, nombre, descripcion, lote, caducidad, cantidad, claveAlterna,
                    presentacion, factor, ubicacionesSeleccionadas, precioEntrada,
                    precioIVA, precioBruto, precioTotal,
                    checkBoxIVA != null && checkBoxIVA.isSelected(),
                    proveedorId, proveedorNombre
            );
            item.setNota(nota);

            itemsCompra.add(item);
        }

        if (mainController != null) {
            mainController.refrescarTabla();
        }

        if (itemParaEditar != null) {
            mostrarAlertaSinEspera("Éxito", "Producto actualizado.");
            cerrarFormulario();
        } else {
            mostrarAlertaSinEspera("Éxito", "Producto agregado a la compra");

            limpiarFormularioParaNuevo();
        }
    }

    private String validarCamposObligatorios(String clave, String nombre, String claveAlterna, String descripcion,
                                             String lote, String cantidad, String presentacion, String factor,
                                             String precioEntrada, String precioIva, String precioBruto,
                                             String precioTotal) {
        if (esVacio(clave) || esVacio(nombre) || esVacio(claveAlterna) || esVacio(descripcion)
                || esVacio(lote) || esVacio(cantidad) || esVacio(presentacion) || esVacio(factor)
                || esVacio(precioEntrada) || esVacio(precioIva) || esVacio(precioBruto)
                || esVacio(precioTotal)) {
            return "Debe completar todos los campos obligatorios (excepto caducidad y nota).";
        }

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
            ComboBox<?> combo = (ComboBox<?>) vboxUbicacion.getChildren().stream()
                    .filter(ComboBox.class::isInstance)
                    .findFirst()
                    .orElse(null);
            TextField campoCantidad = (TextField) vboxCantidad.getChildren().stream()
                    .filter(TextField.class::isInstance)
                    .findFirst()
                    .orElse(null);
            String ubicacion = combo != null ? combo.getEditor().getText() : "";
            String cantidadUbicacion = campoCantidad != null ? campoCantidad.getText() : "";
            if (esVacio(ubicacion) || esVacio(cantidadUbicacion)) {
                return "Debe completar todas las ubicaciones y cantidades.";
            }
        }

        return null;
    }

    private boolean esVacio(String valor) {
        return valor == null || valor.isBlank();
    }

    private List<UbicacionCompra> obtenerUbicacionesSeleccionadas() {
        List<UbicacionCompra> resultado = new ArrayList<>();

        for (javafx.scene.Node nodo : contenedorUbicaciones.getChildren()) {
            if (!(nodo instanceof HBox)) continue;
            HBox fila = (HBox) nodo;
            if (fila.getChildren().size() < 2) continue;

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

            if (combo == null || cantidadField == null) continue;

            String ubicacion = obtenerTextoCombo((ComboBox<String>) combo);
            String cantidadTexto = cantidadField.getText();

            if (ubicacion == null || ubicacion.isBlank() || cantidadTexto == null || cantidadTexto.isBlank()) {
                continue;
            }

            try {
                int cantidad = Integer.parseInt(cantidadTexto);
                if (cantidad > 0) {
                    resultado.add(new UbicacionCompra(ubicacion, cantidad));
                }
            } catch (NumberFormatException ignored) {
                // Ignorar ubicaciones con cantidad inválida
            }
        }

        return resultado;
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
        productoController.limpiarSeleccion();
        seleccionarClaveAlternaPendiente = false;
        limpiarCombosProducto();
        txtDescripcion.clear();
        limpiarImagenProducto();
        txtLote.clear();
        dpCaducidad.setValue(null);
        txtCantidad.clear();
        cbPresentacion.setValue("pz");
        txtFactor.clear();
        txtCantidadUbicacion.clear();
        if (txtNota != null) {
            txtNota.clear();
        }
        txtPrecioEntrada.clear();
        txtPrecioIVA.clear();
        txtPrecioBruto.clear();
        txtPrecioTotal.clear();
        if (checkBoxIVA != null) {
            checkBoxIVA.setSelected(false);
        }
        limpiarUbicaciones();

        //cbClaveProducto.requestFocus();
        //reforzarLimpiezaCombosProducto();
    }

    private void limpiarCombosProducto() {
        limpiarCombo(cbClaveProducto);
        limpiarCombo(cbProductoNombre);
        limpiarCombo(cbClaveAlterna);
    }

    private void reforzarLimpiezaCombosProducto() {
        Platform.runLater(this::limpiarCombosProducto);
    }

    private void limpiarCombo(ComboBox<String> comboBox) {
        if (comboBox == null) {
            return;
        }
        comboBox.setValue(null);
        comboBox.getSelectionModel().clearSelection();
        if (comboBox.getEditor() != null) {
            comboBox.getEditor().clear();
        }
    }

    @FXML
    private void agregarProducto() {
        // Verificar que haya proveedor seleccionado
        if (proveedorId == null || proveedorId.isBlank() || proveedorNombre == null) {
            mostrarAlerta("Advertencia", "Debe seleccionar un proveedor antes de agregar un producto.");
            return;
        }

        try {
            // 1. Abrir formulario de nuevo producto
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Formularios/view/nuevoProducto.fxml"));
            Parent root = loader.load();
            controllerNuevoProducto ctrl = loader.getController();

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Nuevo producto");
            stage.setScene(new Scene(root));
            stage.initOwner(btnGuardar.getScene().getWindow());

            // Mostrar y esperar
            stage.showAndWait();

            // 2. Obtener el producto creado
            String productoId = ctrl.getProductoIdCreado();
            String productoNombre = ctrl.getProductoNombreCreado();

            if (productoId != null && !productoId.isEmpty() &&
                    productoNombre != null && !productoNombre.isEmpty()) {

                // 3. Recargar el controlador de productos para que incluya el nuevo
                if (productoController != null) {
                    productoController.recargarConProveedor(proveedorId);
                }

                // 4. Mostrar mensaje y abrir formulario de claves con proveedor Y producto
                mostrarAlertaSinEspera("Éxito", "Producto creado. Ahora vincule una clave alterna.");

                // 5. Abrir formulario de claves con proveedor Y producto
                abrirFormularioClavesConProducto(null, false, proveedorId, proveedorNombre,
                        productoId, productoNombre);
            }

        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta("Error", "No se pudo abrir el formulario de producto.");
        }
    }

    @FXML
    private void vincularProducto() {
        // Verificar que haya proveedor seleccionado
        if (proveedorId == null || proveedorId.isBlank() || proveedorNombre == null) {
            mostrarAlerta("Advertencia", "Debe seleccionar un proveedor antes de vincular un producto.");
            return;
        }

        abrirFormularioClaves(null, false, proveedorId, proveedorNombre);
    }

    private void abrirFormularioClaves(String[] fila, boolean esEdicion, String proveedorId, String proveedorNombre) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Formularios/view/sincronizarClaves.fxml"));
            Parent vista = loader.load();

            controllerSincronizacionClaves ctrl = loader.getController();

            // Pasar referencia a este controlador para comunicación
            ctrl.setParentController(this);

            // Pasar los datos del proveedor al controlador
            if (proveedorId != null && proveedorNombre != null) {
                ctrl.setProveedorSeleccionado(proveedorId, proveedorNombre);
            }

            if (esEdicion && fila != null) {
                ctrl.cargarParaEdicion(fila);
            }

            // Cambiar título según si es edición o nuevo
            String titulo = esEdicion ? "Editar Clave" : "Nueva Clave";

            Stage stage = new Stage();
            stage.setTitle(titulo);
            stage.setScene(new Scene(vista));
            stage.setResizable(false);

            // Configurar como modal para bloquear la pantalla principal
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(btnGuardar.getScene().getWindow());

            // Centrar la ventana
            stage.centerOnScreen();

            stage.showAndWait();

            // Recargar productos después de cerrar el formulario
            if (productoController != null) {
                productoController.recargarConProveedor(this.proveedorId);
            }

        } catch (IOException e) {
            e.printStackTrace();
            mostrarAlerta("Error", "No se pudo abrir el formulario de sincronización.");
        }
    }

    private void abrirFormularioClavesConProducto(String[] fila, boolean esEdicion,
                                                  String proveedorId, String proveedorNombre,
                                                  String productoId, String productoNombre) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Formularios/view/sincronizarClaves.fxml"));
            Parent vista = loader.load();

            controllerSincronizacionClaves ctrl = loader.getController();

            // Pasar referencia a este controlador para comunicación
            ctrl.setParentController(this);

            // Pasar los datos del proveedor al controlador
            if (proveedorId != null && proveedorNombre != null) {
                ctrl.setProveedorSeleccionado(proveedorId, proveedorNombre);
            }

            // Pasar los datos del producto recién creado
            if (productoId != null && productoNombre != null) {
                ctrl.setProductoSeleccionado(productoId, productoNombre);
            }

            if (esEdicion && fila != null) {
                ctrl.cargarParaEdicion(fila);
            }

            // Cambiar título
            String titulo = "Vincular Clave - " + productoNombre;
            if (proveedorNombre != null) {
                titulo += " (" + proveedorNombre + ")";
            }

            Stage stage = new Stage();
            stage.setTitle(titulo);
            stage.setScene(new Scene(vista));
            stage.setResizable(false);
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(btnGuardar.getScene().getWindow());
            stage.centerOnScreen();

            stage.showAndWait();

            // Recargar productos después de cerrar el formulario
            if (productoController != null) {
                productoController.recargarConProveedor(this.proveedorId);
            }

        } catch (IOException e) {
            e.printStackTrace();
            mostrarAlerta("Error", "No se pudo abrir el formulario de sincronización.");
        }
    }

    private void cargarItemParaEditar() {
        if (itemParaEditar == null) {
            return;
        }

        cbClaveProducto.setValue(itemParaEditar.getClaveProducto());
        cbProductoNombre.setValue(itemParaEditar.getProducto());
        cbClaveAlterna.setValue(itemParaEditar.getClaveAlterna());
        txtDescripcion.setText(itemParaEditar.getDescripcion());
        if (txtNota != null) {
            txtNota.setText(itemParaEditar.getNota());
        }
        txtLote.setText(itemParaEditar.getLote());
        configurarCaducidadDesdeTexto(itemParaEditar.getCaducidad());
        txtCantidad.setText(String.valueOf(itemParaEditar.getCantidad()));
        cbPresentacion.setValue(itemParaEditar.getPresentacion());
        txtFactor.setText(itemParaEditar.getFactor());
        txtPrecioEntrada.setText(itemParaEditar.getPrecioEntrada());
        if (checkBoxIVA != null) {
            checkBoxIVA.setSelected(itemParaEditar.isAplicaIva());
        }
        cargarUbicacionesParaEdicion(itemParaEditar.getUbicaciones());
        recalcularPrecios();
    }

    @FXML
    private void agregarUbicacion() {
        agregarFilaUbicacion();
    }

    private void configurarUbicacionesIniciales() {
        filasUbicaciones.clear();
        if (comboUbicacion == null || txtCantidadUbicacion == null) {
            return;
        }
        HBox fila = obtenerFilaContenedora(comboUbicacion);
        UbicacionRow row = new UbicacionRow(fila, comboUbicacion, txtCantidadUbicacion, true);
        filasUbicaciones.add(row);
        configurarComboUbicacion(comboUbicacion);
        configurarCampoCantidadUbicacion(txtCantidadUbicacion);
        actualizarCombosUbicacion();
    }

    private void agregarFilaUbicacion() {
        if (contenedorUbicaciones == null || filasUbicaciones.size() >= MAX_FILAS) {
            return;
        }

        ComboBox<String> nuevoCombo = new ComboBox<>();
        nuevoCombo.setEditable(true);
        nuevoCombo.setPromptText("Escribe o selecciona una ubicación");

        TextField nuevaCantidad = new TextField();

        VBox vboxUbicacion = new VBox(5);
        vboxUbicacion.getChildren().addAll(new Label("Ubicación:"), nuevoCombo);
        HBox.setHgrow(vboxUbicacion, Priority.ALWAYS);

        VBox vboxCantidad = new VBox(5);
        vboxCantidad.getChildren().addAll(new Label("Cantidad en ubicación:"), nuevaCantidad);
        HBox.setHgrow(vboxCantidad, Priority.ALWAYS);

        Button btnEliminar = new Button("-");
        btnEliminar.setMinWidth(30);
        btnEliminar.setPrefWidth(30);
        btnEliminar.setStyleClass("botonAgregarUbi");
        VBox vboxBoton = new VBox(5);
        vboxBoton.getChildren().addAll(new Pane(), btnEliminar);

        HBox nuevaFila = new HBox(20, vboxUbicacion, vboxCantidad, vboxBoton);
        contenedorUbicaciones.getChildren().add(nuevaFila);

        UbicacionRow row = new UbicacionRow(nuevaFila, nuevoCombo, nuevaCantidad, false);
        filasUbicaciones.add(row);
        configurarComboUbicacion(nuevoCombo);
        configurarCampoCantidadUbicacion(nuevaCantidad);
        actualizarComboUbicacion(nuevoCombo);

        btnEliminar.setOnAction(event -> eliminarFilaUbicacion(row));
    }

    private void eliminarFilaUbicacion(UbicacionRow row) {
        if (row == null || row.esPrimaria) {
            return;
        }
        contenedorUbicaciones.getChildren().remove(row.fila);
        filasUbicaciones.remove(row);
    }

    private void configurarComboUbicacion(ComboBox<String> combo) {
        if (combo == null) {
            return;
        }
        combo.setEditable(true);
        combo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.isBlank()) {
                agregarUbicacionSiNoExiste(newVal);
            }
        });
        if (combo.getEditor() != null) {
            combo.getEditor().focusedProperty().addListener((obs, oldVal, newVal) -> {
                if (!newVal) {
                    confirmarTextoUbicacion(combo);
                }
            });
        }
        combo.setOnAction(event -> confirmarTextoUbicacion(combo));
    }

    private void configurarCampoCantidadUbicacion(TextField campo) {
        if (campo == null) {
            return;
        }
        configurarValidadorCampo(campo, "entero");
    }

    private void confirmarTextoUbicacion(ComboBox<String> combo) {
        if (combo == null) {
            return;
        }
        String texto = combo.getEditor() != null ? combo.getEditor().getText() : null;
        String valor = combo.getValue() != null ? combo.getValue() : texto;
        if (valor == null || valor.isBlank()) {
            return;
        }
        combo.setValue(valor);
        if (combo.getEditor() != null) {
            combo.getEditor().setText(valor);
        }
        agregarUbicacionSiNoExiste(valor);
        actualizarCombosUbicacion();
    }

    private void agregarUbicacionSiNoExiste(String ubicacion) {
        if (ubicacion == null || ubicacion.isBlank()) {
            return;
        }
        String normalizada = ubicacion.trim();
        if (normalizada.isBlank()) {
            return;
        }
        if (!ubicaciones.contains(normalizada)) {
            ubicaciones.add(normalizada);
        }
    }

    private void actualizarCombosUbicacion() {
        for (UbicacionRow row : filasUbicaciones) {
            actualizarComboUbicacion(row.combo);
        }
    }

    private void actualizarComboUbicacion(ComboBox<String> combo) {
        if (combo == null) {
            return;
        }
        combo.setItems(FXCollections.observableArrayList(ubicaciones));
    }

    private void limpiarUbicaciones() {
        if (contenedorUbicaciones == null) {
            return;
        }
        for (UbicacionRow row : new ArrayList<>(filasUbicaciones)) {
            if (!row.esPrimaria) {
                contenedorUbicaciones.getChildren().remove(row.fila);
                filasUbicaciones.remove(row);
            }
        }
        if (comboUbicacion != null) {
            comboUbicacion.setValue(null);
            if (comboUbicacion.getEditor() != null) {
                comboUbicacion.getEditor().clear();
            }
            actualizarComboUbicacion(comboUbicacion);
        }
        if (txtCantidadUbicacion != null) {
            txtCantidadUbicacion.clear();
        }
    }

    private void cargarUbicacionesParaEdicion(List<UbicacionCompra> ubicacionesLista) {
        limpiarUbicaciones();
        if (ubicacionesLista == null || ubicacionesLista.isEmpty()) {
            return;
        }
        for (int i = 0; i < ubicacionesLista.size(); i++) {
            UbicacionCompra ubicacion = ubicacionesLista.get(i);
            if (ubicacion == null) {
                continue;
            }
            if (i > 0) {
                agregarFilaUbicacion();
            }
            if (i < filasUbicaciones.size()) {
                UbicacionRow row = filasUbicaciones.get(i);
                row.combo.setValue(ubicacion.getUbicacion());
                if (row.combo.getEditor() != null) {
                    row.combo.getEditor().setText(ubicacion.getUbicacion());
                }
                row.cantidad.setText(String.valueOf(ubicacion.getCantidad()));
            }
        }
    }

    private helperCompraEmergente.ResultadoValidacion validarUbicacionesSeleccionadas(int cantidadTotal) {
        int suma = 0;
        boolean hayUbicaciones = false;

        for (UbicacionRow row : filasUbicaciones) {
            String ubicacion = obtenerTextoCombo(row.combo);
            String cantidadTexto = row.cantidad.getText();

            if (esVacio(ubicacion) && esVacio(cantidadTexto)) {
                continue;
            }

            if (esVacio(ubicacion) || esVacio(cantidadTexto)) {
                return new helperCompraEmergente.ResultadoValidacion(
                        false,
                        "Debe completar todas las ubicaciones y cantidades."
                );
            }

            hayUbicaciones = true;
            try {
                int cantidad = Integer.parseInt(cantidadTexto.trim());
                suma += cantidad;
            } catch (NumberFormatException ignored) {
                return new helperCompraEmergente.ResultadoValidacion(
                        false,
                        "Las cantidades por ubicación deben ser numéricas."
                );
            }
        }

        if (!hayUbicaciones) {
            return new helperCompraEmergente.ResultadoValidacion(
                    false,
                    "Debe capturar al menos una ubicación."
            );
        }

        if (suma != cantidadTotal) {
            return new helperCompraEmergente.ResultadoValidacion(
                    false,
                    "La suma de cantidades por ubicación debe ser igual a la cantidad total."
            );
        }

        return new helperCompraEmergente.ResultadoValidacion(true, "");
    }

    private String obtenerTextoCombo(ComboBox<String> combo) {
        if (combo == null) {
            return "";
        }
        if (combo.getValue() != null && !combo.getValue().isBlank()) {
            return combo.getValue();
        }
        if (combo.getEditor() != null) {
            return combo.getEditor().getText();
        }
        return "";
    }

    private HBox obtenerFilaContenedora(ComboBox<String> combo) {
        if (combo == null) {
            return null;
        }
        javafx.scene.Node nodo = combo;
        while (nodo != null && !(nodo instanceof HBox)) {
            nodo = nodo.getParent();
        }
        return (HBox) nodo;
    }

    private static class UbicacionRow {
        private final HBox fila;
        private final ComboBox<String> combo;
        private final TextField cantidad;
        private final boolean esPrimaria;

        private UbicacionRow(HBox fila, ComboBox<String> combo, TextField cantidad, boolean esPrimaria) {
            this.fila = fila;
            this.combo = combo;
            this.cantidad = cantidad;
            this.esPrimaria = esPrimaria;
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
        String cantidadStr = txtCantidad.getText();
        String precioEntradaStr = txtPrecioEntrada.getText();
        boolean aplicaIva = checkBoxIVA != null && checkBoxIVA.isSelected();

        helperCompraEmergente.ResultadoCalculo resultado =
                helperCompraEmergente.calcularPrecios(cantidadStr, precioEntradaStr, aplicaIva);

        txtPrecioIVA.setText(resultado.getPrecioConIvaFormateado());
        txtPrecioBruto.setText(resultado.getPrecioBrutoFormateado());
        txtPrecioTotal.setText(resultado.getPrecioTotalFormateado());
    }

    private String formatearDecimal(BigDecimal valor) {
        if (valor == null) {
            return "0.00";
        }
        return valor.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String obtenerCaducidadTexto() {
        if (dpCaducidad == null || dpCaducidad.getValue() == null) {
            return "";
        }
        return dpCaducidad.getValue().format(FECHA_FORMATO);
    }

    private boolean esProductoReactivo() {
        if (productoController == null) {
            return false;
        }
        String categoria = productoController.getCategoriaSeleccionada();
        if (categoria == null) {
            return false;
        }
        String categoriaNormalizada = categoria.trim().toLowerCase();
        return categoriaNormalizada.startsWith("reactiv");
    }

    private void configurarCaducidadDesdeTexto(String caducidad) {
        if (dpCaducidad == null || caducidad == null || caducidad.isBlank()) {
            dpCaducidad.setValue(null);
            return;
        }
        try {
            dpCaducidad.setValue(LocalDate.parse(caducidad.trim(), FECHA_FORMATO));
        } catch (Exception e) {
            dpCaducidad.setValue(null);
        }
    }

    private void cerrarFormulario() {
        if (btnGuardar == null || btnGuardar.getScene() == null) {
            return;
        }
        Stage stage = (Stage) btnGuardar.getScene().getWindow();
        stage.close();
    }
}
