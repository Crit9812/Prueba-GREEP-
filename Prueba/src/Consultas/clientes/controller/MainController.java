package Consultas.clientes.controller;

import Compartido.controller.encabezadoController;
import Compartido.controller.navbarController;
import Compartido.exportar.exportarPlantilla;
import Compartido.importar.importador;
import Consultas.clientes.model.cliente;
import Consultas.clientes.model.model;
import Formularios.controller.controllerNuevoCliente;
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
import Compartido.exportar.exportador;
import Compartido.helper.RefrescoHelper;


import java.io.IOException;

public class MainController {

    @FXML private StackPane root;
    @FXML private BorderPane paneNavbar;
    @FXML private VBox navbar;
    @FXML private VBox contenedor;
    @FXML private Pane overlayPane;
    @FXML private Region expansor;
    @FXML private TextField buscador;
    @FXML private VBox contenedorTabla;
    @FXML private TableView<cliente> contenidoTabla;
    @FXML private TableColumn<cliente, Void> colSelect;
    @FXML private TableColumn<cliente, String> colID;
    @FXML private TableColumn<cliente, String> colNombre;
    @FXML private TableColumn<cliente, String> colRFC;
    @FXML private TableColumn<cliente, String> colCURP;
    @FXML private TableColumn<cliente, String> colRazonSocial;
    @FXML private TableColumn<cliente, String> colCorreo;
    @FXML private TableColumn<cliente, String> colTelefono;
    @FXML private TableColumn<cliente, String> colCP;
    @FXML private TableColumn<cliente, String> colPais;
    @FXML private TableColumn<cliente, String> colEstado;
    @FXML private TableColumn<cliente, String> colCiudad;
    @FXML private TableColumn<cliente, String> colLocalidad;
    @FXML private TableColumn<cliente, String> colColonia;
    @FXML private TableColumn<cliente, String> colDomicilio;
    @FXML private TableColumn<cliente, String> colNumeroExt;
    @FXML private TableColumn<cliente, String> colNumeroInt;

    @FXML private encabezadoController paneNavbarController;

    // EXACTAMENTE IGUAL que productos: instancia única
    private model clienteModel;

    @FXML
    public void initialize() {
        clienteModel = new model();
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
            contenedor.prefHeightProperty().bind(root.heightProperty().multiply(0.75));
            contenedorTabla.prefHeightProperty().bind(contenedor.heightProperty().multiply(0.9));
            contenidoTabla.prefHeightProperty().bind(contenedorTabla.heightProperty().multiply(0.9));

            HBox.setHgrow(expansor, Priority.ALWAYS);
            expansor.setMinWidth(10);

            buscador.prefWidthProperty().bind(root.widthProperty().multiply(0.22));
            buscador.maxHeightProperty().bind(navbar.heightProperty().multiply(0.5));

            paneNavbarController.setTitulo("Clientes", "#ffffff");

            // Cell Value Factories - EXACTO igual estructura
            colID.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(String.valueOf(c.getValue().getId())));
            colNombre.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getNombre()));
            colRFC.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getRfc()));
            colCURP.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getCurp()));
            colRazonSocial.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getRazonSocial()));
            colCorreo.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getCorreo()));
            colTelefono.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(String.valueOf(c.getValue().getTelefono())));
            colCP.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(String.valueOf(c.getValue().getCp())));
            colPais.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getPais()));
            colEstado.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getEstado()));
            colCiudad.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getCiudad()));
            colLocalidad.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getLocalidad()));
            colColonia.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getColonia()));
            colDomicilio.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getDomicilio()));
            colNumeroExt.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(String.valueOf(c.getValue().getNumeroExt())));
            colNumeroInt.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(String.valueOf(c.getValue().getNumeroInt())));

            // Centrado - EXACTO igual
            TableColumn<cliente, String>[] columnas = new TableColumn[]{
                    colSelect, colID,colNombre,colRFC,colCURP,colRazonSocial,colCorreo,colTelefono,colCP,
                    colPais,colEstado,colCiudad,colLocalidad,colColonia,colDomicilio,colNumeroExt,colNumeroInt
            };
            for (TableColumn<cliente, String> col : columnas) col.setStyle("-fx-alignment: CENTER;");

            // Botón eliminar - EXACTA misma estructura que productos
            colSelect.setCellFactory(col -> new TableCell<cliente, Void>() {
                private final Button btn;
                {
                    btn = new Button();
                    ImageView img = new ImageView(new Image(getClass().getResourceAsStream("/img/eliminar.png")));
                    img.setFitWidth(18);
                    img.setFitHeight(18);
                    btn.setGraphic(img);
                    btn.setStyle("-fx-background-color: #333; -fx-cursor: hand;");
                    btn.setOnAction(e -> {
                        cliente seleccionado = getTableView().getItems().get(getIndex());
                        Alert alerta = new Alert(Alert.AlertType.CONFIRMATION);
                        alerta.setTitle("Confirmar eliminación");
                        alerta.setHeaderText(null);
                        alerta.setContentText("¿Está seguro que desea eliminar este cliente?");
                        alerta.showAndWait().ifPresent(response -> {
                            if (response == ButtonType.OK) {
                                if (clienteModel.eliminarCliente(seleccionado.getId())) {
                                    contenidoTabla.getItems().remove(seleccionado);
                                    new Alert(Alert.AlertType.INFORMATION, "Cliente eliminado correctamente").showAndWait();
                                } else {
                                    new Alert(Alert.AlertType.ERROR, "No se pudo eliminar el cliente.").showAndWait();
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

            contenidoTabla.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
                // Puedes agregar algo aquí si necesitas, como en productos con imágenes
            });

            // Doble clic → editar - EXACTO igual
            contenidoTabla.setRowFactory(tv -> {
                TableRow<cliente> row = new TableRow<>();
                row.setOnMouseClicked(event -> {
                    if (event.getClickCount() == 2 && !row.isEmpty()) {
                        abrirFormulario(row.getItem());
                    }
                });
                return row;
            });

            // ENTER sobre un registro → editar - EXACTO igual
            contenidoTabla.setOnKeyPressed(event -> {
                if (event.getCode().toString().equals("ENTER")) {
                    cliente c = contenidoTabla.getSelectionModel().getSelectedItem();
                    if (c != null) abrirFormulario(c);
                }
            });

            // Configurar listener para el buscador - EXACTAMENTE IGUAL que productos
            buscador.textProperty().addListener((observable, oldValue, newValue) -> {
                buscarClientes(newValue);
            });

            RefrescoHelper.setVistaActual("clientes");
            RefrescoHelper.registrarRefresco("clientes", this::actualizarClientes);

            // Carga Inicial en background como productos
            cargarClientesEnTabla();
        });
    }

    // ========== NUEVO MÉTODO DE ACTUALIZACIÓN ==========
    private void actualizarClientes() {
        System.out.println("========================================");
        System.out.println("ACTUALIZANDO CLIENTES");
        System.out.println("Hora: " + new java.util.Date());
        System.out.println("========================================");

        // 1. Crear NUEVA instancia del modelo
        clienteModel = new model();
        System.out.println("✓ Nuevo modelo de clientes creado");

        // 2. Limpiar UI
        Platform.runLater(() -> {
            buscador.clear();
            contenidoTabla.getSelectionModel().clearSelection();
            contenidoTabla.setItems(FXCollections.observableArrayList());
            System.out.println("✓ UI limpiada");
        });

        // 3. Recargar datos
        cargarClientesEnTabla();

        System.out.println("========================================");
        System.out.println("ACTUALIZACIÓN DE CLIENTES COMPLETADA");
        System.out.println("========================================");
    }

    // MetODO EXACTAMENTE IGUAL que cargarProductosEnTabla() en productos
    private void cargarClientesEnTabla() {
        Task<ObservableList<cliente>> task = new Task<>() {
            @Override
            protected ObservableList<cliente> call() {
                return FXCollections.observableArrayList(clienteModel.obtenerClientes());
            }

            @Override
            protected void succeeded() {
                ObservableList<cliente> clientes = getValue();
                contenidoTabla.setItems(clientes);
            }
        };
        new Thread(task).start();
    }

    // MeTODO EXACTAMENTE IGUAL que buscarProductos() en productos
    private void buscarClientes(String texto) {
        if (texto == null || texto.trim().isEmpty()) {
            cargarClientesEnTabla();
        } else {
            // Si tu modelo de clientes tiene busquedaMultiple, úsalo como en productos
            // Si no, usa buscarExacto (pero deberías agregar busquedaMultiple a clientes también)
            ObservableList<cliente> clientes = FXCollections.observableArrayList(clienteModel.buscarExacto(texto));
            contenidoTabla.setItems(clientes);
        }
    }

    @FXML
    public void formularioNuevoCliente() {
        abrirFormulario(null);
    }

    private void abrirFormulario(cliente clienteEditar) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Formularios/view/nuevoCliente.fxml"));
            Parent vista = loader.load();

            controllerNuevoCliente ctrl = loader.getController();
            if (clienteEditar != null) ctrl.cargarCliente(clienteEditar);

            Stage stage = new Stage();
            stage.setTitle(clienteEditar == null ? "Nuevo Cliente" : "Editar Cliente");
            stage.setScene(new Scene(vista));
            stage.setResizable(false);
            stage.centerOnScreen();
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(root.getScene().getWindow());

            stage.showAndWait();
            // Recargar como en productos
            cargarClientesEnTabla();

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
        dialogo.setHeaderText("Seleccione el formato:");
        ButtonType btnPDF = new ButtonType("PDF");
        ButtonType btnExcel = new ButtonType("Excel (.xlsx)");
        ButtonType btnCancelar = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialogo.getButtonTypes().setAll(btnPDF, btnExcel, btnCancelar);

        dialogo.showAndWait().ifPresent(res -> {
            if (res == btnPDF) exportador.exportarTabla(contenidoTabla, "Clientes", "pdf");
            else if (res == btnExcel) exportador.exportarTabla(contenidoTabla, "Clientes", "excel");
        });
    }

    public void importarDatos() {
        importador.importarExcel("clientes", "id");
        // Recargar como en productos
        cargarClientesEnTabla();
    }

    public void exportarPlantilla() {
        exportarPlantilla.exportarPlantilla("clientes");
    }
}