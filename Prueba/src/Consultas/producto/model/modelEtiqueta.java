package Consultas.producto.model;

import Compartido.model.DAO.GenericDAO;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class modelEtiqueta {

    private GenericDAO<etiqueta> etiquetaDAO;

    public modelEtiqueta() {
        this.etiquetaDAO = new GenericDAO<>(etiqueta.class);
    }

    // Obtener todas las etiquetas
    public ArrayList<etiqueta> obtenerTodas() {
        ArrayList<etiqueta> todas = etiquetaDAO.obtenerTodos();
        ArrayList<etiqueta> activas = new ArrayList<>();
        for (etiqueta e : todas) {
            if (e != null && "activo".equalsIgnoreCase(e.getEstado())) {
                activas.add(e);
            }
        }
        return activas;
    }

    public ArrayList<etiqueta> obtenerTodasIncluyendoInactivas() {
        return etiquetaDAO.obtenerTodos();
    }

    // Obtener mapa de id->nombre para usar en combos o tablas
    public Map<String, String> obtenerMapaEtiquetas() {
        Map<String, String> mapa = new HashMap<>();
        ArrayList<etiqueta> etiquetas = obtenerTodas();

        for (etiqueta etiqueta : etiquetas) {
            mapa.put(etiqueta.getId(), etiqueta.getNombre());
        }

        return mapa;
    }

    // Buscar etiqueta por ID
    public etiqueta buscarPorId(String id) {
        return etiquetaDAO.buscarExacto("id", id);
    }

    // Insertar nueva etiqueta
    public boolean insertarEtiqueta(etiqueta etiqueta) {
        return etiquetaDAO.insertar(etiqueta);
    }

    // Actualizar etiqueta
    public boolean actualizarEtiqueta(etiqueta etiqueta) {
        return etiquetaDAO.actualizar(etiqueta);
    }

    // Eliminar etiqueta
    public boolean eliminarEtiqueta(String id) {
        return etiquetaDAO.eliminar(id);
    }

    public etiqueta buscarPorNombre(String nombre) {
        // Primero intentamos con búsqueda exacta
        etiqueta encontrada = null;
        ArrayList<etiqueta> todas = obtenerTodasIncluyendoInactivas();

        for (etiqueta e : todas) {
            if (e.getNombre() != null && e.getNombre().equalsIgnoreCase(nombre)) {
                encontrada = e;
                break;
            }
        }

        return encontrada;
    }
}
