package Compartido.sesion;

public class SesionUsuario {

    private static String nombreUsuario;
    private static Integer idUsuario;

    public static void setNombreUsuario(String nombre){
        nombreUsuario = nombre;
    }

    public static String getNombreUsuario(){
        return nombreUsuario;
    }

    public static void setIdUsuario(Integer id){
        idUsuario = id;
    }

    public static Integer getIdUsuario(){
        return idUsuario;
    }

    public static void cerrarSesion(){
        nombreUsuario = null;
        idUsuario = null;
    }
}
