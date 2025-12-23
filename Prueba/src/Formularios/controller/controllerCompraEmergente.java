package Formularios.controller;

import Compartido.controller.productoCboxController;
import Compartido.model.ProductoComboItem;
import Operaciones.compra.controller.MainController;
import Operaciones.compra.model.itemCompra;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

public class controllerCompraEmergente {

    @FXML private ComboBox<String> cbClaveProducto;
    @FXML private ComboBox<String> cbClaveAlterna;
    @FXML private ComboBox<String> cbProductoNombre;
    @FXML private TextField txtDescripcion;
    @FXML private TextField txtLote;
    @FXML private TextField txtCaducidad;
    @FXML private TextField txtCantidad;
    @FXML private ComboBox<String> cbPresentacion;
    @FXML private TextField txtFactor;
    @FXML private VBox contenedorUbicaciones;
    @FXML private ComboBox<String> comboUbicacion;
    @FXML private TextField txtCantidadUbicacion;
    @FXML private TextField txtPrecioEntrada;
    @FXML private CheckBox checkBoxIVA;
    @FXML private TextField txtPrecioIVA;
    @FXML private TextField txtPrecioBruto;
    @FXML private TextField txtPrecioTotal;
    @FXML private Button btnGuardar;

    private int contadorFilas = 1;
    private final int MAX_FILAS = 10;

    private final ObservableList<String> ubicaciones = FXCollections.observableArrayList(
            "Almacén Principal", "Estante A1", "Estante A2", "Refrigerador", "Mostrador", "Depósito", "Sucursal Norte"
    );

    private final ObservableList<String> presentaciones = FXCollections.observableArrayList(
            "paquete", "pz", "caja", "bolsa", "rollo", "litro", "kilogramo", "metro", "unidad"
    );

    private ObservableList<itemCompra> itemsCompra;
    private MainController mainController;
    private int proveedorId;
    private List<ProductoComboItem> productosDisponibles = new ArrayList<>();

    private productoCboxController productoController;

    @FXML
    public void initialize() {
        productoController = new productoCboxController();
        productoController.inicializar(cbClaveProducto, cbProductoNombre, cbClaveAlterna);
        if (!productosDisponibles.isEmpty()) {
            productoController.setProductos(productosDisponibles);
        }

        cbPresentacion.setItems(presentaciones);
        cbPresentacion.setValue("pz");

        configurarEventosProducto();
        configurarAutocompletado(comboUbicacion);
        configurarValidaciones();

        btnGuardar.setOnAction(e -> guardarItem());
    }

    public void setItemsCompra(ObservableList<itemCompra> itemsCompra) {
        this.itemsCompra = itemsCompra;
    }

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    public void setProveedorSeleccionado(int proveedorId) {
        this.proveedorId = proveedorId;
    }

    public void setProductosDisponibles(List<ProductoComboItem> productos) {
        if (productos != null) {
            productosDisponibles = productos;
        } else {
            productosDisponibles = new ArrayList<>();
        }
        if (productoController != null && !productosDisponibles.isEmpty()) {
            productoController.setProductos(productosDisponibles);
        }
    }

    @FXML
    private void agregarUbicacion() {
        if (contadorFilas >= MAX_FILAS) {
            mostrarAlerta("Límite alcanzado", "Solo se pueden agregar hasta " + MAX_FILAS + " ubicaciones.");
            return;
        }

        HBox nuevaFila = new HBox(20);

        VBox vboxUbicacion = new VBox(5);
        Label lblUbicacion = new Label("Ubicación:");
        ComboBox<String> nuevoCombo = new ComboBox<>(ubicaciones);
        nuevoCombo.setEditable(true);
        nuevoCombo.setPromptText("Escribe o selecciona una ubicación");
        configurarAutocompletado(nuevoCombo);
        vboxUbicacion.getChildren().addAll(lblUbicacion, nuevoCombo);
        HBox.setHgrow(vboxUbicacion, Priority.ALWAYS);

        VBox vboxCantidad = new VBox(5);
        Label lblCantidad = new Label("Cantidad en ubicación:");
        TextField txtCantidad = new TextField();
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

        contadorFilas++;
    }

    private void manejarEliminar(ActionEvent event) {
        Button botonPresionado = (Button) event.getSource();
        VBox contenedorBoton = (VBox) botonPresionado.getParent();
        HBox fila = (HBox) contenedorBoton.getParent();

        contenedorUbicaciones.getChildren().remove(fila);
        contadorFilas--;
    }

    private void configurarAutocompletado(ComboBox<String> comboBox) {
        comboBox.setItems(FXCollections.observableArrayList(ubicaciones));

        comboBox.getEditor().textProperty().addListener((obs, oldValue, newValue) -> {
            if (!comboBox.isShowing()) comboBox.show();

            ObservableList<String> filtrados = FXCollections.observableArrayList();
            for (String item : ubicaciones) {
                if (item.toLowerCase().contains(newValue.toLowerCase())) {
                    filtrados.add(item);
                }
            }
            comboBox.setItems(filtrados);
        });

        comboBox.getEditor().setOnKeyPressed(event -> {
            switch (event.getCode()) {
                case TAB:
                case ENTER:
                    if (!comboBox.getItems().isEmpty()) {
                        comboBox.setValue(comboBox.getEditor().getText());
                        comboBox.hide();
                    }
                    break;
            }
        });
    }

    private void configurarEventosProducto() {
        cbClaveProducto.valueProperty().addListener((obs, oldVal, newVal) -> actualizarDescripcion());
        cbProductoNombre.valueProperty().addListener((obs, oldVal, newVal) -> actualizarDescripcion());
        cbClaveAlterna.valueProperty().addListener((obs, oldVal, newVal) -> actualizarDescripcion());
    }

    private void actualizarDescripcion() {
        String descripcion = productoController.getDescripcionSeleccionada();
        txtDescripcion.setText(descripcion);
    }

    private void configurarValidaciones() {
        txtCantidad.textProperty().addListener((obs, old, val) -> {
            if (!val.matches("\\d*")) {
                txtCantidad.setText(val.replaceAll("[^\\d]", ""));
            }
        });
        txtFactor.textProperty().addListener((obs, old, val) -> {
            if (!val.matches("\\d*(\\.\\d*)?")) {
                txtFactor.setText(val.replaceAll("[^\\d.]", ""));
            }
        });
        txtPrecioEntrada.textProperty().addListener((obs, old, val) -> {
            if (!val.matches("\\d*(\\.\\d*)?")) {
                txtPrecioEntrada.setText(val.replaceAll("[^\\d.]", ""));
            }
        });
        txtPrecioIVA.textProperty().addListener((obs, old, val) -> {
            if (!val.matches("\\d*(\\.\\d*)?")) {
                txtPrecioIVA.setText(val.replaceAll("[^\\d.]", ""));
            }
        });
        txtPrecioBruto.textProperty().addListener((obs, old, val) -> {
            if (!val.matches("\\d*(\\.\\d*)?")) {
                txtPrecioBruto.setText(val.replaceAll("[^\\d.]", ""));
            }
        });
        txtPrecioTotal.textProperty().addListener((obs, old, val) -> {
            if (!val.matches("\\d*(\\.\\d*)?")) {
                txtPrecioTotal.setText(val.replaceAll("[^\\d.]", ""));
            }
        });
    }

    private void guardarItem() {
        if (itemsCompra == null) {
            mostrarAlerta("Error", "No se pudo conectar con la tabla principal.");
            return;
        }

        String claveProducto = productoController.getIdSeleccionado();
        String nombreProducto = productoController.getNombreSeleccionado();
        String claveAlterna = productoController.getClaveAlternaSeleccionada();
        String descripcion = productoController.getDescripcionSeleccionada();
        String lote = txtLote.getText();
        String caducidad = txtCaducidad.getText();
        String cantidadText = txtCantidad.getText();
        String presentacion = cbPresentacion.getValue();
        String factor = txtFactor.getText();

        if (claveProducto == null || claveProducto.isBlank() ||
                nombreProducto == null || nombreProducto.isBlank() ||
                cantidadText == null || cantidadText.isBlank()) {
            mostrarAlerta("Advertencia", "Complete los campos obligatorios.");
            return;
        }

        if (!productoController.validarSeleccion()) {
            mostrarAlerta("Error", "El ID y el nombre del producto no corresponden.");
            return;
        }

        int cantidad = parseEntero(cantidadText);
        if (cantidad <= 0) {
            mostrarAlerta("Advertencia", "La cantidad debe ser mayor a 0.");
            return;
        }

        String ubicacionResumen = construirResumenUbicaciones();
        double precioEntrada = parseDecimal(txtPrecioEntrada.getText());
        double precioIva = parseDecimal(txtPrecioIVA.getText());
        double precioBruto = parseDecimal(txtPrecioBruto.getText());
        double precioTotal = parseDecimal(txtPrecioTotal.getText());

        if (presentacion == null || presentacion.isBlank()) {
            presentacion = "pz";
        }

        itemCompra item = new itemCompra(
                claveProducto,
                claveAlterna == null ? "" : claveAlterna,
                nombreProducto,
                descripcion,
                lote == null ? "" : lote,
                caducidad == null ? "" : caducidad,
                ubicacionResumen,
                cantidad,
                presentacion,
                factor == null ? "" : factor,
                precioEntrada,
                precioIva,
                precioBruto,
                precioTotal
        );

        itemsCompra.add(item);
        if (mainController != null) {
            mainController.refrescarTabla();
        }

        limpiarFormulario();
    }

    private String construirResumenUbicaciones() {
        List<String> partes = new ArrayList<>();

        if (comboUbicacion != null && comboUbicacion.getEditor() != null) {
            String ubicacion = comboUbicacion.getEditor().getText();
            String cantidad = txtCantidadUbicacion == null ? "" : txtCantidadUbicacion.getText();
            if (ubicacion != null && !ubicacion.isBlank()) {
                partes.add(formatearUbicacion(ubicacion, cantidad));
            }
        }

        for (int i = 1; i < contenedorUbicaciones.getChildren().size(); i++) {
            if (!(contenedorUbicaciones.getChildren().get(i) instanceof HBox)) continue;
            HBox fila = (HBox) contenedorUbicaciones.getChildren().get(i);
            if (fila.getChildren().size() < 2) continue;

            VBox vboxUbicacion = (VBox) fila.getChildren().get(0);
            VBox vboxCantidad = (VBox) fila.getChildren().get(1);
            ComboBox<String> combo = (ComboBox<String>) vboxUbicacion.getChildren().get(1);
            TextField txtCantidadFila = (TextField) vboxCantidad.getChildren().get(1);

            String ubicacion = combo.getEditor().getText();
            String cantidad = txtCantidadFila.getText();
            if (ubicacion != null && !ubicacion.isBlank()) {
                partes.add(formatearUbicacion(ubicacion, cantidad));
            }
        }

        return String.join(", ", partes);
    }

    private String formatearUbicacion(String ubicacion, String cantidad) {
        if (cantidad == null || cantidad.isBlank()) {
            return ubicacion.trim();
        }
        return ubicacion.trim() + " (" + cantidad.trim() + ")";
    }

    private int parseEntero(String valor) {
        try {
            return Integer.parseInt(valor.trim());
        } catch (Exception e) {
            return 0;
        }
    }

    private double parseDecimal(String valor) {
        try {
            if (valor == null || valor.isBlank()) return 0.0;
            return Double.parseDouble(valor.trim());
        } catch (Exception e) {
            return 0.0;
        }
    }

    private void limpiarFormulario() {
        productoController.limpiarSeleccion();
        txtDescripcion.clear();
        txtLote.clear();
        txtCaducidad.clear();
        txtCantidad.clear();
        cbPresentacion.setValue("pz");
        txtFactor.clear();
        comboUbicacion.getEditor().clear();
        txtCantidadUbicacion.clear();
        txtPrecioEntrada.clear();
        checkBoxIVA.setSelected(false);
        txtPrecioIVA.clear();
        txtPrecioBruto.clear();
        txtPrecioTotal.clear();
    }

    private void mostrarAlerta(String titulo, String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.INFORMATION);
        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }
}
