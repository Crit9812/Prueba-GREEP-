package Compartido.helper;

import VentanaPrincipal.controller.MainController;

public final class NavegacionHelper {

    private static MainController controladorPrincipal;

    private NavegacionHelper() {
    }

    public static void setControladorPrincipal(MainController controlador) {
        controladorPrincipal = controlador;
    }

    public static MainController getControladorPrincipal() {
        return controladorPrincipal;
    }
}
