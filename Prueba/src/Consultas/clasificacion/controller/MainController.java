package Consultas.clasificacion.controller;

import Compartido.controller.encabezadoController;
import Compartido.controller.navbarController;
import Consultas.clasificacion.model.marcas;
import Consultas.clasificacion.model.etiquetas;
import Consultas.clasificacion.model.model;
import Consultas.clasificacion.model.ubicaciones;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;

import java.io.IOException;

public class MainController {

    @FXML private StackPane root;
    @FXML private BorderPane paneNavbar;
    @FXML private VBox navbar;
    @FXML private VBox contenedor;
    @FXML private Pane overlayPane;
    @FXML private VBox contenedorTabla;
    @FXML private Region expansor;
    @FXML private TextField buscador;

    @FXML private TableView<marcas> contenidoTablaMarcas;
    @FXML private TableColumn<marcas, Void> colSelect;
    @FXML private TableColumn<marcas, Integer> colIDMarca;
    @FXML private TableColumn<marcas, String> colNombreMarca;

    @FXML private TableView<etiquetas> contenidoTablaEtiquetas;
    @FXML private TableColumn<etiquetas, Void> colSelectEtiqueta;
    @FXML private TableColumn<etiquetas, Integer> colIDEtiqueta;
    @FXML private TableColumn<etiquetas, String> colNombreEtiqueta;

    @FXML private TableView<ubicaciones> contenidoTablaUbicaciones;
    @FXML private TableColumn<ubicaciones, Void> colSelectUbicaciones;
    @FXML private TableColumn<ubicaciones, Integer> colIDUbicaciones;
    @FXML private TableColumn<ubicaciones, String> colNombreUbicaciones;


    @FXML private encabezadoController paneNavbarController;

    private final model model = new model();

    @FXML
    public void initialize() {

        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/Compartido/view/navbar.fxml")
                );
                VBox navbarLoaded = loader.load();
                navbarController navbarCtrl = loader.getController();
                navbarCtrl.setOverlayPane(overlayPane);
                navbar.getChildren().setAll(navbarLoaded);
            } catch (IOException e) {
                e.printStackTrace();
            }

            paneNavbarController.setTitulo("Marcas y Etiquetas", "#ffffff");

            configurarTablas();
            configurarDobleClick();
            cargarDatos();
        });
    }

    private void configurarTablas() {

        colIDMarca.setCellValueFactory(c -> c.getValue().idProperty().asObject());
        colNombreMarca.setCellValueFactory(c -> c.getValue().nombreProperty());
        colSelect.setCellFactory(col -> crearBotonEliminarMarca());
        colIDMarca.setStyle("-fx-alignment: CENTER;");

        colIDEtiqueta.setCellValueFactory(c -> c.getValue().idProperty().asObject());
        colNombreEtiqueta.setCellValueFactory(c -> c.getValue().nombreProperty());
        colSelectEtiqueta.setCellFactory(col -> crearBotonEliminarEtiqueta());
        colIDEtiqueta.setStyle("-fx-alignment: CENTER;");

        colIDUbicaciones.setCellValueFactory(c -> c.getValue().idProperty().asObject());
        colNombreUbicaciones.setCellValueFactory(c -> c.getValue().nombreProperty());
        colSelectUbicaciones.setCellFactory(col -> crearBotonEliminarUbicacion());
        colIDUbicaciones.setStyle("-fx-alignment: CENTER;");

    }

    private void configurarDobleClick() {

        contenidoTablaMarcas.setRowFactory(tv -> {
            TableRow<marcas> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) {
                    editarMarca(row.getItem());
                }
            });
            return row;
        });

        contenidoTablaEtiquetas.setRowFactory(tv -> {
            TableRow<etiquetas> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) {
                    editarEtiqueta(row.getItem());
                }
            });
            return row;
        });

        contenidoTablaUbicaciones.setRowFactory(tv -> {
            TableRow<ubicaciones> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) {
                    editarUbicacion(row.getItem());
                }
            });
            return row;
        });

    }

    private void editarMarca(marcas m) {
        TextInputDialog dialog = new TextInputDialog(m.getNombre());
        dialog.setTitle("Editar marca");
        dialog.setHeaderText(null);
        dialog.setContentText("Nombre de la marca:");

        dialog.showAndWait().ifPresent(nombre -> {
            if (!nombre.trim().isEmpty()) {
                model.actualizarMarca(m.getId(), nombre.trim());
                m.setNombre(nombre.trim());
                contenidoTablaMarcas.refresh();
            }
        });
    }

    private void editarEtiqueta(etiquetas e) {
        TextInputDialog dialog = new TextInputDialog(e.getNombre());
        dialog.setTitle("Editar etiqueta");
        dialog.setHeaderText(null);
        dialog.setContentText("Nombre de la etiqueta:");

        dialog.showAndWait().ifPresent(nombre -> {
            if (!nombre.trim().isEmpty()) {
                model.actualizarEtiqueta(e.getId(), nombre.trim());
                e.setNombre(nombre.trim());
                contenidoTablaEtiquetas.refresh();
            }
        });
    }

    // ================= ELIMINAR CON OPCIÓN =================

    private TableCell<marcas, Void> crearBotonEliminarMarca() {
        return new TableCell<>() {

            private final Button btn;

            {
                btn = new Button();
                ImageView img = new ImageView(
                        new Image(getClass().getResourceAsStream("/img/eliminar.png"))
                );
                img.setFitWidth(18);
                img.setFitHeight(18);
                img.setPreserveRatio(true);

                btn.setGraphic(img);
                btn.setStyle("-fx-background-color: #333; -fx-cursor: hand;");

                btn.setOnAction(e -> {
                    marcas m = getTableView().getItems().get(getIndex());

                    int vinculados = model.contarProductosPorMarca(m.getId());

                    if (vinculados > 0) {
                        if (!confirmarConVinculos("marca", m.getNombre(), vinculados)) {
                            return;
                        }
                    } else if (!confirmar("Eliminar marca", m.getNombre())) {
                        return;
                    }

                    model.eliminarMarca(m.getId());
                    contenidoTablaMarcas.getItems().remove(m);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        };
    }

    private TableCell<etiquetas, Void> crearBotonEliminarEtiqueta() {
        return new TableCell<>() {

            private final Button btn;

            {
                btn = new Button();
                ImageView img = new ImageView(
                        new Image(getClass().getResourceAsStream("/img/eliminar.png"))
                );
                img.setFitWidth(18);
                img.setFitHeight(18);
                img.setPreserveRatio(true);

                btn.setGraphic(img);
                btn.setStyle("-fx-background-color: #333; -fx-cursor: hand;");

                btn.setOnAction(e -> {
                    etiquetas et = getTableView().getItems().get(getIndex());

                    int vinculados = model.contarProductosPorEtiqueta(et.getId());

                    if (vinculados > 0) {
                        if (!confirmarConVinculos("etiqueta", et.getNombre(), vinculados)) {
                            return;
                        }
                    } else if (!confirmar("Eliminar etiqueta", et.getNombre())) {
                        return;
                    }

                    model.eliminarEtiqueta(et.getId());
                    contenidoTablaEtiquetas.getItems().remove(et);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        };
    }

    private boolean confirmar(String titulo, String nombre) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle(titulo);
        a.setHeaderText(null);
        a.setContentText("¿Desea eliminar: " + nombre + "?");
        return a.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }

    private boolean confirmarConVinculos(String tipo, String nombre, int cantidad) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle("Elemento vinculado");
        a.setHeaderText(null);
        a.setContentText(
                "La " + tipo + " \"" + nombre + "\" está vinculada a "
                        + cantidad + " producto(s).\n\n¿Desea eliminarla de todos modos?"
        );
        return a.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }

    @FXML
    private void agregarMarca() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Agregar marca");
        dialog.setHeaderText(null);
        dialog.setContentText("Nombre de la marca:");

        dialog.showAndWait().ifPresent(nombre -> {
            if (!nombre.trim().isEmpty()) {
                model.insertarMarca(nombre.trim());
                cargarDatos();
            }
        });
    }

    @FXML
    private void agregarEtiqueta() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Agregar etiqueta");
        dialog.setHeaderText(null);
        dialog.setContentText("Nombre de la etiqueta:");

        dialog.showAndWait().ifPresent(nombre -> {
            if (!nombre.trim().isEmpty()) {
                model.insertarEtiqueta(nombre.trim());
                cargarDatos();
            }
        });
    }

    private void cargarDatos() {

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                ObservableList<marcas> marcas =
                        FXCollections.observableArrayList(model.obtenerMarcas());
                ObservableList<etiquetas> etiquetas =
                        FXCollections.observableArrayList(model.obtenerEtiquetas());
                ObservableList<ubicaciones> ubicaciones =
                        FXCollections.observableArrayList(model.obtenerUbicaciones());

                Platform.runLater(() -> {
                    contenidoTablaMarcas.setItems(marcas);
                    contenidoTablaEtiquetas.setItems(etiquetas);
                    contenidoTablaUbicaciones.setItems(ubicaciones);
                });
                return null;
            }
        };
        new Thread(task).start();
    }

    private void editarUbicacion(ubicaciones u) {
        TextInputDialog dialog = new TextInputDialog(u.getNombre());
        dialog.setTitle("Editar ubicación");
        dialog.setHeaderText(null);
        dialog.setContentText("Nombre de la ubicación:");

        dialog.showAndWait().ifPresent(nombre -> {
            if (!nombre.trim().isEmpty()) {
                model.actualizarUbicacion(u.getId(), nombre.trim());
                u.setNombre(nombre.trim());
                contenidoTablaUbicaciones.refresh();
            }
        });
    }

    private TableCell<ubicaciones, Void> crearBotonEliminarUbicacion() {
        return new TableCell<>() {

            private final Button btn;

            {
                btn = new Button();
                ImageView img = new ImageView(
                        new Image(getClass().getResourceAsStream("/img/eliminar.png"))
                );
                img.setFitWidth(18);
                img.setFitHeight(18);
                img.setPreserveRatio(true);

                btn.setGraphic(img);
                btn.setStyle("-fx-background-color: #333; -fx-cursor: hand;");

                btn.setOnAction(e -> {
                    ubicaciones u = getTableView().getItems().get(getIndex());

                    int vinculados = model.contarProductosPorUbicacion(u.getId());

                    if (vinculados > 0) {
                        if (!confirmarConVinculos("ubicación", u.getNombre(), vinculados)) {
                            return;
                        }
                    } else if (!confirmar("Eliminar ubicación", u.getNombre())) {
                        return;
                    }

                    model.eliminarUbicacion(u.getId());
                    contenidoTablaUbicaciones.getItems().remove(u);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        };
    }

    @FXML
    private void agregarUbicacion() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Agregar ubicación");
        dialog.setHeaderText(null);
        dialog.setContentText("Nombre de la ubicación:");

        dialog.showAndWait().ifPresent(nombre -> {
            if (!nombre.trim().isEmpty()) {
                model.insertarUbicacion(nombre.trim());
                cargarDatos();
            }
        });
    }



}
