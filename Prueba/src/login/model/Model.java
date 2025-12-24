package login.model;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import conexion.Conexion;

public class Model {
    Conexion c = new Conexion();

    public Connection conexion;
    public boolean acceso;

    // Metodo para inyectar la conexión desde otra clase
    public void setConexion(Connection conexion) {
        this.conexion = conexion;
    }

    @FXML
    private void initialize() {
    }

    public boolean verificarUsuario(String username, String password) {
        setConexion(conexion = c.conectar());

        String sql = "SELECT * FROM usuarios WHERE userName = ? AND contrasenaUsuario = ?";

        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    System.out.println("Acceso permitido");
                    acceso = true;
                } else {
                    System.out.println("Usuario o contraseña incorrectos");
                    acceso = false;
                }
            }

        } catch (SQLException e) {
            System.out.println("Ocurrió un error");
        }
        return acceso;
    }

    private void mostrarAlerta(String titulo, String mensaje, AlertType tipo) {
        Alert alerta = new Alert(tipo);
        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }
}
