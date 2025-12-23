package Operaciones.compra.model;

import javafx.beans.property.*;

public class itemCompra {

    private final BooleanProperty seleccionado = new SimpleBooleanProperty(false);
    private final StringProperty claveProducto = new SimpleStringProperty();
    private final StringProperty claveAlterna = new SimpleStringProperty();
    private final StringProperty producto = new SimpleStringProperty();
    private final StringProperty descripcion = new SimpleStringProperty();
    private final StringProperty lote = new SimpleStringProperty();
    private final StringProperty caducidad = new SimpleStringProperty();
    private final StringProperty ubicacion = new SimpleStringProperty();
    private final IntegerProperty cantidad = new SimpleIntegerProperty();
    private final StringProperty presentacion = new SimpleStringProperty();
    private final StringProperty factor = new SimpleStringProperty();
    private final DoubleProperty precioEntrada = new SimpleDoubleProperty();
    private final DoubleProperty precioIva = new SimpleDoubleProperty();
    private final DoubleProperty precioBruto = new SimpleDoubleProperty();
    private final DoubleProperty precioTotal = new SimpleDoubleProperty();

    public itemCompra(String claveProducto,
                      String claveAlterna,
                      String producto,
                      String descripcion,
                      String lote,
                      String caducidad,
                      String ubicacion,
                      int cantidad,
                      String presentacion,
                      String factor,
                      double precioEntrada,
                      double precioIva,
                      double precioBruto,
                      double precioTotal) {
        this.claveProducto.set(claveProducto);
        this.claveAlterna.set(claveAlterna);
        this.producto.set(producto);
        this.descripcion.set(descripcion);
        this.lote.set(lote);
        this.caducidad.set(caducidad);
        this.ubicacion.set(ubicacion);
        this.cantidad.set(cantidad);
        this.presentacion.set(presentacion);
        this.factor.set(factor);
        this.precioEntrada.set(precioEntrada);
        this.precioIva.set(precioIva);
        this.precioBruto.set(precioBruto);
        this.precioTotal.set(precioTotal);
    }

    public boolean isSeleccionado() {
        return seleccionado.get();
    }

    public void setSeleccionado(boolean value) {
        seleccionado.set(value);
    }

    public BooleanProperty seleccionadoProperty() {
        return seleccionado;
    }

    public String getClaveProducto() {
        return claveProducto.get();
    }

    public StringProperty claveProductoProperty() {
        return claveProducto;
    }

    public String getClaveAlterna() {
        return claveAlterna.get();
    }

    public StringProperty claveAlternaProperty() {
        return claveAlterna;
    }

    public String getProducto() {
        return producto.get();
    }

    public StringProperty productoProperty() {
        return producto;
    }

    public String getDescripcion() {
        return descripcion.get();
    }

    public StringProperty descripcionProperty() {
        return descripcion;
    }

    public String getLote() {
        return lote.get();
    }

    public StringProperty loteProperty() {
        return lote;
    }

    public String getCaducidad() {
        return caducidad.get();
    }

    public StringProperty caducidadProperty() {
        return caducidad;
    }

    public String getUbicacion() {
        return ubicacion.get();
    }

    public StringProperty ubicacionProperty() {
        return ubicacion;
    }

    public int getCantidad() {
        return cantidad.get();
    }

    public IntegerProperty cantidadProperty() {
        return cantidad;
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

    public double getPrecioEntrada() {
        return precioEntrada.get();
    }

    public DoubleProperty precioEntradaProperty() {
        return precioEntrada;
    }

    public double getPrecioIva() {
        return precioIva.get();
    }

    public DoubleProperty precioIvaProperty() {
        return precioIva;
    }

    public double getPrecioBruto() {
        return precioBruto.get();
    }

    public DoubleProperty precioBrutoProperty() {
        return precioBruto;
    }

    public double getPrecioTotal() {
        return precioTotal.get();
    }

    public DoubleProperty precioTotalProperty() {
        return precioTotal;
    }
}
