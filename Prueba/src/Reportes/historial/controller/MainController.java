package Reportes.historial.controller;

import Compartido.controller.encabezadoController;
import Compartido.controller.navbarController;
import Reportes.historial.model.HistorialFactura;
import conexion.Conexion;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class MainController {

    @FXML private StackPane root;
    @FXML private BorderPane paneNavbar;
    @FXML private VBox navbar;
    @FXML private Pane overlayPane;
    @FXML private VBox contenedor;

    @FXML private Label lblQuitar;
    @FXML private Label lblOrdenar;
    @FXML private Label lblExportar;
    @FXML private Region expansorBusqueda;
    @FXML private TextField buscarFactura;

    @FXML private Region expansor;
    @FXML private Label lblVista;
    @FXML private Label lblDescargar;

    @FXML private VBox contenedorTabla;
    @FXML private TableView<HistorialFactura> contenidoTabla;
    @FXML private TableColumn<HistorialFactura, String> colMovimiento;
    @FXML private TableColumn<HistorialFactura, String> colClaveMovimiento;
    @FXML private TableColumn<HistorialFactura, String> colFactura;
    @FXML private TableColumn<HistorialFactura, String> colFecha;
    @FXML private TableColumn<HistorialFactura, String> colHora;
    @FXML private TableColumn<HistorialFactura, String> colTipoMovimiento;
    @FXML private TableColumn<HistorialFactura, String> colUsuario;
    @FXML private TableColumn<HistorialFactura, String> colExterno;
    @FXML private TableColumn<HistorialFactura, String> colPrecioNeto;
    @FXML private TableColumn<HistorialFactura, String> colPrecioTotal;
    @FXML private TableColumn<HistorialFactura, String> colNota;
    @FXML private TableColumn<HistorialFactura, String> colEstado;

    @FXML private encabezadoController paneNavbarController;

    private final ObservableList<HistorialFactura> itemsHistorial = FXCollections.observableArrayList();
    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter FORMATO_FECHA_ALT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @FXML
    public void initialize() {
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

            lblQuitar.setMinWidth(Region.USE_PREF_SIZE);
            lblOrdenar.setMinWidth(Region.USE_PREF_SIZE);
            lblExportar.setMinWidth(Region.USE_PREF_SIZE);

            HBox.setHgrow(expansorBusqueda, Priority.ALWAYS);
            expansorBusqueda.setMinWidth(10);

            buscarFactura.prefWidthProperty().bind(root.widthProperty().multiply(0.18));
            buscarFactura.prefHeightProperty().bind(navbar.heightProperty().multiply(0.04));

            HBox.setHgrow(expansor, Priority.ALWAYS);
            expansor.setMinWidth(10);

            lblVista.setMinWidth(Region.USE_PREF_SIZE);
            lblDescargar.setMinWidth(Region.USE_PREF_SIZE);

            contenedorTabla.prefHeightProperty().bind(contenedor.heightProperty().multiply(0.78));
            contenidoTabla.prefHeightProperty().bind(contenedorTabla.heightProperty().multiply(0.9));

            paneNavbarController.setTitulo("Historial por factura", "#ffffff");
            configurarColumnas();
            cargarHistorial();
        });
    }

    private void configurarColumnas() {
        colMovimiento.setCellValueFactory(new PropertyValueFactory<>("movimiento"));
        colClaveMovimiento.setCellValueFactory(new PropertyValueFactory<>("claveMovimiento"));
        colFactura.setCellValueFactory(new PropertyValueFactory<>("factura"));
        colFecha.setCellValueFactory(new PropertyValueFactory<>("fecha"));
        colHora.setCellValueFactory(new PropertyValueFactory<>("hora"));
        colTipoMovimiento.setCellValueFactory(new PropertyValueFactory<>("tipoMovimiento"));
        colUsuario.setCellValueFactory(new PropertyValueFactory<>("usuario"));
        colExterno.setCellValueFactory(new PropertyValueFactory<>("externo"));
        colPrecioNeto.setCellValueFactory(new PropertyValueFactory<>("precioNeto"));
        colPrecioTotal.setCellValueFactory(new PropertyValueFactory<>("precioTotal"));
        colNota.setCellValueFactory(new PropertyValueFactory<>("nota"));
        colEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));

        TableColumn<HistorialFactura, ?>[] columnas = new TableColumn[] {
                colMovimiento,
                colClaveMovimiento,
                colFactura,
                colFecha,
                colHora,
                colTipoMovimiento,
                colUsuario,
                colExterno,
                colPrecioNeto,
                colPrecioTotal,
                colNota,
                colEstado
        };

        for (TableColumn<HistorialFactura, ?> columna : columnas) {
            columna.setStyle("-fx-alignment: CENTER;");
        }

        contenidoTabla.setItems(itemsHistorial);
    }

    private void cargarHistorial() {
        itemsHistorial.clear();
        List<HistorialFactura> registros = new ArrayList<>();

        try (Connection conn = new Conexion().conectar()) {
            if (conn == null) {
                return;
            }

            registros.addAll(obtenerEntradas(conn));
            registros.addAll(obtenerSalidas(conn));
            registros.addAll(obtenerAjustes(conn));
        } catch (SQLException e) {
            e.printStackTrace();
        }

        registros.sort(Comparator.comparing(this::obtenerFechaHoraOrden,
                Comparator.nullsLast(Comparator.reverseOrder())));
        itemsHistorial.setAll(registros);
    }

    private List<HistorialFactura> obtenerEntradas(Connection conn) throws SQLException {
        String query = "SELECT e.idEntrada, e.noFactura, e.fechaEntrada, e.horaEntrada, e.tipoEntrada, "
                + "e.claveUsuarioEntrada, e.idRemitente, e.precioNetoEntrada, e.precioTotalEntrada, e.nota, e.Estado, "
                + "TRIM(CONCAT_WS(' ', u.nombreUsuario, u.apellidoPUsuario, u.apellidoMUsuario)) AS usuarioNombre, "
                + "CASE "
                + "WHEN LOWER(e.tipoEntrada) = 'compra' THEN p.Nombre "
                + "WHEN LOWER(e.tipoEntrada) = 'traspaso' THEN s.nombre "
                + "ELSE e.idRemitente "
                + "END AS externoNombre "
                + "FROM entradas e "
                + "LEFT JOIN usuarios u ON e.claveUsuarioEntrada = u.idUsuario "
                + "LEFT JOIN proveedores p ON e.idRemitente = p.id "
                + "LEFT JOIN sucursales s ON e.idRemitente = s.id";
        List<HistorialFactura> registros = new ArrayList<>();

        try (PreparedStatement statement = conn.prepareStatement(query);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                registros.add(new HistorialFactura(
                        "Entrada",
                        String.valueOf(rs.getInt("idEntrada")),
                        valorTexto(rs.getObject("noFactura")),
                        valorTexto(rs.getObject("fechaEntrada")),
                        valorTexto(rs.getObject("horaEntrada")),
                        valorTexto(rs.getObject("tipoEntrada")),
                        valorTexto(rs.getObject("usuarioNombre")),
                        valorTexto(rs.getObject("externoNombre")),
                        valorTexto(rs.getObject("precioNetoEntrada")),
                        valorTexto(rs.getObject("precioTotalEntrada")),
                        valorTexto(rs.getObject("nota")),
                        valorTexto(rs.getObject("Estado"))
                ));
            }
        }

        return registros;
    }

    private List<HistorialFactura> obtenerSalidas(Connection conn) throws SQLException {
        String query = "SELECT s.idSalida, s.noFactura, s.fechaSalida, s.horaSalida, s.tipoSalida, "
                + "s.claveUsuarioSalida, s.idDestinatario, s.precioNetoSalida, s.precioTotalSalida, s.nota, s.Estado, "
                + "TRIM(CONCAT_WS(' ', u.nombreUsuario, u.apellidoPUsuario, u.apellidoMUsuario)) AS usuarioNombre, "
                + "CASE "
                + "WHEN LOWER(s.tipoSalida) = 'venta' THEN c.Nombre "
                + "WHEN LOWER(s.tipoSalida) = 'traspaso' THEN su.nombre "
                + "ELSE s.idDestinatario "
                + "END AS externoNombre "
                + "FROM salidas s "
                + "LEFT JOIN usuarios u ON s.claveUsuarioSalida = u.idUsuario "
                + "LEFT JOIN clientes c ON s.idDestinatario = c.id "
                + "LEFT JOIN sucursales su ON s.idDestinatario = su.id";
        List<HistorialFactura> registros = new ArrayList<>();

        try (PreparedStatement statement = conn.prepareStatement(query);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                registros.add(new HistorialFactura(
                        "Salida",
                        String.valueOf(rs.getInt("idSalida")),
                        valorTexto(rs.getObject("noFactura")),
                        valorTexto(rs.getObject("fechaSalida")),
                        valorTexto(rs.getObject("horaSalida")),
                        valorTexto(rs.getObject("tipoSalida")),
                        valorTexto(rs.getObject("usuarioNombre")),
                        valorTexto(rs.getObject("externoNombre")),
                        valorTexto(rs.getObject("precioNetoSalida")),
                        valorTexto(rs.getObject("precioTotalSalida")),
                        valorTexto(rs.getObject("nota")),
                        valorTexto(rs.getObject("Estado"))
                ));
            }
        }

        return registros;
    }

    private List<HistorialFactura> obtenerAjustes(Connection conn) throws SQLException {
        String query = "SELECT a.idAjuste, a.idUsuario, a.fechaAjuste, a.horaAjuste, a.precioNeto, a.precioTotal, a.Nota, "
                + "TRIM(CONCAT_WS(' ', u.nombreUsuario, u.apellidoPUsuario, u.apellidoMUsuario)) AS usuarioNombre "
                + "FROM ajuste_inventario a "
                + "LEFT JOIN usuarios u ON a.idUsuario = u.idUsuario";
        List<HistorialFactura> registros = new ArrayList<>();

        try (PreparedStatement statement = conn.prepareStatement(query);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                String horaAjuste = formatearHoraAjuste(rs.getObject("horaAjuste"));
                registros.add(new HistorialFactura(
                        "Ajuste",
                        String.valueOf(rs.getInt("idAjuste")),
                        "-",
                        valorTexto(rs.getObject("fechaAjuste")),
                        horaAjuste,
                        "Ajuste de inventario",
                        valorTexto(rs.getObject("usuarioNombre")),
                        "-",
                        valorTexto(rs.getObject("precioNeto")),
                        valorTexto(rs.getObject("precioTotal")),
                        valorTexto(rs.getObject("Nota")),
                        "-"
                ));
            }
        }

        return registros;
    }

    private String formatearHoraAjuste(Object valor) {
        if (valor == null) {
            return "";
        }
        if (valor instanceof Number) {
            long segundos = ((Number) valor).longValue();
            if (segundos < 0) {
                return "";
            }
            return LocalTime.ofSecondOfDay(segundos).format(FORMATO_HORA);
        }
        return valor.toString();
    }

    private String valorTexto(Object valor) {
        if (valor == null) {
            return "";
        }
        String texto = valor.toString();
        return texto.isBlank() ? "" : texto;
    }

    private LocalDateTime obtenerFechaHoraOrden(HistorialFactura item) {
        LocalDate fecha = parseFecha(item.getFecha());
        if (fecha == null) {
            return null;
        }
        LocalTime hora = parseHora(item.getHora());
        if (hora == null) {
            hora = LocalTime.MIDNIGHT;
        }
        return LocalDateTime.of(fecha, hora);
    }

    private LocalDate parseFecha(String fechaTexto) {
        if (fechaTexto == null || fechaTexto.isBlank()) {
            return null;
        }
        List<DateTimeFormatter> formatos = List.of(DateTimeFormatter.ISO_LOCAL_DATE, FORMATO_FECHA_ALT);
        for (DateTimeFormatter formatter : formatos) {
            try {
                return LocalDate.parse(fechaTexto, formatter);
            } catch (DateTimeParseException ignored) {
                // Intentar con el siguiente formato
            }
        }
        return null;
    }

    private LocalTime parseHora(String horaTexto) {
        if (horaTexto == null || horaTexto.isBlank()) {
            return null;
        }
        String texto = horaTexto.trim();
        if (texto.chars().allMatch(Character::isDigit)) {
            try {
                long segundos = Long.parseLong(texto);
                if (segundos >= 0) {
                    return LocalTime.ofSecondOfDay(segundos);
                }
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        try {
            return LocalTime.parse(texto);
        } catch (DateTimeParseException e) {
            try {
                return LocalTime.parse(texto, DateTimeFormatter.ofPattern("HH:mm"));
            } catch (DateTimeParseException ignored) {
                return null;
            }
        }
    }
}
