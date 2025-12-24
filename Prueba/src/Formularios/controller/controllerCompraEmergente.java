package Formularios.controller;

import Compartido.controller.productoCboxController;
import Operaciones.compra.controller.MainController;
import Operaciones.compra.model.UbicacionCompra;
import Operaciones.compra.model.compra;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.input.KeyCode;

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
    @FXML private TextField txtCaducidad;
    @FXML private TextField txtCantidad;
    @FXML private ComboBox<String> cbPresentacion;
    @FXML private TextField txtFactor;
    @FXML private TextField txtCantidadUbicacion;
    @FXML private TextField txtPrecioEntrada;
    @FXML private CheckBox checkBoxIVA;
    @FXML private TextField txtPrecioIVA;
    @FXML private TextField txtPrecioBruto;
    @FXML private TextField txtPrecioTotal;
    @FXML private Button btnGuardar;

    private int contadorFilas = 1;
    private static final int MAX_FILAS = 10;

    // Lista base de ubicaciones (ejemplo)
    private final ObservableList<String> ubicaciones = FXCollections.observableArrayList(
            "Almacén Principal", "Estante A1", "Estante A2", "Refrigerador", "Mostrador", "Depósito", "Sucursal Norte"
    );

    private final ObservableList<String> presentaciones = FXCollections.observableArrayList(
            "paquete", "pz", "caja", "bolsa", "pieza", "rollo", "litro", "kilogramo", "metro", "unidad"
    );

    private ObservableList<compra> itemsCompra;
    private MainController mainController;
    private productoCboxController productoController;
    private String proveedorId;
    private String proveedorNombre;
    private boolean inicializado = false;

    @FXML
    public void initialize() {
        productoController = new productoCboxController();
        if (proveedorId != null && !proveedorId.isBlank()) {
            productoController.inicializarConProveedor(cbClaveProducto, cbProductoNombre, cbClaveAlterna, proveedorId);
        } else {
            productoController.inicializar(cbClaveProducto, cbProductoNombre, cbClaveAlterna);
        }

        configurarPresentaciones();
        configurarAutocompletado(comboUbicacion);
        configurarEventos();
        configurarValidaciones();
        configurarManejoEnter();

        inicializado = true;

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

    private void configurarPresentaciones() {
        cbPresentacion.setItems(presentaciones);
        cbPresentacion.setValue("pz");
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
        validarNumerosEnteros(txtCantidad);
        validarNumerosEnteros(txtCantidadUbicacion);
        validarDecimal(txtFactor);
        validarDecimal(txtPrecioEntrada);
        validarDecimal(txtPrecioIVA);
        validarDecimal(txtPrecioBruto);
        validarDecimal(txtPrecioTotal);
    }

    private void validarNumerosEnteros(TextField campo) {
        campo.textProperty().addListener((obs, old, val) -> {
            if (!val.matches("\\d*")) {
                campo.setText(val.replaceAll("[^\\d]", ""));
            }
        });
    }

    private void validarDecimal(TextField campo) {
        campo.textProperty().addListener((obs, old, val) -> {
            if (!val.matches("\\d*(\\.\\d*)?")) {
                campo.setText(val.replaceAll("[^\\d.]", ""));
                if (val.chars().filter(ch -> ch == '.').count() > 1) {
                    int firstDot = val.indexOf('.');
                    campo.setText(val.substring(0, firstDot + 1) +
                            val.substring(firstDot + 1).replace(".", ""));
                }
            }
        });
    }

    private void actualizarDescripcionDesdeProducto() {
        String descripcion = productoController.getDescripcionSeleccionada();
        txtDescripcion.setText(descripcion);
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
        String caducidad = txtCaducidad.getText();
        String cantidadTexto = txtCantidad.getText();
        String presentacion = cbPresentacion.getValue();
        String factor = txtFactor.getText();
        String precioEntrada = txtPrecioEntrada.getText();
        String precioIVA = txtPrecioIVA.getText();
        String precioBruto = txtPrecioBruto.getText();
        String precioTotal = txtPrecioTotal.getText();

        if (clave == null || clave.isBlank() ||
                nombre == null || nombre.isBlank() ||
                cantidadTexto == null || cantidadTexto.isBlank()) {
            mostrarAlerta("Advertencia", "Complete los campos obligatorios de producto y cantidad.");
            return;
        }

        if (!productoController.validarSeleccion()) {
            mostrarAlerta("Error", "El ID y el nombre del producto no corresponden.\n" +
                    "Por favor, verifique la selección.");
            return;
        }

        int cantidad;
        try {
            cantidad = Integer.parseInt(cantidadTexto);
            if (cantidad <= 0) {
                mostrarAlerta("Advertencia", "La cantidad debe ser mayor a 0");
                return;
            }
        } catch (NumberFormatException e) {
            mostrarAlerta("Error", "La cantidad debe ser un número válido");
            return;
        }

        List<UbicacionCompra> ubicacionesSeleccionadas = obtenerUbicacionesSeleccionadas();
        if (ubicacionesSeleccionadas.isEmpty()) {
            mostrarAlerta("Advertencia", "Debe capturar al menos una ubicación con cantidad.");
            return;
        }

        if (presentacion == null || presentacion.isBlank()) {
            presentacion = "pz";
        }

        compra item = new compra(
                clave,
                nombre,
                descripcion,
                lote,
                caducidad,
                cantidad,
                claveAlterna,
                presentacion,
                factor,
                ubicacionesSeleccionadas,
                precioEntrada,
                precioIVA,
                precioBruto,
                precioTotal,
                checkBoxIVA != null && checkBoxIVA.isSelected(),
                proveedorId,
                proveedorNombre
        );

        itemsCompra.add(item);

        if (mainController != null) {
            mainController.refrescarTabla();
        }

        mostrarAlertaSinEspera("Éxito", "Producto agregado a la compra");
        limpiarFormularioParaNuevo();
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

    private void limpiarFormularioParaNuevo() {
        productoController.limpiarSeleccion();
        txtDescripcion.clear();
        txtLote.clear();
        txtCaducidad.clear();
        txtCantidad.clear();
        cbPresentacion.setValue("pz");
        txtFactor.clear();
        txtCantidadUbicacion.clear();
        txtPrecioEntrada.clear();
        txtPrecioIVA.clear();
        txtPrecioBruto.clear();
        txtPrecioTotal.clear();
        if (checkBoxIVA != null) {
            checkBoxIVA.setSelected(false);
        }

        while (contenedorUbicaciones.getChildren().size() > 1) {
            contenedorUbicaciones.getChildren().remove(1);
        }
        contadorFilas = 1;
        comboUbicacion.setValue(null);

        cbClaveProducto.requestFocus();
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
        nuevoCombo.setEditable(true);
        nuevoCombo.setPromptText("Escribe o selecciona una ubicación");
        configurarAutocompletado(nuevoCombo);
        vboxUbicacion.getChildren().addAll(lblUbicacion, nuevoCombo);
        HBox.setHgrow(vboxUbicacion, Priority.ALWAYS);

        VBox vboxCantidad = new VBox(5);
        Label lblCantidad = new Label("Cantidad en ubicación:");
        TextField txtCantidad = new TextField();
        validarNumerosEnteros(txtCantidad);
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
}
