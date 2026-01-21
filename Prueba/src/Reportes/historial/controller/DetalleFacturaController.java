package Reportes.historial.controller;

import Reportes.historial.model.HistorialFactura;
import conexion.Conexion;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
        String colPrecioIva = resolverColumna(columnasDetalle, "precioIVA", "precioIva", "precio_iva");
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
                       %s AS precioIva,
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
                columnaOrNull(colPrecioIva),
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
                            valorTexto(rs.getObject("precioIva")),
                            valorTexto(rs.getObject("precioTotal")),
                            valorTexto(rs.getObject("nota")),
                            idDetalle
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
        String colPrecioIva = resolverColumna(columnasDetalle, "precioIVASalida", "precioIVA", "precioIva", "precio_iva");
        String colPrecioTotal = resolverColumna(columnasDetalle, "precioTotalSalida", "precioTotal", "precio_total");
        String colNota = resolverColumna(columnasDetalle, "Nota", "nota", "comentario", "observaciones");

        String colProductoId = resolverColumna(columnasProducto, "id", "idProducto", "claveProducto");
        String colProductoNombre = resolverColumna(columnasProducto, "nombre", "Nombre", "producto");

        Map<String, String> columnasSalidas = obtenerColumnas(conn, "salidas");
        String colSalidaId = resolverColumna(columnasSalidas, "idSalida", "id", "id_salida");
        String colTipoSalida = resolverColumna(columnasSalidas, "tipoSalida", "tipo", "tipo_salida");

        if (colDetalleId == null || colClaveSalida == null || colProducto == null) {
            return List.of();
        }

        String joinProducto = (colProductoId != null && colProductoNombre != null)
                ? "LEFT JOIN productos p ON d.`" + colProducto + "` = p.`" + colProductoId + "`"
                : "";
        String productoExpr = (colProductoId != null && colProductoNombre != null)
                ? "p.`" + colProductoNombre + "`"
                : "d.`" + colProducto + "`";

        String joinSalida = (colSalidaId != null && colTipoSalida != null)
                ? "LEFT JOIN salidas s ON d.`" + colClaveSalida + "` = s.`" + colSalidaId + "`"
                : "";
        String tipoSalidaExpr = (colSalidaId != null && colTipoSalida != null)
                ? "s.`" + colTipoSalida + "`"
                : "NULL";

        String sql = String.format("""
                SELECT d.`%s` AS idDetalle,
                       d.`%s` AS claveProducto,
                       %s AS cantidad,
                       %s AS precioUnitario,
                       %s AS precioIva,
                       %s AS precioTotal,
                       %s AS nota,
                       %s AS producto,
                       %s AS tipoSalida
                FROM detalle_Salida d
                %s
                %s
                WHERE d.`%s` = ?
                ORDER BY producto, idDetalle
                """,
                colDetalleId,
                colProducto,
                columnaOrNull(colCantidad),
                columnaOrNull(colPrecioUnitario),
                columnaOrNull(colPrecioIva),
                columnaOrNull(colPrecioTotal),
                columnaOrNull(colNota),
                productoExpr,
                tipoSalidaExpr,
                joinProducto,
                joinSalida,
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
                            valorTexto(rs.getObject("precioIva")),
                            valorTexto(rs.getObject("precioTotal")),
                            valorTexto(rs.getObject("nota")),
                            idDetalle
                    );
                    linea.tipoSalida = valorTexto(rs.getObject("tipoSalida"));
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
        String colArticuloEstado = resolverColumna(columnasArticulo, "Estado", "estado");
        String colArticuloDetalleEntrada = resolverColumna(columnasArticulo, "idDetalleEntrada", "id_detalle_entrada",
                "detalleEntrada", "detalle_entrada", "detalle_entrada_id");
        String colArticuloDetalleSalida = resolverColumna(columnasArticulo, "idDetalleSalida", "id_detalle_salida",
                "detalleSalida", "detalle_salida", "detalle_salida_id");

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
                       %s AS producto,
                       %s AS estadoArticulo,
                       %s AS detalleEntrada,
                       %s AS detalleSalida
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
                columnaSeguro("a", colArticuloEstado),
                columnaSeguro("a", colArticuloDetalleEntrada),
                columnaSeguro("a", colArticuloDetalleSalida),
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
                            valorTexto(rs.getObject("factor")),
                            valorTexto(rs.getObject("estadoArticulo")),
                            rs.getInt("idArticulo"),
                            rs.getObject("detalleEntrada"),
                            rs.getObject("detalleSalida")
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
        String colArticuloEstado = resolverColumna(columnasArticulo, "Estado", "estado");
        String colArticuloDetalleEntrada = resolverColumna(columnasArticulo, "idDetalleEntrada", "id_detalle_entrada",
                "detalleEntrada", "detalle_entrada", "detalle_entrada_id");
        String colArticuloDetalleSalida = resolverColumna(columnasArticulo, "idDetalleSalida", "id_detalle_salida",
                "detalleSalida", "detalle_salida", "detalle_salida_id");

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
                       %s AS producto,
                       %s AS estadoArticulo,
                       %s AS detalleEntrada,
                       %s AS detalleSalida
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
                columnaSeguro("a", colArticuloEstado),
                columnaSeguro("a", colArticuloDetalleEntrada),
                columnaSeguro("a", colArticuloDetalleSalida),
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
                            valorTexto(rs.getObject("factor")),
                            valorTexto(rs.getObject("estadoArticulo")),
                            rs.getInt("idArticulo"),
                            rs.getObject("detalleEntrada"),
                            rs.getObject("detalleSalida")
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

                boolean todosDisponibles = linea.tieneArticulosDisponibles();

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
                                    "%d) Ubicación: %s | Lote: %s | Caducidad: %s | Presentación: %s | Factor: %s | Estado: %s",
                                    index++,
                                    valorTexto(articulo.ubicacion),
                                    valorTexto(articulo.lote),
                                    valorTexto(articulo.caducidad),
                                    valorTexto(articulo.presentacion),
                                    valorTexto(articulo.factor),
                                    valorTexto(articulo.estado)
                            );
                            HBox filaArticulo = new HBox(8);
                            Label texto = new Label(descripcion);
                            filaArticulo.getChildren().add(texto);
                            boolean mostrarAcciones = articulo.esDetalleSalida() || articulo.esDisponible();
                            if (mostrarAcciones) {
                                Button btnEditar = new Button("Editar");
                                btnEditar.setOnAction(event -> editarArticulo(articulo));
                                Button btnEliminar = new Button("Eliminar");
                                btnEliminar.setOnAction(event -> eliminarArticulo(articulo));
                                filaArticulo.getChildren().addAll(btnEditar, btnEliminar);
                            }
                            listaArticulos.getChildren().add(filaArticulo);
                        }
                    }
                    card.getChildren().add(listaArticulos);
                }

                if ("Entrada".equalsIgnoreCase(linea.tipo) && todosDisponibles) {
                    Button btnEditarPrecio = new Button("Editar precio unitario");
                    btnEditarPrecio.setOnAction(event -> editarPrecioEntrada(linea));
                    card.getChildren().add(btnEditarPrecio);
                }

                boolean esAjuste = historial != null && "Ajuste".equalsIgnoreCase(historial.getMovimiento());
                if ("Salida".equalsIgnoreCase(linea.tipo)
                        && (linea.esVenta() || esAjuste)) {
                    Button btnEditarPrecio = new Button("Editar precio salida");
                    btnEditarPrecio.setOnAction(event -> editarPrecioSalida(linea));
                    card.getChildren().add(btnEditarPrecio);
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

    private void editarArticulo(DetalleArticulo articulo) {
        if (articulo == null || articulo.idArticulo <= 0) {
            return;
        }
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Editar artículo");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        VBox contenido = new VBox(8);
        TextField txtUbicacion = new TextField(valorTexto(articulo.ubicacion));
        TextField txtLote = new TextField(valorTexto(articulo.lote));
        TextField txtCaducidad = new TextField(valorTexto(articulo.caducidad));
        TextField txtPresentacion = new TextField(valorTexto(articulo.presentacion));
        TextField txtFactor = new TextField(valorTexto(articulo.factor));

        contenido.getChildren().addAll(
                new Label("Ubicación:"), txtUbicacion,
                new Label("Lote:"), txtLote,
                new Label("Caducidad (YYYY-MM-DD):"), txtCaducidad,
                new Label("Presentación:"), txtPresentacion,
                new Label("Factor:"), txtFactor
        );
        dialog.getDialogPane().setContent(contenido);

        dialog.showAndWait().ifPresent(respuesta -> {
            if (respuesta != ButtonType.OK) {
                return;
            }
            try (Connection conn = new Conexion().conectar()) {
                if (conn == null) {
                    return;
                }
                Map<String, String> columnasArticulo = obtenerColumnas(conn, "articulo");
                Map<String, String> columnasUbicacion = obtenerColumnas(conn, "ubicaciones");

                String colId = resolverColumna(columnasArticulo, "idArticulo", "id", "id_articulo");
                String colUbicacion = resolverColumna(columnasArticulo, "ubicacion", "idUbicacion", "id_ubicacion");
                String colLote = resolverColumna(columnasArticulo, "lote");
                String colCaducidad = resolverColumna(columnasArticulo, "caducidad");
                String colPresentacion = resolverColumna(columnasArticulo, "presentacion");
                String colFactor = resolverColumna(columnasArticulo, "factor");

                if (colId == null) {
                    return;
                }

                Integer ubicacionId = null;
                String ubicacionTexto = txtUbicacion.getText();
                if (ubicacionTexto != null && !ubicacionTexto.isBlank() && colUbicacion != null) {
                    String colUbicacionId = resolverColumna(columnasUbicacion, "id", "idUbicacion", "ubicacion_id");
                    String colUbicacionNombre = resolverColumna(columnasUbicacion, "nombre", "Nombre", "ubicacion");
                    if (colUbicacionId != null && colUbicacionNombre != null) {
                        try (PreparedStatement ps = conn.prepareStatement(
                                "SELECT `" + colUbicacionId + "` FROM ubicaciones WHERE `" + colUbicacionNombre + "` = ?")) {
                            ps.setString(1, ubicacionTexto.trim());
                            try (ResultSet rs = ps.executeQuery()) {
                                if (rs.next()) {
                                    ubicacionId = rs.getInt(1);
                                }
                            }
                        }
                    }
                    if (ubicacionId == null) {
                        mostrarAdvertencia("Ubicación inválida", "No se encontró la ubicación ingresada.");
                        return;
                    }
                }

                StringBuilder sql = new StringBuilder("UPDATE articulo SET ");
                List<Object> valores = new ArrayList<>();
                agregarCampoActualizacion(sql, valores, colUbicacion, ubicacionId);
                agregarCampoActualizacion(sql, valores, colLote, valorTexto(txtLote.getText()));
                agregarCampoActualizacion(sql, valores, colCaducidad, parseDate(txtCaducidad.getText()));
                agregarCampoActualizacion(sql, valores, colPresentacion, valorTexto(txtPresentacion.getText()));
                agregarCampoActualizacion(sql, valores, colFactor, parseInteger(txtFactor.getText()));

                if (valores.isEmpty()) {
                    return;
                }
                sql.append(" WHERE `").append(colId).append("` = ?");
                valores.add(articulo.idArticulo);

                try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
                    for (int i = 0; i < valores.size(); i++) {
                        ps.setObject(i + 1, valores.get(i));
                    }
                    ps.executeUpdate();
                }
                cargarDetalles();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    private void eliminarArticulo(DetalleArticulo articulo) {
        if (articulo == null || articulo.idArticulo <= 0) {
            return;
        }
        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Eliminar artículo");
        confirmacion.setHeaderText(null);
        confirmacion.setContentText("¿Deseas eliminar el artículo seleccionado?");
        confirmacion.showAndWait().ifPresent(respuesta -> {
            if (respuesta != ButtonType.OK) {
                return;
            }
            try (Connection conn = new Conexion().conectar()) {
                if (conn == null) {
                    return;
                }
                Map<String, String> columnasArticulo = obtenerColumnas(conn, "articulo");
                String colId = resolverColumna(columnasArticulo, "idArticulo", "id", "id_articulo");
                String colEstado = resolverColumna(columnasArticulo, "Estado", "estado");
                if (colId == null) {
                    return;
                }
                if (colEstado == null) {
                    return;
                }
                if (articulo.esDetalleEntrada()) {
                    try (PreparedStatement ps = conn.prepareStatement(
                            "UPDATE articulo SET `" + colEstado + "` = ? WHERE `" + colId + "` = ?")) {
                        ps.setString(1, "eliminado");
                        ps.setInt(2, articulo.idArticulo);
                        ps.executeUpdate();
                    }
                } else if (articulo.esDetalleSalida()) {
                    try (PreparedStatement ps = conn.prepareStatement(
                            "UPDATE articulo SET `" + colEstado + "` = ? WHERE `" + colId + "` = ?")) {
                        ps.setString(1, "disponible");
                        ps.setInt(2, articulo.idArticulo);
                        ps.executeUpdate();
                    }
                }
                cargarDetalles();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    private void editarPrecioEntrada(DetalleLinea linea) {
        if (linea == null || linea.idDetalle <= 0) {
            return;
        }
        BigDecimal precioActual = parseDecimal(linea.precioUnitario);
        TextField txtPrecio = new TextField(precioActual != null ? precioActual.toPlainString() : "");
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Editar precio unitario");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        VBox contenido = new VBox(8, new Label("Precio unitario:"), txtPrecio);
        dialog.getDialogPane().setContent(contenido);

        dialog.showAndWait().ifPresent(respuesta -> {
            if (respuesta != ButtonType.OK) {
                return;
            }
            BigDecimal nuevoPrecio = parseDecimal(txtPrecio.getText());
            if (nuevoPrecio == null) {
                mostrarAdvertencia("Precio inválido", "Ingresa un precio válido.");
                return;
            }
            actualizarPrecioDetalle("detalle_Entrada", linea, nuevoPrecio, true);
        });
    }

    private void editarPrecioSalida(DetalleLinea linea) {
        if (linea == null || linea.idDetalle <= 0) {
            return;
        }
        BigDecimal precioActual = parseDecimal(linea.precioUnitario);
        TextField txtPrecio = new TextField(precioActual != null ? precioActual.toPlainString() : "");
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Editar precio salida");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        VBox contenido = new VBox(8, new Label("Precio unitario salida:"), txtPrecio);
        dialog.getDialogPane().setContent(contenido);

        dialog.showAndWait().ifPresent(respuesta -> {
            if (respuesta != ButtonType.OK) {
                return;
            }
            BigDecimal nuevoPrecio = parseDecimal(txtPrecio.getText());
            if (nuevoPrecio == null) {
                mostrarAdvertencia("Precio inválido", "Ingresa un precio válido.");
                return;
            }
            actualizarPrecioDetalle("detalle_Salida", linea, nuevoPrecio, false);
        });
    }

    private void actualizarPrecioDetalle(String tabla, DetalleLinea linea, BigDecimal nuevoPrecio, boolean esEntrada) {
        try (Connection conn = new Conexion().conectar()) {
            if (conn == null) {
                return;
            }
            Map<String, String> columnasDetalle = obtenerColumnas(conn, tabla);
            String colId = resolverColumna(columnasDetalle,
                    esEntrada ? "idDetalleEntrada" : "idDetalleSalida",
                    "id", esEntrada ? "id_detalle_entrada" : "id_detalle_salida");
            String colPrecioUnitario = resolverColumna(columnasDetalle,
                    esEntrada ? "precioUnitario" : "precioUnitarioSalida",
                    esEntrada ? "precioEntrada" : "precioSalida",
                    esEntrada ? "precio_entrada" : "precio_salida",
                    "precioSalidaUnitario");
            String colPrecioIva = resolverColumna(columnasDetalle,
                    esEntrada ? "precioIVA" : "precioIVASalida",
                    "precioIva", "precio_iva");
            String colPrecioBruto = resolverColumna(columnasDetalle,
                    esEntrada ? "precioBrutoTotal" : "precioBrutoTotalSalida",
                    "precioBrutoTotal", "precio_bruto");
            String colPrecioTotal = resolverColumna(columnasDetalle,
                    esEntrada ? "precioTotal" : "precioTotalSalida",
                    "precioTotal", "precio_total");

            if (colId == null || colPrecioUnitario == null) {
                return;
            }

            BigDecimal cantidad = parseDecimal(linea.cantidad);
            if (cantidad == null || cantidad.compareTo(BigDecimal.ZERO) <= 0) {
                mostrarAdvertencia("Cantidad inválida", "No se pudo determinar la cantidad del detalle.");
                return;
            }

            BigDecimal ivaRate = obtenerTasaIva(linea.precioUnitario, linea.precioIva);
            BigDecimal precioIva = nuevoPrecio.multiply(BigDecimal.ONE.add(ivaRate)).setScale(2, RoundingMode.HALF_UP);
            BigDecimal precioBruto = nuevoPrecio.multiply(cantidad).setScale(2, RoundingMode.HALF_UP);
            BigDecimal precioTotal = precioIva.multiply(cantidad).setScale(2, RoundingMode.HALF_UP);

            StringBuilder sql = new StringBuilder("UPDATE ").append(tabla).append(" SET ");
            List<Object> valores = new ArrayList<>();
            agregarCampoActualizacion(sql, valores, colPrecioUnitario, nuevoPrecio.setScale(2, RoundingMode.HALF_UP));
            agregarCampoActualizacion(sql, valores, colPrecioIva, precioIva);
            agregarCampoActualizacion(sql, valores, colPrecioBruto, precioBruto);
            agregarCampoActualizacion(sql, valores, colPrecioTotal, precioTotal);

            if (valores.isEmpty()) {
                return;
            }
            sql.append(" WHERE `").append(colId).append("` = ?");
            valores.add(linea.idDetalle);

            try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
                for (int i = 0; i < valores.size(); i++) {
                    ps.setObject(i + 1, valores.get(i));
                }
                ps.executeUpdate();
            }
            cargarDetalles();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private BigDecimal obtenerTasaIva(String precioUnitario, String precioIvaTexto) {
        BigDecimal unitario = parseDecimal(precioUnitario);
        BigDecimal precioIva = parseDecimal(precioIvaTexto);
        if (unitario == null || precioIva == null || unitario.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.valueOf(0.16);
        }
        BigDecimal rate = precioIva.divide(unitario, 4, RoundingMode.HALF_UP).subtract(BigDecimal.ONE);
        if (rate.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.valueOf(0.16);
        }
        return rate;
    }

    private void agregarCampoActualizacion(StringBuilder sql, List<Object> valores, String columna, Object valor) {
        if (columna == null) {
            return;
        }
        if (!valores.isEmpty()) {
            sql.append(", ");
        }
        sql.append("`").append(columna).append("` = ?");
        valores.add(valor);
    }

    private BigDecimal parseDecimal(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(valor.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer parseInteger(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(valor.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private java.sql.Date parseDate(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return java.sql.Date.valueOf(valor.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private void mostrarAdvertencia(String titulo, String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.WARNING);
        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);
        alerta.showAndWait();
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
        private final String precioIva;
        private final String precioTotal;
        private final String nota;
        private final int idDetalle;
        private final List<DetalleArticulo> articulos = new ArrayList<>();
        private String tipoSalida;

        private DetalleLinea(String tipo, String producto, String claveProducto, String cantidad,
                             String precioUnitario, String precioIva, String precioTotal, String nota, int idDetalle) {
            this.tipo = tipo;
            this.producto = producto;
            this.claveProducto = claveProducto;
            this.cantidad = cantidad;
            this.precioUnitario = precioUnitario;
            this.precioIva = precioIva;
            this.precioTotal = precioTotal;
            this.nota = nota;
            this.idDetalle = idDetalle;
        }

        private boolean esVenta() {
            if (tipoSalida == null) {
                return false;
            }
            return "venta".equalsIgnoreCase(tipoSalida.trim());
        }

        private boolean tieneArticulosDisponibles() {
            if (articulos.isEmpty()) {
                return false;
            }
            for (DetalleArticulo articulo : articulos) {
                if (!articulo.esDisponible()) {
                    return false;
                }
            }
            return true;
        }
    }

    private static class DetalleArticulo {
        private final String ubicacion;
        private final String lote;
        private final String caducidad;
        private final String presentacion;
        private final String factor;
        private final String estado;
        private final int idArticulo;
        private final Integer detalleEntradaId;
        private final Integer detalleSalidaId;

        private DetalleArticulo(String ubicacion, String lote, String caducidad,
                                String presentacion, String factor, String estado, int idArticulo,
                                Object detalleEntradaId, Object detalleSalidaId) {
            this.ubicacion = ubicacion;
            this.lote = lote;
            this.caducidad = caducidad;
            this.presentacion = presentacion;
            this.factor = factor;
            this.estado = estado;
            this.idArticulo = idArticulo;
            this.detalleEntradaId = parseInteger(detalleEntradaId);
            this.detalleSalidaId = parseInteger(detalleSalidaId);
        }

        private boolean esDisponible() {
            if (estado == null) {
                return false;
            }
            return "disponible".equalsIgnoreCase(estado.trim());
        }

        private boolean esDetalleEntrada() {
            return detalleEntradaId != null;
        }

        private boolean esDetalleSalida() {
            return detalleSalidaId != null;
        }

        private static Integer parseInteger(Object valor) {
            if (valor == null) {
                return null;
            }
            if (valor instanceof Number) {
                return ((Number) valor).intValue();
            }
            String texto = valor.toString();
            if (texto.isBlank()) {
                return null;
            }
            try {
                return Integer.valueOf(texto.trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }
    }
}
