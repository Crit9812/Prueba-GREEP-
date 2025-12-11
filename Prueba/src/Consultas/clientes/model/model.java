package Consultas.clientes.model;

import Compartido.model.DAO.GenericDAO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.ArrayList;

public class model {

    private final GenericDAO<cliente> dao = new GenericDAO<>(cliente.class);

    public ObservableList<cliente> obtenerClientes() {
        ArrayList<cliente> lista = dao.obtenerTodos();
        return FXCollections.observableArrayList(lista);
    }

    public boolean eliminarCliente(int idCliente) {
        return dao.eliminar(String.valueOf(idCliente));
    }

    public cliente obtenerClientePorId(int id) {
        return dao.buscarPorCampo("id", id);
    }

    public ObservableList<cliente> buscarPorNombre(String nombre) {
        ArrayList<cliente> lista = dao.buscar("nombre", nombre);
        return FXCollections.observableArrayList(lista);
    }
}
