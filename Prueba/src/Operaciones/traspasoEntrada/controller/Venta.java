package Operaciones.traspasoEntrada.controller;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

public class Venta {
    private Integer colClaveEntrada;
    private String fecha;
    private String hora;
    private Double total;
    private String nombreSucursal;

    // Nueva propiedad para el CheckBox
    private BooleanProperty seleccionado = new SimpleBooleanProperty(false);

    public Venta(Integer colClaveEntrada, String fecha, String hora, Double total, String nombreSucursal) {
        this.colClaveEntrada = colClaveEntrada;
        this.fecha = fecha;
        this.hora = hora;
        this.total = total;
        this.nombreSucursal = nombreSucursal;
    }

    // Getters y setters normales
    public Integer getId() { return colClaveEntrada; }
    public void setId(Integer colClaveEntrada) { this.colClaveEntrada = colClaveEntrada; }

    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }

    public String getHora() { return hora; }
    public void setHora(String hora) { this.hora = hora; }

    public Double getTotal() { return total; }
    public void setTotal(Double total) { this.total = total; }

    public String getNombreSucursal() { return nombreSucursal; }
    public void setNombreSucursal(String nombreSucursal) { this.nombreSucursal = nombreSucursal; }

    // ---- Para el CheckBox en la tabla ----
    public BooleanProperty seleccionadoProperty() { return seleccionado; }

    public boolean isSeleccionado() { return seleccionado.get(); }
    public void setSeleccionado(boolean seleccionado) {
        this.seleccionado.set(seleccionado);
    }
}
