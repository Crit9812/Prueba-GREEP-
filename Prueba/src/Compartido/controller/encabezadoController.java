package Compartido.controller;

import Compartido.helper.RefrescoHelper;
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

    @FXML
    public void initialize(){
        Platform.runLater(() -> {

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
        });

        labelUsuario.setText(Compartido.sesion.SesionUsuario.getNombreUsuario());
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
}
