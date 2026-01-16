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
        return FXCollections.observableArrayList(lista);
    }

    public boolean eliminar(String id) {
        return claveDAO.eliminar(id);
    }

    // --- USANDO DAO ESPECÍFICO ---

    public ObservableList<String[]> obtenerParaTabla() {
        return FXCollections.observableArrayList(obtenerClavesCompletas(null));
    }

    public ObservableList<String[]> buscarEnTabla(String textoBusqueda) {

        if (textoBusqueda == null || textoBusqueda.trim().isEmpty()) {
            return obtenerParaTabla();
        }

        return FXCollections.observableArrayList(obtenerClavesCompletas(textoBusqueda.trim()));
    }

    private ArrayList<String[]> obtenerClavesCompletas(String filtro) {
        ArrayList<String[]> resultados = new ArrayList<>();
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT c.idAlterno, c.idProducto, p.nombre AS producto_nombre, ")
                .append("c.idProveedor, pr.Nombre AS proveedor_nombre, p.descripcion ")
                .append("FROM claves c ")
                .append("LEFT JOIN productos p ON p.id = c.idProducto ")
                .append("LEFT JOIN proveedores pr ON pr.id = c.idProveedor ")
                .append("WHERE LOWER(c.estado) = 'activo' ");

        boolean tieneFiltro = filtro != null && !filtro.isBlank();
        if (tieneFiltro) {
            sql.append("AND (")
                    .append("c.idAlterno LIKE ? OR ")
                    .append("c.idProducto LIKE ? OR ")
                    .append("p.nombre LIKE ? OR ")
                    .append("pr.Nombre LIKE ? OR ")
                    .append("p.descripcion LIKE ?")
                    .append(") ");
        }
        sql.append("ORDER BY c.idAlterno");

        try (Connection conn = new Conexion().conectar();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            if (tieneFiltro) {
                String like = "%" + filtro + "%";
                ps.setString(1, like);
                ps.setString(2, like);
                ps.setString(3, like);
                ps.setString(4, like);
                ps.setString(5, like);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String[] fila = new String[6];
                    fila[0] = rs.getString("idAlterno");
                    fila[1] = rs.getString("idProducto");
                    fila[2] = rs.getString("producto_nombre");
                    fila[3] = rs.getString("idProveedor");
                    fila[4] = rs.getString("proveedor_nombre");
                    fila[5] = rs.getString("descripcion");
                    resultados.add(fila);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return resultados;
    }
}
