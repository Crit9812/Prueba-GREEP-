package Consultas.producto.model;

import Compartido.model.DAO.GenericDAO;
import conexion.Conexion;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Map;

public class model {

    private GenericDAO<producto> productoDAO;
    private modelEtiqueta modelEtiqueta;
    private modelMarca modelMarca;

    public model() {
        this.productoDAO = new GenericDAO<>(producto.class);
        this.modelEtiqueta = new modelEtiqueta();
        this.modelMarca = new modelMarca();
    }

    // Metodo para obtener todos los productos
    public ObservableList<producto> obtenerProductos() {
        ArrayList<producto> lista = productoDAO.obtenerTodos();
        return FXCollections.observableArrayList(filtrarActivos(lista));
    }

    // Metodo para desactivar un producto por ID
    public boolean eliminarProducto(String id) {
        producto producto = buscarProductoPorId(id);
        if (producto == null) {
            return false;
        }
        producto.setEstado("desactivado");
        return productoDAO.actualizar(producto);
    }

    // Metodo para buscar productos por un campo específico
    public ObservableList<producto> buscarProductos(String campo, String valor) {
        ArrayList<producto> lista = productoDAO.buscarParcial(campo, valor);
        return FXCollections.observableArrayList(filtrarActivos(lista));
    }

    public ObservableList<producto> busquedaMultipleProductos(String textoBusqueda) {
        ArrayList<producto> lista = productoDAO.buscarMultiple("id", "nombre", textoBusqueda);
        return FXCollections.observableArrayList(filtrarActivos(lista));
    }

    // Metodo para buscar un producto por su ID
    public producto buscarProductoPorId(String id) {
        return productoDAO.buscarExacto("id", id);
    }

    public int contarEntradasPorProducto(String idProducto) {
        return contarRegistros("SELECT COUNT(*) FROM detalle_Entrada WHERE claveProducto = ?", idProducto);
    }

    public int contarSalidasPorProducto(String idProducto) {
        return contarRegistros("SELECT COUNT(*) FROM detalle_Salida WHERE claveProductoSalida = ?", idProducto);
    }

    public int contarClavesPorProducto(String idProducto) {
        return contarRegistros("SELECT COUNT(*) FROM claves WHERE idProducto = ? AND estado = 'activo'", idProducto);
    }

    private int contarRegistros(String sql, String valor) {
        try (Connection conn = new Conexion().conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, valor);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    // Metodo para actualizar solo la URL de la imagen
    public boolean actualizarUrlImagen(String id, String nuevaUrl) {
        producto productoActual = buscarProductoPorId(id);
        if (productoActual == null) {
            return false;
        }

        productoActual.setUrlImagen(nuevaUrl);
        return productoDAO.actualizar(productoActual);
    }

    // ===== MÉTODOS PARA ETIQUETAS Y MARCAS =====

    // Obtener mapa de etiquetas (id->nombre)
    public Map<String, String> obtenerMapaEtiquetas() {
        return modelEtiqueta.obtenerMapaEtiquetas();
    }

    // Obtener mapa de marcas (id->nombre)
    public Map<String, String> obtenerMapaMarcas() {
        return modelMarca.obtenerMapaMarcas();
    }

    // Obtener lista de etiquetas para combobox
    public ObservableList<etiqueta> obtenerListaEtiquetas() {
        ArrayList<etiqueta> lista = modelEtiqueta.obtenerTodas();
        return FXCollections.observableArrayList(lista);
    }

    // Obtener lista de marcas para combobox
    public ObservableList<marca> obtenerListaMarcas() {
        ArrayList<marca> lista = modelMarca.obtenerTodas();
        return FXCollections.observableArrayList(lista);
    }

    // Buscar etiqueta por ID
    public etiqueta buscarEtiquetaPorId(String id) {
        return modelEtiqueta.buscarPorId(id);
    }

    // Buscar marca por ID
    public marca buscarMarcaPorId(String id) {
        return modelMarca.buscarPorId(id);
    }

    private ArrayList<producto> filtrarActivos(ArrayList<producto> lista) {
        ArrayList<producto> activos = new ArrayList<>();
        for (producto p : lista) {
            if (p != null && "activo".equalsIgnoreCase(p.getEstado())) {
                activos.add(p);
            }
        }
        return activos;
    }
}
