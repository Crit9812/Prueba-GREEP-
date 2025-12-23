package Operaciones.compra.controller;

import Compartido.controller.encabezadoController;
import Compartido.controller.navbarController;
import Compartido.model.ProductoComboItem;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.application.Platform;
import java.io.IOException;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ListChangeListener;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import Operaciones.compra.model.itemCompra;
import Operaciones.compra.model.model;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableValue;
import javafx.util.Callback;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


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
    @FXML private TableView<itemCompra> contenidoTabla;
    @FXML private HBox contenedorComentario;
    @FXML private TextField comentario;
    @FXML private HBox contenedorBtnConfirmar;
    @FXML private TextField factura;
    @FXML private Button botonConfirmar;
    @FXML private CheckBox miCheckBox;

    @FXML private TableColumn<itemCompra, Boolean> colSelect;
    @FXML private TableColumn<itemCompra, String> colClaveProduct;
    @FXML private TableColumn<itemCompra, String> colClaveAlterna;
    @FXML private TableColumn<itemCompra, String> colProducto;
    @FXML private TableColumn<itemCompra, String> colDescripcionProducto;
    @FXML private TableColumn<itemCompra, Integer> colCantidad;
    @FXML private TableColumn<itemCompra, String> colPresentacion;
    @FXML private TableColumn<itemCompra, String> colFactor;
    @FXML private TableColumn<itemCompra, String> colLote;
    @FXML private TableColumn<itemCompra, String> colCaducidad;
    @FXML private TableColumn<itemCompra, String> colUbicacion;
    @FXML private TableColumn<itemCompra, Double> colPrecioUnitario;
    @FXML private TableColumn<itemCompra, Double> colPrecioIva;
    @FXML private TableColumn<itemCompra, Double> colPrecioBruto;
    @FXML private TableColumn<itemCompra, Double> colPrecioTotaal;

    @FXML private encabezadoController paneNavbarController;
    private final model model = new model();
    private ObservableList<String> proveedoresCache;
    private final ObservableList<itemCompra> itemsCompra = FXCollections.observableArrayList();
    private final Map<String, Integer> proveedorNombreToId = new HashMap<>();
    private final Map<Integer, String> proveedorIdToNombre = new HashMap<>();



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

        List<Map<String, Object>> proveedores = model.obtenerProveedores();
        proveedoresCache = FXCollections.observableArrayList();
        proveedorNombreToId.clear();
        proveedorIdToNombre.clear();

        for (Map<String, Object> proveedor : proveedores) {
            int id = ((Number) proveedor.get("id")).intValue();
            String nombre = String.valueOf(proveedor.get("nombre"));
            proveedoresCache.add(nombre);
            proveedorNombreToId.put(nombre, id);
            proveedorIdToNombre.put(id, nombre);
        }

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

        buscador.setOnAction(e -> validarProveedorSeleccionado());
        buscador.getEditor().focusedProperty().addListener((obs, was, isNow) -> {
            if (!isNow) validarProveedorSeleccionado();
        });
    }

    private void configurarTabla() {
        colSelect.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<itemCompra, Boolean>, ObservableValue<Boolean>>() {
            @Override
            public ObservableValue<Boolean> call(TableColumn.CellDataFeatures<itemCompra, Boolean> param) {
                itemCompra item = param.getValue();
                if (item != null) {
                    return item.seleccionadoProperty();
                }
                return new SimpleBooleanProperty(false);
            }
        });

        colSelect.setCellFactory(CheckBoxTableCell.forTableColumn(colSelect));
        colClaveProduct.setCellValueFactory(new PropertyValueFactory<>("claveProducto"));
        colClaveAlterna.setCellValueFactory(new PropertyValueFactory<>("claveAlterna"));
        colProducto.setCellValueFactory(new PropertyValueFactory<>("producto"));
        colDescripcionProducto.setCellValueFactory(new PropertyValueFactory<>("descripcion"));
        colCantidad.setCellValueFactory(new PropertyValueFactory<>("cantidad"));
        colPresentacion.setCellValueFactory(new PropertyValueFactory<>("presentacion"));
        colFactor.setCellValueFactory(new PropertyValueFactory<>("factor"));
        colLote.setCellValueFactory(new PropertyValueFactory<>("lote"));
        colCaducidad.setCellValueFactory(new PropertyValueFactory<>("caducidad"));
        colUbicacion.setCellValueFactory(new PropertyValueFactory<>("ubicacion"));
        colPrecioUnitario.setCellValueFactory(new PropertyValueFactory<>("precioEntrada"));
        colPrecioIva.setCellValueFactory(new PropertyValueFactory<>("precioIva"));
        colPrecioBruto.setCellValueFactory(new PropertyValueFactory<>("precioBruto"));
        colPrecioTotaal.setCellValueFactory(new PropertyValueFactory<>("precioTotal"));

        contenidoTabla.setItems(itemsCompra);

        TableColumn<itemCompra, ?>[] columnas = new TableColumn[]{
                colSelect, colClaveProduct, colClaveAlterna, colProducto, colDescripcionProducto,
                colCantidad, colPresentacion, colFactor, colLote, colCaducidad, colUbicacion,
                colPrecioUnitario, colPrecioIva, colPrecioBruto, colPrecioTotaal
        };

        for (TableColumn<itemCompra, ?> col : columnas) {
            col.setStyle("-fx-alignment: CENTER;");
        }
    }

    private void configurarSeleccionTodo() {
        if (miCheckBox == null) return;
        miCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            for (itemCompra item : itemsCompra) {
                item.setSeleccionado(newVal);
            }
        });
    }

    private void configurarBloqueoProveedor() {
        itemsCompra.addListener((ListChangeListener<itemCompra>) change -> {
            boolean bloquear = !itemsCompra.isEmpty();
            buscador.setDisable(bloquear);
        });
    }

    private Integer obtenerProveedorIdSeleccionado() {
        String nombre = buscador.getEditor().getText();
        if (nombre == null || nombre.isBlank()) {
            return null;
        }
        Integer id = proveedorNombreToId.get(nombre.trim());
        if (id == null) {
            mostrarAlerta("Proveedor no encontrado", "Seleccione un proveedor válido antes de continuar.");
        }
        return id;
    }

    private void validarProveedorSeleccionado() {
        String nombre = buscador.getEditor().getText();
        if (nombre == null || nombre.isBlank()) return;
        if (!proveedorNombreToId.containsKey(nombre.trim())) {
            mostrarAlerta("Proveedor no encontrado", "El proveedor seleccionado no existe.");
        }
    }

    @FXML
    public void formularioNuevaCompra() {
        Integer proveedorId = obtenerProveedorIdSeleccionado();
        if (proveedorId == null) {
            return;
        }

        List<ProductoComboItem> productos = model.obtenerProductosPorProveedor(proveedorId);
        if (productos.isEmpty()) {
            mostrarAlerta("Sin productos", "No hay productos asociados al proveedor seleccionado.");
            return;
        }

        Formularios.controller.controllerCompraEmergente controlador = new Formularios.controller.controllerCompraEmergente();
        controlador.setItemsCompra(itemsCompra);
        controlador.setMainController(this);
        controlador.setProveedorSeleccionado(proveedorId);
        controlador.setProductosDisponibles(productos);
        controllerFormularios.controllerFormulario.llamarFormulario("/Formularios/view/compraEmergente.fxml",controlador,"Compra");
    }

    @FXML
    public void abrirNuevoProveedor() {
        Formularios.controller.controllerNuevoProveedor controlador = new Formularios.controller.controllerNuevoProveedor();
        controllerFormularios.controllerFormulario.llamarFormulario("/Formularios/view/nuevoProveedor.fxml",controlador,"Proveedor");
    }

    @FXML
    public void guardarCompras() {
        Integer proveedorId = obtenerProveedorIdSeleccionado();
        if (proveedorId == null) return;

        if (itemsCompra.isEmpty()) {
            mostrarAlerta("Sin productos", "Agrega productos antes de guardar la compra.");
            return;
        }

        String facturaNumero = factura.getText();
        if (facturaNumero == null || facturaNumero.isBlank()) {
            mostrarAlerta("Factura requerida", "Ingrese el número de factura antes de guardar.");
            return;
        }

        String comentarioTexto = comentario.getText();
        boolean guardado = model.guardarCompras(proveedorId, facturaNumero.trim(), comentarioTexto == null ? "" : comentarioTexto.trim(), itemsCompra);
        if (guardado) {
            mostrarAlerta("Éxito", "La compra se guardó correctamente.");
            itemsCompra.clear();
            factura.clear();
            comentario.clear();
        } else {
            mostrarAlerta("Error", "No se pudo guardar la compra.");
        }
    }

    public void refrescarTabla() {
        contenidoTabla.refresh();
    }

    private void mostrarAlerta(String titulo, String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.INFORMATION);
        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }
}
