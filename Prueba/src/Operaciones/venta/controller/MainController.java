package Operaciones.venta.controller;

import Compartido.controller.encabezadoController;
import Compartido.controller.navbarController;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ComboBox;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.application.Platform;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.io.IOException;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import Operaciones.venta.model.model;

public class MainController {

    @FXML private StackPane root;
    @FXML private BorderPane paneNavbar;
    @FXML private VBox navbar;
    @FXML private VBox contenedor;
    @FXML private Pane overlayPane;


    @FXML private VBox contenedorTabla;
    @FXML private TableView contenidoTabla;
    @FXML private HBox rootHBox;
    @FXML private Label lblEliminar;
    @FXML private ComboBox<String> buscador;
    @FXML private Label lblAgregar;
    @FXML private Label lblCliente;
    @FXML private Region expansor;
    @FXML private HBox contenedorComentario;
    @FXML private TextField comentario;
    @FXML private HBox contenedorBtnConfirmar;

    @FXML private encabezadoController paneNavbarController;
    private final model model = new model();
    private ObservableList<String> clientesCache;
    private final ObservableList<String> clientesFiltrados = FXCollections.observableArrayList();
    private String clienteSeleccionadoId;
    private boolean actualizandoCliente = false;


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
            lblAgregar.setMinWidth(Region.USE_PREF_SIZE);
            lblCliente.setMinWidth(Region.USE_PREF_SIZE);

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


            paneNavbarController.setTitulo("Venta", "#ffffff");

        });
        configurarAutocompleteClientes();
    }

    private void configurarAutocompleteClientes() {
        clientesCache = FXCollections.observableArrayList();
        buscador.setItems(clientesFiltrados);

        javafx.concurrent.Task<java.util.List<String>> task = new javafx.concurrent.Task<>() {
            @Override
            protected java.util.List<String> call() {
                return model.obtenerNombresClientes();
            }

            @Override
            protected void succeeded() {
                java.util.List<String> resultado = getValue();
                clientesCache.setAll(resultado != null ? resultado : java.util.Collections.emptyList());
                clientesFiltrados.setAll(clientesCache);
            }

            @Override
            protected void failed() {
                clientesCache.clear();
                clientesFiltrados.clear();
            }
        };

        Thread hilo = new Thread(task);
        hilo.setDaemon(true);
        hilo.start();

        buscador.getEditor().textProperty().addListener((obs, oldText, newText) -> {
            if (actualizandoCliente) {
                return;
            }
            actualizandoCliente = true;
            try {
                String seleccionado = buscador.getValue();
                if (seleccionado != null && seleccionado.equals(newText)) {
                    return;
                }
                if (newText == null || newText.isBlank()) {
                    clientesFiltrados.setAll(clientesCache);
                    return;
                }

                ObservableList<String> filtrados = FXCollections.observableArrayList();
                for (String nombre : clientesCache) {
                    if (nombre.toLowerCase().contains(newText.toLowerCase())) {
                        filtrados.add(nombre);
                    }
                }

                java.util.List<String> nuevos = new java.util.ArrayList<>(filtrados);
                javafx.application.Platform.runLater(() -> {
                    clientesFiltrados.setAll(nuevos);
                    if (!nuevos.isEmpty() && buscador.isFocused()) {
                        buscador.show();
                    }
                });
            } finally {
                actualizandoCliente = false;
            }
        });

        buscador.setOnShowing(event -> clientesFiltrados.setAll(clientesCache));

        buscador.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.isBlank()) {
                clienteSeleccionadoId = model.obtenerIdClientePorNombre(newVal);
            } else {
                clienteSeleccionadoId = null;
            }
        });
    }

    @FXML
    public void abrirNuevoCliente() {
        Formularios.controller.controllerNuevoCliente controlador = new Formularios.controller.controllerNuevoCliente();
        controllerFormularios.controllerFormulario.llamarFormulario("/Formularios/view/nuevoCliente.fxml",controlador,"Cliente");
    }

    @FXML
    public void abrirFormularioVenta() {
        Formularios.controller.controllerNuevaVenta controlador = new Formularios.controller.controllerNuevaVenta();
        controllerFormularios.controllerFormulario.llamarFormulario("/Formularios/view/nuevaVenta.fxml",controlador,"Venta");
    }
}
