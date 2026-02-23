package conexion;

import Compartido.helper.OverlayCarga;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class ConexionMonitor {
    private static final ConexionMonitor INSTANCE = new ConexionMonitor();
    private static final long INTERVALO_VERIFICACION_SEGUNDOS = 3;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "conexion-monitor");
        thread.setDaemon(true);
        return thread;
    });

    private final AtomicBoolean monitorIniciado = new AtomicBoolean(false);
    private final AtomicBoolean verificando = new AtomicBoolean(false);

    private volatile boolean conexionDisponible = true;
    private Stage stage;
    private Pane overlayPane;
    private OverlayCarga overlayCarga;

    private ConexionMonitor() {
    }

    public static ConexionMonitor getInstance() {
        return INSTANCE;
    }

    public void iniciar(Stage stagePrincipal) {
        this.stage = stagePrincipal;

        stagePrincipal.sceneProperty().addListener((obs, oldScene, newScene) -> Platform.runLater(this::configurarOverlayEnEscenaActual));
        Platform.runLater(this::configurarOverlayEnEscenaActual);

        if (monitorIniciado.compareAndSet(false, true)) {
            scheduler.scheduleAtFixedRate(this::verificarYRecuperarConexion, 0,
                    INTERVALO_VERIFICACION_SEGUNDOS, TimeUnit.SECONDS);
        }
    }

    private void configurarOverlayEnEscenaActual() {
        if (stage == null) {
            return;
        }

        Scene scene = stage.getScene();
        if (scene == null || !(scene.getRoot() instanceof StackPane root)) {
            overlayCarga = null;
            overlayPane = null;
            return;
        }

        if (overlayPane != null && overlayPane.getParent() == root && overlayCarga != null) {
            return;
        }

        overlayPane = new Pane();
        root.getChildren().add(overlayPane);

        overlayCarga = new OverlayCarga(root, overlayPane, "Conectando...");
        overlayCarga.ocultar();

        if (!conexionDisponible) {
            overlayCarga.mostrar();
        }
    }

    private void verificarYRecuperarConexion() {
        if (!verificando.compareAndSet(false, true)) {
            return;
        }

        try {
            boolean disponible = Conexion.probarConexion();

            if (disponible != conexionDisponible) {
                conexionDisponible = disponible;
                if (disponible) {
                    Platform.runLater(this::ocultarOverlayBloqueante);
                } else {
                    Platform.runLater(this::mostrarOverlayBloqueante);
                }
            }
        } finally {
            verificando.set(false);
        }
    }

    private void mostrarOverlayBloqueante() {
        configurarOverlayEnEscenaActual();
        if (overlayCarga != null) {
            overlayCarga.setMensaje("Conectando...");
            overlayCarga.mostrar();
        }
    }

    private void ocultarOverlayBloqueante() {
        if (overlayCarga != null) {
            overlayCarga.ocultar();
        }
    }
}
