package Consultas.proveedores.controller;

import Compartido.controller.encabezadoController;
import Compartido.controller.navbarController;
import Compartido.exportar.exportador;
import Compartido.exportar.exportarPlantilla;
import Compartido.importar.importador;
import Consultas.proveedores.model.proveedores;
import Consultas.proveedores.model.model;
import Formularios.controller.controllerNuevoProveedor;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;

public class MainController {

    @FXML private StackPane root;
    @FXML private BorderPane paneNavbar;
    @FXML private VBox navbar;
    @FXML private VBox contenedor;
    @FXML private Pane overlayPane;
    @FXML private VBox contenedorTabla;
    @FXML private TextField buscador;
    @FXML private Region expansor;
    @FXML private TableView<proveedores> contenidoTabla;
    @FXML private TableColumn<proveedores, Void> colSelect;
    @FXML private TableColumn<proveedores, String> colID;
    @FXML private TableColumn<proveedores, String> colNombre;
    @FXML private TableColumn<proveedores, String> colRepresentante;
    @FXML private TableColumn<proveedores, String> colRFC;
    @FXML private TableColumn<proveedores, String> colCURP;
    @FXML private TableColumn<proveedores, String> colRazonSocial;
    @FXML private TableColumn<proveedores, String> colCorreo;
    @FXML private TableColumn<proveedores, String> colTelefono;
    @FXML private TableColumn<proveedores, String> colCP;
    @FXML private TableColumn<proveedores, String> colPais;
    @FXML private TableColumn<proveedores, String> colEstado;
    @FXML private TableColumn<proveedores, String> colCiudad;
    @FXML private TableColumn<proveedores, String> colLocalidad;
    @FXML private TableColumn<proveedores, String> colColonia;
    @FXML private TableColumn<proveedores, String> colDomicilio;
    @FXML private TableColumn<proveedores, String> colNumeroExt;
    @FXML private TableColumn<proveedores, String> colNumeroInt;

    @FXML private encabezadoController paneNavbarController;

    // EXACTAMENTE IGUAL que productos: instancia única
    private final model proveedorModel = new model();

    @FXML
    public void initialize() {
        Platform.runLater(() -> {

            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/Compartido/view/navbar.fxml"));
                VBox navbarLoaded = loader.load();
                navbarController navbarCtrl = loader.getController();
                navbarCtrl.setOverlayPane(overlayPane);
                navbar.getChildren().setAll(navbarLoaded);
            } catch (IOException e) {
                e.printStackTrace();
            }

            SplitPane.setResizableWithParent(navbar, false);
            SplitPane.setResizableWithParent(contenedor, true);

            paneNavbar.prefHeightProperty().bind(root.heightProperty().multiply(0.1));
            paneNavbar.prefWidthProperty().bind(root.widthProperty().multiply(0.9));

            navbar.prefWidthProperty().bind(root.widthProperty().multiply(0.15));
            navbar.prefHeightProperty().bind(root.heightProperty().multiply(0.9));

            HBox.setHgrow(expansor, Priority.ALWAYS);
            expansor.setMinWidth(10);

            buscador.prefWidthProperty().bind(root.widthProperty().multiply(0.22));
            buscador.maxHeightProperty().bind(navbar.heightProperty().multiply(0.5));

            contenedor.prefHeightProperty().bind(root.heightProperty().multiply(0.75));
            contenedorTabla.prefHeightProperty().bind(contenedor.heightProperty().multiply(0.9));
            contenidoTabla.prefHeightProperty().bind(contenedorTabla.heightProperty().multiply(0.9));

            paneNavbarController.setTitulo("Proveedores", "#ffffff");

            // Cell Value Factories
            colID.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(String.valueOf(cellData.getValue().getId())));
            colNombre.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getNombre()));
            colRepresentante.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getRepresentante()));
            colRFC.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getRfc()));
            colCURP.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getCurp()));
            colRazonSocial.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getRazonSocial()));
            colCorreo.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getCorreo()));
            colTelefono.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(String.valueOf(cellData.getValue().getTelefono())));
            colCP.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(String.valueOf(cellData.getValue().getCp())));
            colPais.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getPais()));
            colEstado.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getEstado()));
            colCiudad.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getCiudad()));
            colLocalidad.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getLocalidad()));
            colColonia.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getColonia()));
            colDomicilio.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getDomicilio()));
            colNumeroExt.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(String.valueOf(cellData.getValue().getNumeroExt())));
            colNumeroInt.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(String.valueOf(cellData.getValue().getNumeroInt())));

            // Centrado - estructura más limpia
            TableColumn<proveedores, String>[] columnas = new TableColumn[]{
                    colSelect, colID, colNombre, colRepresentante, colRFC, colCURP, colRazonSocial, colCorreo,
                    colTelefono, colCP, colPais, colEstado, colCiudad, colLocalidad, colColonia,
                    colDomicilio, colNumeroExt, colNumeroInt
            };
            for (TableColumn<proveedores, String> col : columnas) {
                col.setStyle("-fx-alignment: CENTER;");
            }

            // Botón eliminar - EXACTA misma estructura que productos
            colSelect.setCellFactory(col -> new TableCell<proveedores, Void>() {
                private final Button btn;
                {
                    btn = new Button();
                    ImageView img = new ImageView(new Image(getClass().getResourceAsStream("/img/eliminar.png")));
                    img.setFitWidth(18);
                    img.setFitHeight(18);
                    img.setPreserveRatio(true);
                    btn.setGraphic(img);
                    btn.setStyle("-fx-background-color: #333; -fx-cursor: hand;");
                    btn.setOnAction(e -> {
                        proveedores seleccionado = getTableView().getItems().get(getIndex());
                        Alert alerta = new Alert(Alert.AlertType.CONFIRMATION);
                        alerta.setTitle("Confirmar eliminación");
                        alerta.setHeaderText(null);
                        alerta.setContentText("¿Está seguro que desea eliminar este proveedor?");
                        alerta.showAndWait().ifPresent(response -> {
                            if (response == ButtonType.OK) {
                                if (proveedorModel.eliminar(seleccionado.getId())) {
                                    contenidoTabla.getItems().remove(seleccionado);
                                    new Alert(Alert.AlertType.INFORMATION, "Proveedor eliminado correctamente").showAndWait();
                                } else {
                                    new Alert(Alert.AlertType.ERROR, "No se pudo eliminar el proveedor.").showAndWait();
                                }
                            }
                        });
                    });
                }

                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    setGraphic(empty ? null : btn);
                }
            });

            // Carga Inicial en background como productos
            cargarProveedoresEnTabla();

            // Doble clic → editar - EXACTO igual
            contenidoTabla.setRowFactory(tv -> {
                TableRow<proveedores> row = new TableRow<>();
                row.setOnMouseClicked(event -> {
                    if (event.getClickCount() == 2 && !row.isEmpty()) {
                        abrirFormulario(row.getItem());
                    }
                });
                return row;
            });

            // ENTER sobre un registro → editar - EXACTO igual (mantener esto)
            contenidoTabla.setOnKeyPressed(event -> {
                if (event.getCode().toString().equals("ENTER")) {
                    proveedores p = contenidoTabla.getSelectionModel().getSelectedItem();
                    if (p != null) abrirFormulario(p);
                }
            });

            // Configurar listener para el buscador - EXACTAMENTE IGUAL que productos
            // ELIMINAR el buscador.setOnKeyPressed que estaba aquí
            buscador.textProperty().addListener((observable, oldValue, newValue) -> {
                buscarProveedores(newValue);
            });
        });
    }

    // MÉTODO EXACTAMENTE IGUAL que cargarProductosEnTabla() en productos
    private void cargarProveedoresEnTabla() {
        Task<ObservableList<proveedores>> task = new Task<>() {
            @Override
            protected ObservableList<proveedores> call() {
                // En productos es: productoModel.obtenerProductos()
                // Aquí es exactamente igual pero con proveedorModel
                return FXCollections.observableArrayList(proveedorModel.obtener());
            }

            @Override
            protected void succeeded() {
                ObservableList<proveedores> proveedores = getValue();
                contenidoTabla.setItems(proveedores);
            }
        };
        new Thread(task).start();
    }

    // MÉTODO EXACTAMENTE IGUAL que buscarProductos() en productos
    private void buscarProveedores(String texto) {
        if (texto == null || texto.trim().isEmpty()) {
            cargarProveedoresEnTabla();
        } else {
            // Si tu modelo de proveedores tiene busquedaMultiple, úsalo como en productos
            // Si no, usa buscarExacto (pero deberías agregar busquedaMultiple a proveedores también)
            ObservableList<proveedores> proveedores = FXCollections.observableArrayList(proveedorModel.buscarExacto(texto));
            contenidoTabla.setItems(proveedores);
            // ELIMINADO: El mensaje de "No se encontraron registros"
        }
    }

    @FXML
    public void formularioNuevoProveedor() {
        abrirFormulario(null);
    }

    private void abrirFormulario(proveedores editar) {
        try {
            // === CREAR CONTROLLER MANUALMENTE ===
            Formularios.controller.controllerNuevoProveedor controlador =
                    new Formularios.controller.controllerNuevoProveedor();

            // === CREAR LOADER Y ASIGNAR CONTROLLER ===
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/Formularios/view/nuevoProveedor.fxml")
            );
            loader.setController(controlador);

            Parent vista = loader.load();

            // === USAR EL CONTROLLER YA ASIGNADO ===
            if (editar != null) {
                controlador.cargarProveedor(editar);
            } else {
                controlador.prepararNuevoProveedor();
            }

            // === STAGE ===
            Stage stage = new Stage();
            stage.setTitle(editar == null ? "Nuevo Registro" : "Editar Registro");
            stage.setScene(new Scene(vista));
            stage.setResizable(false);
            stage.setWidth(600);
            stage.setHeight(640);
            stage.centerOnScreen();
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(root.getScene().getWindow());

            stage.showAndWait();

            // === RECARGAR TABLA ===
            cargarProveedoresEnTabla();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    @FXML
    private void exportarDatos() {

        if (contenidoTabla.getItems().isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "No hay datos para exportar.").showAndWait();
            return;
        }

        Alert dialogo = new Alert(Alert.AlertType.CONFIRMATION);
        dialogo.setTitle("Exportar");
        dialogo.setHeaderText("Seleccione el formato para exportar:");
        ButtonType btnPDF = new ButtonType("PDF");
        ButtonType btnExcel = new ButtonType("Excel (.xlsx)");
        ButtonType btnCancelar = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);

        dialogo.getButtonTypes().setAll(btnPDF, btnExcel, btnCancelar);

        dialogo.showAndWait().ifPresent(res -> {
            if (res == btnPDF) {
                exportador.exportarTabla(contenidoTabla, "proveedores", "pdf");
            } else if (res == btnExcel) {
                exportador.exportarTabla(contenidoTabla, "proveedores", "excel");
            }
        });
    }

    public void importarDatos() {
        importador.importarExcel("proveedores", "id");
        // Recargar como en productos
        cargarProveedoresEnTabla();
    }

    public void exportarPlantilla() {
        exportarPlantilla.exportarPlantilla("proveedores");
    }
}