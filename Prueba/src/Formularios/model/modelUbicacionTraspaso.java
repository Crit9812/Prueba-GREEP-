package Formularios.model;

import Operaciones.compra.model.UbicacionCompra;
import conexion.Conexion;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

public class modelUbicacionTraspaso {

    public static class ProductoConLote {
        private final String claveProducto;
        private final String nombreProducto;
        private final String lote;
        private final String caducidad;
        private int cantidad;
        private final List<Integer> idsArticulos;

        public ProductoConLote(String claveProducto,
                               String nombreProducto,
                               String lote,
                               String caducidad,
                               int cantidad) {
            this.claveProducto = claveProducto;
            this.nombreProducto = nombreProducto;
            this.lote = lote;
            this.caducidad = caducidad;
            this.cantidad = cantidad;
            this.idsArticulos = new ArrayList<>();
        }

        public void agregarArticulo(int idArticulo) {
            idsArticulos.add(idArticulo);
        }

        public String getClaveProducto() {
            return claveProducto;
        }

        public String getNombreProducto() {
            return nombreProducto;
        }

        public String getLote() {
            return lote != null ? lote : "";
        }

        public String getCaducidad() {
            return caducidad != null ? caducidad : "";
        }

        public int getCantidad() {
            return cantidad;
        }

        public List<Integer> getIdsArticulos() {
            return idsArticulos;
        }

        @Override
        public String toString() {
            return nombreProducto + " (Lote: " + (lote != null ? lote : "Sin lote") + ") - " + cantidad + " unidades";
        }
    }

    public List<ProductoConLote> obtenerProductosPorLote(String claveEntrada) {
        List<ProductoConLote> productos = new ArrayList<>();

        try (Connection conn = new Conexion().conectar()) {
            String sql = "SELECT a.idArticulo, de.claveProducto, p.nombre, a.lote, a.caducidad " +
                    "FROM articulo a " +
                    "JOIN detalle_Entrada de ON a.idDetalleEntrada = de.idDetalleEntrada " +
                    "LEFT JOIN productos p ON de.claveProducto = p.id " +
                    "WHERE de.claveEntrada = ? AND a.Estado = 'pendiente' " +
                    "ORDER BY de.claveProducto, a.lote, a.idArticulo";

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, claveEntrada);

                try (ResultSet rs = ps.executeQuery()) {
                    Map<String, ProductoConLote> mapaProductos = new HashMap<>();

                    while (rs.next()) {
                        String claveProducto = rs.getString("claveProducto");
                        String nombreProducto = rs.getString("nombre") != null
                                ? rs.getString("nombre")
                                : claveProducto;
                        String lote = rs.getString("lote");
                        String caducidad = rs.getString("caducidad") != null
                                ? rs.getDate("caducidad").toString()
                                : "";
                        int idArticulo = rs.getInt("idArticulo");

                        String claveUnica = claveProducto + "_" + (lote != null ? lote : "SIN_LOTE") + "_" + caducidad;

                        ProductoConLote producto = mapaProductos.get(claveUnica);
                        if (producto == null) {
                            producto = new ProductoConLote(claveProducto, nombreProducto, lote, caducidad, 0);
                            mapaProductos.put(claveUnica, producto);
                            productos.add(producto);
                        }

                        producto.agregarArticulo(idArticulo);
                        producto.cantidad++;
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return productos;
    }

    public ObservableList<String> obtenerUbicacionesDisponibles() {
        ObservableList<String> ubicaciones = FXCollections.observableArrayList();

        try (Connection conn = new Conexion().conectar()) {
            String sql = "SELECT nombre FROM ubicaciones ORDER BY nombre";

            try (PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    String nombre = rs.getString("nombre");
                    if (nombre != null && !nombre.trim().isEmpty()) {
                        ubicaciones.add(nombre.trim());
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return ubicaciones;
    }

    public boolean guardarUbicacionesYActualizarEstados(
            String claveEntrada,
            Map<String, List<UbicacionCompra>> ubicacionesPorProductoLote) {
        if (ubicacionesPorProductoLote == null || ubicacionesPorProductoLote.isEmpty()) {
            return false;
        }

        Connection conn = null;
        PreparedStatement psUbicacion = null;
        PreparedStatement psEstadoEntrada = null;
        PreparedStatement psEstadoArticulos = null;

        try {
            conn = new Conexion().conectar();
            conn.setAutoCommit(false);

            List<ProductoConLote> productos = obtenerProductosPorLote(claveEntrada);

            Map<String, Queue<Integer>> colasArticulos = new HashMap<>();
            for (ProductoConLote producto : productos) {
                String claveUnica = producto.getClaveProducto() + "_" + producto.getLote() + "_" + producto.getCaducidad();
                colasArticulos.put(claveUnica, new LinkedList<>(producto.getIdsArticulos()));
            }

            String sqlUbicacion = "UPDATE articulo SET ubicacion = ? WHERE idArticulo = ?";
            psUbicacion = conn.prepareStatement(sqlUbicacion);

            int totalActualizados = 0;
            Map<String, Integer> mapaUbicaciones = obtenerMapaUbicaciones(conn);

            for (Map.Entry<String, List<UbicacionCompra>> entry : ubicacionesPorProductoLote.entrySet()) {
                String claveUnica = entry.getKey();
                List<UbicacionCompra> ubicaciones = entry.getValue();
                Queue<Integer> colaArticulos = colasArticulos.get(claveUnica);

                if (colaArticulos == null || colaArticulos.isEmpty()) {
                    continue;
                }

                for (UbicacionCompra ubicacion : ubicaciones) {
                    String nombreUbicacion = ubicacion.getUbicacion();
                    int idUbicacion = obtenerIdUbicacion(conn, mapaUbicaciones, nombreUbicacion);

                    if (idUbicacion <= 0) {
                        idUbicacion = crearUbicacion(conn, nombreUbicacion);
                        if (idUbicacion > 0) {
                            mapaUbicaciones.put(nombreUbicacion, idUbicacion);
                        } else {
                            continue;
                        }
                    }

                    int cantidad = ubicacion.getCantidad();

                    for (int i = 0; i < cantidad && !colaArticulos.isEmpty(); i++) {
                        int idArticulo = colaArticulos.poll();
                        psUbicacion.setInt(1, idUbicacion);
                        psUbicacion.setInt(2, idArticulo);
                        psUbicacion.addBatch();
                        totalActualizados++;
                    }
                }
            }

            if (totalActualizados > 0) {
                int[] resultadosUbicacion = psUbicacion.executeBatch();
                boolean todosUbicacionesActualizadas = Arrays.stream(resultadosUbicacion).allMatch(r -> r >= 0);

                if (!todosUbicacionesActualizadas) {
                    conn.rollback();
                    return false;
                }
            }

            String sqlEstadoEntrada = "UPDATE entradas SET Estado = 'disponible' WHERE idEntrada = ?";
            psEstadoEntrada = conn.prepareStatement(sqlEstadoEntrada);
            psEstadoEntrada.setString(1, claveEntrada);
            int filasEntrada = psEstadoEntrada.executeUpdate();

            if (filasEntrada <= 0) {
                conn.rollback();
                return false;
            }

            String sqlEstadoArticulos = "UPDATE articulo a " +
                    "JOIN detalle_Entrada de ON a.idDetalleEntrada = de.idDetalleEntrada " +
                    "SET a.Estado = 'disponible' " +
                    "WHERE de.claveEntrada = ?";
            psEstadoArticulos = conn.prepareStatement(sqlEstadoArticulos);
            psEstadoArticulos.setString(1, claveEntrada);
            int filasArticulos = psEstadoArticulos.executeUpdate();

            if (filasArticulos <= 0) {
                conn.rollback();
                return false;
            }

            conn.commit();
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            return false;
        } finally {
            try {
                if (psUbicacion != null) {
                    psUbicacion.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                if (psEstadoEntrada != null) {
                    psEstadoEntrada.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                if (psEstadoArticulos != null) {
                    psEstadoArticulos.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public String obtenerDescripcionProducto(String claveProducto) {
        String descripcion = "";

        try (Connection conn = new Conexion().conectar()) {
            String sql = "SELECT descripcion FROM productos WHERE id = ?";

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, claveProducto);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        descripcion = rs.getString("descripcion");
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return descripcion != null ? descripcion : "";
    }

    public String obtenerMarcaProducto(String claveProducto) {
        String marca = "";

        try (Connection conn = new Conexion().conectar()) {
            String sql = "SELECT m.nombre FROM productos p " +
                    "LEFT JOIN marcas m ON p.marca = m.id " +
                    "WHERE p.id = ?";

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, claveProducto);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        marca = rs.getString("nombre");
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return marca != null ? marca : "";
    }

    private Map<String, Integer> obtenerMapaUbicaciones(Connection conn) throws SQLException {
        Map<String, Integer> mapa = new HashMap<>();
        String sql = "SELECT id, nombre FROM ubicaciones";

        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                mapa.put(rs.getString("nombre").trim(), rs.getInt("id"));
            }
        }
        return mapa;
    }

    private int obtenerIdUbicacion(Connection conn,
                                   Map<String, Integer> mapaUbicaciones,
                                   String nombreUbicacion) {
        if (nombreUbicacion == null || nombreUbicacion.trim().isEmpty()) {
            return 0;
        }

        Integer id = mapaUbicaciones.get(nombreUbicacion.trim());
        return id != null ? id : 0;
    }

    private int crearUbicacion(Connection conn, String nombreUbicacion) throws SQLException {
        if (nombreUbicacion == null || nombreUbicacion.trim().isEmpty()) {
            return 0;
        }

        String sql = "INSERT INTO ubicaciones (nombre) VALUES (?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, nombreUbicacion.trim());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }
}
