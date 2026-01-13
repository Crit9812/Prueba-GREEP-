package Operaciones.ajusteInventario.controller;

import Compartido.controller.encabezadoController;
import Compartido.controller.navbarController;
import Operaciones.compra.model.compra;
import Operaciones.traspasoSalida.model.traspasoSalida;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ListChangeListener;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.layout.*;
import javafx.application.Platform;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class MainController {

    @FXML private StackPane root;
    @FXML private BorderPane paneNavbar;
    @FXML private VBox navbar;
    @FXML private VBox contenedor;
    @FXML private Pane overlayPane;
    @FXML private VBox contenedorTabla;
    @FXML private TableView<Object> contenidoTabla;
    @FXML private HBox contenedorBtnConfirmar;
    @FXML private HBox rootHBox;
    @FXML private Label lblEliminar;
    @FXML private Label lblAgregar;
    @FXML private Region expansor;
    @FXML private TextField totalAjuste;
    @FXML private CheckBox miCheckBox;

    @FXML private encabezadoController paneNavbarController;

    @FXML private TableColumn<Object, Boolean> colSelect;
    @FXML private TableColumn<Object, String> colTipo;
    @FXML private TableColumn<Object, String> colClaveProduct;
    @FXML private TableColumn<Object, String> colProducto;
    @FXML private TableColumn<Object, String> colDescripcionProducto;
    @FXML private TableColumn<Object, Number> colCantidad;
    @FXML private TableColumn<Object, String> colLote;
    @FXML private TableColumn<Object, String> colCaducidad;
    @FXML private TableColumn<Object, String> colUbicacion;
    @FXML private TableColumn<Object, String> colNota;
    @FXML private TableColumn<Object, String> colPrecioUnitario;
    @FXML private TableColumn<Object, String> colPrecioIva;
    @FXML private TableColumn<Object, String> colPrecioBruto;
    @FXML private TableColumn<Object, String> colPrecioTotaal;

    private final ObservableList<compra> itemsEntrada = FXCollections.observableArrayList();
    private final ObservableList<traspasoSalida> itemsSalida = FXCollections.observableArrayList();
    private final ObservableList<Object> itemsAjuste = FXCollections.observableArrayList();
    private boolean actualizandoSeleccionTodo = false;

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
        configurarTabla();
        configurarListeners();
        configurarSeleccionTodo();
        configurarTotalAjuste();
    }

    private void configurarTabla() {
        contenidoTabla.setItems(itemsAjuste);
        contenidoTabla.setEditable(true);

        colSelect.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<Object, Boolean>, ObservableValue<Boolean>>() {
            @Override
            public ObservableValue<Boolean> call(TableColumn.CellDataFeatures<Object, Boolean> param) {
                Object item = param.getValue();
                if (item instanceof compra) {
                    return ((compra) item).seleccionadoProperty();
                }
                if (item instanceof traspasoSalida) {
                    return ((traspasoSalida) item).seleccionadoProperty();
                }
                return new SimpleBooleanProperty(false);
            }
        });
        colSelect.setCellFactory(CheckBoxTableCell.forTableColumn(colSelect));
        colSelect.setEditable(true);

        colTipo.setCellValueFactory(param -> {
            Object item = param.getValue();
            if (item instanceof compra) {
                return new SimpleStringProperty("Entrada");
            }
            if (item instanceof traspasoSalida) {
                return new SimpleStringProperty("Salida");
            }
            return new SimpleStringProperty("");
        });

        colClaveProduct.setCellValueFactory(param -> {
            Object item = param.getValue();
            if (item instanceof compra) {
                return ((compra) item).claveProductoProperty();
            }
            if (item instanceof traspasoSalida) {
                return ((traspasoSalida) item).claveProductoProperty();
            }
            return new SimpleStringProperty("");
        });
        colProducto.setCellValueFactory(param -> {
            Object item = param.getValue();
            if (item instanceof compra) {
                return ((compra) item).productoProperty();
            }
            if (item instanceof traspasoSalida) {
                return ((traspasoSalida) item).productoProperty();
            }
            return new SimpleStringProperty("");
        });
        colDescripcionProducto.setCellValueFactory(param -> {
            Object item = param.getValue();
            if (item instanceof compra) {
                return ((compra) item).descripcionProperty();
            }
            if (item instanceof traspasoSalida) {
                return ((traspasoSalida) item).descripcionProperty();
            }
            return new SimpleStringProperty("");
        });
        colCantidad.setCellValueFactory(param -> {
            Object item = param.getValue();
            if (item instanceof compra) {
                return ((compra) item).cantidadProperty();
            }
            if (item instanceof traspasoSalida) {
                return ((traspasoSalida) item).cantidadProperty();
            }
            return new SimpleIntegerProperty(0);
        });
        colLote.setCellValueFactory(param -> {
            Object item = param.getValue();
            if (item instanceof compra) {
                return ((compra) item).loteProperty();
            }
            if (item instanceof traspasoSalida) {
                return ((traspasoSalida) item).loteProperty();
            }
            return new SimpleStringProperty("");
        });
        colCaducidad.setCellValueFactory(param -> {
            Object item = param.getValue();
            if (item instanceof compra) {
                return ((compra) item).caducidadProperty();
            }
            if (item instanceof traspasoSalida) {
                return ((traspasoSalida) item).caducidadProperty();
            }
            return new SimpleStringProperty("");
        });
        colUbicacion.setCellValueFactory(param -> {
            Object item = param.getValue();
            if (item instanceof compra) {
                return ((compra) item).ubicacionResumenProperty();
            }
            if (item instanceof traspasoSalida) {
                return ((traspasoSalida) item).ubicacionResumenProperty();
            }
            return new SimpleStringProperty("");
        });
        colNota.setCellValueFactory(param -> {
            Object item = param.getValue();
            if (item instanceof compra) {
                return ((compra) item).notaProperty();
            }
            if (item instanceof traspasoSalida) {
                return ((traspasoSalida) item).notaProperty();
            }
            return new SimpleStringProperty("");
        });
        colPrecioUnitario.setCellValueFactory(param -> {
            Object item = param.getValue();
            if (item instanceof compra) {
                return ((compra) item).precioEntradaProperty();
            }
            if (item instanceof traspasoSalida) {
                return ((traspasoSalida) item).precioEntradaProperty();
            }
            return new SimpleStringProperty("");
        });
        colPrecioIva.setCellValueFactory(param -> {
            Object item = param.getValue();
            if (item instanceof compra) {
                return ((compra) item).precioIvaProperty();
            }
            if (item instanceof traspasoSalida) {
                return ((traspasoSalida) item).precioIvaProperty();
            }
            return new SimpleStringProperty("");
        });
        colPrecioBruto.setCellValueFactory(param -> {
            Object item = param.getValue();
            if (item instanceof compra) {
                return ((compra) item).precioBrutoProperty();
            }
            if (item instanceof traspasoSalida) {
                return ((traspasoSalida) item).precioBrutoProperty();
            }
            return new SimpleStringProperty("");
        });
        colPrecioTotaal.setCellValueFactory(param -> {
            Object item = param.getValue();
            if (item instanceof compra) {
                return ((compra) item).precioTotalProperty();
            }
            if (item instanceof traspasoSalida) {
                return ((traspasoSalida) item).precioTotalProperty();
            }
            return new SimpleStringProperty("");
        });
    }

    private void configurarListeners() {
        itemsEntrada.addListener((ListChangeListener<compra>) change -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    for (compra item : change.getAddedSubList()) {
                        item.precioTotalProperty().addListener((obs, oldVal, newVal) -> actualizarTotalAjuste());
                        item.seleccionadoProperty().addListener((obs, oldVal, newVal) -> actualizarSeleccionTodo());
                    }
                }
            }
            refrescarTabla();
        });

        itemsSalida.addListener((ListChangeListener<traspasoSalida>) change -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    for (traspasoSalida item : change.getAddedSubList()) {
                        item.precioTotalProperty().addListener((obs, oldVal, newVal) -> actualizarTotalAjuste());
                        item.seleccionadoProperty().addListener((obs, oldVal, newVal) -> actualizarSeleccionTodo());
                    }
                }
            }
            refrescarTabla();
        });
    }

    private void configurarSeleccionTodo() {
        if (miCheckBox == null) {
            return;
        }
        miCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (actualizandoSeleccionTodo) {
                return;
            }
            for (compra item : itemsEntrada) {
                item.setSeleccionado(newVal);
            }
            for (traspasoSalida item : itemsSalida) {
                item.setSeleccionado(newVal);
            }
            contenidoTabla.refresh();
        });
        actualizarSeleccionTodo();
    }

    public void refrescarTabla() {
        itemsAjuste.setAll(itemsEntrada);
        itemsAjuste.addAll(itemsSalida);
        contenidoTabla.refresh();
        actualizarTotalAjuste();
    }

    private void configurarTotalAjuste() {
        if (totalAjuste != null) {
            totalAjuste.setEditable(false);
            totalAjuste.setText("0.00");
            totalAjuste.setStyle("");
        }
        actualizarTotalAjuste();
    }

    private void actualizarTotalAjuste() {
        if (totalAjuste == null) {
            return;
        }
        BigDecimal totalEntradas = BigDecimal.ZERO;
        for (compra item : itemsEntrada) {
            totalEntradas = totalEntradas.add(parseDecimal(item.getPrecioTotal()));
        }
        BigDecimal totalSalidas = BigDecimal.ZERO;
        for (traspasoSalida item : itemsSalida) {
            totalSalidas = totalSalidas.add(parseDecimal(item.getPrecioTotal()));
        }
        BigDecimal total = totalEntradas.subtract(totalSalidas);
        totalAjuste.setText(total.setScale(2, RoundingMode.HALF_UP).toPlainString());
        if (total.compareTo(BigDecimal.ZERO) < 0) {
            totalAjuste.setStyle("-fx-text-fill: #d32f2f;");
        } else {
            totalAjuste.setStyle("");
        }
    }

    private BigDecimal parseDecimal(String valor) {
        if (valor == null) {
            return BigDecimal.ZERO;
        }
        String limpio = valor.replace(",", "").trim();
        if (limpio.isBlank()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(limpio);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    @FXML
    public void abrirFormularioAgregar() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Formularios/view/compraEmergente.fxml"));
            Formularios.controller.controllerCompraEmergente controlador = new Formularios.controller.controllerCompraEmergente();
            controlador.setItemsCompra(itemsEntrada);
            controlador.setTituloFormulario("Agregar");
            loader.setController(controlador);

            Pane formulario = loader.load();

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Agregar");
            stage.setScene(new javafx.scene.Scene(formulario));
            stage.initOwner(root.getScene().getWindow());
            stage.setResizable(false);
            stage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
            mostrarAlerta("Error", "No se pudo abrir el formulario de agregar.");
        }
    }

    @FXML
    public void abrirFormularioQuitar() {
        Formularios.controller.controllerNuevaVenta controlador = new Formularios.controller.controllerNuevaVenta();
        controlador.setItemsVenta(itemsSalida);
        controlador.setTituloFormulario("Quitar");
        controlador.setModoSoloNormal(true);
        controllerFormularios.controllerFormulario.llamarFormulario("/Formularios/view/nuevaVenta.fxml", controlador, "Quitar");
    }

    @FXML
    public void eliminarSeleccionados() {
        if (itemsEntrada.isEmpty() && itemsSalida.isEmpty()) {
            mostrarAlerta("Advertencia", "No hay registros para eliminar.");
            return;
        }

        boolean algunSeleccionado = itemsEntrada.stream().anyMatch(compra::isSeleccionado)
                || itemsSalida.stream().anyMatch(traspasoSalida::isSeleccionado);
        if (!algunSeleccionado) {
            mostrarAlerta("Advertencia", "Seleccione al menos una fila para eliminar.");
            return;
        }

        itemsEntrada.removeIf(compra::isSeleccionado);
        itemsSalida.removeIf(traspasoSalida::isSeleccionado);
        refrescarTabla();
        actualizarSeleccionTodo();
    }

    private void mostrarAlerta(String titulo, String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.INFORMATION);
        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }

    private void actualizarSeleccionTodo() {
        if (miCheckBox == null) {
            return;
        }
        try {
            actualizandoSeleccionTodo = true;
            boolean hayItems = !itemsEntrada.isEmpty() || !itemsSalida.isEmpty();
            boolean seleccionado = hayItems
                    && itemsEntrada.stream().allMatch(compra::isSeleccionado)
                    && itemsSalida.stream().allMatch(traspasoSalida::isSeleccionado);
            miCheckBox.setSelected(seleccionado);
        } finally {
            actualizandoSeleccionTodo = false;
        }
    }
}
