package Operaciones.pedidos.model;

import javafx.beans.property.*;

public class itemPedido {

    private final StringProperty claveProducto = new SimpleStringProperty();
    private final StringProperty producto = new SimpleStringProperty();
    private final StringProperty descripcion = new SimpleStringProperty();
    private final IntegerProperty cantidad = new SimpleIntegerProperty();
    private final BooleanProperty seleccionado = new SimpleBooleanProperty(false);
    private final StringProperty claveAlterna = new SimpleStringProperty("");
    private final StringProperty presentacion = new SimpleStringProperty("");
    private final StringProperty factor = new SimpleStringProperty("");

    // Constructor original (sin clave alterna, presentación ni factor)
    public itemPedido(String claveProducto, String producto, String descripcion, int cantidad) {
        this.claveProducto.set(claveProducto);
        this.producto.set(producto);
        this.descripcion.set(descripcion);
        this.cantidad.set(cantidad);
        this.seleccionado.set(false);
        this.presentacion.set("");
        this.factor.set("");
    }

    // Constructor con clave alterna
    public itemPedido(String claveProducto, String producto, String descripcion, int cantidad, String claveAlterna) {
        this.claveProducto.set(claveProducto);
        this.producto.set(producto);
        this.descripcion.set(descripcion);
        this.cantidad.set(cantidad);
        this.seleccionado.set(false);
        this.claveAlterna.set(claveAlterna != null ? claveAlterna : "");
        this.presentacion.set("");
        this.factor.set("");
    }

    // Constructor completo con todos los campos
    public itemPedido(String claveProducto, String producto, String descripcion, int cantidad,
                      String claveAlterna, String presentacion, String factor) {
        this.claveProducto.set(claveProducto);
        this.producto.set(producto);
        this.descripcion.set(descripcion);
        this.cantidad.set(cantidad);
        this.seleccionado.set(false);
        this.claveAlterna.set(claveAlterna != null ? claveAlterna : "");
        this.presentacion.set(presentacion != null ? presentacion : "");
        this.factor.set(factor != null ? factor : "");
    }

    // Getters y Setters para claveProducto
    public String getClaveProducto() {
        return claveProducto.get();
    }

    public StringProperty claveProductoProperty() {
        return claveProducto;
    }

    public void setClaveProducto(String claveProducto) {
        this.claveProducto.set(claveProducto);
    }

    // Getters y Setters para producto
    public String getProducto() {
        return producto.get();
    }

    public StringProperty productoProperty() {
        return producto;
    }

    public void setProducto(String producto) {
        this.producto.set(producto);
    }

    // Getters y Setters para descripcion
    public String getDescripcion() {
        return descripcion.get();
    }

    public StringProperty descripcionProperty() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion.set(descripcion);
    }

    // Getters y Setters para cantidad
    public int getCantidad() {
        return cantidad.get();
    }

    public IntegerProperty cantidadProperty() {
        return cantidad;
    }

    public void setCantidad(int cantidad) {
        this.cantidad.set(cantidad);
    }

    // Getters y Setters para seleccionado
    public boolean isSeleccionado() {
        return seleccionado.get();
    }

    public BooleanProperty seleccionadoProperty() {
        return seleccionado;
    }

    public void setSeleccionado(boolean seleccionado) {
        this.seleccionado.set(seleccionado);
    }

    // Getters y Setters para claveAlterna
    public String getClaveAlterna() {
        return claveAlterna.get();
    }

    public StringProperty claveAlternaProperty() {
        return claveAlterna;
    }

    public void setClaveAlterna(String claveAlterna) {
        this.claveAlterna.set(claveAlterna);
    }

    // Getters y Setters para presentacion
    public String getPresentacion() {
        return presentacion.get();
    }

    public StringProperty presentacionProperty() {
        return presentacion;
    }

    public void setPresentacion(String presentacion) {
        this.presentacion.set(presentacion);
    }

    // Getters y Setters para factor
    public String getFactor() {
        return factor.get();
    }

    public StringProperty factorProperty() {
        return factor;
    }

    public void setFactor(String factor) {
        this.factor.set(factor);
    }

    // 🔥 NUEVO: Sobreescribir equals para comparar items
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        itemPedido that = (itemPedido) obj;

        // Comparar por clave de producto, nombre y cantidad
        // Puedes agregar más campos si necesitas mayor precisión
        boolean claveIgual = this.claveProducto.get() != null &&
                this.claveProducto.get().equals(that.claveProducto.get());
        boolean nombreIgual = this.producto.get() != null &&
                this.producto.get().equals(that.producto.get());
        boolean cantidadIgual = this.cantidad.get() == that.cantidad.get();

        return claveIgual && nombreIgual && cantidadIgual;
    }

    // 🔥 NUEVO: Sobreescribir hashCode para consistencia con equals
    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + (claveProducto.get() != null ? claveProducto.get().hashCode() : 0);
        result = 31 * result + (producto.get() != null ? producto.get().hashCode() : 0);
        result = 31 * result + cantidad.get();
        return result;
    }

    // 🔥 NUEVO: Método para copiar los valores de otro item
    public void copiarDe(itemPedido otroItem) {
        if (otroItem != null) {
            this.setClaveProducto(otroItem.getClaveProducto());
            this.setProducto(otroItem.getProducto());
            this.setDescripcion(otroItem.getDescripcion());
            this.setCantidad(otroItem.getCantidad());
            this.setClaveAlterna(otroItem.getClaveAlterna());
            this.setPresentacion(otroItem.getPresentacion());
            this.setFactor(otroItem.getFactor());
            this.setSeleccionado(otroItem.isSeleccionado());
        }
    }

    // 🔥 NUEVO: Método para crear una copia del item
    public itemPedido copiar() {
        return new itemPedido(
                this.getClaveProducto(),
                this.getProducto(),
                this.getDescripcion(),
                this.getCantidad(),
                this.getClaveAlterna(),
                this.getPresentacion(),
                this.getFactor()
        );
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(claveProducto.get())
                .append(" - ")
                .append(producto.get())
                .append(" (")
                .append(cantidad.get())
                .append(")");

        if (claveAlterna.get() != null && !claveAlterna.get().isEmpty()) {
            sb.append(" [").append(claveAlterna.get()).append("]");
        }

        if (presentacion.get() != null && !presentacion.get().isEmpty() &&
                !presentacion.get().equals("Pieza")) {
            sb.append(" | Pres: ").append(presentacion.get());
        }

        if (factor.get() != null && !factor.get().isEmpty() &&
                !factor.get().equals("1") && !factor.get().equals("1.0")) {
            sb.append(" | Factor: ").append(factor.get());
        }

        return sb.toString();
    }

    // 🔥 NUEVO: Método para validar si el item está completo
    public boolean esValido() {
        return claveProducto.get() != null && !claveProducto.get().isEmpty() &&
                producto.get() != null && !producto.get().isEmpty() &&
                cantidad.get() > 0;
    }

    // 🔥 NUEVO: Método para obtener representación resumida
    public String getResumen() {
        return producto.get() + " x" + cantidad.get() + " " +
                (presentacion.get() != null && !presentacion.get().isEmpty() ?
                        presentacion.get() : "unidades");
    }
}