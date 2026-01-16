package Formularios.model;

import Consultas.producto.model.producto;
import Consultas.producto.model.etiqueta;
import Consultas.producto.model.marca;
import Consultas.producto.model.modelEtiqueta;
import Consultas.producto.model.modelMarca;
import Compartido.model.DAO.GenericDAO;

public class modelNuevoProducto {

    private GenericDAO<producto> productoDAO;
    private modelEtiqueta modelEtiqueta;
    private modelMarca modelMarca;

    public modelNuevoProducto() {
        this.productoDAO = new GenericDAO<>(producto.class);
        this.modelEtiqueta = new modelEtiqueta();
        this.modelMarca = new modelMarca();
    }

    // Metodo para guardar producto (nuevo)
    public boolean guardarProducto(producto p) {
        return productoDAO.insertar(p);
    }

    // Metodo para modificar producto (existente)
    public boolean modificarProducto(producto p) {
        return productoDAO.actualizar(p);
    }

    // Metodo para buscar producto por ID
    public producto buscarProductoPorId(String id) {
        return productoDAO.buscarExacto("id", id);
    }

    // Métodos para manejar etiquetas
    public etiqueta buscarEtiquetaPorNombre(String nombre) {
        return modelEtiqueta.buscarPorNombre(nombre);
    }

    public String crearOActualizarEtiqueta(String nombre) {
        // Primero buscamos si ya existe (insensible a mayúsculas/minúsculas)
        etiqueta etiquetaExistente = buscarEtiquetaPorNombreInsensible(nombre);

        if (etiquetaExistente != null) {
            // Si existe, actualizamos el nombre por si hay cambios de formato
            if (!etiquetaExistente.getNombre().equals(nombre)) {
                etiquetaExistente.setNombre(nombre);
                modelEtiqueta.actualizarEtiqueta(etiquetaExistente);
            }
            return etiquetaExistente.getId();
        } else {
            // Si no existe, creamos una nueva
            etiqueta nuevaEtiqueta = new etiqueta();
            nuevaEtiqueta.setNombre(nombre);
            if (modelEtiqueta.insertarEtiqueta(nuevaEtiqueta)) {
                return nuevaEtiqueta.getId();
            }
        }
        return null;
    }

    private etiqueta buscarEtiquetaPorNombreInsensible(String nombre) {
        // Necesitamos implementar un método en ModelEtiqueta para búsqueda insensible
        // Por ahora, vamos a obtener todas y buscar manualmente
        java.util.ArrayList<etiqueta> todas = modelEtiqueta.obtenerTodas();
        for (etiqueta e : todas) {
            if (e.getNombre() != null && e.getNombre().equalsIgnoreCase(nombre)) {
                return e;
            }
        }
        return null;
    }

    // Métodos para manejar marcas
    public marca buscarMarcaPorNombre(String nombre) {
        return modelMarca.buscarPorNombre(nombre);
    }

    public String crearOActualizarMarca(String nombre) {
        // Primero buscamos si ya existe (insensible a mayúsculas/minúsculas)
        marca marcaExistente = buscarMarcaPorNombreInsensible(nombre);

        if (marcaExistente != null) {
            // Si existe, actualizamos el nombre por si hay cambios de formato
            if (!marcaExistente.getNombre().equals(nombre)) {
                marcaExistente.setNombre(nombre);
                modelMarca.actualizarMarca(marcaExistente);
            }
            return marcaExistente.getId();
        } else {
            // Si no existe, creamos una nueva
            marca nuevaMarca = new marca();
            nuevaMarca.setNombre(nombre);
            if (modelMarca.insertarMarca(nuevaMarca)) {
                return nuevaMarca.getId();
            }
        }
        return null;
    }

    private marca buscarMarcaPorNombreInsensible(String nombre) {
        // Necesitamos implementar un metodo en ModelMarca para búsqueda insensible
        // Por ahora, vamos a obtener todas y buscar manualmente
        java.util.ArrayList<marca> todas = modelMarca.obtenerTodas();
        for (marca m : todas) {
            if (m.getNombre() != null && m.getNombre().equalsIgnoreCase(nombre)) {
                return m;
            }
        }
        return null;
    }

    // Metodo para obtener etiqueta por ID
    public etiqueta obtenerEtiquetaPorId(String id) {
        return modelEtiqueta.buscarPorId(id);
    }

    // Metodo para obtener marca por ID
    public marca obtenerMarcaPorId(String id) {
        return modelMarca.buscarPorId(id);
    }
}