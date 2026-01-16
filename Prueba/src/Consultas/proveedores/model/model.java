package Consultas.proveedores.model;

import Compartido.model.DAO.GenericDAO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.ArrayList;

public class model {

    private final GenericDAO<proveedores> dao = new GenericDAO<>(proveedores.class);

    public ObservableList<proveedores> obtener() {
        ArrayList<proveedores> lista = dao.obtenerTodos();
        return FXCollections.observableArrayList(filtrarActivos(lista));
    }

    public boolean eliminar(int id) {
        return dao.eliminar(String.valueOf(id));
    }

    public proveedores obtenerPorId(int id) {
        return dao.buscarExacto("id", id);
    }

    public ObservableList<proveedores> buscarExacto(String nombre) {
        ArrayList<proveedores> lista = dao.buscarParcial("Nombre", nombre);
        return FXCollections.observableArrayList(filtrarActivos(lista));
    }

    private ArrayList<proveedores> filtrarActivos(ArrayList<proveedores> lista) {
        ArrayList<proveedores> activos = new ArrayList<>();
        for (proveedores p : lista) {
            if (p != null && "activo".equalsIgnoreCase(p.getStatus())) {
                activos.add(p);
            }
        }
        return activos;
    }
}
