package Consultas.proveedores.model;

import Compartido.model.DAO.GenericDAO;
import conexion.Conexion;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.sql.Connection;
import java.util.ArrayList;

public class model {

    private final GenericDAO<proveedores> dao = new GenericDAO<>(proveedores.class);

    public ObservableList<proveedores> obtener() {
        ArrayList<proveedores> lista = dao.obtenerTodos();
        return FXCollections.observableArrayList(lista);
    }

    public boolean eliminar(int id) {
        if (contarArticulosDisponibles(id) > 0) {
            return false;
        }
        return dao.eliminar(String.valueOf(id));
    }

    public int contarArticulosDisponibles(int idProveedor) {
        try (Connection conn = new Conexion().conectar()) {
            return GenericDAO.contarArticulosDisponiblesPorProveedor(conn, String.valueOf(idProveedor));
        } catch (Exception e) {
            System.out.println("Error en contarArticulosDisponiblesProveedor: " + e.getMessage());
        }

        return 0;
    }

    public proveedores obtenerPorId(int id) {
        return dao.buscarExacto("id", id);
    }

    public ObservableList<proveedores> buscarExacto(String nombre) {
        ArrayList<proveedores> lista = dao.buscarParcial("Nombre", nombre);
        return FXCollections.observableArrayList(lista);
    }
}
