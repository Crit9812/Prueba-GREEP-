package Formularios.controller;

import Formularios.model.modelNuevaSucursal;
import Consultas.sucursales.model.sucursal;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class controllerNuevaSucursal {

    @FXML private Label titulo;
    @FXML private TextField txtNombre;
    @FXML private TextField txtDomicilio;
    @FXML private TextField txtCP;
    @FXML private TextField txtColonia;
    @FXML private TextField txtNumeroExt;
    @FXML private TextField txtNumeroInt;
    @FXML private TextField txtCiudad;
    @FXML private TextField txtEstado;
    @FXML private TextField txtLocalidad;
    @FXML private TextField txtPais;
    @FXML private TextField txtCorreo;
    @FXML private TextField txtTelefono;
    @FXML private Button btnGuardar;

    private boolean modoEdicion = false;
    private int idSucursalEdicion = -1;

    private final modelNuevaSucursal model = new modelNuevaSucursal();
    private Runnable onSaved = null;

    @FXML
    private void initialize() {
        btnGuardar.setOnAction(e -> guardarSucursal());

        // Activar guardar con ENTER
        setEnterAction(txtNombre);
        setEnterAction(txtDomicilio);
        setEnterAction(txtCP);
        setEnterAction(txtColonia);
        setEnterAction(txtNumeroExt);
        setEnterAction(txtNumeroInt);
        setEnterAction(txtCiudad);
        setEnterAction(txtEstado);
        setEnterAction(txtLocalidad);
        setEnterAction(txtPais);
        setEnterAction(txtCorreo);
        setEnterAction(txtTelefono);
    }

    private void setEnterAction(TextField field) {
        field.setOnAction(e -> guardarSucursal());
    }

    public void setOnSaved(Runnable r) { this.onSaved = r; }

    // 👉 Se llama desde la ventana anterior cuando se quiere EDITAR
    public void cargarSucursal(sucursal s) {
        if (s == null) return;

        modoEdicion = true;
        idSucursalEdicion = s.getId();

        txtNombre.setText(s.getNombre());
        txtDomicilio.setText(s.getDomicilio());
        txtCP.setText(String.valueOf(s.getCp()));
        txtColonia.setText(s.getColonia());
        txtNumeroExt.setText(String.valueOf(s.getNumeroExt()));
        txtNumeroInt.setText(String.valueOf(s.getNumeroInt()));
        txtCiudad.setText(s.getCiudad());
        txtEstado.setText(s.getEstado());
        txtLocalidad.setText(s.getLocalidad());
        txtPais.setText(s.getPais());
        txtCorreo.setText(s.getCorreo());
        txtTelefono.setText(String.valueOf(s.getTelefono()));

        btnGuardar.setText("Actualizar");
        titulo.setText("Actualizar sucursal");
    }

    // 👉 Se llama al abrir para indicar que es NUEVO
    @FXML
    public void prepararNuevaSucursal() {
        modoEdicion = false;
        btnGuardar.setText("Guardar");
        titulo.setText("Agregar sucursal");
    }

    @FXML
    public void guardarSucursal() {

        try {
            if (txtNombre.getText().trim().isEmpty() ||
                    txtDomicilio.getText().trim().isEmpty() ||
                    txtCP.getText().trim().isEmpty() ||
                    txtColonia.getText().trim().isEmpty() ||
                    txtNumeroExt.getText().trim().isEmpty() ||
                    txtCiudad.getText().trim().isEmpty() ||
                    txtEstado.getText().trim().isEmpty() ||
                    txtLocalidad.getText().trim().isEmpty() ||
                    txtPais.getText().trim().isEmpty() ||
                    txtCorreo.getText().trim().isEmpty() ||
                    txtTelefono.getText().trim().isEmpty()) {

                new Alert(Alert.AlertType.WARNING,
                        "Todos los campos obligatorios deben estar completos."
                ).showAndWait();
                return;
            }

            // Crear / editar objeto
            sucursal s = new sucursal();
            if (modoEdicion) s.setId(idSucursalEdicion);

            s.setNombre(txtNombre.getText().trim());
            s.setDomicilio(txtDomicilio.getText().trim());
            s.setCp(Integer.parseInt(txtCP.getText().trim()));
            s.setColonia(txtColonia.getText().trim());
            s.setNumeroExt(Integer.parseInt(txtNumeroExt.getText().trim()));
            s.setNumeroInt(txtNumeroInt.getText().trim().isEmpty() ? 0 :
                    Integer.parseInt(txtNumeroInt.getText().trim()));
            s.setCiudad(txtCiudad.getText().trim());
            s.setEstado(txtEstado.getText().trim());
            s.setLocalidad(txtLocalidad.getText().trim());
            s.setPais(txtPais.getText().trim());
            s.setCorreo(txtCorreo.getText().trim());
            s.setTelefono(Integer.parseInt(txtTelefono.getText().trim()));

            boolean exito = modoEdicion ?
                    model.modificarSucursal(s) :
                    model.guardarSucursal(s);

            if (exito) {
                new Alert(Alert.AlertType.INFORMATION,
                        modoEdicion ? "Sucursal actualizada correctamente"
                                : "Sucursal agregada correctamente"
                ).showAndWait();

                if (onSaved != null) onSaved.run();
                Stage stage = (Stage) btnGuardar.getScene().getWindow();
                stage.close();

            } else {
                new Alert(Alert.AlertType.ERROR,
                        "Error al guardar la sucursal en la BD"
                ).showAndWait();
            }

        } catch (NumberFormatException e) {
            new Alert(Alert.AlertType.ERROR,
                    "Revisa CP, N° Interior, N° Exterior y Teléfono: deben ser numéricos"
            ).showAndWait();
        }
    }
}
