package Formularios.controller;

import Formularios.model.itemPedido;
import Formularios.model.modelNuevoPedido;
import javafx.collections.*;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class controllerNuevoPedido {

    @FXML private ComboBox<String> cbClaveProducto;
    @FXML private ComboBox<String> cbProductoNombre;
    @FXML private TextField txtCantidad;
    @FXML private TextField txtDescripcion;
    @FXML private Button btnGuardar;

    private final ObservableList<itemPedido> itemsPedido = FXCollections.observableArrayList();
    private List<Map<String, String>> productos;

    @FXML
    public void initialize() {
        cargarProductos();
        configurarEventos();
        validarCantidad();
    }

    private void cargarProductos() {
        try {
            modelNuevoPedido model = new modelNuevoPedido();
            productos = model.obtenerProductos();

            for (Map<String, String> p : productos) {
                cbClaveProducto.getItems().add(p.get("id"));
                cbProductoNombre.getItems().add(p.get("nombre"));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void configurarEventos() {

        cbClaveProducto.setOnAction(e -> {
            String clave = cbClaveProducto.getValue();
            productos.stream()
                    .filter(p -> p.get("id").equals(clave))
                    .findFirst()
                    .ifPresent(p -> {
                        cbProductoNombre.setValue(p.get("nombre"));
                        txtDescripcion.setText(p.get("descripcion"));
                    });
        });

        cbProductoNombre.setOnAction(e -> {
            String nombre = cbProductoNombre.getValue();
            productos.stream()
                    .filter(p -> p.get("nombre").equals(nombre))
                    .findFirst()
                    .ifPresent(p -> {
                        cbClaveProducto.setValue(p.get("id"));
                        txtDescripcion.setText(p.get("descripcion"));
                    });
        });

        btnGuardar.setOnAction(e -> guardarItem());
    }

    private void guardarItem() {
        if (cbClaveProducto.getValue() == null ||
                cbProductoNombre.getValue() == null ||
                txtCantidad.getText().isBlank()) {
            return;
        }

        int cantidad = Integer.parseInt(txtCantidad.getText());

        itemPedido item = new itemPedido(
                cbClaveProducto.getValue(),
                cbProductoNombre.getValue(),
                txtDescripcion.getText(),
                cantidad
        );

        itemsPedido.add(item);
        limpiarFormulario();
    }

    private void limpiarFormulario() {
        cbClaveProducto.setValue(null);
        cbProductoNombre.setValue(null);
        txtCantidad.clear();
        txtDescripcion.clear();
    }

    private void validarCantidad() {
        txtCantidad.textProperty().addListener((obs, old, val) -> {
            if (!val.matches("\\d*")) {
                txtCantidad.setText(val.replaceAll("[^\\d]", ""));
            }
        });
    }

    // Este método te sirve para pasar la lista a otro controller
    public ObservableList<itemPedido> getItemsPedido() {
        return itemsPedido;
    }
}
