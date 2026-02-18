package Operaciones.controller;

import Compartido.controller.encabezadoController;
import Compartido.controller.navbarController;
import Compartido.sesion.PermisosRol;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextField;
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
    @FXML private SplitPane splitpane;
    @FXML private Button btnAjusteInventario;
    @FXML private Button btnRegistrarUsuario;

    @FXML
    public void initialize() {
        Platform.runLater(() -> {

            SplitPane.setResizableWithParent(navbar, false);
            SplitPane.setResizableWithParent(contenedor, true);

            splitpane.requestLayout();
            splitpane.setDividerPositions(0.03);

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

            aplicarRestriccionesPorRol();
            paneNavbarController.setTitulo("Operaciones", "#f0f0f0");

        });
    }

    private void aplicarRestriccionesPorRol() {
        if (!PermisosRol.esAuxiliar() || btnAjusteInventario == null) {
            return;
        }

        btnAjusteInventario.setVisible(false);
        btnAjusteInventario.setManaged(false);
        if (btnRegistrarUsuario != null) {
            GridPane.setColumnIndex(btnRegistrarUsuario, 1);
        }
    }

    @FXML
    public void ventanaAjusteInventario() {
        if (PermisosRol.esAuxiliar()) {
            return;
        }
        Operaciones.ajusteInventario.controller.MainController controlador = new Operaciones.ajusteInventario.controller.MainController();
        ControllerInterfaz.cambiarVista("/Operaciones/ajusteInventario/view/main_view.fxml", "/Operaciones/ajusteInventario/style/estilos.css", controlador);
    }

    @FXML
    public void ventanaCompra() {
        Operaciones.compra.controller.MainController controlador = new Operaciones.compra.controller.MainController();
        ControllerInterfaz.cambiarVista("/Operaciones/compra/view/main_view.fxml", "/Operaciones/compra/style/estilos.css", controlador);
    }

    @FXML
    public void ventanaPedidos() {
        Operaciones.pedidos.controller.MainController controlador = new Operaciones.pedidos.controller.MainController();
        ControllerInterfaz.cambiarVista("/Operaciones/pedidos/view/main_view.fxml", "/Operaciones/pedidos/style/estilos.css", controlador);
    }

    @FXML
    public void ventanaRegistrarUsuario() {
        Operaciones.registrarUsuario.controller.MainController controlador = new Operaciones.registrarUsuario.controller.MainController();
        ControllerInterfaz.cambiarVista("/Operaciones/registrarUsuario/view/main_view.fxml", "/Operaciones/registrarUsuario/style/estilos.css", controlador);
    }

    @FXML
    public void ventanaTraspasoEntrada() {
        Operaciones.traspasoEntrada.controller.MainController controlador = new Operaciones.traspasoEntrada.controller.MainController();
        ControllerInterfaz.cambiarVista("/Operaciones/traspasoEntrada/view/main_view.fxml", "/Operaciones/traspasoEntrada/style/estilos.css", controlador);
    }

    @FXML
    public void ventanaTraspasoSalida() {
        Operaciones.traspasoSalida.controller.MainController controlador = new Operaciones.traspasoSalida.controller.MainController();
        ControllerInterfaz.cambiarVista("/Operaciones/traspasoSalida/view/main_view.fxml", "/Operaciones/traspasoSalida/style/estilos.css", controlador);
    }

    @FXML
    public void ventanaVenta() {
        Operaciones.venta.controller.MainController controlador = new Operaciones.venta.controller.MainController();
        ControllerInterfaz.cambiarVista("/Operaciones/venta/view/main_view.fxml", "/Operaciones/venta/style/estilos.css", controlador);
    }
}
