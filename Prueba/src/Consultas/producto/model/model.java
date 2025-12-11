package Consultas.producto.model;

import conexion.Conexion;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class model {

    private Conexion c = new Conexion();
    private Connection con;

    public ObservableList<producto> obtenerProductos() {
        ObservableList<producto> lista = FXCollections.observableArrayList();

        try {
            con = c.conectar();
            String sql = "SELECT * FROM productos";
            PreparedStatement st = con.prepareStatement(sql);
            ResultSet rs = st.executeQuery();

            while (rs.next()) {
                lista.add(new producto(
                        rs.getString("id"),
                        rs.getString("nombre"),
                        rs.getString("categoria"),
                        rs.getString("etiqueta"),
                        rs.getString("marca"),
                        rs.getString("material"),
                        rs.getString("unidadMedida"),
                        rs.getString("descripcion"),
                        rs.getInt("inventarioMin"),
                        rs.getString("urlImagen")
                ));
            }

            rs.close();
            st.close();
        } catch (SQLException e) {
            System.out.println("Error al obtener productos: " + e.getMessage());
        } finally {
            try { if (con != null) con.close(); } catch (SQLException e) {}
        }
        return lista;
    }

    public boolean eliminarProducto(String id) {
        try {
            con = c.conectar();
            String sql = "DELETE FROM productos WHERE id = ?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, id);
            int filas = ps.executeUpdate();
            ps.close();
            return filas > 0;
        } catch (SQLException e) {
            System.out.println("Error al eliminar producto: " + e.getMessage());
            return false;
        } finally {
            try { if (con != null) con.close(); } catch (SQLException e) {}
        }
    }

    public int guardarProducto(String nombre, String categoria, String etiqueta, String marca,
                               String material, String unidadMedida, String descripcion,
                               int inventarioMin, String urlImagen) {
        try {
            con = c.conectar();
            String sql = "INSERT INTO productos (nombre, categoria, etiqueta,  marca, material, unidadMedida, descripcion, inventarioMin, urlImagen) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement st = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

            st.setString(1, nombre);
            st.setString(2, categoria);
            st.setString(3, etiqueta);
            st.setString(4, marca);
            st.setString(5, material);
            st.setString(6, unidadMedida);
            st.setString(7, descripcion);
            st.setInt(8, inventarioMin);
            st.setString(9, urlImagen);

            int filas = st.executeUpdate();
            int idGenerado = -1;

            if (filas > 0) {
                ResultSet generatedKeys = st.getGeneratedKeys();
                if (generatedKeys.next()) {
                    idGenerado = generatedKeys.getInt(1);
                }
            }
            st.close();
            return idGenerado;
        } catch (SQLException e) {
            System.out.println("Error al guardar producto: " + e.getMessage());
            return -1;
        } finally {
            try { if (con != null) con.close(); } catch (SQLException e) {}
        }
    }

    public boolean modificarProducto(int id, String nombre, String categoria, String etiqueta, String marca,
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
            st.setInt(10, id);

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

    public boolean actualizarUrlImagen(int id, String nuevaUrl) {
        try {
            con = c.conectar();
            String sql = "UPDATE productos SET urlImagen=? WHERE id=?";
            PreparedStatement st = con.prepareStatement(sql);

            st.setString(1, nuevaUrl);
            st.setInt(2, id);

            int filas = st.executeUpdate();
            st.close();
            return filas > 0;
        } catch (SQLException e) {
            System.out.println("Error al actualizar URL de imagen: " + e.getMessage());
            return false;
        } finally {
            try { if (con != null) con.close(); } catch (SQLException e) {}
        }
    }
}