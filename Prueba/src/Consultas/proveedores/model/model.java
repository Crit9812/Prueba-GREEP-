package Consultas.proveedores.model;

import Compartido.model.DAO.GenericDAO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.ArrayList;

public class model {

    private final GenericDAO<proveedores> dao = new GenericDAO<>(proveedores.class);

    public ObservableList<proveedores> obtener() {
        ArrayList<proveedores> lista = dao.obtenerTodos();
        return FXCollections.observableArrayList(lista);
    }

    public boolean eliminar(int id) {
        return dao.eliminar(String.valueOf(id));
    }

    public proveedores obtenerPorId(int id) {
        return dao.buscarExacto("id", id);
    }

    public ObservableList<proveedores> buscarExacto(String nombre) {
        ArrayList<proveedores> lista = dao.buscarParcial("Nombre", nombre);
        return FXCollections.observableArrayList(lista);
    }
}
