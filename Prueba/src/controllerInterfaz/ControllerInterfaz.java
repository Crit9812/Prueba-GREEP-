package controllerInterfaz;

import Compartido.sesion.PermisosRol;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputControl;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
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

            if (PermisosRol.modoSoloLecturaReportesConsultas()
                    && (rutaFXML.startsWith("/Consultas/") || rutaFXML.startsWith("/Reportes/"))) {
                aplicarSoloLectura(root);
            }

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

    private static void aplicarSoloLectura(Parent root) {
        root.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
            if (event.getClickCount() >= 2) {
                event.consume();
            }
        });

        root.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ENTER) {
                event.consume();
            }
        });

        aplicarSoloLecturaRecursivo(root);
    }

    private static void aplicarSoloLecturaRecursivo(Parent parent) {
        for (Node node : parent.getChildrenUnmodifiable()) {
            if (node instanceof TextInputControl textInputControl) {
                textInputControl.setEditable(false);
            }

            if (node instanceof DatePicker datePicker) {
                datePicker.setDisable(true);
            }

            if (node instanceof ComboBox<?> comboBox) {
                comboBox.setDisable(true);
            }

            if (node instanceof TableView<?> tableView) {
                tableView.setEditable(false);
            }

            if (node instanceof Button button && esBotonMutacion(button)) {
                button.setVisible(false);
                button.setManaged(false);
            }

            if (node instanceof Parent childParent) {
                aplicarSoloLecturaRecursivo(childParent);
            }
        }
    }

    private static boolean esBotonMutacion(Button button) {
        String id = button.getId() == null ? "" : button.getId().toLowerCase();
        String text = button.getText() == null ? "" : button.getText().toLowerCase();
        String valor = id + " " + text;

        return valor.contains("agregar")
                || valor.contains("nuevo")
                || valor.contains("editar")
                || valor.contains("eliminar")
                || valor.contains("guardar")
                || valor.contains("registrar")
                || valor.contains("actualizar")
                || valor.contains("ajuste");
    }
}
