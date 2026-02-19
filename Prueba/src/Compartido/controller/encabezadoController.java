package Compartido.controller;

import Compartido.helper.RefrescoHelper;
import Compartido.model.NotificacionService;
import Compartido.sesion.BusquedaGlobalProductoContext;
import VentanaPrincipal.controller.EnumVistas;
import VentanaPrincipal.controller.MainController;
import conexion.Conexion;
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
    private MainController controladorPrincipal;

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

        configurarBuscadorGlobal();

        Platform.runLater(() -> {
            panel.requestFocus();
            menuSugerencias.hide();
        });
    }

    public void setControladorPrincipal(MainController controladorPrincipal) {
        this.controladorPrincipal = controladorPrincipal;
    }

    public void setTitulo(String titulo, String colorHex) {
        labelTitulo.setText(titulo);
        labelTitulo.setStyle("-fx-background-color: " + colorHex + ";");
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

    private void configurarBuscadorGlobal() {
        searchBar.textProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue == null || newValue.isBlank()) {
                menuSugerencias.hide();
                return;
            }

            List<ProductoSugerencia> sugerencias = buscarSugerenciasProducto(newValue);
            mostrarSugerencias(sugerencias);
        });

        searchBar.focusedProperty().addListener((obs, antes, ahora) -> {
            if (!ahora) {
                menuSugerencias.hide();
            }
        });
    }

    private List<ProductoSugerencia> buscarSugerenciasProducto(String textoBusqueda) {
        List<ProductoSugerencia> sugerencias = new ArrayList<>();

        String sql = """
                SELECT id, nombre, descripcion
                FROM productos
                WHERE estado = 'activo'
                  AND (LOWER(id) LIKE LOWER(?) OR LOWER(nombre) LIKE LOWER(?))
                ORDER BY nombre ASC
                LIMIT 8
                """;

        try (Connection conn = new Conexion().conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            String like = "%" + textoBusqueda.trim() + "%";
            ps.setString(1, like);
            ps.setString(2, like);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String id = rs.getString("id");
                    String nombre = rs.getString("nombre");
                    String descripcion = rs.getString("descripcion");
                    sugerencias.add(new ProductoSugerencia(id, nombre, recortarDescripcion(descripcion)));
                }
            }
        } catch (Exception e) {
            menuSugerencias.hide();
        }

        return sugerencias;
    }

    private void mostrarSugerencias(List<ProductoSugerencia> sugerencias) {
        if (sugerencias.isEmpty() || searchBar.getText() == null || searchBar.getText().isBlank()) {
            menuSugerencias.hide();
            return;
        }

        List<MenuItem> items = new ArrayList<>();
        for (ProductoSugerencia producto : sugerencias) {
            String textoMenu = producto.nombre + " (ID: " + producto.id + ")";
            if (producto.descripcion != null && !producto.descripcion.isBlank()) {
                textoMenu += " • " + producto.descripcion;
            }

            MenuItem item = new MenuItem(textoMenu);
            item.setOnAction(event -> seleccionarProducto(producto));
            items.add(item);
        }

        menuSugerencias.getItems().setAll(items);
        if (!menuSugerencias.isShowing()) {
            menuSugerencias.show(searchBar, Side.BOTTOM, 0, 0);
        }
    }

    private void seleccionarProducto(ProductoSugerencia producto) {
        searchBar.setText(producto.nombre);
        menuSugerencias.hide();

        BusquedaGlobalProductoContext.setBusquedaPendiente(producto.id, producto.nombre);

        if (controladorPrincipal != null) {
            controladorPrincipal.cargarVista(EnumVistas.INVENTARIO);
        }
    }

    private String recortarDescripcion(String descripcion) {
        if (descripcion == null || descripcion.isBlank()) {
            return "";
        }
        String limpia = descripcion.trim().replaceAll("\\s+", " ");
        if (limpia.length() <= 45) {
            return limpia;
        }
        return limpia.substring(0, 45) + "...";
    }

    private static class ProductoSugerencia {
        private final String id;
        private final String nombre;
        private final String descripcion;

        private ProductoSugerencia(String id, String nombre, String descripcion) {
            this.id = id;
            this.nombre = nombre;
            this.descripcion = descripcion;
        }
    }
}
