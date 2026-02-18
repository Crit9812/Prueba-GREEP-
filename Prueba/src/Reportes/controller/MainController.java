package Reportes.controller;

import Compartido.controller.encabezadoController;
import Compartido.controller.navbarController;
import Compartido.sesion.SesionUsuario;
import Compartido.sesion.PermisosRolHelper;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.Node;
import javafx.scene.image.ImageView;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import controllerInterfaz.ControllerInterfaz;
import java.io.IOException;

public class MainController {

    @FXML private StackPane root;
    @FXML private GridPane buttonGrid;
    @FXML private Button botonUtilidades;
    @FXML private BorderPane paneNavbar;
    @FXML private VBox navbar;
    @FXML private VBox contenedor;
    @FXML private Pane overlayPane;
    @FXML private encabezadoController paneNavbarController;

    @FXML
    public void initialize() {
        Platform.runLater(() -> {
            try {
                FXMLLoader overlayLoader = new FXMLLoader(getClass().getResource("/Compartido/view/label.fxml"));
                Pane overlay = overlayLoader.load();
                overlayPane = overlay; // asignas manualmente
                root.getChildren().add(overlay);
                // Cargar el navbar desde el fx:include
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/Compartido/view/navbar.fxml"));
                VBox navbarLoaded = loader.load();

                // Obtener el controller del navbar
                navbarController navbarCtrl = loader.getController();

                // Pasar el overlayPane al navbarController
                //navbarCtrl.setOverlayPane(overlayPane);
                if (overlayPane == null) {
                    Platform.runLater(() -> {
                        if (overlayPane == null) {
                            System.err.println("⚠ overlayPane no está inicializado");
                            return;
                        }
                        //navbarCtrl.setOverlayPane(overlayPane);
                    });
                } else {
                    navbarCtrl.setOverlayPane(overlayPane);
                }

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
                double padVertical = buttonGrid.maxHeightProperty().get() * 0.28;  // menos alto
                double padHorizontal = buttonGrid.maxWidthProperty().get() * 0.05; // más flexible a lo ancho
                return new Insets(padVertical, padHorizontal, padVertical, padHorizontal);
            }, buttonGrid.maxHeightProperty(), buttonGrid.maxWidthProperty()));



            // Ajustar tamaño de cada botón
            for (Node node : buttonGrid.getChildren()) {
                if (node instanceof javafx.scene.control.Button btn) {
                    btn.prefWidthProperty().bind(
                            Bindings.max(buttonGrid.maxWidthProperty().divide(4).subtract(buttonGrid.hgapProperty()), 100)
                    );
                    btn.prefHeightProperty().bind(
                            buttonGrid.maxHeightProperty().multiply(0.7)
                    );
                    // Ajustar ImageView dentro del botón
                    if (btn.getGraphic() instanceof ImageView iv) {
                        iv.fitWidthProperty().bind(btn.widthProperty().multiply(0.4));
                        iv.fitHeightProperty().bind(btn.heightProperty().multiply(0.43)
                        );
                    }
                }
            }

            aplicarRestriccionesPorRol();
            PermisosRolHelper.aplicarSoloLecturaEnModulo(root);

            paneNavbarController.setTitulo("Reportes", "#f0f0f0");
        });
    }

    private void aplicarRestriccionesPorRol() {
        if (SesionUsuario.esUsuario()) {
            ocultarBotonUtilidades();
        }
    }

    private void ocultarBotonUtilidades() {
        if (botonUtilidades == null) {
            return;
        }

        botonUtilidades.setVisible(false);
        botonUtilidades.setManaged(false);
        GridPane.setRowIndex(botonUtilidades, null);
        GridPane.setColumnIndex(botonUtilidades, null);

        for (Node node : buttonGrid.getChildren()) {
            if (!(node instanceof Button boton) || boton == botonUtilidades) {
                continue;
            }
            Integer fila = GridPane.getRowIndex(boton);
            Integer columna = GridPane.getColumnIndex(boton);
            int filaActual = fila == null ? 0 : fila;
            int columnaActual = columna == null ? 0 : columna;

            if (filaActual == 0 && columnaActual > 3) {
                GridPane.setColumnIndex(boton, columnaActual - 1);
            }
        }
    }

    @FXML
    public void ventanaHistorial() {
        Reportes.historial.controller.MainController controlador = new Reportes.historial.controller.MainController();
        ControllerInterfaz.cambiarVista("/Reportes/historial/view/main_view.fxml", "/Reportes/historial/style/estilos.css", controlador);
    }

    @FXML
    public void ventanaInventario() {
        Reportes.inventario.controller.MainController controlador = new Reportes.inventario.controller.MainController();
        ControllerInterfaz.cambiarVista("/Reportes/inventario/view/main_view.fxml", "/Reportes/inventario/style/estilos.css", controlador);
    }

    @FXML
    public void ventanaUtilidades() {
        Reportes.utilidades.controller.MainController controlador = new Reportes.utilidades.controller.MainController();
        ControllerInterfaz.cambiarVista("/Reportes/utilidades/view/main_view.fxml", "/Reportes/utilidades/style/estilos.css", controlador);
    }

    @FXML
    public void ventanaHistorialArticulo() {
        Reportes.historialArticulo.controller.MainController controlador = new Reportes.historialArticulo.controller.MainController();
        ControllerInterfaz.cambiarVista("/Reportes/historialArticulo/view/main_view.fxml", "/Reportes/historialArticulo/style/estilos.css", controlador);
    }

}
