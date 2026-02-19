package Compartido.sesion;

public final class BusquedaInventarioState {

    private static String productoPendiente;

    private BusquedaInventarioState() {
    }

    public static synchronized void setProductoPendiente(String producto) {
        productoPendiente = producto;
    }

    public static synchronized String consumirProductoPendiente() {
        String valor = productoPendiente;
        productoPendiente = null;
        return valor;
    }
}
