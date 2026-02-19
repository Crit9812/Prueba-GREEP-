package Compartido.helper;

import conexion.Conexion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class BusquedaInventarioHelper {

    private static final List<SugerenciaProducto> cacheProductosDisponibles = new ArrayList<>();
    private static volatile String busquedaPendienteInventario;

    private BusquedaInventarioHelper() {
    }

    public static synchronized void recargarProductosDisponibles() {
        cacheProductosDisponibles.clear();

        String sql = """
                SELECT
                    p.id AS id,
                    p.nombre AS nombre,
                    COALESCE(p.descripcion, '') AS descripcion,
                    COUNT(*) AS articulosDisponibles
                FROM productos p
                INNER JOIN (
                    SELECT de.claveProducto AS claveProducto
                    FROM articulo a
                    INNER JOIN detalle_Entrada de ON a.idDetalleEntrada = de.idDetalleEntrada
                    WHERE a.Estado = 'disponible'
                    UNION ALL
                    SELECT de.claveProducto AS claveProducto
                    FROM detalleArticulo da
                    INNER JOIN articulo a ON da.idArticulo = a.idArticulo
                    INNER JOIN detalle_Entrada de ON a.idDetalleEntrada = de.idDetalleEntrada
                    WHERE da.estado = 'activo'
                      AND a.Estado = 'segmentado'
                ) inventario ON inventario.claveProducto = p.id
                GROUP BY p.id, p.nombre, p.descripcion
                ORDER BY p.nombre ASC
                """;

        try (Connection conn = new Conexion().conectar();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                cacheProductosDisponibles.add(new SugerenciaProducto(
                        rs.getString("id"),
                        rs.getString("nombre"),
                        rs.getString("descripcion"),
                        rs.getInt("articulosDisponibles")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static synchronized List<SugerenciaProducto> obtenerProductosDisponibles() {
        return Collections.unmodifiableList(new ArrayList<>(cacheProductosDisponibles));
    }

    public static synchronized List<SugerenciaProducto> filtrarSugerencias(String textoBusqueda, int limite) {
        String criterio = textoBusqueda == null ? "" : textoBusqueda.trim().toLowerCase(Locale.ROOT);
        if (criterio.isEmpty()) {
            return obtenerPrimerasSugerencias(limite);
        }

        List<SugerenciaProducto> sugerencias = new ArrayList<>();
        for (SugerenciaProducto producto : cacheProductosDisponibles) {
            String id = producto.id() == null ? "" : producto.id().toLowerCase(Locale.ROOT);
            String nombre = producto.nombre() == null ? "" : producto.nombre().toLowerCase(Locale.ROOT);
            String descripcion = producto.descripcion() == null ? "" : producto.descripcion().toLowerCase(Locale.ROOT);

            if (id.contains(criterio) || nombre.contains(criterio) || descripcion.contains(criterio)) {
                sugerencias.add(producto);
            }

            if (sugerencias.size() >= limite) {
                break;
            }
        }
        return sugerencias;
    }

    private static List<SugerenciaProducto> obtenerPrimerasSugerencias(int limite) {
        List<SugerenciaProducto> sugerencias = new ArrayList<>();
        for (SugerenciaProducto producto : cacheProductosDisponibles) {
            sugerencias.add(producto);
            if (sugerencias.size() >= limite) {
                break;
            }
        }
        return sugerencias;
    }

    public static void setBusquedaPendienteInventario(String textoBusqueda) {
        busquedaPendienteInventario = textoBusqueda;
    }

    public static String consumirBusquedaPendienteInventario() {
        String texto = busquedaPendienteInventario;
        busquedaPendienteInventario = null;
        return texto;
    }

    public record SugerenciaProducto(String id, String nombre, String descripcion, int articulosDisponibles) {
        public String descripcionCorta() {
            String texto = descripcion == null ? "" : descripcion.trim();
            if (texto.length() <= 70) {
                return texto;
            }
            return texto.substring(0, 67) + "...";
        }
    }
}
