package Compartido.sesion;

import java.util.Locale;

public final class PermisosRol {

    private PermisosRol() {}

    private static String normalizar(String rol) {
        return rol == null ? "" : rol.trim().toLowerCase(Locale.ROOT);
    }

    public static boolean esAdministrador() {
        return "administrador".equals(normalizar(SesionUsuario.getRolUsuario()));
    }

    public static boolean esAuxiliar() {
        return "auxiliar".equals(normalizar(SesionUsuario.getRolUsuario()));
    }

    public static boolean esSupervisor() {
        return "supervisor".equals(normalizar(SesionUsuario.getRolUsuario()));
    }

    public static boolean esUsuarioFinal() {
        return "usuario".equals(normalizar(SesionUsuario.getRolUsuario()));
    }

    public static boolean debeOcultarOperacionesYConfiguracion() {
        return esSupervisor() || esUsuarioFinal();
    }

    public static boolean modoSoloLecturaReportesConsultas() {
        return esSupervisor() || esUsuarioFinal();
    }
}
