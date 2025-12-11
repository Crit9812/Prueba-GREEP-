package Formularios.model;

import conexion.Conexion;

import java.sql.*;
import java.util.*;

/**
 * Model para sincronización de claves.
 * TABLAS:
 *  - proveedores (id INT, Nombre VARCHAR)
 *  - productos   (id VARCHAR(50), nombre, marca, etiqueta, material, unidadMedida, descripcion)
 *  - claves      (idClaveCatalogo VARCHAR(11) PK, claveProveedor INT, claveGreep VARCHAR)
 *
 * Nota: productos.marca and productos.etiqueta se almacenan como identificadores que se intentan relacionar con tablas marcas/etiquetas.
 */
public class modelSincronizacionClaves {

    private static final String TABLA_PROVEEDORES = "proveedores";
    private static final String COL_PROVEEDOR_ID = "id";
    private static final String COL_PROVEEDOR_NOMBRE = "Nombre";

    private static final String TABLA_PRODUCTOS = "productos";
    private static final String COL_PRODUCTO_ID = "id";
    private static final String COL_PRODUCTO_NOMBRE = "nombre";
    private static final String COL_PRODUCTO_MARCA = "marca";      // en productos puede guardarse el id de marca (como texto) o nombre
    private static final String COL_PRODUCTO_ETIQUETA = "etiqueta"; // idem para etiqueta
    private static final String COL_PRODUCTO_MATERIAL = "material";
    private static final String COL_PRODUCTO_UNIDAD = "unidadMedida";
    private static final String COL_PRODUCTO_DESC = "descripcion";

    private static final String TABLA_CLAVES = "claves";
    private static final String COL_CLAVE_CATALOGO = "idClaveCatalogo";
    private static final String COL_CLAVE_PROVEEDOR = "claveProveedor";
    private static final String COL_CLAVE_GREEP = "claveGreep";

    /**
     * Devuelve lista de proveedores: cada Map contiene keys "id" (Integer) y "nombre" (String)
     */
    public List<Map<String, Object>> obtenerProveedores() throws SQLException {
        List<Map<String, Object>> out = new ArrayList<>();
        String sql = "SELECT `" + COL_PROVEEDOR_ID + "`, `" + COL_PROVEEDOR_NOMBRE + "` FROM " + TABLA_PROVEEDORES + " ORDER BY `" + COL_PROVEEDOR_NOMBRE + "`";
        try (Connection conn = new Conexion().conectar();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> m = new HashMap<>();
                m.put("id", rs.getInt(COL_PROVEEDOR_ID));
                m.put("nombre", rs.getString(COL_PROVEEDOR_NOMBRE));
                out.add(m);
            }
        }
        return out;
    }

    /**
     * Devuelve productos con los nombres de marca y etiqueta (cuando existan).
     * Cada Map incluye keys: "id","nombre","marca","etiqueta","material","unidad","descripcion"
     */
    public List<Map<String, Object>> obtenerProductos() throws SQLException {
        List<Map<String, Object>> out = new ArrayList<>();

        // Intentamos unir marcas/etiquetas por id numérico (si p.marca/p.etiqueta contienen el id),
        // y caer en NULL si no hay match. LEFT JOIN's permitirán que devuelva producto aunque no haya marca/etiqueta.
        String sql = "SELECT p.`" + COL_PRODUCTO_ID + "` AS pid, p.`" + COL_PRODUCTO_NOMBRE + "` AS pname, " +
                "m.nombre AS marca_name, e.nombre AS etiqueta_name, p.`" + COL_PRODUCTO_MATERIAL + "` AS material, " +
                "p.`" + COL_PRODUCTO_UNIDAD + "` AS unidad, p.`" + COL_PRODUCTO_DESC + "` AS descripcion, " +
                "p.`" + COL_PRODUCTO_MARCA + "` AS raw_marca, p.`" + COL_PRODUCTO_ETIQUETA + "` AS raw_etiqueta " +
                "FROM " + TABLA_PRODUCTOS + " p " +
                "LEFT JOIN marcas m ON m.id = p." + COL_PRODUCTO_MARCA + " " +
                "LEFT JOIN etiquetas e ON e.id = p." + COL_PRODUCTO_ETIQUETA;

        try (Connection conn = new Conexion().conectar();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> m = new HashMap<>();
                String id = rs.getString("pid");
                String nombre = rs.getString("pname");
                String marcaName = rs.getString("marca_name");
                String etiquetaName = rs.getString("etiqueta_name");
                String material = rs.getString("material");
                String unidad = rs.getString("unidad");
                String descripcion = rs.getString("descripcion");

                // Si marca/etiqueta no se obtuvieron por JOIN y los campos raw contienen nombres, usar raw
                String rawMarca = rs.getString("raw_marca");
                String rawEtiqueta = rs.getString("raw_etiqueta");

                if ((marcaName == null || marcaName.isEmpty()) && rawMarca != null && !rawMarca.isBlank()) {
                    // Si rawMarca no es numérico, tal vez es el nombre guardado directamente
                    marcaName = rawMarca;
                }
                if ((etiquetaName == null || etiquetaName.isEmpty()) && rawEtiqueta != null && !rawEtiqueta.isBlank()) {
                    etiquetaName = rawEtiqueta;
                }

                m.put("id", id);
                m.put("nombre", nombre == null ? "" : nombre);
                m.put("marca", marcaName == null ? "" : marcaName);
                m.put("etiqueta", etiquetaName == null ? "" : etiquetaName);
                m.put("material", material == null ? "" : material);
                m.put("unidad", unidad == null ? "" : unidad);
                m.put("descripcion", descripcion == null ? "" : descripcion);
                out.add(m);
            }
        }
        return out;
    }

    /**
     * Inserta o actualiza un registro en la tabla `claves`.
     * idClaveCatalogo -> String (varchar)
     * claveProveedor -> Integer (puede ser null)
     * claveGreep -> String
     */
    public boolean guardarClave(String idClaveCatalogo, Integer claveProveedor, String claveGreep) throws SQLException {
        try (Connection conn = new Conexion().conectar()) {
            String check = "SELECT COUNT(1) FROM " + TABLA_CLAVES + " WHERE `" + COL_CLAVE_CATALOGO + "` = ?";
            try (PreparedStatement ps = conn.prepareStatement(check)) {
                ps.setString(1, idClaveCatalogo);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        // UPDATE
                        String upd = "UPDATE " + TABLA_CLAVES + " SET `" + COL_CLAVE_PROVEEDOR + "` = ?, `" + COL_CLAVE_GREEP + "` = ? WHERE `" + COL_CLAVE_CATALOGO + "` = ?";
                        try (PreparedStatement ups = conn.prepareStatement(upd)) {
                            if (claveProveedor != null) ups.setObject(1, claveProveedor); else ups.setNull(1, Types.INTEGER);
                            ups.setString(2, claveGreep);
                            ups.setString(3, idClaveCatalogo);
                            return ups.executeUpdate() > 0;
                        }
                    } else {
                        // INSERT
                        String ins = "INSERT INTO " + TABLA_CLAVES + " (`" + COL_CLAVE_CATALOGO + "`, `" + COL_CLAVE_PROVEEDOR + "`, `" + COL_CLAVE_GREEP + "`) VALUES (?, ?, ?)";
                        try (PreparedStatement insP = conn.prepareStatement(ins)) {
                            insP.setString(1, idClaveCatalogo);
                            if (claveProveedor != null) insP.setObject(2, claveProveedor); else insP.setNull(2, Types.INTEGER);
                            insP.setString(3, claveGreep);
                            return insP.executeUpdate() > 0;
                        }
                    }
                }
            }
        }
    }
}
