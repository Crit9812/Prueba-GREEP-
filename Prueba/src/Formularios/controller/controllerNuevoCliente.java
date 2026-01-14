package Formularios.controller;

import Compartido.helper.AutoCompleteComboBoxListener;
import Compartido.helper.CodigoPostalService;
import Formularios.model.modelNuevoCliente;
import Consultas.clientes.model.cliente;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.application.Platform;
import javafx.stage.Stage;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class controllerNuevoCliente {

    @FXML private Label titulo;
    @FXML private TextField txtNombre;
    @FXML private TextField txtRFC;
    @FXML private TextField txtCURP;
    @FXML private TextField txtRazonSocial;
    @FXML private TextField txtCP;
    @FXML private TextField txtPais;
    @FXML private TextField txtEstado;
    @FXML private TextField txtLocalidad;
    @FXML private TextField txtCiudad;
    @FXML private ComboBox<String> cbColonia;
    @FXML private TextField txtDomicilio;
    @FXML private TextField txtNoExt;
    @FXML private TextField txtNoInt;
    @FXML private TextField txtCorreoElectronico;
    @FXML private TextField txtTelefono;
    @FXML private Button btnGuardar;

    private boolean modoEdicion = false;
    private int idClienteEdicion = -1;

    private final modelNuevoCliente model = new modelNuevoCliente();
    private final CodigoPostalService codigoPostalService = new CodigoPostalService();
    private Runnable onSaved = null;
    private String ultimoCpConsultado = "";
    private String coloniaInicial = null;

    @FXML
    private void initialize() {
        btnGuardar.setOnAction(e -> guardarCliente());

        // 👉 Al presionar ENTER en cualquier campo, se guarda automáticamente
        setEnterAction(txtNombre);
        setEnterAction(txtRFC);
        setEnterAction(txtCURP);
        setEnterAction(txtRazonSocial);
        setEnterAction(txtCP);
        setEnterAction(txtPais);
        setEnterAction(txtEstado);
        setEnterAction(txtLocalidad);
        setEnterAction(txtCiudad);
        setEnterAction(txtDomicilio);
        setEnterAction(txtNoExt);
        setEnterAction(txtNoInt);
        setEnterAction(txtCorreoElectronico);
        setEnterAction(txtTelefono);

        cbColonia.setEditable(true);
        new AutoCompleteComboBoxListener<>(cbColonia);
        cbColonia.getEditor().setOnAction(e -> guardarCliente());

        txtCP.textProperty().addListener((obs, oldValue, newValue) -> manejarCambioCp(newValue));
    }

    private void setEnterAction(TextField field) {
        field.setOnAction(e -> guardarCliente());
    }

    public void setOnSaved(Runnable r) { this.onSaved = r; }

    // 👉 Ejecutar cuando es edición
    public void cargarCliente(cliente c) {
        if (c == null) return;

        modoEdicion = true;
        idClienteEdicion = c.getId();

        txtNombre.setText(c.getNombre());
        txtRFC.setText(c.getRfc());
        txtCURP.setText(c.getCurp());
        txtRazonSocial.setText(c.getRazonSocial());
        coloniaInicial = c.getColonia();
        txtCP.setText(String.valueOf(c.getCp()));
        txtPais.setText(c.getPais());
        txtEstado.setText(c.getEstado());
        txtLocalidad.setText(c.getLocalidad());
        txtCiudad.setText(c.getCiudad());
        cbColonia.setValue(coloniaInicial);
        txtDomicilio.setText(c.getDomicilio());
        txtNoExt.setText(String.valueOf(c.getNumeroExt()));
        txtNoInt.setText(String.valueOf(c.getNumeroInt()));
        txtCorreoElectronico.setText(c.getCorreo());
        txtTelefono.setText(String.valueOf(c.getTelefono()));

        btnGuardar.setText("Actualizar");
        titulo.setText("Actualizar cliente");
    }

    // 👉 Ejecutar al abrir formulario "nuevo"
    @FXML
    public void prepararNuevoCliente() {
        modoEdicion = false;
        btnGuardar.setText("Guardar");
        titulo.setText("Agregar cliente");
        coloniaInicial = null;
        ultimoCpConsultado = "";
    }

    // En controllerNuevoCliente.java, modifica el método guardarCliente():
    @FXML
    public void guardarCliente() {

        if (!validarNumericos()) return;

        try {
            cliente c = new cliente();
            if (modoEdicion) c.setId(idClienteEdicion);

            c.setNombre(txtNombre.getText());
            c.setRfc(txtRFC.getText());
            c.setCurp(txtCURP.getText());
            c.setRazonSocial(txtRazonSocial.getText());
            c.setCp(Integer.parseInt(txtCP.getText()));
            c.setPais(txtPais.getText());
            c.setEstado(txtEstado.getText());
            c.setLocalidad(txtLocalidad.getText());
            c.setCiudad(txtCiudad.getText());
            c.setColonia(obtenerColoniaSeleccionada());
            c.setDomicilio(txtDomicilio.getText());
            c.setNumeroExt(Integer.parseInt(txtNoExt.getText()));
            c.setNumeroInt(Integer.parseInt(txtNoInt.getText()));
            c.setCorreo(txtCorreoElectronico.getText());
            c.setTelefono(Integer.parseInt(txtTelefono.getText()));

            boolean exito = modoEdicion ?
                    model.modificarCliente(c) :
                    model.agregarCliente(c);

            if (exito) {
                new Alert(Alert.AlertType.INFORMATION,
                        modoEdicion ? "Cliente actualizado correctamente" :
                                "Cliente agregado correctamente"
                ).showAndWait();

                // 👉 IMPORTANTE: Ejecutar callback ANTES de cerrar
                if (onSaved != null) {
                    onSaved.run();
                }

                // 👉 Cerrar después de un breve retardo (igual que proveedores)
                javafx.application.Platform.runLater(() -> {
                    Stage stage = (Stage) btnGuardar.getScene().getWindow();
                    stage.close();
                });

            } else {
                new Alert(Alert.AlertType.ERROR,
                        "No se pudo guardar el cliente."
                ).showAndWait();
            }

        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR,
                    "Ocurrió un error inesperado al guardar el cliente."
            ).showAndWait();
        }
    }

    private boolean validarNumericos() {
        return validarCampoNumerico(txtCP, "Código Postal") &&
                validarCampoNumerico(txtNoExt, "Número Exterior") &&
                validarCampoNumerico(txtNoInt, "Número Interior") &&
                validarCampoNumerico(txtTelefono, "Teléfono");
    }

    private boolean validarCampoNumerico(TextField campo, String nombreCampo) {
        if (!campo.getText().matches("\\d+")) {
            new Alert(Alert.AlertType.ERROR,
                    "El campo '" + nombreCampo + "' debe contener solo números."
            ).showAndWait();
            campo.requestFocus();
            campo.selectAll();
            return false;
        }
        return true;
    }

    // Añade este método a la clase controllerNuevoCliente (después de setOnSaved)
    public String getNombreCliente() {
        return txtNombre.getText().trim();
    }

    private void manejarCambioCp(String nuevoCp) {
        if (nuevoCp == null) {
            limpiarDireccion();
            return;
        }

        String cp = nuevoCp.trim();
        if (!cp.matches("\\d{5}")) {
            ultimoCpConsultado = "";
            limpiarDireccion();
            return;
        }

        if (cp.equals(ultimoCpConsultado)) {
            return;
        }

        ultimoCpConsultado = cp;
        cargarDireccionPorCp(cp);
    }

    private void cargarDireccionPorCp(String cp) {
        CodigoPostalService.DireccionCp respaldo = obtenerDireccionActual();
        String coloniaSeleccionada = obtenerColoniaSeleccionada();
        boolean bloqueoAnterior = !txtPais.isEditable();

        CompletableFuture
                .supplyAsync(() -> {
                    try {
                        return codigoPostalService.obtenerDireccion(cp);
                    } catch (Exception e) {
                        return null;
                    }
                })
                .thenAccept(resultado -> Platform.runLater(() -> {
                    if (!cp.equals(txtCP.getText().trim())) {
                        return;
                    }

                    if (resultado == null) {
                        restaurarDireccion(respaldo, coloniaSeleccionada, bloqueoAnterior);
                        return;
                    }

                    aplicarDireccion(resultado);
                }));
    }

    private void aplicarDireccion(CodigoPostalService.DireccionCp direccion) {
        txtPais.setText(direccion.pais());
        txtEstado.setText(direccion.estado());
        txtLocalidad.setText(direccion.localidad());
        txtCiudad.setText(direccion.ciudad());

        List<String> colonias = direccion.colonias();
        cbColonia.getItems().setAll(colonias);

        if (coloniaInicial != null && !coloniaInicial.isBlank() && colonias.contains(coloniaInicial)) {
            cbColonia.setValue(coloniaInicial);
        } else if (!colonias.isEmpty()) {
            cbColonia.getSelectionModel().selectFirst();
        }
        coloniaInicial = null;

        bloquearCamposDireccion(true);
    }

    private void limpiarDireccion() {
        txtPais.clear();
        txtEstado.clear();
        txtLocalidad.clear();
        txtCiudad.clear();

        cbColonia.getItems().clear();
        cbColonia.setValue(null);

        bloquearCamposDireccion(false);
    }

    private CodigoPostalService.DireccionCp obtenerDireccionActual() {
        return new CodigoPostalService.DireccionCp(
                txtPais.getText(),
                txtEstado.getText(),
                txtLocalidad.getText(),
                txtCiudad.getText(),
                List.copyOf(cbColonia.getItems())
        );
    }

    private void restaurarDireccion(CodigoPostalService.DireccionCp respaldo, String coloniaSeleccionada, boolean bloqueoAnterior) {
        if (respaldo == null) {
            return;
        }

        txtPais.setText(respaldo.pais());
        txtEstado.setText(respaldo.estado());
        txtLocalidad.setText(respaldo.localidad());
        txtCiudad.setText(respaldo.ciudad());

        cbColonia.getItems().setAll(respaldo.colonias());
        if (coloniaSeleccionada != null && !coloniaSeleccionada.isBlank()) {
            cbColonia.setValue(coloniaSeleccionada);
        }

        bloquearCamposDireccion(bloqueoAnterior);
    }

    private void bloquearCamposDireccion(boolean bloquear) {
        txtPais.setEditable(!bloquear);
        txtEstado.setEditable(!bloquear);
        txtLocalidad.setEditable(!bloquear);
        txtCiudad.setEditable(!bloquear);
    }

    private String obtenerColoniaSeleccionada() {
        if (cbColonia.getValue() != null) {
            return cbColonia.getValue();
        }
        return cbColonia.getEditor().getText().trim();
    }
}
