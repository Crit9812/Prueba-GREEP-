package Formularios.controller;

import Compartido.controller.productoCboxController;
import Formularios.utilities.UbicacionManager;
import Formularios.utilities.helperCompraEmergente;
import Operaciones.compra.controller.MainController;
import Operaciones.compra.model.UbicacionCompra;
import Operaciones.compra.model.compra;
import Operaciones.compra.model.model;
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
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.input.KeyCode;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class controllerCompraEmergente {

    @FXML private VBox contenedorUbicaciones;
    @FXML private ComboBox<String> comboUbicacion;
    @FXML private ComboBox<String> cbClaveProducto;
    @FXML private ComboBox<String> cbClaveAlterna;
    @FXML private ComboBox<String> cbProductoNombre;
    @FXML private TextField txtDescripcion;
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

    private UbicacionManager ubicacionManager;
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

    @FXML
    public void initialize() {
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
        configurarLimpiezaPorCampoVacio();
        configurarManejoEnter();

        ubicacionManager = new UbicacionManager(
                contenedorUbicaciones,
                comboUbicacion,
                txtCantidadUbicacion,
                ubicaciones
        );

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

                // IMPORTANTE: Configurar el combo principal con las ubicaciones cargadas
                if (comboUbicacion != null) {
                    comboUbicacion.setItems(FXCollections.observableArrayList(ubicaciones));
                }

                // Si ya existe el manager, actualizar todas las listas
                if (ubicacionManager != null) {
                    ubicacionManager.actualizarListaUbicaciones(ubicaciones);
                }
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
            }
        });

        cbProductoNombre.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                actualizarDescripcionDesdeProducto();
            }
        });

        cbClaveAlterna.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                actualizarDescripcionDesdeProducto();
            }
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

    private void limpiarSeleccionProducto() {
        productoController.limpiarSeleccion();
        txtDescripcion.clear();
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
        String descripcion = productoController.getDescripcionSeleccionada();
        txtDescripcion.setText(descripcion);
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

        // 2. Validar datos del formulario usando el helper
        List<UbicacionCompra> ubicacionesSeleccionadas = ubicacionManager.obtenerUbicaciones();

        helperCompraEmergente.ResultadoValidacion validacion =
                helperCompraEmergente.validarFormularioCompleto(
                        clave, nombre, cantidadTexto,
                        ubicacionesSeleccionadas, precioEntrada
                );

        if (!validacion.isValido()) {
            mostrarAlerta("Advertencia", validacion.getMensaje());
            return;
        }

        if (tieneUbicacionesDuplicadas(ubicacionesSeleccionadas)) {
            mostrarAlerta("Advertencia", "No se puede seleccionar la misma ubicación más de una vez.");
            return;
        }

        int cantidad = Integer.parseInt(cantidadTexto.trim());

        helperCompraEmergente.ResultadoValidacion validacionUbicaciones =
                ubicacionManager.validarUbicaciones(cantidad);
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

            String ubicacion = combo.getValue() != null ? combo.getValue().toString() : "";
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
        txtDescripcion.clear();
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

        // Limpiar ubicaciones usando el manager
        if (ubicacionManager != null) {
            ubicacionManager.limpiar();
        }

        // Limpiar manualmente el combo principal y cantidad como respaldo
        if (comboUbicacion != null) {
            comboUbicacion.setValue(null);
            if (comboUbicacion.getEditor() != null) {
                comboUbicacion.getEditor().clear();
            }
        }
        if (txtCantidadUbicacion != null) {
            txtCantidadUbicacion.clear();
        }

        cbClaveProducto.requestFocus();
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
        //cargarUbicaciones(itemParaEditar.getUbicaciones());
        ubicacionManager.cargarUbicaciones(itemParaEditar.getUbicaciones());
        recalcularPrecios();
    }

    @FXML private void agregarUbicacion() {ubicacionManager.agregarFilaUbicacion();}

    private void commitirSeleccionCombo(ComboBox<String> comboBox, boolean[] actualizando) {
        if (comboBox == null || actualizando[0]) {
            return;
        }
        String texto = comboBox.getEditor() != null ? comboBox.getEditor().getText() : null;
        String seleccion = comboBox.getSelectionModel().getSelectedItem();
        String valor = (seleccion != null && !seleccion.isBlank()) ? seleccion : texto;
        if (valor == null || valor.isBlank()) {
            return;
        }
        actualizando[0] = true;
        try {
            comboBox.setValue(valor);
            if (comboBox.getEditor() != null) {
                comboBox.getEditor().setText(valor);
            }
        } finally {
            actualizando[0] = false;
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

    private String obtenerCaducidadTexto() {
        if (dpCaducidad == null || dpCaducidad.getValue() == null) {
            return "";
        }
        return dpCaducidad.getValue().format(FECHA_FORMATO);
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
