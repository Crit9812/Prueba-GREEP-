package Configuracion.controller;

import VentanaPrincipal.controller.ControladorVista;
import VentanaPrincipal.controller.EnumVistas;
import javafx.fxml.FXML;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class MainController implements ControladorVista {

    @FXML private StackPane root;
    @FXML private VBox contenedor; // Este es el contenedor específico de Configuración

    private StackPane contentArea; // El contentArea de la ventana principal
    private VentanaPrincipal.controller.MainController controladorPrincipal;

    @FXML
    public void initialize() {

        // Aquí puedes agregar lógica específica de Configuración
        // Por ejemplo, bindear tamaños si es necesario
        if (contenedor != null && root != null) {
            contenedor.prefHeightProperty().bind(root.heightProperty());
            contenedor.prefWidthProperty().bind(root.widthProperty());
        }
        SplitPane.setResizableWithParent(contenedor, true);
    }

    @Override
    public void setContentArea(StackPane contentArea) {
        this.contentArea = contentArea;
    }

    @Override
    public void setControladorPrincipal(VentanaPrincipal.controller.MainController controladorPrincipal) {
        this.controladorPrincipal = controladorPrincipal;
    }

}