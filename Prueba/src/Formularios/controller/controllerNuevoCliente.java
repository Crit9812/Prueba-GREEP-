package Formularios.controller;

import Compartido.helper.AutoCompleteComboBoxListener;
import Compartido.helper.CodigoPostalService;
import Formularios.model.modelNuevoCliente;
import Consultas.clientes.model.cliente;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.util.Optional;

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

        configurarComboColonia();
        configurarAutocompletadoCodigoPostal();
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
        txtCP.setText(String.valueOf(c.getCp()));
        txtPais.setText(c.getPais());
        txtEstado.setText(c.getEstado());
        txtLocalidad.setText(c.getLocalidad());
        txtCiudad.setText(c.getCiudad());
        cargarColonia(c.getColonia());
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

    private void configurarComboColonia() {
        cbColonia.setEditable(true);
        cbColonia.setItems(FXCollections.observableArrayList());
        new AutoCompleteComboBoxListener<>(cbColonia);
    }

    private void configurarAutocompletadoCodigoPostal() {
        txtCP.textProperty().addListener((obs, anterior, actual) -> {
            String cp = actual == null ? "" : actual.trim();
            if (!cp.matches("\\d{5}")) {
                if (!ultimoCpConsultado.isBlank()) {
                    limpiarCamposDireccion();
                }
                return;
            }
            if (cp.equals(ultimoCpConsultado)) {
                return;
            }
            ultimoCpConsultado = cp;
            buscarInformacionCodigoPostal(cp);
        });
    }

    private void buscarInformacionCodigoPostal(String cp) {
        codigoPostalService.buscarCodigoPostal(cp)
                .thenAccept(info -> Platform.runLater(() -> aplicarInformacionCodigoPostal(cp, info)));
    }

    private void aplicarInformacionCodigoPostal(String cp, Optional<CodigoPostalService.CodigoPostalInfo> info) {
        if (!cp.equals(txtCP.getText().trim())) {
            return;
        }
        if (info.isEmpty()) {
            return;
        }
        CodigoPostalService.CodigoPostalInfo data = info.get();
        txtPais.setText(data.getPais());
        txtEstado.setText(data.getEstado());
        txtLocalidad.setText(data.getLocalidad());
        txtCiudad.setText(data.getCiudad());
        actualizarColonias(data.getColonias());
    }

    private void actualizarColonias(java.util.List<String> colonias) {
        ObservableList<String> items = FXCollections.observableArrayList(colonias);
        cbColonia.setItems(items);
        String seleccionActual = obtenerColoniaSeleccionada();
        if (seleccionActual != null && items.contains(seleccionActual)) {
            cbColonia.getSelectionModel().select(seleccionActual);
            cbColonia.getEditor().setText(seleccionActual);
        } else if (!items.isEmpty()) {
            cbColonia.getSelectionModel().selectFirst();
            cbColonia.getEditor().setText(cbColonia.getSelectionModel().getSelectedItem());
        }
    }

    private void cargarColonia(String colonia) {
        if (colonia == null || colonia.isBlank()) {
            cbColonia.getSelectionModel().clearSelection();
            cbColonia.getEditor().clear();
            return;
        }
        cbColonia.setItems(FXCollections.observableArrayList(colonia));
        cbColonia.getSelectionModel().select(colonia);
        cbColonia.getEditor().setText(colonia);
    }

    private String obtenerColoniaSeleccionada() {
        String seleccion = cbColonia.getSelectionModel().getSelectedItem();
        if (seleccion != null && !seleccion.isBlank()) {
            return seleccion;
        }
        return cbColonia.getEditor().getText().trim();
    }

    private void limpiarCamposDireccion() {
        ultimoCpConsultado = "";
        txtPais.clear();
        txtEstado.clear();
        txtLocalidad.clear();
        txtCiudad.clear();
        cbColonia.getSelectionModel().clearSelection();
        cbColonia.getItems().clear();
        cbColonia.getEditor().clear();
    }
}
