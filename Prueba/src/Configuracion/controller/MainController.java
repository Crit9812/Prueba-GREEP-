package Configuracion.controller;

import Compartido.model.IvaConfigService;
import VentanaPrincipal.controller.ControladorVista;
import conexion.Conexion;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Locale;

public class MainController implements ControladorVista {

    @FXML private StackPane root;
    @FXML private VBox contenedor;
    @FXML private TextField txtIva;
    @FXML private Label lblIvaActual;

    @FXML private Label lblSucursalNombre;
    @FXML private Label lblSucursalDomicilio;
    @FXML private Label lblSucursalCp;
    @FXML private Label lblSucursalColonia;
    @FXML private Label lblSucursalNumeroExt;
    @FXML private Label lblSucursalNumeroInt;
    @FXML private Label lblSucursalCiudad;
    @FXML private Label lblSucursalEstado;
    @FXML private Label lblSucursalLocalidad;
    @FXML private Label lblSucursalPais;
    @FXML private Label lblSucursalCorreo;
    @FXML private Label lblSucursalTelefono;

    private StackPane contentArea;
    private VentanaPrincipal.controller.MainController controladorPrincipal;

    @FXML
    public void initialize() {
        if (contenedor != null && root != null) {
            contenedor.prefHeightProperty().bind(root.heightProperty());
            contenedor.prefWidthProperty().bind(root.widthProperty());
        }
        SplitPane.setResizableWithParent(contenedor, true);

        configurarValidaciones();
        cargarDatosSucursalActual();
        recargarIva();
    }

    @FXML
    private void guardarIva() {
        BigDecimal nuevoIva;
        try {
            nuevoIva = new BigDecimal(txtIva.getText().trim());
        } catch (Exception e) {
            mostrarAlerta("Advertencia", "El IVA debe ser numérico.");
            return;
        }

        if (nuevoIva.compareTo(BigDecimal.ZERO) < 0) {
            mostrarAlerta("Advertencia", "El IVA no puede ser negativo.");
            return;
        }

        boolean actualizado = IvaConfigService.actualizarEnBaseDatos(nuevoIva);
        if (!actualizado) {
            mostrarAlerta("Error", "No se pudo actualizar el IVA en base de datos.");
            return;
        }

        mostrarAlerta("Éxito", "IVA actualizado correctamente.");
        refrescarVistaIva();
    }

    @FXML
    private void recargarIva() {
        IvaConfigService.recargarDesdeBaseDatos();
        refrescarVistaIva();
    }

    private void refrescarVistaIva() {
        BigDecimal iva = IvaConfigService.getIvaPorcentaje();
        if (txtIva != null) {
            txtIva.setText(iva.stripTrailingZeros().toPlainString());
        }
        if (lblIvaActual != null) {
            lblIvaActual.setText("IVA actual: " + iva.stripTrailingZeros().toPlainString() + "%");
        }
    }

    private void cargarDatosSucursalActual() {
        String nombreSucursal = obtenerNombreSucursalDesdeUrl();
        if (nombreSucursal == null || nombreSucursal.isBlank()) {
            mostrarDatosSucursalVacios("No se pudo determinar la sucursal desde la URL.");
            return;
        }

        String sql = "SELECT nombre, domicilio, cp, colonia, numeroExt, numeroInt, ciudad, estado, localidad, pais, correo, telefono " +
                "FROM sucursales WHERE LOWER(nombre) = LOWER(?) LIMIT 1";

        try (Connection conn = new Conexion().conectar();
             PreparedStatement st = conn.prepareStatement(sql)) {
            st.setString(1, nombreSucursal);
            try (ResultSet rs = st.executeQuery()) {
                if (rs.next()) {
                    setLabel(lblSucursalNombre, rs.getString("nombre"));
                    setLabel(lblSucursalDomicilio, rs.getString("domicilio"));
                    setLabel(lblSucursalCp, rs.getString("cp"));
                    setLabel(lblSucursalColonia, rs.getString("colonia"));
                    setLabel(lblSucursalNumeroExt, rs.getString("numeroExt"));
                    setLabel(lblSucursalNumeroInt, rs.getString("numeroInt"));
                    setLabel(lblSucursalCiudad, rs.getString("ciudad"));
                    setLabel(lblSucursalEstado, rs.getString("estado"));
                    setLabel(lblSucursalLocalidad, rs.getString("localidad"));
                    setLabel(lblSucursalPais, rs.getString("pais"));
                    setLabel(lblSucursalCorreo, rs.getString("correo"));
                    setLabel(lblSucursalTelefono, rs.getString("telefono"));
                } else {
                    mostrarDatosSucursalVacios("No se encontró la sucursal '" + nombreSucursal + "'.");
                }
            }
        } catch (Exception e) {
            mostrarDatosSucursalVacios("Error al cargar datos de sucursal.");
        }
    }

    private String obtenerNombreSucursalDesdeUrl() {
        String url = Conexion.getUrlPrincipal();
        if (url == null || url.isBlank()) {
            return "";
        }

        int slash = url.lastIndexOf('/');
        if (slash < 0 || slash + 1 >= url.length()) {
            return "";
        }

        String segmento = url.substring(slash + 1);
        int q = segmento.indexOf('?');
        String nombre = (q >= 0) ? segmento.substring(0, q) : segmento;

        return nombre.replace("distribu_", "").trim().toLowerCase(Locale.ROOT);
    }

    private void mostrarDatosSucursalVacios(String nombreFallback) {
        setLabel(lblSucursalNombre, nombreFallback);
        setLabel(lblSucursalDomicilio, "-");
        setLabel(lblSucursalCp, "-");
        setLabel(lblSucursalColonia, "-");
        setLabel(lblSucursalNumeroExt, "-");
        setLabel(lblSucursalNumeroInt, "-");
        setLabel(lblSucursalCiudad, "-");
        setLabel(lblSucursalEstado, "-");
        setLabel(lblSucursalLocalidad, "-");
        setLabel(lblSucursalPais, "-");
        setLabel(lblSucursalCorreo, "-");
        setLabel(lblSucursalTelefono, "-");
    }

    private void setLabel(Label label, String valor) {
        if (label != null) {
            label.setText(valor == null || valor.isBlank() ? "-" : valor);
        }
    }

    private void configurarValidaciones() {
        if (txtIva == null) {
            return;
        }
        txtIva.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) {
                return;
            }
            String limpio = newVal.replace(',', '.');
            if (!limpio.matches("^\\d{0,3}(\\.\\d{0,2})?$")) {
                txtIva.setText(oldVal);
            } else if (!limpio.equals(newVal)) {
                txtIva.setText(limpio);
            }
        });
    }

    private void mostrarAlerta(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    @Override
    public void setContentArea(StackPane contentArea) {
        this.contentArea = contentArea;
    }

    @Override
    public void setControladorPrincipal(VentanaPrincipal.controller.MainController controladorPrincipal) {
        this.controladorPrincipal = controladorPrincipal;
    }
}
