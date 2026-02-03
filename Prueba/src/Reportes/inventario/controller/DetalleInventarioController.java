package Reportes.inventario.controller;

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
import javafx.scene.control.ButtonBar; // ✅ Import necesario
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
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.geometry.Pos;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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

    private ItemInventario itemInventario;
    private Stage stage;
    private Runnable onRefresh;
    private OverlayCarga overlayCarga;
    private static final List<String> PRESENTACIONES_COMPRA = List.of(
            "paquete", "pz", "caja", "bolsa", "pieza", "rollo", "litro", "kilogramo", "metro", "unidad"
    );

    @FXML
    public void initialize() {
        if (root != null && overlayPane != null) {
            overlayCarga = new OverlayCarga(root, overlayPane);
        }
        actualizarDatosProducto();
    }

    public void setItemInventario(ItemInventario itemInventario) {
        this.itemInventario = itemInventario;
        actualizarDatosProducto();
        cargarDetalles();
    }

    public void setStage(Stage stage) {
        this.stage = stage;
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

    private List<UbicacionDetalle> obtenerUbicaciones() {
        if (itemInventario == null || itemInventario.getClaveProducto() == null) {
            return List.of();
        }

        String sql = """
                SELECT a.idArticulo,
                       a.lote,
                       a.caducidad,
                       a.presentacion,
                       a.factor,
                       u.nombre AS ubicacion
                FROM articulo a
                INNER JOIN detalle_Entrada de ON a.idDetalleEntrada = de.idDetalleEntrada
                INNER JOIN productos p ON de.claveProducto = p.id
                LEFT JOIN ubicaciones u ON a.ubicacion = u.id
                WHERE a.Estado = 'disponible'
                  AND p.id = ?
                ORDER BY u.nombre, a.idArticulo
                """;

        Map<String, UbicacionDetalle> ubicaciones = new LinkedHashMap<>();

        try (Connection conn = new Conexion().conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, itemInventario.getClaveProducto());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String ubicacion = valorTexto(rs.getString("ubicacion"));
                    if (ubicacion.isBlank()) {
                        ubicacion = "Sin ubicación";
                    }
                    UbicacionDetalle detalle = ubicaciones.computeIfAbsent(ubicacion, UbicacionDetalle::new);
                    detalle.articulos.add(new ArticuloDetalle(
                            rs.getInt("idArticulo"),
                            ubicacion,
                            valorTexto(rs.getString("lote")),
                            valorTexto(rs.getString("caducidad")),
                            valorTexto(rs.getString("presentacion")),
                            valorTexto(rs.getString("factor"))
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return new ArrayList<>(ubicaciones.values());
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
            card.setStyle("-fx-padding: 12; -fx-background-color: #f5f5f5; " +
                    "-fx-border-color: #ddd; -fx-border-radius: 6; -fx-background-radius: 6;");

            HBox header = new HBox(10);
            Label titulo = new Label(String.format("Ubicación: %s (%d artículos)",
                    valorTexto(ubicacion.nombre), ubicacion.articulos.size()));
            titulo.setStyle("-fx-font-weight: bold; -fx-text-fill: #2c3e50;");
            CheckBox chkDetalles = new CheckBox("Mostrar detalles");
            chkDetalles.getStyleClass().add("custom-check");
            Button btnAgregar = new Button("Agregar");
            configurarBotonIcono(btnAgregar, "/img/agregar.png", "Agregar");
            btnAgregar.setOnAction(event -> abrirFormularioCompra(ubicacion.nombre));

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            header.getChildren().addAll(titulo, spacer, chkDetalles, btnAgregar);

            VBox listaArticulos = new VBox(6);
            listaArticulos.setStyle("-fx-padding: 4 0 0 18;");
            listaArticulos.setVisible(false);
            listaArticulos.setManaged(false);

            if (ubicacion.articulos.isEmpty()) {
                listaArticulos.getChildren().add(new Label("Sin artículos disponibles."));
            } else {
                int index = 1;
                for (ArticuloDetalle articulo : ubicacion.articulos) {
                    HBox fila = new HBox(8);
                    String descripcion = String.format(
                            "%d) Lote: %s | Caducidad: %s | Presentación: %s | Factor: %s",
                            index++,
                            valorTexto(articulo.lote),
                            valorTexto(articulo.caducidad),
                            valorTexto(articulo.presentacion),
                            valorTexto(articulo.factor)
                    );
                    Label texto = new Label(descripcion);
                    Button btnEditar = new Button("Editar");
                    configurarBotonIcono(btnEditar, "/img/editar.png", "Editar");
                    btnEditar.setOnAction(event -> editarArticulo(articulo));
                    Button btnEliminar = new Button("Eliminar");
                    configurarBotonIcono(btnEliminar, "/img/eliminar.png", "Eliminar");
                    btnEliminar.setOnAction(event -> eliminarArticulo(articulo));
                    fila.getChildren().addAll(texto, btnEditar, btnEliminar);
                    listaArticulos.getChildren().add(fila);
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
        String ubicacionPrefill = ubicacion;
        if ("Sin ubicación".equalsIgnoreCase(ubicacionPrefill)) {
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
            controlador.setUbicacionPrefill(ubicacionPrefill);
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
        if (articulo == null || articulo.idArticulo <= 0) {
            return;
        }
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Editar artículo");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);
        configurarDialogoModal(dialog);

        // ✅ aplica estilos + orden de botones
        agregarEstilosDialogo(dialog);

        VBox contenido = new VBox(10);
        ComboBox<String> cbUbicacion = new ComboBox<>();
        cbUbicacion.setItems(FXCollections.observableArrayList(obtenerUbicacionesActivas()));
        cbUbicacion.setPromptText("Selecciona ubicación");
        cbUbicacion.getStyleClass().add("textfield");
        String ubicacionActual = valorTexto(articulo.ubicacion);
        if (!ubicacionActual.isBlank() && !"Sin ubicación".equalsIgnoreCase(ubicacionActual)) {
            cbUbicacion.setValue(ubicacionActual);
        }

        TextField txtLote = new TextField(valorTexto(articulo.lote));
        txtLote.getStyleClass().add("textfield");

        javafx.scene.control.DatePicker dpCaducidad = new javafx.scene.control.DatePicker();
        dpCaducidad.getStyleClass().add("textfield");
        if (articulo.caducidad != null && !articulo.caducidad.isBlank()) {
            try {
                dpCaducidad.setValue(java.time.LocalDate.parse(articulo.caducidad.trim()));
            } catch (java.time.format.DateTimeParseException ignored) {
                dpCaducidad.setValue(null);
            }
        }

        ComboBox<String> cbPresentacion = new ComboBox<>();
        cbPresentacion.setItems(FXCollections.observableArrayList(PRESENTACIONES_COMPRA));
        cbPresentacion.setPromptText("Selecciona presentación");
        cbPresentacion.getStyleClass().add("textfield");
        String presentacionActual = valorTexto(articulo.presentacion);
        if (!presentacionActual.isBlank()) {
            cbPresentacion.setValue(presentacionActual);
        }

        TextField txtFactor = new TextField(valorTexto(articulo.factor));
        txtFactor.getStyleClass().add("textfield");

        GridPane formulario = new GridPane();
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

        Button btnSegmentar = new Button("Segmentar");
        btnSegmentar.getStyleClass().add("boton-formulario");
        btnSegmentar.setOnAction(event -> iniciarSegmentacion(articulo, dialog));
        Button btnEliminar = new Button("Eliminar");
        btnEliminar.getStyleClass().add("boton-formulario");
        btnEliminar.setOnAction(event -> {
            eliminarArticulo(articulo);
            dialog.setResult(ButtonType.CANCEL);
            dialog.close();
        });

        HBox accionesSecundarias = new HBox(12, btnEliminar, btnSegmentar);
        accionesSecundarias.setAlignment(Pos.CENTER_RIGHT);

        actualizarVisibilidadSegmentar(btnSegmentar, cbPresentacion.getValue());
        cbPresentacion.valueProperty().addListener((obs, oldVal, newVal) ->
                actualizarVisibilidadSegmentar(btnSegmentar, newVal));

        contenido.getChildren().addAll(formulario, accionesSecundarias);
        dialog.getDialogPane().setContent(contenido);

        dialog.showAndWait().ifPresent(respuesta -> {
            if (respuesta != ButtonType.OK) {
                return;
            }
            try (Connection conn = new Conexion().conectar()) {
                if (conn == null) {
                    return;
                }

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

                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE articulo SET ubicacion = ?, lote = ?, caducidad = ?, presentacion = ?, factor = ? WHERE idArticulo = ?")) {
                    if (ubicacionId == null) {
                        ps.setNull(1, java.sql.Types.INTEGER);
                    } else {
                        ps.setInt(1, ubicacionId);
                    }
                    ps.setString(2, txtLote.getText());
                    String caducidadTexto = dpCaducidad.getValue() != null ? dpCaducidad.getValue().toString() : "";
                    ps.setString(3, caducidadTexto);
                    String presentacionSeleccionada = cbPresentacion.getValue();
                    ps.setString(4, presentacionSeleccionada != null ? presentacionSeleccionada : "");
                    ps.setString(5, txtFactor.getText());
                    ps.setInt(6, articulo.idArticulo);
                    ps.executeUpdate();
                }

                notificarActualizacion();
                cargarDetalles();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    private void eliminarArticulo(ArticuloDetalle articulo) {
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
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE articulo SET Estado = 'eliminado' WHERE idArticulo = ?")) {
                    ps.setInt(1, articulo.idArticulo);
                    ps.executeUpdate();
                }
                notificarActualizacion();
                cargarDetalles();
            } catch (SQLException e) {
                e.printStackTrace();
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

    // ✅ CORREGIDO: sin getButtonBar() (no existe en tu JavaFX)
    // ✅ Cancelar izquierda y OK derecha
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

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Segmentar artículo");
        ButtonType btnCancelar = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType btnAceptar = new ButtonType("Aceptar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnCancelar, btnAceptar);
        configurarDialogoAcciones(dialog, btnCancelar, btnAceptar);
        configurarDialogoModal(dialog);
        if (dialogPadre != null) {
            dialog.initOwner(dialogPadre.getDialogPane().getScene().getWindow());
        }

        VBox contenido = new VBox(12);
        Label lblDescripcionArticulo = new Label(obtenerDescripcionArticulo(articulo, factor));
        lblDescripcionArticulo.setWrapText(true);
        lblDescripcionArticulo.setStyle("-fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        Label lblUbicaciones = new Label("Ubicaciones para segmentar:");
        lblUbicaciones.setStyle("-fx-font-weight: bold;");

        VBox contenedorUbicaciones = new VBox(10);
        List<UbicacionFila> filas = new ArrayList<>();
        agregarFilaUbicacion(contenedorUbicaciones, filas, true);

        ScrollPane scroll = new ScrollPane(contenedorUbicaciones);
        scroll.setFitToWidth(true);
        scroll.setPrefViewportHeight(320);
        contenido.getChildren().addAll(lblDescripcionArticulo, lblUbicaciones, scroll);
        dialog.getDialogPane().setContent(contenido);
        dialog.getDialogPane().setPrefWidth(520);
        dialog.getDialogPane().setPrefHeight(520);

        AtomicReference<List<UbicacionCantidad>> seleccionadasRef = new AtomicReference<>(Collections.emptyList());
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

                    try (PreparedStatement ps = conn.prepareStatement(
                            "INSERT INTO detalleArticulo (idArticulo, idUbicacion) VALUES (?, ?)")) {
                        for (UbicacionCantidad ubicacion : ubicaciones) {
                            for (int i = 0; i < ubicacion.cantidad; i++) {
                                ps.setInt(1, articulo.idArticulo);
                                ps.setInt(2, ubicacion.id);
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
        private final int idArticulo;
        private final String ubicacion;
        private final String lote;
        private final String caducidad;
        private final String presentacion;
        private final String factor;

        private ArticuloDetalle(int idArticulo, String ubicacion, String lote,
                                String caducidad, String presentacion, String factor) {
            this.idArticulo = idArticulo;
            this.ubicacion = ubicacion;
            this.lote = lote;
            this.caducidad = caducidad;
            this.presentacion = presentacion;
            this.factor = factor;
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
