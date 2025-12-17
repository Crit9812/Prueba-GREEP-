package Consultas.producto.model;

import Compartido.model.DAO.GenericDAO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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
        return FXCollections.observableArrayList(lista);
    }

    // Metodo para eliminar un producto por ID
    public boolean eliminarProducto(String id) {
        return productoDAO.eliminar(id);
    }

    // Metodo para buscar productos por un campo específico
    public ObservableList<producto> buscarProductos(String campo, String valor) {
        ArrayList<producto> lista = productoDAO.buscarParcial(campo, valor);
        return FXCollections.observableArrayList(lista);
    }

    public ObservableList<producto> busquedaMultipleProductos(String textoBusqueda) {
        ArrayList<producto> lista = productoDAO.buscarMultiple("id", "nombre", textoBusqueda);
        return FXCollections.observableArrayList(lista);
    }

    // Metodo para buscar un producto por su ID
    public producto buscarProductoPorId(String id) {
        return productoDAO.buscarExacto("id", id);
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
}