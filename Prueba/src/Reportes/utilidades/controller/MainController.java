package Reportes.utilidades.controller;

import Compartido.controller.encabezadoController;
import Compartido.controller.navbarController;
import Compartido.exportar.exportador;
import Compartido.helper.SelectorColumnasPopup;
import Compartido.helper.SelectorOrdenPopup;
import Reportes.utilidades.model.UtilidadItem;
import conexion.Conexion;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.SplitPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
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

    @FXML private ComboBox<String> comboFiltro;
    @FXML private ComboBox<String> comboValor;
    @FXML private HBox contenedorFiltros;

    @FXML private Region expansor;
    @FXML private Label lblVista;
    @FXML private Label lblDescargar;

    @FXML private VBox contenedorTabla;
    @FXML private TableView<UtilidadItem> contenidoTabla;
    @FXML private TableColumn<UtilidadItem, String> colClaveProducto;
    @FXML private TableColumn<UtilidadItem, String> colNombreProducto;
    @FXML private TableColumn<UtilidadItem, String> colCategoria;
    @FXML private TableColumn<UtilidadItem, String> colDescripcionProducto;
    @FXML private TableColumn<UtilidadItem, String> colPresentacion;
    @FXML private TableColumn<UtilidadItem, String> colFactor;
    @FXML private TableColumn<UtilidadItem, String> colCantidad;
    @FXML private TableColumn<UtilidadItem, String> colTotalCompra;
    @FXML private TableColumn<UtilidadItem, String> colProveedor;
    @FXML private TableColumn<UtilidadItem, String> colTotalVenta;
    @FXML private TableColumn<UtilidadItem, String> colCliente;
    @FXML private TableColumn<UtilidadItem, String> colPorcentajeUtilidad;
    @FXML private TableColumn<UtilidadItem, String> colUtilidadPesos;

    @FXML private encabezadoController paneNavbarController;

    private final ObservableList<UtilidadItem> utilidades = FXCollections.observableArrayList();
    private final ObservableList<UtilidadItem> utilidadesOriginal = FXCollections.observableArrayList();
    private final List<Filtro> filtrosActivos = new ArrayList<>();
    private boolean restaurandoFiltros = false;
    private String criterioOrden = "producto";
    private String direccionOrden = "asc";

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
            configurarColumnas();
            configurarFiltros();
            configurarBusquedaFactura();
            cargarUtilidades();
        });
    }

    private void configurarColumnas() {
        colClaveProducto.setCellValueFactory(new PropertyValueFactory<>("claveProducto"));
        colNombreProducto.setCellValueFactory(new PropertyValueFactory<>("nombreProducto"));
        colCategoria.setCellValueFactory(new PropertyValueFactory<>("categoria"));
        colDescripcionProducto.setCellValueFactory(new PropertyValueFactory<>("descripcionProducto"));
        colPresentacion.setCellValueFactory(new PropertyValueFactory<>("presentacion"));
        colFactor.setCellValueFactory(new PropertyValueFactory<>("factor"));
        colCantidad.setCellValueFactory(new PropertyValueFactory<>("cantidad"));
        colTotalCompra.setCellValueFactory(new PropertyValueFactory<>("totalCompra"));
        colProveedor.setCellValueFactory(new PropertyValueFactory<>("proveedor"));
        colTotalVenta.setCellValueFactory(new PropertyValueFactory<>("totalVenta"));
        colCliente.setCellValueFactory(new PropertyValueFactory<>("cliente"));
        colPorcentajeUtilidad.setCellValueFactory(new PropertyValueFactory<>("porcentajeUtilidad"));
        colUtilidadPesos.setCellValueFactory(new PropertyValueFactory<>("utilidadPesos"));

        TableColumn<UtilidadItem, ?>[] columnas = new TableColumn[] {
                colClaveProducto,
                colNombreProducto,
                colCategoria,
                colDescripcionProducto,
                colPresentacion,
                colFactor,
                colCantidad,
                colTotalCompra,
                colProveedor,
                colTotalVenta,
                colCliente,
                colPorcentajeUtilidad,
                colUtilidadPesos
        };

        for (TableColumn<UtilidadItem, ?> columna : columnas) {
            columna.setStyle("-fx-alignment: CENTER;");
        }

        contenidoTabla.setItems(utilidades);
    }

    private void cargarUtilidades() {
        utilidades.clear();
        List<UtilidadItem> registros = new ArrayList<>();

        try (Connection conn = new Conexion().conectar()) {
            if (conn == null) {
                return;
            }
            registros.addAll(obtenerUtilidades(conn));
        } catch (SQLException e) {
            e.printStackTrace();
        }

        utilidadesOriginal.setAll(registros);
        actualizarValoresFiltro(comboFiltro.getValue());
        aplicarFiltrosYBusqueda();
    }

    private List<UtilidadItem> obtenerUtilidades(Connection conn) throws SQLException {
        String query = "SELECT s.idSalida, s.noFactura AS facturaVenta, s.idDestinatario, "
                + "c.Nombre AS cliente, ds.idDetalleSalida, ds.claveProductoSalida AS claveProducto, "
                + "ds.precioUnitarioSalida, ds.precioTotalSalida, ds.cantidad AS cantidadSalida, "
                + "a.idDetalleEntrada, a.presentacion, a.factor, "
                + "de.idDetalleEntrada AS detalleEntradaId, de.claveEntrada, de.precioUnitario AS precioUnitarioEntrada, "
                + "de.precioTotal AS precioTotalEntrada, de.cantidad AS cantidadEntrada, "
                + "e.noFactura AS facturaCompra, e.idRemitente, p.Nombre AS proveedor, "
                + "pr.nombre AS nombreProducto, pr.categoria AS categoria, pr.material AS material, "
                + "pr.unidadMedida AS unidadMedida, pr.descripcion AS descripcionProducto, et.nombre AS etiquetaNombre "
                + "FROM articulo a "
                + "JOIN detalle_Salida ds ON ds.idDetalleSalida = a.idDetalleSalida "
                + "JOIN salidas s ON s.idSalida = ds.claveSalida "
                + "LEFT JOIN clientes c ON s.idDestinatario = c.id "
                + "LEFT JOIN detalle_Entrada de ON de.idDetalleEntrada = a.idDetalleEntrada "
                + "LEFT JOIN entradas e ON e.idEntrada = de.claveEntrada "
                + "LEFT JOIN proveedores p ON e.idRemitente = p.id "
                + "LEFT JOIN productos pr ON pr.id = ds.claveProductoSalida "
                + "LEFT JOIN etiquetas et ON et.id = pr.etiqueta "
                + "WHERE LOWER(s.tipoSalida) = 'venta' "
                + "AND LOWER(s.Estado) IN ('pendiente', 'finalizado', 'disponible', 'cancelado')";
        Map<String, UtilidadAcumulado> acumulados = new LinkedHashMap<>();

        try (PreparedStatement statement = conn.prepareStatement(query);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                int salidaId = rs.getInt("idSalida");
                Integer detalleEntradaId = obtenerEntero(rs.getObject("detalleEntradaId"));
                String claveProducto = valorTexto(rs.getObject("claveProducto"));
                String facturaVenta = valorTexto(rs.getObject("facturaVenta"));
                String facturaCompra = valorTexto(rs.getObject("facturaCompra"));
                String proveedor = valorTexto(rs.getObject("proveedor"));
                String cliente = valorTexto(rs.getObject("cliente"));
                String nombreProducto = valorTexto(rs.getObject("nombreProducto"));
                String categoria = valorTexto(rs.getObject("categoria"));
                String presentacion = valorTexto(rs.getObject("presentacion"));
                String factor = valorTexto(rs.getObject("factor"));
                String material = valorTexto(rs.getObject("material"));
                String unidad = valorTexto(rs.getObject("unidadMedida"));
                String etiqueta = valorTexto(rs.getObject("etiquetaNombre"));
                String descripcionProducto = valorTexto(rs.getObject("descripcionProducto"));
                String descripcion = construirDescripcionProducto(material, unidad, etiqueta, descripcionProducto);

                double costoUnitario = obtenerCostoUnitario(rs);
                double ventaUnitario = obtenerVentaUnitario(rs);

                String key = salidaId + "|" + Objects.toString(detalleEntradaId, "-") + "|" + claveProducto;
                UtilidadAcumulado acumulado = acumulados.computeIfAbsent(key, k -> new UtilidadAcumulado(
                        salidaId,
                        detalleEntradaId,
                        claveProducto,
                        nombreProducto,
                        categoria,
                        descripcion,
                        presentacion,
                        factor,
                        facturaCompra,
                        proveedor,
                        facturaVenta,
                        cliente
                ));
                acumulado.cantidad++;
                acumulado.totalCompra += costoUnitario;
                acumulado.totalVenta += ventaUnitario;
            }
        }

        List<UtilidadItem> resultado = new ArrayList<>();
        for (UtilidadAcumulado acumulado : acumulados.values()) {
            double utilidadPesos = acumulado.totalVenta - acumulado.totalCompra;
            double porcentaje = acumulado.totalCompra > 0 ? (utilidadPesos / acumulado.totalCompra) * 100 : 0;

            resultado.add(new UtilidadItem(
                    acumulado.claveProducto,
                    acumulado.nombreProducto,
                    acumulado.categoria,
                    acumulado.descripcionProducto,
                    acumulado.presentacion,
                    acumulado.factor,
                    String.valueOf(acumulado.cantidad),
                    formatoNumero(acumulado.totalCompra),
                    acumulado.proveedor,
                    formatoNumero(acumulado.totalVenta),
                    acumulado.cliente,
                    formatoPorcentaje(porcentaje),
                    formatoNumero(utilidadPesos),
                    acumulado.facturaCompra,
                    acumulado.facturaVenta
            ));
        }

        return resultado;
    }

    private double obtenerCostoUnitario(ResultSet rs) throws SQLException {
        Double precioUnitario = obtenerNumero(rs.getObject("precioUnitarioEntrada"));
        Double precioTotal = obtenerNumero(rs.getObject("precioTotalEntrada"));
        Double cantidad = obtenerNumero(rs.getObject("cantidadEntrada"));
        if (precioUnitario != null) {
            return precioUnitario;
        }
        if (precioTotal != null && cantidad != null && cantidad != 0) {
            return precioTotal / cantidad;
        }
        return 0;
    }

    private double obtenerVentaUnitario(ResultSet rs) throws SQLException {
        Double precioUnitario = obtenerNumero(rs.getObject("precioUnitarioSalida"));
        Double precioTotal = obtenerNumero(rs.getObject("precioTotalSalida"));
        Double cantidad = obtenerNumero(rs.getObject("cantidadSalida"));
        if (precioUnitario != null) {
            return precioUnitario;
        }
        if (precioTotal != null && cantidad != null && cantidad != 0) {
            return precioTotal / cantidad;
        }
        return 0;
    }

    private String construirDescripcionProducto(String material, String unidad, String etiqueta, String descripcion) {
        List<String> partes = new ArrayList<>();
        agregarParte(partes, material);
        agregarParte(partes, unidad);
        agregarParte(partes, etiqueta);
        agregarParte(partes, descripcion);
        return String.join(", ", partes);
    }

    private void agregarParte(List<String> partes, String valor) {
        if (valor != null && !valor.isBlank()) {
            partes.add(valor.trim());
        }
    }

    private void configurarFiltros() {
        comboFiltro.getItems().setAll(
                "Clave",
                "Producto",
                "Categoría",
                "Presentación",
                "Factor",
                "Proveedor",
                "Cliente",
                "Factura compra",
                "Factura venta",
                "Cantidad",
                "Total compra",
                "Total venta",
                "Porcentaje utilidad",
                "Utilidad"
        );
        comboFiltro.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (restaurandoFiltros) {
                return;
            }
            actualizarValoresFiltro(newVal);
        });
    }

    private void actualizarValoresFiltro(String campo) {
        if (comboValor == null) {
            return;
        }
        comboValor.getItems().clear();
        if (!restaurandoFiltros) {
            comboValor.setValue(null);
        }
        if (campo == null || campo.isBlank()) {
            return;
        }
        List<String> valores = new ArrayList<>();
        for (UtilidadItem item : utilidadesOriginal) {
            String valor = obtenerValorCampo(item, campo);
            if (valor != null && !valor.isBlank() && !valores.contains(valor)) {
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
        aplicarFiltrosYBusqueda();
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
            aplicarFiltrosYBusqueda();
        });
        chip.getChildren().addAll(texto, quitar);
        return chip;
    }

    private void aplicarFiltrosYBusqueda() {
        String filtroFactura = buscarFactura != null ? buscarFactura.getText() : "";
        String criterioFactura = filtroFactura == null ? "" : filtroFactura.trim().toLowerCase(Locale.ROOT);

        List<UtilidadItem> filtrados = new ArrayList<>();
        for (UtilidadItem item : utilidadesOriginal) {
            boolean coincide = true;
            for (Filtro filtro : filtrosActivos) {
                String valor = obtenerValorCampo(item, filtro.campo);
                if (valor == null || !valor.equals(filtro.valor)) {
                    coincide = false;
                    break;
                }
            }
            if (coincide && !criterioFactura.isBlank()) {
                String facturaVenta = valorTexto(item.getFacturaVenta()).toLowerCase(Locale.ROOT);
                String facturaCompra = valorTexto(item.getFacturaCompra()).toLowerCase(Locale.ROOT);
                coincide = facturaVenta.contains(criterioFactura) || facturaCompra.contains(criterioFactura);
            }
            if (coincide) {
                filtrados.add(item);
            }
        }
        utilidades.setAll(filtrados);
        aplicarOrdenamiento();
    }

    private String obtenerValorCampo(UtilidadItem item, String campo) {
        switch (campo) {
            case "Clave":
                return item.getClaveProducto();
            case "Producto":
                return item.getNombreProducto();
            case "Categoría":
                return item.getCategoria();
            case "Presentación":
                return item.getPresentacion();
            case "Factor":
                return item.getFactor();
            case "Proveedor":
                return item.getProveedor();
            case "Cliente":
                return item.getCliente();
            case "Factura compra":
                return item.getFacturaCompra();
            case "Factura venta":
                return item.getFacturaVenta();
            case "Cantidad":
                return item.getCantidad();
            case "Total compra":
                return item.getTotalCompra();
            case "Total venta":
                return item.getTotalVenta();
            case "Porcentaje utilidad":
                return item.getPorcentajeUtilidad();
            case "Utilidad":
                return item.getUtilidadPesos();
            default:
                return "";
        }
    }

    @FXML
    private void mostrarSelectorColumnas(MouseEvent event) {
        List<TableColumn<UtilidadItem, ?>> columnas = new ArrayList<>(contenidoTabla.getColumns());
        SelectorColumnasPopup.mostrar((Node) event.getSource(), event.getScreenX(), event.getScreenY(),
                columnas, seleccion -> {
                    for (Map.Entry<TableColumn<UtilidadItem, ?>, Boolean> entry : seleccion.entrySet()) {
                        entry.getKey().setVisible(entry.getValue());
                    }
                });
    }

    @FXML
    private void mostrarOrdenPopup(MouseEvent event) {
        List<String> criterios = List.of(
                "producto",
                "categoria",
                "clave",
                "cantidad",
                "totalCompra",
                "totalVenta",
                "proveedor",
                "cliente",
                "utilidad",
                "porcentaje",
                "facturaVenta",
                "facturaCompra"
        );
        SelectorOrdenPopup.mostrar((Node) event.getSource(), event.getScreenX(), event.getScreenY(),
                criterios, criterioOrden, direccionOrden, seleccion -> {
                    criterioOrden = seleccion.getCriterio();
                    direccionOrden = seleccion.getDireccion();
                    aplicarOrdenamiento();
                });
    }

    private void aplicarOrdenamiento() {
        Comparator<UtilidadItem> comparator;
        Function<String, String> normalizar = valor -> valor == null ? "" : valor.toLowerCase(Locale.ROOT);

        switch (criterioOrden) {
            case "clave":
                comparator = Comparator.comparing(item -> normalizar.apply(item.getClaveProducto()));
                break;
            case "categoria":
                comparator = Comparator.comparing(item -> normalizar.apply(item.getCategoria()));
                break;
            case "cantidad":
                comparator = Comparator.comparing(item -> parseNumero(item.getCantidad()));
                break;
            case "totalCompra":
                comparator = Comparator.comparing(item -> parseNumero(item.getTotalCompra()));
                break;
            case "totalVenta":
                comparator = Comparator.comparing(item -> parseNumero(item.getTotalVenta()));
                break;
            case "proveedor":
                comparator = Comparator.comparing(item -> normalizar.apply(item.getProveedor()));
                break;
            case "cliente":
                comparator = Comparator.comparing(item -> normalizar.apply(item.getCliente()));
                break;
            case "utilidad":
                comparator = Comparator.comparing(item -> parseNumero(item.getUtilidadPesos()));
                break;
            case "porcentaje":
                comparator = Comparator.comparing(item -> parseNumero(item.getPorcentajeUtilidad()));
                break;
            case "facturaVenta":
                comparator = Comparator.comparing(item -> normalizar.apply(item.getFacturaVenta()));
                break;
            case "facturaCompra":
                comparator = Comparator.comparing(item -> normalizar.apply(item.getFacturaCompra()));
                break;
            case "producto":
            default:
                comparator = Comparator.comparing(item -> normalizar.apply(item.getNombreProducto()));
                break;
        }

        if ("desc".equalsIgnoreCase(direccionOrden)) {
            comparator = comparator.reversed();
        }

        FXCollections.sort(utilidades, comparator);
    }

    private void configurarBusquedaFactura() {
        if (buscarFactura == null) {
            return;
        }
        buscarFactura.textProperty().addListener((obs, oldVal, newVal) -> aplicarFiltrosYBusqueda());
    }

    @FXML
    private void exportarExcel() {
        if (contenidoTabla.getItems().isEmpty()) {
            mostrarAdvertencia("Advertencia", "No hay datos para exportar.");
            return;
        }
        exportador.exportarTabla(contenidoTabla, "Utilidades", "excel",
                obtenerFiltrosAplicados());
    }

    @FXML
    private void descargarPdf() {
        if (contenidoTabla.getItems().isEmpty()) {
            mostrarAdvertencia("Advertencia", "No hay datos para exportar.");
            return;
        }
        exportador.exportarTabla(contenidoTabla, "Utilidades", "pdf",
                obtenerFiltrosAplicados());
    }

    @FXML
    private void vistaPreviaPdf() {
        if (contenidoTabla.getItems().isEmpty()) {
            mostrarAdvertencia("Advertencia", "No hay datos para exportar.");
            return;
        }
        exportador.previsualizarPDF(contenidoTabla, "Utilidades",
                obtenerFiltrosAplicados());
    }

    private List<String> obtenerFiltrosAplicados() {
        List<String> filtrosAplicados = new ArrayList<>();
        for (Filtro filtro : filtrosActivos) {
            filtrosAplicados.add(filtro.campo + ": " + filtro.valor);
        }
        String filtroFactura = buscarFactura != null ? buscarFactura.getText() : "";
        if (filtroFactura != null && !filtroFactura.isBlank()) {
            filtrosAplicados.add("Factura contiene: " + filtroFactura.trim());
        }
        return filtrosAplicados;
    }

    private void mostrarAdvertencia(String titulo, String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.WARNING);
        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }

    private String valorTexto(Object valor) {
        return valor == null ? "" : valor.toString();
    }

    private Double obtenerNumero(Object valor) {
        if (valor == null) {
            return null;
        }
        if (valor instanceof Number) {
            return ((Number) valor).doubleValue();
        }
        try {
            return Double.parseDouble(valor.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer obtenerEntero(Object valor) {
        if (valor == null) {
            return null;
        }
        if (valor instanceof Number) {
            return ((Number) valor).intValue();
        }
        try {
            return Integer.parseInt(valor.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String formatoNumero(double valor) {
        return String.format(Locale.US, "%.2f", valor);
    }

    private String formatoPorcentaje(double valor) {
        return String.format(Locale.US, "%.2f%%", valor);
    }

    private double parseNumero(String valor) {
        if (valor == null || valor.isBlank()) {
            return 0;
        }
        String limpio = valor.replace("%", "").replace(",", "").trim();
        try {
            return Double.parseDouble(limpio);
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

    private static class UtilidadAcumulado {
        private final int salidaId;
        private final Integer entradaId;
        private final String claveProducto;
        private final String nombreProducto;
        private final String categoria;
        private final String descripcionProducto;
        private final String presentacion;
        private final String factor;
        private final String facturaCompra;
        private final String proveedor;
        private final String facturaVenta;
        private final String cliente;
        private int cantidad;
        private double totalCompra;
        private double totalVenta;

        private UtilidadAcumulado(int salidaId,
                                  Integer entradaId,
                                  String claveProducto,
                                  String nombreProducto,
                                  String categoria,
                                  String descripcionProducto,
                                  String presentacion,
                                  String factor,
                                  String facturaCompra,
                                  String proveedor,
                                  String facturaVenta,
                                  String cliente) {
            this.salidaId = salidaId;
            this.entradaId = entradaId;
            this.claveProducto = claveProducto;
            this.nombreProducto = nombreProducto;
            this.categoria = categoria;
            this.descripcionProducto = descripcionProducto;
            this.presentacion = presentacion;
            this.factor = factor;
            this.facturaCompra = facturaCompra;
            this.proveedor = proveedor;
            this.facturaVenta = facturaVenta;
            this.cliente = cliente;
        }
    }
}
