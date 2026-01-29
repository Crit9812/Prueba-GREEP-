package Operaciones.traspasoEntrada.controller;

import Compartido.helper.RefrescoHelper;
import Compartido.helper.SelectorOrdenPopup;
import javafx.collections.transformation.SortedList;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import java.util.Comparator;
import java.util.function.Function;
import javafx.animation.PauseTransition;
import javafx.util.Duration;
import javafx.application.Platform;
import Formularios.controller.ControllerUbicacionTraspaso;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.fxml.FXMLLoader;
import Compartido.controller.encabezadoController;
import Compartido.controller.navbarController;
import Compartido.exportar.ReporteTraspasoExporter;
import Operaciones.compra.model.UbicacionCompra;
import Operaciones.traspasoEntrada.model.model;
import Operaciones.traspasoEntrada.model.traspasoEntrada;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.layout.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableValue;
import javafx.util.Callback;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// Importaciones añadidas para manejar imágenes
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.geometry.Pos;

public class MainController {

    @FXML private TableColumn<traspasoEntrada, Void> colDesplegar; // Nueva columna para la flecha
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

    private String criterioOrden = "id"; // id, fecha, sucursal
    private String direccionOrden = "asc";
    private final ObservableList<traspasoEntrada> entradasTraspasoOriginal = FXCollections.observableArrayList();
    private final ObservableList<traspasoEntrada> entradasTraspaso = FXCollections.observableArrayList();
    private final model modeloTraspaso = new model();
    private Image flechaDerechaImage; // Imagen de flecha derecha
    private Image flechaAbajoImage;   // Imagen de flecha abajo

    // Mapa para rastrear qué filas están desplegadas
    private Map<String, Boolean> filasDesplegadas = new ConcurrentHashMap<>();
    // Mapa para almacenar las filas de detalle por cada entrada
    private Map<String, List<traspasoEntrada>> detallesPorEntrada = new ConcurrentHashMap<>();
    // Mapa para rastrear cargas en progreso
    private Map<String, javafx.concurrent.Task<List<traspasoEntrada>>> cargasDetalle = new ConcurrentHashMap<>();
    // Tarea de precarga de detalles
    private javafx.concurrent.Task<Void> precargaDetallesTask;

    @FXML
    public void initialize() {
        miComboBox.setItems(FXCollections.observableArrayList("Aceptar", "Rechazar"));
        miComboBox.setValue("Opciones");
        cargarImagenesFlecha();

        Platform.runLater(() -> {

            try {
                // Cargar el navbar desde el fx:include
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/Compartido/view/navbar.fxml"));
                VBox navbarLoaded = loader.load();

                // Obtener el controller del navbar
                navbarController navbarCtrl = loader.getController();

                // Pasar el overlayPane al navbarController
                navbarCtrl.setOverlayPane(overlayPane);

                // Reemplazar el contenido del fx:include con el cargado
                navbar.getChildren().setAll(navbarLoaded);

            } catch (IOException e) {
                e.printStackTrace();
            }

            SplitPane.setResizableWithParent(navbar, false);
            SplitPane.setResizableWithParent(contenedor, true);

            //Navbar superior (header)
            paneNavbar.prefHeightProperty().bind(root.heightProperty().multiply(0.1));
            paneNavbar.prefWidthProperty().bind(root.widthProperty().multiply(0.9));

            // Navbar lateral (menú)
            navbar.prefWidthProperty().bind(root.widthProperty().multiply(0.15));
            navbar.prefHeightProperty().bind(root.heightProperty().multiply(0.9));

            // Center - contenedor general
            contenedor.prefHeightProperty().bind(root.heightProperty().multiply(0.75));

            // Barra de opciones
            HBox.setHgrow(expansor, Priority.ALWAYS);
            expansor.setMinWidth(10);
            lblOrdenar.setMinWidth(Region.USE_PREF_SIZE);

            // Tabla
            contenedorTabla.prefHeightProperty().bind(contenedor.heightProperty().multiply(0.95));
            contenidoTabla.prefHeightProperty().bind(contenedorTabla.heightProperty().multiply(0.9));

            paneNavbarController.setTitulo("Traspaso de Entrada", "#ffffff");

            configurarTabla();
            cargarTabla();
            aplicarOrdenamiento();
            RefrescoHelper.setVistaActual("traspasoEntrada");
            RefrescoHelper.registrarRefresco("traspasoEntrada", this::actualizarTraspasoEntrada);
        });
    }

    private void actualizarTraspasoEntrada() {

        // 1. Limpiar UI y datos locales
        Platform.runLater(() -> {
            // Limpiar selección
            contenidoTabla.getSelectionModel().clearSelection();

            // Limpiar checkbox global
            if (miCheckBox != null) {
                miCheckBox.setSelected(false);
            }

            // Resetear combo box
            if (miComboBox != null) {
                miComboBox.setValue("Opciones");
            }

            // Limpiar estructuras de datos
            filasDesplegadas.clear();
            detallesPorEntrada.clear();
            cargasDetalle.clear();
            if (precargaDetallesTask != null) {
                precargaDetallesTask.cancel();
            }

            // Limpiar listas
            entradasTraspasoOriginal.clear();
            entradasTraspaso.clear();

            contenidoTabla.refresh();
            System.out.println("UI de traspaso entrada limpiada");
        });

        // 2. Recargar datos de forma asíncrona (IMPORTANTE para muchos registros)
        cargarTablaAsincrona();

    }

    private void cargarTablaAsincrona() {
        javafx.concurrent.Task<List<traspasoEntrada>> task = new javafx.concurrent.Task<>() {
            @Override
            protected List<traspasoEntrada> call() throws Exception {
                return modeloTraspaso.obtenerPendientes();
            }

            @Override
            protected void succeeded() {
                // Esto se ejecuta en el hilo de JavaFX cuando termina
                List<traspasoEntrada> datos = getValue();

                // Actualizar las listas en el hilo de JavaFX
                entradasTraspasoOriginal.setAll(datos);

                // Si hay datos, aplicamos ordenamiento
                if (!datos.isEmpty()) {
                    aplicarOrdenamiento();
                } else {
                    entradasTraspaso.clear();
                    contenidoTabla.refresh();
                }

                // Limpiar estado de filas desplegadas
                filasDesplegadas.clear();
                detallesPorEntrada.clear();
                cargasDetalle.clear();
                iniciarPrecargaDetalles(datos);
            }

            @Override
            protected void failed() {
                // Manejo de errores
                Throwable ex = getException();
                System.err.println("✗ Error al cargar traspasos: " + ex.getMessage());
                ex.printStackTrace();

                // Mostrar mensaje al usuario
                Platform.runLater(() -> {
                    mostrarAlerta(Alert.AlertType.ERROR, "Error",
                            "No se pudieron cargar los traspasos: " + ex.getMessage());
                });
            }
        };

        // Iniciar la tarea en un hilo separado
        Thread hilo = new Thread(task);
        hilo.setDaemon(true); // El hilo se cerrará cuando la aplicación cierre
        hilo.start();
    }

    private void cargarTabla() {
        // Usar la versión asíncrona en lugar de la síncrona
        cargarTablaAsincrona();
    }

    private void abrirFormularioUbicaciones(List<traspasoEntrada> seleccionados,
                                            List<String> claves,
                                            String nuevoEstadoEntrada,
                                            String nuevoEstadoArticulos) {

        // Crear una copia de los seleccionados para procesar secuencialmente
        List<traspasoEntrada> pendientes = new ArrayList<>(seleccionados);

        // Procesar el primer traspaso
        procesarSiguienteUbicacion(pendientes, claves, nuevoEstadoEntrada, nuevoEstadoArticulos);
    }

    private void procesarSiguienteUbicacion(List<traspasoEntrada> pendientes,
                                            List<String> claves,
                                            String nuevoEstadoEntrada,
                                            String nuevoEstadoArticulos) {

        if (pendientes.isEmpty()) {
            // Todos los traspasos han sido procesados
            Platform.runLater(() -> {
                mostrarAlerta(Alert.AlertType.INFORMATION, "Proceso completado",
                        "Se han procesado todos los traspasos seleccionados.");
                // Refrescar toda la tabla al finalizar
                cargarTabla();
            });
            return;
        }

        // Tomar el primer traspaso de la lista
        traspasoEntrada actual = pendientes.remove(0);
        String claveEntrada = actual.getClaveEntrada();

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Formularios/view/ubicacionTraspaso.fxml"));
            Parent root = loader.load();

            ControllerUbicacionTraspaso controller = loader.getController();
            controller.setClaveEntrada(claveEntrada);

            // Configurar callback para cuando se confirme este traspaso
            controller.setOnConfirmCallback(ubicacionesPorProducto -> {
                // Actualizar la entrada específica después de asignar ubicaciones
                actualizarEntradaIndividual(claveEntrada, nuevoEstadoEntrada, nuevoEstadoArticulos, pendientes, ubicacionesPorProducto);
            });

            Stage stage = new Stage();
            controller.setStage(stage); // Pasar la referencia del stage al controller

            Scene scene = new Scene(root, 500, 700);
            stage.setScene(scene);
            stage.setTitle("Ubicaciones - " + claveEntrada + " (" + (claves.size() - pendientes.size()) + "/" + claves.size() + ")");
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(contenidoTabla.getScene().getWindow());
            stage.setResizable(false);

            // Centrar la ventana
            stage.centerOnScreen();

            // Configurar para que al cerrar se procese el siguiente (si se cierra sin confirmar)
            stage.setOnHidden(e -> {
                if (!pendientes.isEmpty()) {
                    // Esperar un momento antes de abrir el siguiente
                    Platform.runLater(() -> {
                        PauseTransition pause = new PauseTransition(Duration.millis(300));
                        pause.setOnFinished(ev -> {
                            procesarSiguienteUbicacion(pendientes, claves, nuevoEstadoEntrada, nuevoEstadoArticulos);
                        });
                        pause.play();
                    });
                } else {
                    // Si no hay más pendientes, refrescar la tabla
                    Platform.runLater(() -> {
                        cargarTabla();
                    });
                }
            });

            stage.show();

        } catch (Exception e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error",
                    "No se pudo abrir el formulario de ubicaciones para: " + claveEntrada);
            e.printStackTrace();

            // Si hay error, intentar con el siguiente traspaso
            if (!pendientes.isEmpty()) {
                Platform.runLater(() -> {
                    PauseTransition pause = new PauseTransition(Duration.millis(300));
                    pause.setOnFinished(ev -> {
                        procesarSiguienteUbicacion(pendientes, claves, nuevoEstadoEntrada, nuevoEstadoArticulos);
                    });
                    pause.play();
                });
            }
        }
    }

    private void actualizarEntradaIndividual(String claveEntrada,
                                             String nuevoEstadoEntrada,
                                             String nuevoEstadoArticulos,
                                             List<traspasoEntrada> pendientes,
                                             java.util.Map<String, List<UbicacionCompra>> ubicacionesPorProducto) {
        // Actualizar esta entrada específica en la base de datos
        model.ResultadoOperacion resultado = modeloTraspaso.actualizarUbicacionesYEstados(
                claveEntrada,
                ubicacionesPorProducto,
                nuevoEstadoEntrada,
                nuevoEstadoArticulos
        );

        if (resultado.isExito()) {
            // Actualizar la lista original quitando la entrada procesada
            entradasTraspasoOriginal.removeIf(e -> e.getClaveEntrada().equals(claveEntrada));

            // Actualizar la tabla manteniendo el orden
            actualizarTablaConOrdenamiento(new ArrayList<>(entradasTraspasoOriginal));

            List<model.DetalleEntrada> detalles = modeloTraspaso.obtenerDetallesEntrada(claveEntrada);
            Platform.runLater(() -> mostrarConfirmacionReporte(claveEntrada, resultado.getMensaje(), detalles, ubicacionesPorProducto));

            // Esperar un momento antes de procesar el siguiente
            Platform.runLater(() -> {
                if (!pendientes.isEmpty()) {
                    PauseTransition pause = new PauseTransition(Duration.millis(500));
                    pause.setOnFinished(e -> {
                        procesarSiguienteUbicacion(pendientes,
                                new ArrayList<>(), // Pasamos lista vacía ya que no la usamos más
                                nuevoEstadoEntrada,
                                nuevoEstadoArticulos);
                    });
                    pause.play();
                }
            });
        } else {
            Platform.runLater(() -> {
                mostrarAlerta(Alert.AlertType.ERROR, "Error",
                        "No se pudo actualizar el traspaso: " + resultado.getMensaje());

                // Aún así, intentar con el siguiente si hay
                if (!pendientes.isEmpty()) {
                    PauseTransition pause = new PauseTransition(Duration.millis(500));
                    pause.setOnFinished(e -> {
                        procesarSiguienteUbicacion(pendientes,
                                new ArrayList<>(),
                                nuevoEstadoEntrada,
                                nuevoEstadoArticulos);
                    });
                    pause.play();
                }
            });
        }
    }

    private void actualizarTablaDespuesDeUbicaciones() {
        // Refrescar la tabla eliminando las entradas que ya fueron procesadas
        List<traspasoEntrada> entradasActuales = new ArrayList<>(entradasTraspaso);

        for (traspasoEntrada entrada : entradasActuales) {
            if (entrada.isSeleccionado() &&
                    !esFilaDetalle(entrada) &&
                    !esEncabezadoDetalle(entrada)) {
                // Remover esta entrada de la tabla
                entradasTraspaso.remove(entrada);
            }
        }

        // También limpiar selección del checkbox
        miCheckBox.setSelected(false);
    }

    private void mostrarConfirmacionReporte(String claveEntrada,
                                            String mensaje,
                                            List<model.DetalleEntrada> detalles,
                                            java.util.Map<String, List<UbicacionCompra>> ubicacionesPorProducto) {

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
                // PASAR EL COMENTARIO AL REPORTE - MODIFICADO
                ReporteTraspasoExporter.exportarReporte(
                        claveEntrada,
                        detalles,
                        ubicacionesPorProducto,
                        contenidoTabla != null && contenidoTabla.getScene() != null ? contenidoTabla.getScene().getWindow() : null,
                        comentario  // NUEVO PARÁMETRO
                );
            }
        });
    }

    private void cargarImagenesFlecha() {
        try {
            // Cargar imagen de flecha derecha
            flechaDerechaImage = new Image(getClass().getResourceAsStream("/img/flecha-derecha.png"));

            // Cargar imagen de flecha abajo
            flechaAbajoImage = new Image(getClass().getResourceAsStream("/img/flecha-abajo.png"));

            // Si no existe la imagen de flecha abajo, puedes usar la de derecha rotada
            if (flechaAbajoImage.isError()) {
                System.err.println("No se pudo cargar la imagen de flecha abajo");
                // Podrías crear una imagen por defecto o usar la misma
                flechaAbajoImage = flechaDerechaImage;
            }

        } catch (Exception e) {
            System.err.println("Error al cargar las imágenes de flecha: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void configurarTabla() {
        contenidoTabla.setSortPolicy(param -> {return false; });
        // Configurar la columna de la flecha primero
        configurarColumnaFlecha();

        // Configurar la columna de selección
        colSelect.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<traspasoEntrada, Boolean>, ObservableValue<Boolean>>() {
            @Override
            public ObservableValue<Boolean> call(TableColumn.CellDataFeatures<traspasoEntrada, Boolean> param) {
                traspasoEntrada item = param.getValue();
                if (item != null && !esFilaDetalle(item) && !esEncabezadoDetalle(item)) {
                    return item.seleccionadoProperty();
                }
                return new SimpleBooleanProperty(false);
            }
        });

        colSelect.setCellFactory(new Callback<TableColumn<traspasoEntrada, Boolean>, TableCell<traspasoEntrada, Boolean>>() {
            @Override
            public TableCell<traspasoEntrada, Boolean> call(TableColumn<traspasoEntrada, Boolean> param) {
                return new CheckBoxTableCell<traspasoEntrada, Boolean>() {
                    @Override
                    public void updateItem(Boolean item, boolean empty) {
                        super.updateItem(item, empty);

                        // Si es una fila de detalle o encabezado, no mostrar checkbox
                        traspasoEntrada rowData = getTableRow() != null ? getTableRow().getItem() : null;
                        if (rowData != null && (esFilaDetalle(rowData) || esEncabezadoDetalle(rowData))) {
                            setGraphic(null);
                        }
                    }
                };
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
                if (!esFilaDetalle(entrada) && !esEncabezadoDetalle(entrada)) {
                    entrada.setSeleccionado(newVal);
                }
            }
        });

        // Configurar cell factories para las celdas (con líneas)
        configurarCellFactories();
    }

    private void configurarColumnaFlecha() {
        // Configurar la celda para mostrar la imagen de flecha
        colDesplegar.setCellFactory(new Callback<TableColumn<traspasoEntrada, Void>, TableCell<traspasoEntrada, Void>>() {
            @Override
            public TableCell<traspasoEntrada, Void> call(TableColumn<traspasoEntrada, Void> param) {
                return new TableCell<traspasoEntrada, Void>() {
                    private final ImageView imageView = new ImageView();
                    private final Button button = new Button();

                    {
                        // Configurar el ImageView
                        imageView.setFitHeight(20);
                        imageView.setFitWidth(20);
                        imageView.setPreserveRatio(true);

                        // Configurar el botón (transparente)
                        button.setGraphic(imageView);
                        button.setStyle("-fx-background-color: transparent; -fx-padding: 0;");
                        button.setOnAction(event -> {
                            traspasoEntrada item = getTableRow().getItem();
                            if (item != null && !esFilaDetalle(item) && !esEncabezadoDetalle(item)) {
                                toggleFilaDesplegada(item.getClaveEntrada());
                            }
                        });

                        // Establecer alineación centrada
                        setAlignment(Pos.CENTER);
                    }

                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);

                        if (empty || getTableRow().getItem() == null) {
                            setGraphic(null);
                        } else {
                            traspasoEntrada rowItem = getTableRow().getItem();

                            // Si es una fila de detalle o encabezado, no mostrar flecha
                            if (esFilaDetalle(rowItem) || esEncabezadoDetalle(rowItem)) {
                                setGraphic(null);
                            } else {
                                // Determinar qué imagen mostrar basado en si está desplegada
                                String clave = rowItem.getClaveEntrada();
                                if (filasDesplegadas.containsKey(clave) && filasDesplegadas.get(clave)) {
                                    imageView.setImage(flechaAbajoImage);
                                } else {
                                    imageView.setImage(flechaDerechaImage);
                                }
                                setGraphic(button);
                            }
                        }
                    }
                };
            }
        });

        // Opcional: establecer un ancho preferido para la columna
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

    private void toggleFilaDesplegada(String claveEntrada) {
        boolean estaDesplegada = filasDesplegadas.getOrDefault(claveEntrada, false);

        if (estaDesplegada) {
            // Contraer: remover las filas de detalle y encabezado
            contraerFila(claveEntrada);
        } else {
            // Expandir: agregar filas de detalle con encabezado
            List<traspasoEntrada> filasDetalle = detallesPorEntrada.get(claveEntrada);
            if (filasDetalle != null) {
                if (!filasDetalle.isEmpty()) {
                    insertarDetallesEnTabla(claveEntrada, filasDetalle);
                }
            } else {
                cargarDetallesEnSegundoPlano(claveEntrada);
            }
        }

        // Actualizar el estado
        filasDesplegadas.put(claveEntrada, !estaDesplegada);
    }

    private void cargarDetallesEnSegundoPlano(String claveEntrada) {
        if (cargasDetalle.containsKey(claveEntrada)) {
            return;
        }

        javafx.concurrent.Task<List<traspasoEntrada>> task = new javafx.concurrent.Task<>() {
            @Override
            protected List<traspasoEntrada> call() {
                List<model.DetalleEntrada> detalles = modeloTraspaso.obtenerDetallesEntrada(claveEntrada);
                return construirFilasDetalle(claveEntrada, detalles);
            }
        };

        task.setOnSucceeded(event -> {
            List<traspasoEntrada> filasDetalle = task.getValue();
            detallesPorEntrada.put(claveEntrada, filasDetalle);
            cargasDetalle.remove(claveEntrada);
            if (filasDesplegadas.getOrDefault(claveEntrada, false) && !filasDetalle.isEmpty()) {
                insertarDetallesEnTabla(claveEntrada, filasDetalle);
            }
        });

        task.setOnFailed(event -> cargasDetalle.remove(claveEntrada));

        cargasDetalle.put(claveEntrada, task);
        Thread hilo = new Thread(task);
        hilo.setDaemon(true);
        hilo.start();
    }

    private void contraerFila(String claveEntrada) {
        List<traspasoEntrada> filasDetalle = detallesPorEntrada.get(claveEntrada);
        if (filasDetalle != null && !filasDetalle.isEmpty()) {
            entradasTraspaso.removeAll(filasDetalle);
        }
    }

    private void iniciarPrecargaDetalles(List<traspasoEntrada> entradas) {
        if (precargaDetallesTask != null && precargaDetallesTask.isRunning()) {
            return;
        }
        precargaDetallesTask = new javafx.concurrent.Task<>() {
            @Override
            protected Void call() {
                int total = entradas.size();
                for (int i = 0; i < total; i++) {
                    if (isCancelled()) {
                        break;
                    }
                    traspasoEntrada entrada = entradas.get(i);
                    if (entrada == null) {
                        continue;
                    }
                    String claveEntrada = entrada.getClaveEntrada();
                    if (claveEntrada == null || claveEntrada.isBlank()) {
                        continue;
                    }
                    if (!detallesPorEntrada.containsKey(claveEntrada)) {
                        List<model.DetalleEntrada> detalles = modeloTraspaso.obtenerDetallesEntrada(claveEntrada);
                        List<traspasoEntrada> filasDetalle = construirFilasDetalle(claveEntrada, detalles);
                        detallesPorEntrada.put(claveEntrada, filasDetalle);
                    }
                }
                return null;
            }
        };

        Thread hilo = new Thread(precargaDetallesTask);
        hilo.setDaemon(true);
        hilo.start();
    }

    private List<traspasoEntrada> construirFilasDetalle(String claveEntrada, List<model.DetalleEntrada> detalles) {
        List<traspasoEntrada> filasDetalle = new ArrayList<>();
        if (detalles == null || detalles.isEmpty()) {
            return filasDetalle;
        }
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
            String identificadorInterno = claveEntrada + "_DETALLE_" + contador++;
            traspasoEntrada filaDetalle = new traspasoEntrada(
                    identificadorInterno,
                    detalle.getClaveProducto(),
                    detalle.getCantidad(),
                    detalle.getPrecioUnitario(),
                    detalle.getPrecioTotal()
            );
            filaDetalle.setNombreSucursal(detalle.getPrecioTotal());
            filaDetalle.setSeleccionado(false);
            filasDetalle.add(filaDetalle);
        }
        return filasDetalle;
    }

    private void insertarDetallesEnTabla(String claveEntrada, List<traspasoEntrada> filasDetalle) {
        int index = -1;
        for (int i = 0; i < entradasTraspaso.size(); i++) {
            traspasoEntrada entrada = entradasTraspaso.get(i);
            if (entrada != null && claveEntrada.equals(entrada.getClaveEntrada())) {
                index = i;
                break;
            }
        }
        if (index >= 0) {
            entradasTraspaso.addAll(index + 1, filasDetalle);
        }
    }


    private void aplicarOrdenamiento() {
        List<traspasoEntrada> listaOrdenada = new ArrayList<>(entradasTraspasoOriginal);
        Comparator<traspasoEntrada> comparator = null;
        Function<String, String> normalizar = valor -> valor == null ? "" : valor.toLowerCase();

        switch (criterioOrden) {
            case "fecha":
                comparator = Comparator.comparing(item -> {
                    String fecha = item.getFecha();
                    if (fecha == null || fecha.isBlank()) {
                        return "";
                    }
                    return fecha;
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
        if ("desc".equalsIgnoreCase(direccionOrden)) {
            comparator = comparator.reversed();
        }
        listaOrdenada.sort(comparator);
        actualizarTablaConOrdenamiento(listaOrdenada);
    }

    private void actualizarTablaConOrdenamiento(List<traspasoEntrada> listaOrdenada) {
        entradasTraspaso.clear();
        for (traspasoEntrada entrada : listaOrdenada) {
            entradasTraspaso.add(entrada);
            String clave = entrada.getClaveEntrada();
            if (filasDesplegadas.containsKey(clave) && filasDesplegadas.get(clave)) {
                List<traspasoEntrada> detalles = detallesPorEntrada.get(clave);
                if (detalles != null) {
                    entradasTraspaso.addAll(detalles);
                }
            }
        }
        contenidoTabla.refresh();
    }

    @FXML
    private void mostrarOrdenPopup(MouseEvent event) {
        // Lista de criterios de ordenamiento disponibles
        List<String> criterios = new ArrayList<>();
        criterios.add("id");           // Clave de entrada
        criterios.add("fecha");        // Fecha
        criterios.add("sucursal");     // Nombre de sucursal

        SelectorOrdenPopup.mostrar((Node) event.getSource(), event.getScreenX(), event.getScreenY(),
                criterios, criterioOrden, direccionOrden, seleccion -> {
                    criterioOrden = seleccion.getCriterio();
                    direccionOrden = seleccion.getDireccion();
                    aplicarOrdenamiento();
                });
    }

    private void configurarCellFactories() {
        // Solo agregamos líneas a las filas de detalle
        String colorBorde = "#4A4848"; // Color gris para las líneas

        colClaveEntrada.setCellFactory(column -> new TableCell<traspasoEntrada, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                    setStyle("");
                } else {
                    traspasoEntrada rowData = getTableRow().getItem();

                    if (esEncabezadoDetalle(rowData)) {
                        // ENCABEZADO: Muestra "Id"
                        setText("Id");
                        setStyle("-fx-background-color: #6A6767; " +
                                "-fx-text-fill: white; " +
                                "-fx-alignment: CENTER; " +
                                "-fx-font-weight: bold;");
                    } else if (esFilaDetalle(rowData)) {
                        // DETALLE: Muestra el ID del producto (que está en la propiedad fecha)
                        // Necesitamos obtener el ID del producto de otra manera
                        // Vamos a buscar en los detalles originales
                        String clavePadre = obtenerClavePadre(rowData.getClaveEntrada());
                        if (clavePadre != null) {
                            // Buscar el detalle correspondiente
                            List<traspasoEntrada> detalles = detallesPorEntrada.get(clavePadre);
                            if (detalles != null) {
                                // El primer detalle es el encabezado, así que buscamos desde el índice 1
                                for (int i = 1; i < detalles.size(); i++) {
                                    if (detalles.get(i).getClaveEntrada().equals(rowData.getClaveEntrada())) {
                                        // El ID del producto está en la propiedad fecha
                                        setText(detalles.get(i).getFecha());
                                        break;
                                    }
                                }
                            }
                        }
                        setStyle("-fx-alignment: CENTER; " +
                                "-fx-border-color: " + colorBorde + "; " +
                                "-fx-border-width: 0 1 1 1;"); // Derecha, abajo, izquierda
                    } else {
                        // FILA NORMAL
                        setText(item);
                        setStyle("-fx-alignment: CENTER;");
                    }
                }
            }
        });

        colFecha.setCellFactory(column -> new TableCell<traspasoEntrada, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                    setStyle("");
                } else {
                    traspasoEntrada rowData = getTableRow().getItem();

                    if (esEncabezadoDetalle(rowData)) {
                        // ENCABEZADO: Muestra "Producto"
                        setText("Producto");
                        setStyle("-fx-background-color: #6A6767; " +
                                "-fx-text-fill: white; " +
                                "-fx-alignment: CENTER; " +
                                "-fx-font-weight: bold;");
                    } else if (esFilaDetalle(rowData)) {
                        // DETALLE: Muestra el nombre real del producto
                        // Necesitamos obtener el nombre del producto del modelo
                        String clavePadre = obtenerClavePadre(rowData.getClaveEntrada());
                        if (clavePadre != null) {
                            // Buscar el ID del producto en los detalles
                            List<traspasoEntrada> detalles = detallesPorEntrada.get(clavePadre);
                            if (detalles != null) {
                                for (int i = 1; i < detalles.size(); i++) {
                                    if (detalles.get(i).getClaveEntrada().equals(rowData.getClaveEntrada())) {
                                        String idProducto = detalles.get(i).getFecha();
                                        // Obtener el nombre del producto del modelo
                                        String nombreProducto = modeloTraspaso.obtenerNombreProducto(idProducto);
                                        setText(nombreProducto);
                                        break;
                                    }
                                }
                            }
                        }
                        setStyle("-fx-alignment: CENTER; " +
                                "-fx-border-color: " + colorBorde + "; " +
                                "-fx-border-width: 0 1 1 0;"); // Derecha, abajo
                    } else {
                        // FILA NORMAL
                        setText(item);
                        setStyle("-fx-alignment: CENTER;");
                    }
                }
            }
        });

        colHora.setCellFactory(column -> new TableCell<traspasoEntrada, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                    setStyle("");
                } else {
                    traspasoEntrada rowData = getTableRow().getItem();

                    if (esEncabezadoDetalle(rowData)) {
                        // ENCABEZADO: Muestra "Cantidad"
                        setText("Cantidad");
                        setStyle("-fx-background-color: #6A6767; " +
                                "-fx-text-fill: white; " +
                                "-fx-alignment: CENTER; " +
                                "-fx-font-weight: bold;");
                    } else if (esFilaDetalle(rowData)) {
                        // DETALLE: Muestra cantidad
                        setText(item); // item es cantidad
                        setStyle("-fx-alignment: CENTER; " +
                                "-fx-border-color: " + colorBorde + "; " +
                                "-fx-border-width: 0 1 1 0;"); // Derecha, abajo
                    } else {
                        // FILA NORMAL
                        setText(item);
                        setStyle("-fx-alignment: CENTER;");
                    }
                }
            }
        });

        colTotal.setCellFactory(column -> new TableCell<traspasoEntrada, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                    setStyle("");
                } else {
                    traspasoEntrada rowData = getTableRow().getItem();

                    if (esEncabezadoDetalle(rowData)) {
                        // ENCABEZADO: Muestra "Precio Unitario"
                        setText("Precio Unitario");
                        setStyle("-fx-background-color: #6A6767; " +
                                "-fx-text-fill: white; " +
                                "-fx-alignment: CENTER; " +
                                "-fx-font-weight: bold;");
                    } else if (esFilaDetalle(rowData)) {
                        // DETALLE: Muestra precioUnitario
                        setText(item); // item es precioUnitario
                        setStyle("-fx-alignment: CENTER; " +
                                "-fx-border-color: " + colorBorde + "; " +
                                "-fx-border-width: 0 1 1 0;"); // Derecha, abajo
                    } else {
                        // FILA NORMAL
                        setText(item);
                        setStyle("-fx-alignment: CENTER;");
                    }
                }
            }
        });

        colNombreSucural.setCellFactory(column -> new TableCell<traspasoEntrada, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                    setStyle("");
                } else {
                    traspasoEntrada rowData = getTableRow().getItem();

                    if (esEncabezadoDetalle(rowData)) {
                        // ENCABEZADO: Muestra "Precio Total"
                        setText("Precio Total");
                        setStyle("-fx-background-color: #6A6767; " +
                                "-fx-text-fill: white; " +
                                "-fx-alignment: CENTER; " +
                                "-fx-font-weight: bold;");
                    } else if (esFilaDetalle(rowData)) {
                        // DETALLE: Muestra precioTotal
                        // Aquí necesitamos obtener el precio total original
                        String clavePadre = obtenerClavePadre(rowData.getClaveEntrada());
                        if (clavePadre != null) {
                            // Buscar el detalle correspondiente
                            List<traspasoEntrada> detalles = detallesPorEntrada.get(clavePadre);
                            if (detalles != null) {
                                for (int i = 1; i < detalles.size(); i++) {
                                    if (detalles.get(i).getClaveEntrada().equals(rowData.getClaveEntrada())) {
                                        // El precio total está en la propiedad nombreSucursal
                                        setText(detalles.get(i).getNombreSucursal());
                                        break;
                                    }
                                }
                            }
                        }
                        setStyle("-fx-alignment: CENTER; " +
                                "-fx-border-color: " + colorBorde + "; " +
                                "-fx-border-width: 0 1 1 0;"); // Derecha, abajo
                    } else {
                        // FILA NORMAL
                        setText(item);
                        setStyle("-fx-alignment: CENTER;");
                    }
                }
            }
        });
    }

    private String obtenerClavePadre(String claveDetalle) {
        if (claveDetalle == null) return null;

        // Buscar la parte antes de "_DETALLE_"
        int index = claveDetalle.indexOf("_DETALLE_");
        if (index != -1) {
            return claveDetalle.substring(0, index);
        }

        // Buscar la parte antes de "_ENCABEZADO_"
        index = claveDetalle.indexOf("_ENCABEZADO_");
        if (index != -1) {
            return claveDetalle.substring(0, index);
        }

        return null;
    }

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
        for (traspasoEntrada entrada : seleccionados) {
            claves.add(entrada.getClaveEntrada());
        }

        boolean rechazar = opcion.equalsIgnoreCase("Rechazar");
        String nuevoEstadoEntrada = rechazar ? "rechazado" : "disponible";
        String nuevoEstadoArticulos = rechazar ? "rechazado" : "disponible";

        if (rechazar) {
            // Solo rechazar (no necesita ubicaciones)
            Alert confirmacionRechazar = new Alert(Alert.AlertType.CONFIRMATION);
            confirmacionRechazar.setTitle("Confirmar eliminación");
            confirmacionRechazar.setHeaderText("¿Estás seguro de rechazar las entradas seleccionadas?");
            confirmacionRechazar.setContentText("Se eliminarán " + seleccionados.size() + " entrada(s).\nEsta acción no se puede deshacer.");

            ButtonType botonSi = new ButtonType("Sí, rechazar");
            ButtonType botonCancelar = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
            confirmacionRechazar.getButtonTypes().setAll(botonSi, botonCancelar);

            confirmacionRechazar.showAndWait().ifPresent(response -> {
                if (response == botonSi) {
                    ejecutarActualizacion(claves, nuevoEstadoEntrada, nuevoEstadoArticulos, seleccionados.size());
                }
            });
        } else {
            // Aceptar: abrir formulario para asignar ubicaciones de forma secuencial
            abrirFormularioUbicaciones(seleccionados, claves, nuevoEstadoEntrada, nuevoEstadoArticulos);
        }
    }

    private void ejecutarActualizacion(List<String> claves, String nuevoEstadoEntrada,
                                       String nuevoEstadoArticulos, int cantidad) {
        boolean actualizado = modeloTraspaso.actualizarEstadoEntradas(
                claves,
                nuevoEstadoEntrada,
                nuevoEstadoArticulos
        );

        if (actualizado) {
            // Eliminar las entradas actualizadas de la lista original
            entradasTraspasoOriginal.removeIf(e -> claves.contains(e.getClaveEntrada()));

            // Actualizar la tabla manteniendo el orden
            actualizarTablaConOrdenamiento(new ArrayList<>(entradasTraspasoOriginal));

            miCheckBox.setSelected(false);

            // MODIFICACIÓN: Mensaje específico según la acción
            String mensajeExito;
            if (nuevoEstadoEntrada.equals("rechazado")) {
                mensajeExito = "Se eliminaron " + cantidad + " entrada(s) correctamente.";
            } else {
                mensajeExito = "Se aceptaron " + cantidad + " entrada(s) correctamente.";
            }

            mostrarAlerta(Alert.AlertType.INFORMATION, "Operación exitosa", mensajeExito);
        } else {
            mostrarAlerta(Alert.AlertType.ERROR, "Error", "No se pudieron actualizar las entradas.");
        }
    }

    private List<traspasoEntrada> obtenerSeleccionados() {
        List<traspasoEntrada> seleccionados = new ArrayList<>();
        for (traspasoEntrada entrada : entradasTraspaso) {
            if (entrada.isSeleccionado() && !esFilaDetalle(entrada) && !esEncabezadoDetalle(entrada)) {
                seleccionados.add(entrada);
            }
        }
        return seleccionados;
    }

    private void mostrarAlerta(Alert.AlertType tipo, String titulo, String mensaje) {
        Alert alerta = new Alert(tipo);
        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }

}
