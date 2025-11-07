package Formularios.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class controllerCompraEmergente {

    @FXML private VBox contenedorUbicaciones;
    @FXML private Button btnAgregarUbi;
    @FXML private ComboBox<String> comboUbicacion;
    @FXML private TextField txtCantidadUbicacion;

    private int contadorFilas = 1;
    private final int MAX_FILAS = 10;

    // Lista base de ubicaciones (ejemplo)
    private final ObservableList<String> ubicaciones = FXCollections.observableArrayList(
            "Almacén Principal", "Estante A1", "Estante A2", "Refrigerador", "Mostrador", "Depósito", "Sucursal Norte"
    );

    @FXML
    public void initialize() {
        configurarAutocompletado(comboUbicacion);
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

        // ComboBox de ubicación editable
        VBox vboxUbicacion = new VBox(5);
        Label lblUbicacion = new Label("Ubicación:");
        ComboBox<String> nuevoCombo = new ComboBox<>(ubicaciones);
        nuevoCombo.setEditable(true);
        nuevoCombo.setPromptText("Escribe o selecciona una ubicación");
        configurarAutocompletado(nuevoCombo);
        vboxUbicacion.getChildren().addAll(lblUbicacion, nuevoCombo);
        HBox.setHgrow(vboxUbicacion, Priority.ALWAYS);

        // Campo de cantidad
        VBox vboxCantidad = new VBox(5);
        Label lblCantidad = new Label("Cantidad en ubicación:");
        TextField txtCantidad = new TextField();
        vboxCantidad.getChildren().addAll(lblCantidad, txtCantidad);
        HBox.setHgrow(vboxCantidad, Priority.ALWAYS);

        nuevaFila.getChildren().addAll(vboxUbicacion, vboxCantidad);
        contenedorUbicaciones.getChildren().add(nuevaFila);

        contadorFilas++;
    }

    /**
     * Configura autocompletado para un ComboBox editable.
     */
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

        // Permitir seleccionar con TAB o ENTER
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
}
