package Formularios.model;

import conexion.Conexion;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class modelNuevoProducto {

    Conexion c = new Conexion();
    Connection con;

    public boolean guardarProducto(String id, String nombre, String categoria, String etiqueta, String marca,
                                   String material, String unidadMedida, String descripcion,
                                   int inventarioMin, String urlImagen) {
        try {
            con = c.conectar();
            String sql = "INSERT INTO productos (id, nombre, categoria, etiqueta, marca, material, unidadMedida, descripcion, inventarioMin, urlImagen) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement st = con.prepareStatement(sql);

            st.setString(1, id);
            st.setString(2, nombre);
            st.setString(3, categoria);
            st.setString(4, etiqueta);
            st.setString(5, marca);
            st.setString(6, material);
            st.setString(7, unidadMedida);
            st.setString(8, descripcion);
            st.setInt(9, inventarioMin);
            st.setString(10, urlImagen);

            int filas = st.executeUpdate();
            st.close();
            return filas > 0;

        } catch (SQLException e) {
            System.out.println("Error al guardar producto: " + e.getMessage());
            return false;
        } finally {
            try { if (con != null) con.close(); } catch (SQLException e) {}
        }
    }

    public boolean modificarProducto(String id, String nombre, String categoria, String etiqueta, String marca,
                                     String material, String unidadMedida, String descripcion,
                                     int inventarioMin, String urlImagen) {
        try {
            con = c.conectar();
            String sql = "UPDATE productos SET nombre=?, categoria=?, etiqueta=?, marca=?, material=?, unidadMedida=?, descripcion=?, inventarioMin=?, urlImagen=? WHERE id=?";
            PreparedStatement st = con.prepareStatement(sql);

            st.setString(1, nombre);
            st.setString(2, categoria);
            st.setString(3, etiqueta);
            st.setString(4, marca);
            st.setString(5, material);
            st.setString(6, unidadMedida);
            st.setString(7, descripcion);
            st.setInt(8, inventarioMin);
            st.setString(9, urlImagen);
            st.setString(10, id);

            int filas = st.executeUpdate();
            st.close();
            return filas > 0;

        } catch (SQLException e) {
            System.out.println("Error al modificar producto: " + e.getMessage());
            return false;
        } finally {
            try { if (con != null) con.close(); } catch (SQLException e) {}
        }
    }

}
