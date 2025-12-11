package Consultas.claves.model;

import Compartido.model.DAO.GenericDAO;
import conexion.Conexion;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.sql.*;
import java.util.ArrayList;

public class model {

    private final GenericDAO<claves> dao = new GenericDAO<>(claves.class);

    public ObservableList<claves> obtener() {
        ArrayList<claves> lista = dao.obtenerTodos();
        return FXCollections.observableArrayList(lista);
    }

    public boolean eliminar(String id) {
        return dao.eliminar(id);
    }

    public claves obtenerPorId(String id) {
        return dao.buscarPorCampo("idClaveCatalogo", id);
    }

    public ObservableList<claves> buscarPorProducto(String valor) {
        ArrayList<claves> lista = dao.buscar("idProducto", valor);
        return FXCollections.observableArrayList(lista);
    }

    /**
     * Devuelve los datos listos para poblar la TableView:
     * Orden en el array (6 elementos):
     * 0 -> idClaveCatalogo (clave alterna)
     * 1 -> claveGreep (clave del producto)
     * 2 -> producto (nombre)
     * 3 -> proveedor_id (id del proveedor)
     * 4 -> proveedor (nombre del proveedor)
     * 5 -> descripcion (concatenación: marca, etiqueta, unidadMedida, descripcion)
     */
    public ArrayList<String[]> obtenerParaTabla() {
        ArrayList<String[]> resultado = new ArrayList<>();

        String sql = "SELECT c.idClaveCatalogo AS idClaveCatalogo, " +
                "c.claveGreep AS claveGreep, " +
                "p.nombre AS producto, " +
                "pr.id AS proveedor_id, " +
                "pr.Nombre AS proveedor, " +
                // CONCAT_WS ignora valores NULL y une con espacios. TRIM para limpiar espacios sobrantes.
                "TRIM(CONCAT_WS(' ', m.nombre, e.nombre, p.unidadMedida, p.descripcion)) AS descripcion " +
                "FROM claves c " +
                "LEFT JOIN productos p ON c.claveGreep = p.id " +
                "LEFT JOIN proveedores pr ON c.claveProveedor = pr.id " +
                "LEFT JOIN marcas m ON p.marca = m.id " +
                "LEFT JOIN etiquetas e ON p.etiqueta = e.id";

        try (Connection con = new Conexion().conectar();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                // crear arreglo con exactamente 6 posiciones según el contrato con el controller
                String[] fila = new String[6];

                // Colocamos cada campo en la posición acordada
                fila[0] = rs.getString("idClaveCatalogo");               // clave alterna
                fila[1] = rs.getString("claveGreep");                   // clave del producto
                fila[2] = rs.getString("producto");                     // nombre del producto
                // proveedor_id puede ser NULL en BD -> convertir a cadena vacía si es null
                Object provIdObj = rs.getObject("proveedor_id");
                fila[3] = provIdObj == null ? "" : provIdObj.toString(); // id proveedor (string)
                fila[4] = rs.getString("proveedor");                    // nombre proveedor
                fila[5] = rs.getString("descripcion");                  // descripcion concatenada

                resultado.add(fila);
            }

        } catch (Exception e) {
            System.out.println("Error en obtenerParaTabla: " + e.getMessage());
        }
        return resultado;
    }
}
