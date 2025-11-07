package Compartido.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.image.ImageView;
import javafx.application.Platform;

public class encabezadoController {

    @FXML private TextField searchBar;
    @FXML private BorderPane panel;
    @FXML private ImageView iconoNavbar;
    @FXML private ImageView iconoNavbar2;
    @FXML private Label labelUsuario;
    @FXML private Label labelTitulo;

    @FXML
    public void initialize(){
        Platform.runLater(() -> {

            searchBar.prefWidthProperty().bind(panel.widthProperty().multiply(0.23));
            searchBar.prefHeightProperty().bind(panel.heightProperty().multiply(0.49));

            labelUsuario.prefWidthProperty().bind(panel.widthProperty().multiply(0.08));
            labelUsuario.prefHeightProperty().bind(panel.heightProperty().multiply(0.8));

            iconoNavbar.fitHeightProperty().bind(panel.heightProperty().multiply(0.48));
            iconoNavbar.fitWidthProperty().bind(panel.widthProperty().multiply(0.028));

            iconoNavbar2.fitHeightProperty().bind(panel.heightProperty().multiply(0.48));
            iconoNavbar2.fitWidthProperty().bind(panel.widthProperty().multiply(0.028));

            labelTitulo.prefWidthProperty().bind(panel.widthProperty().multiply(0.15));
            labelTitulo.prefHeightProperty().bind(panel.heightProperty().multiply(0.5));

        });

    }

    public void setTitulo(String titulo, String colorHex) {
        labelTitulo.setText(titulo);
        labelTitulo.setStyle("-fx-background-color: " + colorHex + ";");
    }
}



