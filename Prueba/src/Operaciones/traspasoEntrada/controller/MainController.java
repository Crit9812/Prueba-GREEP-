package Operaciones.traspasoEntrada.controller;

import Compartido.controller.encabezadoController;
import Compartido.controller.navbarController;
import Operaciones.traspasoEntrada.model.model;
import Operaciones.traspasoEntrada.model.traspasoEntrada;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.layout.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableValue;
import javafx.util.Callback;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainController {

    @FXML private ComboBox<String> miComboBox;
    @FXML private CheckBox miCheckBox;
    @FXML private StackPane root;
    @FXML private BorderPane paneNavbar;
    @FXML private VBox navbar;
    @FXML private VBox contenedor;
    @FXML private Pane overlayPane;
    @FXML private VBox contenedorTabla;
    @FXML private TableView<traspasoEntrada> contenidoTabla;
    @FXML private HBox contenedorBtnConfirmar;
    @FXML private HBox rootHBox;
    @FXML private Label lblOrdenar;
    @FXML private Region expansor;

    @FXML private TableColumn<traspasoEntrada, Boolean> colSelect;
    @FXML private TableColumn<traspasoEntrada, String> colClaveEntrada;
    @FXML private TableColumn<traspasoEntrada, String> colFecha;
    @FXML private TableColumn<traspasoEntrada, String> colHora;
    @FXML private TableColumn<traspasoEntrada, String> colTotal;
    @FXML private TableColumn<traspasoEntrada, String> colNombreSucural;

    @FXML private encabezadoController paneNavbarController;

    private final ObservableList<traspasoEntrada> entradasTraspaso = FXCollections.observableArrayList();
    private final model modeloTraspaso = new model();

    @FXML
    public void initialize() {
        miComboBox.setItems(FXCollections.observableArrayList("Aceptar", "Rechazar"));
        miComboBox.setValue("Opciones");

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
            lblOrdenar.setMinWidth(Region.USE_PREF_SIZE);

            // Tabla
            contenedorTabla.prefHeightProperty().bind(contenedor.heightProperty().multiply(0.95));
            contenidoTabla.prefHeightProperty().bind(contenedorTabla.heightProperty().multiply(0.9));

            paneNavbarController.setTitulo("Traspaso de Entrada", "#ffffff");

            configurarTabla();
            cargarTabla();
        });

    }

    private void configurarTabla() {
        colSelect.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<traspasoEntrada, Boolean>, ObservableValue<Boolean>>() {
            @Override
            public ObservableValue<Boolean> call(TableColumn.CellDataFeatures<traspasoEntrada, Boolean> param) {
                traspasoEntrada item = param.getValue();
                if (item != null) {
                    return item.seleccionadoProperty();
                }
                return new SimpleBooleanProperty(false);
            }
        });

        colSelect.setCellFactory(CheckBoxTableCell.forTableColumn(colSelect));
        colClaveEntrada.setCellValueFactory(new PropertyValueFactory<>("claveEntrada"));
        colFecha.setCellValueFactory(new PropertyValueFactory<>("fecha"));
        colHora.setCellValueFactory(new PropertyValueFactory<>("hora"));
        colTotal.setCellValueFactory(new PropertyValueFactory<>("total"));
        colNombreSucural.setCellValueFactory(new PropertyValueFactory<>("nombreSucursal"));

        contenidoTabla.setEditable(true);
        contenidoTabla.setItems(entradasTraspaso);

        TableColumn<traspasoEntrada, ?>[] columnas = new TableColumn[]{
                colSelect, colClaveEntrada, colFecha, colHora, colTotal, colNombreSucural
        };
        for (TableColumn<traspasoEntrada, ?> col : columnas) {
            col.setStyle("-fx-alignment: CENTER;");
        }

        miCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            for (traspasoEntrada entrada : entradasTraspaso) {
                entrada.setSeleccionado(newVal);
            }
        });
    }

    private void cargarTabla() {
        entradasTraspaso.setAll(modeloTraspaso.obtenerPendientes());
    }

    @FXML
    public void aplicarAccion() {
        String opcion = miComboBox.getValue();
        if (opcion == null || opcion.equalsIgnoreCase("Opciones")) {
            mostrarAlerta(Alert.AlertType.WARNING, "Selecciona una opción", "Debes seleccionar Aceptar o Rechazar.");
            return;
        }

        List<traspasoEntrada> seleccionados = obtenerSeleccionados();
        if (seleccionados.isEmpty()) {
            mostrarAlerta(Alert.AlertType.WARNING, "Selección requerida", "Se debe seleccionar alguna entrada.");
            return;
        }

        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Confirmación");
        confirmacion.setHeaderText("¿Deseas continuar?");
        confirmacion.setContentText("Se aplicarán cambios a las entradas seleccionadas.");

        confirmacion.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                procesarSeleccion(opcion, seleccionados);
            }
        });
    }

    private void procesarSeleccion(String opcion, List<traspasoEntrada> seleccionados) {
        List<String> claves = new ArrayList<>();
        for (traspasoEntrada entrada : seleccionados) {
            claves.add(entrada.getClaveEntrada());
        }

        boolean rechazar = opcion.equalsIgnoreCase("Rechazar");
        String nuevoEstadoEntrada = rechazar ? "rechazado" : "disponible";
        String nuevoEstadoArticulos = rechazar ? "rechazado" : "disponible";

        boolean actualizado = modeloTraspaso.actualizarEstadoEntradas(
                claves,
                nuevoEstadoEntrada,
                nuevoEstadoArticulos
        );
        if (actualizado) {
            cargarTabla();
            miCheckBox.setSelected(false);
            mostrarAlerta(Alert.AlertType.INFORMATION, "Actualización exitosa", "Se actualizaron las entradas seleccionadas.");
        } else {
            mostrarAlerta(Alert.AlertType.ERROR, "Error", "No se pudieron actualizar las entradas.");
        }
    }

    private List<traspasoEntrada> obtenerSeleccionados() {
        List<traspasoEntrada> seleccionados = new ArrayList<>();
        for (traspasoEntrada entrada : entradasTraspaso) {
            if (entrada.isSeleccionado()) {
                seleccionados.add(entrada);
            }
        }
        return seleccionados;
    }

    private void mostrarAlerta(Alert.AlertType tipo, String titulo, String mensaje) {
        Alert alerta = new Alert(tipo);
        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }
}
