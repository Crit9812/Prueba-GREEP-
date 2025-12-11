package Consultas.claves.controller;

import Compartido.controller.encabezadoController;
import Compartido.controller.navbarController;
import Compartido.exportar.exportador;
import Compartido.importar.importador;
import Consultas.claves.model.model;
import Formularios.controller.controllerSincronizacionClaves;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
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
    @FXML private TableView<String[]> contenidoTabla;
    @FXML private TableColumn<String[], String> colSelect;
    @FXML private TableColumn<String[], String> colClaveProducto;
    @FXML private TableColumn<String[], String> colProducto;
    @FXML private TableColumn<String[], String> colIDProvedor; // id proveedor (nuevo)
    @FXML private TableColumn<String[], String> colProveedor;
    @FXML private TableColumn<String[], String> colClaveAlterna;
    @FXML private TableColumn<String[], String> colDescripcion;
    @FXML private TextField buscador;
    @FXML private Region expansor;
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

            contenedor.prefHeightProperty().bind(root.heightProperty().multiply(0.75));

            contenedorTabla.prefHeightProperty().bind(contenedor.heightProperty().multiply(0.81));
            contenidoTabla.prefHeightProperty().bind(contenedorTabla.heightProperty().multiply(0.9));

            paneNavbarController.setTitulo("Claves", "#ffffff");

            // Mapeo columnas (el arreglo tiene este orden):
            // 0 -> idClaveCatalogo (clave alterna)
            // 1 -> claveGreep (clave del producto)
            // 2 -> producto (nombre)
            // 3 -> proveedor_id (id proveedor)
            // 4 -> proveedor (nombre)
            // 5 -> descripcion (concatenada)
            colClaveAlterna.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue()[0]));
            colClaveProducto.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue()[1]));
            colProducto.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue()[2]));
            colIDProvedor.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue()[3]));
            colProveedor.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue()[4]));
            colDescripcion.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue()[5]));

            // Cargar tabla al iniciar
            cargarEnTabla();

            buscador.setOnKeyPressed(event -> {
                switch (event.getCode()) {
                    case ENTER -> buscar();
                }
            });
        });
    }

    public void cargarEnTabla() {
        Platform.runLater(() -> {
            model m = new model();
            contenidoTabla.getItems().setAll(m.obtenerParaTabla());
        });
    }

    private void buscar() {
        String texto = buscador.getText().trim();
        if (texto.isEmpty()) {
            cargarEnTabla();
            return;
        }
        model m = new model();
        var resultados = m.obtenerParaTabla();
        var filtrados = resultados.stream()
                .filter(f -> {
                    // filtramos por nombre de producto (posición 2) o por proveedor (posición 4) o clave alterna (posición 0)
                    String prod = f[2] == null ? "" : f[2];
                    String prov = f[4] == null ? "" : f[4];
                    String claveAlt = f[0] == null ? "" : f[0];
                    return prod.toLowerCase().contains(texto.toLowerCase())
                            || prov.toLowerCase().contains(texto.toLowerCase())
                            || claveAlt.toLowerCase().contains(texto.toLowerCase());
                })
                .toList();
        contenidoTabla.getItems().setAll(filtrados);
    }

    @FXML
    public void formularioNuevaSincronizacionClaves() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Formularios/view/sincronizarClaves.fxml"));
            Parent vista = loader.load();

            controllerSincronizacionClaves ctrl = loader.getController();

            Stage stage = new Stage();
            stage.setTitle("Sincronizar Claves");
            stage.setScene(new Scene(vista));
            stage.setResizable(false);
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(root.getScene().getWindow());
            stage.showAndWait();

            cargarEnTabla();
        } catch (IOException e) {
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
                exportador.exportarTabla(contenidoTabla, "Claves", "pdf");
            } else if (res == btnExcel) {
                exportador.exportarTabla(contenidoTabla, "Claves", "excel");
            }
        });
    }

    public void importarDatos() {
        importador.importarExcel("claves", "idClaveCatalogo");
    }
}
