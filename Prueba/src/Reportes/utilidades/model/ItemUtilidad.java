package Reportes.utilidades.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class ItemUtilidad {
    private final StringProperty claveProducto = new SimpleStringProperty();
    private final StringProperty nombreProducto = new SimpleStringProperty();
    private final StringProperty categoria = new SimpleStringProperty();
    private final StringProperty descripcionProducto = new SimpleStringProperty();
    private final StringProperty presentacion = new SimpleStringProperty();
    private final StringProperty factor = new SimpleStringProperty();
    private final StringProperty cantidad = new SimpleStringProperty();
    private final StringProperty totalCompra = new SimpleStringProperty();
    private final StringProperty proveedor = new SimpleStringProperty();
    private final StringProperty totalVenta = new SimpleStringProperty();
    private final StringProperty cliente = new SimpleStringProperty();
    private final StringProperty porcentajeUtilidad = new SimpleStringProperty();
    private final StringProperty utilidad = new SimpleStringProperty();

    public ItemUtilidad(String claveProducto, String nombreProducto, String categoria,
                        String descripcionProducto, String presentacion, String factor,
                        String cantidad, String totalCompra, String proveedor,
                        String totalVenta, String cliente, String porcentajeUtilidad,
                        String utilidad) {
        this.claveProducto.set(claveProducto);
        this.nombreProducto.set(nombreProducto);
        this.categoria.set(categoria);
        this.descripcionProducto.set(descripcionProducto);
        this.presentacion.set(presentacion);
        this.factor.set(factor);
        this.cantidad.set(cantidad);
        this.totalCompra.set(totalCompra);
        this.proveedor.set(proveedor);
        this.totalVenta.set(totalVenta);
        this.cliente.set(cliente);
        this.porcentajeUtilidad.set(porcentajeUtilidad);
        this.utilidad.set(utilidad);
    }

    public String getClaveProducto() {
        return claveProducto.get();
    }

    public StringProperty claveProductoProperty() {
        return claveProducto;
    }

    public String getNombreProducto() {
        return nombreProducto.get();
    }

    public StringProperty nombreProductoProperty() {
        return nombreProducto;
    }

    public String getCategoria() {
        return categoria.get();
    }

    public StringProperty categoriaProperty() {
        return categoria;
    }

    public String getDescripcionProducto() {
        return descripcionProducto.get();
    }

    public StringProperty descripcionProductoProperty() {
        return descripcionProducto;
    }

    public String getPresentacion() {
        return presentacion.get();
    }

    public StringProperty presentacionProperty() {
        return presentacion;
    }

    public String getFactor() {
        return factor.get();
    }

    public StringProperty factorProperty() {
        return factor;
    }

    public String getCantidad() {
        return cantidad.get();
    }

    public StringProperty cantidadProperty() {
        return cantidad;
    }

    public String getTotalCompra() {
        return totalCompra.get();
    }

    public StringProperty totalCompraProperty() {
        return totalCompra;
    }

    public String getProveedor() {
        return proveedor.get();
    }

    public StringProperty proveedorProperty() {
        return proveedor;
    }

    public String getTotalVenta() {
        return totalVenta.get();
    }

    public StringProperty totalVentaProperty() {
        return totalVenta;
    }

    public String getCliente() {
        return cliente.get();
    }

    public StringProperty clienteProperty() {
        return cliente;
    }

    public String getPorcentajeUtilidad() {
        return porcentajeUtilidad.get();
    }

    public StringProperty porcentajeUtilidadProperty() {
        return porcentajeUtilidad;
    }

    public String getUtilidad() {
        return utilidad.get();
    }

    public StringProperty utilidadProperty() {
        return utilidad;
    }
}
