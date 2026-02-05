package Reportes.inventario.controller;

import Compartido.controller.encabezadoController;
import Compartido.controller.navbarController;
import Compartido.exportar.exportador;
import Compartido.helper.SelectorColumnasPopup;
import Compartido.helper.SelectorOrdenPopup;
import Compartido.helper.OverlayCarga;
import Reportes.inventario.model.ItemInventario;
import conexion.Conexion;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
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
import java.util.concurrent.atomic.AtomicReference;
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
    @FXML private TableColumn<ItemInventario, String> colId;
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
    private OverlayCarga overlayCarga;


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
            if (root != null && overlayPane != null) {
                overlayCarga = new OverlayCarga(root, overlayPane);
            }

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
        dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        if (root != null && root.getScene() != null) {
            dialog.initOwner(root.getScene().getWindow());
        }

        dialog.getDialogPane().getButtonTypes().addAll(
                javafx.scene.control.ButtonType.CANCEL,
                javafx.scene.control.ButtonType.OK
        );

        dialog.getDialogPane().getStylesheets().add(
                getClass().getResource("/Reportes/inventario/style/estilos.css").toExternalForm()
        );

        // Estilos
        Button btnOk = (Button) dialog.getDialogPane().lookupButton(javafx.scene.control.ButtonType.OK);
        Button btnCancel = (Button) dialog.getDialogPane().lookupButton(javafx.scene.control.ButtonType.CANCEL);

        if (btnOk != null) btnOk.getStyleClass().add("boton-formulario");
        if (btnCancel != null) btnCancel.getStyleClass().add("boton-formulario");

        // ✅ FORZAR: Cancelar a la izquierda y Aceptar a la derecha (sin getButtonBar)
        if (btnCancel != null) ButtonBar.setButtonData(btnCancel, ButtonBar.ButtonData.CANCEL_CLOSE);
        if (btnOk != null) ButtonBar.setButtonData(btnOk, ButtonBar.ButtonData.OK_DONE);

        ButtonBar bar = (ButtonBar) dialog.getDialogPane().lookup(".button-bar");
        if (bar != null) {
            bar.setButtonOrder(ButtonBar.BUTTON_ORDER_NONE); // respeta el orden agregado: CANCEL, OK
        }

        List<String> ubicacionesActivas = obtenerUbicacionesActivas();
        javafx.scene.control.ComboBox<String> cbUbicacion = new javafx.scene.control.ComboBox<>();
        cbUbicacion.setItems(FXCollections.observableArrayList(ubicacionesActivas));
        cbUbicacion.setPromptText("Selecciona ubicación");
        cbUbicacion.getStyleClass().add("textfield");
        String ubicacionActual = item.getUbicacion();
        if (ubicacionActual != null
                && !ubicacionActual.isBlank()
                && !"Sin ubicación".equalsIgnoreCase(ubicacionActual)
                && ubicacionesActivas.stream().anyMatch(ubicacionActual::equalsIgnoreCase)) {
            cbUbicacion.setValue(ubicacionActual);
        }
        javafx.scene.control.TextField txtLote = new javafx.scene.control.TextField(item.getLote());
        txtLote.getStyleClass().add("textfield");
        javafx.scene.control.DatePicker dpCaducidad = new javafx.scene.control.DatePicker();
        dpCaducidad.getStyleClass().add("textfield");
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
        cbPresentacion.getStyleClass().add("textfield");
        String presentacionActual = item.getPresentacion();
        if (presentacionActual != null
                && !presentacionActual.isBlank()
                && PRESENTACIONES_COMPRA.stream().anyMatch(presentacionActual::equalsIgnoreCase)) {
            cbPresentacion.setValue(presentacionActual);
        }
        javafx.scene.control.TextField txtFactor = new javafx.scene.control.TextField(item.getFactor());
        txtFactor.getStyleClass().add("textfield");

        javafx.scene.layout.GridPane formulario = new javafx.scene.layout.GridPane();
        formulario.setHgap(10);
        formulario.setVgap(8);
        formulario.add(new Label("Ubicación:"), 0, 0);
        formulario.add(cbUbicacion, 1, 0);
        formulario.add(new Label("Lote:"), 0, 1);
        formulario.add(txtLote, 1, 1);
        formulario.add(new Label("Caducidad:"), 0, 2);
        formulario.add(dpCaducidad, 1, 2);
        formulario.add(new Label("Presentación:"), 0, 3);
        formulario.add(cbPresentacion, 1, 3);
        formulario.add(new Label("Factor:"), 0, 4);
        formulario.add(txtFactor, 1, 4);

        javafx.scene.control.Button btnSegmentar = new javafx.scene.control.Button("Segmentar");
        btnSegmentar.getStyleClass().add("boton-formulario");
        btnSegmentar.setOnAction(event -> iniciarSegmentacionInventario(item, idArticulo, dialog));
        javafx.scene.control.Button btnEliminar = new javafx.scene.control.Button("Eliminar");
        btnEliminar.getStyleClass().add("boton-formulario");
        btnEliminar.setOnAction(event -> {
            eliminarArticuloInventario(idArticulo);
            dialog.setResult(javafx.scene.control.ButtonType.CANCEL);
            dialog.close();
        });

        javafx.scene.layout.HBox accionesSecundarias = new javafx.scene.layout.HBox(12, btnEliminar, btnSegmentar);
        accionesSecundarias.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        actualizarVisibilidadSegmentar(btnSegmentar, cbPresentacion.getValue());
        cbPresentacion.valueProperty().addListener((obs, oldVal, newVal) ->
                actualizarVisibilidadSegmentar(btnSegmentar, newVal));

        VBox contenido = new VBox(10, formulario, accionesSecundarias);
        dialog.getDialogPane().setContent(contenido);

        dialog.showAndWait().ifPresent(respuesta -> {
            if (respuesta != javafx.scene.control.ButtonType.OK) {
                return;
            }
            String caducidadTexto = dpCaducidad.getValue() != null ? dpCaducidad.getValue().toString() : "";
            String presentacionSeleccionada = cbPresentacion.getValue();
            actualizarArticuloInventario(idArticulo, cbUbicacion.getValue(), txtLote.getText(),
                    caducidadTexto, presentacionSeleccionada != null ? presentacionSeleccionada : "", txtFactor.getText());
        });
    }

    private void iniciarSegmentacionInventario(ItemInventario item, int idArticulo,
                                               javafx.scene.control.Dialog<javafx.scene.control.ButtonType> dialogPadre) {
        if (item == null || idArticulo <= 0) {
            return;
        }
        int factor = parseFactor(item.getFactor());
        if (factor <= 0) {
            mostrarAdvertencia("Factor inválido", "El factor debe ser un número mayor a cero.");
            return;
        }

        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Segmentar artículo");
        confirmacion.setHeaderText(null);
        confirmacion.setContentText("Al segmentar este artículo se dividirá el producto en " +
                factor + " piezas.\n¿Deseas continuar?");

        javafx.scene.control.ButtonType btnCancelar = new javafx.scene.control.ButtonType("Cancelar",
                ButtonBar.ButtonData.CANCEL_CLOSE);
        javafx.scene.control.ButtonType btnAceptar = new javafx.scene.control.ButtonType("Aceptar",
                ButtonBar.ButtonData.OK_DONE);
        confirmacion.getButtonTypes().setAll(btnCancelar, btnAceptar);
        configurarOrdenBotones(confirmacion.getDialogPane(), btnCancelar, btnAceptar);

        confirmacion.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        if (dialogPadre != null) {
            confirmacion.initOwner(dialogPadre.getDialogPane().getScene().getWindow());
        }
        confirmacion.showAndWait().ifPresent(respuesta -> {
            if (respuesta != btnAceptar) {
                return;
            }
            abrirFormularioSegmentacionInventario(item, idArticulo, factor, dialogPadre);
        });
    }

    private void abrirFormularioSegmentacionInventario(ItemInventario item, int idArticulo, int factor,
                                                       javafx.scene.control.Dialog<javafx.scene.control.ButtonType> dialogPadre) {
        javafx.scene.control.Dialog<javafx.scene.control.ButtonType> dialog =
                new javafx.scene.control.Dialog<>();
        dialog.setTitle("Segmentar artículo");
        dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        if (dialogPadre != null) {
            dialog.initOwner(dialogPadre.getDialogPane().getScene().getWindow());
        }

        javafx.scene.control.ButtonType btnCancelar = new javafx.scene.control.ButtonType("Cancelar",
                ButtonBar.ButtonData.CANCEL_CLOSE);
        javafx.scene.control.ButtonType btnAceptar = new javafx.scene.control.ButtonType("Aceptar",
                ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnCancelar, btnAceptar);

        dialog.getDialogPane().getStylesheets().add(
                getClass().getResource("/Reportes/inventario/style/estilos.css").toExternalForm()
        );
        configurarOrdenBotones(dialog.getDialogPane(), btnCancelar, btnAceptar);

        VBox contenido = new VBox(12);
        Label lblDescripcion = new Label(obtenerDescripcionArticuloInventario(item, factor));
        lblDescripcion.setWrapText(true);
        lblDescripcion.setStyle("-fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        Label lblUbicaciones = new Label("Ubicaciones para segmentar:");
        lblUbicaciones.setStyle("-fx-font-weight: bold;");

        VBox contenedorUbicaciones = new VBox(10);
        List<UbicacionFila> filas = new ArrayList<>();
        agregarFilaUbicacion(contenedorUbicaciones, filas, true);

        ScrollPane scroll = new ScrollPane(contenedorUbicaciones);
        scroll.setFitToWidth(true);
        scroll.setPrefViewportHeight(320);

        contenido.getChildren().addAll(lblDescripcion, lblUbicaciones, scroll);
        dialog.getDialogPane().setContent(contenido);
        dialog.getDialogPane().setPrefWidth(520);
        dialog.getDialogPane().setPrefHeight(520);

        AtomicReference<List<UbicacionCantidad>> seleccionadasRef = new AtomicReference<>(List.of());
        Button btnOk = (Button) dialog.getDialogPane().lookupButton(btnAceptar);
        if (btnOk != null) {
            btnOk.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
                List<UbicacionCantidad> seleccionadas = obtenerUbicacionesSeleccionadas(filas);
                if (seleccionadas.isEmpty()) {
                    mostrarAdvertencia("Validación", "Debe capturar al menos una ubicación con cantidad.");
                    event.consume();
                    return;
                }
                int suma = seleccionadas.stream().mapToInt(ubicacion -> ubicacion.cantidad).sum();
                if (suma != factor) {
                    mostrarAdvertencia("Validación",
                            "La suma de las ubicaciones debe ser " + factor + " y actualmente es " + suma + ".");
                    event.consume();
                    return;
                }
                seleccionadasRef.set(seleccionadas);
            });
        }

        dialog.showAndWait().ifPresent(respuesta -> {
            if (respuesta != btnAceptar) {
                return;
            }
            ejecutarSegmentacionInventario(item, idArticulo, factor, seleccionadasRef.get(), dialogPadre);
        });
    }

    private void ejecutarSegmentacionInventario(ItemInventario item, int idArticulo, int factor,
                                                List<UbicacionCantidad> ubicaciones,
                                                javafx.scene.control.Dialog<javafx.scene.control.ButtonType> dialogPadre) {
        if (item == null || ubicaciones == null || ubicaciones.isEmpty()) {
            return;
        }

        if (dialogPadre != null) {
            dialogPadre.close();
        }
        if (overlayCarga != null) {
            overlayCarga.mostrar();
        }

        javafx.concurrent.Task<String> task = new javafx.concurrent.Task<>() {
            @Override
            protected String call() throws Exception {
                try (Connection conn = new Conexion().conectar()) {
                    if (conn == null) {
                        throw new SQLException("Sin conexión a la base de datos");
                    }
                    conn.setAutoCommit(false);

                    try (PreparedStatement ps = conn.prepareStatement(
                            "UPDATE articulo SET segmentado = 1, Estado = 'segmentado' WHERE idArticulo = ?")) {
                        ps.setInt(1, idArticulo);
                        ps.executeUpdate();
                    }

                    try (PreparedStatement psUbicacion = conn.prepareStatement(
                            "SELECT id FROM ubicaciones WHERE nombre = ? AND estado = 'activo'")) {
                        for (UbicacionCantidad ubicacion : ubicaciones) {
                            psUbicacion.setString(1, ubicacion.nombre);
                            try (ResultSet rs = psUbicacion.executeQuery()) {
                                if (!rs.next()) {
                                    conn.rollback();
                                    throw new SQLException("Ubicación inválida: " + ubicacion.nombre);
                                }
                                ubicacion.id = rs.getInt(1);
                            }
                        }
                    }

                    int consecutivoDetalle = obtenerSiguienteConsecutivoDetalle(conn);
                    try (PreparedStatement ps = conn.prepareStatement(
                            "INSERT INTO detalleArticulo (idDetalle, idArticulo, idUbicacion, estado) VALUES (?, ?, ?, 'activo')")) {
                        for (UbicacionCantidad ubicacion : ubicaciones) {
                            for (int i = 0; i < ubicacion.cantidad; i++) {
                                ps.setString(1, "S-" + consecutivoDetalle++);
                                ps.setString(2, String.valueOf(idArticulo));
                                ps.setInt(3, ubicacion.id);
                                ps.addBatch();
                            }
                        }
                        ps.executeBatch();
                    }

                    conn.commit();
                }

                return construirMensajeSegmentacion(item, factor, ubicaciones);
            }
        };

        task.setOnSucceeded(event -> {
            if (overlayCarga != null) {
                overlayCarga.ocultar();
            }
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Segmentación completada");
            alert.setHeaderText(null);
            alert.setContentText(task.getValue());
            alert.showAndWait();
            cargarInventarioDisponible(chkInventarioDetallado.isSelected());
            if (dialogPadre != null) {
                dialogPadre.close();
            }
        });

        task.setOnFailed(event -> {
            if (overlayCarga != null) {
                overlayCarga.ocultar();
            }
            Throwable ex = task.getException();
            mostrarAdvertencia("Error", ex != null ? ex.getMessage() : "No se pudo segmentar el artículo.");
        });

        Thread hilo = new Thread(task);
        hilo.setDaemon(true);
        hilo.start();
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
        colId.setCellValueFactory(new PropertyValueFactory<>("idArticulo"));
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
                colId,
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
        if (detallado) {
            columnas.add(colId);
        }
        columnas.add(colClaveProducto);
        if (!detallado) {
            columnas.add(colCantidad);
        }
        columnas.add(colProducto);
        columnas.add(colMarca);
        columnas.add(colCategoria);
        columnas.add(colMaterial);
        columnas.add(colUnidad);
        if (detallado) {
            columnas.add(colPresentacion);
            columnas.add(colFactor);
        }
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
        opciones.add("ID producto");
        opciones.add("Producto");
        opciones.add("Marca");
        opciones.add("Categoría");
        opciones.add("Material");
        opciones.add("Unidad");
        if (detallado) {
            opciones.add("Presentación");
            opciones.add("Factor");
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
                return item.getIdArticulo();
            case "ID producto":
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
        criterios.add("id producto");
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
            case "id producto":
                comparator = Comparator.comparing(item -> normalizar.apply(item.getClaveProducto()));
                break;
            case "id":
            default:
                comparator = Comparator.comparing(item -> normalizar.apply(item.getIdArticulo()));
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
            UNION ALL
            SELECT
                da.idDetalle AS idArticulo,
                p.id AS claveProducto,
                p.nombre AS producto,
                m.nombre AS marca,
                p.categoria AS categoria,
                p.material AS material,
                p.unidadMedida AS unidadMedida,
                'pz' AS presentacion,
                '1' AS factor,
                a.lote AS lote,
                a.caducidad AS caducidad,
                u.nombre AS ubicacion,
                p.descripcion AS descripcion,
                p.inventarioMin AS inventarioMinimo
            FROM detalleArticulo da
            INNER JOIN articulo a ON da.idArticulo = a.idArticulo
            INNER JOIN detalle_Entrada de ON a.idDetalleEntrada = de.idDetalleEntrada
            INNER JOIN productos p ON de.claveProducto = p.id
            LEFT JOIN marcas m ON p.marca = m.id
            LEFT JOIN ubicaciones u ON da.idUbicacion = u.id
            WHERE da.estado = 'activo'
              AND a.Estado = 'segmentado'
            """ : """
            SELECT
                NULL AS idArticulo,
                claveProducto,
                SUM(cantidad) AS cantidad,
                producto,
                marca,
                categoria,
                material,
                unidadMedida,
                presentacion,
                factor,
                descripcion,
                inventarioMinimo
            FROM (
                SELECT
                    p.id AS claveProducto,
                    1 AS cantidad,
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
                UNION ALL
                SELECT
                    p.id AS claveProducto,
                    1 AS cantidad,
                    p.nombre AS producto,
                    m.nombre AS marca,
                    p.categoria AS categoria,
                    p.material AS material,
                    p.unidadMedida AS unidadMedida,
                    'pz' AS presentacion,
                    '1' AS factor,
                    p.descripcion AS descripcion,
                    p.inventarioMin AS inventarioMinimo
                FROM detalleArticulo da
                INNER JOIN articulo a ON da.idArticulo = a.idArticulo
                INNER JOIN detalle_Entrada de ON a.idDetalleEntrada = de.idDetalleEntrada
                INNER JOIN productos p ON de.claveProducto = p.id
                LEFT JOIN marcas m ON p.marca = m.id
                WHERE da.estado = 'activo'
                  AND a.Estado = 'segmentado'
            ) AS inventario
            GROUP BY
                claveProducto,
                producto,
                marca,
                categoria,
                material,
                unidadMedida,
                presentacion,
                factor,
                descripcion,
                inventarioMinimo
            """;

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

        aplicarFiltros();
    }

    private List<String> obtenerUbicacionesActivas() {
        return new Operaciones.compra.model.model().obtenerNombresUbicaciones();
    }

    private int obtenerSiguienteConsecutivoDetalle(Connection conn) throws SQLException {
        String sql = "SELECT COALESCE(MAX(CAST(SUBSTRING(idDetalle, 3) AS UNSIGNED)), 0) " +
                "FROM detalleArticulo WHERE idDetalle LIKE 'S-%'";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1) + 1;
            }
        }
        return 1;
    }

    private void actualizarVisibilidadSegmentar(Button botonSegmentar, String presentacion) {
        if (botonSegmentar == null) {
            return;
        }
        String valor = presentacion == null ? "" : presentacion.trim().toLowerCase();
        boolean mostrar = !"pz".equals(valor) && !"pieza".equals(valor);
        botonSegmentar.setVisible(mostrar);
        botonSegmentar.setManaged(mostrar);
    }

    private int parseFactor(String valor) {
        if (valor == null || valor.isBlank()) {
            return 0;
        }
        try {
            return Integer.parseInt(valor.trim());
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private String obtenerDescripcionArticuloInventario(ItemInventario item, int factor) {
        String producto = item.getProducto() == null ? "" : item.getProducto();
        String descripcion = item.getDescripcion() == null ? "" : item.getDescripcion();
        String presentacion = item.getPresentacion() == null ? "" : item.getPresentacion();
        return "Producto: " + producto + " | Presentación: " + presentacion +
                " | Factor: " + factor + "\nDescripción: " + descripcion;
    }

    private void agregarFilaUbicacion(VBox contenedor, List<UbicacionFila> filas, boolean inicial) {
        HBox fila = new HBox(15);

        VBox vboxUbicacion = new VBox(5);
        Label labelUbicacion = new Label("Ubicación:");
        ComboBox<String> combo = new ComboBox<>();
        combo.setEditable(true);
        combo.setPromptText("Selecciona ubicación");
        combo.setItems(FXCollections.observableArrayList(obtenerUbicacionesActivas()));
        vboxUbicacion.getChildren().addAll(labelUbicacion, combo);
        HBox.setHgrow(vboxUbicacion, Priority.ALWAYS);

        VBox vboxCantidad = new VBox(5);
        Label labelCantidad = new Label("Cantidad en ubicación:");
        javafx.scene.control.TextField txtCantidad = new javafx.scene.control.TextField();
        vboxCantidad.getChildren().addAll(labelCantidad, txtCantidad);
        HBox.setHgrow(vboxCantidad, Priority.ALWAYS);

        VBox vboxBoton = new VBox(5);
        Button boton = new Button(inicial ? "+" : "-");
        boton.getStyleClass().add("botonAgregarUbi");
        vboxBoton.setAlignment(javafx.geometry.Pos.BOTTOM_CENTER);
        vboxBoton.getChildren().add(boton);
        HBox.setHgrow(vboxBoton, Priority.ALWAYS);

        if (inicial) {
            boton.setOnAction(event -> agregarFilaUbicacion(contenedor, filas, false));
        } else {
            boton.setOnAction(event -> {
                contenedor.getChildren().remove(fila);
                filas.removeIf(item -> item.contenedor == fila);
            });
        }

        fila.getChildren().addAll(vboxUbicacion, vboxCantidad, vboxBoton);
        contenedor.getChildren().add(fila);
        filas.add(new UbicacionFila(fila, combo, txtCantidad));
    }

    private List<UbicacionCantidad> obtenerUbicacionesSeleccionadas(List<UbicacionFila> filas) {
        List<UbicacionCantidad> resultado = new ArrayList<>();
        for (UbicacionFila fila : filas) {
            String ubicacion = fila.combo.getValue();
            if ((ubicacion == null || ubicacion.isBlank()) && fila.combo.getEditor() != null) {
                ubicacion = fila.combo.getEditor().getText();
            }
            String cantidadTexto = fila.cantidad.getText();
            if (ubicacion == null || ubicacion.isBlank() || cantidadTexto == null || cantidadTexto.isBlank()) {
                continue;
            }
            try {
                int cantidad = Integer.parseInt(cantidadTexto.trim());
                if (cantidad > 0) {
                    resultado.add(new UbicacionCantidad(ubicacion.trim(), cantidad));
                }
            } catch (NumberFormatException ignored) {
                // Ignorar cantidades inválidas
            }
        }
        return resultado;
    }

    private void configurarOrdenBotones(DialogPane pane, javafx.scene.control.ButtonType cancelar,
                                        javafx.scene.control.ButtonType aceptar) {
        if (pane == null) {
            return;
        }
        Button btnCancelar = (Button) pane.lookupButton(cancelar);
        Button btnAceptar = (Button) pane.lookupButton(aceptar);

        if (btnCancelar != null) btnCancelar.getStyleClass().add("boton-formulario");
        if (btnAceptar != null) btnAceptar.getStyleClass().add("boton-formulario");

        if (btnCancelar != null) ButtonBar.setButtonData(btnCancelar, ButtonBar.ButtonData.CANCEL_CLOSE);
        if (btnAceptar != null) ButtonBar.setButtonData(btnAceptar, ButtonBar.ButtonData.OK_DONE);

        ButtonBar bar = (ButtonBar) pane.lookup(".button-bar");
        if (bar != null) {
            bar.setButtonOrder(ButtonBar.BUTTON_ORDER_NONE);
        }
    }

    private String construirMensajeSegmentacion(ItemInventario item, int factor, List<UbicacionCantidad> ubicaciones) {
        String producto = item.getProducto() == null ? "" : item.getProducto();
        String presentacion = item.getPresentacion() == null ? "" : item.getPresentacion();
        StringBuilder detalle = new StringBuilder();
        for (UbicacionCantidad ubicacion : ubicaciones) {
            if (detalle.length() > 0) {
                detalle.append(", ");
            }
            detalle.append(ubicacion.nombre).append(" (").append(ubicacion.cantidad).append(" piezas)");
        }
        return "El artículo " + producto + " con presentación " + presentacion + " y factor " + factor +
                " se segmentó en: " + detalle + ".";
    }

    private static class Filtro {
        private final String campo;
        private final String valor;

        private Filtro(String campo, String valor) {
            this.campo = campo;
            this.valor = valor;
        }
    }

    private static class UbicacionFila {
        private final HBox contenedor;
        private final ComboBox<String> combo;
        private final javafx.scene.control.TextField cantidad;

        private UbicacionFila(HBox contenedor, ComboBox<String> combo, javafx.scene.control.TextField cantidad) {
            this.contenedor = contenedor;
            this.combo = combo;
            this.cantidad = cantidad;
        }
    }

    private static class UbicacionCantidad {
        private final String nombre;
        private final int cantidad;
        private int id;

        private UbicacionCantidad(String nombre, int cantidad) {
            this.nombre = nombre;
            this.cantidad = cantidad;
        }
    }
}
