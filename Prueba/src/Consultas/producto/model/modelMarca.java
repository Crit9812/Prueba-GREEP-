package Consultas.producto.model;

import Compartido.model.DAO.GenericDAO;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class modelMarca {

    private GenericDAO<marca> marcaDAO;

    public modelMarca() {
        this.marcaDAO = new GenericDAO<>(marca.class);
    }

    // Obtener todas las marcas
    public ArrayList<marca> obtenerTodas() {
        ArrayList<marca> todas = marcaDAO.obtenerTodos();
        ArrayList<marca> activas = new ArrayList<>();
        for (marca m : todas) {
            if (m != null && "activo".equalsIgnoreCase(m.getEstado())) {
                activas.add(m);
            }
        }
        return activas;
    }

    public ArrayList<marca> obtenerTodasIncluyendoInactivas() {
        return marcaDAO.obtenerTodos();
    }

    // Obtener mapa de id->nombre para usar en combos o tablas
    public Map<String, String> obtenerMapaMarcas() {
        Map<String, String> mapa = new HashMap<>();
        ArrayList<marca> marcas = obtenerTodas();

        for (marca marca : marcas) {
            mapa.put(marca.getId(), marca.getNombre());
        }

        return mapa;
    }

    // Buscar marca por ID
    public marca buscarPorId(String id) {
        return marcaDAO.buscarExacto("id", id);
    }

    // Insertar nueva marca
    public boolean insertarMarca(marca marca) {
        return marcaDAO.insertar(marca);
    }

    // Actualizar marca
    public boolean actualizarMarca(marca marca) {
        return marcaDAO.actualizar(marca);
    }

    // Eliminar marca
    public boolean eliminarMarca(String id) {
        marca existente = buscarPorId(id);
        if (existente == null) {
            return false;
        }
        existente.setEstado("desactivado");
        return marcaDAO.actualizar(existente);
    }

    public marca buscarPorNombre(String nombre) {
        // Primero intentamos con búsqueda exacta
        marca encontrada = null;
        ArrayList<marca> todas = obtenerTodasIncluyendoInactivas();

        for (marca m : todas) {
            if (m.getNombre() != null && m.getNombre().equalsIgnoreCase(nombre)) {
                encontrada = m;
                break;
            }
        }

        return encontrada;
    }
}
