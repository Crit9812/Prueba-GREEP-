package Consultas.controller;

import VentanaPrincipal.controller.ControladorVista;
import VentanaPrincipal.controller.EnumVistas;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class MainController implements ControladorVista {

    @FXML private StackPane root;
    @FXML private GridPane buttonGrid;
    @FXML private VBox contenedor;

    private StackPane contentArea; // Inyectado desde el MainController principal
    private VentanaPrincipal.controller.MainController controladorPrincipal;

    @FXML
    public void initialize() {
        contenedor.prefHeightProperty().bind(root.heightProperty().multiply(0.9));
        contenedor.prefWidthProperty().bind(root.widthProperty().multiply(1));

        buttonGrid.maxWidthProperty().bind(root.widthProperty().multiply(0.75));
        buttonGrid.maxHeightProperty().bind(root.heightProperty().multiply(0.75));

        buttonGrid.hgapProperty().bind(buttonGrid.maxWidthProperty().multiply(0.007));
        buttonGrid.vgapProperty().bind(buttonGrid.maxHeightProperty().multiply(0.02));

        buttonGrid.paddingProperty().bind(Bindings.createObjectBinding(() -> {
            double pad = buttonGrid.maxHeightProperty().get() * 0.12;
            return new Insets(pad, pad, pad, pad);
        }, buttonGrid.maxHeightProperty()));

        // Ajustar tamaño de cada botón y sus imágenes
        for (Node node : buttonGrid.getChildren()) {
            if (node instanceof Button btn) {
                btn.prefWidthProperty().bind(
                        buttonGrid.maxWidthProperty().divide(4).subtract(buttonGrid.hgapProperty())
                );
                btn.prefHeightProperty().bind(
                        buttonGrid.maxHeightProperty().divide(2).subtract(buttonGrid.vgapProperty())
                );
                if (btn.getGraphic() instanceof ImageView iv) {
                    iv.fitWidthProperty().bind(btn.widthProperty().multiply(0.4));
                    iv.fitHeightProperty().bind(btn.heightProperty().multiply(0.43));
                }
            }
        }
    }

    // Métodos de navegación
    @FXML
    public void ventanaProductos() {
        if (controladorPrincipal != null) {
            controladorPrincipal.cargarVista(EnumVistas.PRODUCTO);
        }
    }

    @FXML
    public void ventanaClave() {
        if (controladorPrincipal != null) {
            controladorPrincipal.cargarVista(EnumVistas.CLAVES);
        }
    }

    @FXML
    public void ventanaClientes() {
        if (controladorPrincipal != null) {
            controladorPrincipal.cargarVista(EnumVistas.CLIENTES);
        }
    }

    @FXML
    public void ventanaProveedores() {
        if (controladorPrincipal != null) {
            controladorPrincipal.cargarVista(EnumVistas.PROVEEDORES);
        }
    }

    @FXML
    public void ventanaSucursales() {
        if (controladorPrincipal != null) {
            controladorPrincipal.cargarVista(EnumVistas.SUCURSALES);
        }
    }

    @FXML
    public void ventanaEtiquetasMarcas() {
        if (controladorPrincipal != null) {
            controladorPrincipal.cargarVista(EnumVistas.CLASIFICACION);
        }
    }

    // Implementación de ControladorVista
    @Override
    public void setContentArea(StackPane contentArea) {
        this.contentArea = contentArea;
    }

    @Override
    public void setControladorPrincipal(VentanaPrincipal.controller.MainController controladorPrincipal) {
        this.controladorPrincipal = controladorPrincipal;
    }
}