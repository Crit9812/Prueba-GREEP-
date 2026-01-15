package Consultas.sucursales.model;

import Compartido.model.DAO.GenericDAO;
import conexion.Conexion;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.sql.Connection;
import java.util.ArrayList;

public class model {

    private final GenericDAO<sucursal> dao = new GenericDAO<>(sucursal.class);

    public ObservableList<sucursal> obtenerSucursales() {
        ArrayList<sucursal> lista = dao.obtenerTodos();
        return FXCollections.observableArrayList(lista);
    }

    public boolean eliminarSucursal(int idSucursal) {
        return dao.eliminar(String.valueOf(idSucursal));
    }

    public int contarArticulosPendientes(int idSucursal) {
        try (Connection conn = new Conexion().conectar()) {
            return GenericDAO.contarArticulosPendientesPorSucursal(conn, String.valueOf(idSucursal));
        } catch (Exception e) {
            System.out.println("Error en contarArticulosPendientesSucursal: " + e.getMessage());
        }

        return 0;
    }

    public sucursal obtenerSucursalPorId(int id) {
        return dao.buscarExacto("id", id);
    }

    public ObservableList<sucursal> buscarExacto(String nombre) {
        ArrayList<sucursal> lista = dao.buscarParcial("nombre", nombre);
        return FXCollections.observableArrayList(lista);
    }

}
