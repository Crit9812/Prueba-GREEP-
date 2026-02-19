package Compartido.sesion;

public final class BusquedaGlobalProductoContext {

    private static String idProductoPendiente;
    private static String nombreProductoPendiente;

    private BusquedaGlobalProductoContext() {
    }

    public static synchronized void setBusquedaPendiente(String idProducto, String nombreProducto) {
        idProductoPendiente = idProducto;
        nombreProductoPendiente = nombreProducto;
    }

    public static synchronized BusquedaPendiente consumirBusquedaPendiente() {
        if ((idProductoPendiente == null || idProductoPendiente.isBlank())
                && (nombreProductoPendiente == null || nombreProductoPendiente.isBlank())) {
            return null;
        }

        BusquedaPendiente busqueda = new BusquedaPendiente(idProductoPendiente, nombreProductoPendiente);
        idProductoPendiente = null;
        nombreProductoPendiente = null;
        return busqueda;
    }

    public static final class BusquedaPendiente {
        private final String idProducto;
        private final String nombreProducto;

        private BusquedaPendiente(String idProducto, String nombreProducto) {
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
