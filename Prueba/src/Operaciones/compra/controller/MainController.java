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
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ComboBox;
import Operaciones.compra.model.model;
import Operaciones.compra.model.compra;
import javafx.scene.control.Alert;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.CheckBox;
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
    @FXML private HBox rootHBox;
    @FXML private Label lblEliminar;
    @FXML private ComboBox<String> buscador;
    @FXML private Label lblAgregar;
    @FXML private Label lblProveedores;
    @FXML private Region expansor;
    @FXML private VBox contenedorTabla;
    @FXML private TableView<compra> contenidoTabla;
    @FXML private HBox contenedorComentario;
    @FXML private TextField comentario;
    @FXML private HBox contenedorBtnConfirmar;
    @FXML private TextField factura;
    @FXML private CheckBox miCheckBox;

    @FXML private TableColumn<compra, Boolean> colSelect;
    @FXML private TableColumn<compra, String> colClaveProduct;
    @FXML private TableColumn<compra, String> colProducto;
    @FXML private TableColumn<compra, String> colDescripcionProducto;
    @FXML private TableColumn<compra, String> colLote;
    @FXML private TableColumn<compra, String> colCaducidad;
    @FXML private TableColumn<compra, String> colUbicacion;
    @FXML private TableColumn<compra, String> colPrecioUnitario;
    @FXML private TableColumn<compra, String> colPrecioIva;
    @FXML private TableColumn<compra, String> colPrecioBruto;
    @FXML private TableColumn<compra, String> colPrecioTotaal;

    @FXML private encabezadoController paneNavbarController;
    private final model model = new model();
    private ObservableList<String> proveedoresCache;
    private final ObservableList<compra> itemsCompra = FXCollections.observableArrayList();
    private String proveedorSeleccionadoId;



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
        configurarTabla();
        configurarSeleccionTodo();
        configurarBloqueoProveedor();

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

        buscador.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.isBlank()) {
                proveedorSeleccionadoId = model.obtenerIdProveedorPorNombre(newVal);
            } else {
                proveedorSeleccionadoId = null;
            }
        });
    }

    private void configurarTabla() {
        contenidoTabla.setItems(itemsCompra);

        colSelect.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<compra, Boolean>, ObservableValue<Boolean>>() {
            @Override
            public ObservableValue<Boolean> call(TableColumn.CellDataFeatures<compra, Boolean> param) {
                compra item = param.getValue();
                if (item != null) {
                    return item.seleccionadoProperty();
                } else {
                    return new SimpleBooleanProperty(false);
                }
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

        TableColumn<compra, ?>[] columnas = new TableColumn[]{
                colSelect, colClaveProduct, colProducto, colDescripcionProducto, colLote,
                colCaducidad, colUbicacion, colPrecioUnitario, colPrecioIva, colPrecioBruto, colPrecioTotaal
        };
        for (TableColumn<compra, ?> col : columnas) col.setStyle("-fx-alignment: CENTER;");
    }

    private void configurarSeleccionTodo() {
        if (miCheckBox == null) return;
        miCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            for (compra item : itemsCompra) {
                item.setSeleccionado(newVal);
            }
        });
    }

    private void configurarBloqueoProveedor() {
        buscador.setDisable(!itemsCompra.isEmpty());
        itemsCompra.addListener((javafx.collections.ListChangeListener<compra>) change -> {
            boolean bloquear = !itemsCompra.isEmpty();
            buscador.setDisable(bloquear);
        });
    }

    @FXML
    public void formularioNuevaCompra() {
        String proveedorNombre = obtenerProveedorSeleccionado();

        if (proveedorNombre == null || proveedorNombre.isBlank()) {
            mostrarAlerta("Advertencia", "Debe seleccionar un proveedor antes de agregar productos.");
            return;
        }

        String idProveedor = proveedorSeleccionadoId != null ? proveedorSeleccionadoId : model.obtenerIdProveedorPorNombre(proveedorNombre);

        if (idProveedor == null || idProveedor.isBlank()) {
            mostrarAlerta("Error", "No se encontró el proveedor seleccionado.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Formularios/view/compraEmergente.fxml"));
            Formularios.controller.controllerCompraEmergente controlador = new Formularios.controller.controllerCompraEmergente();
            controlador.setItemsCompra(itemsCompra);
            controlador.setMainController(this);
            controlador.setProveedorSeleccionado(idProveedor, proveedorNombre);
            loader.setController(controlador);

            Pane formulario = loader.load();

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Compra");
            stage.setScene(new javafx.scene.Scene(formulario));
            stage.initOwner(root.getScene().getWindow());
            stage.setResizable(false);
            stage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
            mostrarAlerta("Error", "No se pudo abrir el formulario de compra.");
        }
    }

    @FXML
    public void abrirNuevoProveedor() {
        Formularios.controller.controllerNuevoProveedor controlador = new Formularios.controller.controllerNuevoProveedor();
        controllerFormularios.controllerFormulario.llamarFormulario("/Formularios/view/nuevoProveedor.fxml",controlador,"Proveedor");
    }

    @FXML
    public void guardarCompras() {
        if (itemsCompra.isEmpty()) {
            mostrarAlerta("Advertencia", "No hay compras para registrar.");
            return;
        }

        String proveedorNombre = obtenerProveedorSeleccionado();
        if (proveedorNombre == null || proveedorNombre.isBlank()) {
            mostrarAlerta("Advertencia", "Debe seleccionar un proveedor.");
            return;
        }

        String idProveedor = proveedorSeleccionadoId != null ? proveedorSeleccionadoId : model.obtenerIdProveedorPorNombre(proveedorNombre);
        if (idProveedor == null || idProveedor.isBlank()) {
            mostrarAlerta("Error", "No se encontró el proveedor seleccionado.");
            return;
        }

        String numeroFactura = factura != null ? factura.getText().trim() : "";
        if (numeroFactura.isEmpty()) {
            mostrarAlerta("Advertencia", "Debe capturar el número de factura antes de confirmar.");
            return;
        }

        String comentarioTexto = comentario != null ? comentario.getText().trim() : "";

        boolean registrado = model.registrarCompra(idProveedor, numeroFactura, comentarioTexto, itemsCompra);
        if (registrado) {
            mostrarAlerta("Éxito", "Compra registrada correctamente.");
            itemsCompra.clear();
            if (factura != null) factura.clear();
            if (comentario != null) comentario.clear();
            proveedorSeleccionadoId = null;
            buscador.setDisable(false);
        } else {
            mostrarAlerta("Error", "No se pudo registrar la compra.");
        }
    }

    public void refrescarTabla() {
        contenidoTabla.refresh();
    }

    private String obtenerProveedorSeleccionado() {
        String valor = buscador.getValue();
        if (valor == null || valor.isBlank()) {
            valor = buscador.getEditor().getText();
        }
        return valor != null ? valor.trim() : "";
    }

    private void mostrarAlerta(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}
