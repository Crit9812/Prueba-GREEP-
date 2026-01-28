package Reportes.historialArticulo.controller;

import Compartido.controller.encabezadoController;
import Compartido.controller.navbarController;
import Compartido.exportar.exportador;
import Compartido.helper.SelectorColumnasPopup;
import Compartido.helper.SelectorOrdenPopup;
import conexion.Conexion;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.scene.Node;
import javafx.scene.layout.*;
import javafx.application.Platform;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javafx.util.StringConverter;

public class MainController {

    private static final String PERIODO_EXPORT = "Periodo: 02/02/25-02/03/25";

    @FXML private StackPane root;
    @FXML private BorderPane paneNavbar;
    @FXML private VBox navbar;
    @FXML private Pane overlayPane;
    @FXML private VBox contenedor;

    @FXML private Label lblQuitar;
    @FXML private Label lblOrdenar;
    @FXML private Label lblExportar;
    @FXML private Region expansorBusqueda;
    @FXML private ComboBox<ProductoOpcion> buscarProducto;
    @FXML private DatePicker fechaInicio;
    @FXML private DatePicker fechaFin;

    @FXML private ComboBox<String> comboFiltro;
    @FXML private ComboBox<String> comboValor;
    @FXML private HBox contenedorFiltros;

    @FXML private Region expansor;
    @FXML private Label lblVista;
    @FXML private Label lblDescargar;

    @FXML private VBox contenedorTabla;
    @FXML private TableView<HistorialArticuloItem> contenidoTabla;
    @FXML private TableColumn<HistorialArticuloItem, String> colFecha;
    @FXML private TableColumn<HistorialArticuloItem, String> colHora;
    @FXML private TableColumn<HistorialArticuloItem, String> colTipoMovimiento;
    @FXML private TableColumn<HistorialArticuloItem, String> colAntes;
    @FXML private TableColumn<HistorialArticuloItem, String> colDespues;
    @FXML private TableColumn<HistorialArticuloItem, String> colEntradas;
    @FXML private TableColumn<HistorialArticuloItem, String> colSalidas;
    @FXML private TableColumn<HistorialArticuloItem, String> colProveedor;
    @FXML private TableColumn<HistorialArticuloItem, String> colFacturaEntrada;
    @FXML private TableColumn<HistorialArticuloItem, String> colCliente;
    @FXML private TableColumn<HistorialArticuloItem, String> colFacturaSalida;
    @FXML private TableColumn<HistorialArticuloItem, String> colUsuario;

    @FXML private Label lblClave;
    @FXML private Label lblDescripcion;
    @FXML private Label lblPresentacion;
    @FXML private Label lblFactor;
    @FXML private Label lblExistencias;

    @FXML private encabezadoController paneNavbarController;

    private final ObservableList<ProductoOpcion> productosCache = FXCollections.observableArrayList();
    private final ObservableList<ProductoOpcion> productosFiltrados = FXCollections.observableArrayList();
    private final ObservableList<HistorialArticuloItem> historialItems = FXCollections.observableArrayList();
    private final ObservableList<HistorialArticuloItem> historialItemsOriginal = FXCollections.observableArrayList();
    private final List<Filtro> filtrosActivos = new ArrayList<>();
    private boolean restaurandoFiltros = false;
    private boolean actualizandoBusqueda = false;
    private String criterioOrden = "fecha";
    private String direccionOrden = "desc";
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm:ss");

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

            buscarProducto.prefWidthProperty().bind(root.widthProperty().multiply(0.18));
            buscarProducto.prefHeightProperty().bind(navbar.heightProperty().multiply(0.04));

            if (fechaInicio != null && fechaFin != null) {
                fechaInicio.prefWidthProperty().bind(root.widthProperty().multiply(0.12));
                fechaInicio.prefHeightProperty().bind(navbar.heightProperty().multiply(0.04));
                fechaFin.prefWidthProperty().bind(root.widthProperty().multiply(0.12));
                fechaFin.prefHeightProperty().bind(navbar.heightProperty().multiply(0.04));
            }

            HBox.setHgrow(expansor, Priority.ALWAYS);
            expansor.setMinWidth(10);

            lblVista.setMinWidth(Region.USE_PREF_SIZE);
            lblDescargar.setMinWidth(Region.USE_PREF_SIZE);

            contenedorTabla.prefHeightProperty().bind(contenedor.heightProperty().multiply(0.71));
            contenidoTabla.prefHeightProperty().bind(contenedorTabla.heightProperty().multiply(0.9));

            paneNavbarController.setTitulo("Historial por artículo", "#ffffff");
            configurarColumnas();
            configurarBuscadorProducto();
            configurarFiltros();
            configurarFiltroFechas();
        });
    }

    private void configurarColumnas() {
        colFecha.setCellValueFactory(new PropertyValueFactory<>("fecha"));
        colHora.setCellValueFactory(new PropertyValueFactory<>("hora"));
        colTipoMovimiento.setCellValueFactory(new PropertyValueFactory<>("tipoMovimiento"));
        colAntes.setCellValueFactory(new PropertyValueFactory<>("antes"));
        colDespues.setCellValueFactory(new PropertyValueFactory<>("despues"));
        colEntradas.setCellValueFactory(new PropertyValueFactory<>("entradas"));
        colSalidas.setCellValueFactory(new PropertyValueFactory<>("salidas"));
        colProveedor.setCellValueFactory(new PropertyValueFactory<>("proveedor"));
        colFacturaEntrada.setCellValueFactory(new PropertyValueFactory<>("facturaEntrada"));
        colCliente.setCellValueFactory(new PropertyValueFactory<>("cliente"));
        colFacturaSalida.setCellValueFactory(new PropertyValueFactory<>("facturaSalida"));
        colUsuario.setCellValueFactory(new PropertyValueFactory<>("usuario"));

        contenidoTabla.setItems(historialItems);
    }

    private void configurarBuscadorProducto() {
        buscarProducto.setItems(productosFiltrados);
        buscarProducto.setEditable(true);
        buscarProducto.setConverter(new StringConverter<>() {
            @Override
            public String toString(ProductoOpcion producto) {
                return producto == null ? "" : producto.getTextoVisible();
            }

            @Override
            public ProductoOpcion fromString(String texto) {
                if (texto == null || texto.isBlank()) {
                    return null;
                }
                String normalizado = texto.trim();
                for (ProductoOpcion producto : productosCache) {
                    if (producto.getTextoVisible().equalsIgnoreCase(normalizado)) {
                        return producto;
                    }
                }
                return null;
            }
        });

        Task<List<ProductoOpcion>> task = new Task<>() {
            @Override
            protected List<ProductoOpcion> call() {
                return cargarProductosActivos();
            }

            @Override
            protected void succeeded() {
                List<ProductoOpcion> resultado = getValue();
                productosCache.setAll(resultado != null ? resultado : List.of());
                productosFiltrados.setAll(productosCache);
            }

            @Override
            protected void failed() {
                productosCache.clear();
                productosFiltrados.clear();
            }
        };

        Thread hilo = new Thread(task);
        hilo.setDaemon(true);
        hilo.start();

        buscarProducto.getEditor().textProperty().addListener((obs, oldText, newText) -> {
            if (actualizandoBusqueda) {
                return;
            }
            ProductoOpcion seleccionado = buscarProducto.getValue();
            if (seleccionado != null && seleccionado.getTextoVisible().equals(newText)) {
                return;
            }
            actualizandoBusqueda = true;
            try {
                filtrarProductos(newText);
            } finally {
                actualizandoBusqueda = false;
            }
        });

        buscarProducto.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                actualizandoBusqueda = true;
                buscarProducto.getEditor().setText(newVal.getTextoVisible());
                actualizandoBusqueda = false;
                cargarHistorialArticulo(newVal.getId());
            } else {
                limpiarDetalleProducto();
            }
        });
    }

    private void configurarFiltros() {
        if (comboFiltro == null || comboValor == null) {
            return;
        }
        comboFiltro.getItems().setAll(
                "Fecha",
                "Hora",
                "Movimiento",
                "Proveedor",
                "Factura entrada",
                "Cliente",
                "Factura salida",
                "Usuario"
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
        for (HistorialArticuloItem item : historialItemsOriginal) {
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
        aplicarFiltros();
    }

    private Node crearChipFiltro(Filtro filtro) {
        HBox chip = new HBox(6);
        chip.setAlignment(javafx.geometry.Pos.CENTER);
        chip.setStyle("-fx-background-color: #000000; -fx-background-radius: 12; -fx-padding: 4 8;");
        Label texto = new Label(filtro.campo + ": " + filtro.valor);
        texto.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 10pt;");
        Button quitar = new Button("x");
        quitar.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-cursor: hand; -fx-font-size: 16pt;");
        quitar.setOnAction(event -> {
            filtrosActivos.remove(filtro);
            contenedorFiltros.getChildren().remove(chip);
            aplicarFiltros();
        });
        chip.getChildren().addAll(texto, quitar);
        return chip;
    }

    private void aplicarFiltros() {
        LocalDate fechaInicioSeleccionada = fechaInicio != null ? fechaInicio.getValue() : null;
        LocalDate fechaFinSeleccionada = fechaFin != null ? fechaFin.getValue() : null;
        List<HistorialArticuloItem> filtrados = new ArrayList<>();
        for (HistorialArticuloItem item : historialItemsOriginal) {
            boolean coincide = true;
            for (Filtro filtro : filtrosActivos) {
                String valor = obtenerValorCampo(item, filtro.campo);
                if (valor == null || !valor.equals(filtro.valor)) {
                    coincide = false;
                    break;
                }
            }
            if (coincide && (fechaInicioSeleccionada != null || fechaFinSeleccionada != null)) {
                LocalDate fechaItem = parseFechaItem(item.getFecha());
                if (fechaItem == null) {
                    coincide = false;
                } else {
                    if (fechaInicioSeleccionada != null && fechaItem.isBefore(fechaInicioSeleccionada)) {
                        coincide = false;
                    }
                    if (coincide && fechaFinSeleccionada != null && fechaItem.isAfter(fechaFinSeleccionada)) {
                        coincide = false;
                    }
                }
            }
            if (coincide) {
                filtrados.add(item);
            }
        }
        historialItems.setAll(filtrados);
        aplicarOrdenamiento();
    }

    private String obtenerValorCampo(HistorialArticuloItem item, String campo) {
        switch (campo) {
            case "Fecha":
                return item.getFecha();
            case "Hora":
                return item.getHora();
            case "Movimiento":
                return item.getTipoMovimiento();
            case "Proveedor":
                return item.getProveedor();
            case "Factura entrada":
                return item.getFacturaEntrada();
            case "Cliente":
                return item.getCliente();
            case "Factura salida":
                return item.getFacturaSalida();
            case "Usuario":
                return item.getUsuario();
            default:
                return "";
        }
    }

    private void configurarFiltroFechas() {
        if (fechaInicio == null || fechaFin == null) {
            return;
        }
        fechaInicio.valueProperty().addListener((obs, oldVal, newVal) -> aplicarFiltros());
        fechaFin.valueProperty().addListener((obs, oldVal, newVal) -> aplicarFiltros());
    }

    private LocalDate parseFechaItem(String fechaTexto) {
        if (fechaTexto == null || fechaTexto.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(fechaTexto, FORMATO_FECHA);
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private void filtrarProductos(String texto) {
        String filtro = texto == null ? "" : texto.trim().toLowerCase(Locale.ROOT);
        ObservableList<ProductoOpcion> filtrados = FXCollections.observableArrayList();
        if (filtro.isEmpty()) {
            filtrados.setAll(productosCache);
        } else {
            for (ProductoOpcion producto : productosCache) {
                if (producto.coincide(filtro)) {
                    filtrados.add(producto);
                }
            }
        }
        String textoActual = buscarProducto.getEditor().getText();
        productosFiltrados.setAll(filtrados);
        buscarProducto.getEditor().setText(textoActual);
        buscarProducto.getEditor().positionCaret(textoActual != null ? textoActual.length() : 0);
        if (!filtrados.isEmpty() && buscarProducto.isFocused()) {
            buscarProducto.show();
        }
    }

    private List<ProductoOpcion> cargarProductosActivos() {
        Map<String, ProductoOpcion> productos = new LinkedHashMap<>();
        try (Connection conn = new Conexion().conectar()) {
            if (conn == null) {
                return List.of();
            }
            String sql = "SELECT p.id AS pid, p.nombre AS pname, p.categoria AS categoria, "
                    + "p.unidadMedida AS unidad, p.material AS material, "
                    + "p.marca AS raw_marca, p.etiqueta AS raw_etiqueta, "
                    + "m.nombre AS marca_name, e.nombre AS etiqueta_name "
                    + "FROM productos p "
                    + "LEFT JOIN marcas m ON m.id = p.marca "
                    + "LEFT JOIN etiquetas e ON e.id = p.etiqueta "
                    + "WHERE p.estado = 'activo' "
                    + "ORDER BY p.nombre";

            try (PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String id = rs.getString("pid");
                    if (id == null || id.isBlank()) {
                        continue;
                    }
                    String nombre = rs.getString("pname");
                    String categoria = rs.getString("categoria");
                    String unidad = rs.getString("unidad");
                    String material = rs.getString("material");
                    String marca = rs.getString("marca_name");
                    String etiqueta = rs.getString("etiqueta_name");

                    String rawMarca = rs.getString("raw_marca");
                    String rawEtiqueta = rs.getString("raw_etiqueta");

                    if ((marca == null || marca.isBlank()) && rawMarca != null && !rawMarca.isBlank()) {
                        marca = rawMarca;
                    }
                    if ((etiqueta == null || etiqueta.isBlank()) && rawEtiqueta != null && !rawEtiqueta.isBlank()) {
                        etiqueta = rawEtiqueta;
                    }

                    String descripcion = construirDescripcion(marca, etiqueta, categoria, unidad, material);
                    productos.put(id, new ProductoOpcion(id, nombre, descripcion));
                }
            }

            String sqlClaves = "SELECT idProducto, idAlterno FROM claves WHERE estado = 'activo'";
            try (PreparedStatement ps = conn.prepareStatement(sqlClaves);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String idProducto = rs.getString("idProducto");
                    String idAlterno = rs.getString("idAlterno");
                    if (idProducto == null || idAlterno == null) {
                        continue;
                    }
                    ProductoOpcion producto = productos.get(idProducto);
                    if (producto != null) {
                        producto.agregarClaveAlterna(idAlterno);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return new ArrayList<>(productos.values());
    }

    private String construirDescripcion(String marca, String etiqueta, String clasificacion,
                                        String unidad, String material) {
        List<String> partes = new ArrayList<>();
        agregarParte(partes, marca);
        agregarParte(partes, etiqueta);
        agregarParte(partes, clasificacion);
        agregarParte(partes, unidad);
        agregarParte(partes, material);
        return String.join(", ", partes);
    }

    private String construirDescripcion(String marca, String etiqueta, String clasificacion,
                                        String unidad, String material, String descripcion) {
        List<String> partes = new ArrayList<>();
        agregarParte(partes, marca);
        agregarParte(partes, etiqueta);
        agregarParte(partes, clasificacion);
        agregarParte(partes, unidad);
        agregarParte(partes, material);
        agregarParte(partes, descripcion);
        return String.join(", ", partes);
    }

    private void agregarParte(List<String> partes, String valor) {
        if (valor != null && !valor.isBlank()) {
            partes.add(valor.trim());
        }
    }

    @FXML
    private void mostrarSelectorColumnas(MouseEvent event) {
        List<TableColumn<HistorialArticuloItem, ?>> columnas = new ArrayList<>(contenidoTabla.getColumns());
        SelectorColumnasPopup.mostrar((Node) event.getSource(), event.getScreenX(), event.getScreenY(),
                columnas, seleccion -> {
                    for (Map.Entry<TableColumn<HistorialArticuloItem, ?>, Boolean> entry : seleccion.entrySet()) {
                        entry.getKey().setVisible(entry.getValue());
                    }
                });
    }

    @FXML
    private void mostrarOrdenPopup(MouseEvent event) {
        List<String> criterios = List.of(
                "fecha",
                "hora",
                "movimiento",
                "antes",
                "despues",
                "entradas",
                "salidas",
                "proveedor",
                "facturaEntrada",
                "cliente",
                "facturaSalida",
                "usuario"
        );
        SelectorOrdenPopup.mostrar((Node) event.getSource(), event.getScreenX(), event.getScreenY(),
                criterios, criterioOrden, direccionOrden, seleccion -> {
                    criterioOrden = seleccion.getCriterio();
                    direccionOrden = seleccion.getDireccion();
                    aplicarOrdenamiento();
                });
    }

    private void aplicarOrdenamiento() {
        TableColumn<HistorialArticuloItem, ?> columna = obtenerColumnaOrden();
        if (columna == null) {
            return;
        }
        columna.setSortType("asc".equalsIgnoreCase(direccionOrden)
                ? TableColumn.SortType.ASCENDING
                : TableColumn.SortType.DESCENDING);
        contenidoTabla.getSortOrder().setAll(columna);
        contenidoTabla.sort();
    }

    private TableColumn<HistorialArticuloItem, ?> obtenerColumnaOrden() {
        Map<String, TableColumn<HistorialArticuloItem, ?>> columnas = new HashMap<>();
        columnas.put("fecha", colFecha);
        columnas.put("hora", colHora);
        columnas.put("movimiento", colTipoMovimiento);
        columnas.put("antes", colAntes);
        columnas.put("despues", colDespues);
        columnas.put("entradas", colEntradas);
        columnas.put("salidas", colSalidas);
        columnas.put("proveedor", colProveedor);
        columnas.put("facturaEntrada", colFacturaEntrada);
        columnas.put("cliente", colCliente);
        columnas.put("facturaSalida", colFacturaSalida);
        columnas.put("usuario", colUsuario);
        return columnas.get(criterioOrden);
    }

    private void cargarHistorialArticulo(String idProducto) {
        if (idProducto == null || idProducto.isBlank()) {
            historialItems.clear();
            limpiarDetalleProducto();
            return;
        }

        List<MovimientoArticulo> movimientos = new ArrayList<>();

        try (Connection conn = new Conexion().conectar()) {
            if (conn == null) {
                historialItems.clear();
                limpiarDetalleProducto();
                return;
            }

            cargarDetalleProducto(conn, idProducto);
            movimientos.addAll(obtenerEntradasArticulo(conn, idProducto));
            movimientos.addAll(obtenerSalidasArticulo(conn, idProducto));
        } catch (SQLException e) {
            e.printStackTrace();
        }

        movimientos.sort(Comparator.comparing(MovimientoArticulo::getFechaHora,
                Comparator.nullsLast(Comparator.naturalOrder())));

        List<HistorialArticuloItem> nuevos = new ArrayList<>();
        int existencias = 0;
        for (MovimientoArticulo mov : movimientos) {
            int antes = existencias;
            int despues;
            String entradas = "";
            String salidas = "";

            if (mov.getCantidad() >= 0) {
                despues = existencias + mov.getCantidad();
                entradas = String.valueOf(mov.getCantidad());
            } else {
                despues = existencias - Math.abs(mov.getCantidad());
                salidas = String.valueOf(Math.abs(mov.getCantidad()));
            }

            nuevos.add(new HistorialArticuloItem(
                    mov.getFecha(),
                    mov.getHora(),
                    mov.getTipoMovimiento(),
                    String.valueOf(antes),
                    String.valueOf(despues),
                    entradas,
                    salidas,
                    mov.getProveedor(),
                    mov.getFacturaEntrada(),
                    mov.getCliente(),
                    mov.getFacturaSalida(),
                    mov.getUsuario()
            ));
            existencias = despues;
        }

        historialItemsOriginal.setAll(nuevos);
        reiniciarFiltros();
        aplicarFiltros();
    }

    private void reiniciarFiltros() {
        restaurandoFiltros = true;
        filtrosActivos.clear();
        if (contenedorFiltros != null) {
            contenedorFiltros.getChildren().clear();
        }
        if (comboFiltro != null) {
            comboFiltro.setValue(null);
        }
        if (comboValor != null) {
            comboValor.getItems().clear();
            comboValor.setValue(null);
        }
        restaurandoFiltros = false;
        actualizarValoresFiltro(comboFiltro != null ? comboFiltro.getValue() : null);
    }

    private List<MovimientoArticulo> obtenerEntradasArticulo(Connection conn, String idProducto) throws SQLException {
        String sql = "SELECT e.fechaEntrada, e.horaEntrada, e.tipoEntrada, e.noFactura, "
                + "e.claveUsuarioEntrada, u.userName AS usuarioNombre, "
                + "e.idRemitente, p.Nombre AS proveedorNombre, s.nombre AS sucursalNombre, "
                + "de.cantidad "
                + "FROM detalle_Entrada de "
                + "JOIN entradas e ON e.idEntrada = de.claveEntrada "
                + "LEFT JOIN usuarios u ON u.idUsuario = e.claveUsuarioEntrada "
                + "LEFT JOIN proveedores p ON p.id = e.idRemitente "
                + "LEFT JOIN sucursales s ON s.id = e.idRemitente "
                + "WHERE de.claveProducto = ?";

        List<MovimientoArticulo> movimientos = new ArrayList<>();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, idProducto);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String proveedor = valorTexto(rs.getObject("proveedorNombre"));
                    if (proveedor.isBlank()) {
                        proveedor = valorTexto(rs.getObject("sucursalNombre"));
                    }
                    movimientos.add(new MovimientoArticulo(
                            valorTexto(rs.getObject("fechaEntrada")),
                            valorTexto(rs.getObject("horaEntrada")),
                            "Entrada",
                            obtenerCantidad(rs.getObject("cantidad")),
                            proveedor,
                            valorTexto(rs.getObject("noFactura")),
                            "",
                            "",
                            valorTexto(rs.getObject("usuarioNombre"))
                    ));
                }
            }
        }

        return movimientos;
    }

    private List<MovimientoArticulo> obtenerSalidasArticulo(Connection conn, String idProducto) throws SQLException {
        String sql = "SELECT s.fechaSalida, s.horaSalida, s.tipoSalida, s.noFactura, "
                + "s.claveUsuarioSalida, u.userName AS usuarioNombre, "
                + "s.idDestinatario, c.Nombre AS clienteNombre, su.nombre AS sucursalNombre, "
                + "ds.cantidad "
                + "FROM detalle_Salida ds "
                + "JOIN salidas s ON s.idSalida = ds.claveSalida "
                + "LEFT JOIN usuarios u ON u.idUsuario = s.claveUsuarioSalida "
                + "LEFT JOIN clientes c ON c.id = s.idDestinatario "
                + "LEFT JOIN sucursales su ON su.id = s.idDestinatario "
                + "WHERE ds.claveProductoSalida = ?";

        List<MovimientoArticulo> movimientos = new ArrayList<>();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, idProducto);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String cliente = valorTexto(rs.getObject("clienteNombre"));
                    if (cliente.isBlank()) {
                        cliente = valorTexto(rs.getObject("sucursalNombre"));
                    }
                    movimientos.add(new MovimientoArticulo(
                            valorTexto(rs.getObject("fechaSalida")),
                            valorTexto(rs.getObject("horaSalida")),
                            "Salida",
                            -obtenerCantidad(rs.getObject("cantidad")),
                            "",
                            "",
                            cliente,
                            valorTexto(rs.getObject("noFactura")),
                            valorTexto(rs.getObject("usuarioNombre"))
                    ));
                }
            }
        }

        return movimientos;
    }

    private int obtenerCantidad(Object valor) {
        if (valor == null) {
            return 0;
        }
        try {
            return Integer.parseInt(valor.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String valorTexto(Object valor) {
        return valor == null ? "" : valor.toString();
    }

    private LocalDateTime obtenerFechaHora(String fechaTexto, String horaTexto) {
        if ((fechaTexto == null || fechaTexto.isBlank()) && (horaTexto == null || horaTexto.isBlank())) {
            return null;
        }
        LocalDate fecha = null;
        LocalTime hora = null;
        if (fechaTexto != null && !fechaTexto.isBlank()) {
            try {
                fecha = LocalDate.parse(fechaTexto, FORMATO_FECHA);
            } catch (DateTimeParseException ignored) {
            }
        }
        if (horaTexto != null && !horaTexto.isBlank()) {
            try {
                hora = LocalTime.parse(horaTexto, FORMATO_HORA);
            } catch (DateTimeParseException ignored) {
            }
        }
        if (fecha == null && hora == null) {
            return null;
        }
        if (fecha == null) {
            fecha = LocalDate.MIN;
        }
        if (hora == null) {
            hora = LocalTime.MIN;
        }
        return LocalDateTime.of(fecha, hora);
    }

    private void cargarDetalleProducto(Connection conn, String idProducto) throws SQLException {
        String sqlProducto = "SELECT p.id, p.categoria, p.material, p.unidadMedida, p.descripcion, "
                + "m.nombre AS marca, e.nombre AS etiqueta "
                + "FROM productos p "
                + "LEFT JOIN marcas m ON m.id = p.marca "
                + "LEFT JOIN etiquetas e ON e.id = p.etiqueta "
                + "WHERE p.id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sqlProducto)) {
            ps.setString(1, idProducto);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String descripcion = construirDescripcion(
                            valorTexto(rs.getObject("marca")),
                            valorTexto(rs.getObject("etiqueta")),
                            valorTexto(rs.getObject("categoria")),
                            valorTexto(rs.getObject("unidadMedida")),
                            valorTexto(rs.getObject("material")),
                            valorTexto(rs.getObject("descripcion"))
                    );
                    lblClave.setText("Clave: " + idProducto);
                    lblDescripcion.setText("Descripción: " + descripcion);
                }
            }
        }

        String sqlPresentacion = "SELECT a.presentacion, a.factor "
                + "FROM articulo a "
                + "JOIN detalle_Entrada de ON de.idDetalleEntrada = a.idDetalleEntrada "
                + "WHERE de.claveProducto = ? "
                + "ORDER BY a.idArticulo DESC "
                + "LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sqlPresentacion)) {
            ps.setString(1, idProducto);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String presentacion = valorTexto(rs.getObject("presentacion"));
                    String factor = valorTexto(rs.getObject("factor"));
                    lblPresentacion.setText("Presentación: " + presentacion);
                    lblFactor.setText("Factor: " + factor);
                }
            }
        }

        String sqlExistencias = "SELECT COUNT(*) AS total "
                + "FROM articulo a "
                + "JOIN detalle_Entrada de ON de.idDetalleEntrada = a.idDetalleEntrada "
                + "WHERE de.claveProducto = ? AND LOWER(a.Estado) = 'disponible'";
        try (PreparedStatement ps = conn.prepareStatement(sqlExistencias)) {
            ps.setString(1, idProducto);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    lblExistencias.setText("Existencias: " + rs.getInt("total"));
                }
            }
        }
    }

    private void limpiarDetalleProducto() {
        lblClave.setText("Clave:");
        lblDescripcion.setText("Descripción:");
        lblPresentacion.setText("Presentación:");
        lblFactor.setText("Factor:");
        lblExistencias.setText("Existencias:");
    }

    @FXML
    private void exportarExcel() {
        if (contenidoTabla.getItems().isEmpty()) {
            mostrarAdvertencia("Advertencia", "No hay datos para exportar.");
            return;
        }
        exportador.exportarTabla(contenidoTabla, "Historial por artículo", "excel",
                obtenerFiltrosAplicados());
    }

    @FXML
    private void descargarPdf() {
        if (contenidoTabla.getItems().isEmpty()) {
            mostrarAdvertencia("Advertencia", "No hay datos para exportar.");
            return;
        }
        exportador.exportarTabla(contenidoTabla, "Historial por artículo", "pdf",
                obtenerFiltrosAplicados());
    }

    @FXML
    private void vistaPreviaPdf() {
        if (contenidoTabla.getItems().isEmpty()) {
            mostrarAdvertencia("Advertencia", "No hay datos para exportar.");
            return;
        }
        exportador.previsualizarPDF(contenidoTabla, "Historial por artículo",
                obtenerFiltrosAplicados());
    }

    private List<String> obtenerFiltrosAplicados() {
        List<String> filtrosAplicados = new ArrayList<>();
        ProductoOpcion seleccionado = buscarProducto.getValue();
        String texto = buscarProducto.getEditor().getText();
        if (seleccionado != null) {
            filtrosAplicados.add("Producto: " + seleccionado.getTextoVisible());
        } else if (texto != null && !texto.isBlank()) {
            filtrosAplicados.add("Producto contiene: " + texto.trim());
        }
        for (Filtro filtro : filtrosActivos) {
            filtrosAplicados.add(filtro.campo + ": " + filtro.valor);
        }
        filtrosAplicados.add(obtenerPeriodoExport());
        return filtrosAplicados;
    }

    private String obtenerPeriodoExport() {
        LocalDate inicio = fechaInicio != null ? fechaInicio.getValue() : null;
        LocalDate fin = fechaFin != null ? fechaFin.getValue() : null;
        if (inicio == null && fin == null) {
            return PERIODO_EXPORT;
        }
        DateTimeFormatter formato = DateTimeFormatter.ofPattern("dd/MM/yy");
        String textoInicio = inicio != null ? inicio.format(formato) : "...";
        String textoFin = fin != null ? fin.format(formato) : "...";
        return "Periodo: " + textoInicio + "-" + textoFin;
    }

    private void mostrarAdvertencia(String titulo, String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.WARNING);
        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }

    private static class ProductoOpcion {
        private final String id;
        private final String nombre;
        private final String descripcion;
        private final Set<String> clavesAlternas = new LinkedHashSet<>();

        private ProductoOpcion(String id, String nombre, String descripcion) {
            this.id = id;
            this.nombre = nombre == null ? "" : nombre;
            this.descripcion = descripcion == null ? "" : descripcion;
        }

        private void agregarClaveAlterna(String clave) {
            if (clave == null || clave.isBlank()) {
                return;
            }
            clavesAlternas.add(clave.trim());
        }

        private String getId() {
            return id;
        }

        private String getTextoVisible() {
            String base = id + " - " + nombre;
            if (descripcion.isBlank()) {
                return base;
            }
            return base + " - " + descripcion;
        }

        private boolean coincide(String filtro) {
            String normalizado = filtro.toLowerCase(Locale.ROOT);
            if (id != null && id.toLowerCase(Locale.ROOT).contains(normalizado)) {
                return true;
            }
            if (nombre != null && nombre.toLowerCase(Locale.ROOT).contains(normalizado)) {
                return true;
            }
            for (String clave : clavesAlternas) {
                if (clave.toLowerCase(Locale.ROOT).contains(normalizado)) {
                    return true;
                }
            }
            return getTextoVisible().toLowerCase(Locale.ROOT).contains(normalizado);
        }
    }

    private class MovimientoArticulo {
        private final String fecha;
        private final String hora;
        private final String tipoMovimiento;
        private final int cantidad;
        private final String proveedor;
        private final String facturaEntrada;
        private final String cliente;
        private final String facturaSalida;
        private final String usuario;

        private MovimientoArticulo(String fecha, String hora, String tipoMovimiento, int cantidad,
                                   String proveedor, String facturaEntrada, String cliente,
                                   String facturaSalida, String usuario) {
            this.fecha = fecha;
            this.hora = hora;
            this.tipoMovimiento = tipoMovimiento;
            this.cantidad = cantidad;
            this.proveedor = proveedor;
            this.facturaEntrada = facturaEntrada;
            this.cliente = cliente;
            this.facturaSalida = facturaSalida;
            this.usuario = usuario;
        }

        private LocalDateTime getFechaHora() {
            return obtenerFechaHora(fecha, hora);
        }

        private String getFecha() {
            return fecha;
        }

        private String getHora() {
            return hora;
        }

        private String getTipoMovimiento() {
            return tipoMovimiento;
        }

        private int getCantidad() {
            return cantidad;
        }

        private String getProveedor() {
            return proveedor;
        }

        private String getFacturaEntrada() {
            return facturaEntrada;
        }

        private String getCliente() {
            return cliente;
        }

        private String getFacturaSalida() {
            return facturaSalida;
        }

        private String getUsuario() {
            return usuario;
        }
    }

    public static class HistorialArticuloItem {
        private final String fecha;
        private final String hora;
        private final String tipoMovimiento;
        private final String antes;
        private final String despues;
        private final String entradas;
        private final String salidas;
        private final String proveedor;
        private final String facturaEntrada;
        private final String cliente;
        private final String facturaSalida;
        private final String usuario;

        public HistorialArticuloItem(String fecha, String hora, String tipoMovimiento, String antes, String despues,
                                     String entradas, String salidas, String proveedor, String facturaEntrada,
                                     String cliente, String facturaSalida, String usuario) {
            this.fecha = fecha;
            this.hora = hora;
            this.tipoMovimiento = tipoMovimiento;
            this.antes = antes;
            this.despues = despues;
            this.entradas = entradas;
            this.salidas = salidas;
            this.proveedor = proveedor;
            this.facturaEntrada = facturaEntrada;
            this.cliente = cliente;
            this.facturaSalida = facturaSalida;
            this.usuario = usuario;
        }

        public String getFecha() { return fecha; }
        public String getHora() { return hora; }
        public String getTipoMovimiento() { return tipoMovimiento; }
        public String getAntes() { return antes; }
        public String getDespues() { return despues; }
        public String getEntradas() { return entradas; }
        public String getSalidas() { return salidas; }
        public String getProveedor() { return proveedor; }
        public String getFacturaEntrada() { return facturaEntrada; }
        public String getCliente() { return cliente; }
        public String getFacturaSalida() { return facturaSalida; }
        public String getUsuario() { return usuario; }
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
