package Compartido.model;

import conexion.Conexion;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

public class modelProductoCbox {

    /**
     * Obtiene todos los productos de la base de datos
     */
    public List<Map<String, String>> obtenerTodosProductos() throws SQLException {
        String sql = """
            SELECT 
                p.id,
                p.nombre,
                p.descripcion,
                p.unidadMedida,
                m.nombre AS marca,
                e.nombre AS etiqueta
            FROM productos p
            LEFT JOIN marcas m ON m.id = p.marca
            LEFT JOIN etiquetas e ON e.id = p.etiqueta
            ORDER BY p.nombre
        """;

        try (Connection conn = new Conexion().conectar();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            return procesarResultSetProductos(rs);
        } catch (SQLException e) {
            System.err.println("Error obteniendo productos: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Procesa el ResultSet de productos
     */
    private List<Map<String, String>> procesarResultSetProductos(ResultSet rs) throws SQLException {
        List<Map<String, String>> productos = new ArrayList<>();

        while (rs.next()) {
            productos.add(crearMapaProducto(rs));
        }

        return productos;
    }

    /**
     * Crea un mapa con los datos del producto
     */
    private Map<String, String> crearMapaProducto(ResultSet rs) throws SQLException {
        Map<String, String> producto = new HashMap<>();

        // Datos básicos
        producto.put("id", rs.getString("id"));
        producto.put("nombre", rs.getString("nombre"));

        // Datos individuales
        String marca = rs.getString("marca");
        String etiqueta = rs.getString("etiqueta");
        String unidadMedida = rs.getString("unidadMedida");
        String descripcionOriginal = rs.getString("descripcion");

        producto.put("marca", marca != null ? marca : "");
        producto.put("etiqueta", etiqueta != null ? etiqueta : "");
        producto.put("unidadMedida", unidadMedida != null ? unidadMedida : "");
        producto.put("descripcion", construirDescripcion(marca, etiqueta, unidadMedida, descripcionOriginal));

        return producto;
    }

    /**
     * Construye la descripción completa
     */
    private String construirDescripcion(String marca, String etiqueta,
                                        String unidadMedida, String descripcionOriginal) {
        List<String> partes = new ArrayList<>();

        if (esValido(marca)) partes.add(marca.trim());
        if (esValido(etiqueta)) partes.add(etiqueta.trim());
        if (esValido(unidadMedida)) partes.add(unidadMedida.trim());
        if (esValido(descripcionOriginal)) partes.add(descripcionOriginal.trim());

        return String.join(", ", partes);
    }

    private boolean esValido(String valor) {
        return valor != null && !valor.trim().isEmpty();
    }

    /**
     * Obtiene las claves alternas para un producto específico
     */
    public List<Map<String, String>> obtenerClavesAlternasPorProducto(String idProducto) throws SQLException {
        String sql = """
            SELECT 
                ca.idAlterno,
                ca.idProducto,
                ca.idProveedor,
                pv.nombre AS nombreProveedor,
                pr.nombre AS nombreProducto
            FROM claves ca
            LEFT JOIN proveedores pv ON pv.id = ca.idProveedor
            LEFT JOIN productos pr ON pr.id = ca.idProducto
            WHERE ca.idProducto = ?
            ORDER BY pv.nombre
        """;

        try (Connection conn = new Conexion().conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, idProducto);

            try (ResultSet rs = ps.executeQuery()) {
                return procesarResultSetClaves(rs);
            }
        }
    }

    /**
     * Busca un producto por su clave alterna
     */
    public Optional<Map<String, String>> buscarPorClaveAlterna(String idAlterno) throws SQLException {
        String sql = """
            SELECT 
                p.id,
                p.nombre,
                p.descripcion,
                p.unidadMedida,
                m.nombre AS marca,
                e.nombre AS etiqueta,
                ca.idAlterno,
                ca.idProveedor,
                pv.nombre AS nombreProveedor
            FROM claves ca
            JOIN productos p ON p.id = ca.idProducto
            LEFT JOIN marcas m ON m.id = p.marca
            LEFT JOIN etiquetas e ON e.id = p.etiqueta
            LEFT JOIN proveedores pv ON pv.id = ca.idProveedor
            WHERE ca.idAlterno = ?
        """;

        try (Connection conn = new Conexion().conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, idAlterno);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(crearMapaProductoConClave(rs));
                }
            }
        }

        return Optional.empty();
    }

    /**
     * Crea mapa de producto con información de clave alterna
     */
    private Map<String, String> crearMapaProductoConClave(ResultSet rs) throws SQLException {
        Map<String, String> producto = crearMapaProducto(rs);

        // Agregar datos específicos de clave alterna
        producto.put("idAlterno", rs.getString("idAlterno"));
        producto.put("idProveedor", rs.getString("idProveedor"));
        producto.put("nombreProveedor", rs.getString("nombreProveedor"));

        return producto;
    }

    /**
     * Obtiene todas las claves alternas disponibles
     */
    public List<Map<String, String>> obtenerTodasClavesAlternas() throws SQLException {
        String sql = """
            SELECT 
                ca.idAlterno,
                ca.idProducto,
                ca.idProveedor,
                pv.nombre AS nombreProveedor,
                pr.nombre AS nombreProducto
            FROM claves ca
            LEFT JOIN proveedores pv ON pv.id = ca.idProveedor
            LEFT JOIN productos pr ON pr.id = ca.idProducto
            ORDER BY ca.idAlterno
        """;

        try (Connection conn = new Conexion().conectar();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            return procesarResultSetClaves(rs);
        }
    }

    /**
     * Procesa ResultSet de claves alternas
     */
    private List<Map<String, String>> procesarResultSetClaves(ResultSet rs) throws SQLException {
        List<Map<String, String>> claves = new ArrayList<>();

        while (rs.next()) {
            Map<String, String> clave = new HashMap<>();
            clave.put("idAlterno", rs.getString("idAlterno"));
            clave.put("idProducto", rs.getString("idProducto"));
            clave.put("idProveedor", rs.getString("idProveedor"));
            clave.put("nombreProveedor", rs.getString("nombreProveedor"));
            clave.put("nombreProducto", rs.getString("nombreProducto"));
            claves.add(clave);
        }

        return claves;
    }

    /**
     * Métodos auxiliares optimizados con Streams
     */
    public Optional<Map<String, String>> buscarPorId(String id, List<Map<String, String>> productos) {
        return productos != null ?
                productos.stream()
                        .filter(p -> id.equals(p.get("id")))
                        .findFirst() :
                Optional.empty();
    }

    public List<Map<String, String>> buscarPorNombre(String nombre, List<Map<String, String>> productos) {
        return productos != null ?
                productos.stream()
                        .filter(p -> nombre.equals(p.get("nombre")))
                        .collect(Collectors.toList()) :
                Collections.emptyList();
    }

    public boolean validarIdNombre(String id, String nombre, List<Map<String, String>> productos) {
        return productos != null &&
                productos.stream()
                        .anyMatch(p -> id.equals(p.get("id")) && nombre.equals(p.get("nombre")));
    }

    public String obtenerDescripcionPorId(String id, List<Map<String, String>> productos) {
        return buscarPorId(id, productos)
                .map(p -> p.get("descripcion"))
                .orElse("");
    }

    public String obtenerUnidadMedidaPorId(String id, List<Map<String, String>> productos) {
        return buscarPorId(id, productos)
                .map(p -> p.get("unidadMedida"))
                .orElse("");
    }
}