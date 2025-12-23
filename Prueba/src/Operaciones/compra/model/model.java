package Operaciones.compra.model;

import Compartido.model.ProductoComboItem;
import conexion.Conexion;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class model {

    public List<Map<String, Object>> obtenerProveedores() {
        List<Map<String, Object>> lista = new ArrayList<>();
        String sql = "SELECT id, Nombre AS nombre FROM proveedores ORDER BY Nombre";

        try (Connection conn = new Conexion().conectar();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Map<String, Object> proveedor = new HashMap<>();
                proveedor.put("id", rs.getInt("id"));
                proveedor.put("nombre", rs.getString("nombre"));
                lista.add(proveedor);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return lista;
    }

    public List<ProductoComboItem> obtenerProductosPorProveedor(int idProveedor) {
        List<ProductoComboItem> productos = new ArrayList<>();
        String sql = "SELECT p.id AS producto_id, p.nombre AS producto_nombre, " +
                "m.nombre AS marca_nombre, e.nombre AS etiqueta_nombre, " +
                "p.material AS material, p.unidadMedida AS unidad, p.descripcion AS descripcion, " +
                "p.marca AS raw_marca, p.etiqueta AS raw_etiqueta, " +
                "c.idAlterno AS clave_alterna " +
                "FROM claves c " +
                "JOIN productos p ON p.id = c.idProducto " +
                "LEFT JOIN marcas m ON m.id = p.marca " +
                "LEFT JOIN etiquetas e ON e.id = p.etiqueta " +
                "WHERE c.idProveedor = ? " +
                "ORDER BY p.nombre";

        try (Connection conn = new Conexion().conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, idProveedor);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String id = rs.getString("producto_id");
                    String nombre = rs.getString("producto_nombre");
                    String marca = rs.getString("marca_nombre");
                    String etiqueta = rs.getString("etiqueta_nombre");
                    String material = rs.getString("material");
                    String unidad = rs.getString("unidad");
                    String descripcion = rs.getString("descripcion");
                    String rawMarca = rs.getString("raw_marca");
                    String rawEtiqueta = rs.getString("raw_etiqueta");
                    String claveAlterna = rs.getString("clave_alterna");

                    if ((marca == null || marca.isBlank()) && rawMarca != null && !rawMarca.isBlank()) {
                        marca = rawMarca;
                    }
                    if ((etiqueta == null || etiqueta.isBlank()) && rawEtiqueta != null && !rawEtiqueta.isBlank()) {
                        etiqueta = rawEtiqueta;
                    }

                    String descripcionFinal = construirDescripcion(marca, etiqueta, material, unidad, descripcion);
                    productos.add(new ProductoComboItem(id, nombre, descripcionFinal, claveAlterna));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return productos;
    }

    public boolean guardarCompras(int idProveedor, String factura, String comentario, List<itemCompra> items) {
        if (items == null || items.isEmpty()) return false;

        String sqlEntrada = "INSERT INTO entradas (idProveedor, factura, comentario) VALUES (?, ?, ?)";
        String sqlDetalle = "INSERT INTO detalle_Entrada " +
                "(idEntrada, idProducto, cantidad, precioEntrada, precioIva, precioBruto, precioTotal, presentacion, factor, claveAlterna, descripcion, lote, caducidad, ubicacion) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        String sqlArticulo = "INSERT INTO articulo " +
                "(idProducto, claveAlterna, descripcion, lote, caducidad, ubicacion, cantidad, precioEntrada, precioIva, precioBruto, precioTotal, presentacion, factor) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = new Conexion().conectar()) {
            conn.setAutoCommit(false);

            int idEntrada;
            try (PreparedStatement psEntrada = conn.prepareStatement(sqlEntrada, Statement.RETURN_GENERATED_KEYS)) {
                psEntrada.setInt(1, idProveedor);
                psEntrada.setString(2, factura);
                psEntrada.setString(3, comentario);
                psEntrada.executeUpdate();

                try (ResultSet rs = psEntrada.getGeneratedKeys()) {
                    if (rs.next()) {
                        idEntrada = rs.getInt(1);
                    } else {
                        conn.rollback();
                        return false;
                    }
                }
            }

            try (PreparedStatement psDetalle = conn.prepareStatement(sqlDetalle);
                 PreparedStatement psArticulo = conn.prepareStatement(sqlArticulo)) {

                for (itemCompra item : items) {
                    psDetalle.setInt(1, idEntrada);
                    psDetalle.setString(2, item.getClaveProducto());
                    psDetalle.setInt(3, item.getCantidad());
                    psDetalle.setDouble(4, item.getPrecioEntrada());
                    psDetalle.setDouble(5, item.getPrecioIva());
                    psDetalle.setDouble(6, item.getPrecioBruto());
                    psDetalle.setDouble(7, item.getPrecioTotal());
                    psDetalle.setString(8, item.getPresentacion());
                    psDetalle.setString(9, item.getFactor());
                    psDetalle.setString(10, item.getClaveAlterna());
                    psDetalle.setString(11, item.getDescripcion());
                    psDetalle.setString(12, item.getLote());
                    psDetalle.setString(13, item.getCaducidad());
                    psDetalle.setString(14, item.getUbicacion());
                    psDetalle.addBatch();

                    psArticulo.setString(1, item.getClaveProducto());
                    psArticulo.setString(2, item.getClaveAlterna());
                    psArticulo.setString(3, item.getDescripcion());
                    psArticulo.setString(4, item.getLote());
                    psArticulo.setString(5, item.getCaducidad());
                    psArticulo.setString(6, item.getUbicacion());
                    psArticulo.setInt(7, item.getCantidad());
                    psArticulo.setDouble(8, item.getPrecioEntrada());
                    psArticulo.setDouble(9, item.getPrecioIva());
                    psArticulo.setDouble(10, item.getPrecioBruto());
                    psArticulo.setDouble(11, item.getPrecioTotal());
                    psArticulo.setString(12, item.getPresentacion());
                    psArticulo.setString(13, item.getFactor());
                    psArticulo.addBatch();
                }

                psDetalle.executeBatch();
                psArticulo.executeBatch();
            }

            conn.commit();
            return true;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private String construirDescripcion(String marca, String etiqueta, String material, String unidad, String descripcion) {
        StringBuilder sb = new StringBuilder();
        if (marca != null && !marca.isBlank()) sb.append(marca.trim());
        if (etiqueta != null && !etiqueta.isBlank()) {
            if (sb.length() > 0) sb.append(" | ");
            sb.append(etiqueta.trim());
        }
        if (material != null && !material.isBlank()) {
            if (sb.length() > 0) sb.append(" | ");
            sb.append(material.trim());
        }
        if (unidad != null && !unidad.isBlank()) {
            if (sb.length() > 0) sb.append(" | ");
            sb.append(unidad.trim());
        }
        if (descripcion != null && !descripcion.isBlank()) {
            if (sb.length() > 0) sb.append(" | ");
            sb.append(descripcion.trim());
        }
        return sb.toString();
    }
}
