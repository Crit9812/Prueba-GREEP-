package Reportes.historial.controller;

import Reportes.historial.model.HistorialFactura;
import conexion.Conexion;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DetalleFacturaController {

    @FXML private Label lblTitulo;
    @FXML private VBox contenedorDetalles;
    @FXML private CheckBox chkDetallado;
    @FXML private Button btnCerrar;
    @FXML private ScrollPane scrollPane;

    private HistorialFactura historial;
    private Stage stage;

    @FXML
    public void initialize() {
        if (chkDetallado != null) {
            chkDetallado.selectedProperty().addListener((obs, oldVal, newVal) -> cargarDetalles());
        }
        actualizarTitulo();
    }

    public void setHistorial(HistorialFactura historial) {
        this.historial = historial;
        actualizarTitulo();
        cargarDetalles();
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    @FXML
    private void cerrarVentana() {
        if (stage != null) {
            stage.close();
            return;
        }
        if (btnCerrar != null && btnCerrar.getScene() != null) {
            btnCerrar.getScene().getWindow().hide();
        }
    }

    private void actualizarTitulo() {
        if (lblTitulo == null) {
            return;
        }
        if (historial == null) {
            lblTitulo.setText("Detalles");
            return;
        }
        lblTitulo.setText("Detalles - " + historial.getMovimiento());
    }

    private void cargarDetalles() {
        if (historial == null) {
            mostrarSinDetalles();
            return;
        }
        boolean detallado = chkDetallado != null && chkDetallado.isSelected();

        Task<List<DetalleLinea>> task = new Task<>() {
            @Override
            protected List<DetalleLinea> call() {
                return obtenerDetalles(detallado);
            }
        };

        task.setOnSucceeded(event -> renderizarDetalles(task.getValue(), detallado));
        task.setOnFailed(event -> mostrarSinDetalles());

        Thread hilo = new Thread(task);
        hilo.setDaemon(true);
        hilo.start();
    }

    private List<DetalleLinea> obtenerDetalles(boolean detallado) {
        String movimiento = historial.getMovimiento();
        String claveMovimiento = historial.getClaveMovimiento();
        if (movimiento == null || claveMovimiento == null || claveMovimiento.isBlank()) {
            return List.of();
        }

        try (Connection conn = new Conexion().conectar()) {
            if (conn == null) {
                return List.of();
            }

            switch (movimiento.toLowerCase()) {
                case "entrada":
                    return obtenerDetallesEntrada(conn, claveMovimiento, detallado, "Entrada");
                case "salida":
                    return obtenerDetallesSalida(conn, claveMovimiento, detallado, "Salida");
                case "ajuste":
                    List<DetalleLinea> resultado = new ArrayList<>();
                    resultado.addAll(obtenerDetallesEntrada(conn, claveMovimiento, detallado, "Entrada"));
                    resultado.addAll(obtenerDetallesSalida(conn, claveMovimiento, detallado, "Salida"));
                    return resultado;
                default:
                    return List.of();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return List.of();
        }
    }

    private List<DetalleLinea> obtenerDetallesEntrada(Connection conn, String claveMovimiento,
                                                     boolean detallado, String tipo) throws SQLException {
        Map<String, String> columnasDetalle = obtenerColumnas(conn, "detalle_Entrada");
        Map<String, String> columnasProducto = obtenerColumnas(conn, "productos");

        String colDetalleId = resolverColumna(columnasDetalle, "idDetalleEntrada", "id", "id_detalle_entrada");
        String colClaveEntrada = resolverColumna(columnasDetalle, "claveEntrada", "idEntrada", "id_entrada", "entrada_id");
        String colProducto = resolverColumna(columnasDetalle, "claveProducto", "idProducto", "id_producto", "producto_id");
        String colCantidad = resolverColumna(columnasDetalle, "cantidad", "cantidadEntrada");
        String colPrecioUnitario = resolverColumna(columnasDetalle, "precioUnitario", "precioEntrada", "precio_entrada",
                "costoEntrada");
        String colPrecioTotal = resolverColumna(columnasDetalle, "precioTotal", "precio_total");
        String colNota = resolverColumna(columnasDetalle, "Nota", "nota", "comentario", "observaciones");

        String colProductoId = resolverColumna(columnasProducto, "id", "idProducto", "claveProducto");
        String colProductoNombre = resolverColumna(columnasProducto, "nombre", "Nombre", "producto");

        if (colDetalleId == null || colClaveEntrada == null || colProducto == null) {
            return List.of();
        }

        String joinProducto = (colProductoId != null && colProductoNombre != null)
                ? "LEFT JOIN productos p ON d.`" + colProducto + "` = p.`" + colProductoId + "`"
                : "";
        String productoExpr = (colProductoId != null && colProductoNombre != null)
                ? "p.`" + colProductoNombre + "`"
                : "d.`" + colProducto + "`";

        String sql = String.format("""
                SELECT d.`%s` AS idDetalle,
                       d.`%s` AS claveProducto,
                       %s AS cantidad,
                       %s AS precioUnitario,
                       %s AS precioTotal,
                       %s AS nota,
                       %s AS producto
                FROM detalle_Entrada d
                %s
                WHERE d.`%s` = ?
                ORDER BY producto, idDetalle
                """,
                colDetalleId,
                colProducto,
                columnaOrNull(colCantidad),
                columnaOrNull(colPrecioUnitario),
                columnaOrNull(colPrecioTotal),
                columnaOrNull(colNota),
                productoExpr,
                joinProducto,
                colClaveEntrada
        );

        List<DetalleLinea> lineas = new ArrayList<>();
        Map<Integer, DetalleLinea> lineasPorId = new HashMap<>();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, claveMovimiento);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int idDetalle = rs.getInt("idDetalle");
                    DetalleLinea linea = new DetalleLinea(
                            tipo,
                            valorTexto(rs.getObject("producto")),
                            valorTexto(rs.getObject("claveProducto")),
                            valorTexto(rs.getObject("cantidad")),
                            valorTexto(rs.getObject("precioUnitario")),
                            valorTexto(rs.getObject("precioTotal")),
                            valorTexto(rs.getObject("nota"))
                    );
                    lineas.add(linea);
                    lineasPorId.put(idDetalle, linea);
                }
            }
        }

        if (detallado && !lineasPorId.isEmpty()) {
            cargarArticulosEntrada(conn, claveMovimiento, lineasPorId);
        }

        return lineas;
    }

    private List<DetalleLinea> obtenerDetallesSalida(Connection conn, String claveMovimiento,
                                                     boolean detallado, String tipo) throws SQLException {
        Map<String, String> columnasDetalle = obtenerColumnas(conn, "detalle_Salida");
        Map<String, String> columnasProducto = obtenerColumnas(conn, "productos");

        String colDetalleId = resolverColumna(columnasDetalle, "idDetalleSalida", "id", "id_detalle_salida");
        String colClaveSalida = resolverColumna(columnasDetalle, "claveSalida", "idSalida", "id_salida", "salida_id");
        String colProducto = resolverColumna(columnasDetalle, "claveProductoSalida", "claveProducto",
                "idProducto", "id_producto", "producto_id");
        String colCantidad = resolverColumna(columnasDetalle, "cantidad", "cantidadSalida", "cantidad_salida");
        String colPrecioUnitario = resolverColumna(columnasDetalle, "precioUnitarioSalida", "precioUnitario",
                "precioSalida", "precio_salida", "precioSalidaUnitario");
        String colPrecioTotal = resolverColumna(columnasDetalle, "precioTotalSalida", "precioTotal", "precio_total");
        String colNota = resolverColumna(columnasDetalle, "Nota", "nota", "comentario", "observaciones");

        String colProductoId = resolverColumna(columnasProducto, "id", "idProducto", "claveProducto");
        String colProductoNombre = resolverColumna(columnasProducto, "nombre", "Nombre", "producto");

        if (colDetalleId == null || colClaveSalida == null || colProducto == null) {
            return List.of();
        }

        String joinProducto = (colProductoId != null && colProductoNombre != null)
                ? "LEFT JOIN productos p ON d.`" + colProducto + "` = p.`" + colProductoId + "`"
                : "";
        String productoExpr = (colProductoId != null && colProductoNombre != null)
                ? "p.`" + colProductoNombre + "`"
                : "d.`" + colProducto + "`";

        String sql = String.format("""
                SELECT d.`%s` AS idDetalle,
                       d.`%s` AS claveProducto,
                       %s AS cantidad,
                       %s AS precioUnitario,
                       %s AS precioTotal,
                       %s AS nota,
                       %s AS producto
                FROM detalle_Salida d
                %s
                WHERE d.`%s` = ?
                ORDER BY producto, idDetalle
                """,
                colDetalleId,
                colProducto,
                columnaOrNull(colCantidad),
                columnaOrNull(colPrecioUnitario),
                columnaOrNull(colPrecioTotal),
                columnaOrNull(colNota),
                productoExpr,
                joinProducto,
                colClaveSalida
        );

        List<DetalleLinea> lineas = new ArrayList<>();
        Map<Integer, DetalleLinea> lineasPorId = new HashMap<>();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, claveMovimiento);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int idDetalle = rs.getInt("idDetalle");
                    DetalleLinea linea = new DetalleLinea(
                            tipo,
                            valorTexto(rs.getObject("producto")),
                            valorTexto(rs.getObject("claveProducto")),
                            valorTexto(rs.getObject("cantidad")),
                            valorTexto(rs.getObject("precioUnitario")),
                            valorTexto(rs.getObject("precioTotal")),
                            valorTexto(rs.getObject("nota"))
                    );
                    lineas.add(linea);
                    lineasPorId.put(idDetalle, linea);
                }
            }
        }

        if (detallado && !lineasPorId.isEmpty()) {
            cargarArticulosSalida(conn, claveMovimiento, lineasPorId);
        }

        return lineas;
    }

    private void cargarArticulosEntrada(Connection conn, String claveMovimiento,
                                        Map<Integer, DetalleLinea> lineasPorId) throws SQLException {
        Map<String, String> columnasDetalle = obtenerColumnas(conn, "detalle_Entrada");
        Map<String, String> columnasArticulo = obtenerColumnas(conn, "articulo");
        Map<String, String> columnasUbicacion = obtenerColumnas(conn, "ubicaciones");
        Map<String, String> columnasProducto = obtenerColumnas(conn, "productos");

        String colDetalleId = resolverColumna(columnasDetalle, "idDetalleEntrada", "id", "id_detalle_entrada");
        String colClaveEntrada = resolverColumna(columnasDetalle, "claveEntrada", "idEntrada", "id_entrada", "entrada_id");
        String colDetalleProducto = resolverColumna(columnasDetalle, "claveProducto", "idProducto",
                "id_producto", "producto_id");

        String colArticuloDetalle = resolverColumna(columnasArticulo, "idDetalleEntrada", "id_detalle_entrada",
                "detalleEntrada", "detalle_entrada", "detalle_entrada_id");
        String colArticuloId = resolverColumna(columnasArticulo, "idArticulo", "id", "id_articulo");
        String colArticuloLote = resolverColumna(columnasArticulo, "lote");
        String colArticuloCaducidad = resolverColumna(columnasArticulo, "caducidad");
        String colArticuloUbicacion = resolverColumna(columnasArticulo, "ubicacion", "idUbicacion", "id_ubicacion");
        String colArticuloPresentacion = resolverColumna(columnasArticulo, "presentacion");
        String colArticuloFactor = resolverColumna(columnasArticulo, "factor");

        String colUbicacionId = resolverColumna(columnasUbicacion, "id", "idUbicacion", "ubicacion_id");
        String colUbicacionNombre = resolverColumna(columnasUbicacion, "nombre", "Nombre", "ubicacion");
        String colProductoId = resolverColumna(columnasProducto, "id", "idProducto", "claveProducto");
        String colProductoNombre = resolverColumna(columnasProducto, "nombre", "Nombre", "producto");

        if (colDetalleId == null || colClaveEntrada == null || colDetalleProducto == null ||
                colArticuloDetalle == null) {
            return;
        }

        String joinUbicacion = (colArticuloUbicacion != null && colUbicacionId != null && colUbicacionNombre != null)
                ? "LEFT JOIN ubicaciones u ON a.`" + colArticuloUbicacion + "` = u.`" + colUbicacionId + "`"
                : "";
        String ubicacionExpr = (colArticuloUbicacion != null && colUbicacionId != null && colUbicacionNombre != null)
                ? "u.`" + colUbicacionNombre + "`"
                : "NULL";
        String joinProducto = (colProductoId != null && colProductoNombre != null)
                ? "LEFT JOIN productos p ON d.`" + colDetalleProducto + "` = p.`" + colProductoId + "`"
                : "";
        String productoExpr = (colProductoId != null && colProductoNombre != null)
                ? "p.`" + colProductoNombre + "`"
                : "d.`" + colDetalleProducto + "`";

        String sql = String.format("""
                SELECT d.`%s` AS idDetalle,
                       %s AS idArticulo,
                       %s AS lote,
                       %s AS caducidad,
                       %s AS presentacion,
                       %s AS factor,
                       %s AS ubicacion,
                       %s AS producto
                FROM articulo a
                JOIN detalle_Entrada d ON a.`%s` = d.`%s`
                %s
                %s
                WHERE d.`%s` = ?
                ORDER BY producto, lote, caducidad, ubicacion, idArticulo
                """,
                colDetalleId,
                columnaSeguro("a", colArticuloId),
                columnaSeguro("a", colArticuloLote),
                columnaSeguro("a", colArticuloCaducidad),
                columnaSeguro("a", colArticuloPresentacion),
                columnaSeguro("a", colArticuloFactor),
                ubicacionExpr,
                productoExpr,
                colArticuloDetalle,
                colDetalleId,
                joinUbicacion,
                joinProducto,
                colClaveEntrada
        );

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, claveMovimiento);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int idDetalle = rs.getInt("idDetalle");
                    DetalleLinea linea = lineasPorId.get(idDetalle);
                    if (linea == null) {
                        continue;
                    }
                    linea.articulos.add(new DetalleArticulo(
                            valorTexto(rs.getObject("ubicacion")),
                            valorTexto(rs.getObject("lote")),
                            valorTexto(rs.getObject("caducidad")),
                            valorTexto(rs.getObject("presentacion")),
                            valorTexto(rs.getObject("factor"))
                    ));
                }
            }
        }
    }

    private void cargarArticulosSalida(Connection conn, String claveMovimiento,
                                       Map<Integer, DetalleLinea> lineasPorId) throws SQLException {
        Map<String, String> columnasDetalle = obtenerColumnas(conn, "detalle_Salida");
        Map<String, String> columnasArticulo = obtenerColumnas(conn, "articulo");
        Map<String, String> columnasUbicacion = obtenerColumnas(conn, "ubicaciones");
        Map<String, String> columnasProducto = obtenerColumnas(conn, "productos");

        String colDetalleId = resolverColumna(columnasDetalle, "idDetalleSalida", "id", "id_detalle_salida");
        String colClaveSalida = resolverColumna(columnasDetalle, "claveSalida", "idSalida", "id_salida", "salida_id");
        String colDetalleProducto = resolverColumna(columnasDetalle, "claveProductoSalida", "claveProducto",
                "idProducto", "id_producto", "producto_id");

        String colArticuloDetalle = resolverColumna(columnasArticulo, "idDetalleSalida", "id_detalle_salida",
                "detalleSalida", "detalle_salida", "detalle_salida_id");
        String colArticuloId = resolverColumna(columnasArticulo, "idArticulo", "id", "id_articulo");
        String colArticuloLote = resolverColumna(columnasArticulo, "lote");
        String colArticuloCaducidad = resolverColumna(columnasArticulo, "caducidad");
        String colArticuloUbicacion = resolverColumna(columnasArticulo, "ubicacion", "idUbicacion", "id_ubicacion");
        String colArticuloPresentacion = resolverColumna(columnasArticulo, "presentacion");
        String colArticuloFactor = resolverColumna(columnasArticulo, "factor");

        String colUbicacionId = resolverColumna(columnasUbicacion, "id", "idUbicacion", "ubicacion_id");
        String colUbicacionNombre = resolverColumna(columnasUbicacion, "nombre", "Nombre", "ubicacion");
        String colProductoId = resolverColumna(columnasProducto, "id", "idProducto", "claveProducto");
        String colProductoNombre = resolverColumna(columnasProducto, "nombre", "Nombre", "producto");

        if (colDetalleId == null || colClaveSalida == null || colDetalleProducto == null ||
                colArticuloDetalle == null) {
            return;
        }

        String joinUbicacion = (colArticuloUbicacion != null && colUbicacionId != null && colUbicacionNombre != null)
                ? "LEFT JOIN ubicaciones u ON a.`" + colArticuloUbicacion + "` = u.`" + colUbicacionId + "`"
                : "";
        String ubicacionExpr = (colArticuloUbicacion != null && colUbicacionId != null && colUbicacionNombre != null)
                ? "u.`" + colUbicacionNombre + "`"
                : "NULL";
        String joinProducto = (colProductoId != null && colProductoNombre != null)
                ? "LEFT JOIN productos p ON d.`" + colDetalleProducto + "` = p.`" + colProductoId + "`"
                : "";
        String productoExpr = (colProductoId != null && colProductoNombre != null)
                ? "p.`" + colProductoNombre + "`"
                : "d.`" + colDetalleProducto + "`";

        String sql = String.format("""
                SELECT d.`%s` AS idDetalle,
                       %s AS idArticulo,
                       %s AS lote,
                       %s AS caducidad,
                       %s AS presentacion,
                       %s AS factor,
                       %s AS ubicacion,
                       %s AS producto
                FROM articulo a
                JOIN detalle_Salida d ON a.`%s` = d.`%s`
                %s
                %s
                WHERE d.`%s` = ?
                ORDER BY producto, lote, caducidad, ubicacion, idArticulo
                """,
                colDetalleId,
                columnaSeguro("a", colArticuloId),
                columnaSeguro("a", colArticuloLote),
                columnaSeguro("a", colArticuloCaducidad),
                columnaSeguro("a", colArticuloPresentacion),
                columnaSeguro("a", colArticuloFactor),
                ubicacionExpr,
                productoExpr,
                colArticuloDetalle,
                colDetalleId,
                joinUbicacion,
                joinProducto,
                colClaveSalida
        );

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, claveMovimiento);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int idDetalle = rs.getInt("idDetalle");
                    DetalleLinea linea = lineasPorId.get(idDetalle);
                    if (linea == null) {
                        continue;
                    }
                    linea.articulos.add(new DetalleArticulo(
                            valorTexto(rs.getObject("ubicacion")),
                            valorTexto(rs.getObject("lote")),
                            valorTexto(rs.getObject("caducidad")),
                            valorTexto(rs.getObject("presentacion")),
                            valorTexto(rs.getObject("factor"))
                    ));
                }
            }
        }
    }

    private void renderizarDetalles(List<DetalleLinea> lineas, boolean detallado) {
        if (contenedorDetalles == null) {
            return;
        }
        contenedorDetalles.getChildren().clear();

        if (lineas == null || lineas.isEmpty()) {
            mostrarSinDetalles();
            return;
        }

        Map<String, List<DetalleLinea>> agrupadas = new LinkedHashMap<>();
        for (DetalleLinea linea : lineas) {
            agrupadas.computeIfAbsent(linea.tipo, key -> new ArrayList<>()).add(linea);
        }

        for (Map.Entry<String, List<DetalleLinea>> entry : agrupadas.entrySet()) {
            Label seccion = new Label("Detalles de " + entry.getKey().toLowerCase());
            seccion.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #333333;");
            contenedorDetalles.getChildren().add(seccion);

            int contador = 1;
            for (DetalleLinea linea : entry.getValue()) {
                VBox card = new VBox(6);
                card.setStyle("-fx-padding: 12; -fx-background-color: #f5f5f5; " +
                        "-fx-border-color: #ddd; -fx-border-radius: 6; -fx-background-radius: 6;");

                Label titulo = new Label(contador++ + ". " + valorTexto(linea.producto));
                titulo.setStyle("-fx-font-weight: bold; -fx-font-size: 13; -fx-text-fill: #2c3e50;");

                VBox detalles = new VBox(4);
                detalles.setStyle("-fx-padding: 0 0 0 18;");
                detalles.getChildren().addAll(
                        crearLineaDetalle("Clave", linea.claveProducto),
                        crearLineaDetalle("Cantidad", linea.cantidad),
                        crearLineaDetalle("Precio unitario", linea.precioUnitario),
                        crearLineaDetalle("Precio total", linea.precioTotal),
                        crearLineaDetalle("Nota", linea.nota)
                );

                card.getChildren().addAll(titulo, detalles);

                if (detallado) {
                    VBox listaArticulos = new VBox(3);
                    listaArticulos.setStyle("-fx-padding: 6 0 0 18;");
                    Label tituloArticulos = new Label("Artículos detallados:");
                    tituloArticulos.setStyle("-fx-font-weight: bold; -fx-font-size: 12;");
                    listaArticulos.getChildren().add(tituloArticulos);

                    if (linea.articulos.isEmpty()) {
                        listaArticulos.getChildren().add(new Label("Sin artículos detallados."));
                    } else {
                        int index = 1;
                        for (DetalleArticulo articulo : linea.articulos) {
                            String descripcion = String.format(
                                    "%d) Ubicación: %s | Lote: %s | Caducidad: %s | Presentación: %s | Factor: %s",
                                    index++,
                                    valorTexto(articulo.ubicacion),
                                    valorTexto(articulo.lote),
                                    valorTexto(articulo.caducidad),
                                    valorTexto(articulo.presentacion),
                                    valorTexto(articulo.factor)
                            );
                            listaArticulos.getChildren().add(new Label(descripcion));
                        }
                    }
                    card.getChildren().add(listaArticulos);
                }

                contenedorDetalles.getChildren().add(card);
            }
        }
    }

    private void mostrarSinDetalles() {
        Platform.runLater(() -> {
            if (contenedorDetalles == null) {
                return;
            }
            contenedorDetalles.getChildren().setAll(new Label("Sin detalles disponibles."));
        });
    }

    private Label crearLineaDetalle(String titulo, String valor) {
        String texto = titulo + ": " + valorTexto(valor);
        return new Label(texto);
    }

    private String valorTexto(Object valor) {
        if (valor == null) {
            return "";
        }
        String texto = valor.toString();
        return texto.isBlank() ? "" : texto;
    }

    private String columnaOrNull(String columna) {
        if (columna == null) {
            return "NULL";
        }
        return "d.`" + columna + "`";
    }

    private String columnaSeguro(String alias, String columna) {
        if (columna == null) {
            return "NULL";
        }
        return alias + ".`" + columna + "`";
    }

    private Map<String, String> obtenerColumnas(Connection conn, String tabla) throws SQLException {
        Map<String, String> columnas = new HashMap<>();
        try (ResultSet rs = conn.getMetaData().getColumns(conn.getCatalog(), null, tabla, null)) {
            while (rs.next()) {
                String nombre = rs.getString("COLUMN_NAME");
                if (nombre == null) {
                    continue;
                }
                String limpio = nombre.trim();
                columnas.put(limpio.toLowerCase(), limpio);
            }
        }
        return columnas;
    }

    private String resolverColumna(Map<String, String> columnas, String... alternativas) {
        for (String alternativa : alternativas) {
            if (alternativa == null) {
                continue;
            }
            String resultado = columnas.get(alternativa.toLowerCase());
            if (resultado != null) {
                return resultado;
            }
        }
        return null;
    }

    private static class DetalleLinea {
        private final String tipo;
        private final String producto;
        private final String claveProducto;
        private final String cantidad;
        private final String precioUnitario;
        private final String precioTotal;
        private final String nota;
        private final List<DetalleArticulo> articulos = new ArrayList<>();

        private DetalleLinea(String tipo, String producto, String claveProducto, String cantidad,
                             String precioUnitario, String precioTotal, String nota) {
            this.tipo = tipo;
            this.producto = producto;
            this.claveProducto = claveProducto;
            this.cantidad = cantidad;
            this.precioUnitario = precioUnitario;
            this.precioTotal = precioTotal;
            this.nota = nota;
        }
    }

    private static class DetalleArticulo {
        private final String ubicacion;
        private final String lote;
        private final String caducidad;
        private final String presentacion;
        private final String factor;

        private DetalleArticulo(String ubicacion, String lote, String caducidad,
                                String presentacion, String factor) {
            this.ubicacion = ubicacion;
            this.lote = lote;
            this.caducidad = caducidad;
            this.presentacion = presentacion;
            this.factor = factor;
        }
    }
}
