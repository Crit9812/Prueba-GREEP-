package Consultas.sucursales.model;

import Compartido.model.DAO.GenericDAO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.ArrayList;

public class model {

    private final GenericDAO<sucursal> dao = new GenericDAO<>(sucursal.class);

    public ObservableList<sucursal> obtenerSucursales() {
        ArrayList<sucursal> lista = dao.obtenerTodos();
        return FXCollections.observableArrayList(filtrarActivos(lista));
    }

    public boolean eliminarSucursal(int idSucursal) {
        return dao.eliminar(String.valueOf(idSucursal));
    }

    public sucursal obtenerSucursalPorId(int id) {
        return dao.buscarExacto("id", id);
    }

    public ObservableList<sucursal> buscarExacto(String nombre) {
        ArrayList<sucursal> lista = dao.buscarParcial("nombre", nombre);
        return FXCollections.observableArrayList(filtrarActivos(lista));
    }

    private ArrayList<sucursal> filtrarActivos(ArrayList<sucursal> lista) {
        ArrayList<sucursal> resultado = new ArrayList<>();
        if (lista == null) {
            return resultado;
        }
        for (sucursal s : lista) {
            if (s != null && "activo".equalsIgnoreCase(s.getStatus())) {
                resultado.add(s);
            }
        }
        return resultado;
    }

}
