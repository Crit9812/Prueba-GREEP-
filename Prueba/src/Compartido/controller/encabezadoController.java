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
import javafx.animation.PauseTransition;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

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
    private final PauseTransition debounceBusqueda = new PauseTransition(Duration.millis(90));
    private final ExecutorService busquedaExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread hilo = new Thread(r, "encabezado-busqueda");
        hilo.setDaemon(true);
        return hilo;
    });
    private final AtomicInteger versionBusqueda = new AtomicInteger();
    private volatile Future<?> tareaBusquedaActual;
    private final Map<String, List<SugerenciaProducto>> cacheBusqueda = Collections.synchronizedMap(
            new LinkedHashMap<>(32, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, List<SugerenciaProducto>> eldest) {
                    return size() > 50;
                }
            }
    );
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
        searchBar.textProperty().addListener((obs, oldVal, newVal) -> {
            String termino = newVal == null ? "" : newVal.trim();
            if (termino.isEmpty()) {
                versionBusqueda.incrementAndGet();
                debounceBusqueda.stop();
                if (tareaBusquedaActual != null) {
                    tareaBusquedaActual.cancel(true);
                }
                menuSugerencias.hide();
                return;
            }

            int versionActual = versionBusqueda.incrementAndGet();
            debounceBusqueda.setOnFinished(event -> buscarProductosEnTiempoReal(termino, versionActual));
            debounceBusqueda.playFromStart();
        });

        searchBar.focusedProperty().addListener((obs, oldVal, focused) -> {
            if (!focused) {
                menuSugerencias.hide();
            }
        });
    }

    private void buscarProductosEnTiempoReal(String termino, int versionEsperada) {
        String terminoNormalizado = normalizarTermino(termino);

        List<SugerenciaProducto> sugerenciasCache = cacheBusqueda.get(terminoNormalizado);
        if (sugerenciasCache != null) {
            mostrarSugerencias(terminoNormalizado, sugerenciasCache, versionEsperada);
            return;
        }

        List<SugerenciaProducto> sugerenciasPrefijo = buscarEnCachePorPrefijo(terminoNormalizado);
        if (!sugerenciasPrefijo.isEmpty()) {
            mostrarSugerencias(terminoNormalizado, sugerenciasPrefijo, versionEsperada);
        }

        if (tareaBusquedaActual != null) {
            tareaBusquedaActual.cancel(true);
        }

        tareaBusquedaActual = busquedaExecutor.submit(() -> {
            List<SugerenciaProducto> resultados = buscarProductos(termino);
            cacheBusqueda.put(terminoNormalizado, resultados);
            Platform.runLater(() -> mostrarSugerencias(terminoNormalizado, resultados, versionEsperada));
        });
    }

    private void mostrarSugerencias(String termino, List<SugerenciaProducto> sugerencias, int versionEsperada) {
        String terminoActual = normalizarTermino(searchBar.getText());
        if (versionEsperada != versionBusqueda.get() || !termino.equals(terminoActual)) {
            return;
        }

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
    }


    private String normalizarTermino(String termino) {
        return termino == null ? "" : termino.trim().toLowerCase();
    }

    private List<SugerenciaProducto> buscarEnCachePorPrefijo(String termino) {
        String mejorClave = null;
        List<SugerenciaProducto> mejorBase = null;

        synchronized (cacheBusqueda) {
            for (Map.Entry<String, List<SugerenciaProducto>> entry : cacheBusqueda.entrySet()) {
                String clave = entry.getKey();
                if (!termino.startsWith(clave)) {
                    continue;
                }

                if (mejorClave == null || clave.length() > mejorClave.length()) {
                    mejorClave = clave;
                    mejorBase = entry.getValue();
                }
            }
        }

        if (mejorBase == null) {
            return Collections.emptyList();
        }

        return filtrarSugerenciasLocales(mejorBase, termino);
    }

    private List<SugerenciaProducto> filtrarSugerenciasLocales(List<SugerenciaProducto> base, String termino) {
        List<SugerenciaProducto> filtradas = new ArrayList<>();
        for (SugerenciaProducto sugerencia : base) {
            if (coincide(sugerencia.id, termino) || coincide(sugerencia.nombre, termino)) {
                filtradas.add(sugerencia);
                if (filtradas.size() == 8) {
                    break;
                }
            }
        }
        return filtradas;
    }

    private boolean coincide(String valor, String termino) {
        return valor != null && valor.toLowerCase().contains(termino);
    }

    private void buscarConEnter() {
        String termino = searchBar.getText() == null ? "" : searchBar.getText().trim();

        if (termino.isEmpty()) {
            menuSugerencias.hide();
            return;
        }

        String terminoNormalizado = normalizarTermino(termino);
        List<SugerenciaProducto> sugerencias = cacheBusqueda.get(terminoNormalizado);
        if (sugerencias == null) {
            sugerencias = buscarProductos(termino);
            cacheBusqueda.put(terminoNormalizado, sugerencias);
        }

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
