package Operaciones.registrarUsuario.model;

import Compartido.model.DAO.GenericDAO;
import java.util.ArrayList;

public class model {

    private GenericDAO<usuario> dao = new GenericDAO<>(usuario.class);

    public ArrayList<usuario> obtenerUsuarios() {
        return filtrarActivos(dao.obtenerTodos());
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

    private ArrayList<usuario> filtrarActivos(ArrayList<usuario> lista) {
        ArrayList<usuario> resultado = new ArrayList<>();
        if (lista == null) {
            return resultado;
        }
        for (usuario u : lista) {
            if (u != null && "activo".equalsIgnoreCase(u.getEstado())) {
                resultado.add(u);
            }
        }
        return resultado;
    }
}
