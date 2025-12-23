package Compartido.controller;

import Compartido.model.ProductoComboItem;
import conexion.Conexion;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ComboBox;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

public class productoCboxController {

    private ComboBox<String> cbIdProducto;
    private ComboBox<String> cbNombreProducto;
    private ComboBox<String> cbClaveAlterna;

    private final Map<String, String> idToNombre = new HashMap<>();
    private final Map<String, String> nombreToId = new HashMap<>();
    private final Map<String, String> idToDescripcion = new HashMap<>();
    private final Map<String, List<String>> idToAlternos = new HashMap<>();
    private final Map<String, String> alternoToId = new HashMap<>();

    private boolean actualizando = false;

    public void inicializar(ComboBox<String> cbIdProducto,
                            ComboBox<String> cbNombreProducto,
                            ComboBox<String> cbClaveAlterna) {
        this.cbIdProducto = cbIdProducto;
        this.cbNombreProducto = cbNombreProducto;
        this.cbClaveAlterna = cbClaveAlterna;

        this.cbIdProducto.setEditable(true);
        this.cbNombreProducto.setEditable(true);
        this.cbClaveAlterna.setEditable(true);

        cargarProductosGenerales();
        configurarListeners();
    }

    public void setProductos(List<ProductoComboItem> productos) {
        cargarProductosDesdeLista(productos);
    }

    public void limpiarSeleccion() {
        if (cbIdProducto != null) {
            cbIdProducto.getSelectionModel().clearSelection();
            cbIdProducto.getEditor().clear();
        }
        if (cbNombreProducto != null) {
            cbNombreProducto.getSelectionModel().clearSelection();
            cbNombreProducto.getEditor().clear();
        }
        if (cbClaveAlterna != null) {
            cbClaveAlterna.getSelectionModel().clearSelection();
            cbClaveAlterna.getEditor().clear();
        }
    }

    public boolean validarSeleccion() {
        String id = getIdSeleccionado();
        String nombre = getNombreSeleccionado();
        if (id == null || id.isBlank() || nombre == null || nombre.isBlank()) {
            return false;
        }
        String nombreEsperado = idToNombre.get(id);
        return nombreEsperado != null && nombreEsperado.equalsIgnoreCase(nombre.trim());
    }

    public String getIdSeleccionado() {
        if (cbIdProducto == null) return null;
        String id = cbIdProducto.getValue();
        if (id == null || id.isBlank()) {
            id = cbIdProducto.getEditor().getText();
        }
        return id == null ? null : id.trim();
    }

    public String getNombreSeleccionado() {
        if (cbNombreProducto == null) return null;
        String nombre = cbNombreProducto.getValue();
        if (nombre == null || nombre.isBlank()) {
            nombre = cbNombreProducto.getEditor().getText();
        }
        return nombre == null ? null : nombre.trim();
    }

    public String getClaveAlternaSeleccionada() {
        if (cbClaveAlterna == null) return null;
        String clave = cbClaveAlterna.getValue();
        if (clave == null || clave.isBlank()) {
            clave = cbClaveAlterna.getEditor().getText();
        }
        return clave == null ? null : clave.trim();
    }

    public String getDescripcionSeleccionada() {
        String id = getIdSeleccionado();
        if (id == null || id.isBlank()) return "";
        return idToDescripcion.getOrDefault(id, "");
    }

    public void setSeleccion(String idProducto, String nombreProducto) {
        if (cbIdProducto == null || cbNombreProducto == null) return;
        actualizando = true;
        cbIdProducto.getSelectionModel().select(idProducto);
        cbIdProducto.getEditor().setText(idProducto);
        cbNombreProducto.getSelectionModel().select(nombreProducto);
        cbNombreProducto.getEditor().setText(nombreProducto);
        sincronizarClaveAlternaPorId(idProducto);
        actualizando = false;
    }

    public void setSeleccionPorClaveAlterna(String claveAlterna) {
        if (claveAlterna == null || claveAlterna.isBlank()) return;
        String id = alternoToId.get(claveAlterna);
        if (id == null) return;
        String nombre = idToNombre.get(id);
        if (nombre == null) return;
        setSeleccion(id, nombre);
        if (cbClaveAlterna != null) {
            cbClaveAlterna.getSelectionModel().select(claveAlterna);
            cbClaveAlterna.getEditor().setText(claveAlterna);
        }
    }

    private void configurarListeners() {
        cbIdProducto.setOnAction(e -> sincronizarProductoPorId(cbIdProducto.getEditor().getText()));
        cbNombreProducto.setOnAction(e -> sincronizarProductoPorNombre(cbNombreProducto.getEditor().getText()));
        cbClaveAlterna.setOnAction(e -> sincronizarProductoPorClaveAlterna(cbClaveAlterna.getEditor().getText()));

        cbIdProducto.getEditor().focusedProperty().addListener((obs, was, isNow) -> {
            if (!isNow) sincronizarProductoPorId(cbIdProducto.getEditor().getText());
        });
        cbNombreProducto.getEditor().focusedProperty().addListener((obs, was, isNow) -> {
            if (!isNow) sincronizarProductoPorNombre(cbNombreProducto.getEditor().getText());
        });
        cbClaveAlterna.getEditor().focusedProperty().addListener((obs, was, isNow) -> {
            if (!isNow) sincronizarProductoPorClaveAlterna(cbClaveAlterna.getEditor().getText());
        });
    }

    private void sincronizarProductoPorId(String id) {
        if (actualizando) return;
        if (id == null || id.isBlank()) {
            limpiarSeleccion();
            return;
        }
        String nombre = idToNombre.get(id.trim());
        if (nombre == null) return;
        actualizando = true;
        cbIdProducto.getSelectionModel().select(id.trim());
        cbIdProducto.getEditor().setText(id.trim());
        cbNombreProducto.getSelectionModel().select(nombre);
        cbNombreProducto.getEditor().setText(nombre);
        sincronizarClaveAlternaPorId(id.trim());
        actualizando = false;
    }

    private void sincronizarProductoPorNombre(String nombre) {
        if (actualizando) return;
        if (nombre == null || nombre.isBlank()) {
            limpiarSeleccion();
            return;
        }
        String id = nombreToId.get(nombre.trim());
        if (id == null) return;
        actualizando = true;
        cbNombreProducto.getSelectionModel().select(nombre.trim());
        cbNombreProducto.getEditor().setText(nombre.trim());
        cbIdProducto.getSelectionModel().select(id);
        cbIdProducto.getEditor().setText(id);
        sincronizarClaveAlternaPorId(id);
        actualizando = false;
    }

    private void sincronizarProductoPorClaveAlterna(String claveAlterna) {
        if (actualizando) return;
        if (claveAlterna == null || claveAlterna.isBlank()) return;
        String id = alternoToId.get(claveAlterna.trim());
        if (id == null) return;
        String nombre = idToNombre.get(id);
        if (nombre == null) return;
        actualizando = true;
        cbIdProducto.getSelectionModel().select(id);
        cbIdProducto.getEditor().setText(id);
        cbNombreProducto.getSelectionModel().select(nombre);
        cbNombreProducto.getEditor().setText(nombre);
        cbClaveAlterna.getSelectionModel().select(claveAlterna.trim());
        cbClaveAlterna.getEditor().setText(claveAlterna.trim());
        actualizando = false;
    }

    private void sincronizarClaveAlternaPorId(String id) {
        if (cbClaveAlterna == null) return;
        List<String> alternos = idToAlternos.getOrDefault(id, Collections.emptyList());
        if (alternos.isEmpty()) {
            cbClaveAlterna.getSelectionModel().clearSelection();
            cbClaveAlterna.getEditor().clear();
            return;
        }
        String actual = cbClaveAlterna.getEditor().getText();
        if (actual != null && alternos.contains(actual)) {
            cbClaveAlterna.getSelectionModel().select(actual);
            cbClaveAlterna.getEditor().setText(actual);
        } else {
            cbClaveAlterna.getSelectionModel().select(alternos.get(0));
            cbClaveAlterna.getEditor().setText(alternos.get(0));
        }
    }

    private void cargarProductosGenerales() {
        List<ProductoComboItem> productos = new ArrayList<>();
        String sql = "SELECT p.id AS producto_id, p.nombre AS producto_nombre, " +
                "m.nombre AS marca_nombre, e.nombre AS etiqueta_nombre, " +
                "p.material AS material, p.unidadMedida AS unidad, p.descripcion AS descripcion, " +
                "p.marca AS raw_marca, p.etiqueta AS raw_etiqueta, " +
                "c.idAlterno AS clave_alterna " +
                "FROM productos p " +
                "LEFT JOIN marcas m ON m.id = p.marca " +
                "LEFT JOIN etiquetas e ON e.id = p.etiqueta " +
                "LEFT JOIN claves c ON c.idProducto = p.id";

        try (Connection conn = new Conexion().conectar();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String id = rs.getString("producto_id");
                String nombre = rs.getString("producto_nombre");
                String marca = rs.getString("marca_nombre");
                String etiqueta = rs.getString("etiqueta_nombre");
                String material = rs.getString("material");
                String unidad = rs.getString("unidad");
                String descripcion = rs.getString("descripcion");
                String rawMarca = rs.getString("raw_marca");
                String rawEtiqueta = rs.getString("raw_etiqueta");
                String claveAlterna = rs.getString("clave_alterna");

                if ((marca == null || marca.isBlank()) && rawMarca != null && !rawMarca.isBlank()) {
                    marca = rawMarca;
                }
                if ((etiqueta == null || etiqueta.isBlank()) && rawEtiqueta != null && !rawEtiqueta.isBlank()) {
                    etiqueta = rawEtiqueta;
                }

                String descripcionFinal = construirDescripcion(marca, etiqueta, material, unidad, descripcion);
                productos.add(new ProductoComboItem(id, nombre, descripcionFinal, claveAlterna));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        cargarProductosDesdeLista(productos);
    }

    private void cargarProductosDesdeLista(List<ProductoComboItem> productos) {
        idToNombre.clear();
        nombreToId.clear();
        idToDescripcion.clear();
        idToAlternos.clear();
        alternoToId.clear();

        Set<String> ids = new LinkedHashSet<>();
        Set<String> nombres = new LinkedHashSet<>();
        Set<String> alternos = new LinkedHashSet<>();

        for (ProductoComboItem item : productos) {
            if (item == null) continue;
            String id = item.getIdProducto();
            String nombre = item.getNombreProducto();
            String descripcion = item.getDescripcion();
            String claveAlterna = item.getClaveAlterna();

            if (id != null && !id.isBlank()) {
                ids.add(id);
                if (nombre != null) {
                    idToNombre.putIfAbsent(id, nombre);
                }
                if (descripcion != null) {
                    idToDescripcion.putIfAbsent(id, descripcion);
                }
                if (claveAlterna != null && !claveAlterna.isBlank()) {
                    idToAlternos.computeIfAbsent(id, key -> new ArrayList<>()).add(claveAlterna);
                    alternoToId.put(claveAlterna, id);
                    alternos.add(claveAlterna);
                }
            }

            if (nombre != null && !nombre.isBlank() && id != null && !id.isBlank()) {
                nombres.add(nombre);
                nombreToId.putIfAbsent(nombre, id);
            }
        }

        if (cbIdProducto != null) {
            cbIdProducto.setItems(FXCollections.observableArrayList(ids));
        }
        if (cbNombreProducto != null) {
            cbNombreProducto.setItems(FXCollections.observableArrayList(nombres));
        }
        if (cbClaveAlterna != null) {
            cbClaveAlterna.setItems(FXCollections.observableArrayList(alternos));
        }
    }

    private String construirDescripcion(String marca, String etiqueta, String material, String unidad, String descripcion) {
        StringBuilder sb = new StringBuilder();
        if (marca != null && !marca.isBlank()) sb.append(marca.trim());
        if (etiqueta != null && !etiqueta.isBlank()) {
            if (sb.length() > 0) sb.append(" | ");
            sb.append(etiqueta.trim());
        }
        if (material != null && !material.isBlank()) {
            if (sb.length() > 0) sb.append(" | ");
            sb.append(material.trim());
        }
        if (unidad != null && !unidad.isBlank()) {
            if (sb.length() > 0) sb.append(" | ");
            sb.append(unidad.trim());
        }
        if (descripcion != null && !descripcion.isBlank()) {
            if (sb.length() > 0) sb.append(" | ");
            sb.append(descripcion.trim());
        }
        return sb.toString();
    }
}
