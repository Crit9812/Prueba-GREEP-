package Reportes.historial.controller;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.geometry.Pos;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import Reportes.historial.model.HistorialFactura;
import Compartido.helper.OverlayCarga;
import conexion.Conexion;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DetalleFacturaController {

    @FXML private StackPane root;
    @FXML private Pane overlayPane;
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
    private OverlayCarga overlayCarga;
    private OverlayCarga overlayCargaGlobal;

    @FXML
    public void initialize() {
        if (chkDetallado != null) {
            chkDetallado.selectedProperty().addListener((obs, oldVal, newVal) -> cargarDetalles());
        }
        actualizarTitulo();
        actualizarBotonCancelar();
        if (root != null && overlayPane != null) {
            overlayCarga = new OverlayCarga(root, overlayPane);
        }

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

    public void setOverlayCargaGlobal(OverlayCarga overlayCargaGlobal) {
        this.overlayCargaGlobal = overlayCargaGlobal;
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
            setProcesandoCancelacion(true);
            mostrarCargandoCancelacion();
            cerrarVentana();
            Platform.runLater(() -> {
                try (Connection conn = new Conexion().conectar()) {
                    if (conn == null) {
                        return;
                    }
                    conn.setAutoCommit(false);

                try {
                    if (!puedeCancelarSalida(conn, salidaId)) {
                        conn.rollback();
                        return;
                    }

                    Map<String, String> columnasDetalle = obtenerColumnas(conn, "detalle_Salida");
                    Map<String, String> columnasArticulo = obtenerColumnas(conn, "articulo");
                    Map<String, String> columnasDetalleArticulo = obtenerColumnas(conn, "detalleArticulo");
                    Map<String, String> columnasDetalleEntrada = obtenerColumnas(conn, "detalle_Entrada");
                    Map<String, String> columnasEntrada = obtenerColumnas(conn, "entradas");
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

                    String colArticuloId = resolverColumna(columnasArticulo, "idArticulo", "id", "id_articulo");
                    String colArticuloDetalleSalida = resolverColumna(columnasArticulo, "idDetalleSalida", "id_detalle_salida",
                            "detalleSalida", "detalle_salida", "detalle_salida_id");
                    String colArticuloDetalleEntrada = resolverColumna(columnasArticulo, "idDetalleEntrada", "id_detalle_entrada",
                            "detalleEntrada", "detalle_entrada", "detalle_entrada_id");
                    String colArticuloEstado = resolverColumna(columnasArticulo, "Estado", "estado");
                    String colDetalleArticuloArticulo = resolverColumna(columnasDetalleArticulo, "idArticulo", "id_articulo",
                            "articulo_id");
                    String colDetalleArticuloSalida = resolverColumna(columnasDetalleArticulo, "idDetalleSalida",
                            "id_detalle_salida", "detalleSalida", "detalle_salida", "detalle_salida_id");
                    String colDetalleArticuloEstado = resolverColumna(columnasDetalleArticulo, "estado", "Estado");
                    String colDetalleEntradaId = resolverColumna(columnasDetalleEntrada, "idDetalleEntrada", "id",
                            "id_detalle_entrada");
                    String colDetalleEntradaClaveEntrada = resolverColumna(columnasDetalleEntrada, "claveEntrada", "idEntrada",
                            "id_entrada", "entrada_id");
                    String colDetalleEntradaEstado = resolverColumna(columnasDetalleEntrada, "estado", "Estado");
                    String colEntradaId = resolverColumna(columnasEntrada, "idEntrada", "id", "id_entrada");
                    String colEntradaEstado = resolverColumna(columnasEntrada, "Estado", "estado");
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

                    Set<Integer> detalleEntradaIds = new HashSet<>();
                    if (!detallesSalida.isEmpty() && colArticuloDetalleEntrada != null && colArticuloDetalleSalida != null) {
                        String sqlDetalleEntrada = "SELECT DISTINCT `" + colArticuloDetalleEntrada
                                + "` AS idDetalleEntrada FROM articulo WHERE `" + colArticuloDetalleSalida + "` IN ("
                                + placeholders(detallesSalida.size()) + ")";
                        try (PreparedStatement ps = conn.prepareStatement(sqlDetalleEntrada)) {
                            int index = 1;
                            for (Integer detalleId : detallesSalida) {
                                ps.setInt(index++, detalleId);
                            }
                            try (ResultSet rs = ps.executeQuery()) {
                                while (rs.next()) {
                                    detalleEntradaIds.add(rs.getInt("idDetalleEntrada"));
                                }
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

                    if (colDetalleArticuloSalida != null && colDetalleArticuloEstado != null && !detallesSalida.isEmpty()) {
                        String sqlActualizarDetalleArticulo = "UPDATE detalleArticulo SET `" + colDetalleArticuloEstado
                                + "` = ?, `" + colDetalleArticuloSalida + "` = NULL WHERE `" + colDetalleArticuloSalida + "` = ?";
                        try (PreparedStatement ps = conn.prepareStatement(sqlActualizarDetalleArticulo)) {
                            for (Integer detalleId : detallesSalida) {
                                ps.setString(1, "disponible");
                                ps.setInt(2, detalleId);
                                ps.addBatch();
                            }
                            ps.executeBatch();
                        }
                    }

                    if (!detallesSalida.isEmpty() && colDetalleArticuloSalida != null && colDetalleArticuloArticulo != null
                            && colArticuloId != null && colArticuloDetalleEntrada != null) {
                        String sqlDetalleEntrada = "SELECT DISTINCT a.`" + colArticuloDetalleEntrada
                                + "` AS idDetalleEntrada FROM detalleArticulo da JOIN articulo a ON a.`" + colArticuloId
                                + "` = da.`" + colDetalleArticuloArticulo + "` WHERE da.`" + colDetalleArticuloSalida + "` IN ("
                                + placeholders(detallesSalida.size()) + ")";
                        try (PreparedStatement ps = conn.prepareStatement(sqlDetalleEntrada)) {
                            int index = 1;
                            for (Integer detalleId : detallesSalida) {
                                ps.setInt(index++, detalleId);
                            }
                            try (ResultSet rs = ps.executeQuery()) {
                                while (rs.next()) {
                                    detalleEntradaIds.add(rs.getInt("idDetalleEntrada"));
                                }
                            }
                        }
                    }

                    if (!detalleEntradaIds.isEmpty() && colDetalleEntradaId != null && colDetalleEntradaEstado != null) {
                        String sqlActualizarDetalleEntrada = "UPDATE detalle_Entrada SET `" + colDetalleEntradaEstado
                                + "` = ? WHERE `" + colDetalleEntradaId + "` IN (" + placeholders(detalleEntradaIds.size()) + ")";
                        try (PreparedStatement ps = conn.prepareStatement(sqlActualizarDetalleEntrada)) {
                            int index = 1;
                            ps.setString(index++, "activo");
                            for (Integer detalleId : detalleEntradaIds) {
                                ps.setInt(index++, detalleId);
                            }
                            ps.executeUpdate();
                        }
                    }

                    if (!detalleEntradaIds.isEmpty() && colDetalleEntradaId != null && colDetalleEntradaClaveEntrada != null
                            && colEntradaId != null && colEntradaEstado != null) {
                        Set<Integer> entradasIds = new HashSet<>();
                        String sqlClaveEntrada = "SELECT DISTINCT `" + colDetalleEntradaClaveEntrada
                                + "` AS claveEntrada FROM detalle_Entrada WHERE `" + colDetalleEntradaId + "` IN ("
                                + placeholders(detalleEntradaIds.size()) + ")";
                        try (PreparedStatement ps = conn.prepareStatement(sqlClaveEntrada)) {
                            int index = 1;
                            for (Integer detalleId : detalleEntradaIds) {
                                ps.setInt(index++, detalleId);
                            }
                            try (ResultSet rs = ps.executeQuery()) {
                                while (rs.next()) {
                                    Integer entradaId = parseInteger(rs.getObject("claveEntrada"));
                                    if (entradaId != null && entradaId > 0) {
                                        entradasIds.add(entradaId);
                                    }
                                }
                            }
                        }

                        if (!entradasIds.isEmpty()) {
                            String sqlActualizarEntrada = "UPDATE entradas SET `" + colEntradaEstado + "` = ? WHERE `"
                                    + colEntradaId + "` IN (" + placeholders(entradasIds.size()) + ")";
                            try (PreparedStatement ps = conn.prepareStatement(sqlActualizarEntrada)) {
                                int index = 1;
                                ps.setString(index++, "disponible");
                                for (Integer entradaId : entradasIds) {
                                    ps.setInt(index++, entradaId);
                                }
                                ps.executeUpdate();
                            }
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
                    finalizarCancelacionConExito("La cancelación de la salida se realizó correctamente.");
                } catch (SQLException e) {
                    conn.rollback();
                    throw e;
                }
                } catch (SQLException e) {
                    e.printStackTrace();
                } finally {
                    finalizarCancelacionSinMensaje();
                }
            });
        });
    }

    @FXML
    private void cancelarMovimiento() {
        if (historial == null) {
            return;
        }
        String movimiento = historial.getMovimiento();
        if ("Salida".equalsIgnoreCase(movimiento)) {
            cancelarSalida();
        } else if ("Ajuste".equalsIgnoreCase(movimiento)) {
            cancelarAjuste();
        }
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

            setProcesandoCancelacion(true);
            mostrarCargandoCancelacion();
            cerrarVentana();

            Task<Boolean> taskCancelacion = new Task<>() {
                @Override
                protected Boolean call() throws Exception {
                    return ejecutarCancelacionEntrada(entradaId);
                }
            };

            taskCancelacion.setOnSucceeded(event -> {
                try {
                    Boolean cancelado = taskCancelacion.getValue();
                    if (Boolean.TRUE.equals(cancelado)) {
                        finalizarCancelacionConExito("La cancelación de la entrada se realizó correctamente.");
                        return;
                    }
                } finally {
                    finalizarCancelacionSinMensaje();
                }
            });

            taskCancelacion.setOnFailed(event -> {
                Throwable error = taskCancelacion.getException();
                if (error != null) {
                    error.printStackTrace();
                }
                ocultarCargandoCancelacion();
                setProcesandoCancelacion(false);
            });

            Thread hiloCancelacion = new Thread(taskCancelacion, "cancelar-entrada-historial");
            hiloCancelacion.setDaemon(true);
            hiloCancelacion.start();
        });
    }

    private boolean ejecutarCancelacionEntrada(Integer entradaId) throws SQLException {
        try (Connection conn = new Conexion().conectar()) {
            if (conn == null) {
                return false;
            }
            conn.setAutoCommit(false);

            try {
                if (!puedeCancelarEntrada(conn, entradaId)) {
                    conn.rollback();
                    return false;
                }

                Map<String, String> columnasDetalle = obtenerColumnas(conn, "detalle_Entrada");
                Map<String, String> columnasArticulo = obtenerColumnas(conn, "articulo");
                Map<String, String> columnasDetalleArticulo = obtenerColumnas(conn, "detalleArticulo");
                Map<String, String> columnasEntrada = obtenerColumnas(conn, "entradas");

                String colDetalleId = resolverColumna(columnasDetalle, "idDetalleEntrada", "id", "id_detalle_entrada");
                String colDetalleClaveEntrada = resolverColumna(columnasDetalle, "claveEntrada", "idEntrada", "id_entrada",
                        "entrada_id");
                String colDetalleCantidad = resolverColumna(columnasDetalle, "cantidad", "cantidadEntrada");
                String colDetallePrecioBruto = resolverColumna(columnasDetalle, "precioBrutoTotal", "precioBruto",
                        "precio_bruto");
                String colDetallePrecioTotal = resolverColumna(columnasDetalle, "precioTotal", "precio_total");
                String colDetalleEstado = resolverColumna(columnasDetalle, "estado", "Estado");

                String colArticuloId = resolverColumna(columnasArticulo, "idArticulo", "id", "id_articulo");
                String colArticuloDetalleEntrada = resolverColumna(columnasArticulo, "idDetalleEntrada", "id_detalle_entrada",
                        "detalleEntrada", "detalle_entrada", "detalle_entrada_id");
                String colArticuloEstado = resolverColumna(columnasArticulo, "Estado", "estado");
                String colDetalleArticuloArticulo = resolverColumna(columnasDetalleArticulo, "idArticulo", "id_articulo",
                        "articulo_id");
                String colDetalleArticuloEstado = resolverColumna(columnasDetalleArticulo, "estado", "Estado");

                String colEntradaId = resolverColumna(columnasEntrada, "idEntrada", "id", "id_entrada");
                String colEntradaPrecioNeto = resolverColumna(columnasEntrada, "precioNetoEntrada", "precioNeto", "precio_neto");
                String colEntradaPrecioTotal = resolverColumna(columnasEntrada, "precioTotalEntrada", "precioTotal",
                        "precio_total");
                String colEntradaEstado = resolverColumna(columnasEntrada, "Estado", "estado");

                if (colDetalleId == null || colDetalleClaveEntrada == null || colEntradaId == null || colEntradaEstado == null) {
                    conn.rollback();
                    return false;
                }

                if (colArticuloDetalleEntrada == null || colArticuloEstado == null) {
                    conn.rollback();
                    return false;
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

                if (!detallesEntrada.isEmpty() && colDetalleArticuloArticulo != null && colDetalleArticuloEstado != null
                        && colArticuloId != null) {
                    String sqlActualizarDetalleArticulo = "UPDATE detalleArticulo da JOIN articulo a ON a.`" + colArticuloId
                            + "` = da.`" + colDetalleArticuloArticulo + "` SET da.`" + colDetalleArticuloEstado + "` = ? "
                            + "WHERE a.`" + colArticuloDetalleEntrada + "` = ?";
                    try (PreparedStatement ps = conn.prepareStatement(sqlActualizarDetalleArticulo)) {
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
                return true;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    @FXML
    private void cancelarAjuste() {
        if (historial == null || !"Ajuste".equalsIgnoreCase(historial.getMovimiento())) {
            return;
        }
        String ajusteId = historial.getClaveMovimiento();
        if (ajusteId == null || ajusteId.isBlank()) {
            return;
        }
        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Cancelar ajuste");
        confirmacion.setHeaderText(null);
        confirmacion.setContentText("¿Deseas cancelar el ajuste seleccionado?");
        confirmacion.showAndWait().ifPresent(respuesta -> {
            if (respuesta != ButtonType.OK) {
                return;
            }
            setProcesandoCancelacion(true);
            mostrarCargandoCancelacion();
            cerrarVentana();
            Platform.runLater(() -> {
                try (Connection conn = new Conexion().conectar()) {
                    if (conn == null) {
                        return;
                    }
                    conn.setAutoCommit(false);

                try {
                    if (!puedeCancelarAjuste(conn, ajusteId)) {
                        conn.rollback();
                        return;
                    }

                    Map<String, String> columnasDetalleEntrada = obtenerColumnas(conn, "detalle_Entrada");
                    Map<String, String> columnasDetalleSalida = obtenerColumnas(conn, "detalle_Salida");
                    Map<String, String> columnasArticulo = obtenerColumnas(conn, "articulo");
                    Map<String, String> columnasDetalleArticulo = obtenerColumnas(conn, "detalleArticulo");
                    Map<String, String> columnasAjuste = obtenerColumnas(conn, "ajuste_inventario");
                    Map<String, String> columnasEntrada = obtenerColumnas(conn, "entradas");

                    String colDetalleEntradaId = resolverColumna(columnasDetalleEntrada, "idDetalleEntrada", "id",
                            "id_detalle_entrada");
                    String colDetalleEntradaClave = resolverColumna(columnasDetalleEntrada, "claveEntrada", "idEntrada",
                            "id_entrada", "entrada_id");
                    String colDetalleEntradaCantidad = resolverColumna(columnasDetalleEntrada, "cantidad", "cantidadEntrada");
                    String colDetalleEntradaPrecioBruto = resolverColumna(columnasDetalleEntrada, "precioBrutoTotal", "precioBruto",
                            "precio_bruto");
                    String colDetalleEntradaPrecioTotal = resolverColumna(columnasDetalleEntrada, "precioTotal", "precio_total");
                    String colDetalleEntradaEstado = resolverColumna(columnasDetalleEntrada, "estado", "Estado");

                    String colDetalleSalidaId = resolverColumna(columnasDetalleSalida, "idDetalleSalida", "id",
                            "id_detalle_salida");
                    String colDetalleSalidaClave = resolverColumna(columnasDetalleSalida, "claveSalida", "idSalida", "id_salida",
                            "salida_id");
                    String colDetalleSalidaCantidad = resolverColumna(columnasDetalleSalida, "cantidad", "cantidadSalida",
                            "cantidad_salida");
                    String colDetalleSalidaPrecioBruto = resolverColumna(columnasDetalleSalida, "precioBrutoTotalSalida",
                            "precioBrutoTotal", "precio_bruto");
                    String colDetalleSalidaPrecioTotal = resolverColumna(columnasDetalleSalida, "precioTotalSalida", "precioTotal",
                            "precio_total");
                    String colDetalleSalidaEstado = resolverColumna(columnasDetalleSalida, "estado", "Estado");

                    String colArticuloId = resolverColumna(columnasArticulo, "idArticulo", "id", "id_articulo");
                    String colArticuloDetalleEntrada = resolverColumna(columnasArticulo, "idDetalleEntrada", "id_detalle_entrada",
                            "detalleEntrada", "detalle_entrada", "detalle_entrada_id");
                    String colArticuloDetalleSalida = resolverColumna(columnasArticulo, "idDetalleSalida", "id_detalle_salida",
                            "detalleSalida", "detalle_salida", "detalle_salida_id");
                    String colArticuloEstado = resolverColumna(columnasArticulo, "Estado", "estado");

                    String colDetalleArticuloArticulo = resolverColumna(columnasDetalleArticulo, "idArticulo", "id_articulo",
                            "articulo_id");
                    String colDetalleArticuloSalida = resolverColumna(columnasDetalleArticulo, "idDetalleSalida",
                            "id_detalle_salida", "detalleSalida", "detalle_salida", "detalle_salida_id");
                    String colDetalleArticuloEstado = resolverColumna(columnasDetalleArticulo, "estado", "Estado");

                    String colAjusteId = resolverColumna(columnasAjuste, "idAjuste", "id", "id_ajuste");
                    String colAjusteEstado = resolverColumna(columnasAjuste, "estado", "Estado");
                    String colAjustePrecioNeto = resolverColumna(columnasAjuste, "precioNeto", "precio_neto");
                    String colAjustePrecioTotal = resolverColumna(columnasAjuste, "precioTotal", "precio_total");

                    String colEntradaId = resolverColumna(columnasEntrada, "idEntrada", "id", "id_entrada");
                    String colEntradaEstado = resolverColumna(columnasEntrada, "Estado", "estado");

                    if (colDetalleEntradaId == null || colDetalleEntradaClave == null || colDetalleSalidaId == null
                            || colDetalleSalidaClave == null || colAjusteId == null || colAjusteEstado == null) {
                        conn.rollback();
                        return;
                    }

                    List<Integer> detallesEntrada = new ArrayList<>();
                    String sqlDetallesEntrada = "SELECT `" + colDetalleEntradaId + "` AS idDetalle FROM detalle_Entrada WHERE `"
                            + colDetalleEntradaClave + "` = ?";
                    try (PreparedStatement ps = conn.prepareStatement(sqlDetallesEntrada)) {
                        ps.setString(1, ajusteId);
                        try (ResultSet rs = ps.executeQuery()) {
                            while (rs.next()) {
                                detallesEntrada.add(rs.getInt("idDetalle"));
                            }
                        }
                    }

                    if (!detallesEntrada.isEmpty() && colArticuloDetalleEntrada != null && colArticuloEstado != null) {
                        String sqlActualizarArticulo = "UPDATE articulo SET `" + colArticuloEstado + "` = ? WHERE `"
                                + colArticuloDetalleEntrada + "` = ?";
                        try (PreparedStatement ps = conn.prepareStatement(sqlActualizarArticulo)) {
                            for (Integer detalleId : detallesEntrada) {
                                ps.setString(1, "eliminado");
                                ps.setInt(2, detalleId);
                                ps.addBatch();
                            }
                            ps.executeBatch();
                        }
                    }

                    if (!detallesEntrada.isEmpty() && colDetalleArticuloArticulo != null && colDetalleArticuloEstado != null
                            && colArticuloId != null) {
                        String sqlActualizarDetalleArticulo = "UPDATE detalleArticulo da JOIN articulo a ON a.`" + colArticuloId
                                + "` = da.`" + colDetalleArticuloArticulo + "` SET da.`" + colDetalleArticuloEstado + "` = ? "
                                + "WHERE a.`" + colArticuloDetalleEntrada + "` = ?";
                        try (PreparedStatement ps = conn.prepareStatement(sqlActualizarDetalleArticulo)) {
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
                        agregarCampoActualizacion(updateDetalle, valoresDetalle, colDetalleEntradaCantidad, 0);
                        agregarCampoActualizacion(updateDetalle, valoresDetalle, colDetalleEntradaPrecioBruto, BigDecimal.ZERO);
                        agregarCampoActualizacion(updateDetalle, valoresDetalle, colDetalleEntradaPrecioTotal, BigDecimal.ZERO);
                        agregarCampoActualizacion(updateDetalle, valoresDetalle, colDetalleEntradaEstado, "desactivado");
                        if (!valoresDetalle.isEmpty()) {
                            updateDetalle.append(" WHERE `").append(colDetalleEntradaId).append("` = ?");
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

                    List<Integer> detallesSalida = new ArrayList<>();
                    String sqlDetallesSalida = "SELECT `" + colDetalleSalidaId + "` AS idDetalle FROM detalle_Salida WHERE `"
                            + colDetalleSalidaClave + "` = ?";
                    try (PreparedStatement ps = conn.prepareStatement(sqlDetallesSalida)) {
                        ps.setString(1, ajusteId);
                        try (ResultSet rs = ps.executeQuery()) {
                            while (rs.next()) {
                                detallesSalida.add(rs.getInt("idDetalle"));
                            }
                        }
                    }

                    Set<Integer> detalleEntradaIds = new HashSet<>();
                    if (!detallesSalida.isEmpty() && colArticuloDetalleEntrada != null && colArticuloDetalleSalida != null) {
                        String sqlDetalleEntrada = "SELECT DISTINCT `" + colArticuloDetalleEntrada
                                + "` AS idDetalleEntrada FROM articulo WHERE `" + colArticuloDetalleSalida + "` IN ("
                                + placeholders(detallesSalida.size()) + ")";
                        try (PreparedStatement ps = conn.prepareStatement(sqlDetalleEntrada)) {
                            int index = 1;
                            for (Integer detalleId : detallesSalida) {
                                ps.setInt(index++, detalleId);
                            }
                            try (ResultSet rs = ps.executeQuery()) {
                                while (rs.next()) {
                                    detalleEntradaIds.add(rs.getInt("idDetalleEntrada"));
                                }
                            }
                        }
                    }

                    if (!detallesSalida.isEmpty() && colArticuloDetalleSalida != null && colArticuloEstado != null) {
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

                    if (!detallesSalida.isEmpty() && colDetalleArticuloSalida != null && colDetalleArticuloEstado != null) {
                        String sqlActualizarDetalleArticulo = "UPDATE detalleArticulo SET `" + colDetalleArticuloEstado + "` = ?, `"
                                + colDetalleArticuloSalida + "` = NULL WHERE `" + colDetalleArticuloSalida + "` = ?";
                        try (PreparedStatement ps = conn.prepareStatement(sqlActualizarDetalleArticulo)) {
                            for (Integer detalleId : detallesSalida) {
                                ps.setString(1, "disponible");
                                ps.setInt(2, detalleId);
                                ps.addBatch();
                            }
                            ps.executeBatch();
                        }
                    }

                    if (!detallesSalida.isEmpty() && colDetalleArticuloSalida != null && colDetalleArticuloArticulo != null
                            && colArticuloId != null && colArticuloDetalleEntrada != null) {
                        String sqlDetalleEntrada = "SELECT DISTINCT a.`" + colArticuloDetalleEntrada
                                + "` AS idDetalleEntrada FROM detalleArticulo da JOIN articulo a ON a.`" + colArticuloId
                                + "` = da.`" + colDetalleArticuloArticulo + "` WHERE da.`" + colDetalleArticuloSalida + "` IN ("
                                + placeholders(detallesSalida.size()) + ")";
                        try (PreparedStatement ps = conn.prepareStatement(sqlDetalleEntrada)) {
                            int index = 1;
                            for (Integer detalleId : detallesSalida) {
                                ps.setInt(index++, detalleId);
                            }
                            try (ResultSet rs = ps.executeQuery()) {
                                while (rs.next()) {
                                    detalleEntradaIds.add(rs.getInt("idDetalleEntrada"));
                                }
                            }
                        }
                    }

                    if (!detalleEntradaIds.isEmpty() && colDetalleEntradaId != null && colDetalleEntradaEstado != null) {
                        String sqlActualizarDetalleEntrada = "UPDATE detalle_Entrada SET `" + colDetalleEntradaEstado
                                + "` = ? WHERE `" + colDetalleEntradaId + "` IN (" + placeholders(detalleEntradaIds.size()) + ")";
                        try (PreparedStatement ps = conn.prepareStatement(sqlActualizarDetalleEntrada)) {
                            int index = 1;
                            ps.setString(index++, "activo");
                            for (Integer detalleId : detalleEntradaIds) {
                                ps.setInt(index++, detalleId);
                            }
                            ps.executeUpdate();
                        }
                    }

                    if (!detalleEntradaIds.isEmpty() && colDetalleEntradaId != null && colDetalleEntradaClave != null
                            && colEntradaId != null && colEntradaEstado != null) {
                        Set<Integer> entradasIds = new HashSet<>();
                        String sqlClaveEntrada = "SELECT DISTINCT `" + colDetalleEntradaClave
                                + "` AS claveEntrada FROM detalle_Entrada WHERE `" + colDetalleEntradaId + "` IN ("
                                + placeholders(detalleEntradaIds.size()) + ")";
                        try (PreparedStatement ps = conn.prepareStatement(sqlClaveEntrada)) {
                            int index = 1;
                            for (Integer detalleId : detalleEntradaIds) {
                                ps.setInt(index++, detalleId);
                            }
                            try (ResultSet rs = ps.executeQuery()) {
                                while (rs.next()) {
                                    Integer entradaId = parseInteger(rs.getObject("claveEntrada"));
                                    if (entradaId != null && entradaId > 0) {
                                        entradasIds.add(entradaId);
                                    }
                                }
                            }
                        }

                        if (!entradasIds.isEmpty()) {
                            String sqlActualizarEntrada = "UPDATE entradas SET `" + colEntradaEstado + "` = ? WHERE `"
                                    + colEntradaId + "` IN (" + placeholders(entradasIds.size()) + ")";
                            try (PreparedStatement ps = conn.prepareStatement(sqlActualizarEntrada)) {
                                int index = 1;
                                ps.setString(index++, "disponible");
                                for (Integer entradaId : entradasIds) {
                                    ps.setInt(index++, entradaId);
                                }
                                ps.executeUpdate();
                            }
                        }
                    }

                    if (!detallesSalida.isEmpty()) {
                        StringBuilder updateDetalle = new StringBuilder("UPDATE detalle_Salida SET ");
                        List<Object> valoresDetalle = new ArrayList<>();
                        agregarCampoActualizacion(updateDetalle, valoresDetalle, colDetalleSalidaCantidad, 0);
                        agregarCampoActualizacion(updateDetalle, valoresDetalle, colDetalleSalidaPrecioBruto, BigDecimal.ZERO);
                        agregarCampoActualizacion(updateDetalle, valoresDetalle, colDetalleSalidaPrecioTotal, BigDecimal.ZERO);
                        agregarCampoActualizacion(updateDetalle, valoresDetalle, colDetalleSalidaEstado, "desactivado");
                        if (!valoresDetalle.isEmpty()) {
                            updateDetalle.append(" WHERE `").append(colDetalleSalidaId).append("` = ?");
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

                    StringBuilder updateAjuste = new StringBuilder("UPDATE ajuste_inventario SET ");
                    List<Object> valoresAjuste = new ArrayList<>();
                    agregarCampoActualizacion(updateAjuste, valoresAjuste, colAjustePrecioNeto, BigDecimal.ZERO);
                    agregarCampoActualizacion(updateAjuste, valoresAjuste, colAjustePrecioTotal, BigDecimal.ZERO);
                    agregarCampoActualizacion(updateAjuste, valoresAjuste, colAjusteEstado, "cancelado");
                    if (!valoresAjuste.isEmpty()) {
                        updateAjuste.append(" WHERE `").append(colAjusteId).append("` = ?");
                        valoresAjuste.add(ajusteId);
                        try (PreparedStatement ps = conn.prepareStatement(updateAjuste.toString())) {
                            for (int i = 0; i < valoresAjuste.size(); i++) {
                                ps.setObject(i + 1, valoresAjuste.get(i));
                            }
                            ps.executeUpdate();
                        }
                    }

                    conn.commit();
                    notificarActualizacion();
                    finalizarCancelacionConExito("La cancelación del ajuste se realizó correctamente.");
                } catch (SQLException e) {
                    conn.rollback();
                    throw e;
                }
                } catch (SQLException e) {
                    e.printStackTrace();
                } finally {
                    finalizarCancelacionSinMensaje();
                }
            });
        });
    }


    private void finalizarCancelacionConExito(String mensaje) {
        finalizarCancelacionSinMensaje();
        mostrarMensajeCancelacionExitosa(mensaje);
        cerrarVentana();
    }

    private void finalizarCancelacionSinMensaje() {
        ocultarCargandoCancelacion();
        setProcesandoCancelacion(false);
    }


    private void setProcesandoCancelacion(boolean enProceso) {
        if (btnCancelar != null) {
            btnCancelar.setDisable(enProceso);
        }
        if (btnCancelarEntrada != null) {
            btnCancelarEntrada.setDisable(enProceso);
        }
        if (btnCerrar != null) {
            btnCerrar.setDisable(enProceso);
        }
        if (root != null) {
            root.setDisable(enProceso);
        }
    }
    private void mostrarCargandoCancelacion() {
        if (overlayCargaGlobal != null) {
            overlayCargaGlobal.mostrar();
        }
        if (overlayCarga != null) {
            overlayCarga.mostrar();
        }
    }

    private void ocultarCargandoCancelacion() {
        if (overlayCarga != null) {
            overlayCarga.ocultar();
        }
        if (overlayCargaGlobal != null) {
            overlayCargaGlobal.ocultar();
        }
    }

    private void mostrarMensajeCancelacionExitosa(String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.INFORMATION);
        alerta.setTitle("Cancelación");
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);
        alerta.showAndWait();
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

        boolean mostrarBoton = false;
        String textoBoton = "Cancelar";
        boolean mostrarEntrada = false;

        if (historial != null) {
            String movimiento = historial.getMovimiento();
            String claveMovimiento = historial.getClaveMovimiento();
            try (Connection conn = new Conexion().conectar()) {
                if (conn != null) {
                    if ("Salida".equalsIgnoreCase(movimiento)) {
                        Integer salidaId = parseInteger(claveMovimiento);
                        if (salidaId != null && salidaId > 0) {
                            mostrarBoton = puedeCancelarSalida(conn, salidaId);
                            textoBoton = "Cancelar salida";
                        }
                    } else if ("Ajuste".equalsIgnoreCase(movimiento)) {
                        if (claveMovimiento != null && !claveMovimiento.isBlank()) {
                            mostrarBoton = puedeCancelarAjuste(conn, claveMovimiento);
                            textoBoton = "Cancelar ajuste";
                        }
                    }

                    if ("Entrada".equalsIgnoreCase(movimiento) && btnCancelarEntrada != null) {
                        Integer entradaId = parseInteger(claveMovimiento);
                        if (entradaId != null && entradaId > 0) {
                            mostrarEntrada = puedeCancelarEntrada(conn, entradaId);
                        }
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        btnCancelar.setText(textoBoton);
        btnCancelar.setVisible(mostrarBoton);
        btnCancelar.setManaged(mostrarBoton);
        btnCancelar.setDisable(!mostrarBoton);

        if (btnCancelarEntrada != null) {
            btnCancelarEntrada.setVisible(mostrarEntrada);
            btnCancelarEntrada.setManaged(mostrarEntrada);
            btnCancelarEntrada.setDisable(!mostrarEntrada);
        }
    }

    private String obtenerPrefijoTipoDetalle(DetalleLinea linea) {
        if (linea == null) {
            return "";
        }
        boolean esAjuste = historial != null && "Ajuste".equalsIgnoreCase(historial.getMovimiento());
        if (!esAjuste) {
            return "";
        }
        if ("Entrada".equalsIgnoreCase(linea.tipo)) {
            return "[Entrada] ";
        }
        if ("Salida".equalsIgnoreCase(linea.tipo)) {
            return "[Salida] ";
        }
        return "";
    }

    private boolean tieneArticulosODetallesEnDetalleSalida(int detalleSalidaId) {
        if (detalleSalidaId <= 0) {
            return false;
        }
        try (Connection conn = new Conexion().conectar()) {
            if (conn == null) {
                return false;
            }
            return tieneArticulosODetallesEnDetalleSalida(conn, detalleSalidaId);
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean tieneArticulosODetallesEnDetalleSalida(Connection conn, Integer detalleSalidaId) throws SQLException {
        if (detalleSalidaId == null || detalleSalidaId <= 0) {
            return false;
        }

        Map<String, String> columnasArticulo = obtenerColumnas(conn, "articulo");
        Map<String, String> columnasDetalleArticulo = obtenerColumnas(conn, "detalleArticulo");

        String colArticuloDetalleSalida = resolverColumna(columnasArticulo, "idDetalleSalida", "id_detalle_salida",
                "detalleSalida", "detalle_salida", "detalle_salida_id");
        if (colArticuloDetalleSalida == null) {
            return false;
        }

        String sqlArticulo = "SELECT COUNT(*) FROM articulo WHERE `" + colArticuloDetalleSalida + "` = ?";
        if (ejecutarConteo(conn, sqlArticulo, detalleSalidaId) > 0) {
            return true;
        }

        String colDetalleArticuloDetalleSalida = resolverColumna(columnasDetalleArticulo, "idDetalleSalida",
                "id_detalle_salida", "detalleSalida", "detalle_salida", "detalle_salida_id");
        if (colDetalleArticuloDetalleSalida != null) {
            String sqlDetalle = "SELECT COUNT(*) FROM detalleArticulo WHERE `" + colDetalleArticuloDetalleSalida + "` = ?";
            if (ejecutarConteo(conn, sqlDetalle, detalleSalidaId) > 0) {
                return true;
            }
        }

        String colDetalleArticuloArticulo = resolverColumna(columnasDetalleArticulo, "idArticulo", "id_articulo", "articulo_id");
        String colArticuloId = resolverColumna(columnasArticulo, "idArticulo", "id", "id_articulo");
        if (colDetalleArticuloArticulo != null && colArticuloId != null) {
            String sqlDetallePorArticulo = "SELECT COUNT(*) FROM detalleArticulo da "
                    + "JOIN articulo a ON da.`" + colDetalleArticuloArticulo + "` = a.`" + colArticuloId + "` "
                    + "WHERE a.`" + colArticuloDetalleSalida + "` = ?";
            return ejecutarConteo(conn, sqlDetallePorArticulo, detalleSalidaId) > 0;
        }

        return false;
    }

    private boolean esEntradaCancelada(Connection conn, Integer entradaId) throws SQLException {
        String estado = obtenerEstadoEntrada(conn, entradaId);
        return estado != null && estado.equalsIgnoreCase("cancelado");
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
        Map<String, String> columnasDetalleArticulo = obtenerColumnas(conn, "detalleArticulo");
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
        String colDetalleArticuloIdArticulo = resolverColumna(columnasDetalleArticulo, "idArticulo", "id_articulo", "articulo_id");
        String colDetalleArticuloEstado = resolverColumna(columnasDetalleArticulo, "estado", "Estado");

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

        String exprDetalleArticuloBloqueado = "0";
        if (colArticuloId != null && colDetalleArticuloIdArticulo != null && colDetalleArticuloEstado != null) {
            exprDetalleArticuloBloqueado = "EXISTS (SELECT 1 FROM detalleArticulo da WHERE da.`"
                    + colDetalleArticuloIdArticulo + "` = a.`" + colArticuloId + "` AND LOWER(da.`"
                    + colDetalleArticuloEstado + "`) IN ('pendiente','vendido'))";
        }

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
                       %s AS detalleSalida,
                       %s AS tieneDetalleArticuloBloqueado
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
                exprDetalleArticuloBloqueado,
                colArticuloDetalle,
                colDetalleId,
                joinUbicacion,
                joinProducto,
                colClaveEntrada,
                ordenEstado
        );

        Map<Integer, DetalleArticulo> articulosPorId = new HashMap<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, claveMovimiento);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int idDetalle = rs.getInt("idDetalle");
                    DetalleLinea linea = lineasPorId.get(idDetalle);
                    if (linea == null) {
                        continue;
                    }
                    DetalleArticulo detalleArticulo = new DetalleArticulo(
                            valorTexto(rs.getObject("ubicacion")),
                            valorTexto(rs.getObject("lote")),
                            valorTexto(rs.getObject("caducidad")),
                            valorTexto(rs.getObject("presentacion")),
                            valorTexto(rs.getObject("factor")),
                            valorTexto(rs.getObject("estadoArticulo")),
                            rs.getInt("idArticulo"),
                            rs.getObject("detalleEntrada"),
                            rs.getObject("detalleSalida"),
                            rs.getBoolean("tieneDetalleArticuloBloqueado")
                    );
                    linea.articulos.add(detalleArticulo);
                    articulosPorId.put(detalleArticulo.idArticulo, detalleArticulo);
                }
            }
        }

        if (!articulosPorId.isEmpty()) {
            cargarDetallesArticuloPorArticulo(conn, articulosPorId, false);
        }
    }

    private void cargarArticulosSalida(Connection conn, String claveMovimiento,
                                       Map<Integer, DetalleLinea> lineasPorId) throws SQLException {
        Map<String, String> columnasDetalle = obtenerColumnas(conn, "detalle_Salida");
        Map<String, String> columnasArticulo = obtenerColumnas(conn, "articulo");
        Map<String, String> columnasDetalleArticulo = obtenerColumnas(conn, "detalleArticulo");
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
        String colDetalleArticuloIdArticulo = resolverColumna(columnasDetalleArticulo, "idArticulo", "id_articulo", "articulo_id");
        String colDetalleArticuloEstado = resolverColumna(columnasDetalleArticulo, "estado", "Estado");

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

        String exprDetalleArticuloBloqueado = "0";
        if (colArticuloId != null && colDetalleArticuloIdArticulo != null && colDetalleArticuloEstado != null) {
            exprDetalleArticuloBloqueado = "EXISTS (SELECT 1 FROM detalleArticulo da WHERE da.`"
                    + colDetalleArticuloIdArticulo + "` = a.`" + colArticuloId + "` AND LOWER(da.`"
                    + colDetalleArticuloEstado + "`) IN ('pendiente','vendido'))";
        }

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
                       %s AS detalleSalida,
                       %s AS tieneDetalleArticuloBloqueado
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
                exprDetalleArticuloBloqueado,
                colArticuloDetalle,
                colDetalleId,
                joinUbicacion,
                joinProducto,
                colClaveSalida,
                ordenEstado
        );

        Map<Integer, DetalleArticulo> articulosPorId = new HashMap<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, claveMovimiento);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int idDetalle = rs.getInt("idDetalle");
                    DetalleLinea linea = lineasPorId.get(idDetalle);
                    if (linea == null) {
                        continue;
                    }
                    DetalleArticulo detalleArticulo = new DetalleArticulo(
                            valorTexto(rs.getObject("ubicacion")),
                            valorTexto(rs.getObject("lote")),
                            valorTexto(rs.getObject("caducidad")),
                            valorTexto(rs.getObject("presentacion")),
                            valorTexto(rs.getObject("factor")),
                            valorTexto(rs.getObject("estadoArticulo")),
                            rs.getInt("idArticulo"),
                            rs.getObject("detalleEntrada"),
                            rs.getObject("detalleSalida"),
                            rs.getBoolean("tieneDetalleArticuloBloqueado")
                    );
                    linea.articulos.add(detalleArticulo);
                    articulosPorId.put(detalleArticulo.idArticulo, detalleArticulo);
                }
            }
        }

        if (!articulosPorId.isEmpty()) {
            cargarDetallesArticuloPorArticulo(conn, articulosPorId, true);
        }
        cargarArticulosSegmentadosDesdeDetalleSalida(conn, lineasPorId, articulosPorId);
    }

    private void cargarArticulosSegmentadosDesdeDetalleSalida(Connection conn,
                                                              Map<Integer, DetalleLinea> lineasPorId,
                                                              Map<Integer, DetalleArticulo> articulosPorId) throws SQLException {
        if (conn == null || lineasPorId == null || lineasPorId.isEmpty()) {
            return;
        }

        Map<String, String> columnasDetalleArticulo = obtenerColumnas(conn, "detalleArticulo");
        Map<String, String> columnasArticulo = obtenerColumnas(conn, "articulo");
        Map<String, String> columnasUbicacion = obtenerColumnas(conn, "ubicaciones");

        String colDetalleArticuloArticulo = resolverColumna(columnasDetalleArticulo, "idArticulo", "id_articulo", "articulo_id");
        String colDetalleArticuloSalida = resolverColumna(columnasDetalleArticulo, "idDetalleSalida", "id_detalle_salida",
                "detalleSalida", "detalle_salida", "detalle_salida_id");
        String colDetalleArticuloLote = resolverColumna(columnasDetalleArticulo, "lote");
        String colDetalleArticuloCaducidad = resolverColumna(columnasDetalleArticulo, "caducidad");
        String colDetalleArticuloPresentacion = resolverColumna(columnasDetalleArticulo, "presentacion");
        String colDetalleArticuloFactor = resolverColumna(columnasDetalleArticulo, "factor");
        String colDetalleArticuloEstado = resolverColumna(columnasDetalleArticulo, "estado", "Estado");
        String colDetalleArticuloUbicacion = resolverColumna(columnasDetalleArticulo, "ubicacion", "idUbicacion", "id_ubicacion");

        String colArticuloId = resolverColumna(columnasArticulo, "idArticulo", "id", "id_articulo");
        String colArticuloLote = resolverColumna(columnasArticulo, "lote");
        String colArticuloCaducidad = resolverColumna(columnasArticulo, "caducidad");
        String colArticuloPresentacion = resolverColumna(columnasArticulo, "presentacion");
        String colArticuloFactor = resolverColumna(columnasArticulo, "factor");
        String colArticuloEstado = resolverColumna(columnasArticulo, "Estado", "estado");

        String colUbicacionId = resolverColumna(columnasUbicacion, "id", "idUbicacion", "ubicacion_id");
        String colUbicacionNombre = resolverColumna(columnasUbicacion, "nombre", "Nombre", "ubicacion");

        if (colDetalleArticuloSalida == null) {
            return;
        }

        String joinArticulo = (colArticuloId != null && colDetalleArticuloArticulo != null)
                ? "LEFT JOIN articulo a ON da.`" + colDetalleArticuloArticulo + "` = a.`" + colArticuloId + "`"
                : "";

        String joinUbicacion = (colDetalleArticuloUbicacion != null && colUbicacionId != null && colUbicacionNombre != null)
                ? "LEFT JOIN ubicaciones u ON da.`" + colDetalleArticuloUbicacion + "` = u.`" + colUbicacionId + "`"
                : "";
        String ubicacionExpr = (colDetalleArticuloUbicacion != null && colUbicacionId != null && colUbicacionNombre != null)
                ? "u.`" + colUbicacionNombre + "`"
                : "NULL";

        List<Integer> detalleSalidaIds = new ArrayList<>(lineasPorId.keySet());
        String sql = String.format("""
                SELECT %s AS idDetalle,
                       da.`%s` AS idDetalleSalida,
                       %s AS idArticulo,
                       %s AS lote,
                       %s AS caducidad,
                       %s AS presentacion,
                       %s AS factor,
                       %s AS estado,
                       %s AS ubicacion,
                       %s AS articuloLote,
                       %s AS articuloCaducidad,
                       %s AS articuloPresentacion,
                       %s AS articuloFactor,
                       %s AS articuloEstado
                FROM detalleArticulo da
                %s
                %s
                WHERE da.`%s` IN (%s)
                ORDER BY da.`%s`
                """,
                colDetalleId != null ? "da.`" + colDetalleId + "`" : "NULL",
                colDetalleArticuloSalida,
                colDetalleArticuloArticulo != null ? "da.`" + colDetalleArticuloArticulo + "`" : "NULL",
                columnaSeguro("da", colDetalleArticuloLote),
                columnaSeguro("da", colDetalleArticuloCaducidad),
                columnaSeguro("da", colDetalleArticuloPresentacion),
                columnaSeguro("da", colDetalleArticuloFactor),
                columnaSeguro("da", colDetalleArticuloEstado),
                ubicacionExpr,
                columnaSeguro("a", colArticuloLote),
                columnaSeguro("a", colArticuloCaducidad),
                columnaSeguro("a", colArticuloPresentacion),
                columnaSeguro("a", colArticuloFactor),
                columnaSeguro("a", colArticuloEstado),
                joinArticulo,
                joinUbicacion,
                colDetalleArticuloSalida,
                placeholders(detalleSalidaIds.size()),
                colDetalleArticuloSalida
        );

        Map<Integer, DetalleArticulo> virtualesPorDetalleSalida = new HashMap<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            int index = 1;
            for (Integer idDetalleSalida : detalleSalidaIds) {
                ps.setInt(index++, idDetalleSalida);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Integer idDetalleSalida = parseInteger(rs.getObject("idDetalleSalida"));
                    if (idDetalleSalida == null) {
                        continue;
                    }

                    Integer idArticulo = parseInteger(rs.getObject("idArticulo"));
                    if (idArticulo != null && articulosPorId != null && articulosPorId.containsKey(idArticulo)) {
                        continue;
                    }

                    DetalleLinea linea = lineasPorId.get(idDetalleSalida);
                    if (linea == null) {
                        continue;
                    }

                    DetalleArticulo articuloVirtual = virtualesPorDetalleSalida.get(idDetalleSalida);
                    if (articuloVirtual == null) {
                        String loteArticulo = valorTexto(rs.getObject("articuloLote"));
                        String caducidadArticulo = valorTexto(rs.getObject("articuloCaducidad"));
                        String presentacionArticulo = valorTexto(rs.getObject("articuloPresentacion"));
                        String factorArticulo = valorTexto(rs.getObject("articuloFactor"));
                        String estadoArticulo = valorTexto(rs.getObject("articuloEstado"));
                        if (estadoArticulo.isBlank()) {
                            estadoArticulo = "segmentado";
                        }

                        articuloVirtual = new DetalleArticulo(
                                "",
                                loteArticulo,
                                caducidadArticulo,
                                presentacionArticulo,
                                factorArticulo,
                                estadoArticulo,
                                0,
                                null,
                                idDetalleSalida,
                                false
                        );
                        linea.articulos.add(articuloVirtual);
                        virtualesPorDetalleSalida.put(idDetalleSalida, articuloVirtual);
                    }

                    articuloVirtual.detallesSegmentados.add(new DetalleArticuloSegmentado(
                            parseInteger(rs.getObject("idDetalle")),
                            valorTexto(rs.getObject("ubicacion")),
                            valorTexto(rs.getObject("lote")),
                            valorTexto(rs.getObject("caducidad")),
                            valorTexto(rs.getObject("presentacion")),
                            valorTexto(rs.getObject("factor")),
                            valorTexto(rs.getObject("estado"))
                    ));
                }
            }
        }
    }

    private void cargarDetallesArticuloPorArticulo(Connection conn,
                                                   Map<Integer, DetalleArticulo> articulosPorId,
                                                   boolean filtrarPorDetalleSalida) throws SQLException {
        if (conn == null || articulosPorId == null || articulosPorId.isEmpty()) {
            return;
        }

        Map<String, String> columnasDetalleArticulo = obtenerColumnas(conn, "detalleArticulo");
        Map<String, String> columnasUbicacion = obtenerColumnas(conn, "ubicaciones");

        String colDetalleId = resolverColumna(columnasDetalleArticulo, "idDetalle", "id", "id_detalle");
        String colDetalleArticuloArticulo = resolverColumna(columnasDetalleArticulo, "idArticulo", "id_articulo", "articulo_id");
        String colDetalleArticuloSalida = resolverColumna(columnasDetalleArticulo, "idDetalleSalida", "id_detalle_salida",
                "detalleSalida", "detalle_salida", "detalle_salida_id");
        String colDetalleArticuloLote = resolverColumna(columnasDetalleArticulo, "lote");
        String colDetalleArticuloCaducidad = resolverColumna(columnasDetalleArticulo, "caducidad");
        String colDetalleArticuloPresentacion = resolverColumna(columnasDetalleArticulo, "presentacion");
        String colDetalleArticuloFactor = resolverColumna(columnasDetalleArticulo, "factor");
        String colDetalleArticuloEstado = resolverColumna(columnasDetalleArticulo, "estado", "Estado");
        String colDetalleArticuloUbicacion = resolverColumna(columnasDetalleArticulo, "ubicacion", "idUbicacion", "id_ubicacion");

        String colUbicacionId = resolverColumna(columnasUbicacion, "id", "idUbicacion", "ubicacion_id");
        String colUbicacionNombre = resolverColumna(columnasUbicacion, "nombre", "Nombre", "ubicacion");

        if (colDetalleArticuloArticulo == null) {
            return;
        }

        String joinUbicacion = (colDetalleArticuloUbicacion != null && colUbicacionId != null && colUbicacionNombre != null)
                ? "LEFT JOIN ubicaciones u ON da.`" + colDetalleArticuloUbicacion + "` = u.`" + colUbicacionId + "`"
                : "";
        String ubicacionExpr = (colDetalleArticuloUbicacion != null && colUbicacionId != null && colUbicacionNombre != null)
                ? "u.`" + colUbicacionNombre + "`"
                : "NULL";

        List<Integer> articulosIds = new ArrayList<>(articulosPorId.keySet());
        String sql = String.format("""
                SELECT %s AS idDetalle,
                       da.`%s` AS idArticulo,
                       %s AS lote,
                       %s AS caducidad,
                       %s AS presentacion,
                       %s AS factor,
                       %s AS estado,
                       %s AS ubicacion,
                       %s AS idDetalleSalida
                FROM detalleArticulo da
                %s
                WHERE da.`%s` IN (%s)
                ORDER BY da.`%s`
                """,
                colDetalleId != null ? "da.`" + colDetalleId + "`" : "NULL",
                colDetalleArticuloArticulo,
                columnaSeguro("da", colDetalleArticuloLote),
                columnaSeguro("da", colDetalleArticuloCaducidad),
                columnaSeguro("da", colDetalleArticuloPresentacion),
                columnaSeguro("da", colDetalleArticuloFactor),
                columnaSeguro("da", colDetalleArticuloEstado),
                ubicacionExpr,
                colDetalleArticuloSalida != null ? "da.`" + colDetalleArticuloSalida + "`" : "NULL",
                joinUbicacion,
                colDetalleArticuloArticulo,
                placeholders(articulosIds.size()),
                colDetalleArticuloArticulo
        );

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            int index = 1;
            for (Integer idArticulo : articulosIds) {
                ps.setInt(index++, idArticulo);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Integer idArticulo = parseInteger(rs.getObject("idArticulo"));
                    if (idArticulo == null) {
                        continue;
                    }
                    DetalleArticulo articulo = articulosPorId.get(idArticulo);
                    if (articulo == null) {
                        continue;
                    }

                    if (filtrarPorDetalleSalida && articulo.detalleSalidaId != null) {
                        Integer idDetalleSalida = parseInteger(rs.getObject("idDetalleSalida"));
                        if (idDetalleSalida == null || !articulo.detalleSalidaId.equals(idDetalleSalida)) {
                            continue;
                        }
                    }

                    articulo.detallesSegmentados.add(new DetalleArticuloSegmentado(
                            parseInteger(rs.getObject("idDetalle")),
                            valorTexto(rs.getObject("ubicacion")),
                            valorTexto(rs.getObject("lote")),
                            valorTexto(rs.getObject("caducidad")),
                            valorTexto(rs.getObject("presentacion")),
                            valorTexto(rs.getObject("factor")),
                            valorTexto(rs.getObject("estado"))
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

            try (Connection conn = new Conexion().conectar()) {
                if (conn != null) {
                    if ("Salida".equalsIgnoreCase(movimiento)) {
                        Integer idMovimiento = parseInteger(clave);
                        if (idMovimiento != null && idMovimiento > 0) {
                            esSalidaCancelada = esSalidaCancelada(conn, idMovimiento);
                        }
                    } else if ("Entrada".equalsIgnoreCase(movimiento)) {
                        Integer idMovimiento = parseInteger(clave);
                        if (idMovimiento != null && idMovimiento > 0) {
                            esEntradaCancelada = esEntradaCancelada(conn, idMovimiento);
                        }
                    } else if ("Ajuste".equalsIgnoreCase(movimiento) && clave != null && !clave.isBlank()) {
                        esSalidaCancelada = esAjusteCancelado(conn, clave);
                        esEntradaCancelada = esSalidaCancelada;
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
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

                String prefijoTipo = obtenerPrefijoTipoDetalle(linea);
                Label titulo = new Label(contador++ + ". " + prefijoTipo + valorTexto(linea.producto));
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

                            boolean bloqueadoPorDetalleArticulo = articulo.esSegmentado()
                                    && articulo.esDetalleEntrada()
                                    && articulo.tieneDetalleArticuloPendienteOVendido();

                            boolean esArticuloSalida = articulo.esDetalleSalida();
                            boolean puedeEditarArticulo;
                            boolean puedeEliminarArticulo;
                            if (esArticuloSalida) {
                                boolean permitidoEnSalida = !articulo.esEliminado() && !articulo.esSegmentado();
                                puedeEditarArticulo = articulo.idArticulo > 0 && permitidoEnSalida;
                                puedeEliminarArticulo = articulo.idArticulo > 0 && permitidoEnSalida;
                            } else {
                                puedeEditarArticulo = articulo.idArticulo > 0
                                        && !bloqueadoPorDetalleArticulo
                                        && !articulo.esPendiente()
                                        && !articulo.esVendido()
                                        && (articulo.esDisponible() || articulo.esSegmentado());
                                puedeEliminarArticulo = articulo.idArticulo > 0
                                        && !bloqueadoPorDetalleArticulo
                                        && !articulo.esPendiente()
                                        && !articulo.esVendido()
                                        && (articulo.esDisponible() || articulo.esSegmentado());
                            }
                            if (puedeEditarArticulo || puedeEliminarArticulo) {
                                // Botón Editar con icono
                                if (puedeEditarArticulo) {
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
                                    botonesContainer.getChildren().add(btnEditar);
                                }

                                if (puedeEliminarArticulo) {
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
                                    botonesContainer.getChildren().add(btnEliminar);
                                }
                            }

                            Label numero = new Label(index++ + ".");
                            numero.setStyle("-fx-font-weight: bold; -fx-font-size: 13; -fx-text-fill: #91d485; " +
                                    "-fx-min-width: 25; -fx-padding: 0 5 0 0;");

                            VBox infoBox = new VBox(6);
                            infoBox.setStyle("-fx-padding: 0 0 0 10;");

                            // Primera línea: Ubicación y Lote
                            HBox linea1 = new HBox(15);
                            Label lblLote = crearEtiquetaDetalleElegante("Lote:", valorTexto(articulo.lote));
                            linea1.getChildren().addAll(lblLote);

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

                            if (articulo.esSegmentado() && articulo.tieneDetallesSegmentados()) {
                                CheckBox chkDesplegar = new CheckBox("Mostrar detalles segmentados");
                                chkDesplegar.getStyleClass().add("check-detalles-segmentados");

                                VBox contenedorDetallesSegmentados = new VBox(6);
                                contenedorDetallesSegmentados.setVisible(false);
                                contenedorDetallesSegmentados.setManaged(false);
                                contenedorDetallesSegmentados.setStyle("-fx-padding: 8 0 0 6;");

                                int idxSeg = 1;
                                for (DetalleArticuloSegmentado detSeg : articulo.detallesSegmentados) {
                                    VBox itemSeg = new VBox(6);
                                    itemSeg.setStyle("-fx-background-color: #eef3f7; -fx-padding: 8; -fx-background-radius: 6;");

                                    HBox encabezadoSeg = new HBox(8);
                                    encabezadoSeg.setAlignment(Pos.CENTER_LEFT);
                                    Label lblDetalleSeg = crearEtiquetaTituloSegmentado("Detalle #" + idxSeg++);
                                    Region spacerSeg = new Region();
                                    HBox.setHgrow(spacerSeg, Priority.ALWAYS);
                                    HBox botonesSeg = new HBox(6);
                                    botonesSeg.setAlignment(Pos.CENTER_RIGHT);

                                    if (articulo.esDetalleSalida() && detSeg.idDetalle != null && detSeg.idDetalle > 0
                                            && !detSeg.esEliminado() && !detSeg.esSegmentado()) {
                                        Button btnEditarSeg = crearBotonIcono("/img/editar.png", "Editar");
                                        btnEditarSeg.setOnAction(event -> editarDetalleArticuloSegmentadoSalida(detSeg));
                                        Button btnEliminarSeg = crearBotonIcono("/img/eliminar.png", "Eliminar");
                                        btnEliminarSeg.setOnAction(event -> eliminarDetalleArticuloSegmentadoSalida(detSeg));
                                        botonesSeg.getChildren().addAll(btnEditarSeg, btnEliminarSeg);
                                    }

                                    encabezadoSeg.getChildren().addAll(lblDetalleSeg, spacerSeg, botonesSeg);

                                    itemSeg.getChildren().addAll(
                                            encabezadoSeg,
                                            crearEtiquetaDetalleElegante("Ubicación:", valorTexto(detSeg.ubicacion)),
                                            crearEtiquetaDetalleElegante("Estado:", valorTexto(detSeg.estado))
                                    );
                                    contenedorDetallesSegmentados.getChildren().add(itemSeg);
                                }

                                chkDesplegar.selectedProperty().addListener((obs, oldVal, newVal) -> {
                                    contenedorDetallesSegmentados.setVisible(newVal);
                                    contenedorDetallesSegmentados.setManaged(newVal);
                                });

                                infoBox.getChildren().addAll(chkDesplegar, contenedorDetallesSegmentados);
                            }

                            // ORDEN CORREGIDO: Botones a la izquierda, luego número, luego información
                            articuloCard.getChildren().addAll(numero, infoBox, botonesContainer);
                            HBox.setHgrow(infoBox, javafx.scene.layout.Priority.ALWAYS);

                            listaArticulos.getChildren().add(articuloCard);
                        }
                    }
                    card.getChildren().add(listaArticulos);
                }

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

                boolean puedeEditarSalida = "Salida".equalsIgnoreCase(linea.tipo)
                        && linea.esVenta()
                        && tieneArticulosODetallesEnDetalleSalida(linea.idDetalle);

                if (puedeEditarSalida) {
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
    private boolean esAjusteCancelado(Connection conn, String ajusteId) throws SQLException {
        String estado = obtenerEstadoAjuste(conn, ajusteId);
        return estado != null && estado.equalsIgnoreCase("cancelado");
    }

    private boolean puedeCancelarEntrada(Connection conn, Integer entradaId) throws SQLException {
        String estado = obtenerEstadoEntrada(conn, entradaId);
        if (estado == null) {
            return entradaSoloTieneEstadosCancelables(conn, entradaId);
        }
        String estadoNormalizado = estado.trim().toLowerCase();
        if (List.of("cancelado", "finalizada", "finalizado", "revision", "revisión").contains(estadoNormalizado)) {
            return false;
        }
        return entradaSoloTieneEstadosCancelables(conn, entradaId);
    }

    private boolean entradaSoloTieneEstadosCancelables(Connection conn, Integer entradaId) throws SQLException {
        if (entradaId == null) {
            return false;
        }

        Map<String, String> columnasDetalleEntrada = obtenerColumnas(conn, "detalle_Entrada");
        Map<String, String> columnasArticulo = obtenerColumnas(conn, "articulo");
        Map<String, String> columnasDetalleArticulo = obtenerColumnas(conn, "detalleArticulo");

        String colDetalleEntradaId = resolverColumna(columnasDetalleEntrada, "idDetalleEntrada", "id", "id_detalle_entrada");
        String colDetalleEntradaClave = resolverColumna(columnasDetalleEntrada, "claveEntrada", "idEntrada", "id_entrada", "entrada_id");
        String colArticuloDetalleEntrada = resolverColumna(columnasArticulo, "idDetalleEntrada", "id_detalle_entrada",
                "detalleEntrada", "detalle_entrada", "detalle_entrada_id");
        String colArticuloId = resolverColumna(columnasArticulo, "idArticulo", "id", "id_articulo");
        String colArticuloEstado = resolverColumna(columnasArticulo, "Estado", "estado");
        String colDetalleArticuloArticulo = resolverColumna(columnasDetalleArticulo, "idArticulo", "id_articulo", "articulo_id");
        String colDetalleArticuloEstado = resolverColumna(columnasDetalleArticulo, "estado", "Estado");

        if (colDetalleEntradaId == null || colDetalleEntradaClave == null
                || colArticuloDetalleEntrada == null || colArticuloEstado == null) {
            return false;
        }

        final String sqlConteoArticulo = "SELECT COUNT(*) FROM articulo a "
                + "JOIN detalle_Entrada d ON a.`" + colArticuloDetalleEntrada + "` = d.`" + colDetalleEntradaId + "` "
                + "WHERE d.`" + colDetalleEntradaClave + "` = ?";

        final String sqlConteoArticuloNoPermitido = "SELECT COUNT(*) FROM articulo a "
                + "JOIN detalle_Entrada d ON a.`" + colArticuloDetalleEntrada + "` = d.`" + colDetalleEntradaId + "` "
                + "WHERE d.`" + colDetalleEntradaClave + "` = ? "
                + "AND (a.`" + colArticuloEstado + "` IS NULL OR LOWER(a.`" + colArticuloEstado + "`) NOT IN ('eliminado', 'disponible'))";

        final String sqlConteoArticuloDisponible = "SELECT COUNT(*) FROM articulo a "
                + "JOIN detalle_Entrada d ON a.`" + colArticuloDetalleEntrada + "` = d.`" + colDetalleEntradaId + "` "
                + "WHERE d.`" + colDetalleEntradaClave + "` = ? "
                + "AND LOWER(a.`" + colArticuloEstado + "`) = 'disponible'";

        int totalArticulos = ejecutarConteo(conn, sqlConteoArticulo, entradaId);
        int articulosNoPermitidos = ejecutarConteo(conn, sqlConteoArticuloNoPermitido, entradaId);
        int articulosDisponibles = ejecutarConteo(conn, sqlConteoArticuloDisponible, entradaId);
        if (articulosNoPermitidos > 0) {
            return false;
        }

        int totalDetalles = 0;
        int detallesNoPermitidos = 0;
        int detallesDisponibles = 0;
        if (colArticuloId != null && colDetalleArticuloArticulo != null && colDetalleArticuloEstado != null) {
            final String sqlConteoDetalleArticulo = "SELECT COUNT(*) FROM detalleArticulo da "
                    + "JOIN articulo a ON da.`" + colDetalleArticuloArticulo + "` = a.`" + colArticuloId + "` "
                    + "JOIN detalle_Entrada d ON a.`" + colArticuloDetalleEntrada + "` = d.`" + colDetalleEntradaId + "` "
                    + "WHERE d.`" + colDetalleEntradaClave + "` = ?";

            final String sqlConteoDetalleNoPermitido = "SELECT COUNT(*) FROM detalleArticulo da "
                    + "JOIN articulo a ON da.`" + colDetalleArticuloArticulo + "` = a.`" + colArticuloId + "` "
                    + "JOIN detalle_Entrada d ON a.`" + colArticuloDetalleEntrada + "` = d.`" + colDetalleEntradaId + "` "
                    + "WHERE d.`" + colDetalleEntradaClave + "` = ? "
                    + "AND (da.`" + colDetalleArticuloEstado + "` IS NULL OR LOWER(da.`" + colDetalleArticuloEstado + "`) NOT IN ('eliminado', 'disponible'))";

            final String sqlConteoDetalleDisponible = "SELECT COUNT(*) FROM detalleArticulo da "
                    + "JOIN articulo a ON da.`" + colDetalleArticuloArticulo + "` = a.`" + colArticuloId + "` "
                    + "JOIN detalle_Entrada d ON a.`" + colArticuloDetalleEntrada + "` = d.`" + colDetalleEntradaId + "` "
                    + "WHERE d.`" + colDetalleEntradaClave + "` = ? "
                    + "AND LOWER(da.`" + colDetalleArticuloEstado + "`) = 'disponible'";

            totalDetalles = ejecutarConteo(conn, sqlConteoDetalleArticulo, entradaId);
            detallesNoPermitidos = ejecutarConteo(conn, sqlConteoDetalleNoPermitido, entradaId);
            detallesDisponibles = ejecutarConteo(conn, sqlConteoDetalleDisponible, entradaId);
            if (detallesNoPermitidos > 0) {
                return false;
            }
        }

        int totalRegistros = totalArticulos + totalDetalles;
        int totalDisponibles = articulosDisponibles + detallesDisponibles;
        return totalRegistros > 0 && totalDisponibles > 0;
    }

    private int ejecutarConteo(Connection conn, String sql, Integer entradaId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, entradaId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    private boolean puedeCancelarSalida(Connection conn, Integer salidaId) throws SQLException {
        String estado = obtenerEstadoSalida(conn, salidaId);
        return estado == null || !estado.equalsIgnoreCase("cancelado");
    }

    private boolean puedeCancelarAjuste(Connection conn, String ajusteId) throws SQLException {
        String estado = obtenerEstadoAjuste(conn, ajusteId);
        if (estado != null && estado.equalsIgnoreCase("cancelado")) {
            return false;
        }
        if (ajusteTieneDetalleEntradaDesactivado(conn, ajusteId)) {
            return false;
        }
        return !ajusteTieneVentasEnEntrada(conn, ajusteId);
    }

    private boolean ajusteTieneDetalleEntradaDesactivado(Connection conn, String ajusteId) throws SQLException {
        if (ajusteId == null || ajusteId.isBlank()) {
            return false;
        }
        Map<String, String> columnasDetalle = obtenerColumnas(conn, "detalle_Entrada");
        String colClaveEntrada = resolverColumna(columnasDetalle, "claveEntrada", "idEntrada", "id_entrada", "entrada_id");
        String colEstado = resolverColumna(columnasDetalle, "estado", "Estado");
        if (colClaveEntrada == null || colEstado == null) {
            return false;
        }
        String sql = "SELECT COUNT(*) FROM detalle_Entrada WHERE `" + colClaveEntrada + "` = ? AND LOWER(`"
                + colEstado + "`) = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, ajusteId);
            ps.setString(2, "desactivado");
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    private boolean ajusteTieneVentasEnEntrada(Connection conn, String ajusteId) throws SQLException {
        if (ajusteId == null || ajusteId.isBlank()) {
            return false;
        }
        Map<String, String> columnasDetalle = obtenerColumnas(conn, "detalle_Entrada");
        Map<String, String> columnasArticulo = obtenerColumnas(conn, "articulo");
        Map<String, String> columnasDetalleArticulo = obtenerColumnas(conn, "detalleArticulo");

        String colDetalleId = resolverColumna(columnasDetalle, "idDetalleEntrada", "id", "id_detalle_entrada");
        String colDetalleClave = resolverColumna(columnasDetalle, "claveEntrada", "idEntrada", "id_entrada", "entrada_id");
        String colArticuloId = resolverColumna(columnasArticulo, "idArticulo", "id", "id_articulo");
        String colArticuloDetalleEntrada = resolverColumna(columnasArticulo, "idDetalleEntrada", "id_detalle_entrada",
                "detalleEntrada", "detalle_entrada", "detalle_entrada_id");
        String colArticuloEstado = resolverColumna(columnasArticulo, "Estado", "estado");
        String colDetalleArticuloArticulo = resolverColumna(columnasDetalleArticulo, "idArticulo", "id_articulo",
                "articulo_id");
        String colDetalleArticuloEstado = resolverColumna(columnasDetalleArticulo, "estado", "Estado");

        if (colDetalleId == null || colDetalleClave == null) {
            return false;
        }

        if (colArticuloDetalleEntrada != null && colArticuloEstado != null) {
            String sqlArticulo = "SELECT COUNT(*) FROM articulo a JOIN detalle_Entrada d ON a.`"
                    + colArticuloDetalleEntrada + "` = d.`" + colDetalleId + "` WHERE d.`" + colDetalleClave
                    + "` = ? AND LOWER(a.`" + colArticuloEstado + "`) = ?";
            try (PreparedStatement ps = conn.prepareStatement(sqlArticulo)) {
                ps.setString(1, ajusteId);
                ps.setString(2, "vendido");
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        return true;
                    }
                }
            }
        }

        if (colDetalleArticuloArticulo != null && colDetalleArticuloEstado != null && colArticuloId != null
                && colArticuloDetalleEntrada != null) {
            String sqlDetalleArticulo = "SELECT COUNT(*) FROM detalleArticulo da JOIN articulo a ON a.`" + colArticuloId
                    + "` = da.`" + colDetalleArticuloArticulo + "` JOIN detalle_Entrada d ON a.`"
                    + colArticuloDetalleEntrada + "` = d.`" + colDetalleId + "` WHERE d.`" + colDetalleClave
                    + "` = ? AND LOWER(da.`" + colDetalleArticuloEstado + "`) = ?";
            try (PreparedStatement ps = conn.prepareStatement(sqlDetalleArticulo)) {
                ps.setString(1, ajusteId);
                ps.setString(2, "vendido");
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private String obtenerEstadoEntrada(Connection conn, Integer entradaId) throws SQLException {
        if (entradaId == null || entradaId <= 0) {
            return null;
        }
        Map<String, String> columnasEntrada = obtenerColumnas(conn, "entradas");
        String colEntradaId = resolverColumna(columnasEntrada, "idEntrada", "id", "id_entrada");
        String colEntradaEstado = resolverColumna(columnasEntrada, "estado", "Estado");
        if (colEntradaId == null || colEntradaEstado == null) {
            return null;
        }
        String sql = "SELECT `" + colEntradaEstado + "` AS estado FROM entradas WHERE `" + colEntradaId + "` = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, entradaId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("estado");
                }
            }
        }
        return null;
    }

    private String obtenerEstadoSalida(Connection conn, Integer salidaId) throws SQLException {
        if (salidaId == null || salidaId <= 0) {
            return null;
        }
        Map<String, String> columnasSalida = obtenerColumnas(conn, "salidas");
        String colSalidaId = resolverColumna(columnasSalida, "idSalida", "id", "id_salida");
        String colSalidaEstado = resolverColumna(columnasSalida, "estado", "Estado");
        if (colSalidaId == null || colSalidaEstado == null) {
            return null;
        }
        String sql = "SELECT `" + colSalidaEstado + "` AS estado FROM salidas WHERE `" + colSalidaId + "` = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, salidaId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("estado");
                }
            }
        }
        return null;
    }

    private String obtenerEstadoAjuste(Connection conn, String ajusteId) throws SQLException {
        if (ajusteId == null || ajusteId.isBlank()) {
            return null;
        }
        Map<String, String> columnasAjuste = obtenerColumnas(conn, "ajuste_inventario");
        String colAjusteId = resolverColumna(columnasAjuste, "idAjuste", "id", "id_ajuste");
        String colAjusteEstado = resolverColumna(columnasAjuste, "estado", "Estado");
        if (colAjusteId == null || colAjusteEstado == null) {
            return null;
        }
        String sql = "SELECT `" + colAjusteEstado + "` AS estado FROM ajuste_inventario WHERE `" + colAjusteId + "` = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, ajusteId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("estado");
                }
            }
        }
        return null;
    }

    private Button crearBotonIcono(String recursoIcono, String textoFallback) {
        Button boton = new Button();
        try {
            ImageView icono = new ImageView(new Image(getClass().getResourceAsStream(recursoIcono)));
            icono.setFitWidth(14);
            icono.setFitHeight(14);
            boton.setGraphic(icono);
        } catch (Exception e) {
            boton.setText(textoFallback);
        }
        boton.setStyle("-fx-background-color: #333; -fx-cursor: hand; -fx-padding: 4 8; -fx-background-radius: 4;");
        return boton;
    }

    private void editarDetalleArticuloSegmentadoSalida(DetalleArticuloSegmentado detalle) {
        if (detalle == null || detalle.idDetalle == null || detalle.idDetalle <= 0) {
            return;
        }
        if (detalle.esEliminado() || detalle.esSegmentado()) {
            mostrarAdvertencia("Acción no permitida", "No se puede editar un detalle segmentado eliminado o segmentado.");
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Editar detalle segmentado");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        ComboBox<String> cbUbicacion = new ComboBox<>();
        cbUbicacion.setEditable(false);

        try (Connection conn = new Conexion().conectar()) {
            if (conn != null) {
                try (PreparedStatement ps = conn.prepareStatement("SELECT nombre FROM ubicaciones WHERE estado = 'activo' ORDER BY nombre")) {
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            cbUbicacion.getItems().add(valorTexto(rs.getObject(1)));
                        }
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        if (!valorTexto(detalle.ubicacion).isBlank()) {
            cbUbicacion.setValue(valorTexto(detalle.ubicacion));
        }

        VBox contenido = new VBox(8,
                new Label("Ubicación:"),
                cbUbicacion
        );
        dialog.getDialogPane().setContent(contenido);

        dialog.showAndWait().ifPresent(respuesta -> {
            if (respuesta != ButtonType.OK) {
                return;
            }
            String ubicacion = cbUbicacion.getValue();
            if (ubicacion == null || ubicacion.isBlank()) {
                mostrarAdvertencia("Campo requerido", "Selecciona una ubicación.");
                return;
            }
            try (Connection conn = new Conexion().conectar()) {
                if (conn == null) {
                    return;
                }
                Map<String, String> columnasDetalleArticulo = obtenerColumnas(conn, "detalleArticulo");
                Map<String, String> columnasUbicacion = obtenerColumnas(conn, "ubicaciones");

                String colDetalleId = resolverColumna(columnasDetalleArticulo, "idDetalle", "id", "id_detalle");
                String colDetalleUbicacion = resolverColumna(columnasDetalleArticulo, "ubicacion", "idUbicacion", "id_ubicacion");
                String colUbicacionId = resolverColumna(columnasUbicacion, "id", "idUbicacion", "ubicacion_id");
                String colUbicacionNombre = resolverColumna(columnasUbicacion, "nombre", "Nombre", "ubicacion");
                if (colDetalleId == null || colDetalleUbicacion == null || colUbicacionId == null || colUbicacionNombre == null) {
                    return;
                }

                Integer idUbicacion = null;
                String sqlUbicacion = "SELECT `" + colUbicacionId + "` AS idUbi FROM ubicaciones WHERE `" + colUbicacionNombre + "` = ? LIMIT 1";
                try (PreparedStatement ps = conn.prepareStatement(sqlUbicacion)) {
                    ps.setString(1, ubicacion);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            idUbicacion = parseInteger(rs.getObject("idUbi"));
                        }
                    }
                }
                if (idUbicacion == null) {
                    mostrarAdvertencia("Ubicación inválida", "No se encontró la ubicación seleccionada.");
                    return;
                }

                String sql = "UPDATE detalleArticulo SET `" + colDetalleUbicacion + "` = ? WHERE `" + colDetalleId + "` = ?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, idUbicacion);
                    ps.setInt(2, detalle.idDetalle);
                    ps.executeUpdate();
                }
                notificarActualizacion();
                cargarDetalles();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    private void eliminarDetalleArticuloSegmentadoSalida(DetalleArticuloSegmentado detalle) {
        if (detalle == null || detalle.idDetalle == null || detalle.idDetalle <= 0) {
            return;
        }
        if (detalle.esEliminado() || detalle.esSegmentado()) {
            mostrarAdvertencia("Acción no permitida", "No se puede eliminar un detalle segmentado eliminado o segmentado.");
            return;
        }

        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Eliminar detalle segmentado");
        confirmacion.setHeaderText(null);
        confirmacion.setContentText("¿Deseas eliminar el detalle segmentado seleccionado?");
        confirmacion.showAndWait().ifPresent(respuesta -> {
            if (respuesta != ButtonType.OK) {
                return;
            }
            try (Connection conn = new Conexion().conectar()) {
                if (conn == null) {
                    return;
                }
                Map<String, String> columnasDetalleArticulo = obtenerColumnas(conn, "detalleArticulo");
                String colDetalleId = resolverColumna(columnasDetalleArticulo, "idDetalle", "id", "id_detalle");
                String colDetalleEstado = resolverColumna(columnasDetalleArticulo, "estado", "Estado");
                if (colDetalleId == null || colDetalleEstado == null) {
                    return;
                }
                String sql = "UPDATE detalleArticulo SET `" + colDetalleEstado + "` = ? WHERE `" + colDetalleId + "` = ?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, "eliminado");
                    ps.setInt(2, detalle.idDetalle);
                    ps.executeUpdate();
                }
                notificarActualizacion();
                cargarDetalles();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    private Label crearEtiquetaTituloSegmentado(String titulo) {
        Label label = new Label(titulo);
        label.setStyle("-fx-font-size: 12; -fx-text-fill: #2c3e50; -fx-font-weight: bold; -fx-font-family: 'Segoe UI', Arial, sans-serif;");
        return label;
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
        if (articulo.esDetalleSalida()) {
            if (articulo.esEliminado() || articulo.esSegmentado()) {
                mostrarAdvertencia("Acción no permitida", "En salidas no se puede editar un artículo eliminado o segmentado.");
                return;
            }
        } else if (articulo.esPendiente() || articulo.esVendido()) {
            mostrarAdvertencia("Acción no permitida", "No se puede editar un artículo con estado pendiente o vendido.");
            return;
        }
        if (articulo.esSegmentado() && articulo.esDetalleEntrada() && articulo.tieneDetalleArticuloPendienteOVendido()) {
            mostrarAdvertencia("Acción no permitida",
                    "No se puede editar un artículo segmentado cuando tiene detalles en estado pendiente o vendido.");
            return;
        }
        boolean edicionSegmentado = articulo.esSegmentado();

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(edicionSegmentado ? "Editar artículo segmentado" : "Editar artículo");

        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);

        // Configurar el DialogPane para evitar el espacio gris inferior
        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.setPadding(new Insets(0));
        dialogPane.setStyle("-fx-background-color: white; -fx-border-color: white;");

        VBox mainContainer = new VBox();
        mainContainer.setStyle("-fx-background-color: white;");
        mainContainer.setPadding(new Insets(0));

        // Título superior
        Label lblTitulo = new Label(edicionSegmentado ? "Editar artículo segmentado" : "Editar artículo");
        lblTitulo.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-padding: 15 0 10 0;");
        lblTitulo.setAlignment(Pos.CENTER);
        lblTitulo.setMaxWidth(Double.MAX_VALUE);
        VBox.setMargin(lblTitulo, new Insets(10, 0, 10, 0));

        // Contenido central - Campos del formulario
        VBox contenido = new VBox(15);
        contenido.setPadding(new Insets(0, 20, 0, 20));
        contenido.setStyle("-fx-background-color: white;");

        // ============ ORGANIZACIÓN EN 3 FILAS DE 2 CAMPOS ============

        // Fila 1: Lote y Caducidad
        HBox fila1 = new HBox(15);
        fila1.setAlignment(Pos.CENTER_LEFT);

        VBox vboxLote = new VBox(5);
        Label lblLote = new Label("Lote:");
        TextField txtLote = new TextField(valorTexto(articulo.lote));
        txtLote.setPrefWidth(180);
        vboxLote.getChildren().addAll(lblLote, txtLote);
        HBox.setHgrow(vboxLote, Priority.ALWAYS);

        VBox vboxCaducidad = new VBox(5);
        Label lblCaducidad = new Label("Caducidad:");
        DatePicker dpCaducidad = new DatePicker();
        dpCaducidad.setEditable(true);
        dpCaducidad.getEditor().setDisable(false);
        dpCaducidad.getEditor().setStyle("-fx-opacity: 1.0; -fx-background-color: white;");
        dpCaducidad.setPromptText("yyyy/MM/dd");
        configurarDatePickerEditable(dpCaducidad);
        dpCaducidad.setPrefWidth(180);

        // Establecer fecha si existe
        String caducidadTexto = valorTexto(articulo.caducidad);
        if (!caducidadTexto.isBlank()) {
            try {
                LocalDate fecha = parsearFechaCaducidadTexto(caducidadTexto);
                if (fecha != null) {
                    dpCaducidad.setValue(fecha);
                }
            } catch (Exception e) {
                System.err.println("Error parsing date: " + caducidadTexto);
            }
        }

        vboxCaducidad.getChildren().addAll(lblCaducidad, dpCaducidad);
        HBox.setHgrow(vboxCaducidad, Priority.ALWAYS);

        fila1.getChildren().addAll(vboxLote, vboxCaducidad);

        // Fila 2: Presentación y Factor
        HBox fila2 = new HBox(15);
        fila2.setAlignment(Pos.CENTER_LEFT);

        VBox vboxPresentacion = new VBox(5);
        Label lblPresentacion = new Label("Presentación:");
        ComboBox<String> cbPresentacion = new ComboBox<>(presentaciones);
        cbPresentacion.setPrefWidth(180);

        // Establecer presentación actual
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

        vboxPresentacion.getChildren().addAll(lblPresentacion, cbPresentacion);
        HBox.setHgrow(vboxPresentacion, Priority.ALWAYS);

        VBox vboxFactor = new VBox(5);
        Label lblFactor = new Label("Factor:");
        TextField txtFactor = new TextField(valorTexto(articulo.factor));
        txtFactor.setPrefWidth(180);
        vboxFactor.getChildren().addAll(lblFactor, txtFactor);
        HBox.setHgrow(vboxFactor, Priority.ALWAYS);

        fila2.getChildren().addAll(vboxPresentacion, vboxFactor);

        // Fila 3: Ubicación (ocupa el ancho completo)
        HBox fila3 = new HBox();
        fila3.setAlignment(Pos.CENTER_LEFT);

        VBox vboxUbicacion = new VBox(5);
        Label lblUbicacion = new Label("Ubicación:");
        ComboBox<String> cbUbicacion = new ComboBox<>();
        cbUbicacion.setItems(FXCollections.observableArrayList(obtenerUbicacionesActivas()));
        cbUbicacion.setEditable(true);
        cbUbicacion.setPrefWidth(375); // Más ancho para ocupar dos columnas

        String ubicacionActual = valorTexto(articulo.ubicacion);
        if (!ubicacionActual.isBlank() && !"Sin ubicación".equalsIgnoreCase(ubicacionActual)) {
            cbUbicacion.setValue(ubicacionActual);
        }

        vboxUbicacion.getChildren().addAll(lblUbicacion, cbUbicacion);
        HBox.setHgrow(vboxUbicacion, Priority.ALWAYS);

        // Espaciador a la derecha para mantener la alineación
        Region espaciadorUbicacion = new Region();
        HBox.setHgrow(espaciadorUbicacion, Priority.ALWAYS);

        fila3.getChildren().addAll(vboxUbicacion, espaciadorUbicacion);

        // Agregar todas las filas al contenido
        if (edicionSegmentado) {
            contenido.getChildren().add(fila1);
        } else {
            contenido.getChildren().addAll(fila1, fila2, fila3);
        }

        // ============ BOTONES EN LA PARTE INFERIOR ============

        // Contenedor de botones inferior
        HBox contenedorBotones = new HBox(15);
        contenedorBotones.setAlignment(Pos.CENTER);
        contenedorBotones.setPadding(new Insets(20));
        contenedorBotones.setStyle("-fx-background-color: white; -fx-border-color: #eee; -fx-border-width: 1 0 0 0;");

        // Ocultar el ButtonBar original
        ButtonBar buttonBar = (ButtonBar) dialog.getDialogPane().lookup(".button-bar");
        if (buttonBar != null) {
            buttonBar.setVisible(false);
            buttonBar.setManaged(false);
            buttonBar.setPrefHeight(0);
            buttonBar.setMinHeight(0);
            buttonBar.setMaxHeight(0);
        }

        // Ocultar también los botones individuales del ButtonBar
        Button btnOkOriginal = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        Button btnCancelOriginal = (Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
        if (btnOkOriginal != null) {
            btnOkOriginal.setVisible(false);
            btnOkOriginal.setManaged(false);
        }
        if (btnCancelOriginal != null) {
            btnCancelOriginal.setVisible(false);
            btnCancelOriginal.setManaged(false);
        }

        Button btnAceptar = new Button("Aceptar");
        btnAceptar.setStyle("-fx-background-color: #333; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 8 20; -fx-background-radius: 4;");
        btnAceptar.setPrefWidth(120);
        btnAceptar.setOnAction(e -> {
            // Validar campos antes de aceptar
            if (!edicionSegmentado) {
                if (cbUbicacion.getValue() == null || cbUbicacion.getValue().isEmpty()) {
                    mostrarAdvertencia("Campo requerido", "La ubicación es requerida.");
                    return;
                }

                if (cbPresentacion.getValue() == null || cbPresentacion.getValue().isEmpty()) {
                    mostrarAdvertencia("Campo requerido", "La presentación es requerida.");
                    return;
                }

                if (txtFactor.getText() == null || txtFactor.getText().isEmpty()) {
                    mostrarAdvertencia("Campo requerido", "El factor es requerido.");
                    return;
                }
            }

            // Si pasa validación, establecer resultado OK
            dialog.setResult(ButtonType.OK);
            dialog.close();
        });

        contenedorBotones.getChildren().addAll(btnAceptar);

        // Agregar todos los componentes al contenedor principal
        mainContainer.getChildren().addAll(lblTitulo, contenido, contenedorBotones);

        // Configurar el crecimiento del contenido
        VBox.setVgrow(contenido, Priority.ALWAYS);

        // Establecer el contenido del diálogo
        dialog.getDialogPane().setContent(mainContainer);

        // Configurar tamaño del diálogo
        dialog.getDialogPane().setPrefWidth(380);
        dialog.getDialogPane().setPrefHeight(430);

        // Hacer el diálogo modal
        dialog.initModality(Modality.APPLICATION_MODAL);
        if (btnCerrar != null && btnCerrar.getScene() != null) {
            dialog.initOwner(btnCerrar.getScene().getWindow());
        }

        dialog.showAndWait().ifPresent(respuesta -> {
            if (respuesta != ButtonType.OK) {
                return;
            }

            try (Connection conn = new Conexion().conectar()) {
                if (conn == null) {
                    return;
                }

                Integer ubicacionId = null;
                if (!edicionSegmentado) {
                    String ubicacionTexto = cbUbicacion.getValue();
                    if (ubicacionTexto != null && !ubicacionTexto.isBlank()) {
                        try (PreparedStatement ps = conn.prepareStatement(
                                "SELECT id FROM ubicaciones WHERE nombre = ? AND estado = 'activo'")) {
                            ps.setString(1, ubicacionTexto.trim());
                            try (ResultSet rs = ps.executeQuery()) {
                                if (rs.next()) {
                                    ubicacionId = rs.getInt(1);
                                }
                            }
                        }
                        if (ubicacionId == null) {
                            mostrarAdvertencia("Ubicación inválida", "No se encontró la ubicación ingresada.");
                            return;
                        }
                    }
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

                StringBuilder sql = new StringBuilder("UPDATE articulo SET ");
                List<Object> valores = new ArrayList<>();

                agregarCampoActualizacion(sql, valores, colLote, valorTexto(txtLote.getText()));

                LocalDate fechaCaducidadLocal = parsearFechaCaducidadEditable(dpCaducidad);
                if (fechaCaducidadLocal == null && !dpCaducidad.getEditor().getText().trim().isEmpty()) {
                    mostrarAdvertencia("Caducidad inválida", "Ingresa una fecha válida con formato yyyy/MM/dd (o yyyy-MM-dd) o déjala vacía.");
                    return;
                }
                java.sql.Date fechaCaducidad = fechaCaducidadLocal != null
                        ? java.sql.Date.valueOf(fechaCaducidadLocal)
                        : null;
                agregarCampoActualizacion(sql, valores, colCaducidad, fechaCaducidad);

                if (!edicionSegmentado) {
                    agregarCampoActualizacion(sql, valores, colUbicacion, ubicacionId);

                    String presentacionSeleccionada = cbPresentacion.getValue();
                    if (presentacionSeleccionada == null && !presentaciones.isEmpty()) {
                        presentacionSeleccionada = presentaciones.get(0);
                    }
                    Integer factorSeleccionado = parseInteger(txtFactor.getText());

                    String presentacionActualArticulo = valorTexto(articulo.presentacion);
                    boolean cambiarPresentacion = !presentacionActualArticulo.equalsIgnoreCase(valorTexto(presentacionSeleccionada));
                    Integer factorActualArticulo = parseInteger(articulo.factor);
                    boolean cambiarFactor = factorSeleccionado != null && !factorSeleccionado.equals(factorActualArticulo);
                    boolean actualizarPresentacionMasiva = false;

                    if ((cambiarPresentacion || cambiarFactor) && articulo.esDetalleEntrada() && articulo.detalleEntradaId != null) {
                        int articulosSincronizados = contarArticulosPorDetalleEntrada(conn, articulo.detalleEntradaId);
                        if (articulosSincronizados > 1) {
                            Alert alertaMasiva = new Alert(Alert.AlertType.CONFIRMATION);
                            alertaMasiva.setTitle("Actualizar artículos sincronizados");
                            alertaMasiva.setHeaderText(null);
                            alertaMasiva.setContentText("Se modificarán presentación y/o factor de múltiples artículos del mismo detalle de entrada. ¿Deseas continuar?");
                            ButtonType respuestaConfirmacion = alertaMasiva.showAndWait().orElse(ButtonType.CANCEL);
                            if (respuestaConfirmacion != ButtonType.OK) {
                                return;
                            }
                            actualizarPresentacionMasiva = true;
                        }
                    }

                    agregarCampoActualizacion(sql, valores, colPresentacion, valorTexto(presentacionSeleccionada));

                    agregarCampoActualizacion(sql, valores, colFactor, factorSeleccionado);

                    if (actualizarPresentacionMasiva) {
                        actualizarCamposDetalleEntrada(conn, articulo, colPresentacion, valorTexto(presentacionSeleccionada),
                                colFactor, factorSeleccionado);
                    }
                }

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
                mostrarAdvertencia("Error", "No se pudo actualizar el artículo: " + e.getMessage());
            }
        });
    }

    private List<String> obtenerUbicacionesActivas() {
        // Esta implementación puede variar según tu estructura
        // Aquí un ejemplo básico:
        List<String> ubicaciones = new ArrayList<>();
        try (Connection conn = new Conexion().conectar()) {
            if (conn != null) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT nombre FROM ubicaciones WHERE estado = 'activo' ORDER BY nombre")) {
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            ubicaciones.add(rs.getString("nombre"));
                        }
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return ubicaciones;
    }

    private void eliminarArticulo(DetalleArticulo articulo) {
        if (articulo == null || articulo.idArticulo <= 0) {
            return;
        }
        if (articulo.esDetalleSalida()) {
            if (articulo.esEliminado() || articulo.esSegmentado()) {
                mostrarAdvertencia("Acción no permitida", "En salidas no se puede eliminar un artículo eliminado o segmentado.");
                return;
            }
        } else if (articulo.esPendiente() || articulo.esVendido()) {
            mostrarAdvertencia("Acción no permitida", "No se puede eliminar un artículo con estado pendiente o vendido.");
            return;
        }
        if (articulo.esSegmentado() && articulo.esDetalleEntrada() && articulo.tieneDetalleArticuloPendienteOVendido()) {
            mostrarAdvertencia("Acción no permitida",
                    "No se puede eliminar un artículo segmentado cuando tiene detalles en estado pendiente o vendido.");
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
                    actualizarEntradaAsociadaADetalleSalida(conn, articulo);
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
            } else if (esEntrada) {
                actualizarTotalesEntradaPorPrecio(conn, linea, nuevoPrecio, precioIva, cantidad);
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

    private void actualizarTotalesEntradaPorPrecio(Connection conn, DetalleLinea linea, BigDecimal nuevoPrecioUnitario,
                                                   BigDecimal nuevoPrecioIva, BigDecimal cantidad) throws SQLException {
        if (linea == null || linea.idDetalle <= 0) {
            return;
        }
        Map<String, String> columnasDetalle = obtenerColumnas(conn, "detalle_Entrada");
        String colDetalleId = resolverColumna(columnasDetalle, "idDetalleEntrada", "id", "id_detalle_entrada");
        String colClaveEntrada = resolverColumna(columnasDetalle, "claveEntrada", "idEntrada", "id_entrada", "entrada_id");
        if (colDetalleId == null || colClaveEntrada == null) {
            return;
        }

        Integer claveEntradaId = null;
        String sqlDetalle = "SELECT `" + colClaveEntrada + "` AS claveEntrada FROM detalle_Entrada WHERE `"
                + colDetalleId + "` = ?";
        try (PreparedStatement ps = conn.prepareStatement(sqlDetalle)) {
            ps.setInt(1, linea.idDetalle);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    claveEntradaId = rs.getInt("claveEntrada");
                }
            }
        }

        if (claveEntradaId == null || claveEntradaId <= 0) {
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

        Map<String, String> columnasEntrada = obtenerColumnas(conn, "entradas");
        String colEntradaId = resolverColumna(columnasEntrada, "idEntrada", "id", "id_entrada");
        String colPrecioNeto = resolverColumna(columnasEntrada, "precioNetoEntrada", "precioNeto", "precio_neto");
        String colPrecioTotal = resolverColumna(columnasEntrada, "precioTotalEntrada", "precioTotal", "precio_total");
        if (colEntradaId == null || (colPrecioNeto == null && colPrecioTotal == null)) {
            return;
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
                colPrecioTotal != null ? "`" + colPrecioTotal + "`" : "NULL",
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
            BigDecimal nuevoNeto = precioNetoActual.add(deltaNeto);
            if (nuevoNeto.compareTo(BigDecimal.ZERO) < 0) {
                nuevoNeto = BigDecimal.ZERO;
            }
            agregarCampoActualizacion(updateEntrada, valoresEntrada, colPrecioNeto,
                    nuevoNeto.setScale(2, RoundingMode.HALF_UP));
        }
        if (colPrecioTotal != null && precioTotalActual != null) {
            BigDecimal nuevoTotal = precioTotalActual.add(deltaTotal);
            if (nuevoTotal.compareTo(BigDecimal.ZERO) < 0) {
                nuevoTotal = BigDecimal.ZERO;
            }
            agregarCampoActualizacion(updateEntrada, valoresEntrada, colPrecioTotal,
                    nuevoTotal.setScale(2, RoundingMode.HALF_UP));
        }
        if (valoresEntrada.isEmpty()) {
            return;
        }
        updateEntrada.append(" WHERE `").append(colEntradaId).append("` = ?");
        valoresEntrada.add(claveEntradaId);

        try (PreparedStatement ps = conn.prepareStatement(updateEntrada.toString())) {
            for (int i = 0; i < valoresEntrada.size(); i++) {
                ps.setObject(i + 1, valoresEntrada.get(i));
            }
            ps.executeUpdate();
        }
    }

    private LocalDate parsearFechaCaducidadEditable(DatePicker datePicker) {
        if (datePicker == null) {
            return null;
        }
        String texto = datePicker.getEditor() != null ? valorTexto(datePicker.getEditor().getText()).trim() : "";
        if (texto.isEmpty()) {
            datePicker.setValue(null);
            return null;
        }
        LocalDate fecha = parsearFechaCaducidadTexto(texto);
        if (fecha != null) {
            datePicker.setValue(fecha);
        }
        return fecha;
    }

    private LocalDate parsearFechaCaducidadTexto(String texto) {
        String limpio = valorTexto(texto).trim();
        if (limpio.isEmpty()) {
            return null;
        }
        DateTimeFormatter formatoSlash = DateTimeFormatter.ofPattern("yyyy/MM/dd");
        DateTimeFormatter formatoGuion = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        try {
            return LocalDate.parse(limpio, formatoSlash);
        } catch (DateTimeParseException ignored) {
        }
        try {
            return LocalDate.parse(limpio, formatoGuion);
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private void configurarDatePickerEditable(DatePicker datePicker) {
        if (datePicker == null) {
            return;
        }
        DateTimeFormatter formato = DateTimeFormatter.ofPattern("yyyy/MM/dd");
        datePicker.setConverter(new javafx.util.StringConverter<LocalDate>() {
            @Override
            public String toString(LocalDate object) {
                return object != null ? object.format(formato) : "";
            }

            @Override
            public LocalDate fromString(String string) {
                return parsearFechaCaducidadTexto(string);
            }
        });
    }

    private int contarArticulosPorDetalleEntrada(Connection conn, Integer detalleEntradaId) throws SQLException {
        if (conn == null || detalleEntradaId == null || detalleEntradaId <= 0) {
            return 0;
        }
        Map<String, String> columnasArticulo = obtenerColumnas(conn, "articulo");
        String colDetalleEntrada = resolverColumna(columnasArticulo, "idDetalleEntrada", "id_detalle_entrada",
                "detalleEntrada", "detalle_entrada", "detalle_entrada_id");
        if (colDetalleEntrada == null) {
            return 0;
        }
        String sql = "SELECT COUNT(*) FROM articulo WHERE `" + colDetalleEntrada + "` = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, detalleEntradaId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    private void actualizarCamposDetalleEntrada(Connection conn, DetalleArticulo articulo,
                                                String colPresentacion, String presentacion,
                                                String colFactor, Integer factor) throws SQLException {
        if (conn == null || articulo == null || articulo.detalleEntradaId == null || articulo.detalleEntradaId <= 0
                || (colPresentacion == null && colFactor == null)) {
            return;
        }
        Map<String, String> columnasArticulo = obtenerColumnas(conn, "articulo");
        String colDetalleEntrada = resolverColumna(columnasArticulo, "idDetalleEntrada", "id_detalle_entrada",
                "detalleEntrada", "detalle_entrada", "detalle_entrada_id");
        if (colDetalleEntrada == null) {
            return;
        }
        StringBuilder sql = new StringBuilder("UPDATE articulo SET ");
        List<Object> valores = new ArrayList<>();
        agregarCampoActualizacion(sql, valores, colPresentacion, presentacion);
        agregarCampoActualizacion(sql, valores, colFactor, factor);
        if (valores.isEmpty()) {
            return;
        }
        sql.append(" WHERE `").append(colDetalleEntrada).append("` = ?");
        valores.add(articulo.detalleEntradaId);
        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < valores.size(); i++) {
                ps.setObject(i + 1, valores.get(i));
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

    private void actualizarEntradaAsociadaADetalleSalida(Connection conn, DetalleArticulo articulo) throws SQLException {
        if (conn == null || articulo == null) {
            return;
        }

        Map<String, String> columnasArticulo = obtenerColumnas(conn, "articulo");
        Map<String, String> columnasDetalleEntrada = obtenerColumnas(conn, "detalle_Entrada");
        Map<String, String> columnasEntrada = obtenerColumnas(conn, "entradas");

        String colArticuloDetalleEntrada = resolverColumna(columnasArticulo, "idDetalleEntrada", "id_detalle_entrada",
                "detalleEntrada", "detalle_entrada", "detalle_entrada_id");
        String colDetalleEntradaId = resolverColumna(columnasDetalleEntrada, "idDetalleEntrada", "id", "id_detalle_entrada");
        String colDetalleEntradaClave = resolverColumna(columnasDetalleEntrada, "claveEntrada", "idEntrada", "id_entrada", "entrada_id");
        String colEntradaId = resolverColumna(columnasEntrada, "idEntrada", "id", "id_entrada");
        String colEntradaEstado = resolverColumna(columnasEntrada, "Estado", "estado");

        if (colArticuloDetalleEntrada == null || colDetalleEntradaId == null
                || colDetalleEntradaClave == null || colEntradaId == null || colEntradaEstado == null) {
            return;
        }

        Integer entradaId = null;
        if (articulo.detalleEntradaId != null && articulo.detalleEntradaId > 0) {
            String sql = "SELECT `" + colDetalleEntradaClave + "` AS entradaId FROM detalle_Entrada WHERE `"
                    + colDetalleEntradaId + "` = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, articulo.detalleEntradaId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        entradaId = parseInteger(rs.getObject("entradaId"));
                    }
                }
            }
        } else if (articulo.detalleSalidaId != null && articulo.detalleSalidaId > 0) {
            String colArticuloDetalleSalida = resolverColumna(columnasArticulo, "idDetalleSalida", "id_detalle_salida",
                    "detalleSalida", "detalle_salida", "detalle_salida_id");
            if (colArticuloDetalleSalida != null) {
                String sql = "SELECT d.`" + colDetalleEntradaClave + "` AS entradaId "
                        + "FROM articulo a "
                        + "JOIN detalle_Entrada d ON a.`" + colArticuloDetalleEntrada + "` = d.`" + colDetalleEntradaId + "` "
                        + "WHERE a.`" + colArticuloDetalleSalida + "` = ? LIMIT 1";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, articulo.detalleSalidaId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            entradaId = parseInteger(rs.getObject("entradaId"));
                        }
                    }
                }
            }
        }

        if (entradaId == null || entradaId <= 0) {
            return;
        }

        String estadoActualEntrada = obtenerEstadoEntrada(conn, entradaId);
        if (estadoActualEntrada != null && "finalizado".equalsIgnoreCase(estadoActualEntrada.trim())) {
            String sqlUpdate = "UPDATE entradas SET `" + colEntradaEstado + "` = ? WHERE `" + colEntradaId + "` = ?";
            try (PreparedStatement ps = conn.prepareStatement(sqlUpdate)) {
                ps.setString(1, "disponible");
                ps.setInt(2, entradaId);
                ps.executeUpdate();
            }
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

    private String placeholders(int total) {
        if (total <= 0) {
            return "";
        }
        return String.join(", ", Collections.nCopies(total, "?"));
    }

    private boolean esSalidaCancelada(Connection conn, Integer salidaId) throws SQLException {
        String estado = obtenerEstadoSalida(conn, salidaId);
        return estado != null && estado.equalsIgnoreCase("cancelado");
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
        private final boolean tieneDetalleArticuloPendienteOVendido;
        private final List<DetalleArticuloSegmentado> detallesSegmentados = new ArrayList<>();

        private DetalleArticulo(String ubicacion, String lote, String caducidad,
                                String presentacion, String factor, String estado, int idArticulo,
                                Object detalleEntradaId, Object detalleSalidaId,
                                boolean tieneDetalleArticuloPendienteOVendido) {
            this.ubicacion = ubicacion;
            this.lote = lote;
            this.caducidad = caducidad;
            this.presentacion = presentacion;
            this.factor = factor;
            this.estado = estado;
            this.idArticulo = idArticulo;
            this.detalleEntradaId = parseInteger(detalleEntradaId);
            this.detalleSalidaId = parseInteger(detalleSalidaId);
            this.tieneDetalleArticuloPendienteOVendido = tieneDetalleArticuloPendienteOVendido;
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

        private boolean esEliminado() {
            if (estado == null) {
                return false;
            }
            return "eliminado".equalsIgnoreCase(estado.trim());
        }

        private boolean esSegmentado() {
            if (estado == null) {
                return false;
            }
            return "segmentado".equalsIgnoreCase(estado.trim());
        }

        private boolean esDetalleEntrada() {
            return detalleEntradaId != null;
        }

        private boolean esDetalleSalida() {
            return detalleSalidaId != null;
        }

        private boolean tieneDetalleArticuloPendienteOVendido() {
            return tieneDetalleArticuloPendienteOVendido;
        }

        private boolean tieneDetallesSegmentados() {
            return detallesSegmentados != null && !detallesSegmentados.isEmpty();
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

    private static class DetalleArticuloSegmentado {
        private final Integer idDetalle;
        private final String ubicacion;
        private final String lote;
        private final String caducidad;
        private final String presentacion;
        private final String factor;
        private final String estado;

        private DetalleArticuloSegmentado(Integer idDetalle, String ubicacion, String lote, String caducidad,
                                          String presentacion, String factor, String estado) {
            this.idDetalle = idDetalle;
            this.ubicacion = ubicacion;
            this.lote = lote;
            this.caducidad = caducidad;
            this.presentacion = presentacion;
            this.factor = factor;
            this.estado = estado;
        }

        private boolean esEliminado() {
            return estado != null && "eliminado".equalsIgnoreCase(estado.trim());
        }

        private boolean esSegmentado() {
            return estado != null && "segmentado".equalsIgnoreCase(estado.trim());
        }
    }
}
