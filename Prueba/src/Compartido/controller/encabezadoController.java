package Compartido.controller;

import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.paint.Color;
import javafx.scene.layout.CornerRadii;
import javafx.geometry.Insets;
import Compartido.helper.BusquedaProductoHelper;
import Compartido.helper.RefrescoHelper;
import Compartido.model.NotificacionService;
import VentanaPrincipal.controller.EnumVistas;
import conexion.Conexion;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.animation.PauseTransition;
import javafx.util.Duration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class encabezadoController {

    @FXML private TextField searchBar;
    @FXML private BorderPane panel;
    @FXML private ImageView iconoNavbar;
    @FXML private ImageView iconoNavbar2;
    @FXML private ImageView iconoNavbar3;
    @FXML private Label labelUsuario;
    @FXML private Label labelTitulo;

    private List<CustomMenuItem> itemsMenu = new ArrayList<>();
    private int selectedIndex = -1;
    private final NotificacionService notificacionService = new NotificacionService();
    private final ContextMenu menuSugerencias = new ContextMenu();
    private final PauseTransition debounceBusqueda = new PauseTransition(Duration.millis(180));
    private final ExecutorService buscadorExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread hilo = new Thread(r, "busqueda-encabezado");
        hilo.setDaemon(true);
        return hilo;
    });
    private final AtomicInteger versionBusqueda = new AtomicInteger(0);
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
        searchBar.setOnAction(e -> buscarConEnter());
        debounceBusqueda.setOnFinished(e -> ejecutarBusquedaAsincrona());

        menuSugerencias.setOnHidden(e -> {
            menuSugerencias.getItems().clear();
            itemsMenu.clear();
            selectedIndex = -1;
        });

        searchBar.textProperty().addListener((obs, oldVal, newVal) -> {
            String termino = newVal == null ? "" : newVal.trim();
            if (termino.isEmpty()) {
                versionBusqueda.incrementAndGet();
                debounceBusqueda.stop();
                menuSugerencias.hide();
                return;
            }

            debounceBusqueda.playFromStart();
        });

        searchBar.focusedProperty().addListener((obs, oldVal, focused) -> {
            if (!focused) {
                menuSugerencias.hide();
            }
        });

        // Manejador de teclas para navegación con flechas y Enter
        searchBar.addEventHandler(javafx.scene.input.KeyEvent.KEY_PRESSED, event -> {
            if (menuSugerencias.isShowing() && !itemsMenu.isEmpty()) {
                if (event.getCode() == javafx.scene.input.KeyCode.DOWN) {
                    seleccionarSiguiente();
                    event.consume();
                } else if (event.getCode() == javafx.scene.input.KeyCode.UP) {
                    seleccionarAnterior();
                    event.consume();
                } else if (event.getCode() == javafx.scene.input.KeyCode.ENTER) {
                    ejecutarItemSeleccionado();
                    event.consume();
                }
            }
        });
    }

    private void ejecutarBusquedaAsincrona() {
        String termino = searchBar.getText() == null ? "" : searchBar.getText().trim();
        if (termino.isEmpty()) {
            menuSugerencias.hide();
            return;
        }

        int versionActual = versionBusqueda.incrementAndGet();

        buscadorExecutor.submit(() -> {
            List<SugerenciaProducto> sugerencias = buscarProductos(termino);

            Platform.runLater(() -> {
                String textoVisible = searchBar.getText() == null ? "" : searchBar.getText().trim();
                if (versionActual != versionBusqueda.get() || !termino.equals(textoVisible)) {
                    return;
                }

                mostrarSugerencias(sugerencias);
            });
        });
    }

    private void mostrarSugerencias(List<SugerenciaProducto> sugerencias) {
        if (sugerencias.isEmpty()) {
            menuSugerencias.hide();
            return;
        }

        itemsMenu.clear();
        List<CustomMenuItem> items = new ArrayList<>();
        for (SugerenciaProducto sugerencia : sugerencias) {
            Label etiqueta = new Label(sugerencia.textoSugerencia());
            // CAMBIO: Eliminado setBackground(null) - no necesario con CSS
            etiqueta.setWrapText(true);
            etiqueta.setMaxWidth(400);
            etiqueta.getStyleClass().add("sugerencia-item");

            int index = items.size();

            // CAMBIO: Simplificado el manejo de selección por mouse
            etiqueta.setOnMouseEntered(e -> {
                if (selectedIndex != index) {
                    selectedIndex = index;
                    // CAMBIO: En lugar de actualizar estilo manual, solo actualizamos índice
                    // El CSS se encarga del hover
                }
            });

            CustomMenuItem item = new CustomMenuItem(etiqueta, true);
            item.setOnAction(event -> seleccionarProducto(sugerencia));
            items.add(item);
            itemsMenu.add(item);
        }

        menuSugerencias.getItems().setAll(items);

        // CAMBIO: Eliminada la llamada a actualizarEstiloSeleccion()
        // El CSS manejará el foco automáticamente

        if (!itemsMenu.isEmpty()) {
            selectedIndex = 0;
            // CAMBIO: Enfocamos el primer item para que CSS muestre selección
            Platform.runLater(() -> {
                if (!itemsMenu.isEmpty() && itemsMenu.get(0).getContent() instanceof Label) {
                    ((Label) itemsMenu.get(0).getContent()).requestFocus();
                }
            });
        }

        if (!menuSugerencias.isShowing()) {
            menuSugerencias.show(searchBar, javafx.geometry.Side.BOTTOM, 0, 0);
        }
    }

    private void buscarConEnter() {
        String termino = searchBar.getText() == null ? "" : searchBar.getText().trim();

        if (termino.isEmpty()) {
            menuSugerencias.hide();
            return;
        }

        List<SugerenciaProducto> sugerencias = buscarProductos(termino);

        if (sugerencias.isEmpty()) {
            menuSugerencias.hide();
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Producto no encontrado", ButtonType.OK);
            alert.setHeaderText(null);
            alert.showAndWait();
            return;
        }

        seleccionarProducto(sugerencias.get(0));
    }

    private List<SugerenciaProducto> buscarProductos(String termino) {
        List<SugerenciaProducto> resultados = new ArrayList<>();
        String sql = "SELECT p.id, p.nombre, " +
                "COALESCE(m.nombre, 'Sin marca') AS marca, " +
                "COALESCE(( " +
                "   SELECT GROUP_CONCAT(DISTINCT prov.Nombre ORDER BY prov.Nombre SEPARATOR ', ') " +
                "   FROM detalle_Entrada deProv " +
                "   JOIN entradas eProv ON eProv.idEntrada = deProv.claveEntrada " +
                "   JOIN proveedores prov ON prov.id = eProv.idRemitente " +
                "   WHERE deProv.claveProducto = p.id " +
                "), 'Sin proveedor') AS proveedor, " +
                "COALESCE(p.material, 'Sin material') AS material, " +
                "COALESCE(p.unidadMedida, 'Sin unidad') AS unidad, " +
                "presentacionData.presentacion AS presentacion, " +
                "presentacionData.factor AS factor, " +
                "presentacionData.cantidad AS existencia " +
                "FROM productos p " +
                "LEFT JOIN marcas m ON m.id = p.marca " +
                "JOIN ( " +
                "   SELECT base.idProducto, base.presentacion, base.factor, SUM(base.cantidad) AS cantidad " +
                "   FROM ( " +
                "       SELECT deDisp.claveProducto AS idProducto, " +
                "              COALESCE(aDisp.presentacion, 'Sin presentación') AS presentacion, " +
                "              COALESCE(aDisp.factor, 0) AS factor, " +
                "              COUNT(DISTINCT aDisp.idArticulo) AS cantidad " +
                "       FROM detalle_Entrada deDisp " +
                "       JOIN articulo aDisp ON aDisp.idDetalleEntrada = deDisp.idDetalleEntrada " +
                "       WHERE LOWER(aDisp.Estado) = 'disponible' " +
                "       GROUP BY deDisp.claveProducto, COALESCE(aDisp.presentacion, 'Sin presentación'), COALESCE(aDisp.factor, 0) " +
                "       UNION ALL " +
                "       SELECT deSeg.claveProducto AS idProducto, " +
                "              'pz' AS presentacion, " +
                "              1 AS factor, " +
                "              COUNT(DISTINCT da.idDetalle) AS cantidad " +
                "       FROM detalle_Entrada deSeg " +
                "       JOIN articulo aSeg ON aSeg.idDetalleEntrada = deSeg.idDetalleEntrada " +
                "       JOIN detalleArticulo da ON da.idArticulo = aSeg.idArticulo " +
                "       WHERE LOWER(aSeg.Estado) = 'segmentado' " +
                "         AND LOWER(da.estado) = 'disponible' " +
                "         AND (da.idDetalleSalida IS NULL OR da.idDetalleSalida = 0) " +
                "       GROUP BY deSeg.claveProducto " +
                "   ) base " +
                "   GROUP BY base.idProducto, base.presentacion, base.factor " +
                ") presentacionData ON presentacionData.idProducto = p.id " +
                "WHERE p.estado = 'activo' AND (p.id LIKE ? OR p.nombre LIKE ?) " +
                "ORDER BY CASE WHEN p.id = ? THEN 0 WHEN p.nombre = ? THEN 1 ELSE 2 END, p.nombre ASC, " +
                "presentacionData.presentacion ASC, presentacionData.factor ASC " +
                "LIMIT 24";

        try (Connection conn = new Conexion().conectar();
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
                            rs.getString("presentacion"),
                            rs.getInt("factor"),
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

        // Se conserva lo que ya hace: guardar la solicitud para Inventario
        BusquedaProductoHelper.guardarSolicitud(idProducto, nombreProducto, nombreProducto);

        // NUEVO: intentar resolver el controlador principal si no está seteado
        resolverControladorPrincipalSiHaceFalta();

        Platform.runLater(() -> controladorPrincipal.cargarVista(EnumVistas.INVENTARIO));
    }

    private void resolverControladorPrincipalSiHaceFalta() {
        if (controladorPrincipal != null) return;

        try {
            if (panel != null && panel.getScene() != null) {
                Object udScene = panel.getScene().getUserData();
                if (udScene instanceof VentanaPrincipal.controller.MainController) {
                    controladorPrincipal = (VentanaPrincipal.controller.MainController) udScene;
                    return;
                }

                if (panel.getScene().getWindow() != null) {
                    Object udWindow = panel.getScene().getWindow().getUserData();
                    if (udWindow instanceof VentanaPrincipal.controller.MainController) {
                        controladorPrincipal = (VentanaPrincipal.controller.MainController) udWindow;
                    }
                }
            }
        } catch (Exception ignored) {
            // intencional: no hacemos cambios extra ni rompemos flujo
        }
    }

    private static class SugerenciaProducto {
        private final String id;
        private final String nombre;
        private final String marca;
        private final String proveedor;
        private final String material;
        private final String unidad;
        private final String presentacion;
        private final int factor;
        private final int existencia;

        private SugerenciaProducto(String id, String nombre, String marca, String proveedor, String material,
                                   String unidad, String presentacion, int factor, int existencia) {
            this.id = id;
            this.nombre = nombre;
            this.marca = marca;
            this.proveedor = proveedor;
            this.material = material;
            this.unidad = unidad;
            this.presentacion = presentacion;
            this.factor = factor;
            this.existencia = existencia;
        }

        private String textoSugerencia() {
            return id + " - " + nombre + "\n" +
                    "Marca: " + marca + " | Proveedor: " + proveedor + "\n" +
                    "Material: " + material + " | Unidad: " + unidad +
                    " | Presentación: " + presentacion + " | Factor: " + factor +
                    " | Existencia: " + existencia;
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

    private void seleccionarSiguiente() {
        if (itemsMenu.isEmpty()) return;
        selectedIndex = (selectedIndex + 1) % itemsMenu.size();
        enfocarItemSeleccionado();
    }

    private void seleccionarAnterior() {
        if (itemsMenu.isEmpty()) return;
        selectedIndex = (selectedIndex - 1 + itemsMenu.size()) % itemsMenu.size();
        enfocarItemSeleccionado();
    }

    private void enfocarItemSeleccionado() {
        if (selectedIndex >= 0 && selectedIndex < itemsMenu.size()) {
            Node contenido = itemsMenu.get(selectedIndex).getContent();
            if (contenido != null) {
                contenido.requestFocus();
            }
        }
    }

    private void ejecutarItemSeleccionado() {
        if (selectedIndex >= 0 && selectedIndex < itemsMenu.size()) {
            itemsMenu.get(selectedIndex).fire();
        }
    }

}