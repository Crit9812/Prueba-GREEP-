package Consultas.clientes.model;

import Compartido.model.DAO.GenericDAO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.ArrayList;

public class model { //njjgj

    private final GenericDAO<cliente> dao = new GenericDAO<>(cliente.class);

    public ObservableList<cliente> obtenerClientes() {
        ArrayList<cliente> lista = dao.obtenerTodos();
        return FXCollections.observableArrayList(lista);
    }

    public boolean eliminarCliente(int idCliente) {
        return dao.eliminar(String.valueOf(idCliente));
    }

    public cliente obtenerClientePorId(int id) {
        return dao.buscarExacto("id", id);
    }

    public ObservableList<cliente> buscarExacto(String nombre) {
        ArrayList<cliente> lista = dao.buscarParcial("nombre", nombre);
        return FXCollections.observableArrayList(lista);
    }
}
