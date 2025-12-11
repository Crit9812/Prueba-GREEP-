package Consultas.clientes.controller;

import Compartido.controller.encabezadoController;
import Compartido.controller.navbarController;
import Compartido.importar.importador;
import Consultas.clientes.model.cliente;
import Consultas.clientes.model.model;
import Formularios.controller.controllerNuevoCliente;
import javafx.application.Platform;
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

import java.io.IOException;
import java.util.ArrayList;

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
            contenedor.prefHeightProperty().bind(root.heightProperty().multiply(0.75));
            contenedorTabla.prefHeightProperty().bind(contenedor.heightProperty().multiply(0.81));
            contenidoTabla.prefHeightProperty().bind(contenedorTabla.heightProperty().multiply(0.9));

            HBox.setHgrow(expansor, Priority.ALWAYS);
            expansor.setMinWidth(10);

            buscador.prefWidthProperty().bind(root.widthProperty().multiply(0.22));
            buscador.maxHeightProperty().bind(navbar.heightProperty().multiply(0.5));

            paneNavbarController.setTitulo("Clientes", "#ffffff");

            // Cell Value Factories
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

            // Centrado
            TableColumn<cliente, String>[] columnas = new TableColumn[]{
                    colID,colNombre,colRFC,colCURP,colRazonSocial,colCorreo,colTelefono,colCP,
                    colPais,colEstado,colCiudad,colLocalidad,colColonia,colDomicilio,colNumeroExt,colNumeroInt
            };
            for (TableColumn<cliente, String> col : columnas) col.setStyle("-fx-alignment: CENTER;");

            // Botón eliminar
            colSelect.setCellFactory(col -> new TableCell<cliente, Void>() {
                private final Button btn = new Button();
                private final HBox contenedor = new HBox();

                {
                    ImageView img = new ImageView(new Image(getClass().getResourceAsStream("/img/eliminar.png")));
                    img.setFitWidth(18);
                    img.setFitHeight(18);
                    btn.setGraphic(img);
                    btn.setStyle("-fx-background-color: #333; -fx-cursor: hand;");
                    contenedor.setAlignment(javafx.geometry.Pos.CENTER);
                    contenedor.getChildren().add(btn);

                    btn.setOnAction(e -> {
                        cliente seleccionado = getTableView().getItems().get(getIndex());
                        Alert alerta = new Alert(Alert.AlertType.CONFIRMATION);
                        alerta.setContentText("¿Eliminar este cliente?");
                        alerta.showAndWait().ifPresent(r -> {
                            if (r == ButtonType.OK) {
                                model m = new model();
                                if (m.eliminarCliente(seleccionado.getId())) {
                                    cargarClientesEnTabla();
                                }
                            }
                        });
                    });
                }
                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    setGraphic(empty ? null : contenedor);
                }
            });

            // Carga Inicial
            cargarClientesEnTabla();

            // Doble clic → editar
            contenidoTabla.setRowFactory(tv -> {
                TableRow<cliente> row = new TableRow<>();
                row.setOnMouseClicked(event -> {
                    if (event.getClickCount() == 2 && !row.isEmpty()) {
                        abrirFormulario(row.getItem());
                    }
                });
                return row;
            });

            // ENTER sobre un registro → editar
            contenidoTabla.setOnKeyPressed(event -> {
                if (event.getCode().toString().equals("ENTER")) {
                    cliente c = contenidoTabla.getSelectionModel().getSelectedItem();
                    if (c != null) abrirFormulario(c);
                }
            });

            // Buscar con ENTER
            buscador.setOnKeyPressed(event -> {
                if (event.getCode().toString().equals("ENTER")) {
                    buscarCliente();
                }
            });
        });
    }

    public void cargarClientesEnTabla() {
        Platform.runLater(() -> {
            model m = new model();
            contenidoTabla.getItems().setAll(m.obtenerClientes());
        });
    }

    private void buscarCliente() {
        String texto = buscador.getText().trim();
        model m = new model();

        if (texto.isEmpty()) {
            cargarClientesEnTabla();
            return;
        }

        var resultados = m.buscarPorNombre(texto);

        if (resultados.isEmpty()) {
            new Alert(Alert.AlertType.INFORMATION,
                    "No se encontraron clientes con ese nombre.").showAndWait();
            return;
        }
        contenidoTabla.getItems().setAll(resultados);
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
    }
}
