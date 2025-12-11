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
        return dao.buscarPorCampo("id", id);
    }

    public ObservableList<sucursal> buscarPorNombre(String nombre) {
        ArrayList<sucursal> lista = dao.buscar("nombre", nombre);
        return FXCollections.observableArrayList(lista);
    }

}

