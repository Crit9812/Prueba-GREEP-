package Reportes.inventario.controller;

import Formularios.controller.controllerCompraEmergente;
import Operaciones.ajusteInventario.model.model;
import Operaciones.compra.model.compra;
import Reportes.inventario.model.ItemInventario;
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
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    private ItemInventario itemInventario;
    private Stage stage;
    private Runnable onRefresh;
    private static final List<String> PRESENTACIONES_COMPRA = List.of(
            "paquete", "pz", "caja", "bolsa", "pieza", "rollo", "litro", "kilogramo", "metro", "unidad"
    );

    @FXML
    public void initialize() {
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

    private void agregarEstilosDialogo(Dialog<ButtonType> dialog) {
        dialog.getDialogPane().getStylesheets().add(
                getClass().getResource("/Reportes/inventario/style/estilos.css").toExternalForm());
        dialog.getDialogPane().lookupButton(ButtonType.OK).getStyleClass().add("boton-formulario");
        dialog.getDialogPane().lookupButton(ButtonType.CANCEL).getStyleClass().add("boton-formulario");
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
}
