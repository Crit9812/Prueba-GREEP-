package Compartido.sesion;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Control;
import javafx.scene.control.TextInputControl;

import java.util.Locale;

public class PermisosRolHelper {

    private PermisosRolHelper() {
    }

    public static void aplicarSoloLecturaEnModulo(Parent root) {
        if (root == null || !SesionUsuario.requiereRestriccionSupervisor()) {
            return;
        }
        aplicarRecursivo(root);
    }

    private static void aplicarRecursivo(Parent parent) {
        for (Node node : parent.getChildrenUnmodifiable()) {
            if (node instanceof Button boton) {
                String texto = boton.getText() == null ? "" : boton.getText().toLowerCase(Locale.ROOT);
                if (esAccionDeEdicion(texto)) {
                    boton.setDisable(true);
                }
            } else if (node instanceof TextInputControl input) {
                input.setEditable(false);
            } else if (node instanceof Control control) {
                String id = control.getId() == null ? "" : control.getId().toLowerCase(Locale.ROOT);
                if (id.contains("guardar") || id.contains("editar") || id.contains("eliminar") || id.contains("nuevo")) {
                    control.setDisable(true);
                }
            }

            if (node instanceof Parent childParent) {
                aplicarRecursivo(childParent);
            }
        }
    }

    private static boolean esAccionDeEdicion(String texto) {
        return texto.contains("guardar")
                || texto.contains("editar")
                || texto.contains("eliminar")
                || texto.contains("nuevo")
                || texto.contains("registrar")
                || texto.contains("agregar")
                || texto.contains("import")
                || texto.contains("sincron")
                || texto.contains("ajuste");
    }
}
