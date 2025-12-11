package Formularios.controller;

import Formularios.model.modelSincronizacionClaves;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Window;

import java.util.*;
import java.util.stream.Collectors;

public class controllerSincronizacionClaves {

    @FXML private ComboBox<String> cbProveedorNombre;
    @FXML private ComboBox<Integer> cbProveedorId;
    @FXML private ComboBox<String> cbProductoId;
    @FXML private ComboBox<String> cbProductoNombre;
    @FXML private TextField txtClaveAlterna;
    @FXML private TextField txtDescripcion;
    @FXML private Button btnGuardar;
    @FXML private Button btnCancelar;

    private final modelSincronizacionClaves model = new modelSincronizacionClaves();

    private final Map<Integer, String> proveedorIdToName = new HashMap<>();
    private final Map<String, Integer> proveedorNameToId = new HashMap<>();

    private final Map<String, String> productoIdToName = new HashMap<>();
    private final Map<String, String> productoNameToId = new HashMap<>();
    private final Map<String, Map<String, String>> productoMeta = new HashMap<>();

    @FXML
    public void initialize() {
        Platform.runLater(() -> {
            try {
                cargarProveedores();
                cargarProductos();
                configurarListeners();
            } catch (Exception e) {
                e.printStackTrace();
                mostrarError("Error al cargar datos: " + e.getMessage());
            }
        });
    }

    private void cargarProveedores() throws Exception {
        var list = model.obtenerProveedores();
        List<String> nombres = new ArrayList<>();
        List<Integer> ids = new ArrayList<>();
        for (var m : list) {
            Integer id = (m.get("id") instanceof Number) ? ((Number) m.get("id")).intValue() : Integer.parseInt(m.get("id").toString());
            String nombre = String.valueOf(m.get("nombre"));
            proveedorIdToName.put(id, nombre);
            proveedorNameToId.put(nombre, id);
            nombres.add(nombre);
            ids.add(id);
        }
        cbProveedorNombre.getItems().setAll(nombres);
        cbProveedorId.getItems().setAll(ids);
        cbProveedorNombre.setEditable(true);
        cbProveedorId.setEditable(true);
    }

    private void cargarProductos() throws Exception {
        var list = model.obtenerProductos();
        List<String> nombres = new ArrayList<>();
        List<String> ids = new ArrayList<>();
        for (var m : list) {
            String id = Objects.toString(m.get("id"), "");
            String nombre = Objects.toString(m.get("nombre"), "");
            productoIdToName.put(id, nombre);
            productoNameToId.put(nombre, id);

            Map<String, String> meta = new HashMap<>();
            meta.put("marca", Objects.toString(m.get("marca"), ""));
            meta.put("material", Objects.toString(m.get("material"), ""));
            meta.put("unidad", Objects.toString(m.get("unidad"), ""));
            meta.put("descripcion", Objects.toString(m.get("descripcion"), ""));
            productoMeta.put(id, meta);

            nombres.add(nombre);
            ids.add(id);
        }
        cbProductoNombre.getItems().setAll(nombres);
        cbProductoId.getItems().setAll(ids);
        cbProductoNombre.setEditable(true);
        cbProductoId.setEditable(true);
    }

    private void configurarListeners() {
        // PROVEEDOR: nombre -> id (al perder focus o seleccionar)
        cbProveedorNombre.getEditor().focusedProperty().addListener((obs, was, isNow) -> {
            if (!isNow) validarYSincronizarProveedorDesdeNombre();
        });
        cbProveedorNombre.setOnAction(e -> sincronizarProveedorPorNombre(cbProveedorNombre.getEditor().getText()));

        // PROVEEDOR: id -> nombre
        cbProveedorId.getEditor().focusedProperty().addListener((obs, was, isNow) -> {
            if (!isNow) validarYSincronizarProveedorDesdeId();
        });
        cbProveedorId.setOnAction(e -> {
            Integer id = null;
            try {
                id = cbProveedorId.getValue();
                if (id == null) {
                    String text = cbProveedorId.getEditor().getText();
                    if (!text.isBlank()) id = Integer.parseInt(text.trim());
                }
            } catch (NumberFormatException ex) {
                mostrarAdvertencia("ID de proveedor inválido.");
                return;
            }
            sincronizarProveedorPorId(id);
        });

        // PRODUCTO: nombre -> id
        cbProductoNombre.getEditor().focusedProperty().addListener((obs, was, isNow) -> {
            if (!isNow) validarYSincronizarProductoDesdeNombre();
        });
        cbProductoNombre.setOnAction(e -> sincronizarProductoPorNombre(cbProductoNombre.getEditor().getText()));

        // PRODUCTO: id -> nombre (id es String)
        cbProductoId.getEditor().focusedProperty().addListener((obs, was, isNow) -> {
            if (!isNow) validarYSincronizarProductoDesdeId();
        });
        cbProductoId.setOnAction(e -> {
            String id = cbProductoId.getValue();
            if (id == null) {
                String text = cbProductoId.getEditor().getText();
                if (!text.isBlank()) id = text.trim();
            }
            sincronizarProductoPorId(id);
        });
    }

    private void sincronizarProveedorPorNombre(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) {
            cbProveedorId.getSelectionModel().clearSelection();
            cbProveedorId.getEditor().clear();
            return;
        }
        Integer id = proveedorNameToId.get(nombre);
        if (id == null) {
            Optional<Map.Entry<String,Integer>> found = proveedorNameToId.entrySet().stream()
                    .filter(en -> en.getKey().equalsIgnoreCase(nombre.trim()))
                    .findFirst();
            if (found.isPresent()) id = found.get().getValue();
        }
        if (id == null) {
            mostrarAdvertencia("Proveedor no encontrado: " + nombre);
            return;
        }
        cbProveedorId.getSelectionModel().select(id);
        cbProveedorId.getEditor().setText(String.valueOf(id));
    }

    private void sincronizarProveedorPorId(Integer id) {
        if (id == null) {
            cbProveedorNombre.getSelectionModel().clearSelection();
            cbProveedorNombre.getEditor().clear();
            return;
        }
        String nombre = proveedorIdToName.get(id);
        if (nombre == null) {
            mostrarAdvertencia("Proveedor con ID " + id + " no encontrado.");
            return;
        }
        cbProveedorNombre.getSelectionModel().select(nombre);
        cbProveedorNombre.getEditor().setText(nombre);
    }

    private void validarYSincronizarProveedorDesdeNombre() {
        String nombre = cbProveedorNombre.getEditor().getText();
        if (nombre == null || nombre.isBlank()) return;
        sincronizarProveedorPorNombre(nombre);
    }

    private void validarYSincronizarProveedorDesdeId() {
        String text = cbProveedorId.getEditor().getText();
        if (text == null || text.isBlank()) return;
        try {
            Integer id = Integer.parseInt(text.trim());
            sincronizarProveedorPorId(id);
        } catch (NumberFormatException ex) {
            mostrarAdvertencia("ID de proveedor inválido.");
        }
    }

    private void sincronizarProductoPorNombre(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) {
            cbProductoId.getSelectionModel().clearSelection();
            cbProductoId.getEditor().clear();
            txtDescripcion.clear();
            return;
        }
        String id = productoNameToId.get(nombre);
        if (id == null) {
            Optional<Map.Entry<String,String>> found = productoNameToId.entrySet().stream()
                    .filter(en -> en.getKey().equalsIgnoreCase(nombre.trim()))
                    .findFirst();
            if (found.isPresent()) id = found.get().getValue();
        }
        if (id == null) {
            mostrarAdvertencia("Producto no encontrado: " + nombre);
            return;
        }
        cbProductoId.getSelectionModel().select(id);
        cbProductoId.getEditor().setText(id);
        rellenarDescripcionProducto(id);
    }

    private void sincronizarProductoPorId(String id) {
        if (id == null || id.isBlank()) {
            cbProductoNombre.getSelectionModel().clearSelection();
            cbProductoNombre.getEditor().clear();
            txtDescripcion.clear();
            return;
        }
        String nombre = productoIdToName.get(id);
        if (nombre == null) {
            mostrarAdvertencia("Producto con ID " + id + " no encontrado.");
            return;
        }
        cbProductoNombre.getSelectionModel().select(nombre);
        cbProductoNombre.getEditor().setText(nombre);
        rellenarDescripcionProducto(id);
    }

    private void validarYSincronizarProductoDesdeNombre() {
        String nombre = cbProductoNombre.getEditor().getText();
        if (nombre == null || nombre.isBlank()) return;
        sincronizarProductoPorNombre(nombre);
    }

    private void validarYSincronizarProductoDesdeId() {
        String text = cbProductoId.getEditor().getText();
        if (text == null || text.isBlank()) return;
        String id = text.trim();
        sincronizarProductoPorId(id);
    }

    private void rellenarDescripcionProducto(String id) {
        Map<String, String> meta = productoMeta.get(id);
        if (meta == null) {
            txtDescripcion.clear();
            return;
        }
        String marca = meta.getOrDefault("marca", "");
        String material = meta.getOrDefault("material", "");
        String unidad = meta.getOrDefault("unidad", "");
        String desc = meta.getOrDefault("descripcion", "");
        StringBuilder sb = new StringBuilder();
        if (!marca.isEmpty()) sb.append(marca);
        if (!material.isEmpty()) {
            if (sb.length() > 0) sb.append(" | ");
            sb.append(material);
        }
        if (!unidad.isEmpty()) {
            if (sb.length() > 0) sb.append(" | ");
            sb.append(unidad);
        }
        if (!desc.isEmpty()) {
            if (sb.length() > 0) sb.append(" | ");
            sb.append(desc);
        }
        txtDescripcion.setText(sb.toString());
    }

    @FXML
    private void onGuardar() {
        try {
            String claveAltText = txtClaveAlterna.getText();
            if (claveAltText == null || claveAltText.trim().isEmpty()) {
                mostrarAdvertencia("La clave alterna (idClaveCatalogo) es obligatoria.");
                return;
            }
            String idClaveCatalogo = claveAltText.trim(); // ahora es String (varchar)

            // proveedor
            Integer proveedorId = null;
            String provIdText = cbProveedorId.getEditor().getText();
            if (provIdText != null && !provIdText.isBlank()) {
                try { proveedorId = Integer.parseInt(provIdText.trim()); } catch (NumberFormatException ex) { proveedorId = null; }
            }
            if (proveedorId == null) {
                String provNameText = cbProveedorNombre.getEditor().getText();
                if (provNameText != null && !provNameText.isBlank()) {
                    proveedorId = proveedorNameToId.get(provNameText);
                }
            }
            if (proveedorId == null) {
                mostrarAdvertencia("Debe seleccionar o escribir un proveedor válido.");
                return;
            }

            // producto (id string)
            String productoId = null;
            String prodIdText = cbProductoId.getEditor().getText();
            if (prodIdText != null && !prodIdText.isBlank()) {
                productoId = prodIdText.trim();
            } else {
                String prodNameText = cbProductoNombre.getEditor().getText();
                if (prodNameText != null && !prodNameText.isBlank()) {
                    productoId = productoNameToId.get(prodNameText);
                }
            }
            if (productoId == null || productoId.isBlank()) {
                mostrarAdvertencia("Debe seleccionar o escribir un producto válido.");
                return;
            }

            boolean ok = model.guardarClave(idClaveCatalogo, proveedorId, productoId);
            if (ok) {
                mostrarInfo("Clave guardada/actualizada correctamente.");
                cerrarVentana();
            } else {
                mostrarError("No se pudo guardar la clave en la base de datos.");
            }

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("Error al guardar: " + e.getMessage());
        }
    }

    @FXML
    private void onCancelar() {
        cerrarVentana();
    }

    private void cerrarVentana() {
        Window w = btnCancelar.getScene().getWindow();
        if (w != null) w.hide();
    }

    private void mostrarAdvertencia(String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    private void mostrarError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    private void mostrarInfo(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}
