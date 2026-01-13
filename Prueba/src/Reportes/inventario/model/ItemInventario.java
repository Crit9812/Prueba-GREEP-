package Reportes.inventario.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class ItemInventario {
    private final StringProperty claveProducto = new SimpleStringProperty();
    private final StringProperty producto = new SimpleStringProperty();
    private final StringProperty marca = new SimpleStringProperty();
    private final StringProperty categoria = new SimpleStringProperty();
    private final StringProperty material = new SimpleStringProperty();
    private final StringProperty unidadMedida = new SimpleStringProperty();
    private final StringProperty presentacion = new SimpleStringProperty();
    private final StringProperty factor = new SimpleStringProperty();
    private final StringProperty descripcion = new SimpleStringProperty();
    private final StringProperty inventarioMinimo = new SimpleStringProperty();

    public ItemInventario(String claveProducto, String producto, String marca, String categoria,
                          String material, String unidadMedida, String presentacion, String factor,
                          String descripcion, String inventarioMinimo) {
        this.claveProducto.set(claveProducto);
        this.producto.set(producto);
        this.marca.set(marca);
        this.categoria.set(categoria);
        this.material.set(material);
        this.unidadMedida.set(unidadMedida);
        this.presentacion.set(presentacion);
        this.factor.set(factor);
        this.descripcion.set(descripcion);
        this.inventarioMinimo.set(inventarioMinimo);
    }

    public String getClaveProducto() {
        return claveProducto.get();
    }

    public StringProperty claveProductoProperty() {
        return claveProducto;
    }

    public void setClaveProducto(String claveProducto) {
        this.claveProducto.set(claveProducto);
    }

    public String getProducto() {
        return producto.get();
    }

    public StringProperty productoProperty() {
        return producto;
    }

    public void setProducto(String producto) {
        this.producto.set(producto);
    }

    public String getMarca() {
        return marca.get();
    }

    public StringProperty marcaProperty() {
        return marca;
    }

    public void setMarca(String marca) {
        this.marca.set(marca);
    }

    public String getCategoria() {
        return categoria.get();
    }

    public StringProperty categoriaProperty() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria.set(categoria);
    }

    public String getMaterial() {
        return material.get();
    }

    public StringProperty materialProperty() {
        return material;
    }

    public void setMaterial(String material) {
        this.material.set(material);
    }

    public String getUnidadMedida() {
        return unidadMedida.get();
    }

    public StringProperty unidadMedidaProperty() {
        return unidadMedida;
    }

    public void setUnidadMedida(String unidadMedida) {
        this.unidadMedida.set(unidadMedida);
    }

    public String getPresentacion() {
        return presentacion.get();
    }

    public StringProperty presentacionProperty() {
        return presentacion;
    }

    public void setPresentacion(String presentacion) {
        this.presentacion.set(presentacion);
    }

    public String getFactor() {
        return factor.get();
    }

    public StringProperty factorProperty() {
        return factor;
    }

    public void setFactor(String factor) {
        this.factor.set(factor);
    }

    public String getDescripcion() {
        return descripcion.get();
    }

    public StringProperty descripcionProperty() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion.set(descripcion);
    }

    public String getInventarioMinimo() {
        return inventarioMinimo.get();
    }

    public StringProperty inventarioMinimoProperty() {
        return inventarioMinimo;
    }

    public void setInventarioMinimo(String inventarioMinimo) {
        this.inventarioMinimo.set(inventarioMinimo);
    }
}
