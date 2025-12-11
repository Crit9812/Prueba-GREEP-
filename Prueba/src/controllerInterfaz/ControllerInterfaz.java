package controllerInterfaz;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.Screen;
import javafx.geometry.Rectangle2D;

import java.io.IOException;

public class ControllerInterfaz {
    private static Stage primaryStage;

    public static void setStage(Stage stage) {
        primaryStage = stage;
    }


    public static void cambiarVista(String rutaFXML, String rutaStyle, Object controlador) {
        try {
            FXMLLoader loader = new FXMLLoader(ControllerInterfaz.class.getResource(rutaFXML));
            loader.setController(controlador); // Asignamos el controlador antes de cargar

            Parent root = loader.load(); // carga FXML con el controlador asignado

            Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
            Scene scene = new Scene(root, screenBounds.getWidth(), screenBounds.getHeight());
            scene.getStylesheets().add(ControllerInterfaz.class.getResource(rutaStyle).toExternalForm());
            root.applyCss();
            root.layout();

            primaryStage.getIcons().add(new Image(ControllerInterfaz.class.getResourceAsStream("/img/logo-GREEP.png")));
            primaryStage.setTitle("Gestor de inventario GREEP");
            primaryStage.setScene(scene);
            primaryStage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}