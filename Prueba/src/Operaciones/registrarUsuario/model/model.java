package Operaciones.registrarUsuario.model;

import Compartido.model.DAO.GenericDAO;
import java.util.ArrayList;

public class model {

    private GenericDAO<usuario> dao = new GenericDAO<>(usuario.class);

    public ArrayList<usuario> obtenerUsuarios() {
        return dao.obtenerTodos();
    }

    public boolean eliminarUsuario(String id) {
        return dao.eliminar(id);
    }

    public boolean insertarUsuario(usuario u) {
        return dao.insertar(u);
    }

    public boolean actualizarUsuario(usuario u) {
        return dao.actualizar(u);
    }
}
