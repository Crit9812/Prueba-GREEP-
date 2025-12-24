package Compartido.sesion;

public class SesionUsuario {

    private static String nombreUsuario;

    public static void setNombreUsuario(String nombre){
        nombreUsuario = nombre;
    }

    public static String getNombreUsuario(){
        return nombreUsuario;
    }
}
