package Reportes.inventario.controller;

import Compartido.controller.encabezadoController;
import Compartido.controller.navbarController;
import Compartido.helper.SelectorColumnasPopup;
import Compartido.helper.SelectorOrdenPopup;
import Reportes.inventario.model.ItemInventario;
import conexion.Conexion;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class MainController {

    @FXML private StackPane root;
    @FXML private BorderPane paneNavbar;
    @FXML private VBox navbar;
    @FXML private VBox contenedor;
    @FXML private Pane overlayPane;

    @FXML private Label lblQuitar;
    @FXML private Label lblOrdenar;
    @FXML private Label lblImportar;
    @FXML private Label lblExportar;
    @FXML private Region expansorDetalles;

    @FXML private Region expansor;
    @FXML private Label lblVista;
    @FXML private Label lblDescargar;
    @FXML private javafx.scene.control.CheckBox chkInventarioDetallado;

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
    private final Map<TableColumn<ItemInventario, ?>, Boolean> visibilidadResumen = new HashMap<>();
    private final Map<TableColumn<ItemInventario, ?>, Boolean> visibilidadDetallado = new HashMap<>();
    private String criterioOrden = "id";
    private String direccionOrden = "asc";

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
            lblImportar.setMinWidth(Region.USE_PREF_SIZE);
            lblExportar.setMinWidth(Region.USE_PREF_SIZE);
            HBox.setHgrow(expansorDetalles, Priority.ALWAYS);
            expansorDetalles.setMinWidth(10);
            lblVista.setMinWidth(Region.USE_PREF_SIZE);
            lblDescargar.setMinWidth(Region.USE_PREF_SIZE);

            // Tabla
            contenedorTabla.prefHeightProperty().bind(contenedor.heightProperty().multiply(0.81));
            contenidoTabla.prefHeightProperty().bind(contenedorTabla.heightProperty().multiply(0.9));

            paneNavbarController.setTitulo("Inventario", "#ffffff");

            configurarColumnasTabla();
            configurarInventarioDetallado();
            cargarInventarioDisponible(false);
        });
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

    private void configurarInventarioDetallado() {
        aplicarVisibilidadModo(chkInventarioDetallado.isSelected());
        chkInventarioDetallado.selectedProperty().addListener((obs, oldVal, newVal) -> {
            guardarVisibilidadModo(oldVal);
            aplicarVisibilidadModo(newVal);
            cargarInventarioDisponible(newVal);
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

    @FXML
    private void mostrarOrdenPopup(MouseEvent event) {
        boolean detallado = chkInventarioDetallado.isSelected();
        List<String> criterios = new ArrayList<>();
        criterios.add("id");
        if (!detallado) {
            criterios.add("cantidad");
        }
        criterios.add("producto");

        SelectorOrdenPopup.mostrar((Node) event.getSource(), event.getScreenX(), event.getScreenY(),
                criterios, criterioOrden, direccionOrden, seleccion -> {
                    criterioOrden = seleccion.getCriterio();
                    direccionOrden = seleccion.getDireccion();
                    aplicarOrdenamiento();
                });
    }

    private void aplicarOrdenamiento() {
        Comparator<ItemInventario> comparator = null;
        Function<ItemInventario, String> normalizar = valor -> valor == null ? "" : valor.toLowerCase();

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

        itemsInventario.clear();

        try (Connection conn = new Conexion().conectar();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                itemsInventario.add(new ItemInventario(
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
    }
}
