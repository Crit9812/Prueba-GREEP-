package Formularios.controller;

import Compartido.helper.AutoCompleteComboBoxListener;
import Operaciones.traspasoEntrada.model.model;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;

public class controllerUbicacionTraspaso {

    @FXML private VBox contenedorUbicaciones;
    @FXML private Button btnConfirmar;
    @FXML private Button btnCancelar;

    private final ObservableList<String> ubicaciones = FXCollections.observableArrayList();
    private final model modelo = new model();
    private Stage stage;
    private Runnable onConfirmCallback;
    private String claveEntrada;

    @FXML
    public void initialize() {
        cargarUbicacionesDesdeBD();
        inicializarUbicacionesDinamicas();
        configurarBotones();
    }

    public void setClaveEntrada(String claveEntrada) {
        this.claveEntrada = claveEntrada;
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void setOnConfirmCallback(Runnable onConfirmCallback) {
        this.onConfirmCallback = onConfirmCallback;
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
        if (contenedorUbicaciones == null) {
            return;
        }
        contenedorUbicaciones.getChildren().clear();
        agregarFilaUbicacion(true);
    }

    private void agregarFilaUbicacion(boolean esInicial) {
        HBox fila = new HBox(10);

        VBox vboxUbicacion = new VBox(5);
        ComboBox<String> combo = new ComboBox<>(ubicaciones);
        combo.setEditable(true);
        configurarComboUbicacion(combo);
        vboxUbicacion.getChildren().addAll(new javafx.scene.control.Label("Ubicación:"), combo);
        HBox.setHgrow(vboxUbicacion, Priority.ALWAYS);

        VBox vboxCantidad = new VBox(5);
        TextField txtCantidad = new TextField();
        txtCantidad.setPromptText("Cantidad");
        configurarCampoCantidad(txtCantidad);
        vboxCantidad.getChildren().addAll(new javafx.scene.control.Label("Cantidad:"), txtCantidad);

        VBox vboxBoton = new VBox(5);
        Button boton = new Button(esInicial ? "+" : "x");
        boton.getStyleClass().add("botones");
        boton.setMinWidth(30);
        if (esInicial) {
            boton.setOnAction(event -> agregarFilaUbicacion(false));
        } else {
            boton.setOnAction(event -> contenedorUbicaciones.getChildren().remove(fila));
        }
        vboxBoton.getChildren().addAll(new javafx.scene.control.Label(""), boton);

        fila.getChildren().addAll(vboxUbicacion, vboxCantidad, vboxBoton);
        contenedorUbicaciones.getChildren().add(fila);
    }

    private void configurarComboUbicacion(ComboBox<String> comboBox) {
        if (comboBox == null) {
            return;
        }
        new AutoCompleteComboBoxListener<>(comboBox);
        comboBox.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                validarOInsertarUbicacion(comboBox);
                event.consume();
            }
        });
        comboBox.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                validarOInsertarUbicacion(comboBox);
            }
        });
    }

    private void configurarCampoCantidad(TextField campo) {
        if (campo == null) {
            return;
        }
        campo.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*")) {
                campo.setText(newVal.replaceAll("[^\\d]", ""));
            }
        });
    }

    private void validarOInsertarUbicacion(ComboBox<String> comboBox) {
        if (comboBox == null) {
            return;
        }
        String valor = comboBox.getEditor() != null ? comboBox.getEditor().getText() : comboBox.getValue();
        if (valor == null || valor.trim().isEmpty()) {
            return;
        }
        String nombre = valor.trim();
        boolean existe = modelo.existeUbicacionNombre(nombre);
        if (!existe) {
            boolean creada = modelo.insertarUbicacionActiva(nombre);
            if (!creada) {
                mostrarAlerta("Error", "No se pudo registrar la ubicación: " + nombre);
                return;
            }
            ubicaciones.add(nombre);
            FXCollections.sort(ubicaciones);
            sincronizarCombosUbicacion();
        }
        comboBox.setValue(nombre);
        if (comboBox.getEditor() != null) {
            comboBox.getEditor().setText(nombre);
        }
    }

    private void sincronizarCombosUbicacion() {
        if (contenedorUbicaciones == null) {
            return;
        }
        for (javafx.scene.Node nodo : contenedorUbicaciones.getChildren()) {
            if (!(nodo instanceof HBox fila)) {
                continue;
            }
            if (fila.getChildren().isEmpty()) {
                continue;
            }
            VBox vboxUbicacion = (VBox) fila.getChildren().get(0);
            if (vboxUbicacion == null || vboxUbicacion.getChildren().size() < 2) {
                continue;
            }
            @SuppressWarnings("unchecked")
            ComboBox<String> combo = (ComboBox<String>) vboxUbicacion.getChildren().get(1);
            combo.setItems(ubicaciones);
        }
    }

    private void configurarBotones() {
        if (btnConfirmar != null) {
            btnConfirmar.setOnAction(event -> confirmar());
        }
        if (btnCancelar != null) {
            btnCancelar.setOnAction(event -> cerrar());
        }
    }

    private void confirmar() {
        if (!validarUbicaciones()) {
            return;
        }
        if (onConfirmCallback != null) {
            onConfirmCallback.run();
        }
        cerrar();
    }

    private boolean validarUbicaciones() {
        List<String> errores = new ArrayList<>();
        if (contenedorUbicaciones == null || contenedorUbicaciones.getChildren().isEmpty()) {
            errores.add("Debe agregar al menos una ubicación.");
        } else {
            for (javafx.scene.Node nodo : contenedorUbicaciones.getChildren()) {
                if (!(nodo instanceof HBox fila)) {
                    continue;
                }
                VBox vboxUbicacion = (VBox) fila.getChildren().get(0);
                VBox vboxCantidad = (VBox) fila.getChildren().get(1);
                if (vboxUbicacion == null || vboxUbicacion.getChildren().size() < 2) {
                    continue;
                }
                @SuppressWarnings("unchecked")
                ComboBox<String> combo = (ComboBox<String>) vboxUbicacion.getChildren().get(1);
                TextField campoCantidad = (TextField) vboxCantidad.getChildren().get(1);
                String ubicacionTexto = combo.getEditor() != null ? combo.getEditor().getText() : combo.getValue();
                String cantidadTexto = campoCantidad.getText();
                if (ubicacionTexto == null || ubicacionTexto.trim().isEmpty()) {
                    errores.add("Debe seleccionar una ubicación válida.");
                } else {
                    validarOInsertarUbicacion(combo);
                }
                if (cantidadTexto == null || cantidadTexto.trim().isEmpty() || cantidadTexto.equals("0")) {
                    errores.add("Debe capturar una cantidad válida para cada ubicación.");
                }
            }
        }

        if (!errores.isEmpty()) {
            mostrarAlerta("Advertencia", errores.get(0));
            return false;
        }
        return true;
    }

    private void cerrar() {
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
            if (btnConfirmar != null && btnConfirmar.getScene() != null) {
                alert.initOwner(btnConfirmar.getScene().getWindow());
            }
            alert.showAndWait();
        });
    }
}
