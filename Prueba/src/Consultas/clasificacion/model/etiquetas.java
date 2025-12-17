package Consultas.clasificacion.model;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class etiquetas {

    private final IntegerProperty id;
    private final StringProperty nombre;

    public etiquetas(int id, String nombre) {
        this.id = new SimpleIntegerProperty(id);
        this.nombre = new SimpleStringProperty(nombre);
    }

    // ===== GETTERS =====
    public int getId() {
        return id.get();
    }

    public String getNombre() {
        return nombre.get();
    }

    // ===== SETTERS =====
    public void setNombre(String nombre) {
        this.nombre.set(nombre);
    }

    // ===== PROPERTIES =====
    public IntegerProperty idProperty() {
        return id;
    }

    public StringProperty nombreProperty() {
        return nombre;
    }
}
