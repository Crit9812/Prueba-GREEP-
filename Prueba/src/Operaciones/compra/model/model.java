package Operaciones.compra.model;

import Compartido.sesion.SesionUsuario;
import conexion.Conexion;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class model {

    public List<String> obtenerNombresProveedores() {
        List<String> lista = new ArrayList<>();
        String sql = "SELECT nombre FROM proveedores ORDER BY nombre";

        try (Connection conn = new Conexion().conectar();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                lista.add(rs.getString("nombre"));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return lista;
    }

    public List<String> obtenerNombresUbicaciones() {
        List<String> lista = new ArrayList<>();
        String sql = "SELECT nombre FROM ubicaciones ORDER BY nombre";

        try (Connection conn = new Conexion().conectar();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                lista.add(rs.getString("nombre"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return lista;
    }

    public String obtenerIdProveedorPorNombre(String nombreProveedor) {
        String sql = "SELECT id FROM proveedores WHERE nombre = ? LIMIT 1";

        try (Connection conn = new Conexion().conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, nombreProveedor);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("id");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean registrarCompra(String idProveedor, String factura, String comentario, List<compra> items) {
        if (items == null || items.isEmpty()) {
            return false;
        }

        try (Connection conn = new Conexion().conectar()) {
            conn.setAutoCommit(false);

            Map<String, String> columnasEntradas = obtenerColumnas(conn, "entradas");
            Map<String, Object> valoresEntrada = new LinkedHashMap<>();

            String colProveedor = resolverColumna(columnasEntradas, "idRemitente", "idProveedor", "id_proveedor", "proveedor", "proveedor_id");
            String colFactura = resolverColumna(columnasEntradas, "noFactura", "factura", "numeroFactura", "numero_factura");
            String colComentario = resolverColumna(columnasEntradas, "nota", "comentario", "observaciones");
            String colFecha = resolverColumna(columnasEntradas, "fechaEntrada", "fecha", "fecha_entrada", "created_at");
            String colHora = resolverColumna(columnasEntradas, "horaEntrada", "hora", "hora_entrada");
            String colTipo = resolverColumna(columnasEntradas, "tipoEntrada", "tipo", "tipo_entrada");
            String colPrecioNeto = resolverColumna(columnasEntradas, "precioNetoEntrada", "precioNeto", "precio_neto");
            String colPrecioTotal = resolverColumna(columnasEntradas, "precioTotalEntrada", "precioTotal", "precio_total");
            String colUsuarioEntrada = resolverColumna(columnasEntradas, "claveUsuarioEntrada", "idUsuarioEntrada", "id_usuario_entrada", "usuarioEntrada", "usuario_entrada");
            String colEstado = resolverColumna(columnasEntradas, "Estado", "estado");

            if (colProveedor != null) valoresEntrada.put(colProveedor, idProveedor);
            if (colFactura != null) valoresEntrada.put(colFactura, factura);
            if (colComentario != null) valoresEntrada.put(colComentario, comentario);
            if (colFecha != null) valoresEntrada.put(colFecha, Date.valueOf(LocalDate.now()));
            if (colHora != null) valoresEntrada.put(colHora, Time.valueOf(LocalTime.now()));
            if (colTipo != null) valoresEntrada.put(colTipo, "Traspaso");
            if (colEstado != null) valoresEntrada.put(colEstado, "Pendiente");
            Integer idUsuarioEntrada = SesionUsuario.getIdUsuario();
            if (colUsuarioEntrada != null && idUsuarioEntrada != null) {
                valoresEntrada.put(colUsuarioEntrada, idUsuarioEntrada);
            }

            BigDecimal totalNeto = BigDecimal.ZERO;
            BigDecimal totalGeneral = BigDecimal.ZERO;
            for (compra item : items) {
                BigDecimal cantidad = BigDecimal.valueOf(item.getCantidad());
                BigDecimal precioUnitario = parseDecimal(item.getPrecioEntrada());
                BigDecimal precioTotal = parseDecimal(item.getPrecioTotal());
                BigDecimal precioBruto = parseDecimal(item.getPrecioBruto());

                totalNeto = totalNeto.add(precioUnitario.multiply(cantidad));
                if (precioTotal.compareTo(BigDecimal.ZERO) > 0) {
                    totalGeneral = totalGeneral.add(precioTotal);
                } else {
                    totalGeneral = totalGeneral.add(precioBruto.multiply(cantidad));
                }
            }

            if (colPrecioNeto != null) valoresEntrada.put(colPrecioNeto, totalNeto.setScale(2, RoundingMode.HALF_UP));
            if (colPrecioTotal != null) valoresEntrada.put(colPrecioTotal, totalGeneral.setScale(2, RoundingMode.HALF_UP));

            long idEntrada = insertarRegistro(conn, "entradas", columnasEntradas, valoresEntrada);

            Map<String, String> columnasDetalle = obtenerColumnas(conn, "detalle_Entrada");
            Map<String, String> columnasArticulo = obtenerColumnas(conn, "articulo");

            for (compra item : items) {
                Map<String, Object> valoresDetalle = new LinkedHashMap<>();

                String colEntrada = resolverColumna(columnasDetalle, "claveEntrada", "idEntrada", "id_entrada", "entrada_id");
                String colProducto = resolverColumna(columnasDetalle, "claveProducto", "idProducto", "id_producto", "producto_id");
                String colCantidad = resolverColumna(columnasDetalle, "cantidad", "cantidadEntrada");
                String colPrecioEntrada = resolverColumna(columnasDetalle, "precioUnitario", "precioEntrada", "precio_entrada", "costoEntrada");
                String colPrecioIva = resolverColumna(columnasDetalle, "precioIVA", "precioIva", "precio_iva");
                String colPrecioBruto = resolverColumna(columnasDetalle, "precioBrutoTotal", "precioBruto", "precio_bruto");
                String colPrecioTotalDetalle = resolverColumna(columnasDetalle, "precioTotal", "precio_total");

                if (colEntrada != null) valoresDetalle.put(colEntrada, idEntrada);
                if (colProducto != null) valoresDetalle.put(colProducto, item.getClaveProducto());
                if (colCantidad != null) valoresDetalle.put(colCantidad, item.getCantidad());
                if (colPrecioEntrada != null) valoresDetalle.put(colPrecioEntrada, parseDecimal(item.getPrecioEntrada()));
                if (colPrecioIva != null) valoresDetalle.put(colPrecioIva, parseDecimal(item.getPrecioIva()));
                if (colPrecioBruto != null) valoresDetalle.put(colPrecioBruto, parseDecimal(item.getPrecioBruto()));
                if (colPrecioTotalDetalle != null) valoresDetalle.put(colPrecioTotalDetalle, parseDecimal(item.getPrecioTotal()));

                long idDetalleEntrada = insertarRegistro(conn, "detalle_Entrada", columnasDetalle, valoresDetalle);

                Map<String, Integer> cantidadesPorUbicacion = new LinkedHashMap<>();
                for (UbicacionCompra ubicacion : item.getUbicaciones()) {
                    if (ubicacion == null || ubicacion.getUbicacion() == null) {
                        continue;
                    }
                    int cantidadUbicacion = Math.max(0, ubicacion.getCantidad());
                    if (cantidadUbicacion == 0) {
                        continue;
                    }
                    cantidadesPorUbicacion.merge(ubicacion.getUbicacion().trim(), cantidadUbicacion, Integer::sum);
                }

                for (Map.Entry<String, Integer> entry : cantidadesPorUbicacion.entrySet()) {
                    String colArticuloProducto = resolverColumna(columnasArticulo, "idProducto", "id_producto", "producto_id");
                    String colArticuloDetalleEntrada = resolverColumna(
                            columnasArticulo,
                            "idDetalleEntrada",
                            "id_detalle_entrada",
                            "detalleEntrada",
                            "detalle_entrada",
                            "detalle_entrada_id"
                    );
                    String colArticuloLote = resolverColumna(columnasArticulo, "lote");
                    String colArticuloCaducidad = resolverColumna(columnasArticulo, "caducidad");
                    String colArticuloUbicacion = resolverColumna(columnasArticulo, "ubicacion", "idUbicacion", "id_ubicacion");
                    String colArticuloPresentacion = resolverColumna(columnasArticulo, "presentacion");
                    String colArticuloFactor = resolverColumna(columnasArticulo, "factor");
                    String colArticuloSegmentado = resolverColumna(columnasArticulo, "segmentado");

                    Integer ubicacionId = resolverUbicacionId(conn, entry.getKey());
                    int cantidadUbicacion = entry.getValue();

                    for (int i = 0; i < cantidadUbicacion; i++) {
                        Map<String, Object> valoresArticulo = new LinkedHashMap<>();

                        if (colArticuloProducto != null) valoresArticulo.put(colArticuloProducto, item.getClaveProducto());
                        if (colArticuloDetalleEntrada != null) valoresArticulo.put(colArticuloDetalleEntrada, idDetalleEntrada);
                        if (colArticuloLote != null) valoresArticulo.put(colArticuloLote, item.getLote());
                        if (colArticuloCaducidad != null) valoresArticulo.put(colArticuloCaducidad, parseDate(item.getCaducidad()));
                        if (colArticuloUbicacion != null) valoresArticulo.put(colArticuloUbicacion, ubicacionId);
                        if (colArticuloPresentacion != null) valoresArticulo.put(colArticuloPresentacion, item.getPresentacion());
                        if (colArticuloFactor != null) valoresArticulo.put(colArticuloFactor, parseInteger(item.getFactor()));
                        if (colArticuloSegmentado != null) valoresArticulo.put(colArticuloSegmentado, esSegmentado(item.getPresentacion()));

                        insertarRegistro(conn, "articulo", columnasArticulo, valoresArticulo);
                    }
                }
            }

            conn.commit();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private Map<String, String> obtenerColumnas(Connection conn, String tabla) throws SQLException {
        Map<String, String> columnas = new java.util.HashMap<>();
        DatabaseMetaData meta = conn.getMetaData();

        try (ResultSet rs = meta.getColumns(conn.getCatalog(), null, tabla, null)) {
            while (rs.next()) {
                String nombre = rs.getString("COLUMN_NAME");
                if (nombre == null) {
                    continue;
                }
                String nombreLimpio = nombre.trim();
                columnas.put(nombreLimpio.toLowerCase(), nombreLimpio);
            }
        }

        if (columnas.isEmpty()) {
            try (ResultSet rs = meta.getColumns(conn.getCatalog(), null, tabla.toLowerCase(), null)) {
                while (rs.next()) {
                    String nombre = rs.getString("COLUMN_NAME");
                    if (nombre == null) {
                        continue;
                    }
                    String nombreLimpio = nombre.trim();
                    columnas.put(nombreLimpio.toLowerCase(), nombreLimpio);
                }
            }
        }

        return columnas;
    }

    private String resolverColumna(Map<String, String> columnas, String... candidatos) {
        for (String candidato : candidatos) {
            if (candidato == null) continue;
            String match = columnas.get(candidato.toLowerCase());
            if (match != null) {
                return match;
            }
        }
        return null;
    }

    private long insertarRegistro(Connection conn, String tabla, Map<String, String> columnas, Map<String, Object> valores)
            throws SQLException {
        if (valores.isEmpty()) {
            throw new SQLException("No hay valores para insertar en " + tabla);
        }

        String columnasSql = String.join(", ", valores.keySet());
        String placeholders = String.join(", ", java.util.Collections.nCopies(valores.size(), "?"));

        String sql = "INSERT INTO " + tabla + " (" + columnasSql + ") VALUES (" + placeholders + ")";

        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            int index = 1;
            for (Object value : valores.values()) {
                ps.setObject(index++, value);
            }

            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
        }

        try (PreparedStatement ps = conn.prepareStatement("SELECT LAST_INSERT_ID()")) {
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }

        return 0;
    }

    private BigDecimal parseDecimal(String valor) {
        if (valor == null || valor.isBlank()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(valor);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private Integer parseInteger(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(valor);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Date parseDate(String fecha) {
        if (fecha == null || fecha.isBlank()) {
            return null;
        }
        try {
            return Date.valueOf(fecha);
        } catch (Exception e) {
            return null;
        }
    }

    private Integer resolverUbicacionId(Connection conn, String ubicacion) throws SQLException {
        if (ubicacion == null || ubicacion.isBlank()) {
            return null;
        }
        String texto = ubicacion.trim();
        try {
            return Integer.valueOf(texto);
        } catch (NumberFormatException ignored) {
        }

        String sql = "SELECT id FROM ubicaciones WHERE nombre = ? LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, texto);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        }
        String insertar = "INSERT INTO ubicaciones (nombre) VALUES (?)";
        try (PreparedStatement ps = conn.prepareStatement(insertar, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, texto);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }

        return null;
    }

    private int esSegmentado(String presentacion) {
        return 0;
    }
}
