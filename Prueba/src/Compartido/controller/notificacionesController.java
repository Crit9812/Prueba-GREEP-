package Compartido.controller;

import Compartido.model.NotificacionService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.input.KeyCode;

public class notificacionesController {

    @FXML
    private ListView<NotificacionService.Notificacion> listaNotificaciones;

    @FXML
    private Label labelMensaje;

    private final NotificacionService notificacionService = new NotificacionService();

    @FXML
    public void initialize() {
        configurarLista();
        cargarNotificaciones();
    }

    private void configurarLista() {
        listaNotificaciones.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(NotificacionService.Notificacion item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                    return;
                }

                String descripcion = item.getDescripcion() == null ? "" : item.getDescripcion();
                String breve = descripcion.length() > 90 ? descripcion.substring(0, 90) + "..." : descripcion;
                setText(item.getId() + " - " + breve);

                if ("activo".equalsIgnoreCase(item.getEstado())) {
                    setStyle("-fx-font-weight: bold; -fx-background-color: #FFF8D6;");
                } else {
                    setStyle("");
                }
            }
        });

        listaNotificaciones.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                abrirNotificacionSeleccionada();
            }
        });

        listaNotificaciones.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                abrirNotificacionSeleccionada();
            }
        });
    }

    private void cargarNotificaciones() {
        listaNotificaciones.setItems(FXCollections.observableArrayList(notificacionService.obtenerNotificaciones()));
        labelMensaje.setText(listaNotificaciones.getItems().isEmpty() ? "No hay notificaciones registradas." : "");
    }

    private void abrirNotificacionSeleccionada() {
        NotificacionService.Notificacion notificacion = listaNotificaciones.getSelectionModel().getSelectedItem();
        if (notificacion == null) {
            labelMensaje.setText("Selecciona una notificación para abrirla.");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.INFORMATION, notificacion.getDescripcion(), ButtonType.OK);
        alert.setTitle("Detalle de notificación");
        alert.setHeaderText("Notificación " + notificacion.getId());
        alert.showAndWait();

        if (notificacionService.marcarComoLeida(notificacion.getId())) {
            cargarNotificaciones();
            labelMensaje.setText("Notificación marcada como leída.");
        }
    }
}
