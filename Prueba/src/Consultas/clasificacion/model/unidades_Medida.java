package Consultas.clasificacion.model;

import Compartido.model.DAO.Column;
import Compartido.model.DAO.PrimaryKey;
import Compartido.model.DAO.Table;

@Table(name = "unidades_Medida")
public class unidades_Medida {

    @PrimaryKey
    @Column(name = "id")
    private Integer id;

    @Column(name = "nombre")
    private String nombre;

    public unidades_Medida() {}

    public unidades_Medida(Integer id, String nombre) {
        this.id = id;
        this.nombre = nombre;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
}
