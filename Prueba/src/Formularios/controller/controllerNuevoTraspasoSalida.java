package Formularios.controller;

import Compartido.controller.productoCboxController;
import Formularios.model.modelNuevoTraspasoSalida;
import Operaciones.compra.model.UbicacionCompra;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import Compartido.helper.AutoCompleteComboBoxListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class controllerNuevoTraspasoSalida {

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
    @FXML private TextField txtPrecioIVA;
    @FXML private TextField txtPrecioBruto;
    @FXML private TextField txtPrecioTotal;
    @FXML private Button btnGuardar;
    @FXML private Button btnLimpiar;

    private int contadorFilas = 1;
    private static final int MAX_FILAS = 10;

    private final ObservableList<String> ubicaciones = FXCollections.observableArrayList();
    private final ObservableList<String> presentaciones = FXCollections.observableArrayList(
            "paquete", "pz", "caja", "bolsa", "pieza", "rollo", "litro", "kilogramo", "metro", "unidad"
    );

    private final modelNuevoTraspasoSalida modelo = new modelNuevoTraspasoSalida();
    private productoCboxController productoController;

    private BigDecimal precioIvaBase = BigDecimal.ZERO;
    private boolean loteValidado = false;
    private boolean caducidadValidada = false;
    private boolean presentacionValida = false;
    private boolean factorValido = false;
    private boolean ubicacionValidada = false;
    private int cantidadDisponibleUbicacion = 0;
    private boolean cantidadTotalValida = false;

    @FXML
    public void initialize() {
        productoController = new productoCboxController();
        productoController.inicializar(cbClaveProducto, cbProductoNombre, cbClaveAlterna);

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

        Platform.runLater(() -> cbClaveProducto.requestFocus());
    }

    private void configurarPresentaciones() {
        cbPresentacion.setItems(presentaciones);
        cbPresentacion.setValue("pz");
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
                actualizarEstadoCascada();
            }
        });

        cbProductoNombre.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                actualizarDescripcionDesdeProducto();
                cargarPreciosDesdeProducto();
                actualizarEstadoCascada();
            }
        });

        cbClaveAlterna.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                actualizarDescripcionDesdeProducto();
                cargarPreciosDesdeProducto();
                actualizarEstadoCascada();
            }
        });

        cbPresentacion.valueProperty().addListener((obs, oldVal, newVal) -> {
            presentacionValida = false;
            factorValido = false;
            validarPresentacion();
            actualizarEstadoCascada();
        });

        txtFactor.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                validarFactor();
                actualizarEstadoCascada();
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
                cbPresentacion.setValue("pz");
                txtFactor.clear();
                limpiarUbicacionPrimaria();
                limpiarPrecios();
                actualizarEstadoCascada();
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
        limpiarPrecios();
        limpiarValidacionesInventario();
        limpiarUbicacionPrimaria();
        cbPresentacion.setValue("pz");
        txtFactor.clear();
        actualizarEstadoCascada();
    }

    private void configurarCalculoPrecios() {
        txtCantidad.textProperty().addListener((obs, oldVal, newVal) -> {
            validarCantidadTotalDisponible();
            validarCantidadDisponible();
            recalcularPrecios();
            actualizarEstadoCascada();
        });
    }

    private void configurarCamposLectura() {
        txtPrecioEntrada.setEditable(false);
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
        String ubicacionNombre = comboUbicacion.getValue() != null ? comboUbicacion.getValue().trim() : "";

        javafx.concurrent.Task<Optional<modelNuevoTraspasoSalida.PreciosProducto>> task = new javafx.concurrent.Task<>() {
            @Override
            protected Optional<modelNuevoTraspasoSalida.PreciosProducto> call() {
                return modelo.obtenerPreciosProducto(idProducto, lote, caducidad, ubicacionNombre);
            }

            @Override
            protected void succeeded() {
                Optional<modelNuevoTraspasoSalida.PreciosProducto> resultado = getValue();
                if (resultado.isPresent()) {
                    aplicarPrecios(resultado.get());
                } else {
                    limpiarPrecios();
                }
            }

            @Override
            protected void failed() {
                limpiarPrecios();
            }
        };

        Thread hilo = new Thread(task);
        hilo.setDaemon(true);
        hilo.start();
    }

    private void aplicarPrecios(modelNuevoTraspasoSalida.PreciosProducto precios) {
        if (precios == null) {
            limpiarPrecios();
            return;
        }
        BigDecimal precioEntrada = precios.getPrecioUnitario();
        BigDecimal precioIva = precios.getPrecioIva();

        txtPrecioEntrada.setText(formatearDecimal(precioEntrada));
        precioIvaBase = precioIva != null ? precioIva : BigDecimal.ZERO;
        recalcularPrecios();
    }

    private void limpiarPrecios() {
        txtPrecioEntrada.clear();
        txtPrecioIVA.clear();
        txtPrecioBruto.clear();
        txtPrecioTotal.clear();
        precioIvaBase = BigDecimal.ZERO;
    }

    private void guardarItem() {
        String clave = productoController.getIdSeleccionado();
        String nombre = productoController.getNombreSeleccionado();
        String cantidadTexto = txtCantidad.getText();

        if (clave == null || clave.isBlank() || nombre == null || nombre.isBlank()) {
            mostrarAlerta("Advertencia", "Complete los campos obligatorios de producto.");
            return;
        }

        if (!productoController.validarSeleccion()) {
            mostrarAlerta("Error", "El ID y el nombre del producto no corresponden.\n" +
                    "Por favor, verifique la selección.");
            return;
        }

        if (!loteValidado || !caducidadValidada || !ubicacionValidada) {
            mostrarAlerta("Advertencia", "Complete el lote, caducidad y ubicación válidos antes de continuar.");
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

        mostrarAlertaSinEspera("Éxito", "Traspaso de salida capturado.");
        cerrarFormulario();
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
        limpiarPrecios();
        limpiarValidacionesInventario();
        limpiarUbicacionPrimaria();

        while (contenedorUbicaciones.getChildren().size() > 1) {
            contenedorUbicaciones.getChildren().remove(1);
        }
        contadorFilas = 1;
        limpiarComboUbicacion(comboUbicacion);

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
        nuevoCombo.setEditable(true);
        nuevoCombo.setPromptText("Escribe o selecciona una ubicación");
        configurarAutocompletadoUbicacion(nuevoCombo);
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
        BigDecimal precioEntrada = parseDecimal(txtPrecioEntrada.getText());

        BigDecimal precioConIva = precioEntrada;
        if (precioIvaBase != null && precioIvaBase.compareTo(BigDecimal.ZERO) > 0) {
            precioConIva = precioIvaBase;
        }

        BigDecimal precioBruto = precioEntrada.multiply(BigDecimal.valueOf(cantidad));
        BigDecimal precioTotal = precioConIva.multiply(BigDecimal.valueOf(cantidad));

        txtPrecioIVA.setText(formatearDecimal(precioConIva));
        txtPrecioBruto.setText(formatearDecimal(precioBruto));
        txtPrecioTotal.setText(formatearDecimal(precioTotal));
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

    private void configurarCascada() {
        txtDescripcion.setEditable(false);
        dpCaducidad.setEditable(false);
        dpCaducidad.setMouseTransparent(true);
        dpCaducidad.setFocusTraversable(false);
        actualizarEstadoCascada();

        txtLote.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                validarLote();
            }
        });

        comboUbicacion.valueProperty().addListener((obs, oldVal, newVal) -> {
            validarUbicacion();
            actualizarEstadoCascada();
        });

        txtCantidadUbicacion.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                validarCantidadDisponible();
            }
        });
    }

    private void actualizarEstadoCascada() {
        boolean descripcionLista = txtDescripcion.getText() != null && !txtDescripcion.getText().isBlank();
        txtLote.setDisable(!descripcionLista);

        boolean loteListo = descripcionLista && txtLote.getText() != null && !txtLote.getText().isBlank() && loteValidado;
        dpCaducidad.setDisable(false);

        boolean caducidadLista = loteListo && dpCaducidad.getValue() != null && caducidadValidada;
        txtCantidad.setDisable(!caducidadLista);

        boolean cantidadLista = caducidadLista && cantidadTotalValida;
        cbPresentacion.setDisable(!cantidadLista);

        boolean presentacionLista = cantidadLista && presentacionValida;
        txtFactor.setDisable(!presentacionLista);

        boolean factorLista = presentacionLista && factorValido;
        comboUbicacion.setDisable(!factorLista);

        boolean ubicacionLista = factorLista && comboUbicacion.getValue() != null && !comboUbicacion.getValue().isBlank() && ubicacionValidada;
        txtCantidadUbicacion.setDisable(!ubicacionLista);
    }

    private void validarLote() {
        String lote = txtLote.getText() != null ? txtLote.getText().trim() : "";
        if (lote.isBlank()) {
            loteValidado = false;
            actualizarEstadoCascada();
            return;
        }
        String idProducto = productoController.getIdSeleccionado();
        if (idProducto == null || idProducto.isBlank()) {
            loteValidado = false;
            txtLote.clear();
            mostrarAlerta("Advertencia", "Seleccione un producto antes de validar el lote.");
            actualizarEstadoCascada();
            return;
        }
        boolean existe = modelo.existeLoteParaProducto(lote, idProducto);
        if (!existe) {
            loteValidado = false;
            txtLote.clear();
            dpCaducidad.setValue(null);
            limpiarUbicacionPrimaria();
            mostrarAlerta("Advertencia", "El lote no corresponde al producto seleccionado.");
        } else {
            loteValidado = true;
            Optional<java.time.LocalDate> caducidad = modelo.obtenerCaducidadParaLoteProducto(lote, idProducto);
            if (caducidad.isPresent()) {
                dpCaducidad.setValue(caducidad.get());
                caducidadValidada = true;
            } else {
                dpCaducidad.setValue(null);
                caducidadValidada = false;
                mostrarAlerta("Advertencia", "No se encontró caducidad para el lote seleccionado.");
            }
        }
        ubicacionValidada = false;
        cantidadTotalValida = false;
        presentacionValida = false;
        factorValido = false;
        actualizarEstadoCascada();
        limpiarPrecios();
    }

    private void validarUbicacion() {
        if (!caducidadValidada) {
            ubicacionValidada = false;
            return;
        }
        String lote = txtLote.getText() != null ? txtLote.getText().trim() : "";
        java.time.LocalDate caducidad = dpCaducidad.getValue();
        String ubicacion = comboUbicacion.getValue() != null ? comboUbicacion.getValue().trim() : "";
        if (ubicacion.isBlank()) {
            ubicacionValidada = false;
            return;
        }
        boolean existe = modelo.existeLoteCaducidadUbicacion(lote, caducidad, ubicacion);
        if (!existe) {
            ubicacionValidada = false;
            comboUbicacion.setValue(null);
            if (comboUbicacion.getEditor() != null) {
                comboUbicacion.getEditor().clear();
            }
            txtCantidadUbicacion.clear();
            mostrarAlerta("Advertencia", "No hay productos en esa ubicación para el lote y caducidad indicados.");
        } else {
            ubicacionValidada = true;
            cantidadDisponibleUbicacion = modelo.obtenerCantidadDisponible(lote, caducidad, ubicacion);
        }
        actualizarEstadoCascada();
        limpiarPrecios();
    }

    private void validarCantidadDisponible() {
        if (!ubicacionValidada) {
            return;
        }
        String texto = txtCantidadUbicacion.getText() != null ? txtCantidadUbicacion.getText().trim() : "";
        if (texto.isBlank()) {
            return;
        }
        int cantidad = parseEntero(texto);
        if (cantidad <= 0) {
            return;
        }
        if (cantidad > cantidadDisponibleUbicacion) {
            txtCantidadUbicacion.clear();
            mostrarAlerta("Advertencia", "La cantidad supera la disponible en esa ubicación.");
        } else {
            cargarPreciosDesdeProducto();
        }
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
        String lote = txtLote.getText() != null ? txtLote.getText().trim() : "";
        String idProducto = productoController.getIdSeleccionado();
        if (idProducto == null || idProducto.isBlank()) {
            cantidadTotalValida = false;
            return;
        }
        int disponible = modelo.obtenerCantidadDisponibleProductoLoteCaducidad(idProducto, lote, dpCaducidad.getValue());
        if (cantidad > disponible) {
            txtCantidad.clear();
            cantidadTotalValida = false;
            mostrarAlerta("Advertencia", "La cantidad supera la disponible para el lote y caducidad seleccionados.");
        } else {
            cantidadTotalValida = true;
        }
    }

    private void validarPresentacion() {
        if (!cantidadTotalValida) {
            presentacionValida = false;
            return;
        }
        String presentacion = cbPresentacion.getValue();
        String lote = txtLote.getText() != null ? txtLote.getText().trim() : "";
        String idProducto = productoController.getIdSeleccionado();
        if (presentacion == null || presentacion.isBlank() || idProducto == null || idProducto.isBlank()) {
            presentacionValida = false;
            return;
        }
        boolean existe = modelo.existePresentacionParaProductoLote(idProducto, lote, presentacion);
        if (!existe) {
            presentacionValida = false;
            cbPresentacion.setValue("pz");
            mostrarAlerta("Advertencia", "La presentación no existe para el lote y producto seleccionados.");
        } else {
            presentacionValida = true;
        }
        factorValido = false;
        txtFactor.clear();
    }

    private void validarFactor() {
        if (!presentacionValida) {
            factorValido = false;
            return;
        }
        String factorTexto = txtFactor.getText() != null ? txtFactor.getText().trim() : "";
        if (factorTexto.isBlank()) {
            factorValido = false;
            return;
        }
        String lote = txtLote.getText() != null ? txtLote.getText().trim() : "";
        String idProducto = productoController.getIdSeleccionado();
        String presentacion = cbPresentacion.getValue();
        if (idProducto == null || idProducto.isBlank() || presentacion == null || presentacion.isBlank()) {
            factorValido = false;
            return;
        }
        boolean existe = modelo.existeFactorParaProductoLotePresentacion(idProducto, lote, presentacion, factorTexto);
        if (!existe) {
            factorValido = false;
            txtFactor.clear();
            mostrarAlerta("Advertencia", "El factor no corresponde con la presentación y lote seleccionados.");
        } else {
            factorValido = true;
        }
    }

    private boolean datosCompletosParaPrecio() {
        return productoController.getIdSeleccionado() != null
                && !productoController.getIdSeleccionado().isBlank()
                && loteValidado
                && caducidadValidada
                && ubicacionValidada
                && cantidadTotalValida
                && txtCantidadUbicacion.getText() != null
                && !txtCantidadUbicacion.getText().isBlank();
    }

    private void limpiarValidacionesInventario() {
        loteValidado = false;
        caducidadValidada = false;
        presentacionValida = false;
        factorValido = false;
        ubicacionValidada = false;
        cantidadDisponibleUbicacion = 0;
        cantidadTotalValida = false;
    }

    private void configurarAutocompletadoUbicacion(ComboBox<String> comboBox) {
        if (comboBox == null) {
            return;
        }
        comboBox.setItems(ubicaciones);
        comboBox.setEditable(true);
        new AutoCompleteComboBoxListener<>(comboBox);

        comboBox.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                validarTextoUbicacion(comboBox);
            }
        });

        comboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.isBlank()) {
                comboBox.getEditor().setText(newVal);
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
    }
}
