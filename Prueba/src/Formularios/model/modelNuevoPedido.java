package Formularios.model;

import Compartido.model.DAO.GenericDAO;
import Consultas.producto.model.producto;
import java.sql.SQLException;
import java.util.*;

public class modelNuevoPedido {

    public List<Map<String, String>> obtenerProductos() throws SQLException {
        try {
            GenericDAO<producto> dao = new GenericDAO<>(producto.class);
            return new ArrayList<>(dao.obtenerProductosConMarcaEtiqueta());

        } catch (Exception e) {
            throw new SQLException("Error al obtener productos: " + e.getMessage(), e);
        }
    }
}