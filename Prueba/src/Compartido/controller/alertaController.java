package Compartido.controller;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public class alertaController {

    @FXML private VBox capaAlerta;
    @FXML private Label alertaTitulo;
    @FXML private Label alertaMensaje;
    @FXML private VBox alertaModal;

    @FXML public void initialize() {
        Platform.runLater(() -> {
            alertaModal.maxWidthProperty().bind(capaAlerta.widthProperty().multiply(0.30));
            alertaModal.maxHeightProperty().bind(capaAlerta.heightProperty().multiply(0.25));
        });
    }

    public void mostrarAlerta(String titulo, String mensaje) {

        alertaTitulo.setText(titulo);
        alertaMensaje.setText(mensaje);
        capaAlerta.setVisible(true);
        capaAlerta.setOpacity(1);
        FadeTransition ft = new FadeTransition(Duration.millis(200), capaAlerta);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();
    }


    @FXML private void cerrarAlerta() {
        FadeTransition ft = new FadeTransition(Duration.millis(200), capaAlerta);
        ft.setFromValue(1);
        ft.setToValue(0);
        ft.setOnFinished(e -> capaAlerta.setVisible(false));
        ft.play();
    }
}
