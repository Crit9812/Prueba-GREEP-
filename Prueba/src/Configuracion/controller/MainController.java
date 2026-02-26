package Configuracion.controller;

import Compartido.model.IvaConfigService;
import VentanaPrincipal.controller.ControladorVista;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;

public class MainController implements ControladorVista {

    @FXML private StackPane root;
    @FXML private VBox contenedor;
    @FXML private TextField txtIva;
    @FXML private Label lblIvaActual;
    @FXML private Button btnGuardarIva;
    @FXML private Button btnRecargarIva;

    private StackPane contentArea;
    private VentanaPrincipal.controller.MainController controladorPrincipal;

    @FXML
    public void initialize() {
        if (contenedor != null && root != null) {
            contenedor.prefHeightProperty().bind(root.heightProperty());
            contenedor.prefWidthProperty().bind(root.widthProperty());
        }
        SplitPane.setResizableWithParent(contenedor, true);

        configurarValidaciones();
        recargarIva();
    }

    @FXML
    private void guardarIva() {
        BigDecimal nuevoIva;
        try {
            nuevoIva = new BigDecimal(txtIva.getText().trim());
        } catch (Exception e) {
            mostrarAlerta("Advertencia", "El IVA debe ser numérico.");
            return;
        }

        if (nuevoIva.compareTo(BigDecimal.ZERO) < 0) {
            mostrarAlerta("Advertencia", "El IVA no puede ser negativo.");
            return;
        }

        boolean actualizado = IvaConfigService.actualizarEnBaseDatos(nuevoIva);
        if (!actualizado) {
            mostrarAlerta("Error", "No se pudo actualizar el IVA en base de datos.");
            return;
        }

        mostrarAlerta("Éxito", "IVA actualizado correctamente.");
        refrescarVistaIva();
    }

    @FXML
    private void recargarIva() {
        IvaConfigService.recargarDesdeBaseDatos();
        refrescarVistaIva();
    }

    private void refrescarVistaIva() {
        BigDecimal iva = IvaConfigService.getIvaPorcentaje();
        if (txtIva != null) {
            txtIva.setText(iva.stripTrailingZeros().toPlainString());
        }
        if (lblIvaActual != null) {
            lblIvaActual.setText("IVA actual: " + iva.stripTrailingZeros().toPlainString() + "%");
        }
    }

    private void configurarValidaciones() {
        if (txtIva == null) {
            return;
        }
        txtIva.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) {
                return;
            }
            String limpio = newVal.replace(',', '.');
            if (!limpio.matches("^\\d{0,3}(\\.\\d{0,2})?$")) {
                txtIva.setText(oldVal);
            } else if (!limpio.equals(newVal)) {
                txtIva.setText(limpio);
            }
        });
    }

    private void mostrarAlerta(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    @Override
    public void setContentArea(StackPane contentArea) {
        this.contentArea = contentArea;
    }

    @Override
    public void setControladorPrincipal(VentanaPrincipal.controller.MainController controladorPrincipal) {
        this.controladorPrincipal = controladorPrincipal;
    }
}
