package Consultas.claves.model;

import Compartido.model.DAO.GenericDAO;
import conexion.Conexion;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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
        String sql = "UPDATE claves SET estado = 'desactivado' WHERE idAlterno = ?";
        try (Connection conn = new Conexion().conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public int contarEntradasPorClave(String idAlterno) {
        return contarRegistros("SELECT COUNT(*) FROM detalle_Entrada WHERE claveProducto = ?", idAlterno);
    }

    public int contarSalidasPorClave(String idAlterno) {
        return contarRegistros("SELECT COUNT(*) FROM detalle_Salida WHERE claveProductoSalida = ?", idAlterno);
    }

    private int contarRegistros(String sql, String idAlterno) {
        try (Connection conn = new Conexion().conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, idAlterno);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
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
