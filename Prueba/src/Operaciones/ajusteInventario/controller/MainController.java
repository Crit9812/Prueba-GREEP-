package Operaciones.ajusteInventario.controller;

import Compartido.controller.encabezadoController;
import Compartido.controller.navbarController;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.application.Platform;
import java.io.IOException;

public class MainController {

    @FXML private StackPane root;
    @FXML private BorderPane paneNavbar;
    @FXML private VBox navbar;
    @FXML private VBox contenedor;
    @FXML private Pane overlayPane;
    @FXML private VBox contenedorTabla;
    @FXML private TableView contenidoTabla;
    @FXML private HBox contenedorBtnConfirmar;
    @FXML private HBox rootHBox;
    @FXML private Label lblEliminar;
    @FXML private Label lblAgregar;
    @FXML private Region expansor;

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
                navbarCtrl.setOverlayPane(overlayPane);

                // Reemplazar el contenido del fx:include con el cargado
                navbar.getChildren().setAll(navbarLoaded);

            } catch (IOException e) {
                e.printStackTrace();
            }

            SplitPane.setResizableWithParent(navbar, false);
            SplitPane.setResizableWithParent(contenedor, true);

            // Navbar superior (header)
            paneNavbar.prefHeightProperty().bind(root.heightProperty().multiply(0.1));
            paneNavbar.prefWidthProperty().bind(root.widthProperty().multiply(0.9));

            // Navbar lateral (menú)
            navbar.prefWidthProperty().bind(root.widthProperty().multiply(0.15));
            navbar.prefHeightProperty().bind(root.heightProperty().multiply(0.9));

            // Center - contenedor general
            contenedor.prefHeightProperty().bind(root.heightProperty().multiply(0.75));

            // Barra de opciones
            HBox.setHgrow(expansor, Priority.ALWAYS);
            expansor.setMinWidth(10);
            lblEliminar.setMinWidth(Region.USE_PREF_SIZE);
            lblAgregar.setMinWidth(Region.USE_PREF_SIZE);


            // Tabla
            contenedorTabla.prefHeightProperty().bind(contenedor.heightProperty().multiply(0.81));
            contenidoTabla.prefHeightProperty().bind(contenedorTabla.heightProperty().multiply(0.9));
            contenedorBtnConfirmar.maxWidthProperty().bind(contenedor.widthProperty().multiply(0.95));

            paneNavbarController.setTitulo("Ajuste de Inventario", "#ffffff");

        });
    }


    @FXML
    public void formularioNuevoCliente() {
        Formularios.controller.controllerNuevoAjuste controlador = new Formularios.controller.controllerNuevoAjuste();
        controllerFormularios.controllerFormulario.llamarFormulario("/Formularios/view/nuevoAjuste.fxml",controlador,"Ajuste");
    }
}
