package Operaciones.traspasoEntrada.controller;

import Compartido.controller.encabezadoController;
import Compartido.controller.navbarController;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.layout.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.cell.PropertyValueFactory;
import java.io.IOException;

public class MainController {

    @FXML private ComboBox<String> miComboBox;
    @FXML private StackPane root;
    @FXML private BorderPane paneNavbar;
    @FXML private VBox navbar;
    @FXML private VBox contenedor;
    @FXML private Pane overlayPane;
    @FXML private VBox contenedorTabla;
    @FXML private TableView contenidoTabla;
    @FXML private HBox contenedorBtnConfirmar;
    @FXML private HBox rootHBox;
    @FXML private Label lblOrdenar;
    @FXML private Region expansor;

    @FXML private encabezadoController paneNavbarController;

    @FXML private TableColumn<Venta, Integer> colClaveEntrada;
    @FXML private TableColumn<Venta, String> colFecha;
    @FXML private TableColumn<Venta, String> colHora;
    @FXML private TableColumn<Venta, Double> colTotal;
    @FXML private TableColumn<Venta, String> colNombreSucural;
    @FXML private TableColumn<Venta, Boolean> colSelect;

    @FXML private CheckBox miCheckBox; // Seleccionar todo

    private ObservableList<Venta> listaVentas;

    @FXML
    public void initialize() {
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

        });

        // Columnas
        colClaveEntrada.setCellValueFactory(new PropertyValueFactory<>("id"));
        colFecha.setCellValueFactory(new PropertyValueFactory<>("fecha"));
        colHora.setCellValueFactory(new PropertyValueFactory<>("hora"));
        colTotal.setCellValueFactory(new PropertyValueFactory<>("total"));
        colNombreSucural.setCellValueFactory(new PropertyValueFactory<>("nombreSucursal"));

        // Columna de selección
        colSelect.setCellValueFactory(cellData -> cellData.getValue().seleccionadoProperty());
        colSelect.setCellFactory(CheckBoxTableCell.forTableColumn(colSelect));

        // Lista y datos de prueba
        listaVentas = FXCollections.observableArrayList(
                new Venta(1, "12/09/2025", "10:00", 500.0, "Sucursal Centro"),
                new Venta(2, "12/09/2025", "11:30", 1200.5, "Sucursal Norte"),
                new Venta(3, "12/09/2025", "13:15", 890.75, "Sucursal Sur")
        );
        contenidoTabla.setItems(listaVentas);

        // Seleccionar todo
        miCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            for (Venta v : listaVentas) v.setSeleccionado(newVal);
        });

        // Listener en cada fila para actualizar "Seleccionar todo"
        for (Venta v : listaVentas) {
            v.seleccionadoProperty().addListener((obs, oldVal, newVal) -> {
                if (!newVal && miCheckBox.isSelected()) miCheckBox.setSelected(false);
                else if (listaVentas.stream().allMatch(Venta::isSeleccionado)) miCheckBox.setSelected(true);
            });
        }

        // Hover filas con cursor solo para filas con contenido
        contenidoTabla.setRowFactory(tv -> new TableRow<Venta>() {
            @Override
            protected void updateItem(Venta item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setStyle("-fx-background-color: transparent;");
                    setOnMouseEntered(null);
                    setOnMouseExited(null);
                } else {
                    setStyle("-fx-cursor: hand;"); // Solo filas con contenido tendrán cursor tipo mano
                    setOnMouseEntered(e -> {
                        if (!isMouseOverCheckBox(e.getX())) {
                            setStyle("-fx-background-color: #91d485; -fx-cursor: hand;");
                        }
                    });
                    setOnMouseExited(e -> {
                        setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
                    });
                }
            }

            private boolean isMouseOverCheckBox(double mouseX) {
                double checkboxColumnWidth = colSelect.getWidth();
                return mouseX <= checkboxColumnWidth;
            }
        });

        contenidoTabla.setSelectionModel(null);
    }

    @FXML
    public void comboBx() {
        miComboBox.getItems().addAll("Aceptar", "Rechazar");
    }
}
