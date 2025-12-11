package Consultas.producto.controller;

import Compartido.controller.encabezadoController;
import Compartido.controller.navbarController;
import Compartido.importar.importador;
import Compartido.exportar.exportador;
import Consultas.producto.model.producto;
import Consultas.producto.model.model;
import Formularios.controller.controllerNuevoProducto;
import conexion.Conexion;
import conexion.conexionFTP;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MainController {

    @FXML private StackPane root;
    @FXML private BorderPane paneNavbar;
    @FXML private VBox navbar;
    @FXML private VBox contenedor;
    @FXML private Pane overlayPane;
    @FXML private VBox contenedorTabla;
    @FXML private TableView<producto> contenidoTabla;
    @FXML private TableColumn<producto, Void> colSelect;
    @FXML private TableColumn<producto, String> colIdProducto;
    @FXML private TableColumn<producto, String> colNombre;
    @FXML private TableColumn<producto, String> colCategoria;
    @FXML private TableColumn<producto, String> colEtiqueta;
    @FXML private TableColumn<producto, String> colMarca;
    @FXML private TableColumn<producto, String> colMaterial;
    @FXML private TableColumn<producto, String> colUnidadMedida;
    @FXML private TableColumn<producto, String> colDescripcion;
    @FXML private TableColumn<producto, String> colInventarioMin;
    @FXML private TableColumn<producto, String> colImagen;

    @FXML private ImageView previewImage;
    @FXML private encabezadoController paneNavbarController;

    private final model productoModel = new model();
    private final double IMAGE_VIEW_SIZE = 180;

    // Mapas concurrentes para alta velocidad y cache
    private final Map<String, String> mapEtiquetas = new ConcurrentHashMap<>();
    private final Map<String, String> mapMarcas = new ConcurrentHashMap<>();
    private final Map<String, Image> cacheImagenes = new ConcurrentHashMap<>();

    @FXML
    public void initialize() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Compartido/view/navbar.fxml"));
            VBox navbarLoaded = loader.load();
            navbarController navbarCtrl = loader.getController();
            navbarCtrl.setOverlayPane(overlayPane);
            navbar.getChildren().setAll(navbarLoaded);
        } catch (IOException e) {
            e.printStackTrace();
        }

        paneNavbar.prefHeightProperty().bind(root.heightProperty().multiply(0.1));
        paneNavbar.prefWidthProperty().bind(root.widthProperty().multiply(0.9));
        navbar.prefWidthProperty().bind(root.widthProperty().multiply(0.15));
        navbar.prefHeightProperty().bind(root.heightProperty().multiply(0.9));
        contenedor.prefHeightProperty().bind(root.heightProperty().multiply(0.75));
        contenedorTabla.prefHeightProperty().bind(contenedor.heightProperty().multiply(0.81));
        contenidoTabla.prefHeightProperty().bind(contenedorTabla.heightProperty().multiply(0.9));

        paneNavbarController.setTitulo("Productos", "#ffffff");

        // Configuración columnas
        colIdProducto.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(String.valueOf(cellData.getValue().getIdProducto())));
        colNombre.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getNombreProducto()));
        colCategoria.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getCategoria()));
        colMaterial.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getMaterial()));
        colUnidadMedida.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getUnidadMedida()));
        colDescripcion.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getDescripcion()));
        colInventarioMin.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(String.valueOf(cellData.getValue().getInventarioMin())));
        colImagen.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getUrlImagen()));

        TableColumn[] columnas = {colIdProducto, colNombre, colCategoria, colEtiqueta, colMarca, colMaterial, colUnidadMedida, colDescripcion, colInventarioMin, colImagen};
        for (TableColumn col : columnas) col.setStyle("-fx-alignment: CENTER;");

        colSelect.setCellFactory(col -> new TableCell<>() {
            private final Button btn;
            {
                btn = new Button();
                ImageView img = new ImageView(new Image(getClass().getResourceAsStream("/img/eliminar.png")));
                img.setFitWidth(18);
                img.setFitHeight(18);
                img.setPreserveRatio(true);
                btn.setGraphic(img);
                btn.setStyle("-fx-background-color: #333; -fx-cursor: hand;");
                btn.setOnAction(e -> eliminarProducto(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });

        contenidoTabla.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> mostrarImagenProducto(newSel));

        contenidoTabla.setRowFactory(tv -> {
            TableRow<producto> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    editarProducto(row.getItem());
                }
            });
            return row;
        });

        previewImage.setFitWidth(IMAGE_VIEW_SIZE);
        previewImage.setFitHeight(IMAGE_VIEW_SIZE);
        previewImage.setPreserveRatio(true);
        previewImage.setSmooth(true);
        Rectangle clip = new Rectangle(IMAGE_VIEW_SIZE, IMAGE_VIEW_SIZE);
        previewImage.setClip(clip);
        StackPane.setAlignment(previewImage, Pos.CENTER);

        // Carga ultra rápida de productos + mapas en paralelo
        preloadDatosUltraRapido();
    }

    private void preloadDatosUltraRapido() {
        Task<Void> preloadTask = new Task<>() {
            @Override
            protected Void call() {
                cargarMapEtiquetas();
                cargarMapMarcas();
                ObservableList<producto> productos = FXCollections.observableArrayList(productoModel.obtenerProductos());
                Platform.runLater(() -> {
                    colEtiqueta.setCellValueFactory(cd -> {
                        String id = cd.getValue().getEtiqueta();
                        return new javafx.beans.property.SimpleStringProperty(mapEtiquetas.getOrDefault(id, ""));
                    });
                    colMarca.setCellValueFactory(cd -> {
                        String id = cd.getValue().getMarca();
                        return new javafx.beans.property.SimpleStringProperty(mapMarcas.getOrDefault(id, ""));
                    });
                    contenidoTabla.setItems(productos);
                });
                return null;
            }
        };
        new Thread(preloadTask).start();
    }

    private void cargarMapEtiquetas() {
        try (Connection con = new Conexion().conectar();
             PreparedStatement ps = con.prepareStatement("SELECT id, nombre FROM etiquetas");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                mapEtiquetas.put(rs.getString("id"), rs.getString("nombre"));
            }
        } catch (Exception e) {
            System.out.println("Error al cargar etiquetas: " + e.getMessage());
        }
    }

    private void cargarMapMarcas() {
        try (Connection con = new Conexion().conectar();
             PreparedStatement ps = con.prepareStatement("SELECT id, nombre FROM marcas");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                mapMarcas.put(rs.getString("id"), rs.getString("nombre"));
            }
        } catch (Exception e) {
            System.out.println("Error al cargar marcas: " + e.getMessage());
        }
    }

    private void eliminarProducto(producto p) {
        Alert alerta = new Alert(Alert.AlertType.CONFIRMATION);
        alerta.setTitle("Confirmar eliminación");
        alerta.setHeaderText(null);
        alerta.setContentText("¿Está seguro que desea eliminar este producto?");
        alerta.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                if (productoModel.eliminarProducto(p.getIdProducto())) {
                    contenidoTabla.getItems().remove(p);
                    new Alert(Alert.AlertType.INFORMATION, "Producto eliminado correctamente").showAndWait();
                } else {
                    new Alert(Alert.AlertType.ERROR, "No se pudo eliminar el producto.").showAndWait();
                }
            }
        });
    }

    private void mostrarImagenProducto(producto p) {
        previewImage.setImage(null);
        if (p == null || p.getUrlImagen() == null || p.getUrlImagen().isEmpty()) return;

        if (cacheImagenes.containsKey(p.getUrlImagen())) {
            previewImage.setImage(cacheImagenes.get(p.getUrlImagen()));
            return;
        }

        Task<Image> task = new Task<>() {
            @Override
            protected Image call() throws Exception {
                conexionFTP ftp = new conexionFTP();
                return ftp.getImageFromFTP(p.getUrlImagen());
            }
        };
        task.setOnSucceeded(e -> {
            Image img = task.getValue();
            cacheImagenes.put(p.getUrlImagen(), img);
            previewImage.setImage(img);
        });
        task.setOnFailed(e -> previewImage.setImage(null));
        new Thread(task).start();
    }

    private void editarProducto(producto p) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Formularios/view/nuevoProducto.fxml"));
            Parent root = loader.load();
            controllerNuevoProducto ctrl = loader.getController();
            ctrl.cargarProducto(p);
            ctrl.setOnSaved(this::preloadDatosUltraRapido);
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Editar producto");
            stage.setScene(new Scene(root));
            stage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void formularioNuevoProducto() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Formularios/view/nuevoProducto.fxml"));
            Parent root = loader.load();
            controllerNuevoProducto ctrl = loader.getController();
            ctrl.setOnSaved(this::preloadDatosUltraRapido);
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Nuevo producto");
            stage.setScene(new Scene(root));
            stage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void exportarDatos() {
        if (contenidoTabla.getItems().isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "No hay datos para exportar.").showAndWait();
            return;
        }

        Alert dialogo = new Alert(Alert.AlertType.CONFIRMATION);
        dialogo.setTitle("Exportar");
        dialogo.setHeaderText("Seleccione el formato:");
        ButtonType btnPDF = new ButtonType("PDF");
        ButtonType btnExcel = new ButtonType("Excel (.xlsx)");
        ButtonType btnCancelar = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialogo.getButtonTypes().setAll(btnPDF, btnExcel, btnCancelar);

        dialogo.showAndWait().ifPresent(res -> {
            if (res == btnPDF) exportador.exportarTabla(contenidoTabla, "Productos", "pdf");
            else if (res == btnExcel) exportador.exportarTabla(contenidoTabla, "Productos", "excel");
        });
    }

    @FXML
    private void importarDatos() {
        importador.importarExcel("productos", "id");
        preloadDatosUltraRapido();
    }
}
