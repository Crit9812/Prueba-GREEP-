package Reportes.historial.model;

public class MovimientoFactura {
    private final String claveMovimiento;
    private final String fecha;
    private final String hora;
    private final String tipoMovimiento;
    private final String total;
    private final String usuario;
    private final String externo;
    private final String facturaExterna;
    private final String estado;

    public MovimientoFactura(String claveMovimiento,
                             String fecha,
                             String hora,
                             String tipoMovimiento,
                             String total,
                             String usuario,
                             String externo,
                             String facturaExterna,
                             String estado) {
        this.claveMovimiento = claveMovimiento;
        this.fecha = fecha;
        this.hora = hora;
        this.tipoMovimiento = tipoMovimiento;
        this.total = total;
        this.usuario = usuario;
        this.externo = externo;
        this.facturaExterna = facturaExterna;
        this.estado = estado;
    }

    public String getClaveMovimiento() { return claveMovimiento; }
    public String getFecha() { return fecha; }
    public String getHora() { return hora; }
    public String getTipoMovimiento() { return tipoMovimiento; }
    public String getTotal() { return total; }
    public String getUsuario() { return usuario; }
    public String getExterno() { return externo; }
    public String getFacturaExterna() { return facturaExterna; }
    public String getEstado() { return estado; }

    public boolean estaCanceladoPorCompleto() {
        if (estado == null) return false;
        String normalizado = estado.trim().toLowerCase();
        return normalizado.contains("cancel") || normalizado.equals("desactivado");
    }
}
