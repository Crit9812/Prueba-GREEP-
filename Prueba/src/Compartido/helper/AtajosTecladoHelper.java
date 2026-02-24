package Compartido.helper;

import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.input.KeyEvent;

public final class AtajosTecladoHelper {

    private AtajosTecladoHelper() {}

    public static void registrarCuandoEscenaEsteLista(Node nodo, String clave, EventHandler<KeyEvent> handler) {
        if (nodo == null) {
            return;
        }
        if (nodo.getScene() != null) {
            registrarEnEscena(nodo.getScene(), clave, handler);
            return;
        }
        Platform.runLater(() -> registrarCuandoEscenaEsteLista(nodo, clave, handler));
    }

    public static void registrarEnEscena(Scene scene, String clave, EventHandler<KeyEvent> handler) {
        if (scene == null || handler == null || clave == null || clave.isBlank()) {
            return;
        }
        String propiedad = "atajo_registrado_" + clave;
        if (Boolean.TRUE.equals(scene.getProperties().get(propiedad))) {
            return;
        }
        scene.addEventFilter(KeyEvent.KEY_PRESSED, handler);
        scene.getProperties().put(propiedad, Boolean.TRUE);
    }
}
