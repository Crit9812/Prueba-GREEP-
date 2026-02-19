package Compartido.helper;

import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

public class OverlayCarga {

    private final StackPane root;
    private final Pane overlayPane;
    private StackPane overlayCarga;
    private boolean listenerOverlayActivo = false;

    public OverlayCarga(StackPane root, Pane overlayPane) {
        this.root = root;
        this.overlayPane = overlayPane;
        configurarOverlayCarga();
    }

    private void configurarOverlayCarga() {
        if (overlayPane == null || root == null || overlayCarga != null) {
            return;
        }

        Label labelCarga = new Label("Cargando...");
        labelCarga.setStyle("-fx-text-fill: white; -fx-font-size: 26px; -fx-font-weight: bold;");

        overlayCarga = new StackPane(labelCarga);
        overlayCarga.setVisible(false);
        overlayCarga.setManaged(false);
        overlayCarga.setMouseTransparent(true);
        overlayCarga.setPickOnBounds(true);
        overlayCarga.setStyle("-fx-background-color: rgba(0, 0, 0, 0.55);");
        overlayCarga.setAlignment(Pos.CENTER);

        overlayCarga.prefWidthProperty().bind(root.widthProperty());
        overlayCarga.prefHeightProperty().bind(root.heightProperty());
        overlayCarga.setMinWidth(Region.USE_PREF_SIZE);
        overlayCarga.setMinHeight(Region.USE_PREF_SIZE);

        overlayPane.getChildren().add(overlayCarga);
    }


    private void asegurarOverlaySiempreAlFrente() {
        if (listenerOverlayActivo || root == null || overlayPane == null) {
            return;
        }

        listenerOverlayActivo = true;
        root.getChildren().addListener((ListChangeListener<javafx.scene.Node>) change -> {
            if (overlayCarga != null && overlayCarga.isVisible()) {
                overlayPane.toFront();
            }
        });
    }

    public void mostrar() {
        if (overlayCarga == null) {
            configurarOverlayCarga();
        }
        if (overlayCarga == null) {
            return;
        }
        Platform.runLater(() -> {
            overlayCarga.setManaged(true);
            overlayCarga.setVisible(true);
            overlayCarga.setMouseTransparent(false);
            asegurarOverlaySiempreAlFrente();
            overlayPane.toFront();
            overlayCarga.toFront();
        });
    }

    public void ocultar() {
        if (overlayCarga == null) {
            return;
        }
        Platform.runLater(() -> {
            overlayCarga.setVisible(false);
            overlayCarga.setManaged(false);
            overlayCarga.setMouseTransparent(true);
        });
    }
}
