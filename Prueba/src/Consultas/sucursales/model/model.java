package Consultas.sucursales.model;

import Compartido.model.DAO.GenericDAO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

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

    public sucursal obtenerSucursalPorId(int id) {
        return dao.buscarExacto("id", id);
    }

    public ObservableList<sucursal> buscarExacto(String nombre) {
        ArrayList<sucursal> lista = dao.buscarParcial("nombre", nombre);
        return FXCollections.observableArrayList(lista);
    }

}

