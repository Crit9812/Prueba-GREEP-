package Operaciones.pedidos.controller;

import Compartido.controller.encabezadoController;
import Compartido.controller.navbarController;
import Compartido.exportar.exportador;
import Formularios.controller.controllerNuevoPedido;
import Operaciones.pedidos.model.itemPedido;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.stage.Modality;
import javafx.util.Callback;

import java.io.IOException;

public class MainController {

    @FXML private StackPane root;
    @FXML private BorderPane paneNavbar;
    @FXML private VBox navbar;
    @FXML private VBox contenedor;
    @FXML private Pane overlayPane;
    @FXML private VBox contenedorTabla;
    @FXML private TableView<itemPedido> contenidoTabla;
    @FXML private Label lblEliminar;
    @FXML private Label lblAgregar;
    @FXML private Region expansor;

    // Columnas
    @FXML private TableColumn<itemPedido, Boolean> colSelect;
    @FXML private TableColumn<itemPedido, String> colClaveProduct;
    @FXML private TableColumn<itemPedido, Integer> colCantidad;
    @FXML private TableColumn<itemPedido, String> colProducto;
    @FXML private TableColumn<itemPedido, String> colDescripcionProducto;
    @FXML private TableColumn<itemPedido, String> colPresentacion;
    @FXML private TableColumn<itemPedido, String> colFactor;

    @FXML private CheckBox miCheckBoxSeleccionarTodo;

    @FXML private encabezadoController paneNavbarController;

    // LISTA ÚNICA QUE ALIMENTA LA TABLA
    private final ObservableList<itemPedido> itemsPedido = FXCollections.observableArrayList();
    private Stage formularioStage; // Para poder cerrar el formulario
    private itemPedido itemSeleccionadoParaEditar; // 🔥 NUEVO: Para guardar referencia al item en edición

    @FXML
    public void initialize() {
        Platform.runLater(() -> {
            // ===== NAVBAR =====
            try {
                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/Compartido/view/navbar.fxml")
                );
                VBox navbarLoaded = loader.load();

                navbarController navbarCtrl = loader.getController();
                navbarCtrl.setOverlayPane(overlayPane);

                navbar.getChildren().setAll(navbarLoaded);

            } catch (IOException e) {
                e.printStackTrace();
            }

            SplitPane.setResizableWithParent(navbar, false);
            SplitPane.setResizableWithParent(contenedor, true);

            // ===== LAYOUT =====
            paneNavbar.prefHeightProperty().bind(root.heightProperty().multiply(0.1));
            paneNavbar.prefWidthProperty().bind(root.widthProperty().multiply(0.9));

            navbar.prefWidthProperty().bind(root.widthProperty().multiply(0.15));
            navbar.prefHeightProperty().bind(root.heightProperty().multiply(0.9));

            contenedor.prefHeightProperty().bind(root.heightProperty().multiply(0.75));

            HBox.setHgrow(expansor, Priority.ALWAYS);
            expansor.setMinWidth(10);
            lblEliminar.setMinWidth(Region.USE_PREF_SIZE);
            lblAgregar.setMinWidth(Region.USE_PREF_SIZE);

            contenedorTabla.prefHeightProperty().bind(contenedor.heightProperty().multiply(0.95));
            contenidoTabla.prefHeightProperty().bind(contenedorTabla.heightProperty().multiply(0.9));

            paneNavbarController.setTitulo("Pedidos", "#ffffff");

            // ===== CONFIGURACIÓN DE LA TABLA =====
            configurarColumnasTabla();

            // ===== 🔗 CONEXIÓN CLAVE =====
            // La tabla queda ligada a la lista
            contenidoTabla.setItems(itemsPedido);

            // Configurar evento para seleccionar todo
            configurarSeleccionTodo();

            // 🔥 NUEVO: Configurar doble clic y Enter para editar
            configurarEventosEdicionTabla();
        });
    }

    private void configurarColumnasTabla() {
        colSelect.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<itemPedido, Boolean>, ObservableValue<Boolean>>() {
            @Override
            public ObservableValue<Boolean> call(TableColumn.CellDataFeatures<itemPedido, Boolean> param) {
                itemPedido item = param.getValue();
                if (item != null) {
                    return item.seleccionadoProperty();
                } else {
                    return new SimpleBooleanProperty(false);
                }
            }
        });

        colSelect.setCellFactory(CheckBoxTableCell.forTableColumn(colSelect));
        colClaveProduct.setCellValueFactory(new PropertyValueFactory<>("claveProducto"));
        colCantidad.setCellValueFactory(new PropertyValueFactory<>("cantidad"));
        colProducto.setCellValueFactory(new PropertyValueFactory<>("producto"));
        colDescripcionProducto.setCellValueFactory(new PropertyValueFactory<>("descripcion"));
        colPresentacion.setCellValueFactory(new PropertyValueFactory<>("presentacion"));
        colFactor.setCellValueFactory(new PropertyValueFactory<>("factor"));

        // Hacer que las columnas sean editables si es necesario
        contenidoTabla.setEditable(true);

        TableColumn<itemPedido, ?>[] columnas = new TableColumn[]{
                colSelect, colClaveProduct, colCantidad, colProducto,
                colDescripcionProducto, colPresentacion, colFactor
        };
        for (TableColumn<itemPedido, ?> col : columnas) col.setStyle("-fx-alignment: CENTER;");
    }

    private void configurarSeleccionTodo() {
        miCheckBoxSeleccionarTodo.selectedProperty().addListener((obs, oldVal, newVal) -> {
            for (itemPedido item : itemsPedido) {
                item.setSeleccionado(newVal);
            }
        });
    }

    /**
     * 🔥 NUEVO: Configurar eventos para editar registros
     */
    private void configurarEventosEdicionTabla() {
        // Doble clic para editar
        contenidoTabla.setOnMouseClicked(event -> {
            if (event.getButton().equals(MouseButton.PRIMARY) && event.getClickCount() == 2) {
                itemPedido seleccionado = contenidoTabla.getSelectionModel().getSelectedItem();
                if (seleccionado != null) {
                    abrirFormularioParaEditar(seleccionado);
                }
            }
        });

        // Tecla Enter para editar
        contenidoTabla.setOnKeyPressed(event -> {
            if (event.getCode() == javafx.scene.input.KeyCode.ENTER) {
                itemPedido seleccionado = contenidoTabla.getSelectionModel().getSelectedItem();
                if (seleccionado != null) {
                    abrirFormularioParaEditar(seleccionado);
                }
            }
        });
    }

    @FXML
    public void abrirFormularioPedido() {
        abrirFormulario(null); // Abrir formulario vacío
    }

    /**
     * 🔥 NUEVO: Método para abrir formulario en modo edición
     */
    private void abrirFormularioParaEditar(itemPedido item) {
        abrirFormulario(item);
    }

    /**
     * 🔥 MODIFICADO: Método reutilizable para abrir formulario
     * @param itemParaEditar Si es null, abre formulario vacío. Si tiene valor, abre en modo edición.
     */
    private void abrirFormulario(itemPedido itemParaEditar) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/Formularios/view/nuevoPedido.fxml")
            );

            // Cargar el formulario
            Pane formulario = loader.load();

            // Obtener el controlador
            Formularios.controller.controllerNuevoPedido controlador = loader.getController();

            // PASAMOS LA LISTA DE LA TABLA AL FORMULARIO
            controlador.setItemsPedido(itemsPedido);
            // Pasamos la referencia al stage para poder cerrarlo
            controlador.setMainController(this);

            // 🔥 NUEVO: Si hay un item para editar, lo cargamos en el formulario
            if (itemParaEditar != null) {
                controlador.cargarItemParaEditar(itemParaEditar);
                this.itemSeleccionadoParaEditar = itemParaEditar; // Guardar referencia
            }

            // Crear nueva ventana modal
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle(itemParaEditar != null ? "Editar Producto" : "Nuevo Pedido");
            stage.setScene(new javafx.scene.Scene(formulario));
            stage.setOnHidden(e -> {
                // Limpiar referencia cuando se cierra
                this.formularioStage = null;
                this.itemSeleccionadoParaEditar = null;
            });

            this.formularioStage = stage;
            stage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
            mostrarError("Error al abrir formulario: " + e.getMessage());
        }
    }

    @FXML
    public void descargar(){
        if (contenidoTabla.getItems().isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "No hay datos para exportar.").showAndWait();
            return;
        }
        exportador.exportarTabla(contenidoTabla, "pedidos", "pdf");
    }

    @FXML
    public void eliminarProducto() {
        // Verificar si hay items seleccionados
        boolean haySeleccionados = false;
        for (itemPedido item : itemsPedido) {
            if (item.isSeleccionado()) {
                haySeleccionados = true;
                break;
            }
        }

        if (!haySeleccionados) {
            mostrarAlerta("Advertencia", "No hay productos seleccionados para eliminar.");
            return;
        }

        // Pedir confirmación al usuario
        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Confirmar eliminación");
        confirmacion.setHeaderText("Eliminar productos seleccionados");
        confirmacion.setContentText("¿Está seguro que desea eliminar los productos seleccionados?");

        confirmacion.showAndWait().ifPresent(respuesta -> {
            if (respuesta == ButtonType.OK) {
                // Crear una copia de la lista para evitar ConcurrentModificationException
                ObservableList<itemPedido> itemsAEliminar = FXCollections.observableArrayList();

                // Identificar los items seleccionados
                for (itemPedido item : itemsPedido) {
                    if (item.isSeleccionado()) {
                        itemsAEliminar.add(item);
                    }
                }

                // Eliminar los items seleccionados
                itemsPedido.removeAll(itemsAEliminar);

                // Desmarcar "Seleccionar todo" si estaba marcado
                miCheckBoxSeleccionarTodo.setSelected(false);

                // Mostrar mensaje de éxito
                mostrarAlerta("Éxito", "Se eliminaron " + itemsAEliminar.size() + " producto(s) del pedido.");

                // Refrescar la tabla
                refrescarTabla();
            }
        });
    }

    /**
     * 🔥 NUEVO: Método para actualizar un item en la lista
     */
    public void actualizarItemEnLista(itemPedido itemViejo, itemPedido itemNuevo) {
        int indice = itemsPedido.indexOf(itemViejo);
        if (indice >= 0) {
            itemsPedido.set(indice, itemNuevo);
            refrescarTabla();
        }
    }

    /**
     * 🔥 NUEVO: Método para eliminar un item específico
     */
    public void eliminarItemDeLista(itemPedido item) {
        itemsPedido.remove(item);
        refrescarTabla();
    }

    /**
     * Metodo para cerrar el formulario desde el controlador del formulario
     */
    public void cerrarFormulario() {
        if (formularioStage != null) {
            formularioStage.close();
            formularioStage = null;
            itemSeleccionadoParaEditar = null;
        }
    }

    /**
     * Metodo para refrescar la tabla
     */
    public void refrescarTabla() {
        contenidoTabla.refresh();
    }

    /**
     * 🔥 NUEVO: Getter para el item en edición
     */
    public itemPedido getItemSeleccionadoParaEditar() {
        return itemSeleccionadoParaEditar;
    }

    private void mostrarError(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    private void mostrarAlerta(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}