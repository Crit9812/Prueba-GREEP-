package Consultas.producto.model;

public class producto {

    private String idProducto; // ahora String
    private String nombreProducto;
    private String categoria;
    private String etiqueta;
    private String marca;
    private String material;
    private String unidadMedida;
    private String descripcion;
    private int inventarioMin;
    private String urlImagen;

    public producto(String idProducto, String nombreProducto, String categoria, String etiqueta, String marca,
                    String material, String unidadMedida, String descripcion,
                    int inventarioMin, String urlImagen) {
        this.idProducto = idProducto;
        this.nombreProducto = nombreProducto;
        this.categoria = categoria;
        this.etiqueta = etiqueta;
        this.marca = marca;
        this.material = material;
        this.unidadMedida = unidadMedida;
        this.descripcion = descripcion;
        this.inventarioMin = inventarioMin;
        this.urlImagen = urlImagen;
    }

    public String getIdProducto() { return idProducto; }
    public String getNombreProducto() { return nombreProducto; }
    public String getCategoria() { return categoria; }
    public String getEtiqueta() { return etiqueta; }
    public String getMarca() { return marca; }
    public String getMaterial() { return material; }
    public String getUnidadMedida() { return unidadMedida; }
    public String getDescripcion() { return descripcion; }
    public int getInventarioMin() { return inventarioMin; }
    public String getUrlImagen() { return urlImagen; }

    public void setIdProducto(String idProducto) { this.idProducto = idProducto; }
    public void setNombreProducto(String nombreProducto) { this.nombreProducto = nombreProducto; }
    public void setCategoria(String categoria) { this.categoria = categoria; }
    public void setEtiqueta(String etiqueta) { this.etiqueta = etiqueta; }
    public void setMarca(String marca) { this.marca = marca; }
    public void setMaterial(String material) { this.material = material; }
    public void setUnidadMedida(String unidadMedida) { this.unidadMedida = unidadMedida; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public void setInventarioMin(int inventarioMin) { this.inventarioMin = inventarioMin; }
    public void setUrlImagen(String urlImagen) { this.urlImagen = urlImagen; }
}
