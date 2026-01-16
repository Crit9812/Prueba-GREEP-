package Consultas.claves.model;

import Compartido.model.DAO.GenericDAO;
import conexion.Conexion;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.sql.Connection;
import java.util.ArrayList;

public class model {

    private final GenericDAO<claves> claveDAO;
    private final dao clavesDAO;

    public model() {
        this.claveDAO = new GenericDAO<>(claves.class);
        this.clavesDAO = new dao();
    }

    // --- USANDO GenericDAO (NO SE TOCA) ---

    public ObservableList<claves> obtener() {
        ArrayList<claves> lista = claveDAO.obtenerTodos();
        return FXCollections.observableArrayList(lista);
    }

    public boolean eliminar(String id) {
        if (id == null || id.isBlank()) {
            return false;
        }
        if (contarArticulosDisponibles(id) > 0) {
            return false;
        }
        return claveDAO.eliminar(id);
    }

    public int contarArticulosDisponibles(String idAlterno) {
        if (idAlterno == null || idAlterno.isBlank()) {
            return 0;
        }

        claves clave = claveDAO.buscarExacto("idAlterno", idAlterno);
        if (clave == null) {
            return 0;
        }

        String idProducto = clave.getIdProducto();
        Integer idProveedor = clave.getIdProveedor();
        String proveedor = idProveedor != null ? String.valueOf(idProveedor) : null;

        if ((idProducto == null || idProducto.isBlank()) && proveedor == null) {
            return 0;
        }

        try (Connection conn = new Conexion().conectar()) {
            return GenericDAO.contarArticulosDisponiblesPorProductoProveedor(conn, idProducto, proveedor);
        } catch (Exception e) {
            System.out.println("Error en contarArticulosDisponiblesClaves: " + e.getMessage());
        }

        return 0;
    }

    // --- USANDO DAO ESPECÍFICO ---

    public ObservableList<String[]> obtenerParaTabla() {
        return FXCollections.observableArrayList(
                clavesDAO.obtenerClavesCompletas()
        );
    }

    public ObservableList<String[]> buscarEnTabla(String textoBusqueda) {

        if (textoBusqueda == null || textoBusqueda.trim().isEmpty()) {
            return obtenerParaTabla();
        }

        return FXCollections.observableArrayList(
                clavesDAO.buscarClavesCompletas(textoBusqueda.trim())
        );
    }
}
