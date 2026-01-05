package Operaciones.venta.controller;

import Compartido.controller.encabezadoController;
import Compartido.controller.navbarController;
import Operaciones.venta.model.model;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import java.io.IOException;
import java.util.List;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.Callback;
import Operaciones.traspasoSalida.model.traspasoSalida;

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
    @FXML private CheckBox miCheckBox;

    @FXML private encabezadoController paneNavbarController;
    private final model model = new model();
    private ObservableList<String> clientesCache;
    private final ObservableList<String> clientesFiltrados = FXCollections.observableArrayList();
    private final ObservableList<traspasoSalida> itemsVenta = FXCollections.observableArrayList();
    private String clienteSeleccionadoId;
    private boolean actualizandoFiltroCliente = false;
    private boolean actualizandoSeleccion = false;
    @FXML private TableColumn<traspasoSalida, Boolean> colSelect;
    @FXML private TableColumn<traspasoSalida, String> colClaveProduct;
    @FXML private TableColumn<traspasoSalida, String> colProducto;
    @FXML private TableColumn<traspasoSalida, String> colDescripcionProducto;
    @FXML private TableColumn<traspasoSalida, String> colCantidad;
    @FXML private TableColumn<traspasoSalida, String> colLote;
    @FXML private TableColumn<traspasoSalida, String> colCaducidad;
    @FXML private TableColumn<traspasoSalida, String> colUbicacion;
    @FXML private TableColumn<traspasoSalida, String> colPrecioUnitario;
    @FXML private TableColumn<traspasoSalida, String> colPrecioIva;
    @FXML private TableColumn<traspasoSalida, String> colPrecioBruto;
    @FXML private TableColumn<traspasoSalida, String> colPrecioTotaal;


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
        configurarTabla();
    }

    private void configurarAutocompleteClientes() {
        clientesCache = FXCollections.observableArrayList();
        buscador.setItems(clientesFiltrados);

        Task<List<String>> task = new Task<>() {
            @Override
            protected List<String> call() {
                return model.obtenerNombresClientes();
            }

            @Override
            protected void succeeded() {
                List<String> resultado = getValue();
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
            if (actualizandoFiltroCliente) {
                return;
            }
            String seleccionado = buscador.getValue();
            if (seleccionado != null && seleccionado.equals(newText)) {
                return;
            }
            actualizandoFiltroCliente = true;
            try {
                String filtro = newText == null ? "" : newText.trim().toLowerCase();
                ObservableList<String> filtrados = FXCollections.observableArrayList();
                if (filtro.isEmpty()) {
                    filtrados.setAll(clientesCache);
                } else {
                    for (String nombre : clientesCache) {
                        if (nombre.toLowerCase().contains(filtro)) {
                            filtrados.add(nombre);
                        }
                    }
                }
                Platform.runLater(() -> {
                    clientesFiltrados.setAll(filtrados);
                    if (!clientesFiltrados.isEmpty() && buscador.isFocused()) {
                        buscador.show();
                    }
                });
            } finally {
                actualizandoFiltroCliente = false;
            }
        });

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
        controlador.setItemsVenta(itemsVenta);
        controlador.setMainController(this);
        controllerFormularios.controllerFormulario.llamarFormulario("/Formularios/view/nuevaVenta.fxml",controlador,"Venta");
    }

    private void configurarTabla() {
        if (contenidoTabla == null) {
            return;
        }
        contenidoTabla.setItems(itemsVenta);

        colSelect.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<traspasoSalida, Boolean>, ObservableValue<Boolean>>() {
            @Override
            public ObservableValue<Boolean> call(TableColumn.CellDataFeatures<traspasoSalida, Boolean> param) {
                traspasoSalida item = param.getValue();
                if (item != null) {
                    return item.seleccionadoProperty();
                }
                return new SimpleBooleanProperty(false);
            }
        });

        colSelect.setCellFactory(CheckBoxTableCell.forTableColumn(colSelect));
        colClaveProduct.setCellValueFactory(new PropertyValueFactory<>("claveProducto"));
        colProducto.setCellValueFactory(new PropertyValueFactory<>("producto"));
        colDescripcionProducto.setCellValueFactory(new PropertyValueFactory<>("descripcion"));
        colCantidad.setCellValueFactory(new PropertyValueFactory<>("cantidad"));
        colLote.setCellValueFactory(new PropertyValueFactory<>("lote"));
        colCaducidad.setCellValueFactory(new PropertyValueFactory<>("caducidad"));
        colUbicacion.setCellValueFactory(new PropertyValueFactory<>("ubicacionResumen"));
        colPrecioUnitario.setCellValueFactory(new PropertyValueFactory<>("precioEntrada"));
        colPrecioIva.setCellValueFactory(new PropertyValueFactory<>("precioIva"));
        colPrecioBruto.setCellValueFactory(new PropertyValueFactory<>("precioBruto"));
        colPrecioTotaal.setCellValueFactory(new PropertyValueFactory<>("precioTotal"));

        TableColumn<traspasoSalida, ?>[] columnas = new TableColumn[] {
                colSelect, colClaveProduct, colProducto, colDescripcionProducto, colCantidad, colLote,
                colCaducidad, colUbicacion, colPrecioUnitario, colPrecioIva, colPrecioBruto, colPrecioTotaal
        };

        for (TableColumn<traspasoSalida, ?> col : columnas) {
            col.setStyle("-fx-alignment: CENTER;");
        }

        contenidoTabla.setEditable(true);
        if (miCheckBox != null) {
            miCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
                if (actualizandoSeleccion) {
                    return;
                }
                actualizandoSeleccion = true;
                try {
                    for (traspasoSalida item : itemsVenta) {
                        item.setSeleccionado(newVal);
                    }
                } finally {
                    actualizandoSeleccion = false;
                }
            });
        }
    }

    public void refrescarTabla() {
        if (contenidoTabla != null) {
            contenidoTabla.refresh();
        }
    }
}
