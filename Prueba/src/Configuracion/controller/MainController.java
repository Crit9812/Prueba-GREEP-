package Configuracion.controller;

import Compartido.config.ConfiguracionFiscal;
import VentanaPrincipal.controller.ControladorVista;
import VentanaPrincipal.controller.EnumVistas;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class MainController implements ControladorVista {

    @FXML private StackPane root;
    @FXML private VBox contenedor; // Este es el contenedor específico de Configuración
    @FXML private TextField txtIvaPorcentaje;
    @FXML private Button btnGuardarIva;

    private StackPane contentArea; // El contentArea de la ventana principal
    private VentanaPrincipal.controller.MainController controladorPrincipal;

    @FXML
    public void initialize() {

        // Aquí puedes agregar lógica específica de Configuración
        // Por ejemplo, bindear tamaños si es necesario
        if (contenedor != null && root != null) {
            contenedor.prefHeightProperty().bind(root.heightProperty());
            contenedor.prefWidthProperty().bind(root.widthProperty());
        }
        SplitPane.setResizableWithParent(contenedor, true);

        inicializarConfiguracionIva();
    }

    private void inicializarConfiguracionIva() {
        if (txtIvaPorcentaje == null) {
            return;
        }

        txtIvaPorcentaje.setText(formatearPorcentaje(ConfiguracionFiscal.getIvaPorcentaje()));

        ConfiguracionFiscal.ivaPorcentajeProperty().addListener((obs, oldVal, newVal) -> {
            if (!txtIvaPorcentaje.isFocused()) {
                txtIvaPorcentaje.setText(formatearPorcentaje(newVal.doubleValue()));
            }
        });

        txtIvaPorcentaje.setOnAction(event -> guardarIva());
        if (btnGuardarIva != null) {
            btnGuardarIva.setOnAction(event -> guardarIva());
        }
    }

    @FXML
    private void guardarIva() {
        if (txtIvaPorcentaje == null) {
            return;
        }

        String texto = txtIvaPorcentaje.getText() != null ? txtIvaPorcentaje.getText().trim().replace(',', '.') : "";
        if (texto.isBlank()) {
            mostrarAlerta("Advertencia", "Ingresa un porcentaje de IVA válido.");
            return;
        }

        try {
            double porcentaje = Double.parseDouble(texto);
            if (porcentaje < 0 || porcentaje > 100) {
                mostrarAlerta("Advertencia", "El IVA debe estar entre 0 y 100.");
                return;
            }
            ConfiguracionFiscal.setIvaPorcentaje(porcentaje);
            txtIvaPorcentaje.setText(formatearPorcentaje(ConfiguracionFiscal.getIvaPorcentaje()));
            mostrarAlerta("Éxito", "Porcentaje de IVA actualizado.");
        } catch (NumberFormatException e) {
            mostrarAlerta("Error", "El porcentaje de IVA debe ser numérico.");
        }
    }

    private String formatearPorcentaje(double porcentaje) {
        return String.format(java.util.Locale.US, "%.2f", porcentaje);
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
