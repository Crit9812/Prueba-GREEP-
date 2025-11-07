package login.controller;

import controllerInterfaz.ControllerInterfaz;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.control.TextField;
import javafx.util.Duration;

public class MainController {

    @FXML private StackPane root;
    @FXML private ImageView backgroundImage;
    @FXML private HBox contenedor;
    @FXML private ImageView logoImage;
    @FXML private TextField usernameField;
    @FXML private TextField passwordField;
    @FXML private Region expansor;
    @FXML private VBox formulario;

    @FXML
    public void initialize() {
        Platform.runLater(() -> {
            // Enlaces originales (sin cambios)
            backgroundImage.fitWidthProperty().bind(root.widthProperty());
            backgroundImage.fitHeightProperty().bind(root.heightProperty());

            contenedor.maxHeightProperty().bind(root.heightProperty().multiply(0.65));
            contenedor.maxWidthProperty().bind(root.widthProperty().multiply(0.8));

            HBox.setHgrow(expansor, Priority.SOMETIMES);

            expansor.minWidthProperty().bind(root.widthProperty().multiply(0.05));
            expansor.maxWidthProperty().bind(root.widthProperty().multiply(0.06));

            usernameField.maxWidthProperty().bind(root.widthProperty().multiply(0.2));
            usernameField.prefHeightProperty().bind(root.heightProperty().multiply(0.05));

            passwordField.maxWidthProperty().bind(root.widthProperty().multiply(0.2));
            passwordField.prefHeightProperty().bind(root.heightProperty().multiply(0.05));

            logoImage.fitHeightProperty().bind(contenedor.heightProperty().multiply(0.65));
            logoImage.fitWidthProperty().bind(contenedor.widthProperty().multiply(0.28));

            // ---- Animación ----
            animarInicio();
        });
    }

    private void animarInicio() {
        // Estado inicial: formulario oculto
        formulario.setOpacity(0);
        contenedor.setOpacity(0); // Oculta todo el contenedor primero

        // Muestra solo fondo y logo
        logoImage.setVisible(true);
        contenedor.setOpacity(1);

        // Guarda la posición original del logo
        double originalTranslateX = logoImage.getTranslateX();
        double originalTranslateY = logoImage.getTranslateY();

        // Centra el logo temporalmente
        logoImage.setTranslateX(root.getWidth() / 2 - logoImage.getLayoutX() - logoImage.getFitWidth() / 2);
        logoImage.setTranslateY(root.getHeight() / 2 - logoImage.getLayoutY() - logoImage.getFitHeight() / 2);

        // Animación para mover el logo a su lugar original
        TranslateTransition moverLogo = new TranslateTransition(Duration.seconds(1.8), logoImage);
        moverLogo.setToX(originalTranslateX);
        moverLogo.setToY(originalTranslateY);
        moverLogo.setInterpolator(Interpolator.EASE_BOTH);

        // Aparecer formulario poco a poco
        FadeTransition aparecerFormulario = new FadeTransition(Duration.seconds(1.2), formulario);
        aparecerFormulario.setFromValue(0);
        aparecerFormulario.setToValue(1);
        aparecerFormulario.setInterpolator(Interpolator.EASE_IN);

        // Secuencia completa
        SequentialTransition secuencia = new SequentialTransition(
                moverLogo,
                aparecerFormulario
        );

        secuencia.play();
    }

    @FXML
    public void ventanaOperaciones() {
        Operaciones.controller.MainController controlador = new Operaciones.controller.MainController();
        ControllerInterfaz.cambiarVista(
                "/Operaciones/view/main_view.fxml",
                "/Operaciones/style/estilos.css",
                controlador
        );
    }
}
