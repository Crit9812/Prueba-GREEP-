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
    private String criterioOrden = "id";
    private String direccionOrden = "asc";
    private final List<Filtro> filtrosActivos = new ArrayList<>();

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

            configurarColumnasTabla();
            configurarInventarioDetallado();
            configurarFiltros();
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
        comboFiltro.valueProperty().addListener((obs, oldVal, newVal) -> actualizarValoresFiltro(newVal));
    }

    private void actualizarOpcionesFiltro(boolean detallado) {
        List<String> opciones = new ArrayList<>();
        opciones.add("ID");
        if (!detallado) {
            opciones.add("Cantidad");
        }
        opciones.add("Producto");
        opciones.add("Marca");
        opciones.add("Categoría");
        opciones.add("Material");
        opciones.add("Unidad");
        opciones.add("Presentación");
        opciones.add("Factor");
        if (detallado) {
            opciones.add("Lote");
            opciones.add("Caducidad");
            opciones.add("Ubicación");
        }
        opciones.add("Descripción");
        opciones.add("Inventario mínimo");

        comboFiltro.getItems().setAll(opciones);
        limpiarFiltrosNoDisponibles(new LinkedHashSet<>(opciones));
    }

    private void actualizarValoresFiltro(String campo) {
        comboValor.getItems().clear();
        comboValor.setValue(null);
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
            return;
        }
        if (filtrosActivos.size() >= 3) {
            return;
        }
        for (Filtro filtro : filtrosActivos) {
            if (filtro.campo.equals(campo)) {
                return;
            }
        }
        Filtro filtro = new Filtro(campo, valor);
        filtrosActivos.add(filtro);
        contenedorFiltros.getChildren().add(crearChipFiltro(filtro));
        aplicarFiltros();
    }

    private Node crearChipFiltro(Filtro filtro) {
        HBox chip = new HBox(6);
        chip.setStyle("-fx-background-color: #000000; -fx-background-radius: 12; -fx-padding: 4 8;");
        Label texto = new Label(filtro.campo + ": " + filtro.valor);
        texto.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
        Button quitar = new Button("x");
        quitar.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-cursor: hand;");
        quitar.setOnAction(event -> {
            filtrosActivos.remove(filtro);
            contenedorFiltros.getChildren().remove(chip);
            aplicarFiltros();
        });
        chip.getChildren().addAll(texto, quitar);
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
            case "Cantidad":
                return item.getCantidad();
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
            case "Factor":
                return item.getFactor();
            case "Lote":
                return item.getLote();
            case "Caducidad":
                return item.getCaducidad();
            case "Ubicación":
                return item.getUbicacion();
            case "Descripción":
                return item.getDescripcion();
            case "Inventario mínimo":
                return item.getInventarioMinimo();
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
        exportador.exportarTabla(contenidoTabla, "Inventario", "excel");
    }

    @FXML
    private void descargarPdf() {
        if (contenidoTabla.getItems().isEmpty()) {
            mostrarAdvertencia("Advertencia", "No hay datos para exportar.");
            return;
        }
        exportador.exportarTabla(contenidoTabla, "Inventario", "pdf");
    }

    @FXML
    private void vistaPreviaPdf() {
        if (contenidoTabla.getItems().isEmpty()) {
            mostrarAdvertencia("Advertencia", "No hay datos para exportar.");
            return;
        }
        exportador.previsualizarPDF(contenidoTabla, "Inventario");
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
            case "id":
            default:
                comparator = Comparator.comparing(item -> normalizar.apply(item.getClaveProducto()));
                break;
        }

        if ("cantidad".equalsIgnoreCase(criterioOrden)) {
            if ("asc".equalsIgnoreCase(direccionOrden)) {
                comparator = comparator.reversed();
            }
        } else if ("desc".equalsIgnoreCase(direccionOrden)) {
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

        itemsInventarioOriginal.clear();

        try (Connection conn = new Conexion().conectar();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                itemsInventarioOriginal.add(new ItemInventario(
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
        actualizarValoresFiltro(comboFiltro.getValue());
        aplicarFiltros();
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
