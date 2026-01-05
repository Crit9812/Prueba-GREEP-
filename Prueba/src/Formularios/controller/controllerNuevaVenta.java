package Formularios.controller;

import Compartido.controller.productoCboxController;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class controllerNuevaVenta {

    private static final BigDecimal IVA_TASA = new BigDecimal("0.16");

    @FXML private VBox contenedorUbicaciones;
    @FXML private ComboBox<String> comboUbicacion;
    @FXML private Button btnAgregarUbi;
    @FXML private ComboBox<String> cbClaveProducto;
    @FXML private ComboBox<String> cbClaveAlterna;
    @FXML private ComboBox<String> cbProductoNombre;
    @FXML private TextField txtDescripcion;
    @FXML private TextField txtCantidad;
    @FXML private TextField txtPrecioEntrada;
    @FXML private TextField txtPrecioSalida;
    @FXML private CheckBox checkBoxIVA;
    @FXML private TextField txtPrecioIVA;
    @FXML private TextField txtPrecioBruto;
    @FXML private TextField txtPrecioTotal;

    private int contadorFilas = 1;
    private static final int MAX_FILAS = 10;

    private final ObservableList<String> ubicaciones = FXCollections.observableArrayList(
            "Mostrador", "Almacén Principal", "Sucursal Norte", "Sucursal Centro", "Refrigerador", "Vitrina"
    );

    private productoCboxController productoController;

    @FXML
    public void initialize() {
        productoController = new productoCboxController();
        productoController.inicializar(cbClaveProducto, cbProductoNombre, cbClaveAlterna);
        cbClaveProducto.valueProperty().addListener((obs, oldVal, newVal) -> actualizarDescripcionDesdeProducto());
        cbProductoNombre.valueProperty().addListener((obs, oldVal, newVal) -> actualizarDescripcionDesdeProducto());
        cbClaveAlterna.valueProperty().addListener((obs, oldVal, newVal) -> actualizarDescripcionDesdeProducto());

        configurarUbicacionesBase();
        configurarCalculoPrecios();
        configurarCamposLectura();
    }

    @FXML
    private void agregarUbicacion() {
        if (contadorFilas >= MAX_FILAS) {
            Alert alerta = new Alert(Alert.AlertType.INFORMATION);
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
        vboxUbicacion.getChildren().addAll(lblUbicacion, nuevoCombo);
        HBox.setHgrow(vboxUbicacion, Priority.ALWAYS);

        VBox vboxCantidad = new VBox(5);
        Label lblCantidad = new Label("Cantidad en ubicación:");
        TextField txtCantidadUbicacion = new TextField();
        vboxCantidad.getChildren().addAll(lblCantidad, txtCantidadUbicacion);
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

    private void configurarUbicacionesBase() {
        if (comboUbicacion != null) {
            comboUbicacion.setItems(ubicaciones);
            comboUbicacion.setEditable(false);
        }
    }

    private void configurarCalculoPrecios() {
        if (txtCantidad != null) {
            txtCantidad.textProperty().addListener((obs, oldVal, newVal) -> recalcularPrecios());
        }
        if (txtPrecioSalida != null) {
            txtPrecioSalida.textProperty().addListener((obs, oldVal, newVal) -> recalcularPrecios());
        }
        if (checkBoxIVA != null) {
            checkBoxIVA.selectedProperty().addListener((obs, oldVal, newVal) -> recalcularPrecios());
        }
    }

    private void configurarCamposLectura() {
        if (txtPrecioEntrada != null) {
            txtPrecioEntrada.setEditable(false);
        }
        if (txtPrecioIVA != null) {
            txtPrecioIVA.setEditable(false);
        }
        if (txtPrecioBruto != null) {
            txtPrecioBruto.setEditable(false);
        }
        if (txtPrecioTotal != null) {
            txtPrecioTotal.setEditable(false);
        }
    }

    private void actualizarDescripcionDesdeProducto() {
        if (txtDescripcion != null) {
            txtDescripcion.setText(productoController.getDescripcionSeleccionada());
        }
    }

    private void recalcularPrecios() {
        int cantidad = parseEntero(txtCantidad != null ? txtCantidad.getText() : null);
        BigDecimal precioSalida = parseDecimal(txtPrecioSalida != null ? txtPrecioSalida.getText() : null);

        BigDecimal precioConIva = precioSalida;
        if (checkBoxIVA != null && checkBoxIVA.isSelected()) {
            BigDecimal iva = precioSalida.multiply(IVA_TASA);
            precioConIva = precioSalida.add(iva);
        }

        BigDecimal precioBruto = precioSalida.multiply(BigDecimal.valueOf(cantidad));
        BigDecimal precioTotal = precioConIva.multiply(BigDecimal.valueOf(cantidad));

        if (txtPrecioIVA != null) {
            txtPrecioIVA.setText(formatearDecimal(precioConIva));
        }
        if (txtPrecioBruto != null) {
            txtPrecioBruto.setText(formatearDecimal(precioBruto));
        }
        if (txtPrecioTotal != null) {
            txtPrecioTotal.setText(formatearDecimal(precioTotal));
        }
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
}
