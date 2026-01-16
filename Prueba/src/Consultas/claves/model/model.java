package Consultas.claves.model;

import Compartido.model.DAO.GenericDAO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.ArrayList;

public class model {

    private final GenericDAO<claves> claveDAO;
    private final dao clavesDAO;

    public model() {
        this.claveDAO = new GenericDAO<>(claves.class);
        this.clavesDAO = new dao();
    }

    // --- USANDO GenericDAO (NO SE TOCA) ---

    public ObservableList<claves> obtener() {
        ArrayList<claves> lista = claveDAO.obtenerTodos();
        return FXCollections.observableArrayList(lista);
    }

    public boolean eliminar(String id) {
        return claveDAO.eliminar(id);
    }

    // --- USANDO DAO ESPECÍFICO ---

    public ObservableList<String[]> obtenerParaTabla() {
        return FXCollections.observableArrayList(
                clavesDAO.obtenerClavesCompletas()
        );
    }

    public ObservableList<String[]> buscarEnTabla(String textoBusqueda) {

        if (textoBusqueda == null || textoBusqueda.trim().isEmpty()) {
            return obtenerParaTabla();
        }

        return FXCollections.observableArrayList(
                clavesDAO.buscarClavesCompletas(textoBusqueda.trim())
        );
    }
}
