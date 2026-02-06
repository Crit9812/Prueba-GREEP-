package Reportes.inventario.controller;

import javafx.scene.Node;
import javafx.scene.control.Separator;
import javafx.geometry.Insets;
import Formularios.controller.controllerCompraEmergente;
import Operaciones.ajusteInventario.model.model;
import Operaciones.compra.model.compra;
import Reportes.inventario.model.ItemInventario;
import Compartido.helper.OverlayCarga;
import conexion.Conexion;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.geometry.Pos;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.io.IOException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class DetalleInventarioController {

    @FXML private Label lblNombre;
    @FXML private Label lblMarca;
    @FXML private Label lblMaterial;
    @FXML private Label lblUnidad;
    @FXML private Label lblClasificacion;
    @FXML private Label lblDescripcion;
    @FXML private VBox contenedorDetalles;
    @FXML private ScrollPane scrollPane;
    @FXML private Button btnCerrar;
    @FXML private StackPane root;
    @FXML private Pane overlayPane;
    @FXML private Button btnAgregarArticulo;
    @FXML private Region expansor;
    private String presentacionFiltro;
    private String factorFiltro;
    private ItemInventario itemInventario;
    private Stage stage;
    private Runnable onRefresh;
    private OverlayCarga overlayCarga;
    private static final List<String> PRESENTACIONES_COMPRA = List.of(
            "paquete", "pz", "caja", "bolsa", "pieza", "rollo", "litro", "kilogramo", "metro", "unidad"
    );
    private static final int MAX_FILAS = 10;

    @FXML
    public void initialize() {
        if (root != null && overlayPane != null) {
            overlayCarga = new OverlayCarga(root, overlayPane);
        }
        HBox.setHgrow(expansor, Priority.ALWAYS);
        expansor.setMinWidth(10);
        actualizarDatosProducto();
    }

    public void setItemInventario(ItemInventario itemInventario) {
        this.itemInventario = itemInventario;
        this.presentacionFiltro = itemInventario.getPresentacion();
        this.factorFiltro = itemInventario.getFactor();
        if (root != null) {
            root.getStyleClass().add("detalle-inventario");
        }
        if (scrollPane != null) {
            scrollPane.getStyleClass().add("detalle-inventario");
        }
        if (btnCerrar != null) {
            btnCerrar.getStyleClass().add("boton-cerrar");
        }
        if (contenedorDetalles != null) {
            contenedorDetalles.getStyleClass().add("contenedor-detalles");
        }

        actualizarDatosProducto();
        cargarDetalles();
    }

    public void setStage(Stage stage) {
        this.stage = stage;
        if (stage != null) {
            stage.setResizable(false);
            stage.setMinWidth(800);
            stage.setMinHeight(700);
            stage.setWidth(800);
            stage.setHeight(700);
            stage.setMaxWidth(800);
            stage.setMaxHeight(700);
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

    private void actualizarDatosProducto() {
        if (itemInventario == null) {
            return;
        }
        if (lblNombre != null) {
            lblNombre.setText(valorTexto(itemInventario.getProducto()));
        }
        if (lblMarca != null) {
            lblMarca.setText(valorTexto(itemInventario.getMarca()));
        }
        if (lblMaterial != null) {
            lblMaterial.setText(valorTexto(itemInventario.getMaterial()));
        }
        if (lblUnidad != null) {
            lblUnidad.setText(valorTexto(itemInventario.getUnidadMedida()));
        }
        if (lblClasificacion != null) {
            lblClasificacion.setText(valorTexto(itemInventario.getCategoria()));
        }
        if (lblDescripcion != null) {
            lblDescripcion.setText(valorTexto(itemInventario.getDescripcion()));
        }
    }

    private void cargarDetalles() {
        if (itemInventario == null) {
            mostrarSinDetalles();
            return;
        }

        Task<List<UbicacionDetalle>> task = new Task<>() {
            @Override
            protected List<UbicacionDetalle> call() {
                return obtenerUbicaciones();
            }
        };

        task.setOnSucceeded(event -> renderizarDetalles(task.getValue()));
        task.setOnFailed(event -> mostrarSinDetalles());

        Thread hilo = new Thread(task);
        hilo.setDaemon(true);
        hilo.start();
    }

    @FXML
    private void abrirFormularioCompraDesdeBoton() {
        abrirFormularioCompra(null);
    }

    private List<UbicacionDetalle> obtenerUbicaciones() {
        if (itemInventario == null || itemInventario.getClaveProducto() == null) {
            return List.of();
        }

        String presentacion = valorTexto(presentacionFiltro);
        String factor = valorTexto(factorFiltro);

        Map<String, UbicacionDetalle> ubicaciones = new LinkedHashMap<>();

        try (Connection conn = new Conexion().conectar()) {

            // 1. Obtener artículos NO segmentados según la presentación/filtro actual
            String sqlBase = """
            SELECT a.idArticulo,
                   a.lote,
                   a.caducidad,
                   a.presentacion,
                   a.factor,
                   u.nombre AS ubicacion,
                   'no_segmentado' AS tipo
            FROM articulo a
            INNER JOIN detalle_Entrada de ON a.idDetalleEntrada = de.idDetalleEntrada
            INNER JOIN productos p ON de.claveProducto = p.id
            LEFT JOIN ubicaciones u ON a.ubicacion = u.id
            WHERE a.Estado = 'disponible'
              AND p.id = ?
              AND a.presentacion = ?
              AND a.factor = ?
            ORDER BY u.nombre, a.idArticulo
            """;

            try (PreparedStatement ps = conn.prepareStatement(sqlBase)) {
                ps.setString(1, itemInventario.getClaveProducto());
                ps.setString(2, presentacion);
                ps.setString(3, factor);

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        agregarArticuloDesdeResultSet(rs, ubicaciones);
                    }
                }
            }

            // 2. Si el filtro actual es para "pz" factor "1", incluir también los segmentados
            if ("pz".equalsIgnoreCase(presentacion) && "1".equals(factor)) {
                // Buscar artículos segmentados del mismo producto
                String sqlSegmentados = """
                SELECT da.idDetalle,
                       a.lote,
                       a.caducidad,
                       'pz' AS presentacion,
                       '1' AS factor,
                       u.nombre AS ubicacion,
                       'segmentado' AS tipo,
                       a.idArticulo AS idArticuloPadre
                FROM detalleArticulo da
                INNER JOIN articulo a ON da.idArticulo = a.idArticulo
                INNER JOIN detalle_Entrada de ON a.idDetalleEntrada = de.idDetalleEntrada
                INNER JOIN productos p ON de.claveProducto = p.id
                LEFT JOIN ubicaciones u ON da.idUbicacion = u.id
                WHERE da.estado = 'activo'
                  AND a.Estado = 'segmentado'
                  AND p.id = ?
                ORDER BY u.nombre, da.idDetalle
                """;

                try (PreparedStatement ps = conn.prepareStatement(sqlSegmentados)) {
                    ps.setString(1, itemInventario.getClaveProducto());

                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            agregarArticuloDesdeResultSet(rs, ubicaciones);
                        }
                    }
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return new ArrayList<>(ubicaciones.values());
    }

    private void agregarArticuloDesdeResultSet(ResultSet rs, Map<String, UbicacionDetalle> ubicaciones)
            throws SQLException {

        String ubicacion = valorTexto(rs.getString("ubicacion"));
        if (ubicacion.isBlank()) {
            ubicacion = "Sin ubicación";
        }

        UbicacionDetalle detalle = ubicaciones.computeIfAbsent(ubicacion, UbicacionDetalle::new);

        String tipo = rs.getString("tipo");
        boolean esSegmentado = "segmentado".equals(tipo);

        ArticuloDetalle articulo;
        if (esSegmentado) {
            // Para segmentados: usar idDetalle, mostrar como pz factor 1
            articulo = new ArticuloDetalle(
                    rs.getString("idDetalle"),           // idDetalle (S-1, S-2, etc.)
                    rs.getInt("idArticuloPadre"),        // id del artículo padre
                    ubicacion,
                    valorTexto(rs.getString("lote")),
                    valorTexto(rs.getString("caducidad"))
            );
        } else {
            articulo = new ArticuloDetalle(
                    rs.getInt("idArticulo"),
                    ubicacion,
                    valorTexto(rs.getString("lote")),
                    valorTexto(rs.getString("caducidad")),
                    valorTexto(rs.getString("presentacion")),
                    valorTexto(rs.getString("factor"))
            );
        }

        detalle.articulos.add(articulo);
    }

    private void renderizarDetalles(List<UbicacionDetalle> ubicaciones) {
        if (contenedorDetalles == null) {
            return;
        }
        contenedorDetalles.getChildren().clear();

        if (ubicaciones == null || ubicaciones.isEmpty()) {
            mostrarSinDetalles();
            return;
        }

        for (UbicacionDetalle ubicacion : ubicaciones) {
            VBox card = new VBox(8);
            card.getStyleClass().add("tarjeta-ubicacion");
            card.setPrefWidth(750);
            card.setMaxWidth(750);

            HBox header = new HBox(10);
            Label titulo = new Label(String.format("Ubicación: %s (%d artículos)",
                    valorTexto(ubicacion.nombre), ubicacion.articulos.size()));
            titulo.getStyleClass().add("titulo-ubicacion");
            CheckBox chkDetalles = new CheckBox("Mostrar detalles");
            chkDetalles.getStyleClass().add("custom-check");
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            header.getChildren().addAll(titulo, spacer, chkDetalles);

            VBox listaArticulos = new VBox(6);
            listaArticulos.setStyle("-fx-padding: 4 0 0 0;");
            listaArticulos.setVisible(false);
            listaArticulos.setManaged(false);

            if (ubicacion.articulos.isEmpty()) {
                listaArticulos.getChildren().add(new Label("Sin artículos disponibles."));
            } else {
                int index = 1;
                for (ArticuloDetalle articulo : ubicacion.articulos) {
                    HBox fila = new HBox(8);

                    // Construir descripción según tipo
                    StringBuilder descripcion = new StringBuilder();
                    descripcion.append(index++).append(") ");

                    if (articulo.isEsSegmentado()) {
                        // Mostrar información para artículos segmentados
                        descripcion.append("[SEGMENTADO] ");
                        descripcion.append("ID Pieza: ").append(valorTexto(articulo.getIdDetalle()));

                        // Solo mostrar lote si tiene valor
                        String lote = valorTexto(articulo.getLote());
                        if (!lote.isBlank() && !"N/A".equalsIgnoreCase(lote)) {
                            descripcion.append(" | Lote: ").append(lote);
                        }

                        // Solo mostrar caducidad si tiene valor
                        String caducidad = valorTexto(articulo.getCaducidad());
                        if (!caducidad.isBlank() && !"N/A".equalsIgnoreCase(caducidad)) {
                            descripcion.append(" | Caducidad: ").append(caducidad);
                        }

                        // Siempre mostrar presentación y factor (que serán "pz" y "1")
                        descripcion.append(" | Presentación: ").append(valorTexto(articulo.getPresentacion()));
                        descripcion.append(" | Factor: ").append(valorTexto(articulo.getFactor()));
                    } else {
                        // Mostrar información para artículos no segmentados
                        descripcion.append("ID: ").append(articulo.getIdArticulo());

                        // Solo mostrar lote si tiene valor
                        String lote = valorTexto(articulo.getLote());
                        if (!lote.isBlank() && !"N/A".equalsIgnoreCase(lote)) {
                            descripcion.append(" | Lote: ").append(lote);
                        }

                        // Solo mostrar caducidad si tiene valor
                        String caducidad = valorTexto(articulo.getCaducidad());
                        if (!caducidad.isBlank() && !"N/A".equalsIgnoreCase(caducidad)) {
                            descripcion.append(" | Caducidad: ").append(caducidad);
                        }

                        // Mostrar presentación y factor
                        descripcion.append(" | Presentación: ").append(valorTexto(articulo.getPresentacion()));
                        descripcion.append(" | Factor: ").append(valorTexto(articulo.getFactor()));
                    }

                    Label texto = new Label(descripcion.toString());

                    // Configurar botones según tipo de artículo
                    HBox contenedorBotones = new HBox(5);
                    contenedorBotones.setAlignment(Pos.CENTER_RIGHT);

                    // TODOS los artículos pueden editarse (tanto segmentados como no segmentados)
                    Button btnEditar = new Button("Editar");
                    configurarBotonIcono(btnEditar, "/img/editar.png", "Editar");
                    btnEditar.getStyleClass().add("boton-detalle");
                    btnEditar.setOnAction(event -> editarArticulo(articulo));
                    contenedorBotones.getChildren().add(btnEditar);

                    // Todos los artículos pueden eliminarse
                    Button btnEliminar = new Button("Eliminar");
                    configurarBotonIcono(btnEliminar, "/img/eliminar.png", "Eliminar");
                    btnEliminar.getStyleClass().add("boton-detalle");
                    btnEliminar.setOnAction(event -> eliminarArticulo(articulo));
                    contenedorBotones.getChildren().add(btnEliminar);

                    // Configurar layout de la fila
                    Region spacerFila = new Region();
                    HBox.setHgrow(spacerFila, Priority.ALWAYS);

                    fila.getChildren().addAll(texto, spacerFila, contenedorBotones);
                    listaArticulos.getChildren().add(fila);

                    // Agregar separador visual entre filas (excepto en la última)
                    if (index <= ubicacion.articulos.size()) {
                        Separator separador = new Separator();
                        separador.setPadding(new javafx.geometry.Insets(5, 0, 5, 0));
                        listaArticulos.getChildren().add(separador);
                    }
                }
            }

            chkDetalles.selectedProperty().addListener((obs, oldVal, newVal) -> {
                listaArticulos.setVisible(newVal);
                listaArticulos.setManaged(newVal);
            });

            card.getChildren().addAll(header, listaArticulos);
            contenedorDetalles.getChildren().add(card);
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

    private void abrirFormularioCompra(String ubicacion) {
        if (itemInventario == null) {
            return;
        }

        String ubicacionPrefill = ubicacion; // Mantener el parámetro por compatibilidad
        if ("Sin ubicación".equalsIgnoreCase(ubicacionPrefill) || ubicacionPrefill == null) {
            ubicacionPrefill = null;
        }

        ObservableList<compra> itemsEntrada = FXCollections.observableArrayList();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Formularios/view/compraEmergente.fxml"));
            controllerCompraEmergente controlador = new controllerCompraEmergente();
            controlador.setItemsCompra(itemsEntrada);
            controlador.setTituloFormulario("Agregar artículo");
            controlador.setModoAjusteInventario(true);
            controlador.setProductoPrefill(itemInventario.getClaveProducto(), itemInventario.getProducto(),
                    itemInventario.getDescripcion());
            controlador.setUbicacionPrefill(ubicacionPrefill); // Ahora puede ser null

            controlador.setPresentacionPrefill(presentacionFiltro);
            controlador.setFactorPrefill(factorFiltro);

            try {
                Method metodo = controlador.getClass().getMethod("setProductoSoloLectura", boolean.class);
                metodo.invoke(controlador, true);
            } catch (Exception e) {
                System.out.println("Nota: No se pudo hacer el producto de solo lectura");
            }
            // ==========================================================

            loader.setController(controlador);

            Pane formulario = loader.load();
            Stage modal = new Stage();
            modal.initModality(Modality.APPLICATION_MODAL);
            modal.setTitle("Agregar artículo");
            modal.setScene(new Scene(formulario));
            if (btnCerrar != null && btnCerrar.getScene() != null) {
                modal.initOwner(btnCerrar.getScene().getWindow());
            }
            modal.setResizable(false);
            modal.showAndWait();
            if (!itemsEntrada.isEmpty()) {
                model ajusteModel = new model();
                String ajusteId = ajusteModel.registrarAjuste(itemsEntrada, List.of(), "");
                if (ajusteId != null && !ajusteId.isBlank()) {
                    notificarActualizacion();
                    cargarDetalles();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void editarArticulo(ArticuloDetalle articulo) {
        if (articulo == null) {
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();

        // Configurar título según tipo
        if (articulo.isEsSegmentado()) {
            dialog.setTitle("Editar artículo segmentado");
        } else {
            dialog.setTitle("Editar artículo");
        }

        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);
        configurarDialogoModal(dialog);

        // Configurar el DialogPane para evitar el espacio gris inferior
        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.setPadding(new Insets(0));

        // Eliminar estilos por defecto que causan el espacio gris
        dialogPane.setStyle("-fx-background-color: white; -fx-border-color: white;");

        VBox mainContainer = new VBox();
        mainContainer.setStyle("-fx-background-color: white;");
        mainContainer.setPadding(new Insets(0));
        mainContainer.getStylesheets().add(
                getClass().getResource("/Reportes/inventario/style/estilos.css").toExternalForm()
        );

        // Título superior con espacio
        Label lblTitulo = new Label("Editar artículo");
        lblTitulo.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-padding: 15 0 10 0;");
        lblTitulo.setAlignment(Pos.CENTER);
        lblTitulo.setMaxWidth(Double.MAX_VALUE);
        VBox.setMargin(lblTitulo, new Insets(10, 0, 10, 0));

        // Contenido central - Campos del formulario
        VBox contenido = new VBox(15);
        contenido.setPadding(new Insets(0, 20, 0, 20));
        contenido.setStyle("-fx-background-color: white;");

        // Para segmentados, mostrar información del artículo padre
        if (articulo.isEsSegmentado()) {
            Label lblInfo = new Label("NOTA: Los cambios de lote y caducidad se aplicarán a TODAS las piezas segmentadas. La ubicación se cambia solo para esta pieza.");
            lblInfo.setStyle("-fx-font-weight: bold; -fx-text-fill: #e74c3c; -fx-wrap-text: true; -fx-padding: 0 0 10 0;");
            contenido.getChildren().add(lblInfo);
        }

        TextField txtLote = new TextField(valorTexto(articulo.getLote()));
        txtLote.setPrefWidth(180);

        javafx.scene.control.DatePicker dpCaducidad = new javafx.scene.control.DatePicker();
        String caducidadTexto = valorTexto(articulo.getCaducidad());
        if (!caducidadTexto.isBlank()) {
            try {
                dpCaducidad.setValue(LocalDate.parse(caducidadTexto.trim()));
            } catch (DateTimeParseException ignored) {
                dpCaducidad.setValue(null);
            }
        }
        dpCaducidad.setPrefWidth(180);

        ComboBox<String> cbPresentacion = new ComboBox<>();
        cbPresentacion.setItems(FXCollections.observableArrayList(PRESENTACIONES_COMPRA));
        cbPresentacion.setPromptText("Selecciona presentación");
        if (articulo.isEsSegmentado()) {
            cbPresentacion.setValue("pz");
            cbPresentacion.setDisable(true);
        } else {
            String presentacionActual = valorTexto(articulo.getPresentacion());
            if (!presentacionActual.isBlank()) {
                cbPresentacion.setValue(presentacionActual);
            }
        }
        cbPresentacion.setPrefWidth(180);

        TextField txtFactor = new TextField(valorTexto(articulo.getFactor()));
        // Para segmentados, el factor siempre es "1" y no se puede cambiar
        if (articulo.isEsSegmentado()) {
            txtFactor.setText("1");
            txtFactor.setDisable(true);
        }
        txtFactor.setPrefWidth(180);

        ComboBox<String> cbUbicacion = new ComboBox<>();
        cbUbicacion.setItems(FXCollections.observableArrayList(obtenerUbicacionesActivas()));
        cbUbicacion.setPromptText("Selecciona ubicación");
        String ubicacionActual = valorTexto(articulo.getUbicacion());
        if (!ubicacionActual.isBlank() && !"Sin ubicación".equalsIgnoreCase(ubicacionActual)) {
            cbUbicacion.setValue(ubicacionActual);
        }
        cbUbicacion.setPrefWidth(180);

        // ============ ORGANIZACIÓN EN 3 FILAS DE 2 CAMPOS ============

        // Fila 1: Lote y Caducidad
        HBox fila1 = new HBox(15);
        fila1.setAlignment(Pos.CENTER_LEFT);

        VBox vboxLote = new VBox(5);
        Label lblLote = new Label("Lote:");
        vboxLote.getChildren().addAll(lblLote, txtLote);
        HBox.setHgrow(vboxLote, Priority.ALWAYS);

        VBox vboxCaducidad = new VBox(5);
        Label lblCaducidad = new Label("Caducidad:");
        vboxCaducidad.getChildren().addAll(lblCaducidad, dpCaducidad);
        HBox.setHgrow(vboxCaducidad, Priority.ALWAYS);

        fila1.getChildren().addAll(vboxLote, vboxCaducidad);

        // Fila 2: Presentación y Factor
        HBox fila2 = new HBox(15);
        fila2.setAlignment(Pos.CENTER_LEFT);

        VBox vboxPresentacion = new VBox(5);
        Label lblPresentacion = new Label("Presentación:");
        vboxPresentacion.getChildren().addAll(lblPresentacion, cbPresentacion);
        HBox.setHgrow(vboxPresentacion, Priority.ALWAYS);

        VBox vboxFactor = new VBox(5);
        Label lblFactor = new Label("Factor:");
        vboxFactor.getChildren().addAll(lblFactor, txtFactor);
        HBox.setHgrow(vboxFactor, Priority.ALWAYS);

        fila2.getChildren().addAll(vboxPresentacion, vboxFactor);

        // Fila 3: Ubicación (ocupa el ancho completo de 2 columnas)
        HBox fila3 = new HBox();
        fila3.setAlignment(Pos.CENTER_LEFT);

        VBox vboxUbicacion = new VBox(5);
        Label lblUbicacion = new Label("Ubicación:");
        vboxUbicacion.getChildren().addAll(lblUbicacion, cbUbicacion);
        HBox.setHgrow(vboxUbicacion, Priority.ALWAYS);

        // Espaciador a la derecha para mantener la alineación
        Region espaciadorUbicacion = new Region();
        HBox.setHgrow(espaciadorUbicacion, Priority.ALWAYS);

        fila3.getChildren().addAll(vboxUbicacion, espaciadorUbicacion);

        // Agregar todas las filas al contenido
        contenido.getChildren().addAll(fila1, fila2, fila3);

        // ============ BOTONES EN LA PARTE INFERIOR ============

        // Contenedor de botones inferior
        HBox contenedorBotones = new HBox(15);
        contenedorBotones.setAlignment(Pos.CENTER);
        contenedorBotones.setPadding(new Insets(20));
        contenedorBotones.setStyle("-fx-background-color: white; -fx-border-color: #eee; -fx-border-width: 1 0 0 0;");

        // Botón Segmentar (solo para no segmentados)
        Button btnSegmentar = new Button("Segmentar");
        btnSegmentar.getStyleClass().add("boton-form");
        btnSegmentar.setPrefWidth(120);
        btnSegmentar.setOnAction(event -> iniciarSegmentacion(articulo, dialog));

        // Actualizar visibilidad del botón Segmentar
        if (!articulo.isEsSegmentado()) {
            actualizarVisibilidadSegmentar(btnSegmentar, cbPresentacion.getValue());
            cbPresentacion.valueProperty().addListener((obs, oldVal, newVal) ->
                    actualizarVisibilidadSegmentar(btnSegmentar, newVal));
        }

        // CORRECCIÓN: Forma segura de eliminar el ButtonBar original
        // En lugar de hacer cast a StackPane, obtenemos el ButtonBar y lo ocultamos
        ButtonBar buttonBar = (ButtonBar) dialog.getDialogPane().lookup(".button-bar");
        if (buttonBar != null) {
            // Ocultar completamente el ButtonBar
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
        btnAceptar.getStyleClass().add("boton-form");
        btnAceptar.setPrefWidth(120);
        btnAceptar.setOnAction(e -> {
            // Validar campos antes de aceptar
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

            // Si pasa validación, establecer resultado OK
            dialog.setResult(ButtonType.OK);
            dialog.close();
        });

        // Agregar botones al contenedor
        if (!articulo.isEsSegmentado() && btnSegmentar.isVisible()) {
            contenedorBotones.getChildren().addAll(btnSegmentar, btnAceptar);
        } else {
            contenedorBotones.getChildren().addAll(btnAceptar);
        }

        // Agregar todos los componentes al contenedor principal
        mainContainer.getChildren().addAll(lblTitulo, contenido, contenedorBotones);

        // Configurar el crecimiento del contenido
        VBox.setVgrow(contenido, Priority.ALWAYS);

        // Establecer el contenido del diálogo
        dialog.getDialogPane().setContent(mainContainer);

        // Configurar tamaño del diálogo
        dialog.getDialogPane().setPrefWidth(430);
        dialog.getDialogPane().setPrefHeight(450);

        dialog.showAndWait().ifPresent(respuesta -> {
            if (respuesta != ButtonType.OK) {
                return;
            }

            try (Connection conn = new Conexion().conectar()) {
                if (conn == null) {
                    return;
                }
                LocalDate caducidadSeleccionada = obtenerCaducidadSeleccionada(dpCaducidad);

                // Obtener ID de ubicación
                Integer ubicacionId = null;
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

                if (articulo.isEsSegmentado()) {
                    // CASO SEGMENTADO: Actualizar el artículo PADRE (sin ubicación) y solo el segmentado actual

                    // 1. Actualizar el artículo padre en la tabla 'articulo' (SOLO lote y caducidad)
                    try (PreparedStatement ps = conn.prepareStatement(
                            "UPDATE articulo SET lote = ?, caducidad = ? WHERE idArticulo = ?")) {
                        ps.setString(1, txtLote.getText().trim());
                        if (caducidadSeleccionada != null) {
                            ps.setString(2, caducidadSeleccionada.toString());
                        } else {
                            ps.setNull(2, java.sql.Types.DATE);
                        }
                        ps.setInt(3, articulo.getIdArticulo());
                        ps.executeUpdate();
                    }

                    // 2. Actualizar SOLO el segmentado actual en 'detalleArticulo' (ubicación individual)
                    try (PreparedStatement ps = conn.prepareStatement(
                            "UPDATE detalleArticulo SET idUbicacion = ? WHERE idDetalle = ? AND estado = 'activo'")) {
                        if (ubicacionId == null) {
                            ps.setNull(1, java.sql.Types.INTEGER);
                        } else {
                            ps.setInt(1, ubicacionId);
                        }
                        ps.setString(2, articulo.getIdDetalle()); // Usar idDetalle específico, no idArticulo
                        int actualizados = ps.executeUpdate();

                        if (actualizados == 0) {
                            // Si no se encontró el detalle, intentar insertar como nuevo registro
                            try (PreparedStatement psInsert = conn.prepareStatement(
                                    "INSERT INTO detalleArticulo (idDetalle, idArticulo, idUbicacion, estado) VALUES (?, ?, ?, 'activo')")) {
                                psInsert.setString(1, articulo.getIdDetalle());
                                psInsert.setInt(2, articulo.getIdArticulo());
                                if (ubicacionId == null) {
                                    psInsert.setNull(3, java.sql.Types.INTEGER);
                                } else {
                                    psInsert.setInt(3, ubicacionId);
                                }
                                psInsert.executeUpdate();
                            }
                        }
                    }

                    // Mostrar mensaje informativo
                    Alert info = new Alert(Alert.AlertType.INFORMATION);
                    info.setTitle("Actualización completada");
                    info.setHeaderText(null);
                    info.setContentText("Se actualizó el segmentado individual ID: " + articulo.getIdDetalle());
                    info.showAndWait();
                } else {
                    // CASO NO SEGMENTADO: Actualizar solo el artículo individual
                    try (PreparedStatement ps = conn.prepareStatement(
                            "UPDATE articulo SET ubicacion = ?, lote = ?, caducidad = ?, presentacion = ?, factor = ? WHERE idArticulo = ?")) {
                        if (ubicacionId == null) {
                            ps.setNull(1, java.sql.Types.INTEGER);
                        } else {
                            ps.setInt(1, ubicacionId);
                        }
                        ps.setString(2, txtLote.getText().trim());
                        if (caducidadSeleccionada != null) {
                            ps.setString(3, caducidadSeleccionada.toString());
                        } else {
                            ps.setNull(3, java.sql.Types.DATE);
                        }
                        String presentacionSeleccionada = cbPresentacion.getValue();
                        ps.setString(4, presentacionSeleccionada != null ? presentacionSeleccionada : "");
                        ps.setString(5, txtFactor.getText().trim());
                        ps.setInt(6, articulo.getIdArticulo());
                        ps.executeUpdate();
                    }
                }

                notificarActualizacion();
                cargarDetalles();

            } catch (SQLException e) {
                e.printStackTrace();
                mostrarAdvertencia("Error", "No se pudo actualizar el artículo: " + e.getMessage());
            }
        });
    }

    private void eliminarArticulo(ArticuloDetalle articulo) {
        if (articulo == null) {
            return;
        }

        String mensaje;
        String titulo;

        if (articulo.isEsSegmentado()) {
            titulo = "Eliminar pieza segmentada";
            mensaje = "¿Deseas eliminar esta pieza segmentada (ID: " + articulo.getIdDetalle() + ")?\n\n" +
                    "NOTA: Solo se eliminará esta pieza específica, no todas las del grupo.";
        } else {
            titulo = "Eliminar artículo";
            mensaje = "¿Deseas eliminar el artículo (ID: " + articulo.getIdArticulo() + ")?";
        }

        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle(titulo);
        confirmacion.setHeaderText(null);
        confirmacion.setContentText(mensaje);

        confirmacion.showAndWait().ifPresent(respuesta -> {
            if (respuesta != ButtonType.OK) {
                return;
            }

            try (Connection conn = new Conexion().conectar()) {
                if (conn == null) {
                    return;
                }

                if (articulo.isEsSegmentado()) {
                    // Para segmentados: inactivar solo esta pieza en detalleArticulo
                    try (PreparedStatement ps = conn.prepareStatement(
                            "UPDATE detalleArticulo SET estado = 'inactivo' WHERE idDetalle = ?")) {
                        ps.setString(1, articulo.getIdDetalle());
                        int afectados = ps.executeUpdate();

                        if (afectados > 0) {
                            // Verificar si quedan segmentados activos para este artículo padre
                            try (PreparedStatement psVerificar = conn.prepareStatement(
                                    "SELECT COUNT(*) FROM detalleArticulo WHERE idArticulo = ? AND estado = 'activo'")) {
                                psVerificar.setInt(1, articulo.getIdArticulo());
                                try (ResultSet rs = psVerificar.executeQuery()) {
                                    if (rs.next() && rs.getInt(1) == 0) {
                                        // Si no quedan segmentados activos, cambiar estado del artículo padre
                                        try (PreparedStatement psActualizarPadre = conn.prepareStatement(
                                                "UPDATE articulo SET Estado = 'disponible', segmentado = 0 WHERE idArticulo = ?")) {
                                            psActualizarPadre.setInt(1, articulo.getIdArticulo());
                                            psActualizarPadre.executeUpdate();
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Para no segmentados: marcar como eliminado en articulo
                    try (PreparedStatement ps = conn.prepareStatement(
                            "UPDATE articulo SET Estado = 'eliminado' WHERE idArticulo = ?")) {
                        ps.setInt(1, articulo.getIdArticulo());
                        ps.executeUpdate();
                    }
                }

                notificarActualizacion();
                cargarDetalles();

            } catch (SQLException e) {
                e.printStackTrace();
                mostrarAdvertencia("Error", "No se pudo eliminar el artículo: " + e.getMessage());
            }
        });
    }

    private void notificarActualizacion() {
        if (onRefresh != null) {
            Platform.runLater(onRefresh);
        }
    }

    private void mostrarAdvertencia(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    private LocalDate obtenerCaducidadSeleccionada(javafx.scene.control.DatePicker dpCaducidad) {
        if (dpCaducidad == null) {
            return null;
        }
        String texto = dpCaducidad.getEditor() != null ? dpCaducidad.getEditor().getText() : "";
        if (texto == null || texto.trim().isEmpty()) {
            return null;
        }
        LocalDate valor = dpCaducidad.getValue();
        if (valor != null) {
            return valor;
        }
        try {
            return LocalDate.parse(texto.trim());
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private String valorTexto(String texto) {
        return texto == null ? "" : texto;
    }

    private List<String> obtenerUbicacionesActivas() {
        return new Operaciones.compra.model.model().obtenerNombresUbicaciones();
    }

    private void configurarBotonIcono(Button boton, String rutaIcono, String textoFallback) {
        boton.getStyleClass().add("boton-icono");
        try {
            ImageView icono = new ImageView(new Image(getClass().getResourceAsStream(rutaIcono)));
            icono.setFitWidth(16);
            icono.setFitHeight(16);
            boton.setGraphic(icono);
            boton.setText("");
        } catch (Exception e) {
            boton.setText(textoFallback);
        }
    }

    private void agregarEstilosDialogo(Dialog<ButtonType> dialog) {
        DialogPane pane = dialog.getDialogPane();

        pane.getStylesheets().add(
                getClass().getResource("/Reportes/inventario/style/estilos.css").toExternalForm()
        );

        Button btnOk = (Button) pane.lookupButton(ButtonType.OK);
        Button btnCancel = (Button) pane.lookupButton(ButtonType.CANCEL);

        if (btnOk != null) btnOk.getStyleClass().add("boton-formulario");
        if (btnCancel != null) btnCancel.getStyleClass().add("boton-formulario");

        if (btnCancel != null) ButtonBar.setButtonData(btnCancel, ButtonBar.ButtonData.CANCEL_CLOSE);
        if (btnOk != null) ButtonBar.setButtonData(btnOk, ButtonBar.ButtonData.OK_DONE);

        // 🔥 Obtiene el ButtonBar real por lookup y define el orden manual
        ButtonBar bar = (ButtonBar) pane.lookup(".button-bar");
        if (bar != null) {
            bar.setButtonOrder(ButtonBar.BUTTON_ORDER_NONE);
        }
    }

    private void iniciarSegmentacion(ArticuloDetalle articulo, Dialog<ButtonType> dialogPadre) {
        if (articulo == null || articulo.idArticulo <= 0) {
            return;
        }
        int factor = parseFactor(articulo.factor);
        if (factor <= 0) {
            mostrarAdvertencia("Factor inválido", "El factor debe ser un número mayor a cero.");
            return;
        }

        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Segmentar artículo");
        confirmacion.setHeaderText(null);
        confirmacion.setContentText("Al segmentar este artículo se dividirá el producto en " +
                factor + " piezas.\n¿Deseas continuar?");

        ButtonType btnCancelar = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType btnAceptar = new ButtonType("Aceptar", ButtonBar.ButtonData.OK_DONE);
        confirmacion.getButtonTypes().setAll(btnCancelar, btnAceptar);
        configurarOrdenBotones(confirmacion.getDialogPane(), btnCancelar, btnAceptar);

        confirmacion.initModality(Modality.APPLICATION_MODAL);
        if (dialogPadre != null) {
            confirmacion.initOwner(dialogPadre.getDialogPane().getScene().getWindow());
        }
        confirmacion.showAndWait().ifPresent(respuesta -> {
            if (respuesta != btnAceptar) {
                return;
            }
            abrirFormularioSegmentacion(articulo, factor, dialogPadre);
        });
    }

    private void abrirFormularioSegmentacion(ArticuloDetalle articulo, int factor, Dialog<ButtonType> dialogPadre) {
        if (articulo == null || factor <= 0) {
            return;
        }

        // Crear ventana de diálogo
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Segmentar artículo");

        // Configurar fondo blanco en el diálogo
        dialog.getDialogPane().setStyle("-fx-background-color: white; -fx-border-color: white;");

        // Crear tipos de botones
        ButtonType btnCancelarType = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType btnAceptarType = new ButtonType("Aceptar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnCancelarType, btnAceptarType);

        // Ocultar completamente el ButtonBar por defecto
        ButtonBar buttonBar = (ButtonBar) dialog.getDialogPane().lookup(".button-bar");
        if (buttonBar != null) {
            buttonBar.setVisible(false);
            buttonBar.setManaged(false);
            buttonBar.setPrefHeight(0);
            buttonBar.setMinHeight(0);
            buttonBar.setMaxHeight(0);
        }

        // También ocultar los botones individuales del ButtonBar
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

        configurarDialogoModal(dialog);
        if (dialogPadre != null) {
            dialog.initOwner(dialogPadre.getDialogPane().getScene().getWindow());
        }

        // ============ CREAR ESTRUCTURA PRINCIPAL ============
        BorderPane borderPane = new BorderPane();
        borderPane.setStyle("-fx-background-color: white;");

        // ============ PARTE SUPERIOR - TÍTULO ============
        VBox topBox = new VBox();
        topBox.setAlignment(Pos.CENTER);
        topBox.setStyle("-fx-background-color: white;");
        topBox.setPadding(new Insets(15, 0, 10, 0));

        Label lblTitulo = new Label("Segmentar artículo");
        lblTitulo.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #333333;");

        topBox.getChildren().add(lblTitulo);
        borderPane.setTop(topBox);

        // ============ PARTE CENTRAL - CONTENIDO ============
        VBox centerBox = new VBox(15);
        centerBox.setPadding(new Insets(20, 25, 20, 25));
        centerBox.setStyle("-fx-background-color: white;");

        // Descripción del artículo
        Label lblDescripcionArticulo = new Label(obtenerDescripcionArticulo(articulo, factor));
        lblDescripcionArticulo.setWrapText(true);
        lblDescripcionArticulo.setStyle("-fx-font-weight: bold; -fx-text-fill: #2c3e50; -fx-font-size: 12px; -fx-padding: 0 0 10 0;");

        // Título de ubicaciones
        Label lblUbicaciones = new Label("Ubicaciones para segmentar:");
        lblUbicaciones.setStyle("-fx-font-size: 13px; -fx-font-weight: bold;");

        // Contenedor para las filas de ubicaciones
        VBox contenedorUbicaciones = new VBox(8);
        contenedorUbicaciones.setStyle("-fx-background-color: white;"); // Cambia a blanco
        contenedorUbicaciones.setMinHeight(200); // Altura mínima para que siempre se vea blanco

        List<UbicacionFila> filas = new ArrayList<>();

        // Agregar la primera fila
        agregarFilaUbicacion(contenedorUbicaciones, filas, true);

        // ScrollPane para las ubicaciones - CONFIGURAR CORRECTAMENTE
        ScrollPane scroll = new ScrollPane(contenedorUbicaciones);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        scroll.setStyle("-fx-background-color: white; " + "-fx-background-radius: 4;");

        // También configurar el viewport para que sea blanco
        scroll.viewportBoundsProperty().addListener((obs, oldBounds, newBounds) -> {
            // Asegurar que el contenido ocupe toda la altura disponible
            contenedorUbicaciones.setMinHeight(newBounds.getHeight());
        });

        scroll.setPrefViewportHeight(280);
        scroll.setMinViewportHeight(200);

        // Agregar elementos al centro
        centerBox.getChildren().addAll(lblDescripcionArticulo, lblUbicaciones, scroll);

        // Poner el centerBox directamente en el BorderPane
        borderPane.setCenter(centerBox);

        // ============ PARTE INFERIOR - BOTONES ============
        HBox bottomBox = new HBox(15);
        bottomBox.setAlignment(Pos.CENTER);
        bottomBox.setStyle("-fx-background-color: white; -fx-border-color: #e0e0e0; -fx-border-width: 1 0 0 0;");
        bottomBox.setPadding(new Insets(15, 0, 15, 0));

        Button btnCancelar = new Button("Cancelar");
        btnCancelar.getStyleClass().add("boton");
        btnCancelar.setPrefWidth(110);
        btnCancelar.setPrefHeight(35);
        btnCancelar.setOnAction(e -> {
            dialog.setResult(btnCancelarType);
            dialog.close();
        });

        Button btnAceptar = new Button("Aceptar");
        btnAceptar.getStyleClass().add("boton");
        btnAceptar.setPrefWidth(110);
        btnAceptar.setPrefHeight(35);

        bottomBox.getChildren().addAll(btnCancelar, btnAceptar);
        borderPane.setBottom(bottomBox);

        // ============ CONFIGURAR DIÁLOGO ============
        dialog.getDialogPane().setContent(borderPane);
        dialog.getDialogPane().setPrefWidth(480);
        dialog.getDialogPane().setPrefHeight(580);
        dialog.getDialogPane().setMinWidth(450);
        dialog.getDialogPane().setMinHeight(500);

        // Cargar estilos del formulario de ubicaciones
        try {
            dialog.getDialogPane().getStylesheets().add(
                    getClass().getResource("/Formularios/style/estilos.css").toExternalForm()
            );
        } catch (Exception e) {
            System.out.println("No se pudo cargar el CSS, usando estilos inline");
        }

        // ============ CONFIGURAR COMPORTAMIENTO ============
        AtomicReference<List<UbicacionCantidad>> seleccionadasRef = new AtomicReference<>(Collections.emptyList());

        btnAceptar.setOnAction(event -> {
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
            dialog.setResult(btnAceptarType);
            dialog.close();
        });

        // También manejar el botón Cancelar con ESC
        dialog.getDialogPane().setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                dialog.setResult(btnCancelarType);
                dialog.close();
            }
        });

        // Mostrar diálogo y procesar resultado
        dialog.showAndWait().ifPresent(respuesta -> {
            if (respuesta != btnAceptarType) {
                return;
            }
            ejecutarSegmentacion(articulo, factor, seleccionadasRef.get(), dialogPadre);
        });
    }

    private void ejecutarSegmentacion(ArticuloDetalle articulo, int factor, List<UbicacionCantidad> ubicaciones,
                                      Dialog<ButtonType> dialogPadre) {
        if (articulo == null || ubicaciones == null || ubicaciones.isEmpty()) {
            return;
        }

        if (dialogPadre != null) {
            dialogPadre.close();
        }
        if (overlayCarga != null) {
            overlayCarga.mostrar();
        }

        Task<String> task = new Task<>() {
            @Override
            protected String call() throws Exception {
                try (Connection conn = new Conexion().conectar()) {
                    if (conn == null) {
                        throw new SQLException("Sin conexión a la base de datos");
                    }
                    conn.setAutoCommit(false);

                    try (PreparedStatement ps = conn.prepareStatement(
                            "UPDATE articulo SET segmentado = 1, Estado = 'segmentado' WHERE idArticulo = ?")) {
                        ps.setInt(1, articulo.idArticulo);
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
                                ps.setString(2, String.valueOf(articulo.idArticulo));
                                ps.setInt(3, ubicacion.id);
                                ps.addBatch();
                            }
                        }
                        ps.executeBatch();
                    }

                    conn.commit();
                }

                return construirMensajeSegmentacion(articulo, factor, ubicaciones);
            }
        };

        task.setOnSucceeded(event -> {
            if (overlayCarga != null) {
                overlayCarga.ocultar();
            }
            String mensaje = task.getValue();
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Segmentación completada");
            alert.setHeaderText(null);
            alert.setContentText(mensaje);
            alert.showAndWait();
            notificarActualizacion();
            cargarDetalles();
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

    private String obtenerDescripcionArticulo(ArticuloDetalle articulo, int factor) {
        String producto = itemInventario != null ? valorTexto(itemInventario.getProducto()) : "";
        String descripcion = itemInventario != null ? valorTexto(itemInventario.getDescripcion()) : "";
        String presentacion = valorTexto(articulo.presentacion);
        return "Producto: " + producto + " | Presentación: " + presentacion +
                " | Factor: " + factor + "\nDescripción: " + descripcion;
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

    private void agregarFilaUbicacion(VBox contenedor, List<UbicacionFila> filas, boolean inicial) {
        if (filas.size() >= MAX_FILAS) {
            mostrarAdvertencia("Límite alcanzado",
                    "Solo se pueden agregar hasta " + MAX_FILAS + " ubicaciones.");
            return;
        }
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
        TextField txtCantidad = new TextField();
        vboxCantidad.getChildren().addAll(labelCantidad, txtCantidad);
        HBox.setHgrow(vboxCantidad, Priority.ALWAYS);

        VBox vboxBoton = new VBox(5);
        Button boton = new Button(inicial ? "+" : "-");
        boton.getStyleClass().add("botonAgregarUbi");
        vboxBoton.setAlignment(Pos.BOTTOM_CENTER);
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

    private void configurarOrdenBotones(DialogPane pane, ButtonType cancelar, ButtonType aceptar) {
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

    private void configurarDialogoAcciones(Dialog<ButtonType> dialog, ButtonType cancelar, ButtonType aceptar) {
        DialogPane pane = dialog.getDialogPane();
        pane.getStylesheets().add(
                getClass().getResource("/Reportes/inventario/style/estilos.css").toExternalForm()
        );
        configurarOrdenBotones(pane, cancelar, aceptar);
    }

    private void configurarDialogoModal(Dialog<ButtonType> dialog) {
        if (dialog == null) {
            return;
        }
        dialog.initModality(Modality.APPLICATION_MODAL);
        if (btnCerrar != null && btnCerrar.getScene() != null) {
            dialog.initOwner(btnCerrar.getScene().getWindow());
        }
    }

    private String construirMensajeSegmentacion(ArticuloDetalle articulo, int factor, List<UbicacionCantidad> ubicaciones) {
        String producto = itemInventario != null ? valorTexto(itemInventario.getProducto()) : "";
        String presentacion = valorTexto(articulo.presentacion);
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

    private void actualizarVisibilidadSegmentar(Button botonSegmentar, String presentacion) {
        if (botonSegmentar == null) {
            return;
        }
        String valor = valorTexto(presentacion).trim().toLowerCase();
        boolean mostrar = !"pz".equals(valor) && !"pieza".equals(valor);
        botonSegmentar.setVisible(mostrar);
        botonSegmentar.setManaged(mostrar);
    }

    private static class UbicacionDetalle {
        private final String nombre;
        private final List<ArticuloDetalle> articulos = new ArrayList<>();

        private UbicacionDetalle(String nombre) {
            this.nombre = nombre;
        }
    }

    private static class ArticuloDetalle {
        private int idArticulo; // Para no segmentados, o id padre para segmentados
        private String idDetalle; // Solo para segmentados (ej: "S-1", "S-2")
        private final String ubicacion;
        private final String lote;
        private final String caducidad;
        private final String presentacion;
        private final String factor;
        private final boolean esSegmentado;

        // Constructor para NO segmentados
        private ArticuloDetalle(int idArticulo, String ubicacion, String lote,
                                String caducidad, String presentacion, String factor) {
            this.idArticulo = idArticulo;
            this.idDetalle = null;
            this.ubicacion = ubicacion;
            this.lote = lote;
            this.caducidad = caducidad;
            this.presentacion = presentacion;
            this.factor = factor;
            this.esSegmentado = false;
        }

        // Constructor para SEGMENTADOS (siempre pz factor 1)
        private ArticuloDetalle(String idDetalle, int idArticuloPadre, String ubicacion,
                                String lote, String caducidad) {
            this.idArticulo = idArticuloPadre;
            this.idDetalle = idDetalle;
            this.ubicacion = ubicacion;
            this.lote = lote;
            this.caducidad = caducidad;
            this.presentacion = "pz";
            this.factor = "1";
            this.esSegmentado = true;
        }

        // Getters (mantener públicos)
        public int getIdArticulo() { return idArticulo; }
        public String getIdDetalle() { return idDetalle; }
        public String getUbicacion() { return ubicacion; }
        public String getLote() { return lote; }
        public String getCaducidad() { return caducidad; }
        public String getPresentacion() { return presentacion; }
        public String getFactor() { return factor; }
        public boolean isEsSegmentado() { return esSegmentado; }

        public String getIdentificadorUnico() {
            return esSegmentado ? idDetalle : String.valueOf(idArticulo);
        }

        public Object getIdentificadorBD() {
            if (esSegmentado) {
                return idDetalle;
            } else {
                return idArticulo;
            }
        }

        public String getTipoDescripcion() {
            return esSegmentado ? "Pieza segmentada" : "Artículo completo";
        }
    }

    private static class UbicacionFila {
        private final HBox contenedor;
        private final ComboBox<String> combo;
        private final TextField cantidad;

        private UbicacionFila(HBox contenedor, ComboBox<String> combo, TextField cantidad) {
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
