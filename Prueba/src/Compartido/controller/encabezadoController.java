package Compartido.controller;

import Compartido.helper.BusquedaProductoHelper;
import Compartido.helper.RefrescoHelper;
import Compartido.model.NotificacionService;
import VentanaPrincipal.controller.EnumVistas;
import conexion.Conexion;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class encabezadoController {

    @FXML private TextField searchBar;
    @FXML private BorderPane panel;
    @FXML private ImageView iconoNavbar;
    @FXML private ImageView iconoNavbar2;
    @FXML private ImageView iconoNavbar3;
    @FXML private Label labelUsuario;
    @FXML private Label labelTitulo;

    private final NotificacionService notificacionService = new NotificacionService();
    private final ContextMenu menuSugerencias = new ContextMenu();
    private VentanaPrincipal.controller.MainController controladorPrincipal;

    @FXML
    public void initialize(){
        searchBar.prefWidthProperty().bind(panel.widthProperty().multiply(0.23));
        searchBar.prefHeightProperty().bind(panel.heightProperty().multiply(0.49));

        labelUsuario.prefWidthProperty().bind(panel.widthProperty().multiply(0.08));
        labelUsuario.prefHeightProperty().bind(panel.heightProperty().multiply(0.8));

        iconoNavbar.fitHeightProperty().bind(panel.heightProperty().multiply(0.48));
        iconoNavbar.fitWidthProperty().bind(panel.widthProperty().multiply(0.028));

        iconoNavbar2.fitHeightProperty().bind(panel.heightProperty().multiply(0.48));
        iconoNavbar2.fitWidthProperty().bind(panel.widthProperty().multiply(0.028));

        iconoNavbar3.fitHeightProperty().bind(panel.heightProperty().multiply(0.43));
        iconoNavbar3.fitWidthProperty().bind(panel.widthProperty().multiply(0.028));

        labelTitulo.prefWidthProperty().bind(panel.widthProperty().multiply(0.15));
        labelTitulo.prefHeightProperty().bind(panel.heightProperty().multiply(0.5));

        searchBar.setFocusTraversable(false);
        configurarBusquedaProductos();

        actualizarIconoNotificacionesEnParalelo();

        labelUsuario.setText(Compartido.sesion.SesionUsuario.getNombreUsuario());
    }

    public void setTitulo(String titulo, String colorHex) {
        labelTitulo.setText(titulo);
        labelTitulo.setStyle("-fx-background-color: " + colorHex + ";");
    }

    public void setControladorPrincipal(VentanaPrincipal.controller.MainController controladorPrincipal) {
        this.controladorPrincipal = controladorPrincipal;
    }

    private void configurarBusquedaProductos() {
        menuSugerencias.setAutoHide(true);

        searchBar.textProperty().addListener((obs, oldVal, newVal) -> {
            String termino = newVal == null ? "" : newVal.trim();
            if (termino.isEmpty()) {
                menuSugerencias.hide();
                return;
            }

            List<SugerenciaProducto> sugerencias = buscarProductos(termino);
            if (sugerencias.isEmpty()) {
                menuSugerencias.hide();
                return;
            }

            List<CustomMenuItem> items = new ArrayList<>();
            for (SugerenciaProducto sugerencia : sugerencias) {
                Label etiqueta = new Label(sugerencia.textoSugerencia());
                etiqueta.setWrapText(true);

                CustomMenuItem item = new CustomMenuItem(etiqueta, true);
                item.setOnAction(event -> seleccionarProducto(sugerencia));
                items.add(item);
            }

            menuSugerencias.getItems().setAll(items);
            if (!menuSugerencias.isShowing()) {
                menuSugerencias.show(searchBar, javafx.geometry.Side.BOTTOM, 0, 0);
            }
        });

        searchBar.focusedProperty().addListener((obs, oldVal, focused) -> {
            if (!focused) {
                menuSugerencias.hide();
            }
        });
    }

    private List<SugerenciaProducto> buscarProductos(String termino) {
        List<SugerenciaProducto> resultados = new ArrayList<>();
        String sql = "SELECT p.id, p.nombre, " +
                "COALESCE(m.nombre, 'Sin marca') AS marca, " +
                "COALESCE(GROUP_CONCAT(DISTINCT prov.Nombre ORDER BY prov.Nombre SEPARATOR ', '), 'Sin proveedor') AS proveedor, " +
                "COALESCE(p.material, 'Sin material') AS material, " +
                "COALESCE(p.unidadMedida, 'Sin unidad') AS unidad, " +
                "SUM(CASE WHEN a.Estado = 'disponible' THEN 1 ELSE 0 END) AS existencia " +
                "FROM productos p " +
                "LEFT JOIN marcas m ON m.id = p.marca " +
                "LEFT JOIN detalle_Entrada de ON de.claveProducto = p.id " +
                "LEFT JOIN entradas e ON e.idEntrada = de.claveEntrada " +
                "LEFT JOIN proveedores prov ON prov.id = e.idRemitente " +
                "LEFT JOIN articulo a ON a.idDetalleEntrada = de.idDetalleEntrada " +
                "WHERE p.estado = 'activo' AND (p.id LIKE ? OR p.nombre LIKE ?) " +
                "GROUP BY p.id, p.nombre, m.nombre, p.material, p.unidadMedida " +
                "ORDER BY CASE WHEN p.id = ? THEN 0 WHEN p.nombre = ? THEN 1 ELSE 2 END, p.nombre ASC " +
                "LIMIT 8";

        try (Connection conn = Conexion.conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            String like = "%" + termino + "%";
            ps.setString(1, like);
            ps.setString(2, like);
            ps.setString(3, termino);
            ps.setString(4, termino);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    resultados.add(new SugerenciaProducto(
                            rs.getString("id"),
                            rs.getString("nombre"),
                            rs.getString("marca"),
                            rs.getString("proveedor"),
                            rs.getString("material"),
                            rs.getString("unidad"),
                            rs.getInt("existencia")
                    ));
                }
            }
        } catch (Exception e) {
            menuSugerencias.hide();
        }

        return resultados;
    }

    private void seleccionarProducto(SugerenciaProducto sugerencia) {
        String nombreProducto = sugerencia.nombre == null ? "" : sugerencia.nombre.trim();
        String idProducto = sugerencia.id == null ? "" : sugerencia.id.trim();

        searchBar.setText(nombreProducto);
        menuSugerencias.hide();

        BusquedaProductoHelper.guardarSolicitud(idProducto, nombreProducto, nombreProducto);

        if (controladorPrincipal != null) {
            controladorPrincipal.cargarVista(EnumVistas.INVENTARIO);
        }
    }

    private static class SugerenciaProducto {
        private final String id;
        private final String nombre;
        private final String marca;
        private final String proveedor;
        private final String material;
        private final String unidad;
        private final int existencia;

        private SugerenciaProducto(String id, String nombre, String marca, String proveedor, String material, String unidad, int existencia) {
            this.id = id;
            this.nombre = nombre;
            this.marca = marca;
            this.proveedor = proveedor;
            this.material = material;
            this.unidad = unidad;
            this.existencia = existencia;
        }

        private String textoSugerencia() {
            return id + " - " + nombre + "\n" +
                    "Marca: " + marca + " | Proveedor: " + proveedor + "\n" +
                    "Material: " + material + " | Unidad: " + unidad + " | Existencia: " + existencia;
        }
    }

    @FXML
    private void salir() {
        login.controller.MainController controlador = new login.controller.MainController();
        controllerInterfaz.ControllerInterfaz.cambiarVista(
                "/login/view/main_view.fxml",
                "/login/style/estilos.css",
                controlador
        );
    }

    @FXML
    private void actualizar() {
        RefrescoHelper.refrescar();
        actualizarIconoNotificacionesEnParalelo();
    }

    @FXML
    private void abrirNotificaciones() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Compartido/view/notificaciones.fxml"));
            Scene scene = new Scene(loader.load());

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Notificaciones");
            stage.setScene(scene);
            stage.showAndWait();
            actualizarIconoNotificacionesEnParalelo();
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "No se pudo abrir la ventana de notificaciones: " + e.getMessage());
            alert.showAndWait();
        }
    }

    private void actualizarIconoNotificacionesEnParalelo() {
        Thread hiloRevisionNotificaciones = new Thread(() -> {
            boolean hayNotificacionesActivas = notificacionService.hayNotificacionesActivas();
            String icono = hayNotificacionesActivas ? "/img/n.png" : "/img/sobreC.png";

            Platform.runLater(() -> iconoNavbar3.setImage(new Image(getClass().getResourceAsStream(icono))));
        }, "hilo-revision-notificaciones-encabezado");

        hiloRevisionNotificaciones.setDaemon(true);
        hiloRevisionNotificaciones.start();
    }
}
