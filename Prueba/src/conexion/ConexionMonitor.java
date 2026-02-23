package conexion;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.stage.Popup;
import javafx.stage.Stage;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class ConexionMonitor {
    private static final ConexionMonitor INSTANCE = new ConexionMonitor();

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "conexion-monitor");
        thread.setDaemon(true);
        return thread;
    });

    private final AtomicBoolean monitorIniciado = new AtomicBoolean(false);
    private final AtomicBoolean reconectando = new AtomicBoolean(false);

    private final Popup popup = new Popup();
    private final Button botonReconectar = new Button("Conectar");
    private volatile boolean conexionDisponible = true;
    private Stage stage;

    private ConexionMonitor() {
        crearPopup();
    }

    public static ConexionMonitor getInstance() {
        return INSTANCE;
    }

    public void iniciar(Stage stagePrincipal) {
        this.stage = stagePrincipal;
        if (monitorIniciado.compareAndSet(false, true)) {
            scheduler.scheduleAtFixedRate(this::verificarConexion, 5, 20, TimeUnit.SECONDS);
        }

        stagePrincipal.xProperty().addListener((obs, oldVal, newVal) -> reposicionarPopup());
        stagePrincipal.yProperty().addListener((obs, oldVal, newVal) -> reposicionarPopup());
        stagePrincipal.widthProperty().addListener((obs, oldVal, newVal) -> reposicionarPopup());
        stagePrincipal.heightProperty().addListener((obs, oldVal, newVal) -> reposicionarPopup());

        verificarConexion();
    }

    private void crearPopup() {
        Label mensaje = new Label("Se perdió la conexión con el servidor.");
        mensaje.setStyle("-fx-text-fill: white; -fx-font-size: 13px;");

        botonReconectar.setStyle("-fx-background-color: #1f7a31; -fx-text-fill: white; -fx-font-weight: bold;");
        botonReconectar.setOnAction(event -> reconectarManual());

        HBox contenedor = new HBox(12, mensaje, botonReconectar);
        contenedor.setPadding(new Insets(12));
        contenedor.setStyle("-fx-background-color: #c0392b; -fx-background-radius: 10;");

        popup.getContent().add(contenedor);
        popup.setAutoHide(false);
        popup.setHideOnEscape(false);
    }

    private void verificarConexion() {
        if (reconectando.get()) {
            return;
        }

        boolean disponible = Conexion.probarConexion();
        if (disponible == conexionDisponible) {
            return;
        }

        conexionDisponible = disponible;
        if (!disponible) {
            Platform.runLater(this::mostrarPopup);
        } else {
            Platform.runLater(this::ocultarPopup);
        }
    }

    private void mostrarPopup() {
        if (stage == null || !stage.isShowing()) {
            return;
        }

        if (!popup.isShowing()) {
            popup.show(stage);
        }
        reposicionarPopup();
    }

    private void reposicionarPopup() {
        if (stage == null || !popup.isShowing() || popup.getContent().isEmpty()) {
            return;
        }

        double ancho = popup.getContent().get(0).prefWidth(-1);
        double alto = popup.getContent().get(0).prefHeight(-1);
        double x = stage.getX() + (stage.getWidth() - ancho) / 2;
        double y = stage.getY() + stage.getHeight() - alto - 24;
        popup.setX(x);
        popup.setY(y);
    }

    private void ocultarPopup() {
        if (popup.isShowing()) {
            popup.hide();
        }
    }

    private void reconectarManual() {
        if (!reconectando.compareAndSet(false, true)) {
            return;
        }

        botonReconectar.setDisable(true);
        botonReconectar.setText("Conectando...");

        scheduler.execute(() -> {
            boolean disponible = Conexion.probarConexion();
            Platform.runLater(() -> {
                conexionDisponible = disponible;
                if (disponible) {
                    ocultarPopup();
                }
                botonReconectar.setDisable(false);
                botonReconectar.setText("Conectar");
                reconectando.set(false);
            });
        });
    }
}
