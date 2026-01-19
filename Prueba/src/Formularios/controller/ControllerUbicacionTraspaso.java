package Formularios.controller;

import Formularios.model.modelUbicacionTraspaso;
import Formularios.model.modelUbicacionTraspaso.ProductoConLote;
import Operaciones.compra.model.UbicacionCompra;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

public class ControllerUbicacionTraspaso implements Initializable {

    @FXML private VBox contenedorProductos;
    @FXML private ScrollPane scrollPane;
    @FXML private Button btnConfirmar;
    @FXML private Button btnCancelar;

    private String claveEntrada;
    private List<ProductoConLote> productos;
    private final modelUbicacionTraspaso modeloUbicacion = new modelUbicacionTraspaso();
    private Runnable onConfirmCallback;
    private Stage stage;

    private final Map<String, UbicacionManager> ubicacionManagers = new HashMap<>();
    private final Map<String, ProductoConLote> datosProductos = new HashMap<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        configurarEstiloScrollPane();
        btnConfirmar.setOnAction(e -> confirmarUbicaciones());
        btnCancelar.setOnAction(e -> cancelar());
    }

    public void setClaveEntrada(String claveEntrada) {
        this.claveEntrada = claveEntrada;
        cargarProductos();
    }

    public void setOnConfirmCallback(Runnable callback) {
        this.onConfirmCallback = callback;
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    private void configurarEstiloScrollPane() {
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: white; -fx-background: white;");
    }

    private void cargarProductos() {
        productos = modeloUbicacion.obtenerProductosPorLote(claveEntrada);

        if (productos == null || productos.isEmpty()) {
            mostrarAlerta("Error", "No hay productos pendientes de ubicación para este traspaso.");
            Platform.runLater(() -> {
                if (stage != null) {
                    stage.close();
                }
            });
            return;
        }

        ObservableList<String> ubicacionesDisponibles = modeloUbicacion.obtenerUbicacionesDisponibles();

        contenedorProductos.getChildren().clear();
        ubicacionManagers.clear();
        datosProductos.clear();

        int contador = 1;
        for (ProductoConLote producto : productos) {
            agregarProductoAlFormulario(producto, contador++, ubicacionesDisponibles);
        }
    }

    private void agregarProductoAlFormulario(ProductoConLote producto,
                                             int numero,
                                             ObservableList<String> ubicaciones) {
        VBox productoContainer = new VBox(10);
        productoContainer.setStyle("-fx-padding: 15; -fx-background-color: #f5f5f5; " +
                "-fx-border-color: #ddd; -fx-border-radius: 5; -fx-background-radius: 5;");
        productoContainer.setPadding(new Insets(15));

        HBox headerBox = new HBox(10);
        headerBox.setAlignment(Pos.CENTER_LEFT);

        Label lblNumero = new Label(numero + ".");
        lblNumero.setStyle("-fx-font-weight: bold; -fx-font-size: 14; -fx-min-width: 30;");

        Label lblProducto = new Label(producto.getNombreProducto());
        lblProducto.setStyle("-fx-font-weight: bold; -fx-font-size: 14; -fx-text-fill: #2c3e50;");

        headerBox.getChildren().addAll(lblNumero, lblProducto);

        VBox detallesBox = new VBox(3);
        detallesBox.setStyle("-fx-padding: 0 0 0 40;");

        Label lblClave = new Label("Clave: " + producto.getClaveProducto());
        Label lblLote = new Label("Lote: " + (producto.getLote() != null && !producto.getLote().isEmpty()
                ? producto.getLote()
                : "Sin lote"));
        Label lblCaducidad = new Label("Caducidad: " + (producto.getCaducidad() != null
                && !producto.getCaducidad().isEmpty() ? producto.getCaducidad() : "Sin fecha"));
        Label lblCantidad = new Label("Cantidad total: " + producto.getCantidad() + " unidades");

        String descripcion = modeloUbicacion.obtenerDescripcionProducto(producto.getClaveProducto());
        String marca = modeloUbicacion.obtenerMarcaProducto(producto.getClaveProducto());

        if (!descripcion.isEmpty()) {
            Label lblDescripcion = new Label("Descripción: " + descripcion);
            detallesBox.getChildren().add(lblDescripcion);
        }

        if (!marca.isEmpty()) {
            Label lblMarca = new Label("Marca: " + marca);
            detallesBox.getChildren().add(lblMarca);
        }

        detallesBox.getChildren().addAll(lblClave, lblLote, lblCaducidad, lblCantidad);

        VBox contenedorCompletoUbicaciones = new VBox(10);
        contenedorCompletoUbicaciones.setStyle("-fx-padding: 10 0 0 40;");
        contenedorCompletoUbicaciones.setPadding(new Insets(10, 0, 0, 40));

        Label lblTituloUbicaciones = new Label(
                "Distribuir " + producto.getCantidad() + " unidad(es) en ubicaciones:");
        lblTituloUbicaciones.setStyle("-fx-font-weight: bold; -fx-font-size: 13;");

        HBox ubicacionesMainContainer = new HBox(20);

        VBox contenedorUbicaciones = new VBox(10);
        contenedorUbicaciones.setId("contenedorUbicaciones");

        HBox primeraFila = new HBox(20);
        primeraFila.setStyle("-fx-padding: 0 0 0 0;");

        VBox vboxUbicacion = new VBox(5);
        vboxUbicacion.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(vboxUbicacion, Priority.ALWAYS);

        Label lblUbicacion = new Label("Ubicación:");
        ComboBox<String> comboUbicacion = new ComboBox<>(ubicaciones);
        comboUbicacion.setEditable(true);
        comboUbicacion.setPromptText("Escribe o selecciona una ubicación");
        comboUbicacion.setMaxWidth(Double.MAX_VALUE);

        vboxUbicacion.getChildren().addAll(lblUbicacion, comboUbicacion);

        VBox vboxCantidad = new VBox(5);
        vboxCantidad.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(vboxCantidad, Priority.ALWAYS);

        Label lblCantidadUbicacion = new Label("Cantidad en ubicación:");
        TextField txtCantidadUbicacion = new TextField();
        txtCantidadUbicacion.setPromptText("Cantidad");
        txtCantidadUbicacion.setMaxWidth(Double.MAX_VALUE);
        configurarValidadorEnteros(txtCantidadUbicacion);

        vboxCantidad.getChildren().addAll(lblCantidadUbicacion, txtCantidadUbicacion);

        VBox vboxBoton = new VBox(5);
        vboxBoton.setAlignment(Pos.CENTER);

        Pane espacioVertical = new Pane();
        VBox.setVgrow(espacioVertical, Priority.ALWAYS);

        HBox contenedorBoton = new HBox();
        contenedorBoton.setAlignment(Pos.CENTER);
        contenedorBoton.setSpacing(20);

        Button btnAgregarUbicacion = new Button("+");
        btnAgregarUbicacion.setId("btnAgregarUbi");
        btnAgregarUbicacion.getStyleClass().add("botonAgregarUbi");
        btnAgregarUbicacion.setText("+");

        contenedorBoton.getChildren().add(btnAgregarUbicacion);
        vboxBoton.getChildren().addAll(espacioVertical, contenedorBoton);

        primeraFila.getChildren().addAll(vboxUbicacion, vboxCantidad, vboxBoton);
        contenedorUbicaciones.getChildren().add(primeraFila);
        ubicacionesMainContainer.getChildren().add(contenedorUbicaciones);

        UbicacionManager manager = new UbicacionManager(
                contenedorUbicaciones,
                comboUbicacion,
                txtCantidadUbicacion,
                ubicaciones,
                this::configurarValidadorEnteros
        );

        btnAgregarUbicacion.setOnAction(e -> manager.agregarFilaUbicacion());

        String claveUnica = producto.getClaveProducto() + "_" + producto.getLote() + "_" + producto.getCaducidad();
        ubicacionManagers.put(claveUnica, manager);
        datosProductos.put(claveUnica, producto);

        contenedorCompletoUbicaciones.getChildren().addAll(lblTituloUbicaciones, ubicacionesMainContainer);
        productoContainer.getChildren().addAll(headerBox, detallesBox, contenedorCompletoUbicaciones);
        contenedorProductos.getChildren().add(productoContainer);

        if (numero < productos.size()) {
            Region separador = new Region();
            separador.setPrefHeight(20);
            contenedorProductos.getChildren().add(separador);
        }
    }

    private void configurarValidadorEnteros(TextField campo) {
        campo.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) {
                return;
            }
            String filtrado = newVal.replaceAll("[^0-9]", "");
            if (!filtrado.equals(newVal)) {
                campo.setText(filtrado);
            }
        });
    }

    private void confirmarUbicaciones() {
        List<String> errores = new ArrayList<>();
        Map<String, List<UbicacionCompra>> ubicacionesPorProducto = new HashMap<>();

        for (Map.Entry<String, UbicacionManager> entry : ubicacionManagers.entrySet()) {
            String claveUnica = entry.getKey();
            UbicacionManager manager = entry.getValue();
            ProductoConLote producto = datosProductos.get(claveUnica);

            if (producto == null) {
                continue;
            }

            int cantidadTotal = producto.getCantidad();
            List<UbicacionCompra> ubicaciones = manager.obtenerUbicaciones();

            if (tieneUbicacionesDuplicadas(ubicaciones)) {
                errores.add("• " + producto.getNombreProducto() +
                        (producto.getLote() != null && !producto.getLote().isEmpty()
                                ? " (Lote: " + producto.getLote() + ")"
                                : "") +
                        " - No se puede seleccionar la misma ubicación más de una vez.");
                continue;
            }

            ResultadoValidacion validacion = manager.validarUbicaciones(cantidadTotal);

            if (!validacion.isValido()) {
                errores.add("• " + producto.getNombreProducto() +
                        (producto.getLote() != null && !producto.getLote().isEmpty()
                                ? " (Lote: " + producto.getLote() + ")"
                                : "") +
                        " - " + validacion.getMensaje());
            } else {
                ubicacionesPorProducto.put(claveUnica, ubicaciones);
            }
        }

        if (!errores.isEmpty()) {
            mostrarAlerta("Error de validación",
                    "Los siguientes errores deben corregirse:\n\n" + String.join("\n", errores));
            return;
        }

        if (ubicacionesPorProducto.isEmpty()) {
            mostrarAlerta("Error", "No se capturaron ubicaciones válidas.");
            return;
        }

        boolean guardado = modeloUbicacion.guardarUbicacionesYActualizarEstados(
                claveEntrada,
                ubicacionesPorProducto
        );

        if (guardado) {
            int totalArticulos = productos.stream().mapToInt(ProductoConLote::getCantidad).sum();

            Alert alert = new Alert(AlertType.INFORMATION);
            alert.setTitle("Éxito");
            alert.setHeaderText(null);
            alert.setContentText("Ubicaciones asignadas correctamente a " +
                    totalArticulos + " artículo(s) de " + productos.size() + " producto(s).\n" +
                    "El traspaso ha sido aceptado y los artículos están disponibles.");

            alert.showAndWait().ifPresent(response -> {
                if (onConfirmCallback != null) {
                    onConfirmCallback.run();
                }
                if (stage != null) {
                    stage.close();
                }
            });
        } else {
            mostrarAlerta("Error", "No se pudieron guardar las ubicaciones ni aceptar el traspaso.");
        }
    }

    private boolean tieneUbicacionesDuplicadas(List<UbicacionCompra> ubicacionesSeleccionadas) {
        if (ubicacionesSeleccionadas == null || ubicacionesSeleccionadas.isEmpty()) {
            return false;
        }

        Set<String> ubicacionesUnicas = new HashSet<>();
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

    private void cancelar() {
        if (stage != null) {
            stage.close();
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

    private static class UbicacionManager {
        private final VBox contenedorUbicaciones;
        private final ObservableList<String> ubicaciones;
        private final List<UbicacionRow> filas = new ArrayList<>();
        private final CampoConfigurador configuradorEnteros;

        private UbicacionManager(VBox contenedorUbicaciones,
                                 ComboBox<String> comboUbicacion,
                                 TextField txtCantidadUbicacion,
                                 ObservableList<String> ubicaciones,
                                 CampoConfigurador configuradorEnteros) {
            this.contenedorUbicaciones = contenedorUbicaciones;
            this.ubicaciones = ubicaciones;
            this.configuradorEnteros = configuradorEnteros;
            filas.add(new UbicacionRow(comboUbicacion, txtCantidadUbicacion, null));
        }

        private void agregarFilaUbicacion() {
            HBox fila = new HBox(20);

            VBox vboxUbicacion = new VBox(5);
            vboxUbicacion.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(vboxUbicacion, Priority.ALWAYS);

            Label lblUbicacion = new Label("Ubicación:");
            ComboBox<String> comboUbicacion = new ComboBox<>(ubicaciones);
            comboUbicacion.setEditable(true);
            comboUbicacion.setPromptText("Escribe o selecciona una ubicación");
            comboUbicacion.setMaxWidth(Double.MAX_VALUE);
            vboxUbicacion.getChildren().addAll(lblUbicacion, comboUbicacion);

            VBox vboxCantidad = new VBox(5);
            vboxCantidad.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(vboxCantidad, Priority.ALWAYS);

            Label lblCantidadUbicacion = new Label("Cantidad en ubicación:");
            TextField txtCantidadUbicacion = new TextField();
            txtCantidadUbicacion.setPromptText("Cantidad");
            txtCantidadUbicacion.setMaxWidth(Double.MAX_VALUE);
            configuradorEnteros.configurar(txtCantidadUbicacion);
            vboxCantidad.getChildren().addAll(lblCantidadUbicacion, txtCantidadUbicacion);

            VBox vboxBoton = new VBox(5);
            vboxBoton.setAlignment(Pos.CENTER);

            Button btnEliminar = new Button("-");
            btnEliminar.getStyleClass().add("botonAgregarUbi");
            btnEliminar.setOnAction(e -> eliminarFila(fila));

            vboxBoton.getChildren().add(btnEliminar);

            fila.getChildren().addAll(vboxUbicacion, vboxCantidad, vboxBoton);
            contenedorUbicaciones.getChildren().add(fila);

            filas.add(new UbicacionRow(comboUbicacion, txtCantidadUbicacion, fila));
        }

        private void eliminarFila(HBox fila) {
            contenedorUbicaciones.getChildren().remove(fila);
            filas.removeIf(row -> row.fila == fila);
        }

        private List<UbicacionCompra> obtenerUbicaciones() {
            List<UbicacionCompra> ubicacionesCapturadas = new ArrayList<>();
            for (UbicacionRow row : filas) {
                String ubicacion = obtenerTextoCombo(row.combo);
                String cantidadTexto = row.cantidad.getText();

                if (ubicacion == null || ubicacion.isBlank()
                        || cantidadTexto == null || cantidadTexto.isBlank()) {
                    continue;
                }

                try {
                    int cantidad = Integer.parseInt(cantidadTexto.trim());
                    if (cantidad > 0) {
                        ubicacionesCapturadas.add(new UbicacionCompra(ubicacion.trim(), cantidad));
                    }
                } catch (NumberFormatException ignored) {
                    // Ignorar registros inválidos
                }
            }
            return ubicacionesCapturadas;
        }

        private ResultadoValidacion validarUbicaciones(int cantidadTotal) {
            List<UbicacionCompra> ubicacionesCapturadas = obtenerUbicaciones();
            if (ubicacionesCapturadas.isEmpty()) {
                return ResultadoValidacion.invalido("Debe capturar al menos una ubicación con cantidad.");
            }

            int totalCapturado = ubicacionesCapturadas.stream()
                    .mapToInt(UbicacionCompra::getCantidad)
                    .sum();

            if (totalCapturado != cantidadTotal) {
                return ResultadoValidacion.invalido(
                        "La suma de cantidades debe ser " + cantidadTotal + " y capturaste " + totalCapturado + ".");
            }

            return ResultadoValidacion.valido();
        }

        private String obtenerTextoCombo(ComboBox<String> comboBox) {
            if (comboBox == null) {
                return "";
            }
            String valor = comboBox.getValue();
            if ((valor == null || valor.isBlank()) && comboBox.getEditor() != null) {
                valor = comboBox.getEditor().getText();
            }
            return valor != null ? valor : "";
        }
    }

    private static class UbicacionRow {
        private final ComboBox<String> combo;
        private final TextField cantidad;
        private final HBox fila;

        private UbicacionRow(ComboBox<String> combo, TextField cantidad, HBox fila) {
            this.combo = combo;
            this.cantidad = cantidad;
            this.fila = fila;
        }
    }

    private interface CampoConfigurador {
        void configurar(TextField campo);
    }

    private static class ResultadoValidacion {
        private final boolean valido;
        private final String mensaje;

        private ResultadoValidacion(boolean valido, String mensaje) {
            this.valido = valido;
            this.mensaje = mensaje;
        }

        private static ResultadoValidacion valido() {
            return new ResultadoValidacion(true, "");
        }

        private static ResultadoValidacion invalido(String mensaje) {
            return new ResultadoValidacion(false, mensaje);
        }

        private boolean isValido() {
            return valido;
        }

        private String getMensaje() {
            return mensaje;
        }
    }
}
