package Consultas.claves.model;

import Compartido.model.DAO.Table;
import Compartido.model.DAO.Column;
import Compartido.model.DAO.PrimaryKey;

@Table(name = "claves")
public class claves {

    @PrimaryKey
    @Column(name = "idAlterno")
    private String idClaveCatalogo;

    @Column(name = "claveProveedor")
    private Integer claveProveedor;

    @Column(name = "claveGreep")
    private String claveGreep;

    @Column(name = "idProducto")
    private String idProducto; // relación con productos

    @Column(name = "idProveedor")
    private Integer idProveedor; // relación con proveedores

    @Column(name = "estado")
    private String estado;

    public claves() {}

    public claves(String idClaveCatalogo, Integer claveProveedor, String claveGreep, String idProducto, Integer idProveedor) {
        this.idClaveCatalogo = idClaveCatalogo;
        this.claveProveedor = claveProveedor;
        this.claveGreep = claveGreep;
        this.idProducto = idProducto;
        this.idProveedor = idProveedor;
    }

    public String getIdClaveCatalogo() { return idClaveCatalogo; }
    public void setIdClaveCatalogo(String idClaveCatalogo) { this.idClaveCatalogo = idClaveCatalogo; }

    public Integer getClaveProveedor() { return claveProveedor; }
    public void setClaveProveedor(Integer claveProveedor) { this.claveProveedor = claveProveedor; }

    public String getClaveGreep() { return claveGreep; }
    public void setClaveGreep(String claveGreep) { this.claveGreep = claveGreep; }

    public String getIdProducto() { return idProducto; }
    public void setIdProducto(String idProducto) { this.idProducto = idProducto; }

    public Integer getIdProveedor() { return idProveedor; }
    public void setIdProveedor(Integer idProveedor) { this.idProveedor = idProveedor; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
}
