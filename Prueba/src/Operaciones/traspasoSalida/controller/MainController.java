package Operaciones.traspasoSalida.controller;

import Compartido.controller.encabezadoController;
import Compartido.controller.navbarController;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.*;
import javafx.application.Platform;
import java.io.IOException;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.ComboBox;
import Operaciones.traspasoSalida.model.model;
import Operaciones.traspasoSalida.model.traspasoSalida;
import javafx.beans.value.ObservableValue;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;

public class MainController {

    @FXML private StackPane root;
    @FXML private BorderPane paneNavbar;
    @FXML private Label labelUsuario;
    @FXML private VBox navbar;
    @FXML private VBox contenedor;
    @FXML private Pane overlayPane;
    @FXML private VBox contenedorTabla;
    @FXML private TableView<traspasoSalida> contenidoTabla;
    @FXML private HBox contenedorBtnConfirmar;
    @FXML private HBox rootHBox;
    @FXML private Label lblEliminar;
    @FXML private ComboBox<String> buscador;
    @FXML private Label lblAgregar;
    @FXML private Label lblSucursal;
    @FXML private Region expansor;
    @FXML private TableColumn<traspasoSalida, Boolean> colSelect;
    @FXML private TableColumn<traspasoSalida, String> colClaveProduct;
    @FXML private TableColumn<traspasoSalida, String> colProducto;
    @FXML private TableColumn<traspasoSalida, String> colDescripcionProducto;
    @FXML private TableColumn<traspasoSalida, String> colLote;
    @FXML private TableColumn<traspasoSalida, String> colCaducidad;
    @FXML private TableColumn<traspasoSalida, String> colUbicacion;
    @FXML private TableColumn<traspasoSalida, String> colPrecioUnitario;
    @FXML private TableColumn<traspasoSalida, String> colPrecioIva;
    @FXML private TableColumn<traspasoSalida, String> colPrecioBruto;
    @FXML private TableColumn<traspasoSalida, String> colPrecioTotaal;

    @FXML private encabezadoController paneNavbarController;
    private final model model = new model();
    private ObservableList<String> sucursalesCache;
    private final ObservableList<String> sucursalesFiltradas = FXCollections.observableArrayList();
    private final ObservableList<traspasoSalida> itemsTraspaso = FXCollections.observableArrayList();
    private String sucursalSeleccionadaId;
    private boolean actualizandoSucursal = false;

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
            lblSucursal.setMinWidth(Region.USE_PREF_SIZE);

            // Tabla
            contenedorTabla.prefHeightProperty().bind(contenedor.heightProperty().multiply(0.81));
            contenidoTabla.prefHeightProperty().bind(contenedorTabla.heightProperty().multiply(0.9));
            contenedorBtnConfirmar.maxWidthProperty().bind(contenedor.widthProperty().multiply(0.95));

            paneNavbarController.setTitulo("Traspaso de Salida", "#ffffff");

        });
        configurarAutocompleteSucursales();
        configurarTabla();
    }

    private void configurarAutocompleteSucursales() {
        sucursalesCache = FXCollections.observableArrayList();
        buscador.setItems(sucursalesFiltradas);

        javafx.concurrent.Task<java.util.List<String>> task = new javafx.concurrent.Task<>() {
            @Override
            protected java.util.List<String> call() {
                return model.obtenerNombresSucursales();
            }

            @Override
            protected void succeeded() {
                java.util.List<String> resultado = getValue();
                sucursalesCache.setAll(resultado != null ? resultado : java.util.Collections.emptyList());
                sucursalesFiltradas.setAll(sucursalesCache);
            }

            @Override
            protected void failed() {
                sucursalesCache.clear();
                sucursalesFiltradas.clear();
            }
        };

        Thread hilo = new Thread(task);
        hilo.setDaemon(true);
        hilo.start();

        buscador.getEditor().textProperty().addListener((obs, oldText, newText) -> {
            if (actualizandoSucursal) {
                return;
            }
            actualizandoSucursal = true;
            try {
                String seleccionado = buscador.getValue();
                if (seleccionado != null && seleccionado.equals(newText)) {
                    return;
                }
                if (newText == null || newText.isBlank()) {
                    sucursalesFiltradas.setAll(sucursalesCache);
                    return;
                }

                ObservableList<String> filtrados = FXCollections.observableArrayList();
                for (String nombre : sucursalesCache) {
                    if (nombre.toLowerCase().contains(newText.toLowerCase())) {
                        filtrados.add(nombre);
                    }
                }

                java.util.List<String> nuevos = new java.util.ArrayList<>(filtrados);
                javafx.application.Platform.runLater(() -> {
                    sucursalesFiltradas.setAll(nuevos);
                    if (!nuevos.isEmpty() && buscador.isFocused()) {
                        buscador.show();
                    }
                });
            } finally {
                actualizandoSucursal = false;
            }
        });

        buscador.setOnShowing(event -> sucursalesFiltradas.setAll(sucursalesCache));

        buscador.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.isBlank()) {
                sucursalSeleccionadaId = model.obtenerIdSucursalPorNombre(newVal);
            } else {
                sucursalSeleccionadaId = null;
            }
        });
    }

    @FXML
    public void abrirTraspasoSalida() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Formularios/view/nuevoTraspasoSalida.fxml"));
            Formularios.controller.controllerNuevoTraspasoSalida controlador =
                    new Formularios.controller.controllerNuevoTraspasoSalida();
            controlador.setItemsTraspaso(itemsTraspaso);
            controlador.setMainController(this);
            loader.setController(controlador);

            Pane formulario = loader.load();

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Traspaso de salida");
            stage.setScene(new javafx.scene.Scene(formulario));
            stage.initOwner(root.getScene().getWindow());
            stage.setResizable(false);
            stage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void configurarTabla() {
        contenidoTabla.setItems(itemsTraspaso);

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
        colLote.setCellValueFactory(new PropertyValueFactory<>("lote"));
        colCaducidad.setCellValueFactory(new PropertyValueFactory<>("caducidad"));
        colUbicacion.setCellValueFactory(new PropertyValueFactory<>("ubicacionResumen"));
        colPrecioUnitario.setCellValueFactory(new PropertyValueFactory<>("precioEntrada"));
        colPrecioIva.setCellValueFactory(new PropertyValueFactory<>("precioIva"));
        colPrecioBruto.setCellValueFactory(new PropertyValueFactory<>("precioBruto"));
        colPrecioTotaal.setCellValueFactory(new PropertyValueFactory<>("precioTotal"));

        TableColumn<traspasoSalida, ?>[] columnas = new TableColumn[] {
                colSelect, colClaveProduct, colProducto, colDescripcionProducto, colLote,
                colCaducidad, colUbicacion, colPrecioUnitario, colPrecioIva, colPrecioBruto, colPrecioTotaal
        };

        for (TableColumn<traspasoSalida, ?> col : columnas) {
            col.setStyle("-fx-alignment: CENTER;");
        }
    }

    public void refrescarTabla() {
        contenidoTabla.refresh();
    }
}
