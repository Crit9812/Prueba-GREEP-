package Reportes.historial.controller;

import Compartido.controller.encabezadoController;
import Compartido.controller.navbarController;
import Reportes.historial.exportar.ReporteMovimientoExporter;
import Reportes.historial.model.MovimientoFactura;
import Reportes.historial.model.model;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import javafx.application.Platform;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class MainController {

    @FXML private StackPane root;
    @FXML private BorderPane paneNavbar;
    @FXML private VBox navbar;
    @FXML private Pane overlayPane;
    @FXML private VBox contenedor;

    @FXML private Label lblQuitar;
    @FXML private Label lblOrdenar;
    @FXML private Label lblExportar;
    @FXML private Region expansorBusqueda;
    @FXML private TextField buscarFactura;

    @FXML private Region expansor;
    @FXML private Label lblVista;
    @FXML private Label lblDescargar;

    @FXML private VBox contenedorTabla;
    @FXML private TableView<MovimientoFactura> contenidoTabla;
    @FXML private TableColumn<MovimientoFactura, String> colClaveMovimiento;
    @FXML private TableColumn<MovimientoFactura, String> colFecha;
    @FXML private TableColumn<MovimientoFactura, String> colHora;
    @FXML private TableColumn<MovimientoFactura, String> colTipoMovimiento;
    @FXML private TableColumn<MovimientoFactura, String> colTotal;
    @FXML private TableColumn<MovimientoFactura, String> colUsuario;
    @FXML private TableColumn<MovimientoFactura, String> colExterno;
    @FXML private TableColumn<MovimientoFactura, String> colFacturaExterna;

    @FXML private encabezadoController paneNavbarController;

    private final model historialModel = new model();

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

            lblQuitar.setMinWidth(Region.USE_PREF_SIZE);
            lblOrdenar.setMinWidth(Region.USE_PREF_SIZE);
            lblExportar.setMinWidth(Region.USE_PREF_SIZE);

            HBox.setHgrow(expansorBusqueda, Priority.ALWAYS);
            expansorBusqueda.setMinWidth(10);

            buscarFactura.prefWidthProperty().bind(root.widthProperty().multiply(0.18));
            buscarFactura.prefHeightProperty().bind(navbar.heightProperty().multiply(0.04));

            HBox.setHgrow(expansor, Priority.ALWAYS);
            expansor.setMinWidth(10);

            lblVista.setMinWidth(Region.USE_PREF_SIZE);
            lblDescargar.setMinWidth(Region.USE_PREF_SIZE);

            configurarTabla();
            cargarDatos();
            configurarBuscador();
            configurarEventos();

            contenedorTabla.prefHeightProperty().bind(contenedor.heightProperty().multiply(0.78));
            contenidoTabla.prefHeightProperty().bind(contenedorTabla.heightProperty().multiply(0.9));

            paneNavbarController.setTitulo("Historial por factura", "#ffffff");
        });
    }

    private void configurarTabla() {
        colClaveMovimiento.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().getClaveMovimiento()));
        colFecha.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().getFecha()));
        colHora.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().getHora()));
        colTipoMovimiento.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().getTipoMovimiento()));
        colTotal.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().getTotal()));
        colUsuario.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().getUsuario()));
        colExterno.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().getExterno()));
        colFacturaExterna.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().getFacturaExterna()));
    }

    private void cargarDatos() {
        contenidoTabla.setItems(FXCollections.observableArrayList(historialModel.obtenerMovimientos()));
    }

    private void configurarBuscador() {
        buscarFactura.textProperty().addListener((obs, oldValue, newValue) -> {
            String texto = newValue == null ? "" : newValue.trim().toLowerCase(Locale.ROOT);
            List<MovimientoFactura> filtrados = historialModel.obtenerMovimientos().stream()
                    .filter(m -> texto.isBlank() || contiene(m, texto))
                    .collect(Collectors.toList());
            contenidoTabla.setItems(FXCollections.observableArrayList(filtrados));
        });
    }

    private boolean contiene(MovimientoFactura m, String texto) {
        return m.getClaveMovimiento().toLowerCase(Locale.ROOT).contains(texto)
                || m.getFacturaExterna().toLowerCase(Locale.ROOT).contains(texto)
                || m.getTipoMovimiento().toLowerCase(Locale.ROOT).contains(texto)
                || m.getExterno().toLowerCase(Locale.ROOT).contains(texto);
    }

    private void configurarEventos() {
        contenidoTabla.setRowFactory(tv -> {
            TableRow<MovimientoFactura> row = new TableRow<>();
            row.setOnMouseClicked(evt -> {
                if (evt.getClickCount() == 2 && !row.isEmpty()) {
                    mostrarDetalleMovimiento(row.getItem());
                }
            });
            return row;
        });

        lblDescargar.setOnMouseClicked(evt -> descargarSeleccionado());
    }

    private void descargarSeleccionado() {
        MovimientoFactura seleccionado = contenidoTabla.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            mostrarMensaje("Selecciona un movimiento para descargar su reporte.", Alert.AlertType.INFORMATION);
            return;
        }
        if (seleccionado.estaCanceladoPorCompleto()) {
            mostrarMensaje("El movimiento está cancelado por completo y no se puede descargar.", Alert.AlertType.WARNING);
            return;
        }
        ReporteMovimientoExporter.exportar(seleccionado, contenidoTabla.getScene() != null ? contenidoTabla.getScene().getWindow() : null);
    }

    private void mostrarDetalleMovimiento(MovimientoFactura movimiento) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Movimiento " + movimiento.getClaveMovimiento());
        dialog.setHeaderText("Detalle del movimiento");

        String detalle = "Tipo: " + movimiento.getTipoMovimiento() + "\n"
                + "Fecha: " + movimiento.getFecha() + " " + movimiento.getHora() + "\n"
                + "Total: " + movimiento.getTotal() + "\n"
                + "Usuario: " + movimiento.getUsuario() + "\n"
                + "Externo: " + movimiento.getExterno() + "\n"
                + "Factura: " + movimiento.getFacturaExterna() + "\n"
                + "Estado: " + movimiento.getEstado();
        dialog.getDialogPane().setContent(new Label(detalle));

        ButtonType cerrar = new ButtonType("Cerrar", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().add(cerrar);

        if (!movimiento.estaCanceladoPorCompleto()) {
            ButtonType descargar = new ButtonType("Descargar");
            dialog.getDialogPane().getButtonTypes().add(descargar);
            dialog.showAndWait().ifPresent(bt -> {
                if (bt == descargar) {
                    ReporteMovimientoExporter.exportar(movimiento, contenidoTabla.getScene() != null ? contenidoTabla.getScene().getWindow() : null);
                }
            });
            return;
        }

        dialog.showAndWait();
    }

    private void mostrarMensaje(String texto, Alert.AlertType tipo) {
        Alert alert = new Alert(tipo);
        alert.setTitle("Historial por factura");
        alert.setHeaderText(null);
        alert.setContentText(texto);
        alert.showAndWait();
    }
}
