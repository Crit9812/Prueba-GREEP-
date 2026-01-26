package Reportes.utilidades.controller;

import Compartido.controller.encabezadoController;
import Compartido.controller.navbarController;
import Reportes.utilidades.model.ItemUtilidad;
import conexion.Conexion;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import javafx.application.Platform;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

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
    private final DecimalFormat formatoMoneda = new DecimalFormat("$ #,##0.00", new DecimalFormatSymbols(Locale.US));
    private final DecimalFormat formatoPorcentaje = new DecimalFormat("0.00'%'");

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
                "WHERE s.tipoSalida = 'venta' AND s.Estado = 'finalizado' " +
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

                itemsUtilidad.add(new ItemUtilidad(
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
}
