package Formularios.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.input.MouseEvent;

public class controllerNuevoProducto {

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

    // 🔹 Contenedor donde se apilan las filas
    @FXML private VBox contenedorFilas;

    private int contadorFilas = 1; // ya hay una fila inicial en el FXML

    @FXML
    public void initialize() {
    }

    // 🔹 Método llamado desde FXML
    @FXML
    private void crearFila(MouseEvent event) {
        if (contadorFilas >= 5) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Solo puedes agregar hasta 5 unidades de medidad.", ButtonType.OK);
            alert.showAndWait();
            return;
        }

        HBox fila = new HBox(10);

        VBox vbValor = new VBox(5);
        Label lblValor = new Label("Valor:");
        TextField txtValor = new TextField();
        txtValor.setPrefWidth(100);
        txtValor.setMaxWidth(100);
        vbValor.getChildren().addAll(lblValor, txtValor);

        VBox vbUM = new VBox(5);
        Label lblUM = new Label("UM:");
        TextField txtUM = new TextField();
        txtUM.setPrefWidth(100);
        txtUM.setMaxWidth(100);
        vbUM.getChildren().addAll(lblUM, txtUM);

        fila.getChildren().addAll(vbValor, vbUM);

        contenedorFilas.getChildren().add(fila);

        contadorFilas++;
    }

    @FXML
    private void abrirSincronzarClave() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Formularios/view/sincronizarClaves.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("compra emergente");
            stage.getIcons().add(new Image(getClass().getResourceAsStream("/img/logo-GREEP.png")));
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
