package Consultas.proveedores.model;

import Compartido.model.DAO.GenericDAO;
import conexion.Conexion;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;

public class model {

    private final GenericDAO<proveedores> dao = new GenericDAO<>(proveedores.class);

    public ObservableList<proveedores> obtener() {
        ArrayList<proveedores> lista = dao.obtenerTodos();
        return FXCollections.observableArrayList(filtrarActivos(lista));
    }

    public boolean eliminar(int id) {
        proveedores proveedor = obtenerPorId(id);
        if (proveedor == null) {
            return false;
        }
        proveedor.setStatus("desactivado");
        return dao.actualizar(proveedor);
    }

    public proveedores obtenerPorId(int id) {
        return dao.buscarExacto("id", id);
    }

    public ObservableList<proveedores> buscarExacto(String nombre) {
        ArrayList<proveedores> lista = dao.buscarParcial("Nombre", nombre);
        return FXCollections.observableArrayList(filtrarActivos(lista));
    }

    public int contarEntradasPorProveedor(int idProveedor) {
        String sql = """
                SELECT COUNT(*)
                FROM entradas
                WHERE idRemitente = ?
                  AND LOWER(Estado) IN (?, ?, ?)
                """;
        return contarRegistrosConEstados(sql, idProveedor);
    }

    public int contarClavesPorProveedor(int idProveedor) {
        String sql = """
                SELECT COUNT(*)
                FROM claves
                WHERE idProveedor = ?
                  AND LOWER(estado) IN (?, ?, ?)
                """;
        return contarRegistrosConEstados(sql, idProveedor);
    }

    private int contarRegistrosConEstados(String sql, int idProveedor) {
        try (Connection conn = new Conexion().conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idProveedor);
            ps.setString(2, "activo");
            ps.setString(3, "pendiente");
            ps.setString(4, "disponible");
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

    private ArrayList<proveedores> filtrarActivos(ArrayList<proveedores> lista) {
        ArrayList<proveedores> activos = new ArrayList<>();
        for (proveedores p : lista) {
            if (p != null && "activo".equalsIgnoreCase(p.getStatus())) {
                activos.add(p);
            }
        }
        return activos;
    }
}
