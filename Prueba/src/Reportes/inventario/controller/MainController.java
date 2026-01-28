package Reportes.inventario.controller;

import Compartido.controller.encabezadoController;
import Compartido.controller.navbarController;
import Compartido.exportar.exportador;
import Compartido.helper.SelectorColumnasPopup;
import Compartido.helper.SelectorOrdenPopup;
import Reportes.inventario.model.ItemInventario;
import conexion.Conexion;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MainController {

    @FXML private StackPane root;
    @FXML private BorderPane paneNavbar;
    @FXML private VBox navbar;
    @FXML private VBox contenedor;
    @FXML private Pane overlayPane;

    @FXML private Label lblQuitar;
    @FXML private Label lblOrdenar;
    @FXML private Label lblExportar;
    @FXML private Region expansorDetalles;

    @FXML private Region expansor;
    @FXML private Label lblVista;
    @FXML private Label lblDescargar;
    @FXML private javafx.scene.control.CheckBox chkInventarioDetallado;
    @FXML private ComboBox<String> comboFiltro;
    @FXML private ComboBox<String> comboValor;
    @FXML private HBox contenedorFiltros;

    @FXML private VBox contenedorTabla;
    @FXML private TableView<ItemInventario> contenidoTabla;
    @FXML private TableColumn<ItemInventario, String> colClaveProducto;
    @FXML private TableColumn<ItemInventario, String> colCantidad;
    @FXML private TableColumn<ItemInventario, String> colProducto;
    @FXML private TableColumn<ItemInventario, String> colMarca;
    @FXML private TableColumn<ItemInventario, String> colCategoria;
    @FXML private TableColumn<ItemInventario, String> colMaterial;
    @FXML private TableColumn<ItemInventario, String> colUnidad;
    @FXML private TableColumn<ItemInventario, String> colPresentacion;
    @FXML private TableColumn<ItemInventario, String> colFactor;
    @FXML private TableColumn<ItemInventario, String> colLote;
    @FXML private TableColumn<ItemInventario, String> colCaducidad;
    @FXML private TableColumn<ItemInventario, String> colUbicacion;
    @FXML private TableColumn<ItemInventario, String> colDescripcion;
    @FXML private TableColumn<ItemInventario, String> colInventarioMinimo;

    @FXML private encabezadoController paneNavbarController;

    private final ObservableList<ItemInventario> itemsInventario = FXCollections.observableArrayList();
    private final ObservableList<ItemInventario> itemsInventarioOriginal = FXCollections.observableArrayList();
    private final Map<TableColumn<ItemInventario, ?>, Boolean> visibilidadResumen = new HashMap<>();
    private final Map<TableColumn<ItemInventario, ?>, Boolean> visibilidadDetallado = new HashMap<>();
    private static final List<String> PRESENTACIONES_COMPRA = List.of(
            "paquete", "pz", "caja", "bolsa", "pieza", "rollo", "litro", "kilogramo", "metro", "unidad"
    );
    private String criterioOrden = "id";
    private String direccionOrden = "asc";
    private final List<Filtro> filtrosActivos = new ArrayList<>();
    private boolean restaurandoFiltros = false;


    @FXML
    public void initialize() {
        Platform.runLater(() -> {

            try {
                // Cargar el navbar desde el fx:include
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/Compartido/view/navbar.fxml"));
                VBox navbarLoaded = loader.load();

                // Obtener el controller del navbar
                navbarController navbarCtrl = loader.getController();

                // Pasar el overlayPane al navbarController
                navbarCtrl.setOverlayPane(overlayPane);

                // Reemplazar el contenido del fx:include con el cargado
                navbar.getChildren().setAll(navbarLoaded);

            } catch (IOException e) {
                e.printStackTrace();
            }

            SplitPane.setResizableWithParent(navbar, false);
            SplitPane.setResizableWithParent(contenedor, true);

            //Navbar superior (header)
            paneNavbar.prefHeightProperty().bind(root.heightProperty().multiply(0.1));
            paneNavbar.prefWidthProperty().bind(root.widthProperty().multiply(0.9));

            // Navbar lateral (menú)
            navbar.prefWidthProperty().bind(root.widthProperty().multiply(0.15));
            navbar.prefHeightProperty().bind(root.heightProperty().multiply(0.9));

            // Center - contenedor general
            contenedor.prefHeightProperty().bind(root.heightProperty().multiply(0.75));

            // Barra de opciones
            HBox.setHgrow(expansor, Priority.ALWAYS);
            expansor.setMinWidth(10);
            lblQuitar.setMinWidth(Region.USE_PREF_SIZE);
            lblOrdenar.setMinWidth(Region.USE_PREF_SIZE);
            lblExportar.setMinWidth(Region.USE_PREF_SIZE);
            HBox.setHgrow(expansorDetalles, Priority.ALWAYS);
            expansorDetalles.setMinWidth(10);
            lblVista.setMinWidth(Region.USE_PREF_SIZE);
            lblDescargar.setMinWidth(Region.USE_PREF_SIZE);

            // Tabla
            contenedorTabla.prefHeightProperty().bind(contenedor.heightProperty().multiply(0.81));
            contenidoTabla.prefHeightProperty().bind(contenedorTabla.heightProperty().multiply(0.9));

            paneNavbarController.setTitulo("Inventario", "#ffffff");

            contenedorTabla.widthProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal.doubleValue() > 0) {
                    // Pequeño delay para asegurar que todo esté estable
                    Platform.runLater(() -> {
                        Platform.runLater(this::actualizarPoliticaRedimensionamiento);
                    });
                }
            });

            // Escuchar cambios en el tamaño de la tabla
            contenidoTabla.widthProperty().addListener((obs, oldVal, newVal) -> {
               System.out.println("escuchando");
                if (newVal.doubleValue() > 0 && newVal.doubleValue() != oldVal.doubleValue()) {
                    Platform.runLater(this::actualizarPoliticaRedimensionamiento);
                }
            });

            configurarColumnasTabla();
            configurarInventarioDetallado();
            configurarFiltros();
            cargarInventarioDisponible(false);
            configurarDobleClick();
        });
    }

    private void configurarDobleClick() {
        if (contenidoTabla == null) {
            return;
        }

        contenidoTabla.setRowFactory(table -> {
            javafx.scene.control.TableRow<ItemInventario> row = new javafx.scene.control.TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    if (chkInventarioDetallado != null && chkInventarioDetallado.isSelected()) {
                        abrirEdicionArticulo(row.getItem());
                    } else {
                        abrirDetalleInventario(row.getItem());
                    }
                }
            });
            return row;
        });
    }

    private void abrirDetalleInventario(ItemInventario item) {
        if (item == null) {
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Reportes/inventario/view/detalle_view.fxml"));
            Pane rootDetalle = loader.load();
            DetalleInventarioController controller = loader.getController();
            controller.setItemInventario(item);
            controller.setOnRefresh(() -> cargarInventarioDisponible(chkInventarioDetallado.isSelected()));

            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.setTitle("Detalle de inventario");
            stage.setScene(new javafx.scene.Scene(rootDetalle));
            controller.setStage(stage);
            stage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void abrirEdicionArticulo(ItemInventario item) {
        if (item == null || item.getIdArticulo() == null || item.getIdArticulo().isBlank()) {
            return;
        }
        Integer idArticulo = parseInteger(item.getIdArticulo());
        if (idArticulo == null || idArticulo <= 0) {
            return;
        }
        javafx.scene.control.Dialog<javafx.scene.control.ButtonType> dialog = new javafx.scene.control.Dialog<>();
        dialog.setTitle("Editar artículo");
        javafx.scene.control.ButtonType deleteType = new javafx.scene.control.ButtonType(
                "Eliminar", javafx.scene.control.ButtonBar.ButtonData.LEFT);
        dialog.getDialogPane().getButtonTypes().addAll(deleteType, javafx.scene.control.ButtonType.OK,
                javafx.scene.control.ButtonType.CANCEL);

        List<String> ubicacionesActivas = obtenerUbicacionesActivas();
        javafx.scene.control.ComboBox<String> cbUbicacion = new javafx.scene.control.ComboBox<>();
        cbUbicacion.setItems(FXCollections.observableArrayList(ubicacionesActivas));
        cbUbicacion.setPromptText("Selecciona ubicación");
        String ubicacionActual = item.getUbicacion();
        if (ubicacionActual != null
                && !ubicacionActual.isBlank()
                && !"Sin ubicación".equalsIgnoreCase(ubicacionActual)
                && ubicacionesActivas.stream().anyMatch(ubicacionActual::equalsIgnoreCase)) {
            cbUbicacion.setValue(ubicacionActual);
        }
        javafx.scene.control.TextField txtLote = new javafx.scene.control.TextField(item.getLote());
        javafx.scene.control.DatePicker dpCaducidad = new javafx.scene.control.DatePicker();
        if (item.getCaducidad() != null && !item.getCaducidad().isBlank()) {
            try {
                dpCaducidad.setValue(java.time.LocalDate.parse(item.getCaducidad().trim()));
            } catch (java.time.format.DateTimeParseException ignored) {
                dpCaducidad.setValue(null);
            }
        }
        javafx.scene.control.ComboBox<String> cbPresentacion = new javafx.scene.control.ComboBox<>();
        cbPresentacion.setItems(FXCollections.observableArrayList(PRESENTACIONES_COMPRA));
        cbPresentacion.setPromptText("Selecciona presentación");
        String presentacionActual = item.getPresentacion();
        if (presentacionActual != null
                && !presentacionActual.isBlank()
                && PRESENTACIONES_COMPRA.stream().anyMatch(presentacionActual::equalsIgnoreCase)) {
            cbPresentacion.setValue(presentacionActual);
        }
        javafx.scene.control.TextField txtFactor = new javafx.scene.control.TextField(item.getFactor());

        VBox contenido = new VBox(8,
                new Label("Ubicación:"), cbUbicacion,
                new Label("Lote:"), txtLote,
                new Label("Caducidad:"), dpCaducidad,
                new Label("Presentación:"), cbPresentacion,
                new Label("Factor:"), txtFactor
        );
        dialog.getDialogPane().setContent(contenido);

        javafx.scene.control.Button deleteButton = (javafx.scene.control.Button) dialog.getDialogPane().lookupButton(deleteType);
        if (deleteButton != null) {
            deleteButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
        }

        dialog.showAndWait().ifPresent(respuesta -> {
            if (respuesta == deleteType) {
                eliminarArticuloInventario(idArticulo);
                return;
            }
            if (respuesta != javafx.scene.control.ButtonType.OK) {
                return;
            }
            String caducidadTexto = dpCaducidad.getValue() != null ? dpCaducidad.getValue().toString() : "";
            String presentacionSeleccionada = cbPresentacion.getValue();
            actualizarArticuloInventario(idArticulo, cbUbicacion.getValue(), txtLote.getText(),
                    caducidadTexto, presentacionSeleccionada != null ? presentacionSeleccionada : "", txtFactor.getText());
        });
    }

    private void actualizarArticuloInventario(int idArticulo, String ubicacionTexto, String lote,
                                              String caducidad, String presentacion, String factor) {
        try (Connection conn = new Conexion().conectar()) {
            if (conn == null) {
                return;
            }

            Integer ubicacionId = null;
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

            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE articulo SET ubicacion = ?, lote = ?, caducidad = ?, presentacion = ?, factor = ? " +
                            "WHERE idArticulo = ?")) {
                if (ubicacionId == null) {
                    ps.setNull(1, java.sql.Types.INTEGER);
                } else {
                    ps.setInt(1, ubicacionId);
                }
                ps.setString(2, lote);
                ps.setString(3, caducidad);
                ps.setString(4, presentacion);
                ps.setString(5, factor);
                ps.setInt(6, idArticulo);
                ps.executeUpdate();
            }
            cargarInventarioDisponible(chkInventarioDetallado.isSelected());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void eliminarArticuloInventario(int idArticulo) {
        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Eliminar artículo");
        confirmacion.setHeaderText(null);
        confirmacion.setContentText("¿Deseas eliminar el artículo seleccionado?");
        confirmacion.showAndWait().ifPresent(respuesta -> {
            if (respuesta != javafx.scene.control.ButtonType.OK) {
                return;
            }
            try (Connection conn = new Conexion().conectar()) {
                if (conn == null) {
                    return;
                }
                conn.setAutoCommit(false);

                try {
                    Map<String, String> columnasArticulo = obtenerColumnas(conn, "articulo");
                    Map<String, String> columnasDetalle = obtenerColumnas(conn, "detalle_Entrada");
                    Map<String, String> columnasEntrada = obtenerColumnas(conn, "entradas");

                    String colArticuloId = resolverColumna(columnasArticulo, "idArticulo", "id", "id_articulo");
                    String colArticuloEstado = resolverColumna(columnasArticulo, "Estado", "estado");
                    String colArticuloDetalleEntrada = resolverColumna(columnasArticulo, "idDetalleEntrada", "id_detalle_entrada",
                            "detalleEntrada", "detalle_entrada", "detalle_entrada_id");

                    String colDetalleId = resolverColumna(columnasDetalle, "idDetalleEntrada", "id", "id_detalle_entrada");
                    String colDetalleClaveEntrada = resolverColumna(columnasDetalle, "claveEntrada", "idEntrada", "id_entrada",
                            "entrada_id");
                    String colDetalleCantidad = resolverColumna(columnasDetalle, "cantidad", "cantidadEntrada");
                    String colDetallePrecioUnitario = resolverColumna(columnasDetalle, "precioUnitario", "precioEntrada",
                            "precio_entrada");
                    String colDetallePrecioIva = resolverColumna(columnasDetalle, "precioIVA", "precioIva", "precio_iva");
                    String colDetallePrecioBruto = resolverColumna(columnasDetalle, "precioBrutoTotal", "precioBruto",
                            "precio_bruto");
                    String colDetallePrecioTotal = resolverColumna(columnasDetalle, "precioTotal", "precio_total");

                    String colEntradaId = resolverColumna(columnasEntrada, "idEntrada", "id", "id_entrada");
                    String colEntradaPrecioNeto = resolverColumna(columnasEntrada, "precioNetoEntrada", "precioNeto",
                            "precio_neto");
                    String colEntradaPrecioTotal = resolverColumna(columnasEntrada, "precioTotalEntrada", "precioTotal",
                            "precio_total");

                    if (colArticuloId == null || colArticuloEstado == null || colArticuloDetalleEntrada == null
                            || colDetalleId == null || colDetalleClaveEntrada == null
                            || colDetalleCantidad == null || colDetallePrecioUnitario == null || colDetallePrecioIva == null) {
                        conn.rollback();
                        return;
                    }

                    Integer detalleEntradaId = null;
                    try (PreparedStatement ps = conn.prepareStatement(
                            "SELECT `" + colArticuloDetalleEntrada + "` AS detalleEntrada FROM articulo WHERE `" +
                                    colArticuloId + "` = ?")) {
                        ps.setInt(1, idArticulo);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) {
                                detalleEntradaId = rs.getInt("detalleEntrada");
                            }
                        }
                    }

                    if (detalleEntradaId == null || detalleEntradaId <= 0) {
                        conn.rollback();
                        return;
                    }

                    try (PreparedStatement ps = conn.prepareStatement(
                            "UPDATE articulo SET `" + colArticuloEstado + "` = ? WHERE `" + colArticuloId + "` = ?")) {
                        ps.setString(1, "eliminado");
                        ps.setInt(2, idArticulo);
                        ps.executeUpdate();
                    }

                    Integer entradaId = null;
                    BigDecimal cantidad = null;
                    BigDecimal precioUnitario = null;
                    BigDecimal precioIva = null;

                    String sqlDetalle = String.format("""
                            SELECT `%s` AS claveEntrada,
                                   `%s` AS cantidad,
                                   `%s` AS precioUnitario,
                                   `%s` AS precioIva
                            FROM detalle_Entrada
                            WHERE `%s` = ?
                            """,
                            colDetalleClaveEntrada,
                            colDetalleCantidad,
                            colDetallePrecioUnitario,
                            colDetallePrecioIva,
                            colDetalleId
                    );
                    try (PreparedStatement ps = conn.prepareStatement(sqlDetalle)) {
                        ps.setInt(1, detalleEntradaId);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) {
                                entradaId = rs.getInt("claveEntrada");
                                cantidad = parseDecimal(rs.getObject("cantidad"));
                                precioUnitario = parseDecimal(rs.getObject("precioUnitario"));
                                precioIva = parseDecimal(rs.getObject("precioIva"));
                            }
                        }
                    }

                    if (cantidad == null || precioUnitario == null || precioIva == null) {
                        conn.rollback();
                        return;
                    }

                    BigDecimal nuevaCantidad = cantidad.subtract(BigDecimal.ONE);
                    if (nuevaCantidad.compareTo(BigDecimal.ZERO) < 0) {
                        nuevaCantidad = BigDecimal.ZERO;
                    }
                    BigDecimal nuevoBruto = precioUnitario.multiply(nuevaCantidad).setScale(2, java.math.RoundingMode.HALF_UP);
                    BigDecimal nuevoTotal = precioIva.multiply(nuevaCantidad).setScale(2, java.math.RoundingMode.HALF_UP);

                    StringBuilder updateDetalle = new StringBuilder("UPDATE detalle_Entrada SET ");
                    List<Object> valoresDetalle = new ArrayList<>();
                    agregarCampoActualizacion(updateDetalle, valoresDetalle, colDetalleCantidad, nuevaCantidad.intValue());
                    agregarCampoActualizacion(updateDetalle, valoresDetalle, colDetallePrecioBruto, nuevoBruto);
                    agregarCampoActualizacion(updateDetalle, valoresDetalle, colDetallePrecioTotal, nuevoTotal);
                    updateDetalle.append(" WHERE `").append(colDetalleId).append("` = ?");
                    valoresDetalle.add(detalleEntradaId);

                    try (PreparedStatement ps = conn.prepareStatement(updateDetalle.toString())) {
                        for (int i = 0; i < valoresDetalle.size(); i++) {
                            ps.setObject(i + 1, valoresDetalle.get(i));
                        }
                        ps.executeUpdate();
                    }

                    if (entradaId != null && entradaId > 0 && colEntradaId != null
                            && (colEntradaPrecioNeto != null || colEntradaPrecioTotal != null)) {
                        BigDecimal precioNetoActual = null;
                        BigDecimal precioTotalActual = null;
                        String sqlEntrada = String.format("""
                                SELECT %s AS precioNeto,
                                       %s AS precioTotal
                                FROM entradas
                                WHERE `%s` = ?
                                """,
                                colEntradaPrecioNeto != null ? "`" + colEntradaPrecioNeto + "`" : "NULL",
                                colEntradaPrecioTotal != null ? "`" + colEntradaPrecioTotal + "`" : "NULL",
                                colEntradaId
                        );
                        try (PreparedStatement ps = conn.prepareStatement(sqlEntrada)) {
                            ps.setInt(1, entradaId);
                            try (ResultSet rs = ps.executeQuery()) {
                                if (rs.next()) {
                                    precioNetoActual = parseDecimal(rs.getObject("precioNeto"));
                                    precioTotalActual = parseDecimal(rs.getObject("precioTotal"));
                                }
                            }
                        }

                        StringBuilder updateEntrada = new StringBuilder("UPDATE entradas SET ");
                        List<Object> valoresEntrada = new ArrayList<>();
                        if (colEntradaPrecioNeto != null && precioNetoActual != null) {
                            BigDecimal nuevoNeto = precioNetoActual.subtract(precioUnitario);
                            if (nuevoNeto.compareTo(BigDecimal.ZERO) < 0) {
                                nuevoNeto = BigDecimal.ZERO;
                            }
                            agregarCampoActualizacion(updateEntrada, valoresEntrada, colEntradaPrecioNeto,
                                    nuevoNeto.setScale(2, java.math.RoundingMode.HALF_UP));
                        }
                        if (colEntradaPrecioTotal != null && precioTotalActual != null) {
                            BigDecimal nuevoTotalEntrada = precioTotalActual.subtract(precioIva);
                            if (nuevoTotalEntrada.compareTo(BigDecimal.ZERO) < 0) {
                                nuevoTotalEntrada = BigDecimal.ZERO;
                            }
                            agregarCampoActualizacion(updateEntrada, valoresEntrada, colEntradaPrecioTotal,
                                    nuevoTotalEntrada.setScale(2, java.math.RoundingMode.HALF_UP));
                        }
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
                    }

                    conn.commit();
                    cargarInventarioDisponible(chkInventarioDetallado.isSelected());
                } catch (SQLException e) {
                    conn.rollback();
                    throw e;
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    private Integer parseInteger(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(valor.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private java.math.BigDecimal parseDecimal(Object valor) {
        if (valor == null) {
            return java.math.BigDecimal.ZERO;
        }
        String limpio = valor.toString().replace(",", "").trim();
        if (limpio.isBlank()) {
            return java.math.BigDecimal.ZERO;
        }
        try {
            return new java.math.BigDecimal(limpio);
        } catch (NumberFormatException e) {
            return java.math.BigDecimal.ZERO;
        }
    }

    private Map<String, String> obtenerColumnas(Connection conn, String tabla) throws SQLException {
        Map<String, String> columnas = new java.util.HashMap<>();
        java.sql.DatabaseMetaData metaData = conn.getMetaData();
        try (ResultSet rs = metaData.getColumns(null, null, tabla, null)) {
            while (rs.next()) {
                String nombre = rs.getString("COLUMN_NAME");
                if (nombre != null) {
                    columnas.put(nombre.toLowerCase(), nombre);
                }
            }
        }
        return columnas;
    }

    private String resolverColumna(Map<String, String> columnas, String... alternativas) {
        if (columnas == null || alternativas == null) {
            return null;
        }
        for (String alternativa : alternativas) {
            if (alternativa == null) {
                continue;
            }
            String encontrada = columnas.get(alternativa.toLowerCase());
            if (encontrada != null) {
                return encontrada;
            }
        }
        return null;
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

    private void configurarColumnasTabla() {
        colClaveProducto.setCellValueFactory(new PropertyValueFactory<>("claveProducto"));
        colCantidad.setCellValueFactory(new PropertyValueFactory<>("cantidad"));
        colProducto.setCellValueFactory(new PropertyValueFactory<>("producto"));
        colMarca.setCellValueFactory(new PropertyValueFactory<>("marca"));
        colCategoria.setCellValueFactory(new PropertyValueFactory<>("categoria"));
        colMaterial.setCellValueFactory(new PropertyValueFactory<>("material"));
        colUnidad.setCellValueFactory(new PropertyValueFactory<>("unidadMedida"));
        colPresentacion.setCellValueFactory(new PropertyValueFactory<>("presentacion"));
        colFactor.setCellValueFactory(new PropertyValueFactory<>("factor"));
        colLote.setCellValueFactory(new PropertyValueFactory<>("lote"));
        colCaducidad.setCellValueFactory(new PropertyValueFactory<>("caducidad"));
        colUbicacion.setCellValueFactory(new PropertyValueFactory<>("ubicacion"));
        colDescripcion.setCellValueFactory(new PropertyValueFactory<>("descripcion"));
        colInventarioMinimo.setCellValueFactory(new PropertyValueFactory<>("inventarioMinimo"));

        TableColumn<ItemInventario, ?>[] columnas = new TableColumn[] {
                colClaveProducto,
                colCantidad,
                colProducto,
                colMarca,
                colCategoria,
                colMaterial,
                colUnidad,
                colPresentacion,
                colFactor,
                colLote,
                colCaducidad,
                colUbicacion,
                colDescripcion,
                colInventarioMinimo
        };
        for (TableColumn<ItemInventario, ?> col : columnas) {
            col.setStyle("-fx-alignment: CENTER;");
        }

        contenidoTabla.setItems(itemsInventario);
    }

    private void actualizarPoliticaRedimensionamiento() {
        List<TableColumn<ItemInventario, ?>> columnasVisibles = contenidoTabla.getColumns().stream()
                .filter(TableColumn::isVisible)
                .collect(Collectors.toList());

        Platform.runLater(() -> {
            double anchoDisponible = contenidoTabla.getWidth();
            if (anchoDisponible <= 0) {
                anchoDisponible = Math.max(100, contenedorTabla.getWidth() );
            }

            double minWidthTotal = columnasVisibles.stream()
                    .mapToDouble(TableColumn::getMinWidth)
                    .sum();

            // Margen del 5% para evitar problemas de redondeo
            boolean columnasCaben = minWidthTotal <= (anchoDisponible * 1.05);

            if (columnasCaben) {
                contenidoTabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
                // Resetear para distribución equitativa
                for (TableColumn<ItemInventario, ?> col : columnasVisibles) {
                    col.setPrefWidth(-1);
                }
            } else {
                contenidoTabla.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);

                // Intentar hacer ajustes inteligentes
                if (minWidthTotal > anchoDisponible * 1.2) { // Si excede en más del 20%
                    double factor = (anchoDisponible * 0.9) / minWidthTotal;
                    for (TableColumn<ItemInventario, ?> col : columnasVisibles) {
                        col.setPrefWidth(col.getMinWidth() * factor);
                    }
                }
            }

            contenidoTabla.requestLayout();
        });
    }

    private void configurarInventarioDetallado() {
        aplicarVisibilidadModo(chkInventarioDetallado.isSelected());
        chkInventarioDetallado.selectedProperty().addListener((obs, oldVal, newVal) -> {
            guardarVisibilidadModo(oldVal);
            aplicarVisibilidadModo(newVal);
            cargarInventarioDisponible(newVal);
            actualizarOpcionesFiltro(newVal);
        });
    }

    private void guardarVisibilidadModo(boolean detallado) {
        Map<TableColumn<ItemInventario, ?>, Boolean> destino =
                detallado ? visibilidadDetallado : visibilidadResumen;
        for (TableColumn<ItemInventario, ?> columna : obtenerColumnasModo(detallado)) {
            destino.put(columna, columna.isVisible());
        }
    }

    private void aplicarVisibilidadModo(boolean detallado) {
        for (TableColumn<ItemInventario, ?> columna : obtenerColumnasModo(!detallado)) {
            columna.setVisible(false);
        }

        Map<TableColumn<ItemInventario, ?>, Boolean> estado =
                detallado ? visibilidadDetallado : visibilidadResumen;
        for (TableColumn<ItemInventario, ?> columna : obtenerColumnasModo(detallado)) {
            columna.setVisible(estado.getOrDefault(columna, true));
        }
    }


    private List<TableColumn<ItemInventario, ?>> obtenerColumnasModo(boolean detallado) {
        List<TableColumn<ItemInventario, ?>> columnas = new ArrayList<>();
        columnas.add(colClaveProducto);
        if (!detallado) {
            columnas.add(colCantidad);
        }
        columnas.add(colProducto);
        columnas.add(colMarca);
        columnas.add(colCategoria);
        columnas.add(colMaterial);
        columnas.add(colUnidad);
        columnas.add(colPresentacion);
        columnas.add(colFactor);
        if (detallado) {
            columnas.add(colLote);
            columnas.add(colCaducidad);
            columnas.add(colUbicacion);
        }
        columnas.add(colDescripcion);
        columnas.add(colInventarioMinimo);
        return columnas;
    }

    @FXML
    private void mostrarSelectorColumnas(MouseEvent event) {
        boolean detallado = chkInventarioDetallado.isSelected();
        List<TableColumn<ItemInventario, ?>> columnas = obtenerColumnasModo(detallado);
        Map<TableColumn<ItemInventario, ?>, Boolean> estado =
                detallado ? visibilidadDetallado : visibilidadResumen;

        SelectorColumnasPopup.mostrar((Node) event.getSource(), event.getScreenX(), event.getScreenY(),
                columnas, seleccion -> {
                    for (Map.Entry<TableColumn<ItemInventario, ?>, Boolean> entry : seleccion.entrySet()) {
                        entry.getKey().setVisible(entry.getValue());
                    }
                    estado.putAll(seleccion);
                });
    }

    private void configurarFiltros() {
        actualizarOpcionesFiltro(chkInventarioDetallado.isSelected());
        comboFiltro.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (restaurandoFiltros) return;
            actualizarValoresFiltro(newVal);
        });
    }

    private void actualizarOpcionesFiltro(boolean detallado) {

        String campoSeleccionado = comboFiltro.getValue();
        String valorSeleccionado = comboValor.getValue();

        restaurandoFiltros = true;

        List<String> opciones = new ArrayList<>();
        opciones.add("ID");
        opciones.add("Producto");
        opciones.add("Marca");
        opciones.add("Categoría");
        opciones.add("Material");
        opciones.add("Unidad");
        opciones.add("Presentación");
        if (detallado) {
            opciones.add("Lote");
            opciones.add("Caducidad");
            opciones.add("Ubicación");
        }

        comboFiltro.getItems().setAll(opciones);

        if (campoSeleccionado != null && opciones.contains(campoSeleccionado)) {
            comboFiltro.setValue(campoSeleccionado);

            actualizarValoresFiltro(campoSeleccionado);

            if (valorSeleccionado != null &&
                    comboValor.getItems().contains(valorSeleccionado)) {
                comboValor.setValue(valorSeleccionado);
            }
        } else {
            comboValor.getItems().clear();
            comboValor.setValue(null);
        }

        restaurandoFiltros = false;

        limpiarFiltrosNoDisponibles(new LinkedHashSet<>(opciones));
    }

    private void actualizarValoresFiltro(String campo) {

        comboValor.getItems().clear();

        if (!restaurandoFiltros) {
            comboValor.setValue(null);
        }

        if (campo == null || campo.isBlank()) {
            return;
        }

        Set<String> valores = new LinkedHashSet<>();
        for (ItemInventario item : itemsInventarioOriginal) {
            String valor = obtenerValorCampo(item, campo);
            if (valor != null && !valor.isBlank()) {
                valores.add(valor);
            }
        }

        comboValor.getItems().setAll(valores);
    }


    @FXML
    private void agregarFiltro() {
        String campo = comboFiltro.getValue();
        String valor = comboValor.getValue();
        if (campo == null || valor == null) {
            mostrarAdvertencia(
                    "Filtro incompleto",
                    "Debes seleccionar un valor para el campo \"" + campo + "\"."
            );
            return;
        }
        if (filtrosActivos.size() >= 3) {
            mostrarAdvertencia(
                    "Límite de filtros",
                    "Solo puedes aplicar hasta 3 filtros al mismo tiempo.\n" +
                            "Elimina uno para agregar otro."
            );
            return;
        }
        for (Filtro filtro : filtrosActivos) {
            if (filtro.campo.equals(campo)) {
                mostrarAdvertencia(
                        "Filtro duplicado",
                        "Ya existe un filtro aplicado para el campo \"" + campo + "\".\n" +
                                "Elimina el filtro actual si deseas cambiar su valor."
                );
                return;
            }
        }
        Filtro filtro = new Filtro(campo, valor);
        filtrosActivos.add(filtro);
        contenedorFiltros.getChildren().add(crearChipFiltro(filtro));
        aplicarFiltros();
    }

    private Node crearChipFiltro(Filtro filtro) {
        HBox chip = new HBox(5); // spacing igual que en FXML
        chip.setAlignment(javafx.geometry.Pos.CENTER);
        chip.getStyleClass().add("chip"); // Aquí aplicas la clase CSS

        Label texto = new Label(filtro.campo + ": " + filtro.valor);
        texto.getStyleClass().add("chip-text"); // Si tienes clase específica para texto

        Button quitar = new Button("✕");
        quitar.getStyleClass().add("chip-close"); // La misma clase que en FXML

        quitar.setOnAction(event -> {
            filtrosActivos.remove(filtro);
            contenedorFiltros.getChildren().remove(chip);
            aplicarFiltros();
        });

        chip.getChildren().addAll(texto, quitar);
        if (contenedorFiltros.getChildren().isEmpty()) {
            HBox.setMargin(chip, new Insets(0, 0, 0, 25));
        }
        return chip;
    }

    private void limpiarFiltrosNoDisponibles(Set<String> opcionesValidas) {
        List<Filtro> filtrosRemover = new ArrayList<>();
        for (Filtro filtro : filtrosActivos) {
            if (!opcionesValidas.contains(filtro.campo)) {
                filtrosRemover.add(filtro);
            }
        }
        for (Filtro filtro : filtrosRemover) {
            filtrosActivos.remove(filtro);
            contenedorFiltros.getChildren().removeIf(node ->
                    node instanceof HBox && ((HBox) node).getChildren().stream()
                            .anyMatch(child -> child instanceof Label &&
                                    ((Label) child).getText().startsWith(filtro.campo + ":")));
        }
        aplicarFiltros();
    }

    private void aplicarFiltros() {
        List<ItemInventario> filtrados = new ArrayList<>();
        for (ItemInventario item : itemsInventarioOriginal) {
            boolean coincide = true;
            for (Filtro filtro : filtrosActivos) {
                String valor = obtenerValorCampo(item, filtro.campo);
                if (valor == null || !valor.equals(filtro.valor)) {
                    coincide = false;
                    break;
                }
            }
            if (coincide) {
                filtrados.add(item);
            }
        }
        itemsInventario.setAll(filtrados);
        aplicarOrdenamiento();
    }

    private String obtenerValorCampo(ItemInventario item, String campo) {
        switch (campo) {
            case "ID":
                return item.getClaveProducto();
            case "Producto":
                return item.getProducto();
            case "Marca":
                return item.getMarca();
            case "Categoría":
                return item.getCategoria();
            case "Material":
                return item.getMaterial();
            case "Unidad":
                return item.getUnidadMedida();
            case "Presentación":
                return item.getPresentacion();
            case "Lote":
                return item.getLote();
            case "Caducidad":
                return item.getCaducidad();
            case "Ubicación":
                return item.getUbicacion();
            default:
                return "";
        }
    }

    @FXML
    private void exportarExcel() {
        if (contenidoTabla.getItems().isEmpty()) {
            mostrarAdvertencia("Advertencia", "No hay datos para exportar.");
            return;
        }

        // Crear lista de filtros aplicados
        List<String> filtrosAplicados = new ArrayList<>();
        for (Filtro filtro : filtrosActivos) {
            filtrosAplicados.add(filtro.campo + ": " + filtro.valor);
        }

        exportador.exportarTabla(contenidoTabla, "Inventario", "excel", filtrosAplicados);
    }

    @FXML
    private void descargarPdf() {
        if (contenidoTabla.getItems().isEmpty()) {
            mostrarAdvertencia("Advertencia", "No hay datos para exportar.");
            return;
        }

        // Crear lista de filtros aplicados
        List<String> filtrosAplicados = new ArrayList<>();
        for (Filtro filtro : filtrosActivos) {
            filtrosAplicados.add(filtro.campo + ": " + filtro.valor);
        }

        exportador.exportarTabla(contenidoTabla, "Inventario", "pdf", filtrosAplicados);
    }

    @FXML
    private void vistaPreviaPdf() {
        if (contenidoTabla.getItems().isEmpty()) {
            mostrarAdvertencia("Advertencia", "No hay datos para exportar.");
            return;
        }

        // Crear lista de filtros aplicados
        List<String> filtrosAplicados = new ArrayList<>();
        for (Filtro filtro : filtrosActivos) {
            filtrosAplicados.add(filtro.campo + ": " + filtro.valor);
        }

        exportador.previsualizarPDF(contenidoTabla, "Inventario", filtrosAplicados);
    }

    private void mostrarAdvertencia(String titulo, String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.WARNING);
        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }

    @FXML
    private void mostrarOrdenPopup(MouseEvent event) {
        boolean detallado = chkInventarioDetallado.isSelected();
        List<String> criterios = new ArrayList<>();
        criterios.add("id");
        if (!detallado) {
            criterios.add("cantidad");
        }
        criterios.add("producto");

        if (detallado) {
            criterios.add("ubicacion");
        }

        SelectorOrdenPopup.mostrar((Node) event.getSource(), event.getScreenX(), event.getScreenY(),
                criterios, criterioOrden, direccionOrden, seleccion -> {
                    criterioOrden = seleccion.getCriterio();
                    direccionOrden = seleccion.getDireccion();
                    aplicarOrdenamiento();
                });
    }

    private void aplicarOrdenamiento() {
        Comparator<ItemInventario> comparator = null;
        Function<String, String> normalizar = valor -> valor == null ? "" : valor.toLowerCase();

        switch (criterioOrden) {
            case "cantidad":
                comparator = Comparator.comparingInt(item -> {
                    String valor = item.getCantidad();
                    if (valor == null || valor.isBlank()) {
                        return 0;
                    }
                    try {
                        return Integer.parseInt(valor);
                    } catch (NumberFormatException e) {
                        return 0;
                    }
                });
                break;
            case "producto":
                comparator = Comparator.comparing(item -> normalizar.apply(item.getProducto()));
                break;
            case "ubicacion":
                comparator = Comparator.comparing(item -> normalizar.apply(item.getUbicacion()));
                break;
            case "id":
            default:
                comparator = Comparator.comparing(item -> normalizar.apply(item.getClaveProducto()));
                break;
        }
        if ("desc".equalsIgnoreCase(direccionOrden)) {
            comparator = comparator.reversed();
        }

        FXCollections.sort(itemsInventario, comparator);
    }

    private void cargarInventarioDisponible(boolean detallado) {

        String sql = detallado ? """
            SELECT
                a.idArticulo AS idArticulo,
                p.id AS claveProducto,
                p.nombre AS producto,
                m.nombre AS marca,
                p.categoria AS categoria,
                p.material AS material,
                p.unidadMedida AS unidadMedida,
                a.presentacion AS presentacion,
                a.factor AS factor,
                a.lote AS lote,
                a.caducidad AS caducidad,
                u.nombre AS ubicacion,
                p.descripcion AS descripcion,
                p.inventarioMin AS inventarioMinimo
            FROM articulo a
            INNER JOIN detalle_Entrada de ON a.idDetalleEntrada = de.idDetalleEntrada
            INNER JOIN productos p ON de.claveProducto = p.id
            LEFT JOIN marcas m ON p.marca = m.id
            LEFT JOIN ubicaciones u ON a.ubicacion = u.id
            WHERE a.Estado = 'disponible'
            """ : """
            SELECT
                NULL AS idArticulo,
                p.id AS claveProducto,
                COUNT(a.idArticulo) AS cantidad,
                p.nombre AS producto,
                m.nombre AS marca,
                p.categoria AS categoria,
                p.material AS material,
                p.unidadMedida AS unidadMedida,
                a.presentacion AS presentacion,
                a.factor AS factor,
                p.descripcion AS descripcion,
                p.inventarioMin AS inventarioMinimo
            FROM articulo a
            INNER JOIN detalle_Entrada de ON a.idDetalleEntrada = de.idDetalleEntrada
            INNER JOIN productos p ON de.claveProducto = p.id
            LEFT JOIN marcas m ON p.marca = m.id
            WHERE a.Estado = 'disponible'
            GROUP BY
                p.id,
                p.nombre,
                m.nombre,
                p.categoria,
                p.material,
                p.unidadMedida,
                a.presentacion,
                a.factor,
                p.descripcion,
                p.inventarioMin
            """;

        // 1️⃣ Guardar selección actual de filtros
        String campoActual = comboFiltro.getValue();
        String valorActual = comboValor.getValue();

        itemsInventarioOriginal.clear();

        try (Connection conn = new Conexion().conectar();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                itemsInventarioOriginal.add(new ItemInventario(
                        rs.getString("idArticulo"),
                        rs.getString("claveProducto"),
                        detallado ? "" : rs.getString("cantidad"),
                        rs.getString("producto"),
                        rs.getString("marca"),
                        rs.getString("categoria"),
                        rs.getString("material"),
                        rs.getString("unidadMedida"),
                        rs.getString("presentacion"),
                        rs.getString("factor"),
                        detallado ? rs.getString("lote") : "",
                        detallado ? rs.getString("caducidad") : "",
                        detallado ? rs.getString("ubicacion") : "",
                        rs.getString("descripcion"),
                        rs.getString("inventarioMinimo")
                ));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        restaurandoFiltros = true;

        actualizarValoresFiltro(campoActual);

        if (valorActual != null &&
                comboValor.getItems().contains(valorActual)) {
            comboValor.setValue(valorActual);
        }

        restaurandoFiltros = false;

        // 3️⃣ Aplicar filtros activos
        aplicarFiltros();
    }

    private List<String> obtenerUbicacionesActivas() {
        return new Operaciones.compra.model.model().obtenerNombresUbicaciones();
    }

    private static class Filtro {
        private final String campo;
        private final String valor;

        private Filtro(String campo, String valor) {
            this.campo = campo;
            this.valor = valor;
        }
    }
}
