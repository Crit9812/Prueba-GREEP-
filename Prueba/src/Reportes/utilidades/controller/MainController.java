package Reportes.utilidades.controller;

import Compartido.controller.encabezadoController;
import Compartido.controller.navbarController;
import Compartido.exportar.exportador;
import Compartido.helper.SelectorColumnasPopup;
import Compartido.helper.SelectorOrdenPopup;
import Reportes.utilidades.model.ItemUtilidad;
import conexion.Conexion;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.application.Platform;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.function.Function;

public class MainController {

    @FXML private StackPane root;
    @FXML private BorderPane paneNavbar;
    @FXML private VBox navbar;
    @FXML private Pane overlayPane;
    @FXML private VBox contenedor;

    @FXML private Label lblQuitar;
    @FXML private Label lblOrdenar;
    @FXML private Label lblExportar;
    @FXML private Region expansorBusqueda;
    @FXML private TextField buscarFactura;

    @FXML private Region expansor;
    @FXML private Label lblVista;
    @FXML private Label lblDescargar;
    @FXML private ComboBox<String> comboFiltro;
    @FXML private ComboBox<String> comboValor;
    @FXML private HBox contenedorFiltros;

    @FXML private VBox contenedorTabla;
    @FXML private TableView<ItemUtilidad> contenidoTabla;
    @FXML private TableColumn<ItemUtilidad, String> colClaveProducto;
    @FXML private TableColumn<ItemUtilidad, String> colNombreProducto;
    @FXML private TableColumn<ItemUtilidad, String> colCategoria;
    @FXML private TableColumn<ItemUtilidad, String> colDescripcion;
    @FXML private TableColumn<ItemUtilidad, String> colPresentacion;
    @FXML private TableColumn<ItemUtilidad, String> colFactor;
    @FXML private TableColumn<ItemUtilidad, String> colCantidad;
    @FXML private TableColumn<ItemUtilidad, String> colTotalCompra;
    @FXML private TableColumn<ItemUtilidad, String> colProveedor;
    @FXML private TableColumn<ItemUtilidad, String> colTotalVenta;
    @FXML private TableColumn<ItemUtilidad, String> colCliente;
    @FXML private TableColumn<ItemUtilidad, String> colPorcentajeUtilidad;
    @FXML private TableColumn<ItemUtilidad, String> colUtilidad;

    @FXML private encabezadoController paneNavbarController;

    private final ObservableList<ItemUtilidad> itemsUtilidad = FXCollections.observableArrayList();
    private final ObservableList<ItemUtilidad> itemsUtilidadOriginal = FXCollections.observableArrayList();
    private final Map<TableColumn<ItemUtilidad, ?>, Boolean> visibilidadColumnas = new HashMap<>();
    private final DecimalFormat formatoMoneda = new DecimalFormat("$ #,##0.00", new DecimalFormatSymbols(Locale.US));
    private final DecimalFormat formatoPorcentaje = new DecimalFormat("0.00'%'");
    private final List<Filtro> filtrosActivos = new ArrayList<>();
    private String criterioOrden = "id";
    private String direccionOrden = "asc";
    private boolean restaurandoFiltros = false;

    @FXML
    public void initialize() {
        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/Compartido/view/navbar.fxml"));
                VBox navbarLoaded = loader.load();

                navbarController navbarCtrl = loader.getController();
                navbarCtrl.setOverlayPane(overlayPane);
                navbar.getChildren().setAll(navbarLoaded);

            } catch (IOException e) {
                e.printStackTrace();
            }
            SplitPane.setResizableWithParent(navbar, false);
            SplitPane.setResizableWithParent(contenedor, true);

            paneNavbar.prefHeightProperty().bind(root.heightProperty().multiply(0.1));
            paneNavbar.prefWidthProperty().bind(root.widthProperty().multiply(0.9));

            navbar.prefWidthProperty().bind(root.widthProperty().multiply(0.15));
            navbar.prefHeightProperty().bind(root.heightProperty().multiply(0.9));

            contenedor.prefHeightProperty().bind(root.heightProperty().multiply(0.75));

            lblQuitar.setMinWidth(Region.USE_PREF_SIZE);
            lblOrdenar.setMinWidth(Region.USE_PREF_SIZE);
            lblExportar.setMinWidth(Region.USE_PREF_SIZE);

            HBox.setHgrow(expansorBusqueda, Priority.ALWAYS);
            expansorBusqueda.setMinWidth(10);

            buscarFactura.prefWidthProperty().bind(root.widthProperty().multiply(0.18));
            buscarFactura.prefHeightProperty().bind(navbar.heightProperty().multiply(0.04));

            HBox.setHgrow(expansor, Priority.ALWAYS);
            expansor.setMinWidth(10);

            lblVista.setMinWidth(Region.USE_PREF_SIZE);
            lblDescargar.setMinWidth(Region.USE_PREF_SIZE);

            contenedorTabla.prefHeightProperty().bind(contenedor.heightProperty().multiply(0.78));
            contenidoTabla.prefHeightProperty().bind(contenedorTabla.heightProperty().multiply(0.9));

            paneNavbarController.setTitulo("Utilidades", "#ffffff");

            configurarTabla();
            configurarFiltros();
            cargarUtilidades();
        });
    }

    private void configurarTabla() {
        colClaveProducto.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("claveProducto"));
        colNombreProducto.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("nombreProducto"));
        colCategoria.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("categoria"));
        colDescripcion.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("descripcionProducto"));
        colPresentacion.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("presentacion"));
        colFactor.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("factor"));
        colCantidad.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("cantidad"));
        colTotalCompra.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("totalCompra"));
        colProveedor.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("proveedor"));
        colTotalVenta.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("totalVenta"));
        colCliente.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("cliente"));
        colPorcentajeUtilidad.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("porcentajeUtilidad"));
        colUtilidad.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("utilidad"));

        TableColumn<ItemUtilidad, ?>[] columnas = new TableColumn[] {
                colClaveProducto,
                colNombreProducto,
                colCategoria,
                colDescripcion,
                colPresentacion,
                colFactor,
                colCantidad,
                colTotalCompra,
                colProveedor,
                colTotalVenta,
                colCliente,
                colPorcentajeUtilidad,
                colUtilidad
        };
        for (TableColumn<ItemUtilidad, ?> col : columnas) {
            col.setStyle("-fx-alignment: CENTER;");
        }

        contenidoTabla.setItems(itemsUtilidad);
    }

    private void cargarUtilidades() {
        itemsUtilidad.clear();
        itemsUtilidadOriginal.clear();
        String query = "SELECT s.noFactura AS facturaVenta, " +
                "ds.claveProductoSalida AS claveProducto, " +
                "p.nombre AS nombreProducto, " +
                "p.categoria AS categoria, " +
                "p.material AS material, " +
                "p.unidadMedida AS unidadMedida, " +
                "et.nombre AS etiqueta, " +
                "p.descripcion AS descripcionProducto, " +
                "MAX(a.presentacion) AS presentacion, " +
                "MAX(a.factor) AS factor, " +
                "pr.Nombre AS proveedor, " +
                "c.Nombre AS cliente, " +
                "COUNT(a.idArticulo) AS cantidad, " +
                "SUM(de.precioUnitario) AS totalCompra, " +
                "SUM(ds.precioUnitarioSalida) AS totalVenta " +
                "FROM salidas s " +
                "INNER JOIN detalle_Salida ds ON ds.claveSalida = s.idSalida " +
                "INNER JOIN articulo a ON a.idDetalleSalida = ds.idDetalleSalida " +
                "INNER JOIN detalle_Entrada de ON de.idDetalleEntrada = a.idDetalleEntrada " +
                "INNER JOIN entradas en ON en.idEntrada = de.claveEntrada " +
                "LEFT JOIN proveedores pr ON pr.id = en.idRemitente " +
                "LEFT JOIN clientes c ON c.id = s.idDestinatario " +
                "LEFT JOIN productos p ON p.id = ds.claveProductoSalida " +
                "LEFT JOIN etiquetas et ON et.id = p.etiqueta " +
                "WHERE s.tipoSalida = 'venta' AND s.Estado <> 'cancelado' " +
                "GROUP BY s.noFactura, ds.claveProductoSalida, pr.Nombre, c.Nombre, " +
                "p.nombre, p.categoria, p.material, p.unidadMedida, et.nombre, p.descripcion " +
                "ORDER BY s.noFactura, ds.claveProductoSalida";

        try (Connection conn = new Conexion().conectar();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                int cantidad = rs.getInt("cantidad");
                double totalCompra = rs.getDouble("totalCompra");
                double totalVenta = rs.getDouble("totalVenta");
                double utilidad = totalVenta - totalCompra;
                double porcentaje = totalCompra != 0 ? (utilidad / totalCompra) * 100 : 0;

                String descripcion = construirDescripcion(
                        rs.getString("material"),
                        rs.getString("unidadMedida"),
                        rs.getString("etiqueta"),
                        rs.getString("descripcionProducto")
                );

                itemsUtilidadOriginal.add(new ItemUtilidad(
                        rs.getString("claveProducto"),
                        rs.getString("nombreProducto"),
                        rs.getString("categoria"),
                        descripcion,
                        rs.getString("presentacion"),
                        String.valueOf(rs.getInt("factor")),
                        String.valueOf(cantidad),
                        formatoMoneda.format(totalCompra),
                        rs.getString("proveedor"),
                        formatoMoneda.format(totalVenta),
                        rs.getString("cliente"),
                        formatoPorcentaje.format(porcentaje),
                        formatoMoneda.format(utilidad)
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        aplicarFiltros();
    }

    private String construirDescripcion(String material, String unidad, String etiqueta, String descripcion) {
        StringBuilder builder = new StringBuilder();
        if (material != null && !material.isBlank()) {
            builder.append("Material: ").append(material);
        }
        if (unidad != null && !unidad.isBlank()) {
            if (builder.length() > 0) {
                builder.append(" | ");
            }
            builder.append("Unidad: ").append(unidad);
        }
        if (etiqueta != null && !etiqueta.isBlank()) {
            if (builder.length() > 0) {
                builder.append(" | ");
            }
            builder.append("Etiqueta: ").append(etiqueta);
        }
        if (descripcion != null && !descripcion.isBlank()) {
            if (builder.length() > 0) {
                builder.append(" | ");
            }
            builder.append("Descripción: ").append(descripcion);
        }
        return builder.toString();
    }

    @FXML
    private void mostrarSelectorColumnas(MouseEvent event) {
        List<TableColumn<ItemUtilidad, ?>> columnas = obtenerColumnasVisibles();
        SelectorColumnasPopup.mostrar((Node) event.getSource(), event.getScreenX(), event.getScreenY(),
                columnas, seleccion -> {
                    for (Map.Entry<TableColumn<ItemUtilidad, ?>, Boolean> entry : seleccion.entrySet()) {
                        entry.getKey().setVisible(entry.getValue());
                        visibilidadColumnas.put(entry.getKey(), entry.getValue());
                    }
                });
    }

    private List<TableColumn<ItemUtilidad, ?>> obtenerColumnasVisibles() {
        List<TableColumn<ItemUtilidad, ?>> columnas = new ArrayList<>();
        columnas.add(colClaveProducto);
        columnas.add(colNombreProducto);
        columnas.add(colCategoria);
        columnas.add(colDescripcion);
        columnas.add(colPresentacion);
        columnas.add(colFactor);
        columnas.add(colCantidad);
        columnas.add(colTotalCompra);
        columnas.add(colProveedor);
        columnas.add(colTotalVenta);
        columnas.add(colCliente);
        columnas.add(colPorcentajeUtilidad);
        columnas.add(colUtilidad);
        return columnas;
    }

    private void configurarFiltros() {
        actualizarOpcionesFiltro();
        comboFiltro.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (restaurandoFiltros) return;
            actualizarValoresFiltro(newVal);
        });
    }

    private void actualizarOpcionesFiltro() {
        String campoSeleccionado = comboFiltro.getValue();
        String valorSeleccionado = comboValor.getValue();

        restaurandoFiltros = true;

        List<String> opciones = new ArrayList<>();
        opciones.add("ID");
        opciones.add("Producto");
        opciones.add("Categoría");
        opciones.add("Presentación");
        opciones.add("Proveedor");
        opciones.add("Cliente");
        opciones.add("Cantidad");
        opciones.add("Total compra");
        opciones.add("Total venta");
        opciones.add("Utilidad");

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
        for (ItemUtilidad item : itemsUtilidadOriginal) {
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
        HBox chip = new HBox(6);
        chip.setAlignment(javafx.geometry.Pos.CENTER);
        chip.setStyle("-fx-background-color: #000000; -fx-background-radius: 12; -fx-padding: 4 8;");
        Label texto = new Label(filtro.campo + ": " + filtro.valor);
        texto.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 10pt;");
        javafx.scene.control.Button quitar = new javafx.scene.control.Button("x");
        quitar.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-cursor: hand; -fx-font-size: 16pt;");
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
        List<ItemUtilidad> filtrados = new ArrayList<>();
        for (ItemUtilidad item : itemsUtilidadOriginal) {
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
        itemsUtilidad.setAll(filtrados);
        aplicarOrdenamiento();
    }

    private String obtenerValorCampo(ItemUtilidad item, String campo) {
        switch (campo) {
            case "ID":
                return item.getClaveProducto();
            case "Producto":
                return item.getNombreProducto();
            case "Categoría":
                return item.getCategoria();
            case "Presentación":
                return item.getPresentacion();
            case "Proveedor":
                return item.getProveedor();
            case "Cliente":
                return item.getCliente();
            case "Cantidad":
                return item.getCantidad();
            case "Total compra":
                return item.getTotalCompra();
            case "Total venta":
                return item.getTotalVenta();
            case "Utilidad":
                return item.getUtilidad();
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

        exportador.exportarTabla(contenidoTabla, "Utilidades", "excel", filtrosAplicados);
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

        exportador.exportarTabla(contenidoTabla, "Utilidades", "pdf", filtrosAplicados);
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

        exportador.previsualizarPDF(contenidoTabla, "Utilidades", filtrosAplicados);
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
        List<String> criterios = new ArrayList<>();
        criterios.add("id");
        criterios.add("producto");
        criterios.add("proveedor");
        criterios.add("cliente");
        criterios.add("cantidad");
        criterios.add("totalVenta");
        criterios.add("utilidad");

        SelectorOrdenPopup.mostrar((Node) event.getSource(), event.getScreenX(), event.getScreenY(),
                criterios, criterioOrden, direccionOrden, seleccion -> {
                    criterioOrden = seleccion.getCriterio();
                    direccionOrden = seleccion.getDireccion();
                    aplicarOrdenamiento();
                });
    }

    private void aplicarOrdenamiento() {
        Comparator<ItemUtilidad> comparator;
        Function<String, String> normalizar = valor -> valor == null ? "" : valor.toLowerCase();

        switch (criterioOrden) {
            case "producto":
                comparator = Comparator.comparing(item -> normalizar.apply(item.getNombreProducto()));
                break;
            case "proveedor":
                comparator = Comparator.comparing(item -> normalizar.apply(item.getProveedor()));
                break;
            case "cliente":
                comparator = Comparator.comparing(item -> normalizar.apply(item.getCliente()));
                break;
            case "cantidad":
                comparator = Comparator.comparingInt(item -> parseNumero(item.getCantidad()));
                break;
            case "totalVenta":
                comparator = Comparator.comparingDouble(item -> parseMoneda(item.getTotalVenta()));
                break;
            case "utilidad":
                comparator = Comparator.comparingDouble(item -> parseMoneda(item.getUtilidad()));
                break;
            case "id":
            default:
                comparator = Comparator.comparing(item -> normalizar.apply(item.getClaveProducto()));
                break;
        }

        if ("desc".equalsIgnoreCase(direccionOrden)) {
            comparator = comparator.reversed();
        }

        FXCollections.sort(itemsUtilidad, comparator);
    }

    private int parseNumero(String valor) {
        if (valor == null || valor.isBlank()) {
            return 0;
        }
        try {
            return Integer.parseInt(valor.replaceAll("[^0-9-]", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private double parseMoneda(String valor) {
        if (valor == null || valor.isBlank()) {
            return 0;
        }
        try {
            return Double.parseDouble(valor.replaceAll("[^0-9.-]", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
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
