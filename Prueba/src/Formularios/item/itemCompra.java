package Formularios.item;

import javafx.beans.property.*;

public class itemCompra {
    private final IntegerProperty idArticulo;
    private final StringProperty claveProducto;
    private final StringProperty claveAlterna;
    private final StringProperty producto;
    private final StringProperty descripcion;
    private final IntegerProperty cantidad;
    private final StringProperty lote;
    private final StringProperty caducidad;
    private final StringProperty presentacion;
    private final IntegerProperty factor;
    private final DoubleProperty precioEntrada;
    private final BooleanProperty iva;
    private final DoubleProperty precioIVA;
    private final DoubleProperty precioBruto;
    private final DoubleProperty precioTotal;
    private final StringProperty ubicaciones;
    private final BooleanProperty seleccionado;
    private final IntegerProperty idProveedor;
    private final StringProperty nombreProveedor;
    private final StringProperty noFactura;
    private final StringProperty nota;

    public itemCompra() {
        this.idArticulo = new SimpleIntegerProperty();
        this.claveProducto = new SimpleStringProperty();
        this.claveAlterna = new SimpleStringProperty();
        this.producto = new SimpleStringProperty();
        this.descripcion = new SimpleStringProperty();
        this.cantidad = new SimpleIntegerProperty();
        this.lote = new SimpleStringProperty();
        this.caducidad = new SimpleStringProperty();
        this.presentacion = new SimpleStringProperty();
        this.factor = new SimpleIntegerProperty();
        this.precioEntrada = new SimpleDoubleProperty();
        this.iva = new SimpleBooleanProperty();
        this.precioIVA = new SimpleDoubleProperty();
        this.precioBruto = new SimpleDoubleProperty();
        this.precioTotal = new SimpleDoubleProperty();
        this.ubicaciones = new SimpleStringProperty();
        this.seleccionado = new SimpleBooleanProperty(false);
        this.idProveedor = new SimpleIntegerProperty();
        this.nombreProveedor = new SimpleStringProperty();
        this.noFactura = new SimpleStringProperty();
        this.nota = new SimpleStringProperty();
    }

    // Constructor útil
    public itemCompra(String claveProducto, String claveAlterna, String producto, String descripcion, int cantidad) {
        this();
        setClaveProducto(claveProducto);
        setClaveAlterna(claveAlterna);
        setProducto(producto);
        setDescripcion(descripcion);
        setCantidad(cantidad);
    }

    public int getIdArticulo() { return idArticulo.get(); }
    public void setIdArticulo(int id) { this.idArticulo.set(id); }
    public IntegerProperty idArticuloProperty() { return idArticulo; }

    public String getClaveProducto() { return claveProducto.get(); }
    public void setClaveProducto(String clave) { this.claveProducto.set(clave); }
    public StringProperty claveProductoProperty() { return claveProducto; }

    public String getClaveAlterna() { return claveAlterna.get(); }
    public void setClaveAlterna(String clave) { this.claveAlterna.set(clave); }
    public StringProperty claveAlternaProperty() { return claveAlterna; }

    public String getProducto() { return producto.get(); }
    public void setProducto(String producto) { this.producto.set(producto); }
    public StringProperty productoProperty() { return producto; }

    public String getDescripcion() { return descripcion.get(); }
    public void setDescripcion(String desc) { this.descripcion.set(desc); }
    public StringProperty descripcionProperty() { return descripcion; }

    public int getCantidad() { return cantidad.get(); }
    public void setCantidad(int cantidad) { this.cantidad.set(cantidad); }
    public IntegerProperty cantidadProperty() { return cantidad; }

    public String getLote() { return lote.get(); }
    public void setLote(String lote) { this.lote.set(lote); }
    public StringProperty loteProperty() { return lote; }

    public String getCaducidad() { return caducidad.get(); }
    public void setCaducidad(String caducidad) { this.caducidad.set(caducidad); }
    public StringProperty caducidadProperty() { return caducidad; }

    public String getPresentacion() { return presentacion.get(); }
    public void setPresentacion(String presentacion) { this.presentacion.set(presentacion); }
    public StringProperty presentacionProperty() { return presentacion; }

    public int getFactor() { return factor.get(); }
    public void setFactor(int factor) { this.factor.set(factor); }
    public IntegerProperty factorProperty() { return factor; }

    public double getPrecioEntrada() { return precioEntrada.get(); }
    public void setPrecioEntrada(double precio) { this.precioEntrada.set(precio); }
    public DoubleProperty precioEntradaProperty() { return precioEntrada; }

    public boolean isIva() { return iva.get(); }
    public void setIva(boolean iva) { this.iva.set(iva); }
    public BooleanProperty ivaProperty() { return iva; }

    public double getPrecioIVA() { return precioIVA.get(); }
    public void setPrecioIVA(double precio) { this.precioIVA.set(precio); }
    public DoubleProperty precioIVAProperty() { return precioIVA; }

    public double getPrecioBruto() { return precioBruto.get(); }
    public void setPrecioBruto(double precio) { this.precioBruto.set(precio); }
    public DoubleProperty precioBrutoProperty() { return precioBruto; }

    public double getPrecioTotal() { return precioTotal.get(); }
    public void setPrecioTotal(double precio) { this.precioTotal.set(precio); }
    public DoubleProperty precioTotalProperty() { return precioTotal; }

    public String getUbicaciones() { return ubicaciones.get(); }
    public void setUbicaciones(String ubicaciones) { this.ubicaciones.set(ubicaciones); }
    public StringProperty ubicacionesProperty() { return ubicaciones; }

    public boolean isSeleccionado() { return seleccionado.get(); }
    public void setSeleccionado(boolean seleccionado) { this.seleccionado.set(seleccionado); }
    public BooleanProperty seleccionadoProperty() { return seleccionado; }

    public int getIdProveedor() { return idProveedor.get(); }
    public void setIdProveedor(int id) { this.idProveedor.set(id); }
    public IntegerProperty idProveedorProperty() { return idProveedor; }

    public String getNombreProveedor() { return nombreProveedor.get(); }
    public void setNombreProveedor(String nombre) { this.nombreProveedor.set(nombre); }
    public StringProperty nombreProveedorProperty() { return nombreProveedor; }

    public String getNoFactura() { return noFactura.get(); }
    public void setNoFactura(String factura) { this.noFactura.set(factura); }
    public StringProperty noFacturaProperty() { return noFactura; }

    public String getNota() { return nota.get(); }
    public void setNota(String nota) { this.nota.set(nota); }
    public StringProperty notaProperty() { return nota; }
}
