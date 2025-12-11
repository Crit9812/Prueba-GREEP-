package conexion;
//javac -encoding UTF-8 -cp ".;mysql.jar" prueba.java
//java -Dfile.encoding=UTF-8 -cp ".;mysql.jar" prueba

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Conexion {
    public Connection conn;

    public static void main(String[] args) {
    }

    public Connection conectar(){
        String url = "jdbc:mysql://distribuidoragreep.com.mx:3306/distribu_almacen?useSSL=false&serverTimezone=UTC&characterEncoding=UTF-8";
        String user = "distribu_Admin";
        String password = "AdminGreep2025.";

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");

            conn = DriverManager.getConnection(url, user, password);
            System.out.println("Conexión exitosa a la base de datos.");

        } catch (ClassNotFoundException e) {
            System.err.println("Driver JDBC no encontrado: " + e.getMessage());
        } catch (SQLException e) {
            System.err.println("Error de conexión o SQL: " + e.getMessage());
        }

        return conn;
    }
}
