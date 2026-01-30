package Operaciones.traspasoEntrada.controller;

import Compartido.controller.encabezadoController;
import Compartido.controller.navbarController;
import Compartido.exportar.ReporteTraspasoExporter;
import Compartido.helper.RefrescoHelper;
import Compartido.helper.SelectorOrdenPopup;
import Formularios.controller.ControllerUbicacionTraspaso;
import Operaciones.compra.model.UbicacionCompra;
import Operaciones.traspasoEntrada.model.model;
import Operaciones.traspasoEntrada.model.traspasoEntrada;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.Duration;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Function;

public class MainController {

    @FXML private TableColumn<traspasoEntrada, Void> colDesplegar;
    @FXML private ComboBox<String> miComboBox;
    @FXML private CheckBox miCheckBox;
    @FXML private StackPane root;
    @FXML private BorderPane paneNavbar;
    @FXML private VBox navbar;
    @FXML private VBox contenedor;
    @FXML private Pane overlayPane;
    @FXML private VBox contenedorTabla;
    @FXML private TableView<traspasoEntrada> contenidoTabla;
    @FXML private HBox contenedorBtnConfirmar;
    @FXML private HBox rootHBox;
    @FXML private Label lblOrdenar;
    @FXML private Region expansor;

    @FXML private TableColumn<traspasoEntrada, Boolean> colSelect;
    @FXML private TableColumn<traspasoEntrada, String> colClaveEntrada;
    @FXML private TableColumn<traspasoEntrada, String> colFecha;
    @FXML private TableColumn<traspasoEntrada, String> colHora;
    @FXML private TableColumn<traspasoEntrada, String> colTotal;
    @FXML private TableColumn<traspasoEntrada, String> colNombreSucural;

    @FXML private encabezadoController paneNavbarController;

    private String criterioOrden = "id";
    private String direccionOrden = "asc";

    private final ObservableList<traspasoEntrada> entradasTraspasoOriginal = FXCollections.observableArrayList();
    private final ObservableList<traspasoEntrada> entradasTraspaso = FXCollections.observableArrayList();

    private final model modeloTraspaso = new model();

    private Image flechaDerechaImage;
    private Image flechaAbajoImage;

    // Estado desplegado
    private final Map<String, Boolean> filasDesplegadas = new ConcurrentHashMap<>();

    // Cache de detalles por entrada (para que al expandir sea inmediato)
    private final Map<String, List<traspasoEntrada>> detallesPorEntrada = new ConcurrentHashMap<>();

    // Cargas detalle en progreso
    private final Map<String, Task<List<traspasoEntrada>>> cargasDetalle = new ConcurrentHashMap<>();

    // Pre-carga (opcional)
    private Task<Void> precargaDetallesTask;

    // Cache de nombres de producto (evita consultas en UI)
    private final Map<String, String> nombreProductoCache = new ConcurrentHashMap<>();
    private final Map<String, Task<String>> cargasNombreProducto = new ConcurrentHashMap<>();

    private StackPane overlayCarga;

    // Ejecutores: uno para interacción (expandir/cargar tabla), otro para precarga/nombres
    private final ExecutorService fxExecutor = Executors.newFixedThreadPool(
            Math.max(2, Runtime.getRuntime().availableProcessors() / 2),
            daemonFactory("fx-bg")
    );

    private final ExecutorService prefetchExecutor = Executors.newFixedThreadPool(
            2,
            daemonFactory("fx-prefetch")
    );

    private static ThreadFactory daemonFactory(String prefix) {
        return r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            t.setName(prefix + "-" + t.getId());
            return t;
        };
    }

    @FXML
    public void initialize() {
        miComboBox.setItems(FXCollections.observableArrayList("Aceptar", "Rechazar"));
        miComboBox.setValue("Opciones");

        cargarImagenesFlecha();

        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/Compartido/view/navbar.fxml"));
                VBox navbarLoaded = loader.load();
                navbarController navbarCtrl = loader.getController();
                navbarCtrl.setOverlayPane(overlayPane);
                navbar.getChildren().setAll(navbarLoaded);
            } catch (IOException e) {
                e.printStackTrace();
            }

            SplitPane.setResizableWithParent(navbar, false);
            SplitPane.setResizableWithParent(contenedor, true);

            paneNavbar.prefHeightProperty().bind(root.heightProperty().multiply(0.1));
            paneNavbar.prefWidthProperty().bind(root.widthProperty().multiply(0.9));

            navbar.prefWidthProperty().bind(root.widthProperty().multiply(0.15));
            navbar.prefHeightProperty().bind(root.heightProperty().multiply(0.9));

            contenedor.prefHeightProperty().bind(root.heightProperty().multiply(0.75));

            HBox.setHgrow(expansor, Priority.ALWAYS);
            expansor.setMinWidth(10);
            lblOrdenar.setMinWidth(Region.USE_PREF_SIZE);

            contenedorTabla.prefHeightProperty().bind(contenedor.heightProperty().multiply(0.95));
            contenidoTabla.prefHeightProperty().bind(contenedorTabla.heightProperty().multiply(0.9));

            paneNavbarController.setTitulo("Traspaso de Entrada", "#ffffff");

            configurarOverlayCarga();

            configurarTabla();
            cargarTabla(); // async
            RefrescoHelper.setVistaActual("traspasoEntrada");
            RefrescoHelper.registrarRefresco("traspasoEntrada", this::actualizarTraspasoEntrada);
        });
    }

    // =========================
    // Refresco / carga principal
    // =========================

    private void actualizarTraspasoEntrada() {
        Platform.runLater(() -> {
            contenidoTabla.getSelectionModel().clearSelection();
            if (miCheckBox != null) miCheckBox.setSelected(false);
            if (miComboBox != null) miComboBox.setValue("Opciones");

            // Cancelar tasks en progreso
            if (precargaDetallesTask != null) precargaDetallesTask.cancel();
            cargasDetalle.values().forEach(Task::cancel);
            cargasDetalle.clear();

            cargasNombreProducto.values().forEach(Task::cancel);
            cargasNombreProducto.clear();

            filasDesplegadas.clear();
            detallesPorEntrada.clear();
            nombreProductoCache.clear();

            entradasTraspasoOriginal.clear();
            entradasTraspaso.clear();
            contenidoTabla.refresh();
        });

        cargarTablaAsincrona();
    }

    private void cargarTabla() {
        cargarTablaAsincrona();
    }

    private void cargarTablaAsincrona() {
        runAsync(
                () -> modeloTraspaso.obtenerPendientes(),
                datos -> {
                    entradasTraspasoOriginal.setAll(datos);

                    filasDesplegadas.clear();
                    detallesPorEntrada.clear();

                    // Cancelar cargas anteriores (si existían)
                    cargasDetalle.values().forEach(Task::cancel);
                    cargasDetalle.clear();

                    if (!datos.isEmpty()) {
                        aplicarOrdenamiento();
                    } else {
                        entradasTraspaso.clear();
                        contenidoTabla.refresh();
                    }

                    // Opcional: precarga para que expandir sea instantáneo
                    iniciarPrecargaDetalles(datos);
                },
                ex -> {
                    ex.printStackTrace();
                    mostrarAlerta(Alert.AlertType.ERROR, "Error",
                            "No se pudieron cargar los traspasos: " + ex.getMessage());
                }
        );
    }

    private <T> void runAsync(Callable<T> background,
                              Consumer<T> onSuccessFxThread,
                              Consumer<Throwable> onErrorFxThread) {
        Task<T> task = new Task<>() {
            @Override
            protected T call() throws Exception {
                return background.call();
            }
        };
        task.setOnSucceeded(e -> onSuccessFxThread.accept(task.getValue()));
        task.setOnFailed(e -> onErrorFxThread.accept(task.getException()));
        fxExecutor.execute(task);
    }

    // =========================
    // Expansión rápida de detalles
    // =========================

    private void toggleFilaDesplegada(String claveEntrada) {
        boolean estaDesplegada = filasDesplegadas.getOrDefault(claveEntrada, false);

        if (estaDesplegada) {
            // Colapsar
            filasDesplegadas.put(claveEntrada, false);
            cancelarCargaDetalle(claveEntrada);
            contraerFila(claveEntrada);
            removerFilaCargando(claveEntrada);
            contenidoTabla.refresh();
            return;
        }

        // Expandir
        filasDesplegadas.put(claveEntrada, true);

        List<traspasoEntrada> filasDetalleCache = detallesPorEntrada.get(claveEntrada);
        if (filasDetalleCache != null && !filasDetalleCache.isEmpty()) {
            insertarDetallesEnTabla(claveEntrada, filasDetalleCache);
            contenidoTabla.refresh();
            return;
        }

        // Mostrar “Cargando…” inmediato para que se sienta rápido
        insertarFilaCargando(claveEntrada);
        contenidoTabla.refresh();

        // Cargar detalles en background
        cargarDetallesEnSegundoPlano(claveEntrada);
    }

    private void cancelarCargaDetalle(String claveEntrada) {
        Task<List<traspasoEntrada>> task = cargasDetalle.remove(claveEntrada);
        if (task != null) task.cancel();
    }

    private void cargarDetallesEnSegundoPlano(String claveEntrada) {
        if (cargasDetalle.containsKey(claveEntrada)) return;

        Task<List<traspasoEntrada>> task = new Task<>() {
            @Override
            protected List<traspasoEntrada> call() {
                List<model.DetalleEntrada> detalles = modeloTraspaso.obtenerDetallesEntrada(claveEntrada);
                return construirFilasDetalle(claveEntrada, detalles);
            }
        };

        task.setOnSucceeded(event -> {
            List<traspasoEntrada> filasDetalle = task.getValue();
            cargasDetalle.remove(claveEntrada);

            detallesPorEntrada.put(claveEntrada, filasDetalle);

            // Quitar “Cargando…”
            removerFilaCargando(claveEntrada);

            // Si todavía está desplegada, insertar
            if (filasDesplegadas.getOrDefault(claveEntrada, false) && filasDetalle != null && !filasDetalle.isEmpty()) {
                insertarDetallesEnTabla(claveEntrada, filasDetalle);
            }
            contenidoTabla.refresh();
        });

        task.setOnFailed(event -> {
            cargasDetalle.remove(claveEntrada);
            removerFilaCargando(claveEntrada);
            contenidoTabla.refresh();
        });

        cargasDetalle.put(claveEntrada, task);
        fxExecutor.execute(task);
    }

    private void insertarFilaCargando(String claveEntrada) {
        removerFilaCargando(claveEntrada);

        traspasoEntrada loading = new traspasoEntrada(
                "",            // claveEntrada visible (no se usa)
                "Cargando...",  // fecha (se usa en detalle como id, aquí solo texto)
                "", "", ""
        );
        loading.setClaveEntrada(claveEntrada + "_CARGANDO_1");
        loading.setSeleccionado(false);

        int index = indiceEntradaPadre(claveEntrada);
        if (index >= 0) {
            entradasTraspaso.add(index + 1, loading);
        }
    }

    private void removerFilaCargando(String claveEntrada) {
        entradasTraspaso.removeIf(e -> e != null && e.getClaveEntrada() != null
                && e.getClaveEntrada().startsWith(claveEntrada + "_CARGANDO_"));
    }

    private int indiceEntradaPadre(String claveEntrada) {
        for (int i = 0; i < entradasTraspaso.size(); i++) {
            traspasoEntrada e = entradasTraspaso.get(i);
            if (e != null && claveEntrada.equals(e.getClaveEntrada())) return i;
        }
        return -1;
    }

    private void contraerFila(String claveEntrada) {
        List<traspasoEntrada> filasDetalle = detallesPorEntrada.get(claveEntrada);
        if (filasDetalle != null && !filasDetalle.isEmpty()) {
            entradasTraspaso.removeAll(filasDetalle);
        }
    }

    private void insertarDetallesEnTabla(String claveEntrada, List<traspasoEntrada> filasDetalle) {
        // Evitar doble inserción si por alguna razón ya estaban
        entradasTraspaso.removeAll(filasDetalle);

        int index = indiceEntradaPadre(claveEntrada);
        if (index >= 0) {
            entradasTraspaso.addAll(index + 1, filasDetalle);
        }
    }

    private List<traspasoEntrada> construirFilasDetalle(String claveEntrada, List<model.DetalleEntrada> detalles) {
        List<traspasoEntrada> filasDetalle = new ArrayList<>();
        if (detalles == null || detalles.isEmpty()) return filasDetalle;

        // Encabezado detalle
        traspasoEntrada encabezadoDetalle = new traspasoEntrada(
                "Id",
                "Producto",
                "Cantidad",
                "Precio Unitario",
                "Precio Total"
        );
        encabezadoDetalle.setClaveEntrada(claveEntrada + "_ENCABEZADO_1");
        encabezadoDetalle.setSeleccionado(false);
        filasDetalle.add(encabezadoDetalle);

        int contador = 1;
        for (model.DetalleEntrada detalle : detalles) {
            String identificadorInterno = claveEntrada + "_DETALLE_" + (contador++);
            traspasoEntrada filaDetalle = new traspasoEntrada(
                    identificadorInterno,                 // claveEntrada interno
                    detalle.getClaveProducto(),           // fecha => ID Producto
                    detalle.getCantidad(),                // hora  => Cantidad
                    detalle.getPrecioUnitario(),          // total => Precio Unitario
                    detalle.getPrecioTotal()              // nombreSucursal => Precio Total (reutilizado)
            );
            filaDetalle.setSeleccionado(false);
            filasDetalle.add(filaDetalle);

            // Opcional: dispara carga del nombre del producto (sin bloquear)
            precargarNombreProducto(detalle.getClaveProducto());
        }

        return filasDetalle;
    }

    // =========================
    // Cache de nombres (sin freeze)
    // =========================

    private void precargarNombreProducto(String idProducto) {
        if (idProducto == null || idProducto.isBlank()) return;
        if (nombreProductoCache.containsKey(idProducto)) return;

        cargasNombreProducto.computeIfAbsent(idProducto, key -> {
            Task<String> t = new Task<>() {
                @Override
                protected String call() {
                    return modeloTraspaso.obtenerNombreProducto(key);
                }
            };

            t.setOnSucceeded(e -> {
                nombreProductoCache.put(key, t.getValue());
                cargasNombreProducto.remove(key);
                contenidoTabla.refresh();
            });

            t.setOnFailed(e -> {
                // Para no reintentar infinito, guardamos algo (o el mismo id)
                nombreProductoCache.putIfAbsent(key, key);
                cargasNombreProducto.remove(key);
                contenidoTabla.refresh();
            });

            prefetchExecutor.execute(t);
            return t;
        });
    }

    // =========================
    // Precarga de detalles (mejora expansión)
    // =========================

    private void iniciarPrecargaDetalles(List<traspasoEntrada> entradas) {
        if (entradas == null || entradas.isEmpty()) return;
        if (precargaDetallesTask != null && precargaDetallesTask.isRunning()) return;

        precargaDetallesTask = new Task<>() {
            @Override
            protected Void call() {
                int total = entradas.size();

                // Si tienes demasiadas filas y no quieres precargar todas, limita aquí:
                // int limite = Math.min(total, 80);
                int limite = total;

                for (int i = 0; i < limite; i++) {
                    if (isCancelled()) break;

                    traspasoEntrada entrada = entradas.get(i);
                    if (entrada == null) continue;
                    String clave = entrada.getClaveEntrada();
                    if (clave == null || clave.isBlank()) continue;

                    if (detallesPorEntrada.containsKey(clave)) continue;
                    if (cargasDetalle.containsKey(clave)) continue;

                    try {
                        List<model.DetalleEntrada> detalles = modeloTraspaso.obtenerDetallesEntrada(clave);
                        List<traspasoEntrada> filas = construirFilasDetalle(clave, detalles);
                        detallesPorEntrada.put(clave, filas);
                    } catch (Exception ignored) {
                    }
                }
                return null;
            }
        };

        prefetchExecutor.execute(precargaDetallesTask);
    }

    // =========================
    // Tabla / UI
    // =========================

    private void cargarImagenesFlecha() {
        try {
            flechaDerechaImage = new Image(getClass().getResourceAsStream("/img/flecha-derecha.png"));
            flechaAbajoImage = new Image(getClass().getResourceAsStream("/img/flecha-abajo.png"));

            if (flechaAbajoImage.isError()) {
                flechaAbajoImage = flechaDerechaImage;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void configurarTabla() {
        contenidoTabla.setSortPolicy(param -> false);

        configurarColumnaFlecha();

        colSelect.setCellValueFactory(param -> {
            traspasoEntrada item = param.getValue();
            if (item != null && !esFilaDetalle(item) && !esEncabezadoDetalle(item) && !esFilaCargando(item)) {
                return item.seleccionadoProperty();
            }
            return new SimpleBooleanProperty(false);
        });

        colSelect.setCellFactory(param -> new CheckBoxTableCell<>() {
            @Override
            public void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                traspasoEntrada rowData = getTableRow() != null ? getTableRow().getItem() : null;
                if (rowData != null && (esFilaDetalle(rowData) || esEncabezadoDetalle(rowData) || esFilaCargando(rowData))) {
                    setGraphic(null);
                }
            }
        });

        colClaveEntrada.setCellValueFactory(new PropertyValueFactory<>("claveEntrada"));
        colFecha.setCellValueFactory(new PropertyValueFactory<>("fecha"));
        colHora.setCellValueFactory(new PropertyValueFactory<>("hora"));
        colTotal.setCellValueFactory(new PropertyValueFactory<>("total"));
        colNombreSucural.setCellValueFactory(new PropertyValueFactory<>("nombreSucursal"));

        contenidoTabla.setEditable(true);
        contenidoTabla.setItems(entradasTraspaso);

        TableColumn<traspasoEntrada, ?>[] columnas = new TableColumn[]{
                colDesplegar, colSelect, colClaveEntrada, colFecha, colHora, colTotal, colNombreSucural
        };
        for (TableColumn<traspasoEntrada, ?> col : columnas) {
            col.setStyle("-fx-alignment: CENTER;");
        }

        miCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            for (traspasoEntrada entrada : entradasTraspaso) {
                if (!esFilaDetalle(entrada) && !esEncabezadoDetalle(entrada) && !esFilaCargando(entrada)) {
                    entrada.setSeleccionado(newVal);
                }
            }
        });

        configurarCellFactories();
    }

    private void configurarColumnaFlecha() {
        colDesplegar.setCellFactory(param -> new TableCell<>() {
            private final ImageView imageView = new ImageView();
            private final Button button = new Button();

            {
                imageView.setFitHeight(20);
                imageView.setFitWidth(20);
                imageView.setPreserveRatio(true);

                button.setGraphic(imageView);
                button.setStyle("-fx-background-color: transparent; -fx-padding: 0;");
                button.setOnAction(event -> {
                    traspasoEntrada item = getTableRow().getItem();
                    if (item != null && !esFilaDetalle(item) && !esEncabezadoDetalle(item) && !esFilaCargando(item)) {
                        toggleFilaDesplegada(item.getClaveEntrada());
                    }
                });
                setAlignment(Pos.CENTER);
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || getTableRow().getItem() == null) {
                    setGraphic(null);
                    return;
                }

                traspasoEntrada rowItem = getTableRow().getItem();

                if (esFilaDetalle(rowItem) || esEncabezadoDetalle(rowItem) || esFilaCargando(rowItem)) {
                    setGraphic(null);
                    return;
                }

                String clave = rowItem.getClaveEntrada();
                boolean desplegada = filasDesplegadas.getOrDefault(clave, false);
                imageView.setImage(desplegada ? flechaAbajoImage : flechaDerechaImage);
                setGraphic(button);
            }
        });

        colDesplegar.setPrefWidth(40);
        colDesplegar.setResizable(false);
        colDesplegar.setSortable(false);
    }

    private boolean esFilaDetalle(traspasoEntrada entrada) {
        return entrada.getClaveEntrada() != null && entrada.getClaveEntrada().contains("_DETALLE_");
    }

    private boolean esEncabezadoDetalle(traspasoEntrada entrada) {
        return entrada.getClaveEntrada() != null && entrada.getClaveEntrada().contains("_ENCABEZADO_");
    }

    private boolean esFilaCargando(traspasoEntrada entrada) {
        return entrada.getClaveEntrada() != null && entrada.getClaveEntrada().contains("_CARGANDO_");
    }

    private void configurarCellFactories() {
        String colorBorde = "#4A4848";

        // Columna "Id" (en detalle muestra ID del producto SIN loops pesados)
        colClaveEntrada.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                    setStyle("");
                    return;
                }

                traspasoEntrada rowData = getTableRow().getItem();

                if (esFilaCargando(rowData)) {
                    setText("");
                    setStyle("-fx-alignment: CENTER;");
                    return;
                }

                if (esEncabezadoDetalle(rowData)) {
                    setText("Id");
                    setStyle("-fx-background-color: #6A6767; -fx-text-fill: white; -fx-alignment: CENTER; -fx-font-weight: bold;");
                    return;
                }

                if (esFilaDetalle(rowData)) {
                    // Aquí el ID real está en rowData.getFecha()
                    setText(rowData.getFecha());
                    setStyle("-fx-alignment: CENTER; -fx-border-color: " + colorBorde + "; -fx-border-width: 0 1 1 1;");
                    return;
                }

                setText(item);
                setStyle("-fx-alignment: CENTER;");
            }
        });

        // Columna "Producto" (carga nombre con cache + hilo, sin congelar UI)
        colFecha.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                    setStyle("");
                    return;
                }

                traspasoEntrada rowData = getTableRow().getItem();

                if (esFilaCargando(rowData)) {
                    setText("Cargando...");
                    setStyle("-fx-alignment: CENTER; -fx-font-style: italic;");
                    return;
                }

                if (esEncabezadoDetalle(rowData)) {
                    setText("Producto");
                    setStyle("-fx-background-color: #6A6767; -fx-text-fill: white; -fx-alignment: CENTER; -fx-font-weight: bold;");
                    return;
                }

                if (esFilaDetalle(rowData)) {
                    String idProducto = rowData.getFecha(); // aquí está el id
                    String nombre = nombreProductoCache.get(idProducto);

                    if (nombre == null || nombre.isBlank()) {
                        setText("...");
                        precargarNombreProducto(idProducto);
                    } else {
                        setText(nombre);
                    }

                    setStyle("-fx-alignment: CENTER; -fx-border-color: " + colorBorde + "; -fx-border-width: 0 1 1 0;");
                    return;
                }

                setText(item);
                setStyle("-fx-alignment: CENTER;");
            }
        });

        // Cantidad
        colHora.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                    setStyle("");
                    return;
                }

                traspasoEntrada rowData = getTableRow().getItem();

                if (esFilaCargando(rowData)) {
                    setText("");
                    setStyle("-fx-alignment: CENTER;");
                    return;
                }

                if (esEncabezadoDetalle(rowData)) {
                    setText("Cantidad");
                    setStyle("-fx-background-color: #6A6767; -fx-text-fill: white; -fx-alignment: CENTER; -fx-font-weight: bold;");
                    return;
                }

                if (esFilaDetalle(rowData)) {
                    setText(item);
                    setStyle("-fx-alignment: CENTER; -fx-border-color: " + colorBorde + "; -fx-border-width: 0 1 1 0;");
                    return;
                }

                setText(item);
                setStyle("-fx-alignment: CENTER;");
            }
        });

        // Precio Unitario
        colTotal.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                    setStyle("");
                    return;
                }

                traspasoEntrada rowData = getTableRow().getItem();

                if (esFilaCargando(rowData)) {
                    setText("");
                    setStyle("-fx-alignment: CENTER;");
                    return;
                }

                if (esEncabezadoDetalle(rowData)) {
                    setText("Precio Unitario");
                    setStyle("-fx-background-color: #6A6767; -fx-text-fill: white; -fx-alignment: CENTER; -fx-font-weight: bold;");
                    return;
                }

                if (esFilaDetalle(rowData)) {
                    setText(item);
                    setStyle("-fx-alignment: CENTER; -fx-border-color: " + colorBorde + "; -fx-border-width: 0 1 1 0;");
                    return;
                }

                setText(item);
                setStyle("-fx-alignment: CENTER;");
            }
        });

        // Precio Total
        colNombreSucural.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                    setStyle("");
                    return;
                }

                traspasoEntrada rowData = getTableRow().getItem();

                if (esFilaCargando(rowData)) {
                    setText("");
                    setStyle("-fx-alignment: CENTER;");
                    return;
                }

                if (esEncabezadoDetalle(rowData)) {
                    setText("Precio Total");
                    setStyle("-fx-background-color: #6A6767; -fx-text-fill: white; -fx-alignment: CENTER; -fx-font-weight: bold;");
                    return;
                }

                if (esFilaDetalle(rowData)) {
                    setText(item);
                    setStyle("-fx-alignment: CENTER; -fx-border-color: " + colorBorde + "; -fx-border-width: 0 1 1 0;");
                    return;
                }

                setText(item);
                setStyle("-fx-alignment: CENTER;");
            }
        });
    }

    // =========================
    // Ordenamiento
    // =========================

    private void aplicarOrdenamiento() {
        List<traspasoEntrada> listaOrdenada = new ArrayList<>(entradasTraspasoOriginal);

        Comparator<traspasoEntrada> comparator;
        Function<String, String> normalizar = valor -> valor == null ? "" : valor.toLowerCase();

        switch (criterioOrden) {
            case "fecha":
                comparator = Comparator.comparing(item -> {
                    String fecha = item.getFecha();
                    return (fecha == null || fecha.isBlank()) ? "" : fecha;
                });
                break;
            case "sucursal":
                comparator = Comparator.comparing(item -> normalizar.apply(item.getNombreSucursal()));
                break;
            case "id":
            default:
                comparator = Comparator.comparing(item -> normalizar.apply(item.getClaveEntrada()));
                break;
        }

        if ("desc".equalsIgnoreCase(direccionOrden)) comparator = comparator.reversed();

        listaOrdenada.sort(comparator);
        actualizarTablaConOrdenamiento(listaOrdenada);
    }

    private void actualizarTablaConOrdenamiento(List<traspasoEntrada> listaOrdenada) {
        entradasTraspaso.clear();

        for (traspasoEntrada entrada : listaOrdenada) {
            entradasTraspaso.add(entrada);

            String clave = entrada.getClaveEntrada();
            if (filasDesplegadas.getOrDefault(clave, false)) {
                List<traspasoEntrada> detalles = detallesPorEntrada.get(clave);
                if (detalles != null && !detalles.isEmpty()) {
                    entradasTraspaso.addAll(detalles);
                }
            }
        }

        contenidoTabla.refresh();
    }

    @FXML
    private void mostrarOrdenPopup(MouseEvent event) {
        List<String> criterios = Arrays.asList("id", "fecha", "sucursal");

        SelectorOrdenPopup.mostrar(
                (Node) event.getSource(),
                event.getScreenX(),
                event.getScreenY(),
                criterios,
                criterioOrden,
                direccionOrden,
                seleccion -> {
                    criterioOrden = seleccion.getCriterio();
                    direccionOrden = seleccion.getDireccion();
                    aplicarOrdenamiento();
                }
        );
    }

    // =========================
    // Acciones aceptar / rechazar
    // =========================

    @FXML
    public void aplicarAccion() {
        String opcion = miComboBox.getValue();
        if (opcion == null || opcion.equalsIgnoreCase("Opciones")) {
            mostrarAlerta(Alert.AlertType.WARNING, "Selecciona una opción", "Debes seleccionar Aceptar o Rechazar.");
            return;
        }

        List<traspasoEntrada> seleccionados = obtenerSeleccionados();
        if (seleccionados.isEmpty()) {
            mostrarAlerta(Alert.AlertType.WARNING, "Selección requerida", "Se debe seleccionar alguna entrada.");
            return;
        }

        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Confirmación");
        confirmacion.setHeaderText("¿Deseas continuar?");
        confirmacion.setContentText("Se aplicarán cambios a las entradas seleccionadas.");

        confirmacion.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                procesarSeleccion(opcion, seleccionados);
            }
        });
    }

    private void procesarSeleccion(String opcion, List<traspasoEntrada> seleccionados) {
        List<String> claves = new ArrayList<>();
        for (traspasoEntrada entrada : seleccionados) claves.add(entrada.getClaveEntrada());

        boolean rechazar = opcion.equalsIgnoreCase("Rechazar");
        String nuevoEstadoEntrada = rechazar ? "rechazado" : "disponible";
        String nuevoEstadoArticulos = rechazar ? "rechazado" : "disponible";

        if (rechazar) {
            Alert confirmacionRechazar = new Alert(Alert.AlertType.CONFIRMATION);
            confirmacionRechazar.setTitle("Confirmar eliminación");
            confirmacionRechazar.setHeaderText("¿Estás seguro de rechazar las entradas seleccionadas?");
            confirmacionRechazar.setContentText("Se eliminarán " + seleccionados.size() + " entrada(s).\nEsta acción no se puede deshacer.");

            ButtonType botonSi = new ButtonType("Sí, rechazar");
            ButtonType botonCancelar = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
            confirmacionRechazar.getButtonTypes().setAll(botonSi, botonCancelar);

            confirmacionRechazar.showAndWait().ifPresent(response -> {
                if (response == botonSi) {
                    ejecutarActualizacionAsync(claves, nuevoEstadoEntrada, nuevoEstadoArticulos, seleccionados.size());
                }
            });
        } else {
            abrirFormularioUbicaciones(seleccionados, claves, nuevoEstadoEntrada, nuevoEstadoArticulos);
        }
    }

    private void ejecutarActualizacionAsync(List<String> claves, String nuevoEstadoEntrada,
                                            String nuevoEstadoArticulos, int cantidad) {

        runAsync(
                () -> modeloTraspaso.actualizarEstadoEntradas(claves, nuevoEstadoEntrada, nuevoEstadoArticulos),
                actualizado -> {
                    if (actualizado) {
                        entradasTraspasoOriginal.removeIf(e -> claves.contains(e.getClaveEntrada()));
                        actualizarTablaConOrdenamiento(new ArrayList<>(entradasTraspasoOriginal));
                        miCheckBox.setSelected(false);

                        String mensajeExito = nuevoEstadoEntrada.equals("rechazado")
                                ? "Se eliminaron " + cantidad + " entrada(s) correctamente."
                                : "Se aceptaron " + cantidad + " entrada(s) correctamente.";

                        mostrarAlerta(Alert.AlertType.INFORMATION, "Operación exitosa", mensajeExito);
                    } else {
                        mostrarAlerta(Alert.AlertType.ERROR, "Error", "No se pudieron actualizar las entradas.");
                    }
                },
                ex -> mostrarAlerta(Alert.AlertType.ERROR, "Error", "Fallo actualizando: " + ex.getMessage())
        );
    }

    private List<traspasoEntrada> obtenerSeleccionados() {
        List<traspasoEntrada> seleccionados = new ArrayList<>();
        for (traspasoEntrada entrada : entradasTraspaso) {
            if (entrada.isSeleccionado() && !esFilaDetalle(entrada) && !esEncabezadoDetalle(entrada) && !esFilaCargando(entrada)) {
                seleccionados.add(entrada);
            }
        }
        return seleccionados;
    }

    // =========================
    // Formulario ubicaciones (igual que tenías)
    // =========================

    private void abrirFormularioUbicaciones(List<traspasoEntrada> seleccionados,
                                            List<String> claves,
                                            String nuevoEstadoEntrada,
                                            String nuevoEstadoArticulos) {

        List<traspasoEntrada> pendientes = new ArrayList<>(seleccionados);
        procesarSiguienteUbicacion(pendientes, claves, nuevoEstadoEntrada, nuevoEstadoArticulos);
    }

    private void procesarSiguienteUbicacion(List<traspasoEntrada> pendientes,
                                            List<String> claves,
                                            String nuevoEstadoEntrada,
                                            String nuevoEstadoArticulos) {

        if (pendientes.isEmpty()) {
            Platform.runLater(() -> {
                mostrarAlerta(Alert.AlertType.INFORMATION, "Proceso completado",
                        "Se han procesado todos los traspasos seleccionados.");
                cargarTabla();
            });
            return;
        }

        traspasoEntrada actual = pendientes.remove(0);
        String claveEntrada = actual.getClaveEntrada();

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Formularios/view/ubicacionTraspaso.fxml"));
            Parent root = loader.load();

            ControllerUbicacionTraspaso controller = loader.getController();
            controller.setClaveEntrada(claveEntrada);

            controller.setOnConfirmCallback(ubicacionesPorProducto -> {
                actualizarEntradaIndividual(claveEntrada, nuevoEstadoEntrada, nuevoEstadoArticulos, pendientes, ubicacionesPorProducto);
            });

            Stage stage = new Stage();
            controller.setStage(stage);

            Scene scene = new Scene(root, 500, 700);
            stage.setScene(scene);
            stage.setTitle("Ubicaciones - " + claveEntrada + " (" + (claves.size() - pendientes.size()) + "/" + claves.size() + ")");
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(contenidoTabla.getScene().getWindow());
            stage.setResizable(false);
            stage.centerOnScreen();

            stage.setOnHidden(e -> {
                if (!pendientes.isEmpty()) {
                    PauseTransition pause = new PauseTransition(Duration.millis(300));
                    pause.setOnFinished(ev -> procesarSiguienteUbicacion(pendientes, claves, nuevoEstadoEntrada, nuevoEstadoArticulos));
                    pause.play();
                } else {
                    cargarTabla();
                }
            });

            stage.show();

        } catch (Exception e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error",
                    "No se pudo abrir el formulario de ubicaciones para: " + claveEntrada);
            e.printStackTrace();

            if (!pendientes.isEmpty()) {
                PauseTransition pause = new PauseTransition(Duration.millis(300));
                pause.setOnFinished(ev -> procesarSiguienteUbicacion(pendientes, claves, nuevoEstadoEntrada, nuevoEstadoArticulos));
                pause.play();
            }
        }
    }

    private void actualizarEntradaIndividual(String claveEntrada,
                                             String nuevoEstadoEntrada,
                                             String nuevoEstadoArticulos,
                                             List<traspasoEntrada> pendientes,
                                             Map<String, List<UbicacionCompra>> ubicacionesPorProducto) {

        mostrarOverlayCarga();
        runAsync(
                () -> {
                    model.ResultadoOperacion resultado = modeloTraspaso.actualizarUbicacionesYEstados(
                            claveEntrada,
                            ubicacionesPorProducto,
                            nuevoEstadoEntrada,
                            nuevoEstadoArticulos
                    );

                    List<model.DetalleEntrada> detalles = null;
                    if (resultado.isExito()) {
                        detalles = modeloTraspaso.obtenerDetallesEntrada(claveEntrada);
                    }
                    return new Object[]{resultado, detalles};
                },
                payload -> {
                    model.ResultadoOperacion resultado = (model.ResultadoOperacion) payload[0];
                    @SuppressWarnings("unchecked")
                    List<model.DetalleEntrada> detalles = (List<model.DetalleEntrada>) payload[1];

                    if (resultado.isExito()) {
                        ocultarOverlayCarga();
                        entradasTraspasoOriginal.removeIf(e -> e.getClaveEntrada().equals(claveEntrada));
                        actualizarTablaConOrdenamiento(new ArrayList<>(entradasTraspasoOriginal));

                        mostrarConfirmacionReporte(claveEntrada, resultado.getMensaje(), detalles, ubicacionesPorProducto);

                        if (!pendientes.isEmpty()) {
                            PauseTransition pause = new PauseTransition(Duration.millis(500));
                            pause.setOnFinished(e -> procesarSiguienteUbicacion(
                                    pendientes, new ArrayList<>(), nuevoEstadoEntrada, nuevoEstadoArticulos
                            ));
                            pause.play();
                        }
                    } else {
                        ocultarOverlayCarga();
                        mostrarAlerta(Alert.AlertType.ERROR, "Error",
                                "No se pudo actualizar el traspaso: " + resultado.getMensaje());

                        if (!pendientes.isEmpty()) {
                            PauseTransition pause = new PauseTransition(Duration.millis(500));
                            pause.setOnFinished(e -> procesarSiguienteUbicacion(
                                    pendientes, new ArrayList<>(), nuevoEstadoEntrada, nuevoEstadoArticulos
                            ));
                            pause.play();
                        }
                    }
                },
                ex -> {
                    ocultarOverlayCarga();
                    mostrarAlerta(Alert.AlertType.ERROR, "Error",
                            "Fallo actualizando traspaso: " + ex.getMessage());
                }
        );
    }

    private void mostrarConfirmacionReporte(String claveEntrada,
                                            String mensaje,
                                            List<model.DetalleEntrada> detalles,
                                            Map<String, List<UbicacionCompra>> ubicacionesPorProducto) {

        String comentario = modeloTraspaso.obtenerComentarioEntrada(claveEntrada);

        Alert dialogo = new Alert(Alert.AlertType.CONFIRMATION);
        dialogo.setTitle("Registro exitoso");
        dialogo.setHeaderText(mensaje);
        dialogo.setContentText("¿Deseas descargar el reporte de este traspaso?");
        ButtonType btnDescargar = new ButtonType("Descargar");
        ButtonType btnAhoraNo = new ButtonType("Ahora no", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialogo.getButtonTypes().setAll(btnDescargar, btnAhoraNo);

        dialogo.showAndWait().ifPresent(respuesta -> {
            if (respuesta == btnDescargar) {
                ReporteTraspasoExporter.exportarReporte(
                        claveEntrada,
                        detalles,
                        ubicacionesPorProducto,
                        (contenidoTabla != null && contenidoTabla.getScene() != null) ? contenidoTabla.getScene().getWindow() : null,
                        comentario
                );
            }
        });
    }

    private void mostrarAlerta(Alert.AlertType tipo, String titulo, String mensaje) {
        Alert alerta = new Alert(tipo);
        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }

    private void configurarOverlayCarga() {
        if (overlayPane == null || root == null || overlayCarga != null) {
            return;
        }

        overlayPane.setPickOnBounds(true);

        Label labelCarga = new Label("Cargando...");
        labelCarga.setStyle("-fx-text-fill: white; -fx-font-size: 26px; -fx-font-weight: bold;");

        overlayCarga = new StackPane(labelCarga);
        overlayCarga.setVisible(false);
        overlayCarga.setManaged(false);
        overlayCarga.setPickOnBounds(true);
        overlayCarga.setStyle("-fx-background-color: rgba(0, 0, 0, 0.55);");
        overlayCarga.setAlignment(Pos.CENTER);

        overlayCarga.prefWidthProperty().bind(root.widthProperty());
        overlayCarga.prefHeightProperty().bind(root.heightProperty());
        overlayCarga.setMinWidth(Region.USE_PREF_SIZE);
        overlayCarga.setMinHeight(Region.USE_PREF_SIZE);

        overlayPane.getChildren().add(overlayCarga);
    }

    private void mostrarOverlayCarga() {
        if (overlayCarga == null) {
            configurarOverlayCarga();
        }
        if (overlayCarga == null) {
            return;
        }
        Platform.runLater(() -> {
            overlayCarga.setManaged(true);
            overlayCarga.setVisible(true);
            overlayCarga.toFront();
        });
    }

    private void ocultarOverlayCarga() {
        if (overlayCarga == null) {
            return;
        }
        Platform.runLater(() -> {
            overlayCarga.setVisible(false);
            overlayCarga.setManaged(false);
        });
    }
}
