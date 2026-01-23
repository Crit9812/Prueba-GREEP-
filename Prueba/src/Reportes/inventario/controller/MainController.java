package Reportes.inventario.controller;

import Compartido.controller.encabezadoController;
import Compartido.controller.navbarController;
import Compartido.exportar.exportador;
import Compartido.helper.SelectorColumnasPopup;
import Compartido.helper.SelectorOrdenPopup;
import Reportes.inventario.model.ItemInventario;
import conexion.Conexion;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MainController {

    @FXML private StackPane root;
    @FXML private BorderPane paneNavbar;
    @FXML private VBox navbar;
    @FXML private VBox contenedor;
    @FXML private Pane overlayPane;

    @FXML private Label lblQuitar;
    @FXML private Label lblOrdenar;
    @FXML private Label lblExportar;
    @FXML private Region expansorDetalles;

    @FXML private Region expansor;
    @FXML private Label lblVista;
    @FXML private Label lblDescargar;
    @FXML private javafx.scene.control.CheckBox chkInventarioDetallado;
    @FXML private ComboBox<String> comboFiltro;
    @FXML private ComboBox<String> comboValor;
    @FXML private HBox contenedorFiltros;

    @FXML private VBox contenedorTabla;
    @FXML private TableView<ItemInventario> contenidoTabla;
    @FXML private TableColumn<ItemInventario, String> colClaveProducto;
    @FXML private TableColumn<ItemInventario, String> colCantidad;
    @FXML private TableColumn<ItemInventario, String> colProducto;
    @FXML private TableColumn<ItemInventario, String> colMarca;
    @FXML private TableColumn<ItemInventario, String> colCategoria;
    @FXML private TableColumn<ItemInventario, String> colMaterial;
    @FXML private TableColumn<ItemInventario, String> colUnidad;
    @FXML private TableColumn<ItemInventario, String> colPresentacion;
    @FXML private TableColumn<ItemInventario, String> colFactor;
    @FXML private TableColumn<ItemInventario, String> colLote;
    @FXML private TableColumn<ItemInventario, String> colCaducidad;
    @FXML private TableColumn<ItemInventario, String> colUbicacion;
    @FXML private TableColumn<ItemInventario, String> colDescripcion;
    @FXML private TableColumn<ItemInventario, String> colInventarioMinimo;

    @FXML private encabezadoController paneNavbarController;

    private final ObservableList<ItemInventario> itemsInventario = FXCollections.observableArrayList();
    private final ObservableList<ItemInventario> itemsInventarioOriginal = FXCollections.observableArrayList();
    private final Map<TableColumn<ItemInventario, ?>, Boolean> visibilidadResumen = new HashMap<>();
    private final Map<TableColumn<ItemInventario, ?>, Boolean> visibilidadDetallado = new HashMap<>();
    private String criterioOrden = "id";
    private String direccionOrden = "asc";
    private final List<Filtro> filtrosActivos = new ArrayList<>();
    private boolean restaurandoFiltros = false;


    @FXML
    public void initialize() {
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
            lblQuitar.setMinWidth(Region.USE_PREF_SIZE);
            lblOrdenar.setMinWidth(Region.USE_PREF_SIZE);
            lblExportar.setMinWidth(Region.USE_PREF_SIZE);
            HBox.setHgrow(expansorDetalles, Priority.ALWAYS);
            expansorDetalles.setMinWidth(10);
            lblVista.setMinWidth(Region.USE_PREF_SIZE);
            lblDescargar.setMinWidth(Region.USE_PREF_SIZE);

            // Tabla
            contenedorTabla.prefHeightProperty().bind(contenedor.heightProperty().multiply(0.81));
            contenidoTabla.prefHeightProperty().bind(contenedorTabla.heightProperty().multiply(0.9));

            paneNavbarController.setTitulo("Inventario", "#ffffff");

            contenedorTabla.widthProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal.doubleValue() > 0) {
                    // Pequeño delay para asegurar que todo esté estable
                    Platform.runLater(() -> {
                        Platform.runLater(this::actualizarPoliticaRedimensionamiento);
                    });
                }
            });

            // Escuchar cambios en el tamaño de la tabla
            contenidoTabla.widthProperty().addListener((obs, oldVal, newVal) -> {
               System.out.println("escuchando");
                if (newVal.doubleValue() > 0 && newVal.doubleValue() != oldVal.doubleValue()) {
                    Platform.runLater(this::actualizarPoliticaRedimensionamiento);
                }
            });

            configurarColumnasTabla();
            configurarInventarioDetallado();
            configurarFiltros();
            cargarInventarioDisponible(false);
        });
    }

    private void configurarColumnasTabla() {
        colClaveProducto.setCellValueFactory(new PropertyValueFactory<>("claveProducto"));
        colCantidad.setCellValueFactory(new PropertyValueFactory<>("cantidad"));
        colProducto.setCellValueFactory(new PropertyValueFactory<>("producto"));
        colMarca.setCellValueFactory(new PropertyValueFactory<>("marca"));
        colCategoria.setCellValueFactory(new PropertyValueFactory<>("categoria"));
        colMaterial.setCellValueFactory(new PropertyValueFactory<>("material"));
        colUnidad.setCellValueFactory(new PropertyValueFactory<>("unidadMedida"));
        colPresentacion.setCellValueFactory(new PropertyValueFactory<>("presentacion"));
        colFactor.setCellValueFactory(new PropertyValueFactory<>("factor"));
        colLote.setCellValueFactory(new PropertyValueFactory<>("lote"));
        colCaducidad.setCellValueFactory(new PropertyValueFactory<>("caducidad"));
        colUbicacion.setCellValueFactory(new PropertyValueFactory<>("ubicacion"));
        colDescripcion.setCellValueFactory(new PropertyValueFactory<>("descripcion"));
        colInventarioMinimo.setCellValueFactory(new PropertyValueFactory<>("inventarioMinimo"));

        TableColumn<ItemInventario, ?>[] columnas = new TableColumn[] {
                colClaveProducto,
                colCantidad,
                colProducto,
                colMarca,
                colCategoria,
                colMaterial,
                colUnidad,
                colPresentacion,
                colFactor,
                colLote,
                colCaducidad,
                colUbicacion,
                colDescripcion,
                colInventarioMinimo
        };
        for (TableColumn<ItemInventario, ?> col : columnas) {
            col.setStyle("-fx-alignment: CENTER;");
        }

        contenidoTabla.setItems(itemsInventario);
    }

    private void actualizarPoliticaRedimensionamiento() {
        List<TableColumn<ItemInventario, ?>> columnasVisibles = contenidoTabla.getColumns().stream()
                .filter(TableColumn::isVisible)
                .collect(Collectors.toList());

        Platform.runLater(() -> {
            double anchoDisponible = contenidoTabla.getWidth();
            if (anchoDisponible <= 0) {
                anchoDisponible = Math.max(100, contenedorTabla.getWidth() );
            }

            double minWidthTotal = columnasVisibles.stream()
                    .mapToDouble(TableColumn::getMinWidth)
                    .sum();

            // Margen del 5% para evitar problemas de redondeo
            boolean columnasCaben = minWidthTotal <= (anchoDisponible * 1.05);

            if (columnasCaben) {
                contenidoTabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
                // Resetear para distribución equitativa
                for (TableColumn<ItemInventario, ?> col : columnasVisibles) {
                    col.setPrefWidth(-1);
                }
            } else {
                contenidoTabla.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);

                // Intentar hacer ajustes inteligentes
                if (minWidthTotal > anchoDisponible * 1.2) { // Si excede en más del 20%
                    double factor = (anchoDisponible * 0.9) / minWidthTotal;
                    for (TableColumn<ItemInventario, ?> col : columnasVisibles) {
                        col.setPrefWidth(col.getMinWidth() * factor);
                    }
                }
            }

            contenidoTabla.requestLayout();
        });
    }

    private void configurarInventarioDetallado() {
        aplicarVisibilidadModo(chkInventarioDetallado.isSelected());
        chkInventarioDetallado.selectedProperty().addListener((obs, oldVal, newVal) -> {
            guardarVisibilidadModo(oldVal);
            aplicarVisibilidadModo(newVal);
            cargarInventarioDisponible(newVal);
            actualizarOpcionesFiltro(newVal);
        });
    }

    private void guardarVisibilidadModo(boolean detallado) {
        Map<TableColumn<ItemInventario, ?>, Boolean> destino =
                detallado ? visibilidadDetallado : visibilidadResumen;
        for (TableColumn<ItemInventario, ?> columna : obtenerColumnasModo(detallado)) {
            destino.put(columna, columna.isVisible());
        }
    }

    private void aplicarVisibilidadModo(boolean detallado) {
        for (TableColumn<ItemInventario, ?> columna : obtenerColumnasModo(!detallado)) {
            columna.setVisible(false);
        }

        Map<TableColumn<ItemInventario, ?>, Boolean> estado =
                detallado ? visibilidadDetallado : visibilidadResumen;
        for (TableColumn<ItemInventario, ?> columna : obtenerColumnasModo(detallado)) {
            columna.setVisible(estado.getOrDefault(columna, true));
        }
    }


    private List<TableColumn<ItemInventario, ?>> obtenerColumnasModo(boolean detallado) {
        List<TableColumn<ItemInventario, ?>> columnas = new ArrayList<>();
        columnas.add(colClaveProducto);
        if (!detallado) {
            columnas.add(colCantidad);
        }
        columnas.add(colProducto);
        columnas.add(colMarca);
        columnas.add(colCategoria);
        columnas.add(colMaterial);
        columnas.add(colUnidad);
        columnas.add(colPresentacion);
        columnas.add(colFactor);
        if (detallado) {
            columnas.add(colLote);
            columnas.add(colCaducidad);
            columnas.add(colUbicacion);
        }
        columnas.add(colDescripcion);
        columnas.add(colInventarioMinimo);
        return columnas;
    }

    @FXML
    private void mostrarSelectorColumnas(MouseEvent event) {
        boolean detallado = chkInventarioDetallado.isSelected();
        List<TableColumn<ItemInventario, ?>> columnas = obtenerColumnasModo(detallado);
        Map<TableColumn<ItemInventario, ?>, Boolean> estado =
                detallado ? visibilidadDetallado : visibilidadResumen;

        SelectorColumnasPopup.mostrar((Node) event.getSource(), event.getScreenX(), event.getScreenY(),
                columnas, seleccion -> {
                    for (Map.Entry<TableColumn<ItemInventario, ?>, Boolean> entry : seleccion.entrySet()) {
                        entry.getKey().setVisible(entry.getValue());
                    }
                    estado.putAll(seleccion);
                });
    }

    private void configurarFiltros() {
        actualizarOpcionesFiltro(chkInventarioDetallado.isSelected());
        comboFiltro.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (restaurandoFiltros) return;
            actualizarValoresFiltro(newVal);
        });
    }

    private void actualizarOpcionesFiltro(boolean detallado) {

        String campoSeleccionado = comboFiltro.getValue();
        String valorSeleccionado = comboValor.getValue();

        restaurandoFiltros = true;

        List<String> opciones = new ArrayList<>();
        opciones.add("ID");
        opciones.add("Producto");
        opciones.add("Marca");
        opciones.add("Categoría");
        opciones.add("Material");
        opciones.add("Unidad");
        opciones.add("Presentación");
        if (detallado) {
            opciones.add("Lote");
            opciones.add("Caducidad");
            opciones.add("Ubicación");
        }

        comboFiltro.getItems().setAll(opciones);

        if (campoSeleccionado != null && opciones.contains(campoSeleccionado)) {
            comboFiltro.setValue(campoSeleccionado);

            actualizarValoresFiltro(campoSeleccionado);

            if (valorSeleccionado != null &&
                    comboValor.getItems().contains(valorSeleccionado)) {
                comboValor.setValue(valorSeleccionado);
            }
        } else {
            comboValor.getItems().clear();
            comboValor.setValue(null);
        }

        restaurandoFiltros = false;

        limpiarFiltrosNoDisponibles(new LinkedHashSet<>(opciones));
    }

    private void actualizarValoresFiltro(String campo) {

        comboValor.getItems().clear();

        if (!restaurandoFiltros) {
            comboValor.setValue(null);
        }

        if (campo == null || campo.isBlank()) {
            return;
        }

        Set<String> valores = new LinkedHashSet<>();
        for (ItemInventario item : itemsInventarioOriginal) {
            String valor = obtenerValorCampo(item, campo);
            if (valor != null && !valor.isBlank()) {
                valores.add(valor);
            }
        }

        comboValor.getItems().setAll(valores);
    }


    @FXML
    private void agregarFiltro() {
        String campo = comboFiltro.getValue();
        String valor = comboValor.getValue();
        if (campo == null || valor == null) {
            mostrarAdvertencia(
                    "Filtro incompleto",
                    "Debes seleccionar un valor para el campo \"" + campo + "\"."
            );
            return;
        }
        if (filtrosActivos.size() >= 3) {
            mostrarAdvertencia(
                    "Límite de filtros",
                    "Solo puedes aplicar hasta 3 filtros al mismo tiempo.\n" +
                            "Elimina uno para agregar otro."
            );
            return;
        }
        for (Filtro filtro : filtrosActivos) {
            if (filtro.campo.equals(campo)) {
                mostrarAdvertencia(
                        "Filtro duplicado",
                        "Ya existe un filtro aplicado para el campo \"" + campo + "\".\n" +
                                "Elimina el filtro actual si deseas cambiar su valor."
                );
                return;
            }
        }
        Filtro filtro = new Filtro(campo, valor);
        filtrosActivos.add(filtro);
        contenedorFiltros.getChildren().add(crearChipFiltro(filtro));
        aplicarFiltros();
    }

    private Node crearChipFiltro(Filtro filtro) {
        HBox chip = new HBox(5); // spacing igual que en FXML
        chip.setAlignment(javafx.geometry.Pos.CENTER);
        chip.getStyleClass().add("chip"); // Aquí aplicas la clase CSS

        Label texto = new Label(filtro.campo + ": " + filtro.valor);
        texto.getStyleClass().add("chip-text"); // Si tienes clase específica para texto

        Button quitar = new Button("✕");
        quitar.getStyleClass().add("chip-close"); // La misma clase que en FXML

        quitar.setOnAction(event -> {
            filtrosActivos.remove(filtro);
            contenedorFiltros.getChildren().remove(chip);
            aplicarFiltros();
        });

        chip.getChildren().addAll(texto, quitar);
        return chip;
    }

    private void limpiarFiltrosNoDisponibles(Set<String> opcionesValidas) {
        List<Filtro> filtrosRemover = new ArrayList<>();
        for (Filtro filtro : filtrosActivos) {
            if (!opcionesValidas.contains(filtro.campo)) {
                filtrosRemover.add(filtro);
            }
        }
        for (Filtro filtro : filtrosRemover) {
            filtrosActivos.remove(filtro);
            contenedorFiltros.getChildren().removeIf(node ->
                    node instanceof HBox && ((HBox) node).getChildren().stream()
                            .anyMatch(child -> child instanceof Label &&
                                    ((Label) child).getText().startsWith(filtro.campo + ":")));
        }
        aplicarFiltros();
    }

    private void aplicarFiltros() {
        List<ItemInventario> filtrados = new ArrayList<>();
        for (ItemInventario item : itemsInventarioOriginal) {
            boolean coincide = true;
            for (Filtro filtro : filtrosActivos) {
                String valor = obtenerValorCampo(item, filtro.campo);
                if (valor == null || !valor.equals(filtro.valor)) {
                    coincide = false;
                    break;
                }
            }
            if (coincide) {
                filtrados.add(item);
            }
        }
        itemsInventario.setAll(filtrados);
        aplicarOrdenamiento();
    }

    private String obtenerValorCampo(ItemInventario item, String campo) {
        switch (campo) {
            case "ID":
                return item.getClaveProducto();
            case "Producto":
                return item.getProducto();
            case "Marca":
                return item.getMarca();
            case "Categoría":
                return item.getCategoria();
            case "Material":
                return item.getMaterial();
            case "Unidad":
                return item.getUnidadMedida();
            case "Presentación":
                return item.getPresentacion();
            case "Lote":
                return item.getLote();
            case "Caducidad":
                return item.getCaducidad();
            case "Ubicación":
                return item.getUbicacion();
            default:
                return "";
        }
    }

    @FXML
    private void exportarExcel() {
        if (contenidoTabla.getItems().isEmpty()) {
            mostrarAdvertencia("Advertencia", "No hay datos para exportar.");
            return;
        }

        // Crear lista de filtros aplicados
        List<String> filtrosAplicados = new ArrayList<>();
        for (Filtro filtro : filtrosActivos) {
            filtrosAplicados.add(filtro.campo + ": " + filtro.valor);
        }

        exportador.exportarTabla(contenidoTabla, "Inventario", "excel", filtrosAplicados);
    }

    @FXML
    private void descargarPdf() {
        if (contenidoTabla.getItems().isEmpty()) {
            mostrarAdvertencia("Advertencia", "No hay datos para exportar.");
            return;
        }

        // Crear lista de filtros aplicados
        List<String> filtrosAplicados = new ArrayList<>();
        for (Filtro filtro : filtrosActivos) {
            filtrosAplicados.add(filtro.campo + ": " + filtro.valor);
        }

        exportador.exportarTabla(contenidoTabla, "Inventario", "pdf", filtrosAplicados);
    }

    @FXML
    private void vistaPreviaPdf() {
        if (contenidoTabla.getItems().isEmpty()) {
            mostrarAdvertencia("Advertencia", "No hay datos para exportar.");
            return;
        }

        // Crear lista de filtros aplicados
        List<String> filtrosAplicados = new ArrayList<>();
        for (Filtro filtro : filtrosActivos) {
            filtrosAplicados.add(filtro.campo + ": " + filtro.valor);
        }

        exportador.previsualizarPDF(contenidoTabla, "Inventario", filtrosAplicados);
    }

    private void mostrarAdvertencia(String titulo, String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.WARNING);
        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }

    @FXML
    private void mostrarOrdenPopup(MouseEvent event) {
        boolean detallado = chkInventarioDetallado.isSelected();
        List<String> criterios = new ArrayList<>();
        criterios.add("id");
        if (!detallado) {
            criterios.add("cantidad");
        }
        criterios.add("producto");

        if (detallado) {
            criterios.add("ubicacion");
        }

        SelectorOrdenPopup.mostrar((Node) event.getSource(), event.getScreenX(), event.getScreenY(),
                criterios, criterioOrden, direccionOrden, seleccion -> {
                    criterioOrden = seleccion.getCriterio();
                    direccionOrden = seleccion.getDireccion();
                    aplicarOrdenamiento();
                });
    }

    private void aplicarOrdenamiento() {
        Comparator<ItemInventario> comparator = null;
        Function<String, String> normalizar = valor -> valor == null ? "" : valor.toLowerCase();

        switch (criterioOrden) {
            case "cantidad":
                comparator = Comparator.comparingInt(item -> {
                    String valor = item.getCantidad();
                    if (valor == null || valor.isBlank()) {
                        return 0;
                    }
                    try {
                        return Integer.parseInt(valor);
                    } catch (NumberFormatException e) {
                        return 0;
                    }
                });
                break;
            case "producto":
                comparator = Comparator.comparing(item -> normalizar.apply(item.getProducto()));
                break;
            case "ubicacion":
                comparator = Comparator.comparing(item -> normalizar.apply(item.getUbicacion()));
                break;
            case "id":
            default:
                comparator = Comparator.comparing(item -> normalizar.apply(item.getClaveProducto()));
                break;
        }
        if ("desc".equalsIgnoreCase(direccionOrden)) {
            comparator = comparator.reversed();
        }

        FXCollections.sort(itemsInventario, comparator);
    }

    private void cargarInventarioDisponible(boolean detallado) {

        String sql = detallado ? """
            SELECT
                p.id AS claveProducto,
                p.nombre AS producto,
                m.nombre AS marca,
                p.categoria AS categoria,
                p.material AS material,
                p.unidadMedida AS unidadMedida,
                a.presentacion AS presentacion,
                a.factor AS factor,
                a.lote AS lote,
                a.caducidad AS caducidad,
                u.nombre AS ubicacion,
                p.descripcion AS descripcion,
                p.inventarioMin AS inventarioMinimo
            FROM articulo a
            INNER JOIN detalle_Entrada de ON a.idDetalleEntrada = de.idDetalleEntrada
            INNER JOIN productos p ON de.claveProducto = p.id
            LEFT JOIN marcas m ON p.marca = m.id
            LEFT JOIN ubicaciones u ON a.ubicacion = u.id
            WHERE a.Estado = 'disponible'
            """ : """
            SELECT
                p.id AS claveProducto,
                COUNT(a.idArticulo) AS cantidad,
                p.nombre AS producto,
                m.nombre AS marca,
                p.categoria AS categoria,
                p.material AS material,
                p.unidadMedida AS unidadMedida,
                a.presentacion AS presentacion,
                a.factor AS factor,
                p.descripcion AS descripcion,
                p.inventarioMin AS inventarioMinimo
            FROM articulo a
            INNER JOIN detalle_Entrada de ON a.idDetalleEntrada = de.idDetalleEntrada
            INNER JOIN productos p ON de.claveProducto = p.id
            LEFT JOIN marcas m ON p.marca = m.id
            WHERE a.Estado = 'disponible'
            GROUP BY
                p.id,
                p.nombre,
                m.nombre,
                p.categoria,
                p.material,
                p.unidadMedida,
                a.presentacion,
                a.factor,
                p.descripcion,
                p.inventarioMin
            """;

        // 1️⃣ Guardar selección actual de filtros
        String campoActual = comboFiltro.getValue();
        String valorActual = comboValor.getValue();

        itemsInventarioOriginal.clear();

        try (Connection conn = new Conexion().conectar();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                itemsInventarioOriginal.add(new ItemInventario(
                        rs.getString("claveProducto"),
                        detallado ? "" : rs.getString("cantidad"),
                        rs.getString("producto"),
                        rs.getString("marca"),
                        rs.getString("categoria"),
                        rs.getString("material"),
                        rs.getString("unidadMedida"),
                        rs.getString("presentacion"),
                        rs.getString("factor"),
                        detallado ? rs.getString("lote") : "",
                        detallado ? rs.getString("caducidad") : "",
                        detallado ? rs.getString("ubicacion") : "",
                        rs.getString("descripcion"),
                        rs.getString("inventarioMinimo")
                ));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        restaurandoFiltros = true;

        actualizarValoresFiltro(campoActual);

        if (valorActual != null &&
                comboValor.getItems().contains(valorActual)) {
            comboValor.setValue(valorActual);
        }

        restaurandoFiltros = false;

        // 3️⃣ Aplicar filtros activos
        aplicarFiltros();
    }

    private static class Filtro {
        private final String campo;
        private final String valor;

        private Filtro(String campo, String valor) {
            this.campo = campo;
            this.valor = valor;
        }
    }
}
