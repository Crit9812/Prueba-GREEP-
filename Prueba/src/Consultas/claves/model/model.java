package Consultas.claves.model;

import Compartido.model.DAO.GenericDAO;
import conexion.Conexion;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;

public class model {

    private final GenericDAO<claves> claveDAO;

    public model() {
        this.claveDAO = new GenericDAO<>(claves.class);
    }

    // --- USANDO GenericDAO (NO SE TOCA) ---

    public ObservableList<claves> obtener() {
        ArrayList<claves> lista = claveDAO.obtenerTodos();
        ArrayList<claves> activos = new ArrayList<>();
        for (claves clave : lista) {
            if (clave.getEstado() != null && clave.getEstado().equalsIgnoreCase("activo")) {
                activos.add(clave);
            }
        }
        return FXCollections.observableArrayList(activos);
    }

    public boolean eliminar(String id) {
        return claveDAO.eliminar(id);
    }

    // --- USANDO DAO ESPECÍFICO ---

    public ObservableList<String[]> obtenerParaTabla() {
        return FXCollections.observableArrayList(obtenerClavesActivas(""));
    }

    public ObservableList<String[]> buscarEnTabla(String textoBusqueda) {

        if (textoBusqueda == null || textoBusqueda.trim().isEmpty()) {
            return obtenerParaTabla();
        }

        return FXCollections.observableArrayList(obtenerClavesActivas(textoBusqueda.trim()));
    }

    private ArrayList<String[]> obtenerClavesActivas(String filtro) {
        ArrayList<String[]> resultados = new ArrayList<>();
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT c.idAlterno, c.idProducto, p.nombre AS producto, ")
                .append("c.idProveedor, pr.Nombre AS proveedor, p.descripcion ")
                .append("FROM claves c ")
                .append("LEFT JOIN productos p ON p.id = c.idProducto ")
                .append("LEFT JOIN proveedores pr ON pr.id = c.idProveedor ")
                .append("WHERE LOWER(c.estado) = 'activo' ");

        boolean tieneFiltro = filtro != null && !filtro.isBlank();
        if (tieneFiltro) {
            sql.append("AND (c.idAlterno LIKE ? ")
                    .append("OR c.idProducto LIKE ? ")
                    .append("OR p.nombre LIKE ? ")
                    .append("OR pr.Nombre LIKE ? ")
                    .append("OR p.descripcion LIKE ?) ");
        }
        sql.append("ORDER BY c.idAlterno");

        try (Connection conn = new Conexion().conectar();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            if (tieneFiltro) {
                String like = "%" + filtro + "%";
                for (int i = 1; i <= 5; i++) {
                    ps.setString(i, like);
                }
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    resultados.add(new String[]{
                            rs.getString("idAlterno"),
                            rs.getString("idProducto"),
                            rs.getString("producto"),
                            rs.getString("idProveedor"),
                            rs.getString("proveedor"),
                            rs.getString("descripcion")
                    });
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return resultados;
    }
}
