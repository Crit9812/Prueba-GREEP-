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
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
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
    @FXML private Button btnEliminar;
    @FXML private ComboBox<String> buscador;
    @FXML private Label lblAgregar;
    @FXML private Label lblCliente;
    @FXML private Region expansor;
    @FXML private HBox contenedorComentario;
    @FXML private TextField comentario;
    @FXML private HBox contenedorBtnConfirmar;
    @FXML private TextField factura;
    @FXML private TextField totalVenta;
    @FXML private Button botonConfirmar;
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
        configurarConfirmacion();
        configurarTotalVenta();
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

    private void configurarConfirmacion() {
        if (botonConfirmar != null) {
            botonConfirmar.setOnAction(event -> confirmarVenta());
        }
    }

    private void confirmarVenta() {
        if (itemsVenta.isEmpty()) {
            mostrarAlerta("Advertencia", "Debe agregar al menos un producto para confirmar la venta.");
            return;
        }
        if (clienteSeleccionadoId == null || clienteSeleccionadoId.isBlank()) {
            mostrarAlerta("Advertencia", "Debe seleccionar un cliente.");
            return;
        }
        String facturaTexto = factura != null ? factura.getText().trim() : "";
        if (facturaTexto.isBlank()) {
            mostrarAlerta("Advertencia", "Debe capturar el número de factura.");
            return;
        }

        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Confirmación");
        confirmacion.setHeaderText("Se está realizando una venta y una salida de productos de tu inventario.");
        confirmacion.setContentText("¿Deseas continuar con el registro?");
        confirmacion.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                String nota = comentario != null ? comentario.getText() : "";
                boolean registrado = model.registrarVenta(
                        clienteSeleccionadoId,
                        nota,
                        facturaTexto,
                        new ArrayList<>(itemsVenta)
                );
                if (registrado) {
                    itemsVenta.clear();
                    if (comentario != null) {
                        comentario.clear();
                    }
                    if (factura != null) {
                        factura.clear();
                    }
                    mostrarAlerta("Éxito", "La venta se registró correctamente.");
                } else {
                    mostrarAlerta("Error", "No se pudo registrar la venta.");
                }
            }
        });
    }

    private void mostrarAlerta(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
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

        itemsVenta.addListener((javafx.collections.ListChangeListener<traspasoSalida>) change -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    for (traspasoSalida item : change.getAddedSubList()) {
                        item.seleccionadoProperty().addListener((obs, oldVal, newVal) -> actualizarSeleccionGeneral());
                        if (miCheckBox != null && miCheckBox.isSelected() && !item.isSeleccionado()) {
                            item.setSeleccionado(true);
                        }
                    }
                }
                if (change.wasRemoved()) {
                    actualizarSeleccionGeneral();
                }
            }
        });

        for (traspasoSalida item : itemsVenta) {
            item.seleccionadoProperty().addListener((obs, oldVal, newVal) -> actualizarSeleccionGeneral());
        }
    }

    private void configurarTotalVenta() {
        if (totalVenta != null) {
            totalVenta.setEditable(false);
            totalVenta.setText("0.00");
        }

        itemsVenta.addListener((javafx.collections.ListChangeListener<traspasoSalida>) change -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    for (traspasoSalida item : change.getAddedSubList()) {
                        item.precioTotalProperty().addListener((obs, oldVal, newVal) -> actualizarTotalVenta());
                    }
                }
                if (change.wasRemoved()) {
                    actualizarTotalVenta();
                }
            }
        });

        actualizarTotalVenta();
    }

    private void actualizarTotalVenta() {
        if (totalVenta == null) {
            return;
        }
        java.math.BigDecimal total = java.math.BigDecimal.ZERO;
        java.util.List<traspasoSalida> filas = contenidoTabla != null
                ? (java.util.List<traspasoSalida>) contenidoTabla.getItems()
                : itemsVenta;
        for (traspasoSalida item : filas) {
            total = total.add(parseTotal(item != null ? item.getPrecioTotal() : null));
        }
        totalVenta.setText(total.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString());
    }

    private java.math.BigDecimal parseTotal(String valor) {
        if (valor == null || valor.isBlank()) {
            return java.math.BigDecimal.ZERO;
        }
        String limpio = valor.trim().replace(",", "");
        limpio = limpio.replaceAll("[^0-9.\\-]", "");
        if (limpio.isBlank() || "-".equals(limpio)) {
            return java.math.BigDecimal.ZERO;
        }
        try {
            return new java.math.BigDecimal(limpio);
        } catch (NumberFormatException e) {
            return java.math.BigDecimal.ZERO;
        }
    }

    public void refrescarTabla() {
        if (contenidoTabla != null) {
            contenidoTabla.refresh();
        }
    }

    @FXML
    public void eliminarSeleccionados() {
        List<traspasoSalida> seleccionados = itemsVenta.stream()
                .filter(traspasoSalida::isSeleccionado)
                .collect(Collectors.toList());
        if (seleccionados.isEmpty()) {
            return;
        }
        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Confirmación");
        confirmacion.setHeaderText("¿Deseas eliminar las ventas seleccionadas?");
        confirmacion.setContentText("Esta acción eliminará los elementos seleccionados de la venta.");
        confirmacion.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                itemsVenta.removeAll(seleccionados);
                actualizarSeleccionGeneral();
            }
        });
    }

    private void actualizarSeleccionGeneral() {
        if (miCheckBox == null || actualizandoSeleccion) {
            return;
        }
        actualizandoSeleccion = true;
        try {
            boolean seleccionarTodo = !itemsVenta.isEmpty()
                    && itemsVenta.stream().allMatch(traspasoSalida::isSeleccionado);
            miCheckBox.setSelected(seleccionarTodo);
        } finally {
            actualizandoSeleccion = false;
        }
    }
}
