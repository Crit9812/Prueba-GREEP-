package Operaciones.compra.controller;

import Compartido.controller.encabezadoController;
import Compartido.controller.navbarController;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import javafx.application.Platform;
import java.io.IOException;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ComboBox;
import Operaciones.compra.model.model;


public class MainController {

    @FXML private StackPane root;
    @FXML private BorderPane paneNavbar;
    @FXML private Label labelUsuario;
    @FXML private VBox navbar;
    @FXML private VBox contenedor;
    @FXML private Pane overlayPane;
    @FXML private HBox rootHBox;
    @FXML private Label lblEliminar;
    @FXML private ComboBox<String> buscador;
    @FXML private Label lblAgregar;
    @FXML private Label lblProveedores;
    @FXML private Region expansor;
    @FXML private VBox contenedorTabla;
    @FXML private TableView contenidoTabla;
    @FXML private HBox contenedorComentario;
    @FXML private TextField comentario;
    @FXML private HBox contenedorBtnConfirmar;

    @FXML private encabezadoController paneNavbarController;
    private final model model = new model();
    private ObservableList<String> proveedoresCache;



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

            // Barra de opciones
            HBox.setHgrow(expansor, Priority.ALWAYS);
            expansor.setMinWidth(10);
            buscador.prefWidthProperty().bind(rootHBox.widthProperty().multiply(0.2));
            lblEliminar.setMinWidth(Region.USE_PREF_SIZE);
            lblAgregar.setMinWidth(Region.USE_PREF_SIZE);
            lblProveedores.setMinWidth(Region.USE_PREF_SIZE);

            // Tabla
            contenedorTabla.prefHeightProperty().bind(contenedor.heightProperty().multiply(0.73));
            contenidoTabla.prefHeightProperty().bind(contenedorTabla.heightProperty().multiply(0.9));

            // Comentario
            contenedorComentario.maxWidthProperty().bind(contenedor.widthProperty());
            HBox.setHgrow(comentario, Priority.ALWAYS);
            comentario.setMaxWidth(Double.MAX_VALUE);

            // Botón confirmar
            contenedorBtnConfirmar.setMinWidth(Region.USE_PREF_SIZE);
            contenedorBtnConfirmar.setMaxWidth(Region.USE_PREF_SIZE);
            HBox.setHgrow(contenedorBtnConfirmar, Priority.NEVER);

            paneNavbarController.setTitulo("Compra", "#ffffff");
        });
        configurarAutocompleteProveedores();

    }

    private void configurarAutocompleteProveedores() {

        proveedoresCache = FXCollections.observableArrayList(
                model.obtenerNombresProveedores()
        );

        buscador.setItems(proveedoresCache);

        buscador.getEditor().textProperty().addListener((obs, oldText, newText) -> {

            if (newText == null || newText.isEmpty()) {
                buscador.hide();
                buscador.setItems(proveedoresCache);
                return;
            }

            ObservableList<String> filtrados = FXCollections.observableArrayList();

            for (String nombre : proveedoresCache) {
                if (nombre.toLowerCase().contains(newText.toLowerCase())) {
                    filtrados.add(nombre);
                }
            }

            buscador.setItems(filtrados);

            if (!filtrados.isEmpty()) {
                buscador.show();
            } else {
                buscador.hide();
            }
        });
    }


    @FXML
    public void formularioNuevaCompra() {
        Formularios.controller.controllerCompraEmergente controlador = new Formularios.controller.controllerCompraEmergente();
        controllerFormularios.controllerFormulario.llamarFormulario("/Formularios/view/compraEmergente.fxml",controlador,"Compra");
    }

    @FXML
    public void abrirNuevoProveedor() {
        Formularios.controller.controllerNuevoProveedor controlador = new Formularios.controller.controllerNuevoProveedor();
        controllerFormularios.controllerFormulario.llamarFormulario("/Formularios/view/nuevoProveedor.fxml",controlador,"Proveedor");
    }
}
