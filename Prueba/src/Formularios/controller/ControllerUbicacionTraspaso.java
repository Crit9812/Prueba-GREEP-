package Formularios.controller;

import Operaciones.compra.model.UbicacionCompra;
import Operaciones.traspasoEntrada.model.model;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;

public class ControllerUbicacionTraspaso {

    @FXML private VBox contenedorUbicaciones;
    @FXML private Label lblTitulo;
    @FXML private Button btnConfirmar;
    @FXML private Button btnCancelar;

    private static final int MAX_FILAS = 10;

    private final ObservableList<String> ubicaciones = FXCollections.observableArrayList();
    private final List<UbicacionRow> filasUbicacion = new ArrayList<>();

    private final model modeloTraspaso = new model();

    private int contadorFilas = 0;
    private String claveEntrada;
    private Stage stage;
    private Runnable onConfirmCallback;

    @FXML
    public void initialize() {
        inicializarUbicacionesDinamicas();
        cargarUbicacionesDesdeBD();
        actualizarTitulo();
    }

    public void setClaveEntrada(String claveEntrada) {
        this.claveEntrada = claveEntrada;
        actualizarTitulo();
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void setOnConfirmCallback(Runnable onConfirmCallback) {
        this.onConfirmCallback = onConfirmCallback;
    }

    private void actualizarTitulo() {
        if (lblTitulo == null) {
            return;
        }
        if (claveEntrada == null || claveEntrada.isBlank()) {
            lblTitulo.setText("Ubicaciones");
            return;
        }
        lblTitulo.setText("Ubicaciones - " + claveEntrada);
    }

    private void cargarUbicacionesDesdeBD() {
        javafx.concurrent.Task<List<String>> task = new javafx.concurrent.Task<>() {
            @Override
            protected List<String> call() {
                return modeloTraspaso.obtenerNombresUbicaciones();
            }

            @Override
            protected void succeeded() {
                List<String> resultados = getValue();
                ubicaciones.setAll(resultados != null ? resultados : List.of());
                sincronizarCombosUbicacion();
            }

            @Override
            protected void failed() {
                ubicaciones.clear();
                sincronizarCombosUbicacion();
            }
        };

        Thread hilo = new Thread(task);
        hilo.setDaemon(true);
        hilo.start();
    }

    private void inicializarUbicacionesDinamicas() {
        contenedorUbicaciones.getChildren().clear();
        filasUbicacion.clear();
        contadorFilas = 0;
        agregarFilaUbicacion(true);
    }

    private void agregarFilaUbicacion(boolean esInicial) {
        if (contadorFilas >= MAX_FILAS) {
            mostrarAlerta("Límite alcanzado", "Solo se pueden agregar hasta " + MAX_FILAS + " ubicaciones.");
            return;
        }

        HBox nuevaFila = new HBox(20);

        VBox vboxUbicacion = new VBox(5);
        Label labelUbicacion = new Label("Ubicación:");
        ComboBox<String> combo = new ComboBox<>();
        combo.setEditable(true);
        combo.setPromptText("Escribe o selecciona una ubicación");
        combo.setItems(ubicaciones);
        vboxUbicacion.getChildren().addAll(labelUbicacion, combo);
        HBox.setHgrow(vboxUbicacion, Priority.ALWAYS);

        VBox vboxCantidad = new VBox(5);
        Label labelCantidad = new Label("Cantidad en ubicación:");
        TextField txtCantidad = new TextField();
        vboxCantidad.getChildren().addAll(labelCantidad, txtCantidad);
        HBox.setHgrow(vboxCantidad, Priority.ALWAYS);

        VBox vboxBoton = new VBox(5);
        Button boton = new Button(esInicial ? "+" : "-");
        boton.getStyleClass().add("botonAgregarUbi");
        vboxBoton.setAlignment(Pos.BOTTOM_CENTER);
        vboxBoton.getChildren().add(boton);
        HBox.setHgrow(vboxBoton, Priority.ALWAYS);

        if (esInicial) {
            boton.setOnAction(event -> agregarFilaUbicacion(false));
        } else {
            boton.setOnAction(event -> eliminarFilaUbicacion(nuevaFila));
        }

        nuevaFila.getChildren().addAll(vboxUbicacion, vboxCantidad, vboxBoton);
        contenedorUbicaciones.getChildren().add(nuevaFila);

        configurarComboUbicacion(combo);

        filasUbicacion.add(new UbicacionRow(nuevaFila, combo, txtCantidad));
        contadorFilas++;
    }

    private void eliminarFilaUbicacion(HBox fila) {
        UbicacionRow filaEncontrada = null;
        for (UbicacionRow filaUbicacion : filasUbicacion) {
            if (filaUbicacion.contenedor == fila) {
                filaEncontrada = filaUbicacion;
                break;
            }
        }
        if (filaEncontrada != null) {
            filasUbicacion.remove(filaEncontrada);
        }
        contenedorUbicaciones.getChildren().remove(fila);
        contadorFilas = Math.max(0, contadorFilas - 1);
    }

    private void configurarComboUbicacion(ComboBox<String> comboBox) {
        if (comboBox == null) {
            return;
        }

        boolean[] actualizando = {false};

        comboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (actualizando[0]) {
                return;
            }
            actualizando[0] = true;
            if (newVal == null || newVal.isBlank()) {
                if (comboBox.getEditor() != null) {
                    comboBox.getEditor().clear();
                }
            } else if (comboBox.getEditor() != null) {
                comboBox.getEditor().setText(newVal);
            }
            actualizando[0] = false;
        });

        if (comboBox.getEditor() != null) {
            comboBox.getEditor().textProperty().addListener((obs, oldVal, newVal) -> {
                if (actualizando[0]) {
                    return;
                }
                if (newVal == null || newVal.isBlank()) {
                    actualizando[0] = true;
                    comboBox.setValue(null);
                    actualizando[0] = false;
                }
            });

            comboBox.getEditor().setOnKeyPressed(event -> {
                if (event.getCode() == KeyCode.ENTER) {
                    confirmarTextoCombo(comboBox, actualizando);
                    event.consume();
                }
            });
        }

        comboBox.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                confirmarTextoCombo(comboBox, actualizando);
            }
        });

        comboBox.setOnAction(event -> confirmarTextoCombo(comboBox, actualizando));
    }

    private void confirmarTextoCombo(ComboBox<String> comboBox, boolean[] actualizando) {
        if (comboBox == null || actualizando[0]) {
            return;
        }
        String texto = comboBox.getEditor() != null ? comboBox.getEditor().getText() : null;
        String seleccionado = comboBox.getSelectionModel().getSelectedItem();
        String valor = (seleccionado != null && !seleccionado.isBlank()) ? seleccionado : texto;
        if (valor == null || valor.isBlank()) {
            return;
        }
        actualizando[0] = true;
        comboBox.setValue(valor);
        if (comboBox.getEditor() != null) {
            comboBox.getEditor().setText(valor);
        }
        actualizando[0] = false;
    }

    private void sincronizarCombosUbicacion() {
        for (UbicacionRow filaUbicacion : filasUbicacion) {
            filaUbicacion.combo.setItems(ubicaciones);
        }
    }

    @FXML
    private void confirmarUbicaciones() {
        List<UbicacionCompra> ubicacionesSeleccionadas = obtenerUbicacionesSeleccionadas();
        if (ubicacionesSeleccionadas.isEmpty()) {
            mostrarAlerta("Validación", "Debe capturar al menos una ubicación con cantidad.");
            return;
        }

        if (onConfirmCallback != null) {
            onConfirmCallback.run();
        }
        cerrarFormulario();
    }

    @FXML
    private void cerrarFormulario() {
        Stage ventana = stage;
        if (ventana == null && btnCancelar != null && btnCancelar.getScene() != null) {
            ventana = (Stage) btnCancelar.getScene().getWindow();
        }
        if (ventana != null) {
            ventana.close();
        }
    }

    private List<UbicacionCompra> obtenerUbicacionesSeleccionadas() {
        List<UbicacionCompra> resultado = new ArrayList<>();
        for (UbicacionRow filaUbicacion : filasUbicacion) {
            ComboBox<String> combo = filaUbicacion.combo;
            TextField cantidadField = filaUbicacion.cantidad;

            String ubicacion = combo.getValue();
            if ((ubicacion == null || ubicacion.isBlank()) && combo.getEditor() != null) {
                ubicacion = combo.getEditor().getText();
            }
            String cantidadTexto = cantidadField.getText();

            if (ubicacion == null || ubicacion.isBlank() || cantidadTexto == null || cantidadTexto.isBlank()) {
                continue;
            }

            try {
                int cantidad = Integer.parseInt(cantidadTexto.trim());
                if (cantidad > 0) {
                    resultado.add(new UbicacionCompra(ubicacion.trim(), cantidad));
                }
            } catch (NumberFormatException ignored) {
                // Ignorar ubicaciones con cantidad inválida
            }
        }
        return resultado;
    }

    private void mostrarAlerta(String titulo, String mensaje) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle(titulo);
            alert.setHeaderText(null);
            alert.setContentText(mensaje);
            alert.showAndWait();
        });
    }

    private static class UbicacionRow {
        private final HBox contenedor;
        private final ComboBox<String> combo;
        private final TextField cantidad;

        private UbicacionRow(HBox contenedor, ComboBox<String> combo, TextField cantidad) {
            this.contenedor = contenedor;
            this.combo = combo;
            this.cantidad = cantidad;
        }
    }
}
