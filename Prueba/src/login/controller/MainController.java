package login.controller;

import controllerInterfaz.ControllerInterfaz;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.control.TextField;
import javafx.util.Duration;
import login.model.Model;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Button;
import Compartido.controller.alertaController;

public class MainController {

    @FXML private StackPane root;
    @FXML private ImageView backgroundImage;
    @FXML private HBox contenedor;
    @FXML private ImageView logoImage;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private TextField passwordVisibleField;
    @FXML private Button togglePasswordButton;
    @FXML private Region expansor;
    @FXML private VBox formulario;
    @FXML private alertaController alertaController;
    @FXML private Button botonOcultarContrasena;

    private boolean botonActivo = false;
    private boolean contrasenaVisible = false;

    @FXML
    public void initialize() {
        Platform.runLater(() -> {
            backgroundImage.fitWidthProperty().bind(root.widthProperty());
            backgroundImage.fitHeightProperty().bind(root.heightProperty());

            contenedor.maxHeightProperty().bind(root.heightProperty().multiply(0.65));
            contenedor.maxWidthProperty().bind(root.widthProperty().multiply(0.8));

            HBox.setHgrow(expansor, Priority.SOMETIMES);
            expansor.minWidthProperty().bind(root.widthProperty().multiply(0.05));
            expansor.maxWidthProperty().bind(root.widthProperty().multiply(0.06));

            usernameField.maxWidthProperty().bind(root.widthProperty().multiply(0.2));
            usernameField.prefHeightProperty().bind(root.heightProperty().multiply(0.05));

            passwordField.maxWidthProperty().bind(usernameField.widthProperty());
            passwordField.prefHeightProperty().bind(usernameField.heightProperty());

            passwordVisibleField.maxWidthProperty().bind(usernameField.widthProperty());
            passwordVisibleField.prefHeightProperty().bind(usernameField.heightProperty());

            botonOcultarContrasena.prefHeightProperty().bind(usernameField.heightProperty());


            logoImage.fitHeightProperty().bind(contenedor.heightProperty().multiply(0.65));
            logoImage.fitWidthProperty().bind(contenedor.widthProperty().multiply(0.28));

            animarInicio();
            usernameField.setOnAction(event -> iniciarSesion());
            passwordField.setOnAction(event -> iniciarSesion());
            passwordVisibleField.setOnAction(event -> iniciarSesion());

        });
    }

    @FXML
    public void togglePasswordVisibility() {
        // Alternar visibilidad de la contraseña
        if (contrasenaVisible) {
            passwordField.setText(passwordVisibleField.getText());
            passwordVisibleField.setVisible(false);
            passwordVisibleField.setManaged(false);
            passwordField.setVisible(true);
            passwordField.setManaged(true);
        } else {
            passwordVisibleField.setText(passwordField.getText());
            passwordField.setVisible(false);
            passwordField.setManaged(false);
            passwordVisibleField.setVisible(true);
            passwordVisibleField.setManaged(true);
        }
        contrasenaVisible = !contrasenaVisible;

        // Cambiar color de fondo del botón usando el flag
        if (botonActivo) {
            botonOcultarContrasena.setStyle("-fx-background-color: #fff; -fx-border-color: #b2b2b2; -fx-border-width: 1px;");
        } else {
            botonOcultarContrasena.setStyle("-fx-background-color: #d2d2d2; -fx-border-color: #b2b2b2; -fx-border-width: 1px;");
        }
        botonActivo = !botonActivo;
    }

    private void animarInicio() {
        formulario.setOpacity(0);
        contenedor.setOpacity(0);

        logoImage.setVisible(true);
        contenedor.setOpacity(1);

        double originalTranslateX = logoImage.getTranslateX();
        double originalTranslateY = logoImage.getTranslateY();

        logoImage.setTranslateX(root.getWidth() / 2 - logoImage.getLayoutX() - logoImage.getFitWidth() / 2);
        logoImage.setTranslateY(root.getHeight() / 2 - logoImage.getLayoutY() - logoImage.getFitHeight() / 2);

        TranslateTransition moverLogo = new TranslateTransition(Duration.seconds(1.8), logoImage);
        moverLogo.setToX(originalTranslateX);
        moverLogo.setToY(originalTranslateY);
        moverLogo.setInterpolator(Interpolator.EASE_BOTH);

        FadeTransition aparecerFormulario = new FadeTransition(Duration.seconds(1.2), formulario);
        aparecerFormulario.setFromValue(0);
        aparecerFormulario.setToValue(1);
        aparecerFormulario.setInterpolator(Interpolator.EASE_IN);

        SequentialTransition secuencia = new SequentialTransition(
                moverLogo,
                aparecerFormulario
        );

        secuencia.play();
    }

    @FXML
    public void iniciarSesion() {
        Model modelo = new Model();
        String username = usernameField.getText();
        String password = contrasenaVisible ? passwordVisibleField.getText() : passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            alertaController.mostrarAlerta("Error", "Por favor, ingresa usuario y contraseña. \n(Campos vacíos).");
        }
        else{
            if(modelo.verificarUsuario(username, password )){

                Compartido.sesion.SesionUsuario.setNombreUsuario(username);
                Operaciones.controller.MainController controlador = new Operaciones.controller.MainController();
                //VentanaFalsa.controller.MainController controlador = new VentanaFalsa.controller.MainController();
                ControllerInterfaz.cambiarVista(
                        "/Operaciones/view/main_view.fxml",
                        "/Operaciones/style/estilos.css",
                        controlador/*
                        "/VentanaFalsa/view/main_view.fxml",
                        "/VentanaFalsa/style/estilos.css",
                        controlador*/
                );
            }
            else{
                alertaController.mostrarAlerta("Error", "Usuario o contraseña incorrectos");
            }
        }
    }
}
