package main;

import Compartido.model.NotificacionService;
import conexion.Conexion;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        // Establecer el stage en la clase ControllerInterfaz
        Conexion conect = new Conexion();
        conect.conectar();

        NotificacionService notificacionService = new NotificacionService();
        notificacionService.generarNotificacionesIniciales();

        //conexion de css de interfaz operaciones
        FXMLLoader inicio = new FXMLLoader(getClass().getResource("/login/view/main_view.fxml"));
        login.controller.MainController controlador = new login.controller.MainController();
        inicio.setController(controlador);
        Scene sceneInicio = new Scene(inicio.load());
        sceneInicio.getStylesheets().add(getClass().getResource("/login/style/estilos.css").toExternalForm());


        //Ajustes de la ventana principal/completa
        stage.getIcons().add(new Image(getClass().getResourceAsStream("/img/logo-GREEP.png")));
        stage.setTitle("Gestor de inventario GREEP");
        stage.setScene(sceneInicio);
        stage.setMaximized(true);
        controllerInterfaz.ControllerInterfaz.setStage(stage);
        stage.show();

        Platform.runLater(() -> mostrarAvisoNotificacionesSiExisten(notificacionService));
    }

    private void mostrarAvisoNotificacionesSiExisten(NotificacionService notificacionService) {
        if (!notificacionService.hayNotificacionesActivas()) {
            return;
        }

        ButtonType btnCerrar = new ButtonType("Cerrar", ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType btnVer = new ButtonType("Ver", ButtonBar.ButtonData.YES);

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Notificaciones");
        alert.setHeaderText("Existen notificaciones sin leer");
        alert.setContentText("¿Deseas verlas ahora?");
        alert.getButtonTypes().setAll(btnCerrar, btnVer);

        alert.showAndWait().ifPresent(buttonType -> {
            if (buttonType == btnVer) {
                abrirVentanaNotificaciones();
            }
        });
    }

    private void abrirVentanaNotificaciones() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Compartido/view/notificaciones.fxml"));
            Scene scene = new Scene(loader.load());

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Notificaciones");
            stage.setScene(scene);
            stage.showAndWait();
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "No se pudo abrir la ventana de notificaciones: " + e.getMessage());
            alert.showAndWait();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }

}
