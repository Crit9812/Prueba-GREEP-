package Consultas.producto.model;

import Compartido.model.DAO.Column;
import Compartido.model.DAO.PrimaryKey;
import Compartido.model.DAO.Table;

@Table(name = "etiquetas")
public class etiqueta {

    @PrimaryKey
    @Column(name = "id")
    private String id;

    @Column(name = "nombre")
    private String nombre;

    // Constructor vacío necesario para el DAO
    public etiqueta() {}

    public etiqueta(String id, String nombre) {
        this.id = id;
        this.nombre = nombre;
    }

    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    @Override
    public String toString() {
        return nombre;
    }
}