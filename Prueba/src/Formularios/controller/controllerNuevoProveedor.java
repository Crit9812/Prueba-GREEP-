package Formularios.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class controllerNuevoProveedor {

    @FXML private TextField txtClave;
    @FXML private TextField txtProducto;
    @FXML private TextField txtCantidad;
    @FXML private TextField txtPrecioEntrada;
    @FXML private TextField txtPrecioSalida;
    @FXML private CheckBox chkIVA;
    @FXML private TextField txtPrecioIVA;
    @FXML private TextField txtPrecioBruto;
    @FXML private TextField txtPrecioTotal;
    @FXML private Button btnGuardar;
    @FXML private Button btnCancelar;

    @FXML
    public void initialize() {
    }

    @FXML
    private void abrirFormularioNuevoProducto() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Formularios/view/nuevoProducto.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Detalle de salida");
            stage.getIcons().add(new Image(getClass().getResourceAsStream("/img/logo-GREEP.png")));
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL); // Bloquea la ventana principal
            stage.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
