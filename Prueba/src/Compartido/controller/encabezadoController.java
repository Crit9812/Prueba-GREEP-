package Compartido.controller;

import Compartido.helper.RefrescoHelper;
import Compartido.sesion.BusquedaInventarioState;
import Consultas.producto.model.model;
import Consultas.producto.model.producto;
import VentanaPrincipal.controller.EnumVistas;
import Compartido.model.NotificacionService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class encabezadoController {

    @FXML private TextField searchBar;
    @FXML private BorderPane panel;
    @FXML private ImageView iconoNavbar;
    @FXML private ImageView iconoNavbar2;
    @FXML private ImageView iconoNavbar3;
    @FXML private Label labelUsuario;
    @FXML private Label labelTitulo;

    private final NotificacionService notificacionService = new NotificacionService();
    private final model productoModel = new model();
    private final ContextMenu sugerenciasMenu = new ContextMenu();
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

        panel.requestFocus();
        configurarBusquedaProductos();
    }

    public void setControladorPrincipal(VentanaPrincipal.controller.MainController controladorPrincipal) {
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

    private void configurarBusquedaProductos() {
        searchBar.textProperty().addListener((obs, oldValue, newValue) -> {
            String texto = newValue == null ? "" : newValue.trim();
            if (texto.isEmpty()) {
                sugerenciasMenu.hide();
                return;
            }

            javafx.collections.ObservableList<producto> productos = productoModel.busquedaMultipleProductos(texto);
            if (productos.isEmpty()) {
                sugerenciasMenu.hide();
                return;
            }

            sugerenciasMenu.getItems().clear();
            int limite = Math.min(productos.size(), 8);
            for (int i = 0; i < limite; i++) {
                producto item = productos.get(i);
                MenuItem menuItem = crearItemSugerencia(item);
                sugerenciasMenu.getItems().add(menuItem);
            }

            if (!sugerenciasMenu.isShowing()) {
                sugerenciasMenu.show(searchBar, javafx.geometry.Side.BOTTOM, 0, 0);
            }
        });

        searchBar.focusedProperty().addListener((obs, antes, enfocado) -> {
            if (!enfocado) {
                sugerenciasMenu.hide();
            }
        });
    }

    private MenuItem crearItemSugerencia(producto producto) {
        Label titulo = new Label(producto.getIdProducto() + " · " + producto.getNombreProducto());
        String descripcion = producto.getDescripcion() == null ? "" : producto.getDescripcion().trim();
        if (descripcion.length() > 70) {
            descripcion = descripcion.substring(0, 70) + "...";
        }
        Label subtitulo = new Label(descripcion);
        VBox contenido = new VBox(titulo, subtitulo);
        contenido.setSpacing(2);

        CustomMenuItem item = new CustomMenuItem(contenido, true);
        item.setOnAction(event -> seleccionarProducto(producto));
        return item;
    }

    private void seleccionarProducto(producto producto) {
        String nombreProducto = producto.getNombreProducto() == null ? "" : producto.getNombreProducto().trim();
        if (nombreProducto.isEmpty()) {
            return;
        }

        searchBar.setText(nombreProducto);
        sugerenciasMenu.hide();

        BusquedaInventarioState.setProductoPendiente(nombreProducto);
        if (controladorPrincipal != null) {
            controladorPrincipal.cargarVista(EnumVistas.INVENTARIO);
        }
    }
}
