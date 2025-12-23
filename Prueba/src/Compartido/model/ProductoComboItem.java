package Compartido.model;

public class ProductoComboItem {
    private final String idProducto;
    private final String nombreProducto;
    private final String descripcion;
    private final String claveAlterna;

    public ProductoComboItem(String idProducto, String nombreProducto, String descripcion, String claveAlterna) {
        this.idProducto = idProducto;
        this.nombreProducto = nombreProducto;
        this.descripcion = descripcion;
        this.claveAlterna = claveAlterna;
    }

    public String getIdProducto() {
        return idProducto;
    }

    public String getNombreProducto() {
        return nombreProducto;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public String getClaveAlterna() {
        return claveAlterna;
    }
}
