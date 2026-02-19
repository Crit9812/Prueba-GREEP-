package Compartido.helper;

public class BusquedaProductoHelper {

    private static SolicitudBusqueda solicitudPendiente;

    private BusquedaProductoHelper() {
    }

    public static synchronized void guardarSolicitud(String idProducto, String nombreProducto, String terminoBusqueda) {
        solicitudPendiente = new SolicitudBusqueda(idProducto, nombreProducto, terminoBusqueda);
    }

    public static synchronized SolicitudBusqueda consumirSolicitud() {
        SolicitudBusqueda solicitud = solicitudPendiente;
        solicitudPendiente = null;
        return solicitud;
    }

    public static class SolicitudBusqueda {
        private final String idProducto;
        private final String nombreProducto;
        private final String terminoBusqueda;

        public SolicitudBusqueda(String idProducto, String nombreProducto, String terminoBusqueda) {
            this.idProducto = idProducto;
            this.nombreProducto = nombreProducto;
            this.terminoBusqueda = terminoBusqueda;
        }

        public String getIdProducto() {
            return idProducto;
        }

        public String getNombreProducto() {
            return nombreProducto;
        }

        public String getTerminoBusqueda() {
            return terminoBusqueda;
        }
    }
}
