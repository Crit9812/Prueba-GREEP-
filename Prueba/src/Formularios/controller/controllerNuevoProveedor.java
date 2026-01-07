package Formularios.controller;

import Formularios.model.modelNuevoProveedor;
import Consultas.proveedores.model.proveedores;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.application.Platform;

public class controllerNuevoProveedor {

    @FXML private Label titulo;
    @FXML private TextField txtNombre;
    @FXML private TextField txtRepresentante;
    @FXML private TextField txtRFC;
    @FXML private TextField txtCURP;
    @FXML private TextField txtRazonSocial;
    @FXML private TextField txtDomicilio;
    @FXML private TextField txtCP;
    @FXML private TextField txtColonia;
    @FXML private TextField txtNoInt;
    @FXML private TextField txtNoExt;
    @FXML private TextField txtCiudad;
    @FXML private TextField txtEstado;
    @FXML private TextField txtLocalidad;
    @FXML private TextField txtPais;
    @FXML private TextField txtCorreo;
    @FXML private TextField txtTelefono;
    @FXML private Button btnGuardar;

    private boolean modoEdicion = false;
    private int idProveedorEdicion = -1;

    private final modelNuevoProveedor model = new modelNuevoProveedor();
    private Runnable onSaved = null;

    // 👉 Este método se ejecuta automáticamente al cargar escena
    @FXML
    private void initialize() {
        btnGuardar.setOnAction(e -> guardarProveedor());

        // 👉 Todos los TextField ejecutarán guardarProveedor() al presionar ENTER
        setEnterAction(txtNombre);
        setEnterAction(txtRepresentante);
        setEnterAction(txtRFC);
        setEnterAction(txtCURP);
        setEnterAction(txtRazonSocial);
        setEnterAction(txtDomicilio);
        setEnterAction(txtCP);
        setEnterAction(txtColonia);
        setEnterAction(txtNoInt);
        setEnterAction(txtNoExt);
        setEnterAction(txtCiudad);
        setEnterAction(txtEstado);
        setEnterAction(txtLocalidad);
        setEnterAction(txtPais);
        setEnterAction(txtCorreo);
        setEnterAction(txtTelefono);
    }

    private void setEnterAction(TextField field) {
        field.setOnAction(e -> guardarProveedor());
    }

    public void setOnSaved(Runnable r) { this.onSaved = r; }

    public void cargarProveedor(proveedores p) {
        if (p == null) return;

        modoEdicion = true;
        idProveedorEdicion = p.getId();

        txtNombre.setText(p.getNombre());
        txtRepresentante.setText(p.getRepresentante());
        txtRFC.setText(p.getRfc());
        txtCURP.setText(p.getCurp());
        txtRazonSocial.setText(p.getRazonSocial());
        txtDomicilio.setText(p.getDomicilio());
        txtCP.setText(String.valueOf(p.getCp()));
        txtColonia.setText(p.getColonia());
        txtNoInt.setText(String.valueOf(p.getNumeroInt()));
        txtNoExt.setText(String.valueOf(p.getNumeroExt()));
        txtCiudad.setText(p.getCiudad());
        txtEstado.setText(p.getEstado());
        txtLocalidad.setText(p.getLocalidad());
        txtPais.setText(p.getPais());
        txtCorreo.setText(p.getCorreo());
        txtTelefono.setText(String.valueOf(p.getTelefono()));

        btnGuardar.setText("Actualizar");
        titulo.setText("Actualizar proveedor");
    }

    @FXML
    public void prepararNuevoProveedor() {
        modoEdicion = false;
        btnGuardar.setText("Guardar");
        titulo.setText("Agregar proveedor");
    }

    @FXML
    public void guardarProveedor() {
        try {
            proveedores p = new proveedores();

            if (modoEdicion) p.setId(idProveedorEdicion);

            p.setNombre(txtNombre.getText());
            p.setRepresentante(txtRepresentante.getText());
            p.setRfc(txtRFC.getText());
            p.setCurp(txtCURP.getText());
            p.setRazonSocial(txtRazonSocial.getText());
            p.setDomicilio(txtDomicilio.getText());
            p.setCp(Integer.parseInt(txtCP.getText()));
            p.setColonia(txtColonia.getText());
            p.setNumeroInt(Integer.parseInt(txtNoInt.getText()));
            p.setNumeroExt(Integer.parseInt(txtNoExt.getText()));
            p.setCiudad(txtCiudad.getText());
            p.setEstado(txtEstado.getText());
            p.setLocalidad(txtLocalidad.getText());
            p.setPais(txtPais.getText());
            p.setCorreo(txtCorreo.getText());
            p.setTelefono(Integer.parseInt(txtTelefono.getText()));

            boolean exito = modoEdicion ?
                    model.modificarProveedor(p) :
                    model.agregarProveedor(p);

            if (exito) {
                String mensaje = modoEdicion ?
                        "Proveedor actualizado correctamente" :
                        "Proveedor agregado correctamente";

                new Alert(Alert.AlertType.INFORMATION, mensaje).showAndWait();

                // 👉 NUEVO: Ejecutar callback si existe (para notificar al controller principal)
                if (onSaved != null) {
                    onSaved.run();
                }

                // 👉 NUEVO: Cerrar la ventana después de un breve retardo
                Platform.runLater(() -> {
                    Stage stage = (Stage) btnGuardar.getScene().getWindow();
                    stage.close();
                });

            } else {
                new Alert(Alert.AlertType.ERROR,
                        "Error al guardar proveedor en la BD"
                ).showAndWait();
            }

        } catch (NumberFormatException e) {
            new Alert(Alert.AlertType.ERROR,
                    "Revisa CP, N° Interior, N° Exterior y Teléfono: deben ser numéricos"
            ).showAndWait();
        }
    }

    public String getNombreProveedor() {
        return txtNombre.getText().trim();
    }
}
