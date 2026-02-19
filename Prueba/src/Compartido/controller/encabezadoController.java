package Compartido.controller;

import Compartido.helper.BusquedaProductoHelper;
import Compartido.helper.RefrescoHelper;
import Compartido.model.NotificacionService;
import VentanaPrincipal.controller.EnumVistas;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Side;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import conexion.Conexion;

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

        menuSugerencias.setAutoHide(true);
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
            Label label = new Label(producto.id + " - " + producto.nombre);
            label.setWrapText(false);

            CustomMenuItem item = new CustomMenuItem(label, true);
            item.setOnAction(event -> seleccionarProducto(producto));
            items.add(item);
        }

        menuSugerencias.getItems().setAll(items);

        if (!menuSugerencias.isShowing()) {
            menuSugerencias.show(searchBar, Side.BOTTOM, 0, 0);
        }
    }

    private List<ProductoBusqueda> buscarProductos(String texto) {
        List<ProductoBusqueda> productos = new ArrayList<>();

        String sql = """
                SELECT p.id, p.nombre
                FROM productos p
                WHERE p.id LIKE ? OR p.nombre LIKE ?
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
                    productos.add(new ProductoBusqueda(rs.getString("id"), rs.getString("nombre")));
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

        BusquedaProductoHelper.guardarSolicitud(producto.id, producto.nombre);

        if (controladorPrincipal != null) {
            controladorPrincipal.cargarVista(EnumVistas.INVENTARIO);
        }
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

        private ProductoBusqueda(String id, String nombre) {
            this.id = id;
            this.nombre = nombre;
        }
    }
}
