package Reportes.historial.controller;

import java.time.LocalDate;

import javafx.collections.ObservableList;
import javafx.scene.control.DatePicker;
import javafx.scene.control.ComboBox;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.geometry.Pos;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
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
    @FXML private Button btnCancelar;
    @FXML private Button btnCancelarEntrada;
    @FXML private ScrollPane scrollPane;
    private final ObservableList<String> presentaciones = FXCollections.observableArrayList(
            "paquete", "pz", "caja", "bolsa", "pieza", "rollo", "litro", "kilogramo", "metro", "unidad"
    );

    private HistorialFactura historial;
    private Stage stage;
    private Runnable onRefresh;

    @FXML
    public void initialize() {
        if (chkDetallado != null) {
            chkDetallado.selectedProperty().addListener((obs, oldVal, newVal) -> cargarDetalles());
        }
        actualizarTitulo();
        actualizarBotonCancelar();

    }

    public void setHistorial(HistorialFactura historial) {
        this.historial = historial;
        actualizarTitulo();
        actualizarBotonCancelar();
        cargarDetalles();
    }

    public void setStage(Stage stage) {
        this.stage = stage;
        if (stage != null) {
            stage.setResizable(false);

        }
    }

    public void setOnRefresh(Runnable onRefresh) {
        this.onRefresh = onRefresh;
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

    @FXML
    private void cancelarSalida() {
        if (historial == null || !"Salida".equalsIgnoreCase(historial.getMovimiento())) {
            return;
        }
        Integer salidaId = parseInteger(historial.getClaveMovimiento());
        if (salidaId == null || salidaId <= 0) {
            return;
        }
        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Cancelar salida");
        confirmacion.setHeaderText(null);
        confirmacion.setContentText("¿Deseas cancelar la salida seleccionada?");
        confirmacion.showAndWait().ifPresent(respuesta -> {
            if (respuesta != ButtonType.OK) {
                return;
            }
            try (Connection conn = new Conexion().conectar()) {
                if (conn == null) {
                    return;
                }
                conn.setAutoCommit(false);

                try {
                    Map<String, String> columnasDetalle = obtenerColumnas(conn, "detalle_Salida");
                    Map<String, String> columnasArticulo = obtenerColumnas(conn, "articulo");
                    Map<String, String> columnasSalida = obtenerColumnas(conn, "salidas");

                    String colDetalleId = resolverColumna(columnasDetalle, "idDetalleSalida", "id", "id_detalle_salida");
                    String colDetalleClaveSalida = resolverColumna(columnasDetalle, "claveSalida", "idSalida", "id_salida",
                            "salida_id");
                    String colDetalleCantidad = resolverColumna(columnasDetalle, "cantidad", "cantidadSalida", "cantidad_salida");
                    String colDetallePrecioBruto = resolverColumna(columnasDetalle, "precioBrutoTotalSalida",
                            "precioBrutoTotal", "precio_bruto");
                    String colDetallePrecioTotal = resolverColumna(columnasDetalle, "precioTotalSalida", "precioTotal",
                            "precio_total");
                    String colDetalleEstado = resolverColumna(columnasDetalle, "estado", "Estado");

                    String colArticuloDetalleSalida = resolverColumna(columnasArticulo, "idDetalleSalida", "id_detalle_salida",
                            "detalleSalida", "detalle_salida", "detalle_salida_id");
                    String colArticuloEstado = resolverColumna(columnasArticulo, "Estado", "estado");
                    String colSalidaId = resolverColumna(columnasSalida, "idSalida", "id", "id_salida");
                    String colSalidaPrecioNeto = resolverColumna(columnasSalida, "precioNetoSalida", "precioNeto", "precio_neto");
                    String colSalidaPrecioTotal = resolverColumna(columnasSalida, "precioTotalSalida", "precioTotal",
                            "precio_total");
                    String colSalidaEstado = resolverColumna(columnasSalida, "Estado", "estado");

                    if (colDetalleId == null || colDetalleClaveSalida == null || colSalidaId == null || colSalidaEstado == null) {
                        conn.rollback();
                        return;
                    }

                    List<Integer> detallesSalida = new ArrayList<>();
                    String sqlDetalles = "SELECT `" + colDetalleId + "` AS idDetalle FROM detalle_Salida WHERE `"
                            + colDetalleClaveSalida + "` = ?";
                    try (PreparedStatement ps = conn.prepareStatement(sqlDetalles)) {
                        ps.setInt(1, salidaId);
                        try (ResultSet rs = ps.executeQuery()) {
                            while (rs.next()) {
                                detallesSalida.add(rs.getInt("idDetalle"));
                            }
                        }
                    }

                    if (colArticuloDetalleSalida != null && colArticuloEstado != null && !detallesSalida.isEmpty()) {
                        String sqlActualizarArticulo = "UPDATE articulo SET `" + colArticuloEstado + "` = ?, `"
                                + colArticuloDetalleSalida + "` = NULL WHERE `" + colArticuloDetalleSalida + "` = ?";
                        try (PreparedStatement ps = conn.prepareStatement(sqlActualizarArticulo)) {
                            for (Integer detalleId : detallesSalida) {
                                ps.setString(1, "disponible");
                                ps.setInt(2, detalleId);
                                ps.addBatch();
                            }
                            ps.executeBatch();
                        }
                    }

                    if (!detallesSalida.isEmpty()) {
                        StringBuilder updateDetalle = new StringBuilder("UPDATE detalle_Salida SET ");
                        List<Object> valoresDetalle = new ArrayList<>();
                        agregarCampoActualizacion(updateDetalle, valoresDetalle, colDetalleCantidad, 0);
                        agregarCampoActualizacion(updateDetalle, valoresDetalle, colDetallePrecioBruto, BigDecimal.ZERO);
                        agregarCampoActualizacion(updateDetalle, valoresDetalle, colDetallePrecioTotal, BigDecimal.ZERO);
                        agregarCampoActualizacion(updateDetalle, valoresDetalle, colDetalleEstado, "desactivado");
                        if (!valoresDetalle.isEmpty()) {
                            updateDetalle.append(" WHERE `").append(colDetalleId).append("` = ?");
                            try (PreparedStatement ps = conn.prepareStatement(updateDetalle.toString())) {
                                for (Integer detalleId : detallesSalida) {
                                    int index = 1;
                                    for (Object valor : valoresDetalle) {
                                        ps.setObject(index++, valor);
                                    }
                                    ps.setInt(index, detalleId);
                                    ps.addBatch();
                                }
                                ps.executeBatch();
                            }
                        }
                    }

                    StringBuilder updateSalida = new StringBuilder("UPDATE salidas SET ");
                    List<Object> valoresSalida = new ArrayList<>();
                    agregarCampoActualizacion(updateSalida, valoresSalida, colSalidaPrecioNeto, BigDecimal.ZERO);
                    agregarCampoActualizacion(updateSalida, valoresSalida, colSalidaPrecioTotal, BigDecimal.ZERO);
                    agregarCampoActualizacion(updateSalida, valoresSalida, colSalidaEstado, "cancelado");
                    if (!valoresSalida.isEmpty()) {
                        updateSalida.append(" WHERE `").append(colSalidaId).append("` = ?");
                        valoresSalida.add(salidaId);
                        try (PreparedStatement ps = conn.prepareStatement(updateSalida.toString())) {
                            for (int i = 0; i < valoresSalida.size(); i++) {
                                ps.setObject(i + 1, valoresSalida.get(i));
                            }
                            ps.executeUpdate();
                        }
                    }

                    conn.commit();
                    notificarActualizacion();
                    cargarDetalles();
                } catch (SQLException e) {
                    conn.rollback();
                    throw e;
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    @FXML
    private void cancelarEntrada() {
        if (historial == null || !"Entrada".equalsIgnoreCase(historial.getMovimiento())) {
            return;
        }
        Integer entradaId = parseInteger(historial.getClaveMovimiento());
        if (entradaId == null || entradaId <= 0) {
            return;
        }
        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Cancelar entrada");
        confirmacion.setHeaderText(null);
        confirmacion.setContentText("¿Deseas cancelar la entrada seleccionada?");
        confirmacion.showAndWait().ifPresent(respuesta -> {
            if (respuesta != ButtonType.OK) {
                return;
            }
            try (Connection conn = new Conexion().conectar()) {
                if (conn == null) {
                    return;
                }
                conn.setAutoCommit(false);

                try {
                    Map<String, String> columnasDetalle = obtenerColumnas(conn, "detalle_Entrada");
                    Map<String, String> columnasArticulo = obtenerColumnas(conn, "articulo");
                    Map<String, String> columnasEntrada = obtenerColumnas(conn, "entradas");

                    String colDetalleId = resolverColumna(columnasDetalle, "idDetalleEntrada", "id", "id_detalle_entrada");
                    String colDetalleClaveEntrada = resolverColumna(columnasDetalle, "claveEntrada", "idEntrada", "id_entrada",
                            "entrada_id");
                    String colDetalleCantidad = resolverColumna(columnasDetalle, "cantidad", "cantidadEntrada");
                    String colDetallePrecioBruto = resolverColumna(columnasDetalle, "precioBrutoTotal", "precioBruto",
                            "precio_bruto");
                    String colDetallePrecioTotal = resolverColumna(columnasDetalle, "precioTotal", "precio_total");
                    String colDetalleEstado = resolverColumna(columnasDetalle, "estado", "Estado");

                    String colArticuloDetalleEntrada = resolverColumna(columnasArticulo, "idDetalleEntrada", "id_detalle_entrada",
                            "detalleEntrada", "detalle_entrada", "detalle_entrada_id");
                    String colArticuloEstado = resolverColumna(columnasArticulo, "Estado", "estado");

                    String colEntradaId = resolverColumna(columnasEntrada, "idEntrada", "id", "id_entrada");
                    String colEntradaPrecioNeto = resolverColumna(columnasEntrada, "precioNetoEntrada", "precioNeto", "precio_neto");
                    String colEntradaPrecioTotal = resolverColumna(columnasEntrada, "precioTotalEntrada", "precioTotal",
                            "precio_total");
                    String colEntradaEstado = resolverColumna(columnasEntrada, "Estado", "estado");

                    if (colDetalleId == null || colDetalleClaveEntrada == null || colEntradaId == null || colEntradaEstado == null) {
                        conn.rollback();
                        return;
                    }

                    if (colArticuloDetalleEntrada == null || colArticuloEstado == null) {
                        conn.rollback();
                        return;
                    }

                    String sqlBloqueo = "SELECT COUNT(*) FROM articulo a "
                            + "JOIN detalle_Entrada d ON a.`" + colArticuloDetalleEntrada + "` = d.`" + colDetalleId + "` "
                            + "WHERE d.`" + colDetalleClaveEntrada + "` = ? "
                            + "AND LOWER(a.`" + colArticuloEstado + "`) IN ('pendiente', 'vendido')";
                    int bloqueados = 0;
                    try (PreparedStatement ps = conn.prepareStatement(sqlBloqueo)) {
                        ps.setInt(1, entradaId);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) {
                                bloqueados = rs.getInt(1);
                            }
                        }
                    }

                    if (bloqueados > 0) {
                        conn.rollback();
                        mostrarAdvertencia("No se puede cancelar",
                                "Ya hay productos vendidos o traspasados en esta entrada.");
                        return;
                    }

                    List<Integer> detallesEntrada = new ArrayList<>();
                    String sqlDetalles = "SELECT `" + colDetalleId + "` AS idDetalle FROM detalle_Entrada WHERE `"
                            + colDetalleClaveEntrada + "` = ?";
                    try (PreparedStatement ps = conn.prepareStatement(sqlDetalles)) {
                        ps.setInt(1, entradaId);
                        try (ResultSet rs = ps.executeQuery()) {
                            while (rs.next()) {
                                detallesEntrada.add(rs.getInt("idDetalle"));
                            }
                        }
                    }

                    if (!detallesEntrada.isEmpty()) {
                        String sqlActualizarArticulo = "UPDATE articulo SET `" + colArticuloEstado + "` = ? "
                                + "WHERE `" + colArticuloDetalleEntrada + "` = ?";
                        try (PreparedStatement ps = conn.prepareStatement(sqlActualizarArticulo)) {
                            for (Integer detalleId : detallesEntrada) {
                                ps.setString(1, "eliminado");
                                ps.setInt(2, detalleId);
                                ps.addBatch();
                            }
                            ps.executeBatch();
                        }
                    }

                    if (!detallesEntrada.isEmpty()) {
                        StringBuilder updateDetalle = new StringBuilder("UPDATE detalle_Entrada SET ");
                        List<Object> valoresDetalle = new ArrayList<>();
                        agregarCampoActualizacion(updateDetalle, valoresDetalle, colDetalleCantidad, 0);
                        agregarCampoActualizacion(updateDetalle, valoresDetalle, colDetallePrecioBruto, BigDecimal.ZERO);
                        agregarCampoActualizacion(updateDetalle, valoresDetalle, colDetallePrecioTotal, BigDecimal.ZERO);
                        agregarCampoActualizacion(updateDetalle, valoresDetalle, colDetalleEstado, "desactivado");
                        if (!valoresDetalle.isEmpty()) {
                            updateDetalle.append(" WHERE `").append(colDetalleId).append("` = ?");
                            try (PreparedStatement ps = conn.prepareStatement(updateDetalle.toString())) {
                                for (Integer detalleId : detallesEntrada) {
                                    int index = 1;
                                    for (Object valor : valoresDetalle) {
                                        ps.setObject(index++, valor);
                                    }
                                    ps.setInt(index, detalleId);
                                    ps.addBatch();
                                }
                                ps.executeBatch();
                            }
                        }
                    }

                    StringBuilder updateEntrada = new StringBuilder("UPDATE entradas SET ");
                    List<Object> valoresEntrada = new ArrayList<>();
                    agregarCampoActualizacion(updateEntrada, valoresEntrada, colEntradaPrecioNeto, BigDecimal.ZERO);
                    agregarCampoActualizacion(updateEntrada, valoresEntrada, colEntradaPrecioTotal, BigDecimal.ZERO);
                    agregarCampoActualizacion(updateEntrada, valoresEntrada, colEntradaEstado, "cancelado");
                    if (!valoresEntrada.isEmpty()) {
                        updateEntrada.append(" WHERE `").append(colEntradaId).append("` = ?");
                        valoresEntrada.add(entradaId);
                        try (PreparedStatement ps = conn.prepareStatement(updateEntrada.toString())) {
                            for (int i = 0; i < valoresEntrada.size(); i++) {
                                ps.setObject(i + 1, valoresEntrada.get(i));
                            }
                            ps.executeUpdate();
                        }
                    }

                    conn.commit();
                    notificarActualizacion();
                    cargarDetalles();
                } catch (SQLException e) {
                    conn.rollback();
                    throw e;
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
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

    /*private void actualizarBotonCancelar() {
        if (btnCancelar == null) {
            return;
        }
        boolean esSalida = historial != null && "Salida".equalsIgnoreCase(historial.getMovimiento());
        btnCancelar.setVisible(esSalida);
        btnCancelar.setManaged(esSalida);
        btnCancelar.setDisable(!esSalida);
        if (btnCancelarEntrada != null) {
            boolean esEntrada = historial != null && "Entrada".equalsIgnoreCase(historial.getMovimiento());
            btnCancelarEntrada.setVisible(esEntrada);
            btnCancelarEntrada.setManaged(esEntrada);
            btnCancelarEntrada.setDisable(!esEntrada);
        }
    }*/

    private void actualizarBotonCancelar() {
        if (btnCancelar == null) {
            return;
        }

        boolean esSalida = historial != null && "Salida".equalsIgnoreCase(historial.getMovimiento());
        boolean yaCancelada = false;

        if (esSalida) {
            // Verificar si ya está cancelada
            Integer salidaId = parseInteger(historial.getClaveMovimiento());
            if (salidaId != null && salidaId > 0) {
                try (Connection conn = new Conexion().conectar()) {
                    if (conn != null) {
                        yaCancelada = esSalidaCancelada(conn, salidaId);
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }

        // Mostrar/ocultar botón según estado
        boolean mostrarBoton = esSalida && !yaCancelada;
        btnCancelar.setVisible(mostrarBoton);
        btnCancelar.setManaged(mostrarBoton);
        btnCancelar.setDisable(!mostrarBoton);

        // También actualizar botón de entrada si existe
        if (btnCancelarEntrada != null) {
            boolean esEntrada = historial != null && "Entrada".equalsIgnoreCase(historial.getMovimiento());
            boolean entradaCancelada = false;

            if (esEntrada) {
                // Similar verificación para entradas
                Integer entradaId = parseInteger(historial.getClaveMovimiento());
                if (entradaId != null && entradaId > 0) {
                    try (Connection conn = new Conexion().conectar()) {
                        if (conn != null) {
                            entradaCancelada = esEntradaCancelada(conn, entradaId);
                        }
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            }

            boolean mostrarEntrada = esEntrada && !entradaCancelada;
            btnCancelarEntrada.setVisible(mostrarEntrada);
            btnCancelarEntrada.setManaged(mostrarEntrada);
            btnCancelarEntrada.setDisable(!mostrarEntrada);
        }
    }

    private boolean esEntradaCancelada(Connection conn, Integer entradaId) throws SQLException {
        if (entradaId == null || entradaId <= 0) {
            return false;
        }

        String sql = "SELECT `Estado`  FROM entradas WHERE `idEntrada` = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, entradaId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String estado = rs.getString("estado");
                    return estado != null && estado.equalsIgnoreCase("cancelado");
                }
            }
        }
        return false;
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
        String colEstado = resolverColumna(columnasDetalle, "estado", "Estado");

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

        String ordenEstado = colEstado != null
                ? "CASE WHEN LOWER(d.`" + colEstado + "`) = 'activo' THEN 0 ELSE 1 END, "
                : "";

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
                ORDER BY %sproducto, idDetalle
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
                colClaveEntrada,
                ordenEstado
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
        String colEstado = resolverColumna(columnasDetalle, "estado", "Estado");

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

        String ordenEstado = colEstado != null
                ? "CASE WHEN LOWER(d.`" + colEstado + "`) = 'activo' THEN 0 ELSE 1 END, "
                : "";

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
                ORDER BY %sproducto, idDetalle
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
                colClaveSalida,
                ordenEstado
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

        String ordenEstado = colArticuloEstado != null
                ? "CASE WHEN LOWER(a.`" + colArticuloEstado + "`) = 'eliminado' THEN 1 ELSE 0 END, "
                : "";

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
                ORDER BY %sproducto, lote, caducidad, ubicacion, idArticulo
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
                colClaveEntrada,
                ordenEstado
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

        String ordenEstado = colArticuloEstado != null
                ? "CASE WHEN LOWER(a.`" + colArticuloEstado + "`) = 'eliminado' THEN 1 ELSE 0 END, "
                : "";

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
                ORDER BY %sproducto, lote, caducidad, ubicacion, idArticulo
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
                colClaveSalida,
                ordenEstado
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

        boolean esSalidaCancelada = false;
        boolean esEntradaCancelada = false;

        if (historial != null) {
            String movimiento = historial.getMovimiento();
            String clave = historial.getClaveMovimiento();
            Integer idMovimiento = parseInteger(clave);

            if (idMovimiento != null && idMovimiento > 0) {
                try (Connection conn = new Conexion().conectar()) {
                    if (conn != null) {
                        if ("Salida".equalsIgnoreCase(movimiento)) {
                            esSalidaCancelada = esSalidaCancelada(conn, idMovimiento);
                        } else if ("Entrada".equalsIgnoreCase(movimiento)) {
                            esEntradaCancelada = esEntradaCancelada(conn, idMovimiento);
                        } else if ("Ajuste".equalsIgnoreCase(movimiento)) {
                            // Para ajustes, verificar si está cancelado
                            esSalidaCancelada = esAjusteCancelado(conn, idMovimiento);
                            esEntradaCancelada = esSalidaCancelada; // Mismo estado para ajustes
                        }
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }

        Map<String, List<DetalleLinea>> agrupadas = new LinkedHashMap<>();
        for (DetalleLinea linea : lineas) {
            agrupadas.computeIfAbsent(linea.tipo, key -> new ArrayList<>()).add(linea);
        }

        for (Map.Entry<String, List<DetalleLinea>> entry : agrupadas.entrySet()) {
            /*Label seccion = new Label("Detalles de " + entry.getKey().toLowerCase());
            seccion.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #333333;");
            contenedorDetalles.getChildren().add(seccion)*/;

            int contador = 1;
            for (DetalleLinea linea : entry.getValue()) {
                VBox card = new VBox(8);
                card.setStyle("-fx-padding: 15; -fx-background-color: white; " +
                        "-fx-border-color: #e0e0e0; -fx-border-width: 1; -fx-border-radius: 8; " +
                        "-fx-background-radius: 8; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 6, 0, 0, 2);");
                card.setMaxWidth(Double.MAX_VALUE);

                Label titulo = new Label(contador++ + ". " + valorTexto(linea.producto));
                titulo.setStyle("-fx-font-weight: bold; -fx-font-size: 14; -fx-text-fill: #2c3e50; " +
                        "-fx-padding: 0 0 5 0;");
                titulo.setWrapText(true);

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
                    VBox listaArticulos = new VBox(8);
                    listaArticulos.setStyle("-fx-padding: 10 0 0 0;");

                    Label tituloArticulos = new Label("Artículos Detallados:");
                    tituloArticulos.setStyle("-fx-font-weight: bold; -fx-font-size: 13; -fx-text-fill: #34495e; " +
                            "-fx-padding: 0 0 8 0;");
                    listaArticulos.getChildren().add(tituloArticulos);

                    if (linea.articulos.isEmpty()) {
                        Label sinArticulos = new Label("No hay artículos detallados.");
                        sinArticulos.setStyle("-fx-text-fill: #7f8c8d; -fx-font-style: italic; -fx-padding: 5 0;");
                        listaArticulos.getChildren().add(sinArticulos);
                    } else {
                        int index = 1;
                        for (DetalleArticulo articulo : linea.articulos) {
                            // Tarjeta para cada artículo
                            HBox articuloCard = new HBox(12);
                            articuloCard.setStyle("-fx-padding: 14; -fx-background-color: #f8f9fa; " +
                                    "-fx-border-color: #e9ecef; -fx-border-width: 1; " +
                                    "-fx-border-radius: 8; -fx-background-radius: 8;");
                            articuloCard.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

                            // Contenedor para botones (A LA IZQUIERDA)
                            HBox botonesContainer = new HBox(8);
                            botonesContainer.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

                            boolean mostrarAcciones = articulo.esDetalleSalida() || articulo.esDisponible();
                            if (mostrarAcciones) {
                                // Botón Editar con icono
                                Button btnEditar = new Button();
                                try {
                                    ImageView imgEditar = new ImageView(new Image(getClass().getResourceAsStream("/img/editar.png")));
                                    imgEditar.setFitWidth(16);
                                    imgEditar.setFitHeight(16);
                                    btnEditar.setGraphic(imgEditar);
                                } catch (Exception e) {
                                    btnEditar.setText("Editar");
                                }
                                btnEditar.setStyle("-fx-background-color: #333; -fx-cursor: hand; -fx-padding: 5 10; -fx-background-radius: 4;");
                                btnEditar.setOnAction(event -> editarArticulo(articulo));

                                Button btnEliminar = new Button();
                                try {
                                    ImageView imgEliminar = new ImageView(new Image(getClass().getResourceAsStream("/img/eliminar.png")));
                                    imgEliminar.setFitWidth(16);
                                    imgEliminar.setFitHeight(16);
                                    btnEliminar.setGraphic(imgEliminar);
                                } catch (Exception e) {
                                    btnEliminar.setText("Eliminar");
                                }
                                btnEliminar.setStyle("-fx-background-color: #333; -fx-cursor: hand; -fx-padding: 5 10; -fx-background-radius: 4;");
                                btnEliminar.setOnAction(event -> eliminarArticulo(articulo));

                                botonesContainer.getChildren().addAll(btnEditar, btnEliminar);
                            }

                            Label numero = new Label(index++ + ".");
                            numero.setStyle("-fx-font-weight: bold; -fx-font-size: 13; -fx-text-fill: #91d485; " +
                                    "-fx-min-width: 25; -fx-padding: 0 5 0 0;");

                            VBox infoBox = new VBox(6);
                            infoBox.setStyle("-fx-padding: 0 0 0 10;");

                            // Primera línea: Ubicación y Lote
                            HBox linea1 = new HBox(15);
                            Label lblUbicacion = crearEtiquetaDetalleElegante("Ubicación:", valorTexto(articulo.ubicacion));
                            Label lblLote = crearEtiquetaDetalleElegante("Lote:", valorTexto(articulo.lote));
                            linea1.getChildren().addAll(lblUbicacion, lblLote);

                            // Segunda línea: Caducidad y Presentación
                            HBox linea2 = new HBox(15);
                            Label lblPresentacion = crearEtiquetaDetalleElegante("Presentación:", valorTexto(articulo.presentacion));
                            Label lblFactor = crearEtiquetaDetalleElegante("Factor:", valorTexto(articulo.factor));
                            linea2.getChildren().addAll(lblPresentacion, lblFactor);
                            HBox linea3 = new HBox(15);
                            Label lblCaducidad = crearEtiquetaDetalleElegante("Caducidad:", valorTexto(articulo.caducidad));
                            linea3.getChildren().add(lblCaducidad);

                            // Cuarta línea: Solo Estado
                            HBox linea4 = new HBox(15);
                            Label lblEstado = new Label("Estado: " + valorTexto(articulo.estado));
                            String colorEstado = obtenerColorEstado(articulo.estado);
                            lblEstado.setStyle("-fx-font-weight: bold; -fx-text-fill: " + colorEstado + "; -fx-font-size: 12;");
                            linea4.getChildren().add(lblEstado);

                            infoBox.getChildren().addAll(linea1, linea2, linea3, linea4);

                            // ORDEN CORREGIDO: Botones a la izquierda, luego número, luego información
                            articuloCard.getChildren().addAll(numero, infoBox, botonesContainer);
                            HBox.setHgrow(infoBox, javafx.scene.layout.Priority.ALWAYS);

                            listaArticulos.getChildren().add(articuloCard);
                        }
                    }
                    card.getChildren().add(listaArticulos);
                }

                boolean esAjuste = historial != null && "Ajuste".equalsIgnoreCase(historial.getMovimiento());
                boolean puedeEditarEntrada = "Entrada".equalsIgnoreCase(linea.tipo)
                        && linea.tieneArticulosSinPendienteOVendido();

                HBox botonesContainer = new HBox(10);
                botonesContainer.setAlignment(Pos.CENTER_RIGHT);
                botonesContainer.setStyle("-fx-padding: 10 0 0 0;"); // Espacio superior

                boolean hayBotones = false;

                if (puedeEditarEntrada) {
                    Button btnEditarPrecio = new Button("Editar precio unitario");
                    btnEditarPrecio.setOnAction(event -> editarPrecioEntrada(linea));
                    btnEditarPrecio.getStyleClass().add("boton-formulario");
                    botonesContainer.getChildren().add(btnEditarPrecio);
                    hayBotones = true;
                }

                if ("Salida".equalsIgnoreCase(linea.tipo)
                        && (linea.esVenta() || esAjuste)) {
                    Button btnEditarPrecio = new Button("Editar precio salida");
                    btnEditarPrecio.setOnAction(event -> editarPrecioSalida(linea));
                    btnEditarPrecio.getStyleClass().add("boton-formulario");
                    botonesContainer.getChildren().add(btnEditarPrecio);
                    hayBotones = true;
                }
                if (hayBotones) {
                    card.getChildren().add(botonesContainer);
                }

                contenedorDetalles.getChildren().add(card);
            }
        }
    }
    private boolean esAjusteCancelado(Connection conn, Integer ajusteId) throws SQLException {
        if (ajusteId == null || ajusteId <= 0) {
            return false;
        }
        String sql = "SELECT `Estado` FROM ajuste_inventario WHERE `idAjuste` = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, ajusteId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String estado = rs.getString("Estado");
                    return estado != null && estado.equalsIgnoreCase("cancelado");
                }
            }
        }
        return false;
    }

    private Label crearEtiquetaDetalleElegante(String titulo, String valor) {
        String texto = titulo + " " + (valor.isEmpty() ? "N/A" : valor);
        Label label = new Label(texto);
        label.setStyle("-fx-font-size: 12; -fx-text-fill: #2c3e50; -fx-font-family: 'Segoe UI', Arial, sans-serif;");
        return label;
    }

    private String obtenerColorEstado(String estado) {
        if (estado == null || estado.isEmpty()) return "#333";

        String estadoLower = estado.toLowerCase();
        switch (estadoLower) {
            case "disponible": return "#91d485";
            case "pendiente": return "#e74c3c";
            case "vendido": return "#333";
            case "eliminado": return "#333";
            case "ajustado": return "#333";
            default: return "#333";
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
        DatePicker dpCaducidad = new DatePicker();
        dpCaducidad.getEditor().setDisable(true);
        dpCaducidad.getEditor().setStyle("-fx-opacity: 1.0; -fx-background-color: white;");
        dpCaducidad.setPromptText("Haz clic en el calendario");

        if (articulo.caducidad != null && !articulo.caducidad.trim().isEmpty()) {
            try {
                // Parsear la fecha en formato YYYY-MM-DD
                String[] partes = articulo.caducidad.trim().split("-");
                if (partes.length == 3) {
                    int year = Integer.parseInt(partes[0]);
                    int month = Integer.parseInt(partes[1]);
                    int day = Integer.parseInt(partes[2]);
                    LocalDate fecha = LocalDate.of(year, month, day);
                    dpCaducidad.setValue(fecha);
                }
            } catch (Exception e) {
                System.err.println("Error parsing date: " + articulo.caducidad);
            }
        }

        ComboBox<String> cbPresentacion = new ComboBox<>(presentaciones);

        String presentacionActual = valorTexto(articulo.presentacion).toLowerCase();
        if (presentacionActual != null && !presentacionActual.isEmpty()) {
            for (String opcion : presentaciones) {
                if (opcion.equalsIgnoreCase(presentacionActual)) {
                    cbPresentacion.setValue(opcion);
                    break;
                }
            }
            if (cbPresentacion.getValue() == null && !presentaciones.isEmpty()) {
                cbPresentacion.setValue(presentaciones.get(0));
            }
        } else if (!presentaciones.isEmpty()) {
            cbPresentacion.setValue(presentaciones.get(0));
        }

        // Deshabilitar edición manual
        cbPresentacion.setEditable(false);

        TextField txtFactor = new TextField(valorTexto(articulo.factor));

        contenido.getChildren().addAll(
                new Label("Ubicación:"), txtUbicacion,
                new Label("Lote:"), txtLote,
                new Label("Caducidad:"), dpCaducidad,
                new Label("Presentación:"), cbPresentacion,
                new Label("Factor:"), txtFactor
        );

        // Hacer que los campos tengan el mismo ancho
        txtUbicacion.setPrefWidth(200);
        txtLote.setPrefWidth(200);
        dpCaducidad.setPrefWidth(200);
        cbPresentacion.setPrefWidth(200);
        txtFactor.setPrefWidth(200);

        dialog.getDialogPane().setContent(contenido);

        ButtonType btnOk = ButtonType.OK;
        dialog.getDialogPane().lookupButton(btnOk).addEventFilter(ActionEvent.ACTION, event -> {
            if (cbPresentacion.getValue() == null || cbPresentacion.getValue().isEmpty()) {
                mostrarAdvertencia("Presentación requerida", "Selecciona una presentación de la lista");
                event.consume();
            }
        });

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

                java.sql.Date fechaCaducidad = null;
                if (dpCaducidad.getValue() != null) {
                    LocalDate fecha = dpCaducidad.getValue();
                    fechaCaducidad = java.sql.Date.valueOf(fecha);
                }
                agregarCampoActualizacion(sql, valores, colCaducidad, fechaCaducidad);

                String presentacionSeleccionada = cbPresentacion.getValue();
                if (presentacionSeleccionada == null && !presentaciones.isEmpty()) {
                    presentacionSeleccionada = presentaciones.get(0);
                }
                agregarCampoActualizacion(sql, valores, colPresentacion, valorTexto(presentacionSeleccionada));

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
        boolean esAjuste = historial != null && "Ajuste".equalsIgnoreCase(historial.getMovimiento());
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
                String colDetalleSalida = resolverColumna(columnasArticulo, "idDetalleSalida", "id_detalle_salida",
                        "detalleSalida", "detalle_salida", "detalle_salida_id");
                if (colId == null) {
                    return;
                }
                if (colEstado == null) {
                    return;
                }
                if (articulo.esDetalleSalida()) {
                    if (colDetalleSalida == null) {
                        return;
                    }
                    try (PreparedStatement ps = conn.prepareStatement(
                            "UPDATE articulo SET `" + colEstado + "` = ?, `" + colDetalleSalida
                                    + "` = NULL WHERE `" + colId + "` = ?")) {
                        ps.setString(1, "disponible");
                        ps.setInt(2, articulo.idArticulo);
                        ps.executeUpdate();
                    }
                    Integer ajusteId = ajustarTotalesSalida(conn, articulo, esAjuste);
                    actualizarEstadoDetalleSalidaSiVacio(conn, articulo.detalleSalidaId, !esAjuste);
                    if (esAjuste) {
                        actualizarEstadoAjusteSiVacio(conn, ajusteId);
                    }
                } else if (articulo.esDetalleEntrada()) {
                    try (PreparedStatement ps = conn.prepareStatement(
                            "UPDATE articulo SET `" + colEstado + "` = ? WHERE `" + colId + "` = ?")) {
                        ps.setString(1, "eliminado");
                        ps.setInt(2, articulo.idArticulo);
                        ps.executeUpdate();
                    }
                    Integer ajusteId = ajustarTotalesEntrada(conn, articulo, esAjuste);
                    actualizarEstadoDetalleEntradaSiVacio(conn, articulo.detalleEntradaId, !esAjuste);
                    if (esAjuste) {
                        actualizarEstadoAjusteSiVacio(conn, ajusteId);
                    }
                }
                notificarActualizacion();
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
            boolean esAjuste = historial != null && "Ajuste".equalsIgnoreCase(historial.getMovimiento());
            if (esAjuste) {
                actualizarTotalesAjustePorPrecio(conn, tabla, linea, nuevoPrecio, precioIva, cantidad, esEntrada);
            } else if (!esEntrada && linea.esVenta()) {
                actualizarTotalesSalidaPorPrecio(conn, linea, nuevoPrecio, precioIva, cantidad);
            }
            notificarActualizacion();
            cargarDetalles();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void actualizarTotalesSalidaPorPrecio(Connection conn, DetalleLinea linea, BigDecimal nuevoPrecioUnitario,
                                                  BigDecimal nuevoPrecioIva, BigDecimal cantidad) throws SQLException {
        if (linea == null || linea.idDetalle <= 0) {
            return;
        }
        Map<String, String> columnasDetalle = obtenerColumnas(conn, "detalle_Salida");
        String colDetalleId = resolverColumna(columnasDetalle, "idDetalleSalida", "id", "id_detalle_salida");
        String colClaveSalida = resolverColumna(columnasDetalle, "claveSalida", "idSalida", "id_salida", "salida_id");
        if (colDetalleId == null || colClaveSalida == null) {
            return;
        }

        Integer claveSalidaId = null;
        String sqlDetalle = "SELECT `" + colClaveSalida + "` AS claveSalida FROM detalle_Salida WHERE `"
                + colDetalleId + "` = ?";
        try (PreparedStatement ps = conn.prepareStatement(sqlDetalle)) {
            ps.setInt(1, linea.idDetalle);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    claveSalidaId = rs.getInt("claveSalida");
                }
            }
        }

        if (claveSalidaId == null || claveSalidaId <= 0) {
            return;
        }

        BigDecimal precioUnitarioAnterior = parseDecimal(linea.precioUnitario);
        BigDecimal precioIvaAnterior = parseDecimal(linea.precioIva);
        if (precioUnitarioAnterior == null || precioIvaAnterior == null || cantidad == null) {
            return;
        }

        BigDecimal deltaNeto = nuevoPrecioUnitario.subtract(precioUnitarioAnterior)
                .multiply(cantidad)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal deltaTotal = nuevoPrecioIva.subtract(precioIvaAnterior)
                .multiply(cantidad)
                .setScale(2, RoundingMode.HALF_UP);

        Map<String, String> columnasSalida = obtenerColumnas(conn, "salidas");
        String colSalidaId = resolverColumna(columnasSalida, "idSalida", "id", "id_salida");
        String colPrecioNeto = resolverColumna(columnasSalida, "precioNetoSalida", "precioNeto", "precio_neto");
        String colPrecioTotal = resolverColumna(columnasSalida, "precioTotalSalida", "precioTotal", "precio_total");
        if (colSalidaId == null || (colPrecioNeto == null && colPrecioTotal == null)) {
            return;
        }

        BigDecimal precioNetoActual = null;
        BigDecimal precioTotalActual = null;
        String sqlSalida = String.format("""
                SELECT %s AS precioNeto,
                       %s AS precioTotal
                FROM salidas
                WHERE `%s` = ?
                """,
                colPrecioNeto != null ? "`" + colPrecioNeto + "`" : "NULL",
                colPrecioTotal != null ? "`" + colPrecioTotal + "`" : "NULL",
                colSalidaId
        );
        try (PreparedStatement ps = conn.prepareStatement(sqlSalida)) {
            ps.setInt(1, claveSalidaId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    precioNetoActual = parseDecimal(rs.getObject("precioNeto"));
                    precioTotalActual = parseDecimal(rs.getObject("precioTotal"));
                }
            }
        }

        StringBuilder updateSalida = new StringBuilder("UPDATE salidas SET ");
        List<Object> valoresSalida = new ArrayList<>();
        if (colPrecioNeto != null && precioNetoActual != null) {
            BigDecimal nuevoNeto = precioNetoActual.add(deltaNeto);
            if (nuevoNeto.compareTo(BigDecimal.ZERO) < 0) {
                nuevoNeto = BigDecimal.ZERO;
            }
            agregarCampoActualizacion(updateSalida, valoresSalida, colPrecioNeto,
                    nuevoNeto.setScale(2, RoundingMode.HALF_UP));
        }
        if (colPrecioTotal != null && precioTotalActual != null) {
            BigDecimal nuevoTotal = precioTotalActual.add(deltaTotal);
            if (nuevoTotal.compareTo(BigDecimal.ZERO) < 0) {
                nuevoTotal = BigDecimal.ZERO;
            }
            agregarCampoActualizacion(updateSalida, valoresSalida, colPrecioTotal,
                    nuevoTotal.setScale(2, RoundingMode.HALF_UP));
        }
        if (valoresSalida.isEmpty()) {
            return;
        }
        updateSalida.append(" WHERE `").append(colSalidaId).append("` = ?");
        valoresSalida.add(claveSalidaId);

        try (PreparedStatement ps = conn.prepareStatement(updateSalida.toString())) {
            for (int i = 0; i < valoresSalida.size(); i++) {
                ps.setObject(i + 1, valoresSalida.get(i));
            }
            ps.executeUpdate();
        }
    }

    private void actualizarTotalesAjustePorPrecio(Connection conn, String tabla, DetalleLinea linea,
                                                  BigDecimal nuevoPrecioUnitario, BigDecimal nuevoPrecioIva,
                                                  BigDecimal cantidad, boolean esEntrada) throws SQLException {
        if (linea == null || linea.idDetalle <= 0) {
            return;
        }
        BigDecimal precioUnitarioAnterior = parseDecimal(linea.precioUnitario);
        BigDecimal precioIvaAnterior = parseDecimal(linea.precioIva);
        if (precioUnitarioAnterior == null || precioIvaAnterior == null || cantidad == null) {
            return;
        }

        BigDecimal deltaNeto = nuevoPrecioUnitario.subtract(precioUnitarioAnterior)
                .multiply(cantidad)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal deltaTotal = nuevoPrecioIva.subtract(precioIvaAnterior)
                .multiply(cantidad)
                .setScale(2, RoundingMode.HALF_UP);

        if (!esEntrada) {
            deltaNeto = deltaNeto.negate();
            deltaTotal = deltaTotal.negate();
        }

        Integer ajusteId = obtenerClaveMovimientoDetalle(conn, tabla, linea.idDetalle, esEntrada);
        actualizarTotalesAjuste(conn, ajusteId, deltaNeto, deltaTotal);
    }

    private Integer obtenerClaveMovimientoDetalle(Connection conn, String tabla, int detalleId, boolean esEntrada)
            throws SQLException {
        Map<String, String> columnasDetalle = obtenerColumnas(conn, tabla);
        String colDetalleId = resolverColumna(columnasDetalle,
                esEntrada ? "idDetalleEntrada" : "idDetalleSalida",
                "id", esEntrada ? "id_detalle_entrada" : "id_detalle_salida");
        String colClave = resolverColumna(columnasDetalle,
                esEntrada ? "claveEntrada" : "claveSalida",
                esEntrada ? "idEntrada" : "idSalida",
                esEntrada ? "id_entrada" : "id_salida",
                esEntrada ? "entrada_id" : "salida_id");
        if (colDetalleId == null || colClave == null) {
            return null;
        }
        String sql = "SELECT `" + colClave + "` AS claveMovimiento FROM " + tabla + " WHERE `" + colDetalleId + "` = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, detalleId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return parseInteger(rs.getObject("claveMovimiento"));
                }
            }
        }
        return null;
    }

    private void actualizarTotalesAjuste(Connection conn, Integer ajusteId,
                                         BigDecimal deltaNeto, BigDecimal deltaTotal) throws SQLException {
        if (ajusteId == null || ajusteId <= 0) {
            return;
        }
        if (deltaNeto == null && deltaTotal == null) {
            return;
        }
        Map<String, String> columnasAjuste = obtenerColumnas(conn, "ajuste_inventario");
        String colAjusteId = resolverColumna(columnasAjuste, "idAjuste", "id", "id_ajuste");
        String colPrecioNeto = resolverColumna(columnasAjuste, "precioNeto", "precio_neto");
        String colPrecioTotal = resolverColumna(columnasAjuste, "precioTotal", "precio_total");

        if (colAjusteId == null || (colPrecioNeto == null && colPrecioTotal == null)) {
            return;
        }

        BigDecimal precioNetoActual = null;
        BigDecimal precioTotalActual = null;
        String sql = String.format("""
                SELECT %s AS precioNeto,
                       %s AS precioTotal
                FROM ajuste_inventario
                WHERE `%s` = ?
                """,
                colPrecioNeto != null ? "`" + colPrecioNeto + "`" : "NULL",
                colPrecioTotal != null ? "`" + colPrecioTotal + "`" : "NULL",
                colAjusteId
        );
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, ajusteId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    precioNetoActual = parseDecimal(rs.getObject("precioNeto"));
                    precioTotalActual = parseDecimal(rs.getObject("precioTotal"));
                }
            }
        }

        StringBuilder updateAjuste = new StringBuilder("UPDATE ajuste_inventario SET ");
        List<Object> valores = new ArrayList<>();
        if (colPrecioNeto != null && deltaNeto != null) {
            BigDecimal baseNeto = precioNetoActual != null ? precioNetoActual : BigDecimal.ZERO;
            BigDecimal nuevoNeto = baseNeto.add(deltaNeto).setScale(2, RoundingMode.HALF_UP);
            agregarCampoActualizacion(updateAjuste, valores, colPrecioNeto, nuevoNeto);
        }
        if (colPrecioTotal != null && deltaTotal != null) {
            BigDecimal baseTotal = precioTotalActual != null ? precioTotalActual : BigDecimal.ZERO;
            BigDecimal nuevoTotal = baseTotal.add(deltaTotal).setScale(2, RoundingMode.HALF_UP);
            agregarCampoActualizacion(updateAjuste, valores, colPrecioTotal, nuevoTotal);
        }
        if (valores.isEmpty()) {
            return;
        }
        updateAjuste.append(" WHERE `").append(colAjusteId).append("` = ?");
        valores.add(ajusteId);

        try (PreparedStatement ps = conn.prepareStatement(updateAjuste.toString())) {
            for (int i = 0; i < valores.size(); i++) {
                ps.setObject(i + 1, valores.get(i));
            }
            ps.executeUpdate();
        }
    }

    private void notificarActualizacion() {
        if (onRefresh == null) {
            return;
        }
        Platform.runLater(onRefresh);
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

    private Integer ajustarTotalesSalida(Connection conn, DetalleArticulo articulo, boolean esAjuste) throws SQLException {
        if (articulo == null || articulo.detalleSalidaId == null) {
            return null;
        }
        Map<String, String> columnasDetalle = obtenerColumnas(conn, "detalle_Salida");
        String colDetalleId = resolverColumna(columnasDetalle, "idDetalleSalida", "id", "id_detalle_salida");
        String colClaveSalida = resolverColumna(columnasDetalle, "claveSalida", "idSalida", "id_salida", "salida_id");
        String colCantidad = resolverColumna(columnasDetalle, "cantidad", "cantidadSalida", "cantidad_salida");
        String colPrecioUnitario = resolverColumna(columnasDetalle, "precioUnitarioSalida", "precioUnitario",
                "precioSalida", "precio_salida", "precioSalidaUnitario");
        String colPrecioIva = resolverColumna(columnasDetalle, "precioIVASalida", "precioIVA", "precioIva", "precio_iva");
        String colPrecioBruto = resolverColumna(columnasDetalle, "precioBrutoTotalSalida", "precioBrutoTotal",
                "precio_bruto");
        String colPrecioTotal = resolverColumna(columnasDetalle, "precioTotalSalida", "precioTotal", "precio_total");

        if (colDetalleId == null || colClaveSalida == null || colCantidad == null
                || colPrecioUnitario == null || colPrecioIva == null) {
            return null;
        }

        String sql = String.format("""
                SELECT `%s` AS claveSalida,
                       `%s` AS cantidad,
                       `%s` AS precioUnitario,
                       `%s` AS precioIva,
                       %s AS precioBruto,
                       %s AS precioTotal
                FROM detalle_Salida
                WHERE `%s` = ?
                """,
                colClaveSalida,
                colCantidad,
                colPrecioUnitario,
                colPrecioIva,
                colPrecioBruto != null ? "`" + colPrecioBruto + "`" : "NULL",
                colPrecioTotal != null ? "`" + colPrecioTotal + "`" : "NULL",
                colDetalleId
        );

        Integer claveSalidaId = null;
        BigDecimal cantidad = null;
        BigDecimal precioUnitario = null;
        BigDecimal precioIva = null;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, articulo.detalleSalidaId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                claveSalidaId = rs.getInt("claveSalida");
                cantidad = parseDecimal(rs.getObject("cantidad"));
                precioUnitario = parseDecimal(rs.getObject("precioUnitario"));
                precioIva = parseDecimal(rs.getObject("precioIva"));
            }
        }

        if (cantidad == null || precioUnitario == null || precioIva == null) {
            return null;
        }

        BigDecimal nuevaCantidad = cantidad.subtract(BigDecimal.ONE);
        if (nuevaCantidad.compareTo(BigDecimal.ZERO) < 0) {
            nuevaCantidad = BigDecimal.ZERO;
        }
        BigDecimal nuevoBruto = precioUnitario.multiply(nuevaCantidad).setScale(2, RoundingMode.HALF_UP);
        BigDecimal nuevoTotal = precioIva.multiply(nuevaCantidad).setScale(2, RoundingMode.HALF_UP);

        StringBuilder updateDetalle = new StringBuilder("UPDATE detalle_Salida SET ");
        List<Object> valoresDetalle = new ArrayList<>();
        agregarCampoActualizacion(updateDetalle, valoresDetalle, colCantidad, nuevaCantidad.intValue());
        agregarCampoActualizacion(updateDetalle, valoresDetalle, colPrecioBruto, nuevoBruto);
        agregarCampoActualizacion(updateDetalle, valoresDetalle, colPrecioTotal, nuevoTotal);
        updateDetalle.append(" WHERE `").append(colDetalleId).append("` = ?");
        valoresDetalle.add(articulo.detalleSalidaId);

        try (PreparedStatement ps = conn.prepareStatement(updateDetalle.toString())) {
            for (int i = 0; i < valoresDetalle.size(); i++) {
                ps.setObject(i + 1, valoresDetalle.get(i));
            }
            ps.executeUpdate();
        }

        if (claveSalidaId == null || claveSalidaId <= 0) {
            return null;
        }

        if (esAjuste) {
            actualizarTotalesAjuste(conn, claveSalidaId, precioUnitario, precioIva);
            return claveSalidaId;
        }

        Map<String, String> columnasSalida = obtenerColumnas(conn, "salidas");
        String colSalidaId = resolverColumna(columnasSalida, "idSalida", "id", "id_salida");
        String colPrecioNeto = resolverColumna(columnasSalida, "precioNetoSalida", "precioNeto", "precio_neto");
        String colPrecioTotalSalida = resolverColumna(columnasSalida, "precioTotalSalida", "precioTotal", "precio_total");

        if (colSalidaId == null || (colPrecioNeto == null && colPrecioTotalSalida == null)) {
            return claveSalidaId;
        }

        BigDecimal precioNetoActual = null;
        BigDecimal precioTotalActual = null;
        String sqlSalida = String.format("""
                SELECT %s AS precioNeto,
                       %s AS precioTotal
                FROM salidas
                WHERE `%s` = ?
                """,
                colPrecioNeto != null ? "`" + colPrecioNeto + "`" : "NULL",
                colPrecioTotalSalida != null ? "`" + colPrecioTotalSalida + "`" : "NULL",
                colSalidaId
        );

        try (PreparedStatement ps = conn.prepareStatement(sqlSalida)) {
            ps.setInt(1, claveSalidaId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    precioNetoActual = parseDecimal(rs.getObject("precioNeto"));
                    precioTotalActual = parseDecimal(rs.getObject("precioTotal"));
                }
            }
        }

        StringBuilder updateSalida = new StringBuilder("UPDATE salidas SET ");
        List<Object> valoresSalida = new ArrayList<>();
        if (colPrecioNeto != null && precioNetoActual != null) {
            BigDecimal nuevoNeto = precioNetoActual.subtract(precioUnitario);
            if (nuevoNeto.compareTo(BigDecimal.ZERO) < 0) {
                nuevoNeto = BigDecimal.ZERO;
            }
            agregarCampoActualizacion(updateSalida, valoresSalida, colPrecioNeto,
                    nuevoNeto.setScale(2, RoundingMode.HALF_UP));
        }
        if (colPrecioTotalSalida != null && precioTotalActual != null) {
            BigDecimal nuevoTotalSalida = precioTotalActual.subtract(precioIva);
            if (nuevoTotalSalida.compareTo(BigDecimal.ZERO) < 0) {
                nuevoTotalSalida = BigDecimal.ZERO;
            }
            agregarCampoActualizacion(updateSalida, valoresSalida, colPrecioTotalSalida,
                    nuevoTotalSalida.setScale(2, RoundingMode.HALF_UP));
        }
        if (valoresSalida.isEmpty()) {
            return claveSalidaId;
        }
        updateSalida.append(" WHERE `").append(colSalidaId).append("` = ?");
        valoresSalida.add(claveSalidaId);

        try (PreparedStatement ps = conn.prepareStatement(updateSalida.toString())) {
            for (int i = 0; i < valoresSalida.size(); i++) {
                ps.setObject(i + 1, valoresSalida.get(i));
            }
            ps.executeUpdate();
        }
        return claveSalidaId;
    }

    private Integer ajustarTotalesEntrada(Connection conn, DetalleArticulo articulo, boolean esAjuste) throws SQLException {
        if (articulo == null || articulo.detalleEntradaId == null) {
            return null;
        }
        Map<String, String> columnasDetalle = obtenerColumnas(conn, "detalle_Entrada");
        String colDetalleId = resolverColumna(columnasDetalle, "idDetalleEntrada", "id", "id_detalle_entrada");
        String colClaveEntrada = resolverColumna(columnasDetalle, "claveEntrada", "idEntrada", "id_entrada", "entrada_id");
        String colCantidad = resolverColumna(columnasDetalle, "cantidad", "cantidadEntrada");
        String colPrecioUnitario = resolverColumna(columnasDetalle, "precioUnitario", "precioEntrada", "precio_entrada");
        String colPrecioIva = resolverColumna(columnasDetalle, "precioIVA", "precioIva", "precio_iva");
        String colPrecioBruto = resolverColumna(columnasDetalle, "precioBrutoTotal", "precioBruto", "precio_bruto");
        String colPrecioTotal = resolverColumna(columnasDetalle, "precioTotal", "precio_total");

        if (colDetalleId == null || colClaveEntrada == null || colCantidad == null
                || colPrecioUnitario == null || colPrecioIva == null) {
            return null;
        }

        String sql = String.format("""
                SELECT `%s` AS claveEntrada,
                       `%s` AS cantidad,
                       `%s` AS precioUnitario,
                       `%s` AS precioIva,
                       %s AS precioBruto,
                       %s AS precioTotal
                FROM detalle_Entrada
                WHERE `%s` = ?
                """,
                colClaveEntrada,
                colCantidad,
                colPrecioUnitario,
                colPrecioIva,
                colPrecioBruto != null ? "`" + colPrecioBruto + "`" : "NULL",
                colPrecioTotal != null ? "`" + colPrecioTotal + "`" : "NULL",
                colDetalleId
        );

        Integer claveEntradaId = null;
        BigDecimal cantidad = null;
        BigDecimal precioUnitario = null;
        BigDecimal precioIva = null;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, articulo.detalleEntradaId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                claveEntradaId = rs.getInt("claveEntrada");
                cantidad = parseDecimal(rs.getObject("cantidad"));
                precioUnitario = parseDecimal(rs.getObject("precioUnitario"));
                precioIva = parseDecimal(rs.getObject("precioIva"));
            }
        }

        if (cantidad == null || precioUnitario == null || precioIva == null) {
            return null;
        }

        BigDecimal nuevaCantidad = cantidad.subtract(BigDecimal.ONE);
        if (nuevaCantidad.compareTo(BigDecimal.ZERO) < 0) {
            nuevaCantidad = BigDecimal.ZERO;
        }
        BigDecimal nuevoBruto = precioUnitario.multiply(nuevaCantidad).setScale(2, RoundingMode.HALF_UP);
        BigDecimal nuevoTotal = precioIva.multiply(nuevaCantidad).setScale(2, RoundingMode.HALF_UP);

        StringBuilder updateDetalle = new StringBuilder("UPDATE detalle_Entrada SET ");
        List<Object> valoresDetalle = new ArrayList<>();
        agregarCampoActualizacion(updateDetalle, valoresDetalle, colCantidad, nuevaCantidad.intValue());
        agregarCampoActualizacion(updateDetalle, valoresDetalle, colPrecioBruto, nuevoBruto);
        agregarCampoActualizacion(updateDetalle, valoresDetalle, colPrecioTotal, nuevoTotal);
        updateDetalle.append(" WHERE `").append(colDetalleId).append("` = ?");
        valoresDetalle.add(articulo.detalleEntradaId);

        try (PreparedStatement ps = conn.prepareStatement(updateDetalle.toString())) {
            for (int i = 0; i < valoresDetalle.size(); i++) {
                ps.setObject(i + 1, valoresDetalle.get(i));
            }
            ps.executeUpdate();
        }

        if (claveEntradaId == null || claveEntradaId <= 0) {
            return null;
        }

        if (esAjuste) {
            actualizarTotalesAjuste(conn, claveEntradaId, precioUnitario.negate(), precioIva.negate());
            return claveEntradaId;
        }

        Map<String, String> columnasEntrada = obtenerColumnas(conn, "entradas");
        String colEntradaId = resolverColumna(columnasEntrada, "idEntrada", "id", "id_entrada");
        String colPrecioNeto = resolverColumna(columnasEntrada, "precioNetoEntrada", "precioNeto", "precio_neto");
        String colPrecioTotalEntrada = resolverColumna(columnasEntrada, "precioTotalEntrada", "precioTotal", "precio_total");

        if (colEntradaId == null || (colPrecioNeto == null && colPrecioTotalEntrada == null)) {
            return claveEntradaId;
        }

        BigDecimal precioNetoActual = null;
        BigDecimal precioTotalActual = null;
        String sqlEntrada = String.format("""
                SELECT %s AS precioNeto,
                       %s AS precioTotal
                FROM entradas
                WHERE `%s` = ?
                """,
                colPrecioNeto != null ? "`" + colPrecioNeto + "`" : "NULL",
                colPrecioTotalEntrada != null ? "`" + colPrecioTotalEntrada + "`" : "NULL",
                colEntradaId
        );
        try (PreparedStatement ps = conn.prepareStatement(sqlEntrada)) {
            ps.setInt(1, claveEntradaId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    precioNetoActual = parseDecimal(rs.getObject("precioNeto"));
                    precioTotalActual = parseDecimal(rs.getObject("precioTotal"));
                }
            }
        }

        StringBuilder updateEntrada = new StringBuilder("UPDATE entradas SET ");
        List<Object> valoresEntrada = new ArrayList<>();
        if (colPrecioNeto != null && precioNetoActual != null) {
            BigDecimal nuevoNeto = precioNetoActual.subtract(precioUnitario);
            if (nuevoNeto.compareTo(BigDecimal.ZERO) < 0) {
                nuevoNeto = BigDecimal.ZERO;
            }
            agregarCampoActualizacion(updateEntrada, valoresEntrada, colPrecioNeto,
                    nuevoNeto.setScale(2, RoundingMode.HALF_UP));
        }
        if (colPrecioTotalEntrada != null && precioTotalActual != null) {
            BigDecimal nuevoTotalEntrada = precioTotalActual.subtract(precioIva);
            if (nuevoTotalEntrada.compareTo(BigDecimal.ZERO) < 0) {
                nuevoTotalEntrada = BigDecimal.ZERO;
            }
            agregarCampoActualizacion(updateEntrada, valoresEntrada, colPrecioTotalEntrada,
                    nuevoTotalEntrada.setScale(2, RoundingMode.HALF_UP));
        }
        if (valoresEntrada.isEmpty()) {
            return claveEntradaId;
        }
        updateEntrada.append(" WHERE `").append(colEntradaId).append("` = ?");
        valoresEntrada.add(claveEntradaId);

        try (PreparedStatement ps = conn.prepareStatement(updateEntrada.toString())) {
            for (int i = 0; i < valoresEntrada.size(); i++) {
                ps.setObject(i + 1, valoresEntrada.get(i));
            }
            ps.executeUpdate();
        }
        return claveEntradaId;
    }

    private void actualizarEstadoDetalleSalidaSiVacio(Connection conn, Integer detalleSalidaId,
                                                      boolean actualizarSalida) throws SQLException {
        if (detalleSalidaId == null || detalleSalidaId <= 0) {
            return;
        }
        Map<String, String> columnasDetalle = obtenerColumnas(conn, "detalle_Salida");
        Map<String, String> columnasArticulo = obtenerColumnas(conn, "articulo");

        String colDetalleId = resolverColumna(columnasDetalle, "idDetalleSalida", "id", "id_detalle_salida");
        String colDetalleEstado = resolverColumna(columnasDetalle, "estado", "Estado");
        String colArticuloDetalleSalida = resolverColumna(columnasArticulo, "idDetalleSalida", "id_detalle_salida",
                "detalleSalida", "detalle_salida", "detalle_salida_id");

        if (colDetalleId == null || colDetalleEstado == null || colArticuloDetalleSalida == null) {
            return;
        }

        String sqlConteo = "SELECT COUNT(*) FROM articulo WHERE `" + colArticuloDetalleSalida + "` = ?";
        int total = 0;
        try (PreparedStatement ps = conn.prepareStatement(sqlConteo)) {
            ps.setInt(1, detalleSalidaId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    total = rs.getInt(1);
                }
            }
        }

        if (total > 0) {
            return;
        }

        String sqlUpdate = "UPDATE detalle_Salida SET `" + colDetalleEstado + "` = ? WHERE `" + colDetalleId + "` = ?";
        try (PreparedStatement ps = conn.prepareStatement(sqlUpdate)) {
            ps.setString(1, "desactivado");
            ps.setInt(2, detalleSalidaId);
            ps.executeUpdate();
        }

        if (!actualizarSalida) {
            return;
        }

        String colClaveSalida = resolverColumna(columnasDetalle, "claveSalida", "idSalida", "id_salida", "salida_id");
        if (colClaveSalida == null) {
            return;
        }

        Integer salidaId = null;
        String sqlSalida = "SELECT `" + colClaveSalida + "` AS claveSalida FROM detalle_Salida WHERE `" + colDetalleId + "` = ?";
        try (PreparedStatement ps = conn.prepareStatement(sqlSalida)) {
            ps.setInt(1, detalleSalidaId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    salidaId = rs.getInt("claveSalida");
                }
            }
        }

        if (salidaId == null || salidaId <= 0) {
            return;
        }

        String sqlConteoDetalles = "SELECT COUNT(*) FROM detalle_Salida WHERE `" + colClaveSalida + "` = ? "
                + "AND LOWER(`" + colDetalleEstado + "`) = 'activo'";
        int detallesActivos = 0;
        try (PreparedStatement ps = conn.prepareStatement(sqlConteoDetalles)) {
            ps.setInt(1, salidaId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    detallesActivos = rs.getInt(1);
                }
            }
        }

        if (detallesActivos > 0) {
            return;
        }

        Map<String, String> columnasSalida = obtenerColumnas(conn, "salidas");
        String colSalidaId = resolverColumna(columnasSalida, "idSalida", "id", "id_salida");
        String colSalidaEstado = resolverColumna(columnasSalida, "Estado", "estado");
        if (colSalidaId == null || colSalidaEstado == null) {
            return;
        }

        String sqlCancelarSalida = "UPDATE salidas SET `" + colSalidaEstado + "` = ? WHERE `" + colSalidaId + "` = ?";
        try (PreparedStatement ps = conn.prepareStatement(sqlCancelarSalida)) {
            ps.setString(1, "cancelado");
            ps.setInt(2, salidaId);
            ps.executeUpdate();
        }
    }

    private void actualizarEstadoDetalleEntradaSiVacio(Connection conn, Integer detalleEntradaId,
                                                       boolean actualizarEntrada) throws SQLException {
        if (detalleEntradaId == null || detalleEntradaId <= 0) {
            return;
        }
        Map<String, String> columnasDetalle = obtenerColumnas(conn, "detalle_Entrada");
        Map<String, String> columnasArticulo = obtenerColumnas(conn, "articulo");

        String colDetalleId = resolverColumna(columnasDetalle, "idDetalleEntrada", "id", "id_detalle_entrada");
        String colDetalleEstado = resolverColumna(columnasDetalle, "estado", "Estado");
        String colDetalleClaveEntrada = resolverColumna(columnasDetalle, "claveEntrada", "idEntrada", "id_entrada",
                "entrada_id");
        String colArticuloDetalleEntrada = resolverColumna(columnasArticulo, "idDetalleEntrada", "id_detalle_entrada",
                "detalleEntrada", "detalle_entrada", "detalle_entrada_id");
        String colArticuloEstado = resolverColumna(columnasArticulo, "Estado", "estado");

        if (colDetalleId == null || colDetalleEstado == null || colArticuloDetalleEntrada == null
                || colDetalleClaveEntrada == null) {
            return;
        }

        String sqlConteo = "SELECT COUNT(*) FROM articulo WHERE `" + colArticuloDetalleEntrada + "` = ?"
                + (colArticuloEstado != null ? " AND LOWER(`" + colArticuloEstado + "`) <> 'eliminado'" : "");
        int total = 0;
        try (PreparedStatement ps = conn.prepareStatement(sqlConteo)) {
            ps.setInt(1, detalleEntradaId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    total = rs.getInt(1);
                }
            }
        }

        if (total > 0) {
            return;
        }

        String sqlUpdate = "UPDATE detalle_Entrada SET `" + colDetalleEstado + "` = ? WHERE `" + colDetalleId + "` = ?";
        try (PreparedStatement ps = conn.prepareStatement(sqlUpdate)) {
            ps.setString(1, "desactivado");
            ps.setInt(2, detalleEntradaId);
            ps.executeUpdate();
        }

        if (!actualizarEntrada) {
            return;
        }

        Integer entradaId = null;
        String sqlEntrada = "SELECT `" + colDetalleClaveEntrada + "` AS claveEntrada FROM detalle_Entrada WHERE `"
                + colDetalleId + "` = ?";
        try (PreparedStatement ps = conn.prepareStatement(sqlEntrada)) {
            ps.setInt(1, detalleEntradaId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    entradaId = rs.getInt("claveEntrada");
                }
            }
        }

        if (entradaId == null || entradaId <= 0) {
            return;
        }

        String sqlConteoDetalles = "SELECT COUNT(*) FROM detalle_Entrada WHERE `" + colDetalleClaveEntrada + "` = ? "
                + "AND LOWER(`" + colDetalleEstado + "`) = 'activo'";
        int detallesActivos = 0;
        try (PreparedStatement ps = conn.prepareStatement(sqlConteoDetalles)) {
            ps.setInt(1, entradaId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    detallesActivos = rs.getInt(1);
                }
            }
        }

        if (detallesActivos > 0) {
            return;
        }

        Map<String, String> columnasEntrada = obtenerColumnas(conn, "entradas");
        String colEntradaId = resolverColumna(columnasEntrada, "idEntrada", "id", "id_entrada");
        String colEntradaEstado = resolverColumna(columnasEntrada, "Estado", "estado");
        if (colEntradaId == null || colEntradaEstado == null) {
            return;
        }

        String sqlCancelarEntrada = "UPDATE entradas SET `" + colEntradaEstado + "` = ? WHERE `" + colEntradaId + "` = ?";
        try (PreparedStatement ps = conn.prepareStatement(sqlCancelarEntrada)) {
            ps.setString(1, "cancelado");
            ps.setInt(2, entradaId);
            ps.executeUpdate();
        }
    }

    private void actualizarEstadoAjusteSiVacio(Connection conn, Integer ajusteId) throws SQLException {
        if (ajusteId == null || ajusteId <= 0) {
            return;
        }
        Map<String, String> columnasDetalleEntrada = obtenerColumnas(conn, "detalle_Entrada");
        Map<String, String> columnasDetalleSalida = obtenerColumnas(conn, "detalle_Salida");
        Map<String, String> columnasAjuste = obtenerColumnas(conn, "ajuste_inventario");

        String colClaveEntrada = resolverColumna(columnasDetalleEntrada, "claveEntrada", "idEntrada", "id_entrada",
                "entrada_id");
        String colEstadoEntrada = resolverColumna(columnasDetalleEntrada, "estado", "Estado");
        String colClaveSalida = resolverColumna(columnasDetalleSalida, "claveSalida", "idSalida", "id_salida",
                "salida_id");
        String colEstadoSalida = resolverColumna(columnasDetalleSalida, "estado", "Estado");

        int activosEntrada = contarDetallesActivos(conn, "detalle_Entrada", colClaveEntrada, colEstadoEntrada, ajusteId);
        int activosSalida = contarDetallesActivos(conn, "detalle_Salida", colClaveSalida, colEstadoSalida, ajusteId);
        if (activosEntrada > 0 || activosSalida > 0) {
            return;
        }

        String colAjusteId = resolverColumna(columnasAjuste, "idAjuste", "id", "id_ajuste");
        String colAjusteEstado = resolverColumna(columnasAjuste, "estado", "Estado");
        if (colAjusteId == null || colAjusteEstado == null) {
            return;
        }
        String sqlCancelar = "UPDATE ajuste_inventario SET `" + colAjusteEstado + "` = ? WHERE `" + colAjusteId + "` = ?";
        try (PreparedStatement ps = conn.prepareStatement(sqlCancelar)) {
            ps.setString(1, "cancelado");
            ps.setInt(2, ajusteId);
            ps.executeUpdate();
        }
    }

    private int contarDetallesActivos(Connection conn, String tabla, String colClave, String colEstado, int ajusteId)
            throws SQLException {
        if (colClave == null) {
            return Integer.MAX_VALUE;
        }
        String filtroEstado = colEstado != null ? " AND LOWER(`" + colEstado + "`) = 'activo'" : "";
        String sql = "SELECT COUNT(*) FROM " + tabla + " WHERE `" + colClave + "` = ?" + filtroEstado;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, ajusteId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return Integer.MAX_VALUE;
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

    private BigDecimal parseDecimal(Object valor) {
        if (valor == null) {
            return null;
        }
        if (valor instanceof BigDecimal) {
            return (BigDecimal) valor;
        }
        if (valor instanceof Number) {
            return new BigDecimal(valor.toString());
        }
        return parseDecimal(valor.toString());
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

    private Integer parseInteger(Object valor) {
        if (valor == null) {
            return null;
        }
        if (valor instanceof Number) {
            return ((Number) valor).intValue();
        }
        return parseInteger(valor.toString());
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

    private boolean esSalidaCancelada(Connection conn, Integer salidaId) throws SQLException {
        if (salidaId == null || salidaId <= 0) {
            return false;
        }

        String sql = "SELECT `Estado` FROM salidas WHERE `idSalida` = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, salidaId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String estado = rs.getString("estado");
                    return estado != null && estado.equalsIgnoreCase("cancelado");
                }
            }
        }
        return false;
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

        private boolean tieneArticulosSinPendienteOVendido() {
            if (articulos.isEmpty()) {
                return false;
            }
            for (DetalleArticulo articulo : articulos) {
                if (articulo.esPendiente() || articulo.esVendido()) {
                    return false;
                }
                if (!articulo.esDisponible() && !articulo.esAjustado()) {
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

        private boolean esPendiente() {
            if (estado == null) {
                return false;
            }
            return "pendiente".equalsIgnoreCase(estado.trim());
        }

        private boolean esVendido() {
            if (estado == null) {
                return false;
            }
            return "vendido".equalsIgnoreCase(estado.trim());
        }

        private boolean esAjustado() {
            if (estado == null) {
                return false;
            }
            return "ajustado".equalsIgnoreCase(estado.trim());
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
