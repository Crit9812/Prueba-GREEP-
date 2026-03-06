package Compartido.helper;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

public class OverlayCarga {

    private final StackPane root;
    private final Pane overlayPane;
    private StackPane overlayCarga;
    private Label labelCarga;
    private String mensaje = "Cargando...";

    public OverlayCarga(StackPane root, Pane overlayPane) {
        this.root = root;
        this.overlayPane = overlayPane;
        configurarOverlayCarga();
    }

    public OverlayCarga(StackPane root, Pane overlayPane, String mensaje) {
        this.root = root;
        this.overlayPane = overlayPane;
        this.mensaje = (mensaje == null || mensaje.trim().isEmpty()) ? "Cargando..." : mensaje;
        configurarOverlayCarga();
    }

    private StackPane obtenerContenedorGlobal() {
        StackPane objetivo = root;
        Parent actual = root;

        while (actual != null) {
            if (actual instanceof StackPane) {
                objetivo = (StackPane) actual;
            }
            actual = actual.getParent();
        }

        return objetivo;
    }

    private void bindearDimensionAContenedor(Region region, Region contenedor) {
        if (region == null || contenedor == null) {
            return;
        }

        if (region.prefWidthProperty().isBound()) {
            region.prefWidthProperty().unbind();
        }
        if (region.prefHeightProperty().isBound()) {
            region.prefHeightProperty().unbind();
        }

        region.prefWidthProperty().bind(contenedor.widthProperty());
        region.prefHeightProperty().bind(contenedor.heightProperty());
        region.setMinWidth(Region.USE_PREF_SIZE);
        region.setMinHeight(Region.USE_PREF_SIZE);
    }

    private void asegurarOverlayPaneEnRoot() {
        if (overlayPane == null || root == null) {
            return;
        }

        Runnable asegurar = () -> {
            StackPane contenedorGlobal = obtenerContenedorGlobal();

            if (overlayPane.getParent() != contenedorGlobal) {
                if (overlayPane.getParent() instanceof Pane) {
                    Pane parentPane = (Pane) overlayPane.getParent();
                    parentPane.getChildren().remove(overlayPane);
                }
                contenedorGlobal.getChildren().add(overlayPane);
            }

            bindearDimensionAContenedor(overlayPane, contenedorGlobal);
            if (overlayCarga != null) {
                bindearDimensionAContenedor(overlayCarga, contenedorGlobal);
            }
        };

        if (Platform.isFxApplicationThread()) {
            asegurar.run();
        } else {
            Platform.runLater(asegurar);
        }
    }

    private void configurarOverlayCarga() {
        if (overlayPane == null || root == null || overlayCarga != null) {
            return;
        }

        labelCarga = new Label(mensaje);
        labelCarga.setStyle("-fx-text-fill: white; -fx-font-size: 26px; -fx-font-weight: bold;");

        overlayCarga = new StackPane(labelCarga);
        overlayCarga.setVisible(false);
        overlayCarga.setManaged(false);
        overlayCarga.setMouseTransparent(true);
        overlayCarga.setPickOnBounds(true);
        overlayCarga.setStyle("-fx-background-color: rgba(0, 0, 0, 0.55);");
        overlayCarga.setAlignment(Pos.CENTER);

        overlayPane.setPickOnBounds(false);
        overlayPane.setMouseTransparent(true);
        if (!overlayPane.getChildren().contains(overlayCarga)) {
            overlayPane.getChildren().add(overlayCarga);
        }

        asegurarOverlayPaneEnRoot();
    }

    public void setMensaje(String mensaje) {
        String nuevoMensaje = (mensaje == null || mensaje.trim().isEmpty()) ? "Cargando..." : mensaje;
        this.mensaje = nuevoMensaje;

        Runnable actualizarTexto = () -> {
            if (labelCarga != null) {
                labelCarga.setText(nuevoMensaje);
            }
        };

        if (Platform.isFxApplicationThread()) {
            actualizarTexto.run();
        } else {
            Platform.runLater(actualizarTexto);
        }
    }

    public void mostrar() {
        if (overlayCarga == null) {
            configurarOverlayCarga();
        }
        if (overlayCarga == null) {
            return;
        }

        Runnable mostrarOverlay = () -> {
            asegurarOverlayPaneEnRoot();
            StackPane contenedorGlobal = obtenerContenedorGlobal();

            overlayCarga.setManaged(true);
            overlayCarga.setVisible(true);
            overlayPane.setPickOnBounds(true);
            overlayPane.setMouseTransparent(false);
            overlayCarga.setMouseTransparent(false);
            if (overlayPane.getParent() == contenedorGlobal) {
                int ultimoIndice = contenedorGlobal.getChildren().size() - 1;
                if (ultimoIndice >= 0 && contenedorGlobal.getChildren().get(ultimoIndice) != overlayPane) {
                    contenedorGlobal.getChildren().remove(overlayPane);
                    contenedorGlobal.getChildren().add(overlayPane);
                }
            }
            if (overlayCarga.getParent() == overlayPane) {
                int ultimoIndiceOverlay = overlayPane.getChildren().size() - 1;
                if (ultimoIndiceOverlay >= 0 && overlayPane.getChildren().get(ultimoIndiceOverlay) != overlayCarga) {
                    overlayPane.getChildren().remove(overlayCarga);
                    overlayPane.getChildren().add(overlayCarga);
                }
            }
        };

        if (Platform.isFxApplicationThread()) {
            mostrarOverlay.run();
        } else {
            Platform.runLater(mostrarOverlay);
        }
    }

    public void ocultar() {
        if (overlayCarga == null) {
            return;
        }

        Runnable ocultarOverlay = () -> {
            overlayCarga.setVisible(false);
            overlayCarga.setManaged(false);
            overlayCarga.setMouseTransparent(true);
            overlayPane.setMouseTransparent(true);
            overlayPane.setPickOnBounds(false);
        };

        if (Platform.isFxApplicationThread()) {
            ocultarOverlay.run();
        } else {
            Platform.runLater(ocultarOverlay);
        }
    }
}
