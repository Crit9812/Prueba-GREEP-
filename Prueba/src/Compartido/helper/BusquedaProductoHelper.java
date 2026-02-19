package Compartido.helper;

public class BusquedaProductoHelper {

    private static SolicitudBusqueda solicitudPendiente;

    private BusquedaProductoHelper() {
    }

    public static synchronized void guardarSolicitud(String idProducto, String nombreProducto) {
        solicitudPendiente = new SolicitudBusqueda(idProducto, nombreProducto);
    }

    public static synchronized SolicitudBusqueda consumirSolicitud() {
        SolicitudBusqueda solicitud = solicitudPendiente;
        solicitudPendiente = null;
        return solicitud;
    }

    public static class SolicitudBusqueda {
        private final String idProducto;
        private final String nombreProducto;

        public SolicitudBusqueda(String idProducto, String nombreProducto) {
            this.idProducto = idProducto;
            this.nombreProducto = nombreProducto;
        }

        public String getIdProducto() {
            return idProducto;
        }

        public String getNombreProducto() {
            return nombreProducto;
        }
    }
}
