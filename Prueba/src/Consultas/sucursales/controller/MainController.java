package Consultas.sucursales.controller;

import Compartido.controller.encabezadoController;
import Compartido.controller.navbarController;
import Compartido.exportar.exportador;
import Compartido.importar.importador;
import Consultas.sucursales.model.sucursal;
import Consultas.sucursales.model.model;
import Formularios.controller.controllerNuevaSucursal;
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
    @FXML private Region expansor;
    @FXML private TextField buscador;
    @FXML private VBox contenedorTabla;
    @FXML private TableView<sucursal> contenidoTabla;
    @FXML private TableColumn<sucursal, Void> colSelect;
    @FXML private TableColumn<sucursal, String> colId;
    @FXML private TableColumn<sucursal, String> colNombre;
    @FXML private TableColumn<sucursal, String> colCorreo;
    @FXML private TableColumn<sucursal, String> colTelefono;
    @FXML private TableColumn<sucursal, String> colDomicilio;
    @FXML private TableColumn<sucursal, String> colCP;
    @FXML private TableColumn<sucursal, String> colColonia;
    @FXML private TableColumn<sucursal, String> colNumeroExt;
    @FXML private TableColumn<sucursal, String> colNumeroInt;
    @FXML private TableColumn<sucursal, String> colCiudad;
    @FXML private TableColumn<sucursal, String> colEstado;
    @FXML private TableColumn<sucursal, String> colLocalidad;
    @FXML private TableColumn<sucursal, String> colPais;

    @FXML private encabezadoController paneNavbarController;

    private final model m = new model();

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
            contenedorTabla.prefHeightProperty().bind(contenedor.heightProperty().multiply(0.95));
            contenidoTabla.prefHeightProperty().bind(contenedorTabla.heightProperty().multiply(0.9));

            HBox.setHgrow(expansor, Priority.ALWAYS);
            expansor.setMinWidth(10);

            buscador.prefWidthProperty().bind(root.widthProperty().multiply(0.22));
            buscador.maxHeightProperty().bind(navbar.heightProperty().multiply(0.5));

            paneNavbarController.setTitulo("Sucursales", "#ffffff");

            // CONFIGURACIÓN DE COLUMNAS (cell value factories)
            colId.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(String.valueOf(cd.getValue().getId())));
            colNombre.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().getNombre()));
            colDomicilio.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().getDomicilio()));
            colCP.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(String.valueOf(cd.getValue().getCp())));
            colColonia.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().getColonia()));
            colNumeroExt.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(String.valueOf(cd.getValue().getNumeroExt())));
            colNumeroInt.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(String.valueOf(cd.getValue().getNumeroInt())));
            colCiudad.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().getCiudad()));
            colEstado.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().getEstado()));
            colLocalidad.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().getLocalidad()));
            colPais.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().getPais()));
            colCorreo.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().getCorreo()));
            colTelefono.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(String.valueOf(cd.getValue().getTelefono())));

            // ALINEACIÓN DE COLUMNAS
            colId.setStyle("-fx-alignment: CENTER;");
            colNombre.setStyle("-fx-alignment: CENTER;");
            colDomicilio.setStyle("-fx-alignment: CENTER;");
            colCP.setStyle("-fx-alignment: CENTER;");
            colColonia.setStyle("-fx-alignment: CENTER;");
            colNumeroExt.setStyle("-fx-alignment: CENTER;");
            colNumeroInt.setStyle("-fx-alignment: CENTER;");
            colCiudad.setStyle("-fx-alignment: CENTER;");
            colEstado.setStyle("-fx-alignment: CENTER;");
            colLocalidad.setStyle("-fx-alignment: CENTER;");
            colPais.setStyle("-fx-alignment: CENTER;");
            colCorreo.setStyle("-fx-alignment: CENTER;");
            colTelefono.setStyle("-fx-alignment: CENTER;");

            // BOTÓN ELIMINAR
            colSelect.setCellFactory(col -> new TableCell<sucursal, Void>() {
                private final Button btn = new Button();
                private final HBox contenedorBtn = new HBox();

                {
                    ImageView img = new ImageView(new Image(getClass().getResourceAsStream("/img/eliminar.png")));
                    img.setFitWidth(18);
                    img.setFitHeight(18);
                    img.setPreserveRatio(true);

                    btn.setGraphic(img);
                    btn.setStyle("-fx-background-color: #333; -fx-cursor: hand;");

                    contenedorBtn.setSpacing(0);
                    contenedorBtn.setAlignment(javafx.geometry.Pos.CENTER);
                    contenedorBtn.getChildren().add(btn);

                    btn.setOnAction(e -> {
                        sucursal sel = getTableView().getItems().get(getIndex());
                        int id = sel.getId();

                        Alert alerta = new Alert(Alert.AlertType.CONFIRMATION);
                        alerta.setContentText("¿Está seguro que desea eliminar esta sucursal?");
                        alerta.showAndWait().ifPresent(r -> {
                            if (r == ButtonType.OK) {
                                if (m.eliminarSucursal(id)) {
                                    // refrescar tabla
                                    cargarSucursalesEnTabla();
                                } else {
                                    new Alert(Alert.AlertType.ERROR, "No se pudo eliminar la sucursal").showAndWait();
                                }
                            }
                        });
                    });
                }

                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    setGraphic(empty ? null : contenedorBtn);
                }
            });

            // Carga inicial
            cargarSucursalesEnTabla();

            // Doble clic → abrir edición (usa abrirFormulario para mantener consistencia)
            contenidoTabla.setRowFactory(tv -> {
                TableRow<sucursal> row = new TableRow<>();
                row.setOnMouseClicked(evt -> {
                    if (evt.getClickCount() == 2 && !row.isEmpty()) {
                        abrirFormulario(row.getItem());
                    }
                });
                return row;
            });

            // ENTER sobre un registro → abrir edición
            contenidoTabla.setOnKeyPressed(evt -> {
                if (evt.getCode().toString().equals("ENTER")) {
                    sucursal sel = contenidoTabla.getSelectionModel().getSelectedItem();
                    if (sel != null) abrirFormulario(sel);
                }
            });

            // Buscar con ENTER
            buscador.setOnKeyPressed(evt -> {
                if (evt.getCode().toString().equals("ENTER")) {
                    buscarSucursal();
                }
            });

        });
    }

    public void cargarSucursalesEnTabla() {
        Platform.runLater(() -> {
            if (contenidoTabla != null) {
                contenidoTabla.getItems().setAll(m.obtenerSucursales());
            }
        });
    }

    private void buscarSucursal() {
        String texto = buscador.getText().trim();

        if (texto.isEmpty()) {
            cargarSucursalesEnTabla();
            return;
        }

        var resultados = m.buscarPorNombre(texto);

        if (resultados == null || resultados.isEmpty()) {
            new Alert(Alert.AlertType.INFORMATION,
                    "No se encontraron sucursales con ese nombre.").showAndWait();
            return;
        }

        contenidoTabla.getItems().setAll(resultados);
    }

    @FXML
    public void formularioNuevaSucursal() {
        abrirFormulario(null);
    }

    private void abrirFormulario(sucursal sucursalEditar) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Formularios/view/nuevaSucursal.fxml"));
            Parent view = loader.load();

            controllerNuevaSucursal ctrl = loader.getController();

            // Preparar stage modal
            Stage stage = new Stage();
            stage.setScene(new Scene(view));
            stage.setResizable(false);
            stage.centerOnScreen();
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(root.getScene().getWindow());

            if (sucursalEditar == null) {
                // modo nuevo
                ctrl.prepararNuevaSucursal();
                stage.setTitle("Nueva Sucursal");
            } else {
                // modo edición
                ctrl.cargarSucursal(sucursalEditar);
                stage.setTitle("Editar Sucursal");
            }

            stage.showAndWait();

            // recargar tabla al cerrar
            cargarSucursalesEnTabla();

        } catch (Exception ex) {
            ex.printStackTrace();
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
                exportador.exportarTabla(contenidoTabla, "Sucursales", "pdf");
            } else if (res == btnExcel) {
                exportador.exportarTabla(contenidoTabla, "Sucursales", "excel");
            }
        });
    }

    public void importarDatos() {
        importador.importarExcel("sucursales", "id");
    }
}
