package Formularios.model;

import conexion.Conexion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class modelCompra {

    public static class ClaveDTO {
        public String idAlterno;
        public Integer idProveedor;
        public String idProducto;
        public String producto;
        public String descripcion;
    }

    public List<ClaveDTO> obtenerClavesPorProveedor(int idProveedor) {
        List<ClaveDTO> lista = new ArrayList<>();

        String sql =
                "SELECT c.idAlterno AS idAlterno, c.idProveedor AS idProveedor, c.idProducto AS idProducto, " +
                        "       p.nombre AS producto, " +
                        "       TRIM(CONCAT_WS(' ', m.nombre, e.nombre, p.unidadMedida, p.descripcion)) AS descripcion " +
                        "FROM claves c " +
                        "LEFT JOIN productos p ON c.idProducto = p.id " +
                        "LEFT JOIN marcas m ON p.marca = m.id " +
                        "LEFT JOIN etiquetas e ON p.etiqueta = e.id " +
                        "WHERE c.idProveedor = ? " +
                        "ORDER BY c.idAlterno";

        try (Connection conn = new Conexion().conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, idProveedor);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ClaveDTO d = new ClaveDTO();
                    d.idAlterno = rs.getString("idAlterno");
                    d.idProveedor = rs.getObject("idProveedor") == null ? null : rs.getInt("idProveedor");
                    d.idProducto = rs.getString("idProducto");
                    d.producto = rs.getString("producto");
                    d.descripcion = rs.getString("descripcion");
                    lista.add(d);
                }
            }
        } catch (Exception e) {
            System.out.println("Error obtenerClavesPorProveedor: " + e.getMessage());
            e.printStackTrace();
        }

        return lista;
    }

    public List<String[]> obtenerUbicaciones() {
        List<String[]> lista = new ArrayList<>();
        String sql = "SELECT id, nombre FROM ubicaciones ORDER BY id";

        try (Connection conn = new Conexion().conectar();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                lista.add(new String[]{ rs.getString("id"), rs.getString("nombre") });
            }
        } catch (Exception e) {
            System.out.println("Error obtenerUbicaciones: " + e.getMessage());
            e.printStackTrace();
        }
        return lista;
    }
}
