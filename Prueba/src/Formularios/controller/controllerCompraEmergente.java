package Formularios.controller;

import Compartido.controller.productoCboxController;
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
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;

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
    @FXML private TextField txtPrecioEntrada;
    @FXML private CheckBox checkBoxIVA;
    @FXML private TextField txtPrecioIVA;
    @FXML private TextField txtPrecioBruto;
    @FXML private TextField txtPrecioTotal;
    @FXML private Button btnGuardar;
    @FXML private Button btnLimpiar;

    private int contadorFilas = 1;
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

    private static final BigDecimal IVA_TASA = new BigDecimal("0.16");
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
        configurarAutocompletado(comboUbicacion);
        configurarEventos();
        configurarValidaciones();
        configurarCalculoPrecios();
        configurarCamposLectura();
        cargarUbicacionesDesdeBD();
        configurarLimpiezaPorCampoVacio();
        configurarManejoEnter();

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
        String caducidad = obtenerCaducidadTexto();
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
        int sumaUbicaciones = ubicacionesSeleccionadas.stream()
                .mapToInt(UbicacionCompra::getCantidad)
                .sum();
        if (sumaUbicaciones != cantidad) {
            mostrarAlerta("Advertencia", "La suma de cantidades por ubicación debe ser igual a la cantidad total.");
            return;
        }

        if (presentacion == null || presentacion.isBlank()) {
            presentacion = "pz";
        }

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
            itemParaEditar.setPrecioEntrada(precioEntrada);
            itemParaEditar.setPrecioIva(precioIVA);
            itemParaEditar.setPrecioBruto(precioBruto);
            itemParaEditar.setPrecioTotal(precioTotal);
            itemParaEditar.setAplicaIva(checkBoxIVA != null && checkBoxIVA.isSelected());
        } else {
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

    private void limpiarFormularioParaNuevo() {
        productoController.limpiarSeleccion();
        txtDescripcion.clear();
        txtLote.clear();
        dpCaducidad.setValue(null);
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
        limpiarComboUbicacion(comboUbicacion);

        cbClaveProducto.requestFocus();
    }

    private void cargarItemParaEditar() {
        if (itemParaEditar == null) {
            return;
        }

        cbClaveProducto.setValue(itemParaEditar.getClaveProducto());
        cbProductoNombre.setValue(itemParaEditar.getProducto());
        cbClaveAlterna.setValue(itemParaEditar.getClaveAlterna());
        txtDescripcion.setText(itemParaEditar.getDescripcion());
        txtLote.setText(itemParaEditar.getLote());
        configurarCaducidadDesdeTexto(itemParaEditar.getCaducidad());
        txtCantidad.setText(String.valueOf(itemParaEditar.getCantidad()));
        cbPresentacion.setValue(itemParaEditar.getPresentacion());
        txtFactor.setText(itemParaEditar.getFactor());
        txtPrecioEntrada.setText(itemParaEditar.getPrecioEntrada());
        if (checkBoxIVA != null) {
            checkBoxIVA.setSelected(itemParaEditar.isAplicaIva());
        }
        cargarUbicaciones(itemParaEditar.getUbicaciones());
        recalcularPrecios();
    }

    private void cargarUbicaciones(List<UbicacionCompra> ubicacionesExistentes) {
        while (contenedorUbicaciones.getChildren().size() > 1) {
            contenedorUbicaciones.getChildren().remove(1);
        }
        contadorFilas = 1;

        if (ubicacionesExistentes == null || ubicacionesExistentes.isEmpty()) {
            comboUbicacion.setValue(null);
            txtCantidadUbicacion.clear();
            return;
        }

        UbicacionCompra primera = ubicacionesExistentes.get(0);
        comboUbicacion.setValue(primera.getUbicacion());
        txtCantidadUbicacion.setText(String.valueOf(primera.getCantidad()));

        for (int i = 1; i < ubicacionesExistentes.size(); i++) {
            agregarUbicacion();
            UbicacionCompra ubicacion = ubicacionesExistentes.get(i);
            HBox fila = (HBox) contenedorUbicaciones.getChildren().get(i);
            configurarFilaUbicacion(fila, ubicacion);
        }
    }

    private void configurarFilaUbicacion(HBox fila, UbicacionCompra ubicacion) {
        if (fila.getChildren().size() < 2) {
            return;
        }

        VBox vboxUbicacion = (VBox) fila.getChildren().get(0);
        VBox vboxCantidad = (VBox) fila.getChildren().get(1);

        ComboBox<String> combo = null;
        TextField cantidadField = null;

        for (javafx.scene.Node child : vboxUbicacion.getChildren()) {
            if (child instanceof ComboBox) {
                @SuppressWarnings("unchecked")
                ComboBox<String> comboBox = (ComboBox<String>) child;
                combo = comboBox;
                break;
            }
        }

        for (javafx.scene.Node child : vboxCantidad.getChildren()) {
            if (child instanceof TextField) {
                cantidadField = (TextField) child;
                break;
            }
        }

        if (combo != null) {
            combo.setValue(ubicacion.getUbicacion());
        }
        if (cantidadField != null) {
            cantidadField.setText(String.valueOf(ubicacion.getCantidad()));
        }
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
        FilteredList<String> filtrados = new FilteredList<>(ubicaciones, item -> true);
        comboBox.setItems(filtrados);
        final boolean[] actualizando = {false};

        comboBox.getEditor().textProperty().addListener((obs, oldValue, newValue) -> {
            if (actualizando[0]) {
                return;
            }
            if (!comboBox.isFocused()) {
                return;
            }
            if (newValue == null || newValue.isBlank()) {
                filtrados.setPredicate(item -> true);
                return;
            }
            String texto = newValue.toLowerCase();
            filtrados.setPredicate(item -> item != null && item.toLowerCase().contains(texto));
        });

        comboBox.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (actualizando[0]) {
                return;
            }
            actualizando[0] = true;
            try {
                if (newValue != null) {
                    comboBox.getEditor().setText(newValue);
                }
            } finally {
                actualizando[0] = false;
            }
        });

        comboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            if (actualizando[0]) {
                return;
            }
            if (newValue == null || newValue.isBlank()) {
                return;
            }
            actualizando[0] = true;
            try {
                comboBox.setValue(newValue);
                comboBox.getEditor().setText(newValue);
            } finally {
                actualizando[0] = false;
            }
        });

        comboBox.setOnAction(event -> {
            Platform.runLater(() -> commitirSeleccionCombo(comboBox, actualizando));
        });

        comboBox.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                commitirSeleccionCombo(comboBox, actualizando);
                if (!actualizando[0]) {
                    String texto = comboBox.getEditor() != null ? comboBox.getEditor().getText() : null;
                    if ((comboBox.getValue() == null || comboBox.getValue().isBlank())
                            && texto != null && !texto.isBlank()) {
                        actualizando[0] = true;
                        try {
                            comboBox.setValue(texto);
                            comboBox.getEditor().setText(texto);
                        } finally {
                            actualizando[0] = false;
                        }
                    } else if (comboBox.getValue() != null) {
                        actualizando[0] = true;
                        try {
                            comboBox.getEditor().setText(comboBox.getValue());
                        } finally {
                            actualizando[0] = false;
                        }
                    }
                }
                Platform.runLater(() -> filtrados.setPredicate(item -> true));
            }
        });

        comboBox.showingProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                Platform.runLater(() -> commitirSeleccionCombo(comboBox, actualizando));
            }
        });

        comboBox.getEditor().setOnKeyPressed(event -> {
            switch (event.getCode()) {
                case TAB:
                case ENTER:
                    String seleccion = comboBox.getSelectionModel().getSelectedItem();
                    String texto = comboBox.getEditor().getText();
                    actualizando[0] = true;
                    try {
                        if (seleccion != null && !seleccion.isBlank()) {
                            comboBox.setValue(seleccion);
                        } else if (texto != null && !texto.isBlank()) {
                            comboBox.setValue(texto);
                        } else {
                            comboBox.setValue(null);
                        }
                    } finally {
                        actualizando[0] = false;
                    }
                    comboBox.hide();
                    break;
            }
        });
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
        int cantidad = parseEntero(txtCantidad.getText());
        BigDecimal precioEntrada = parseDecimal(txtPrecioEntrada.getText());

        BigDecimal precioConIva = precioEntrada;
        if (checkBoxIVA != null && checkBoxIVA.isSelected()) {
            BigDecimal iva = precioEntrada.multiply(IVA_TASA);
            precioConIva = precioEntrada.add(iva);
        }

        BigDecimal precioBruto = precioEntrada.multiply(BigDecimal.valueOf(cantidad));
        BigDecimal precioTotal = precioConIva.multiply(BigDecimal.valueOf(cantidad));

        txtPrecioIVA.setText(formatearDecimal(precioConIva));
        txtPrecioBruto.setText(formatearDecimal(precioBruto));
        txtPrecioTotal.setText(formatearDecimal(precioTotal));
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
            return new BigDecimal(texto);
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
}
