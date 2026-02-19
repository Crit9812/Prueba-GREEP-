package main;

import Compartido.model.NotificacionService;
import conexion.Conexion;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
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

    }

    public static void main(String[] args) {
        launch(args);
    }

}
