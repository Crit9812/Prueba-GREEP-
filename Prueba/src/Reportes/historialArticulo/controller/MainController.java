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
import javafx.scene.input.MouseEvent;
import javafx.scene.Node;
import javafx.scene.layout.*;
import javafx.application.Platform;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javafx.util.StringConverter;

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
    @FXML private ComboBox<ProductoOpcion> buscarProducto;

    @FXML private Region expansor;
    @FXML private Label lblVista;
    @FXML private Label lblDescargar;

    @FXML private VBox contenedorTabla;
    @FXML private TableView<?> contenidoTabla;
    @FXML private TableColumn<?, ?> colFecha;
    @FXML private TableColumn<?, ?> colHora;
    @FXML private TableColumn<?, ?> colTipoMovimiento;
    @FXML private TableColumn<?, ?> colAntes;
    @FXML private TableColumn<?, ?> colDespues;
    @FXML private TableColumn<?, ?> colEntradas;
    @FXML private TableColumn<?, ?> colSalidas;
    @FXML private TableColumn<?, ?> colProveedor;
    @FXML private TableColumn<?, ?> colFacturaEntrada;
    @FXML private TableColumn<?, ?> colCliente;
    @FXML private TableColumn<?, ?> colFacturaSalida;
    @FXML private TableColumn<?, ?> colUsuario;

    @FXML private encabezadoController paneNavbarController;

    private final ObservableList<ProductoOpcion> productosCache = FXCollections.observableArrayList();
    private final ObservableList<ProductoOpcion> productosFiltrados = FXCollections.observableArrayList();
    private boolean actualizandoBusqueda = false;
    private String criterioOrden = "fecha";
    private String direccionOrden = "desc";

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

            HBox.setHgrow(expansor, Priority.ALWAYS);
            expansor.setMinWidth(10);

            lblVista.setMinWidth(Region.USE_PREF_SIZE);
            lblDescargar.setMinWidth(Region.USE_PREF_SIZE);

            contenedorTabla.prefHeightProperty().bind(contenedor.heightProperty().multiply(0.71));
            contenidoTabla.prefHeightProperty().bind(contenedorTabla.heightProperty().multiply(0.9));

            paneNavbarController.setTitulo("Historial por artículo", "#ffffff");
            configurarBuscadorProducto();
        });
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
            }
        });
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

    private void agregarParte(List<String> partes, String valor) {
        if (valor != null && !valor.isBlank()) {
            partes.add(valor.trim());
        }
    }

    @FXML
    private void mostrarSelectorColumnas(MouseEvent event) {
        List<TableColumn<?, ?>> columnas = new ArrayList<>(contenidoTabla.getColumns());
        SelectorColumnasPopup.mostrar((Node) event.getSource(), event.getScreenX(), event.getScreenY(),
                columnas, seleccion -> {
                    for (Map.Entry<TableColumn<?, ?>, Boolean> entry : seleccion.entrySet()) {
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
        TableColumn<?, ?> columna = obtenerColumnaOrden();
        if (columna == null) {
            return;
        }
        columna.setSortType("asc".equalsIgnoreCase(direccionOrden)
                ? TableColumn.SortType.ASCENDING
                : TableColumn.SortType.DESCENDING);
        contenidoTabla.getSortOrder().setAll(columna);
        contenidoTabla.sort();
    }

    private TableColumn<?, ?> obtenerColumnaOrden() {
        Map<String, TableColumn<?, ?>> columnas = new HashMap<>();
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
        return filtrosAplicados;
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
}
