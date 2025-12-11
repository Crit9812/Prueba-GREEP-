package Consultas.proveedores.controller;

import Compartido.controller.encabezadoController;
import Compartido.controller.navbarController;
import Compartido.exportar.exportador;
import Compartido.importar.importador;
import Consultas.proveedores.model.proveedores;
import Consultas.proveedores.model.model;
import Formularios.controller.controllerNuevoProveedor;
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

            colID.setStyle("-fx-alignment: CENTER;");
            colNombre.setStyle("-fx-alignment: CENTER;");
            colRepresentante.setStyle("-fx-alignment: CENTER;");
            colRFC.setStyle("-fx-alignment: CENTER;");
            colCURP.setStyle("-fx-alignment: CENTER;");
            colRazonSocial.setStyle("-fx-alignment: CENTER;");
            colCorreo.setStyle("-fx-alignment: CENTER;");
            colTelefono.setStyle("-fx-alignment: CENTER;");
            colCP.setStyle("-fx-alignment: CENTER;");
            colPais.setStyle("-fx-alignment: CENTER;");
            colEstado.setStyle("-fx-alignment: CENTER;");
            colCiudad.setStyle("-fx-alignment: CENTER;");
            colLocalidad.setStyle("-fx-alignment: CENTER;");
            colColonia.setStyle("-fx-alignment: CENTER;");
            colDomicilio.setStyle("-fx-alignment: CENTER;");
            colNumeroExt.setStyle("-fx-alignment: CENTER;");
            colNumeroInt.setStyle("-fx-alignment: CENTER;");

            colSelect.setCellFactory(col -> new TableCell<proveedores, Void>() {
                private final Button btn = new Button();
                private final HBox contenedor = new HBox();

                {
                    ImageView img = new ImageView(new Image(getClass().getResourceAsStream("/img/eliminar.png")));
                    img.setFitWidth(18);
                    img.setFitHeight(18);
                    img.setPreserveRatio(true);

                    btn.setGraphic(img);
                    btn.setStyle("-fx-background-color: #333; -fx-cursor: hand;");

                    contenedor.setAlignment(javafx.geometry.Pos.CENTER);
                    contenedor.getChildren().add(btn);

                    btn.setOnAction(e -> {
                        proveedores seleccionado = getTableView().getItems().get(getIndex());
                        int id = seleccionado.getId();

                        Alert alerta = new Alert(Alert.AlertType.CONFIRMATION);
                        alerta.setContentText("¿Está seguro que desea eliminar este proveedor?");

                        alerta.showAndWait().ifPresent(response -> {
                            if (response == ButtonType.OK) {
                                model m = new model();
                                if (m.eliminar(id)) {
                                    cargarEnTabla();
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
                    setGraphic(empty ? null : contenedor);
                }
            });

            cargarEnTabla();

            contenidoTabla.setRowFactory(tv -> {
                TableRow<proveedores> row = new TableRow<>();
                row.setOnMouseClicked(event -> {
                    if (event.getClickCount() == 2 && !row.isEmpty()) {
                        abrirFormulario(row.getItem());
                    }
                });
                return row;
            });

            contenidoTabla.setOnKeyPressed(event -> {
                if (event.getCode().toString().equals("ENTER")) {
                    proveedores p = contenidoTabla.getSelectionModel().getSelectedItem();
                    if (p != null) abrirFormulario(p);
                }
            });

            buscador.setOnKeyPressed(event -> {
                if (event.getCode().toString().equals("ENTER")) {
                    buscar();
                }
            });
        });
    }

    public void cargarEnTabla() {
        Platform.runLater(() -> {
            model m = new model();
            contenidoTabla.getItems().setAll(m.obtener());
        });
    }

    private void buscar() {
        String texto = buscador.getText().trim();
        model m = new model();

        if (texto.isEmpty()) {
            cargarEnTabla();
        } else {
            var resultados = m.buscarPorNombre(texto);

            if (resultados.isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Sin resultados");
                alert.setHeaderText(null);
                alert.setContentText("No se encontraron registros con ese nombre.");
                alert.showAndWait();
                return;
            }

            contenidoTabla.getItems().setAll(resultados);
        }
    }

    @FXML
    public void formularioNuevoProveedor() {
        abrirFormulario(null);
    }

    private void abrirFormulario(proveedores editar) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Formularios/view/nuevoProveedor.fxml"));
            Parent vista = loader.load();

            controllerNuevoProveedor ctrl = loader.getController();
            if (editar != null) {
                ctrl.cargarProveedor(editar);
            } else {
                ctrl.prepararNuevoProveedor();
            }

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

            cargarEnTabla();

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
    }
}
