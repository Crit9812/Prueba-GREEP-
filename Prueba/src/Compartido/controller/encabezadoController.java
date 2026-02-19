package Compartido.controller;

import Compartido.helper.BusquedaProductoHelper;
import Compartido.helper.RefrescoHelper;
import Compartido.model.NotificacionService;
import VentanaPrincipal.controller.EnumVistas;
import conexion.Conexion;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Side;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class encabezadoController {

    @FXML private TextField searchBar;
    @FXML private BorderPane panel;
    @FXML private ImageView iconoNavbar;
    @FXML private ImageView iconoNavbar2;
    @FXML private ImageView iconoNavbar3;
    @FXML private Label labelUsuario;
    @FXML private Label labelTitulo;

    private final NotificacionService notificacionService = new NotificacionService();
    private final ContextMenu menuSugerencias = new ContextMenu();
    private VentanaPrincipal.controller.MainController controladorPrincipal;

    @FXML
    public void initialize(){
        searchBar.prefWidthProperty().bind(panel.widthProperty().multiply(0.23));
        searchBar.prefHeightProperty().bind(panel.heightProperty().multiply(0.49));

        labelUsuario.prefWidthProperty().bind(panel.widthProperty().multiply(0.08));
        labelUsuario.prefHeightProperty().bind(panel.heightProperty().multiply(0.8));

        iconoNavbar.fitHeightProperty().bind(panel.heightProperty().multiply(0.48));
        iconoNavbar.fitWidthProperty().bind(panel.widthProperty().multiply(0.028));

        iconoNavbar2.fitHeightProperty().bind(panel.heightProperty().multiply(0.48));
        iconoNavbar2.fitWidthProperty().bind(panel.widthProperty().multiply(0.028));

        iconoNavbar3.fitHeightProperty().bind(panel.heightProperty().multiply(0.43));
        iconoNavbar3.fitWidthProperty().bind(panel.widthProperty().multiply(0.028));

        labelTitulo.prefWidthProperty().bind(panel.widthProperty().multiply(0.15));
        labelTitulo.prefHeightProperty().bind(panel.heightProperty().multiply(0.5));

        actualizarIconoNotificacionesEnParalelo();

        labelUsuario.setText(Compartido.sesion.SesionUsuario.getNombreUsuario());
        configurarBusquedaGlobal();

        Platform.runLater(() -> {
            if (panel != null) {
                panel.requestFocus();
            }
        });
    }

    private void configurarBusquedaGlobal() {
        if (searchBar == null) {
            return;
        }

        searchBar.textProperty().addListener((obs, oldValue, newValue) -> mostrarSugerencias(newValue));

        searchBar.focusedProperty().addListener((obs, oldValue, focused) -> {
            if (!focused || searchBar.getText() == null || searchBar.getText().trim().isBlank()) {
                menuSugerencias.hide();
            }
        });

        searchBar.setOnAction(event -> ejecutarBusquedaPorEnter());
        menuSugerencias.setAutoHide(true);
    }

    private void ejecutarBusquedaPorEnter() {
        String texto = searchBar.getText() == null ? "" : searchBar.getText().trim();
        if (texto.isBlank()) {
            menuSugerencias.hide();
            return;
        }

        menuSugerencias.hide();
        BusquedaProductoHelper.guardarSolicitud("", "", texto);
        navegarAInventario();
    }

    private void mostrarSugerencias(String textoBusqueda) {
        String texto = textoBusqueda == null ? "" : textoBusqueda.trim();
        if (texto.isBlank()) {
            menuSugerencias.hide();
            return;
        }

        List<ProductoBusqueda> resultados = buscarProductos(texto);
        if (resultados.isEmpty()) {
            menuSugerencias.hide();
            return;
        }

        List<CustomMenuItem> items = new ArrayList<>();
        for (ProductoBusqueda producto : resultados) {
            Label label = new Label(armarTextoSugerencia(producto));
            label.setWrapText(true);
            label.setMaxWidth(620);

            CustomMenuItem item = new CustomMenuItem(label, true);
            item.setOnAction(event -> seleccionarProducto(producto));
            items.add(item);
        }

        menuSugerencias.getItems().setAll(items);

        if (!menuSugerencias.isShowing()) {
            menuSugerencias.show(searchBar, Side.BOTTOM, 0, 0);
        }
    }

    private String armarTextoSugerencia(ProductoBusqueda producto) {
        return producto.id + " - " + producto.nombre +
                " | Marca: " + producto.marca +
                " | Proveedor: " + producto.proveedor +
                " | Unidad: " + producto.unidadMedida +
                " | Clasificación: " + producto.clasificacion +
                " | Existencias: " + producto.existencias;
    }

    private List<ProductoBusqueda> buscarProductos(String texto) {
        List<ProductoBusqueda> productos = new ArrayList<>();

        String sql = """
                SELECT p.id,
                       p.nombre,
                       COALESCE(m.nombre, 'Sin marca') AS marca,
                       COALESCE(MIN(pv.Nombre), 'Sin proveedor') AS proveedor,
                       COALESCE(p.unidadMedida, 'Sin unidad') AS unidadMedida,
                       COALESCE(p.categoria, 'Sin clasificación') AS clasificacion,
                       (SELECT COUNT(*)
                        FROM articulo a
                        INNER JOIN detalle_Entrada de2 ON a.idDetalleEntrada = de2.idDetalleEntrada
                        WHERE de2.claveProducto = p.id
                          AND LOWER(a.Estado) IN ('disponible','segmentado')) AS existencias
                FROM productos p
                LEFT JOIN marcas m ON m.id = p.marca
                LEFT JOIN claves ca ON ca.idProducto = p.id AND ca.estado = 'activo'
                LEFT JOIN proveedores pv ON pv.id = ca.idProveedor AND pv.status = 'activo'
                WHERE p.id LIKE ? OR p.nombre LIKE ?
                GROUP BY p.id, p.nombre, m.nombre, p.unidadMedida, p.categoria, existencias
                ORDER BY
                    CASE WHEN p.id = ? THEN 0 ELSE 1 END,
                    CASE WHEN p.nombre = ? THEN 0 ELSE 1 END,
                    p.nombre ASC
                LIMIT 8
                """;

        try (Connection conn = new Conexion().conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            String patron = "%" + texto + "%";
            ps.setString(1, patron);
            ps.setString(2, patron);
            ps.setString(3, texto);
            ps.setString(4, texto);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    productos.add(new ProductoBusqueda(
                            rs.getString("id"),
                            rs.getString("nombre"),
                            rs.getString("marca"),
                            rs.getString("proveedor"),
                            rs.getString("unidadMedida"),
                            rs.getString("clasificacion"),
                            rs.getInt("existencias")
                    ));
                }
            }
        } catch (Exception e) {
            menuSugerencias.hide();
        }

        return productos;
    }

    private void seleccionarProducto(ProductoBusqueda producto) {
        if (producto == null) {
            return;
        }

        searchBar.setText(producto.nombre);
        menuSugerencias.hide();

        BusquedaProductoHelper.guardarSolicitud("", producto.nombre, producto.nombre);

        navegarAInventario();
    }


    private void navegarAInventario() {
        if (controladorPrincipal == null) {
            return;
        }

        Platform.runLater(() -> {
            try {
                controladorPrincipal.cambiarVista("REPORTES");
            } catch (Exception ignored) {
            }

            Platform.runLater(() -> controladorPrincipal.cargarVista(EnumVistas.INVENTARIO));
        });
    }
    public void setTitulo(String titulo, String colorHex) {
        labelTitulo.setText(titulo);
        labelTitulo.setStyle("-fx-background-color: " + colorHex + ";");
    }

    public void setControladorPrincipal(VentanaPrincipal.controller.MainController controladorPrincipal) {
        this.controladorPrincipal = controladorPrincipal;
    }

    @FXML
    private void salir() {
        login.controller.MainController controlador = new login.controller.MainController();
        controllerInterfaz.ControllerInterfaz.cambiarVista(
                "/login/view/main_view.fxml",
                "/login/style/estilos.css",
                controlador
        );
    }

    @FXML
    private void actualizar() {
        RefrescoHelper.refrescar();
        actualizarIconoNotificacionesEnParalelo();
    }

    @FXML
    private void abrirNotificaciones() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Compartido/view/notificaciones.fxml"));
            Scene scene = new Scene(loader.load());

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Notificaciones");
            stage.setScene(scene);
            stage.showAndWait();
            actualizarIconoNotificacionesEnParalelo();
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "No se pudo abrir la ventana de notificaciones: " + e.getMessage());
            alert.showAndWait();
        }
    }

    private void actualizarIconoNotificacionesEnParalelo() {
        Thread hiloRevisionNotificaciones = new Thread(() -> {
            boolean hayNotificacionesActivas = notificacionService.hayNotificacionesActivas();
            String icono = hayNotificacionesActivas ? "/img/n.png" : "/img/sobreC.png";

            Platform.runLater(() -> iconoNavbar3.setImage(new Image(getClass().getResourceAsStream(icono))));
        }, "hilo-revision-notificaciones-encabezado");

        hiloRevisionNotificaciones.setDaemon(true);
        hiloRevisionNotificaciones.start();
    }

    private static class ProductoBusqueda {
        private final String id;
        private final String nombre;
        private final String marca;
        private final String proveedor;
        private final String unidadMedida;
        private final String clasificacion;
        private final int existencias;

        private ProductoBusqueda(String id, String nombre, String marca, String proveedor,
                                 String unidadMedida, String clasificacion, int existencias) {
            this.id = id;
            this.nombre = nombre;
            this.marca = marca;
            this.proveedor = proveedor;
            this.unidadMedida = unidadMedida;
            this.clasificacion = clasificacion;
            this.existencias = existencias;
        }
    }
}
