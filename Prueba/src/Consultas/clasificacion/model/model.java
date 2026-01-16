package Consultas.clasificacion.model;

import Compartido.model.DAO.GenericDAO;
import conexion.Conexion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

public class model {

    // DAOs para cada entidad
    private final GenericDAO<marcas> marcaDAO = new GenericDAO<>(marcas.class);
    private final GenericDAO<etiquetas> etiquetaDAO = new GenericDAO<>(etiquetas.class);
    private final GenericDAO<ubicaciones> ubicacionDAO = new GenericDAO<>(ubicaciones.class);
    private final GenericDAO<unidades_Medida> umDAO = new GenericDAO<>(unidades_Medida.class);

    // ================= MARCAS =================

    public List<marcas> obtenerMarcas() {
        return marcaDAO.obtenerTodos();
    }

    public boolean insertarMarca(String nombre) {
        marcas marca = new marcas();
        marca.setNombre(nombre);
        return marcaDAO.insertar(marca);
    }

    public boolean actualizarMarca(int id, String nombre) {
        // Método más eficiente usando búsqueda directa
        marcas marca = buscarMarcaPorId(id);
        if (marca != null) {
            marca.setNombre(nombre);
            return marcaDAO.actualizar(marca);
        }
        return false;
    }

    private marcas buscarMarcaPorId(int id) {
        List<marcas> marcas = marcaDAO.obtenerTodos();
        for (marcas m : marcas) {
            if (m.getId() == id) {
                return m;
            }
        }
        return null;
    }

    public boolean eliminarMarca(int id) {
        if (contarArticulosDisponiblesPorMarca(id) > 0) {
            return false;
        }
        return marcaDAO.eliminar(String.valueOf(id));
    }

    public int contarProductosPorMarca(int marcaId) {
        String sql = "SELECT COUNT(*) FROM productos WHERE marca = ?";

        try (Connection conn = new Conexion().conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, marcaId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    public int contarArticulosDisponiblesPorMarca(int marcaId) {
        try (Connection conn = new Conexion().conectar()) {
            return GenericDAO.contarArticulosDisponiblesPorMarca(conn, marcaId);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    // ================= ETIQUETAS =================

    public List<etiquetas> obtenerEtiquetas() {
        return etiquetaDAO.obtenerTodos();
    }

    public boolean insertarEtiqueta(String nombre) {
        etiquetas etiqueta = new etiquetas();
        etiqueta.setNombre(nombre);
        return etiquetaDAO.insertar(etiqueta);
    }

    public boolean actualizarEtiqueta(int id, String nombre) {
        etiquetas etiqueta = buscarEtiquetaPorId(id);
        if (etiqueta != null) {
            etiqueta.setNombre(nombre);
            return etiquetaDAO.actualizar(etiqueta);
        }
        return false;
    }

    private etiquetas buscarEtiquetaPorId(int id) {
        List<etiquetas> etiquetas = etiquetaDAO.obtenerTodos();
        for (etiquetas e : etiquetas) {
            if (e.getId() == id) {
                return e;
            }
        }
        return null;
    }

    public boolean eliminarEtiqueta(int id) {
        if (contarArticulosDisponiblesPorEtiqueta(id) > 0) {
            return false;
        }
        return etiquetaDAO.eliminar(String.valueOf(id));
    }

    public int contarProductosPorEtiqueta(int etiquetaId) {
        String sql = "SELECT COUNT(*) FROM productos WHERE etiqueta = ?";

        try (Connection conn = new Conexion().conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, etiquetaId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    public int contarArticulosDisponiblesPorEtiqueta(int etiquetaId) {
        try (Connection conn = new Conexion().conectar()) {
            return GenericDAO.contarArticulosDisponiblesPorEtiqueta(conn, etiquetaId);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    // ================= UBICACIONES =================

    public List<ubicaciones> obtenerUbicaciones() {
        return ubicacionDAO.obtenerTodos();
    }

    public boolean insertarUbicacion(String nombre) {
        ubicaciones ubicacion = new ubicaciones();
        ubicacion.setNombre(nombre);
        return ubicacionDAO.insertar(ubicacion);
    }

    public boolean actualizarUbicacion(int id, String nombre) {
        ubicaciones ubicacion = buscarUbicacionPorId(id);
        if (ubicacion != null) {
            ubicacion.setNombre(nombre);
            return ubicacionDAO.actualizar(ubicacion);
        }
        return false;
    }

    private ubicaciones buscarUbicacionPorId(int id) {
        List<ubicaciones> ubicaciones = ubicacionDAO.obtenerTodos();
        for (ubicaciones u : ubicaciones) {
            if (u.getId() == id) {
                return u;
            }
        }
        return null;
    }

    public boolean eliminarUbicacion(int id) {
        if (contarArticulosDisponiblesPorUbicacion(id) > 0) {
            return false;
        }
        return ubicacionDAO.eliminar(String.valueOf(id));
    }

    public int contarProductosPorUbicacion(int ubicacionId) {
        String sql = "SELECT COUNT(*) FROM detalleArticulo WHERE idUbicacion = ?";

        try (Connection conn = new Conexion().conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, ubicacionId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    public int contarArticulosDisponiblesPorUbicacion(int ubicacionId) {
        try (Connection conn = new Conexion().conectar()) {
            return GenericDAO.contarArticulosDisponiblesPorUbicacion(conn, ubicacionId);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    // ================= UNIDADES DE MEDIDA =================

    public List<unidades_Medida> obtenerUM() {
        return umDAO.obtenerTodos();
    }

    public boolean insertarUM(String nombre) {
        unidades_Medida um = new unidades_Medida();
        um.setNombre(nombre);
        return umDAO.insertar(um);
    }

    public boolean actualizarUM(int id, String nombre) {
        unidades_Medida um = buscarUMPorId(id);
        if (um != null) {
            um.setNombre(nombre);
            return umDAO.actualizar(um);
        }
        return false;
    }

    private unidades_Medida buscarUMPorId(int id) {
        List<unidades_Medida> unidades = umDAO.obtenerTodos();
        for (unidades_Medida u : unidades) {
            if (u.getId() == id) {
                return u;
            }
        }
        return null;
    }

    public boolean eliminarUM(int id) {
        if (contarArticulosDisponiblesPorUM(id) > 0) {
            return false;
        }
        return umDAO.eliminar(String.valueOf(id));
    }

    public int contarProductosPorUM(int umId) {
        String sql = "SELECT COUNT(*) FROM productos WHERE unidad_Medida = ?";

        try (Connection conn = new Conexion().conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, umId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    public int contarArticulosDisponiblesPorUM(int umId) {
        try (Connection conn = new Conexion().conectar()) {
            return GenericDAO.contarArticulosDisponiblesPorUnidadMedida(conn, umId);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }
}
