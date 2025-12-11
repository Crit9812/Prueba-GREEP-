package Consultas.controller;

import Compartido.controller.encabezadoController;
import Compartido.controller.navbarController;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.*;
import javafx.scene.Node;
import javafx.scene.image.ImageView;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import controllerInterfaz.ControllerInterfaz;
import java.io.IOException;

public class MainController {

    @FXML private StackPane root;
    @FXML private GridPane buttonGrid;
    @FXML private BorderPane paneNavbar;
    @FXML private VBox navbar;
    @FXML private VBox contenedor;
    @FXML private Pane overlayPane;
    @FXML private encabezadoController paneNavbarController;

    @FXML
    public void initialize() {
        Platform.runLater(() -> {

           try {
                // Cargar el navbar desde el fx:include
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/Compartido/view/navbar.fxml"));
                VBox navbarLoaded = loader.load();

                // Obtener el controller del navbar
                navbarController navbarCtrl = loader.getController();

                // Pasar el overlayPane al navbarController
                navbarCtrl.setOverlayPane(overlayPane);

                // Reemplazar el contenido del fx:include con el cargado
                navbar.getChildren().setAll(navbarLoaded);

            } catch (IOException e) {
                e.printStackTrace();
            }

            SplitPane.setResizableWithParent(navbar, false);
            SplitPane.setResizableWithParent(contenedor, true);

            //Navbar superior (header)
            paneNavbar.prefHeightProperty().bind(root.heightProperty().multiply(0.1));
            paneNavbar.prefWidthProperty().bind(root.widthProperty().multiply(0.9));

            // Navbar lateral (menú)
            navbar.prefWidthProperty().bind(root.widthProperty().multiply(0.15));
            navbar.prefHeightProperty().bind(root.heightProperty().multiply(0.9));

            // Center - contenedor general
            contenedor.prefHeightProperty().bind(root.heightProperty().multiply(0.75));
            contenedor.prefWidthProperty().bind(root.widthProperty().multiply(0.9));

            // Área central
            buttonGrid.maxWidthProperty().bind(root.widthProperty().multiply(0.73));
            buttonGrid.maxHeightProperty().bind(root.heightProperty().multiply(0.7));

            buttonGrid.hgapProperty().bind(buttonGrid.maxWidthProperty().multiply(0.007));
            buttonGrid.vgapProperty().bind(buttonGrid.maxHeightProperty().multiply(0.02));

            buttonGrid.paddingProperty().bind(Bindings.createObjectBinding(() -> {
                double pad = buttonGrid.maxHeightProperty().get() * 0.12;
                return new Insets(pad, pad, pad, pad);
            }, buttonGrid.maxHeightProperty()));

            // Ajustar tamaño de cada botón
            for (Node node : buttonGrid.getChildren()) {
                if (node instanceof javafx.scene.control.Button btn) {
                    btn.prefWidthProperty().bind(
                            Bindings.max(buttonGrid.maxWidthProperty().divide(4).subtract(buttonGrid.hgapProperty()), 100)
                    );
                    btn.prefHeightProperty().bind(
                            Bindings.max(buttonGrid.maxHeightProperty().divide(2).subtract(buttonGrid.vgapProperty()), 800)
                    );
                    // Ajustar ImageView dentro del botón
                    if (btn.getGraphic() instanceof ImageView iv) {
                        iv.fitWidthProperty().bind(btn.widthProperty().multiply(0.4));
                        iv.fitHeightProperty().bind(btn.heightProperty().multiply(0.43));
                    }
                }
            }

            paneNavbarController.setTitulo("Consultas", "#f0f0f0");

        });
    }

    @FXML
    public void ventanaProductos() {
        Consultas.producto.controller.MainController controlador = new Consultas.producto.controller.MainController();
        ControllerInterfaz.cambiarVista("/Consultas/producto/view/main_view.fxml", "/Consultas/producto/style/estilos.css", controlador);
    }

    @FXML
    public void ventanaClave() {
        Consultas.claves.controller.MainController controlador = new Consultas.claves.controller.MainController();
        ControllerInterfaz.cambiarVista("/Consultas/claves/view/main_view.fxml", "/Consultas/claves/style/estilos.css", controlador);
    }

    @FXML
    public void ventanaClientes() {
        Consultas.clientes.controller.MainController controlador = new Consultas.clientes.controller.MainController();
        ControllerInterfaz.cambiarVista("/Consultas/clientes/view/main_view.fxml", "/Consultas/clientes/style/estilos.css", controlador);
    }

    @FXML
    public void ventanaProveedores() {
        Consultas.proveedores.controller.MainController controlador = new Consultas.proveedores.controller.MainController();
        ControllerInterfaz.cambiarVista("/Consultas/proveedores/view/main_view.fxml", "/Consultas/proveedores/style/estilos.css", controlador);
    }

    @FXML
    public void ventanaSucursales() {
        Consultas.sucursales.controller.MainController controlador = new Consultas.sucursales.controller.MainController();
        ControllerInterfaz.cambiarVista("/Consultas/sucursales/view/main_view.fxml", "/Consultas/sucursales/style/estilos.css", controlador);
    }


}
