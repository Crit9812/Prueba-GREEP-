package Consultas.claves.model;

import Compartido.model.DAO.GenericDAO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.util.ArrayList;

public class model {

    private final GenericDAO<claves> claveDAO;

    public model() {
        this.claveDAO = new GenericDAO<>(claves.class);
    }

    public ObservableList<claves> obtener() {
        ArrayList<claves> lista = claveDAO.obtenerTodos();
        return FXCollections.observableArrayList(lista);
    }

    public boolean eliminar(String id) {
        return claveDAO.eliminar(id);
    }

    public ObservableList<String[]> obtenerParaTabla() {
        ArrayList<String[]> resultados = claveDAO.obtenerClavesCompletas();
        return FXCollections.observableArrayList(resultados);
    }

    public ObservableList<String[]> buscarEnTabla(String textoBusqueda) {
        if (textoBusqueda == null || textoBusqueda.trim().isEmpty()) {
            return obtenerParaTabla();
        }
        ArrayList<String[]> resultados = claveDAO.buscarClavesCompletas(textoBusqueda.trim());
        return FXCollections.observableArrayList(resultados);
    }
}