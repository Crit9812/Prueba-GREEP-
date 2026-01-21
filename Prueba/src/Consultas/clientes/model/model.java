package Consultas.clientes.model;

import Compartido.model.DAO.GenericDAO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.ArrayList;

public class model {

    private final GenericDAO<cliente> dao = new GenericDAO<>(cliente.class);

    public ObservableList<cliente> obtenerClientes() {
        ArrayList<cliente> lista = dao.obtenerTodos();
        return FXCollections.observableArrayList(filtrarActivos(lista));
    }

    public boolean eliminarCliente(int idCliente) {
        cliente cliente = obtenerClientePorId(idCliente);
        if (cliente == null) {
            return false;
        }
        cliente.setStatus("desactivado");
        return dao.actualizar(cliente);
    }

    public cliente obtenerClientePorId(int id) {
        return dao.buscarExacto("id", id);
    }

    public ObservableList<cliente> buscarExacto(String nombre) {
        ArrayList<cliente> lista = dao.buscarParcial("nombre", nombre);
        return FXCollections.observableArrayList(filtrarActivos(lista));
    }

    private ArrayList<cliente> filtrarActivos(ArrayList<cliente> lista) {
        ArrayList<cliente> activos = new ArrayList<>();
        for (cliente c : lista) {
            if (c != null && "activo".equalsIgnoreCase(c.getStatus())) {
                activos.add(c);
            }
        }
        return activos;
    }
}
