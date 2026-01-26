package Formularios.controller;

import Operaciones.traspasoSalida.model.traspasoSalida;
import Operaciones.traspasoSalida.controller.MainController;
import Operaciones.compra.model.UbicacionCompra;
import Formularios.model.modelNuevoTraspasoSalida;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.concurrent.Task;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

public class controllerNuevoTraspasoSalida extends FormularioSalidaController {

    // === CAMPOS ESPECÍFICOS PARA TRASPASO SALIDA ===
    @FXML private TextField txtPrecioSalida;
    @FXML private TextField txtPrecioEntradaIva;
    @FXML private TextField txtPrecioSalidaRapida;
    @FXML private TextField txtPrecioEntradaIvaRapida;
    @FXML private TextField txtPrecioIVARapida;
    @FXML private Label lblTitulo;
    @FXML private ScrollPane scrollPaneTraspaso;

    // === VARIABLES ESPECÍFICAS DE TRASPASO ===
    private ObservableList<traspasoSalida> itemsTraspaso;
    private MainController mainController;
    private traspasoSalida itemParaEditar;

    private String tituloFormulario = "Traspaso de Salida";
    private boolean modoSoloNormal = false;
    private boolean modoAjusteInventario = false;

    // === CONSTANTE ESPECÍFICA ===
    private static final BigDecimal IVA_TASA = new BigDecimal("0.16");

    @FXML
    public void initialize() {
        // Configurar título
        if (lblTitulo != null) {
            lblTitulo.setText(tituloFormulario);
        }

        // Inicialización base
        initializeBase();

        // Configuración específica de traspaso
        aplicarModoSoloNormal();

        if (itemParaEditar != null) {
            if (tabRapido != null) {
                tabRapido.setDisable(true);
                if (tabPaneModo != null && tabNormal != null) {
                    tabPaneModo.getSelectionModel().select(tabNormal);
                }
            }
            cargarItemParaEditar();
        }
    }

    // === IMPLEMENTACIÓN DE MÉTODOS ABSTRACTS ===

    @Override
    protected void guardarItem() {
        if (esModoRapido()) {
            guardarItemRapido();
            return;
        }

        String clave = productoController.getIdSeleccionado();
        String nombre = productoController.getNombreSeleccionado();
        String descripcion = txtDescripcion.getText() != null ? txtDescripcion.getText().trim() : "";
        String lote = txtLote.getText() != null ? txtLote.getText().trim() : "";
        LocalDate caducidad = dpCaducidad.getValue();
        String cantidadTexto = txtCantidad.getText() != null ? txtCantidad.getText().trim() : "";
        String nota = txtNota != null && txtNota.getText() != null ? txtNota.getText().trim() : "";
        String presentacion = cbPresentacion.getValue();
        String factorTexto = txtFactor.getText() != null ? txtFactor.getText().trim() : "";
        String precioEntrada = txtPrecioEntrada != null ? txtPrecioEntrada.getText().trim() : "";
        String precioEntradaIva = txtPrecioEntradaIva != null ? txtPrecioEntradaIva.getText().trim() : "";
        String precioSalida = txtPrecioSalida != null ? txtPrecioSalida.getText().trim() : "";
        String precioIva = txtPrecioIVA != null ? txtPrecioIVA.getText().trim() : "";
        String precioBruto = txtPrecioBruto != null ? txtPrecioBruto.getText().trim() : "";
        String precioTotal = txtPrecioTotal != null ? txtPrecioTotal.getText().trim() : "";

        if (clave == null || clave.isBlank()
                || nombre == null || nombre.isBlank()
                || descripcion.isBlank()
                || lote.isBlank()
                || cantidadTexto.isBlank()
                || presentacion == null || presentacion.isBlank()
                || factorTexto.isBlank()
                || precioEntrada.isBlank()
                || precioEntradaIva.isBlank()
                || precioSalida.isBlank()
                || precioIva.isBlank()
                || precioBruto.isBlank()
                || precioTotal.isBlank()) {
            mostrarAlerta("Advertencia", "Debe completar todos los campos antes de guardar, excepto el comentario.");
            return;
        }

        int cantidad;
        int factor;
        try {
            cantidad = Integer.parseInt(cantidadTexto);
            if (cantidad <= 0) {
                mostrarAlerta("Advertencia", "La cantidad debe ser mayor a 0.");
                return;
            }
        } catch (NumberFormatException e) {
            mostrarAlerta("Error", "La cantidad debe ser un número válido.");
            return;
        }

        try {
            factor = Integer.parseInt(factorTexto);
            if (factor <= 0) {
                mostrarAlerta("Advertencia", "El factor debe ser mayor a 0.");
                return;
            }
        } catch (NumberFormatException e) {
            mostrarAlerta("Error", "El factor debe ser un número válido.");
            return;
        }

        List<UbicacionCompra> ubicacionesSeleccionadas = obtenerUbicacionesSeleccionadas();
        if (ubicacionesSeleccionadas.isEmpty()) {
            mostrarAlerta("Advertencia", "Debe capturar las ubicaciones con cantidad.");
            return;
        }

        if (existeProductoLoteEnLista(clave, lote, itemParaEditar)) {
            mostrarAlerta("Advertencia",
                    "Ya se agregó este producto con el mismo lote. Finaliza el traspaso para poder repetirlo.");
            return;
        }

        if (tieneUbicacionesDuplicadas(ubicacionesSeleccionadas)) {
            mostrarAlerta("Advertencia", "No se puede seleccionar la misma ubicación más de una vez.");
            return;
        }

        int sumaUbicaciones = ubicacionesSeleccionadas.stream()
                .mapToInt(UbicacionCompra::getCantidad)
                .sum();
        if (sumaUbicaciones != cantidad) {
            mostrarAlerta("Advertencia", "La suma de cantidades por ubicación debe ser igual a la cantidad total.");
            return;
        }

        // En traspaso salida, el precio de salida debe ser igual al de entrada
        BigDecimal precioEntradaDecimal = parseDecimal(precioEntrada);
        BigDecimal precioSalidaDecimal = parseDecimal(precioSalida);
        if (precioSalidaDecimal.compareTo(precioEntradaDecimal) != 0) {
            Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
            confirmacion.setTitle("Advertencia");
            confirmacion.setHeaderText("El precio de salida no coincide con el precio de entrada.");
            confirmacion.setContentText("En traspaso de salida, el precio debe ser el mismo. ¿Deseas ajustarlo automáticamente?");
            Optional<ButtonType> respuesta = confirmacion.showAndWait();
            if (respuesta.isPresent() && respuesta.get() == ButtonType.OK) {
                txtPrecioSalida.setText(precioEntrada);
                precioSalidaDecimal = precioEntradaDecimal;
            } else {
                return;
            }
        }

        if (itemsTraspaso == null) {
            mostrarAlerta("Error", "No se pudo registrar el traspaso en la tabla.");
            return;
        }

        if (itemParaEditar != null) {
            itemParaEditar.setClaveProducto(clave);
            itemParaEditar.setProducto(nombre);
            itemParaEditar.setDescripcion(descripcion);
            itemParaEditar.setLote(lote);
            itemParaEditar.setCaducidad(caducidad != null ? caducidad.toString() : "");
            itemParaEditar.setCantidad(cantidad);
            itemParaEditar.setPresentacion(presentacion);
            itemParaEditar.setFactor(factor);
            itemParaEditar.setUbicaciones(ubicacionesSeleccionadas);
            itemParaEditar.setNota(nota);
            itemParaEditar.setPrecioEntrada(precioSalida);
            itemParaEditar.setPrecioIva(precioIva);
            itemParaEditar.setPrecioBruto(precioBruto);
            itemParaEditar.setPrecioTotal(precioTotal);
        } else {
            traspasoSalida item = new traspasoSalida(
                    clave,
                    nombre,
                    descripcion,
                    lote,
                    caducidad != null ? caducidad.toString() : "",
                    cantidad,
                    presentacion,
                    factor,
                    ubicacionesSeleccionadas,
                    precioSalida,
                    precioIva,
                    precioBruto,
                    precioTotal
            );
            item.setNota(nota);
            itemsTraspaso.add(item);
        }

        if (mainController != null) {
            mainController.refrescarTabla();
        }

        if (itemParaEditar != null) {
            mostrarAlertaSinEspera("Éxito", "Producto actualizado.");
            cerrarFormulario();
        } else {
            mostrarAlertaSinEspera("Éxito", "Producto agregado al traspaso.");
            limpiarFormularioParaNuevo();
        }
    }

    @Override
    protected void guardarItemRapido() {
        if (itemParaEditar != null) {
            mostrarAlerta("Advertencia", "La edición está disponible solo en el modo normal.");
            return;
        }

        String clave = productoController.getIdSeleccionado();
        String nombre = productoController.getNombreSeleccionado();
        String descripcion = txtDescripcion.getText() != null ? txtDescripcion.getText().trim() : "";
        String cantidadTexto = txtCantidadRapida != null && txtCantidadRapida.getText() != null
                ? txtCantidadRapida.getText().trim()
                : "";

        // Obtener presentación y factor del modo rápido
        String presentacionRapida = "pz"; // Valor por defecto
        int factorRapido = 1; // Valor por defecto

        if (cbPresentacionRapida != null && cbPresentacionRapida.getValue() != null
                && !cbPresentacionRapida.getValue().isBlank()) {
            presentacionRapida = cbPresentacionRapida.getValue().trim();
        }

        if (txtFactorRapido != null && txtFactorRapido.getText() != null && !txtFactorRapido.getText().isBlank()) {
            try {
                factorRapido = Integer.parseInt(txtFactorRapido.getText().trim());
                if (factorRapido <= 0) {
                    mostrarAlerta("Advertencia", "El factor debe ser mayor a 0.");
                    return;
                }
            } catch (NumberFormatException e) {
                mostrarAlerta("Error", "El factor debe ser un número válido.");
                return;
            }
        } else if (!presentacionRapida.equalsIgnoreCase("pz")) {
            // Si no es "pz" y no tiene factor, mostrar error
            mostrarAlerta("Advertencia", "Debe especificar un factor para la presentación seleccionada.");
            return;
        }

        // VALIDACIÓN 1: Verificar que el producto esté seleccionado
        if (clave == null || clave.isBlank() || nombre == null || nombre.isBlank()) {
            mostrarAlerta("Advertencia", "Debe seleccionar un producto.");
            return;
        }

        // VALIDACIÓN 2: Verificar que la presentación esté seleccionada
        if (presentacionRapida.isBlank()) {
            mostrarAlerta("Advertencia", "Debe seleccionar una presentación.");
            return;
        }

        // VALIDACIÓN 3: Verificar que la cantidad esté capturada
        if (cantidadTexto.isBlank()) {
            mostrarAlerta("Advertencia", "Debe capturar la cantidad.");
            return;
        }

        // VALIDACIÓN 4: Verificar que los precios estén completos
        String precioSalida = txtPrecioSalidaRapida != null && txtPrecioSalidaRapida.getText() != null
                ? txtPrecioSalidaRapida.getText().trim()
                : "";
        String precioIva = txtPrecioIVARapida != null && txtPrecioIVARapida.getText() != null
                ? txtPrecioIVARapida.getText().trim()
                : "";
        String precioBruto = txtPrecioBrutoRapida != null && txtPrecioBrutoRapida.getText() != null
                ? txtPrecioBrutoRapida.getText().trim()
                : "";
        String precioTotal = txtPrecioTotalRapida != null && txtPrecioTotalRapida.getText() != null
                ? txtPrecioTotalRapida.getText().trim()
                : "";

        if (descripcion.isBlank() || precioSalida.isBlank() || precioIva.isBlank()
                || precioBruto.isBlank() || precioTotal.isBlank()) {
            mostrarAlerta("Advertencia", "Debe completar todos los campos antes de guardar.");
            return;
        }

        // VALIDACIÓN 5: Validar cantidad numérica
        int cantidad;
        try {
            cantidad = Integer.parseInt(cantidadTexto);
            if (cantidad <= 0) {
                mostrarAlerta("Advertencia", "La cantidad debe ser mayor a 0.");
                return;
            }
        } catch (NumberFormatException e) {
            mostrarAlerta("Error", "La cantidad debe ser un número válido.");
            return;
        }

        // VALIDACIÓN 6: Verificar que el precio de salida coincida con el de entrada
        if (txtPrecioEntradaRapida != null && txtPrecioSalidaRapida != null) {
            String precioEntradaTexto = txtPrecioEntradaRapida.getText() != null
                    ? txtPrecioEntradaRapida.getText().trim()
                    : "";
            String precioSalidaTexto = txtPrecioSalidaRapida.getText() != null
                    ? txtPrecioSalidaRapida.getText().trim()
                    : "";

            if (!precioEntradaTexto.isBlank() && !precioSalidaTexto.isBlank()) {
                BigDecimal precioEntrada = parseDecimal(precioEntradaTexto);
                BigDecimal precioSalidaDec = parseDecimal(precioSalidaTexto);

                if (precioSalidaDec.compareTo(precioEntrada) != 0) {
                    Alert alerta = new Alert(Alert.AlertType.WARNING);
                    alerta.setTitle("Advertencia de precio");
                    alerta.setHeaderText("El precio de salida no coincide con el precio de entrada.");
                    alerta.setContentText("En traspaso de salida, el precio debe ser el mismo. ¿Deseas ajustarlo automáticamente?");
                    alerta.getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);

                    Optional<ButtonType> respuesta = alerta.showAndWait();
                    if (respuesta.isPresent() && respuesta.get() == ButtonType.OK) {
                        txtPrecioSalidaRapida.setText(precioEntradaTexto);
                    } else {
                        return;
                    }
                }
            }
        }

        // VALIDACIÓN 7: Verificar disponibilidad del producto con presentación y factor específicos
        int disponible = modelo.obtenerCantidadDisponibleProductoPresentacionFactor(
                clave, presentacionRapida, factorRapido);
        if (cantidad > disponible) {
            mostrarAlerta("Advertencia",
                    "La cantidad supera la disponible para la presentación " + presentacionRapida
                            + " con factor " + factorRapido + ".");
            return;
        }

        // VALIDACIÓN 8: Verificar que la combinación producto-presentación-factor exista en inventario
        String claveSnapshot = clave;
        String presentacionSnapshot = presentacionRapida;
        int factorSnapshot = factorRapido;
        int cantidadSnapshot = cantidad;

        Task<Boolean> validacionTask = new Task<>() {
            @Override
            protected Boolean call() {
                // Primero verificar si la combinación existe
                boolean combinacionExiste = modelo.existeCombinacionProductoPresentacionFactor(
                        claveSnapshot, presentacionSnapshot, factorSnapshot);

                if (!combinacionExiste) {
                    return false;
                }

                // Luego verificar disponibilidad específica
                return modelo.obtenerCantidadDisponibleProductoPresentacionFactor(
                        claveSnapshot, presentacionSnapshot, factorSnapshot) >= cantidadSnapshot;
            }

            @Override
            protected void succeeded() {
                boolean validacionExitosa = getValue();
                if (!validacionExitosa) {
                    Platform.runLater(() -> {
                        mostrarAlerta("Error",
                                "La combinación de producto, presentación y factor no existe en inventario " +
                                        "o no hay suficiente cantidad disponible.");
                    });
                } else {
                    // Si la validación es exitosa, continuar con el resto del proceso
                    Platform.runLater(() -> continuarGuardadoRapido(claveSnapshot, nombre, descripcion,
                            presentacionSnapshot, factorSnapshot, cantidadSnapshot,
                            precioSalida, precioIva, precioBruto, precioTotal));
                }
            }

            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    mostrarAlerta("Error", "Error al validar la disponibilidad del producto.");
                });
            }
        };

        // Ejecutar la validación en segundo plano
        Thread hiloValidacion = new Thread(validacionTask);
        hiloValidacion.setDaemon(true);
        hiloValidacion.start();
    }


    @Override
    protected boolean existeProductoLoteEnLista(String clave, String lote, traspasoSalida itemExcluir) {
        if (clave == null || clave.isBlank() || lote == null || lote.isBlank()) {
            return false;
        }

        if (mainController != null && mainController.existeProductoLote(clave, lote, itemExcluir)) {
            return true;
        }

        if (itemsTraspaso == null) {
            return false;
        }

        String claveNormalizada = clave.trim();
        String loteNormalizado = lote.trim();

        return itemsTraspaso.stream()
                .anyMatch(item -> item != null
                        && item != itemExcluir
                        && claveNormalizada.equals(item.getClaveProducto())
                        && loteNormalizado.equals(item.getLote()));
    }

    @Override
    protected List<UbicacionCompra> obtenerUbicacionesSeleccionadas() {
        List<UbicacionCompra> resultado = new java.util.ArrayList<>();

        if (contenedorUbicaciones == null) {
            return resultado;
        }

        for (javafx.scene.Node nodo : contenedorUbicaciones.getChildren()) {
            if (!(nodo instanceof HBox)) {
                continue;
            }

            HBox fila = (HBox) nodo;
            if (fila.getChildren().size() < 2) {
                continue;
            }

            VBox contenedorUbicacion = (VBox) fila.getChildren().get(0);
            VBox contenedorCantidad = (VBox) fila.getChildren().get(1);

            ComboBox<String> combo = null;
            TextField campoCantidad = null;

            if (contenedorUbicacion != null && !contenedorUbicacion.getChildren().isEmpty()) {
                javafx.scene.Node nodoCombo = contenedorUbicacion.getChildren().get(1);
                if (nodoCombo instanceof ComboBox) {
                    combo = (ComboBox<String>) nodoCombo;
                }
            }

            if (contenedorCantidad != null && !contenedorCantidad.getChildren().isEmpty()) {
                javafx.scene.Node nodoCantidad = contenedorCantidad.getChildren().get(1);
                if (nodoCantidad instanceof TextField) {
                    campoCantidad = (TextField) nodoCantidad;
                }
            }

            if (combo == null || campoCantidad == null) {
                continue;
            }

            String ubicacion = combo.getValue();
            String cantidadTexto = campoCantidad.getText() != null ? campoCantidad.getText().trim() : "";

            if (ubicacion == null || ubicacion.isBlank() || cantidadTexto.isBlank()) {
                continue;
            }

            int cantidad;
            try {
                cantidad = Integer.parseInt(cantidadTexto);
            } catch (NumberFormatException e) {
                continue;
            }

            if (cantidad <= 0) {
                continue;
            }

            resultado.add(new UbicacionCompra(ubicacion, cantidad));
        }

        return resultado;
    }

    @Override
    protected boolean tieneUbicacionesDuplicadas(List<UbicacionCompra> ubicacionesSeleccionadas) {
        if (ubicacionesSeleccionadas == null) {
            return false;
        }

        HashSet<String> ubicacionesUnicas = new HashSet<>();
        for (UbicacionCompra ubicacionCompra : ubicacionesSeleccionadas) {
            if (ubicacionCompra == null || ubicacionCompra.getUbicacion() == null) {
                continue;
            }
            String ubicacion = ubicacionCompra.getUbicacion().trim();
            if (ubicacion.isEmpty()) {
                continue;
            }
            if (!ubicacionesUnicas.add(ubicacion)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void limpiarFormularioParaNuevo() {
        limpiarValidacionesInventario();
        seleccionarClaveAlternaPendiente = false;

        if (productoController != null) {
            productoController.limpiarSeleccion();
        }

        limpiarFormularioDependiente();

        if (txtPrecioSalida != null) {
            txtPrecioSalida.clear();
        }

        ubicacionesCapturadas.clear();
        debounceCantidadUbicacion.clear();
        limpiarFilasAdicionales();

        Platform.runLater(() -> {
            if (cbClaveProducto != null) {
                cbClaveProducto.requestFocus();
            }
        });
    }

    @Override
    protected void mostrarAlerta(String titulo, String mensaje) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(titulo);
            alert.setHeaderText(null);
            alert.setContentText(mensaje);
            alert.showAndWait();
        });
    }

    @Override
    protected void mostrarAlertaSinEspera(String titulo, String mensaje) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(titulo);
            alert.setHeaderText(null);
            alert.setContentText(mensaje);
            alert.show();

            new Thread(() -> {
                try {
                    Thread.sleep(2000);
                    if (alert.isShowing()) {
                        Platform.runLater(alert::close);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        });
    }

    @Override
    protected void recalcularPrecios() {
        int cantidad = parseEntero(txtCantidad != null ? txtCantidad.getText() : "");

        if (cantidad <= 0) {
            if (txtPrecioBruto != null) txtPrecioBruto.clear();
            if (txtPrecioTotal != null) txtPrecioTotal.clear();
            if (txtPrecioIVA != null) txtPrecioIVA.clear();
            return;
        }

        BigDecimal precioSalida = parseDecimal(txtPrecioSalida != null ? txtPrecioSalida.getText() : "");
        BigDecimal precioConIva = precioSalida;

        // En traspaso no se aplica/quita IVA - el precio con IVA viene del precio de entrada con IVA
        if (txtPrecioEntradaIva != null && !txtPrecioEntradaIva.getText().isBlank()) {
            precioConIva = parseDecimal(txtPrecioEntradaIva.getText());
        }

        BigDecimal precioBruto = precioSalida.multiply(BigDecimal.valueOf(cantidad));
        BigDecimal precioTotal = precioConIva.multiply(BigDecimal.valueOf(cantidad));

        if (txtPrecioIVA != null) txtPrecioIVA.setText(formatearDecimal(precioConIva));
        if (txtPrecioBruto != null) txtPrecioBruto.setText(formatearDecimal(precioBruto));
        if (txtPrecioTotal != null) txtPrecioTotal.setText(formatearDecimal(precioTotal));
    }

    @Override
    protected void recalcularPreciosRapido() {
        if (txtCantidadRapida == null || txtPrecioSalidaRapida == null) {
            return;
        }

        int cantidad = parseEntero(txtCantidadRapida.getText());

        if (cantidad <= 0) {
            if (txtPrecioBrutoRapida != null) txtPrecioBrutoRapida.clear();
            if (txtPrecioTotalRapida != null) txtPrecioTotalRapida.clear();
            if (txtPrecioIVARapida != null) txtPrecioIVARapida.clear();
            return;
        }

        BigDecimal precioSalida = parseDecimal(txtPrecioSalidaRapida.getText());
        BigDecimal precioConIva = precioSalida;

        // En traspaso no se aplica/quita IVA
        if (txtPrecioEntradaIvaRapida != null && !txtPrecioEntradaIvaRapida.getText().isBlank()) {
            precioConIva = parseDecimal(txtPrecioEntradaIvaRapida.getText());
        }

        BigDecimal precioBruto = precioSalida.multiply(BigDecimal.valueOf(cantidad));
        BigDecimal precioTotal = precioConIva.multiply(BigDecimal.valueOf(cantidad));

        if (txtPrecioIVARapida != null) txtPrecioIVARapida.setText(formatearDecimal(precioConIva));
        if (txtPrecioBrutoRapida != null) txtPrecioBrutoRapida.setText(formatearDecimal(precioBruto));
        if (txtPrecioTotalRapida != null) txtPrecioTotalRapida.setText(formatearDecimal(precioTotal));
    }

    @Override
    protected void limpiarPrecios() {
        if (txtPrecioEntrada != null) txtPrecioEntrada.clear();
        if (txtPrecioEntradaIva != null) txtPrecioEntradaIva.clear();
        if (txtPrecioSalida != null) txtPrecioSalida.clear();
        if (txtPrecioIVA != null) txtPrecioIVA.clear();
        if (txtPrecioBruto != null) txtPrecioBruto.clear();
        if (txtPrecioTotal != null) txtPrecioTotal.clear();

        precioEntradaBase = BigDecimal.ZERO;
        precioIvaBase = BigDecimal.ZERO;
    }

    @Override
    protected void limpiarPreciosRapidos() {
        if (txtPrecioEntradaRapida != null) txtPrecioEntradaRapida.clear();
        if (txtPrecioEntradaIvaRapida != null) txtPrecioEntradaIvaRapida.clear();
        if (txtPrecioSalidaRapida != null) txtPrecioSalidaRapida.clear();
        if (txtPrecioIVARapida != null) txtPrecioIVARapida.clear();
        if (txtPrecioBrutoRapida != null) txtPrecioBrutoRapida.clear();
        if (txtPrecioTotalRapida != null) txtPrecioTotalRapida.clear();
    }

    @Override
    protected void cargarPreciosDesdeProducto() {
        String idProducto = productoController.getIdSeleccionado();
        if (idProducto == null || idProducto.isBlank()) {
            limpiarPrecios();
            return;
        }

        if (!datosCompletosParaPrecioEntrada()) {
            limpiarPrecios();
            return;
        }

        boolean precioSalidaVacio = txtPrecioSalida == null ||
                txtPrecioSalida.getText() == null ||
                txtPrecioSalida.getText().isBlank();

        if (!precioSalidaVacio) {
            return;
        }

        String lote = txtLote.getText() != null ? txtLote.getText().trim() : "";
        String presentacion = cbPresentacion.getValue();

        Task<Optional<modelNuevoTraspasoSalida.PreciosProducto>> task = new Task<>() {
            @Override
            protected Optional<modelNuevoTraspasoSalida.PreciosProducto> call() {
                return modelo.obtenerPreciosProductoPorLotePresentacion(idProducto, lote, presentacion);
            }

            @Override
            protected void succeeded() {
                Optional<modelNuevoTraspasoSalida.PreciosProducto> resultado = getValue();
                if (resultado.isPresent()) {
                    aplicarPrecioEntrada(resultado.get());
                }
            }
        };

        Thread hilo = new Thread(task);
        hilo.setDaemon(true);
        hilo.start();
    }

    @Override
    protected void cargarPreciosRapidosDesdeUltimaEntrada() {
        String idProducto = productoController.getIdSeleccionado();
        if (idProducto == null || idProducto.isBlank()) {
            limpiarPreciosRapidos();
            return;
        }

        String idSnapshot = idProducto;
        Task<Optional<modelNuevoTraspasoSalida.PreciosProducto>> task = new Task<>() {
            @Override
            protected Optional<modelNuevoTraspasoSalida.PreciosProducto> call() {
                return modelo.obtenerPreciosProductoUltimaEntrada(idSnapshot);
            }

            @Override
            protected void succeeded() {
                String idActual = productoController.getIdSeleccionado();
                if (!idSnapshot.equals(idActual)) {
                    return;
                }

                Optional<modelNuevoTraspasoSalida.PreciosProducto> resultado = getValue();
                if (resultado.isPresent()) {
                    aplicarPreciosRapidos(resultado.get());
                } else {
                    limpiarPreciosRapidos();
                }
            }

            @Override
            protected void failed() {
                limpiarPreciosRapidos();
            }
        };

        Thread hilo = new Thread(task);
        hilo.setDaemon(true);
        hilo.start();
    }

    @Override
    protected void configurarCamposLecturaEspecificos() {
        // Campos específicos de traspaso de solo lectura
        if (txtPrecioEntrada != null) txtPrecioEntrada.setEditable(false);
        if (txtPrecioEntradaIva != null) txtPrecioEntradaIva.setEditable(false);
        if (txtPrecioEntradaRapida != null) txtPrecioEntradaRapida.setEditable(false);
        if (txtPrecioEntradaIvaRapida != null) txtPrecioEntradaIvaRapida.setEditable(false);
        if (txtPrecioIVA != null) txtPrecioIVA.setEditable(false);
        if (txtPrecioBruto != null) txtPrecioBruto.setEditable(false);
        if (txtPrecioTotal != null) txtPrecioTotal.setEditable(false);
        if (txtPrecioIVARapida != null) txtPrecioIVARapida.setEditable(false);
        if (txtPrecioBrutoRapida != null) txtPrecioBrutoRapida.setEditable(false);
        if (txtPrecioTotalRapida != null) txtPrecioTotalRapida.setEditable(false);
        if (txtPrecioSalida != null) txtPrecioSalida.setEditable(false);
        if (txtPrecioSalidaRapida != null) txtPrecioSalidaRapida.setEditable(false);

        aplicarModoAjusteInventario();
    }

    @Override
    protected void configurarCalculoPreciosEspecifico() {
        // Configuración específica de cálculo de precios para traspaso
        if (txtPrecioSalida != null) {
            txtPrecioSalida.textProperty().addListener((obs, oldVal, newVal) -> recalcularPrecios());
        }

        if (txtPrecioSalidaRapida != null) {
            txtPrecioSalidaRapida.textProperty().addListener((obs, oldVal, newVal) -> recalcularPreciosRapido());
        }
    }

    // === MÉTODOS ESPECÍFICOS DE TRASPASO ===

    private void aplicarModoSoloNormal() {
        if (!modoSoloNormal || tabPaneModo == null) {
            return;
        }

        if (tabRapido != null) {
            tabPaneModo.getTabs().remove(tabRapido);
        }

        if (tabNormal != null) {
            tabPaneModo.getSelectionModel().select(tabNormal);
        }

        tabPaneModo.getStyleClass().add("modo-tabs-sin-header");
    }

    private void aplicarModoAjusteInventario() {
        if (!modoAjusteInventario) {
            return;
        }

        if (txtPrecioSalida != null) {
            txtPrecioSalida.setEditable(false);
        }

        if (txtPrecioSalidaRapida != null) {
            txtPrecioSalidaRapida.setEditable(false);
        }
    }

    private boolean datosCompletosParaPrecioEntrada() {
        return productoController.getIdSeleccionado() != null
                && !productoController.getIdSeleccionado().isBlank()
                && loteValidado
                && cbPresentacion.getValue() != null
                && !cbPresentacion.getValue().isBlank();
    }

    private void aplicarPrecioEntrada(modelNuevoTraspasoSalida.PreciosProducto precios) {
        if (precios == null) {
            limpiarPrecios();
            return;
        }

        precioEntradaBase = precios.getPrecioUnitario() != null ? precios.getPrecioUnitario() : BigDecimal.ZERO;
        precioIvaBase = precios.getPrecioIva() != null ? precios.getPrecioIva() : BigDecimal.ZERO;

        // SOLO actualizar txtPrecioEntrada (solo lectura)
        if (txtPrecioEntrada != null) {
            txtPrecioEntrada.setText(formatearDecimal(precioEntradaBase));
        }

        if (txtPrecioEntradaIva != null) {
            txtPrecioEntradaIva.setText(formatearDecimal(precioIvaBase));
        }

        // SOLO actualizar txtPrecioSalida si está vacío
        if (txtPrecioSalida != null &&
                (txtPrecioSalida.getText() == null || txtPrecioSalida.getText().isBlank())) {
            txtPrecioSalida.setText(formatearDecimal(precioEntradaBase));
        }

        recalcularPrecios();
    }

    private void aplicarPreciosRapidos(modelNuevoTraspasoSalida.PreciosProducto precios) {
        if (precios == null) {
            limpiarPreciosRapidos();
            return;
        }

        BigDecimal precioEntradaRapida = precios.getPrecioUnitario() != null
                ? precios.getPrecioUnitario()
                : BigDecimal.ZERO;
        BigDecimal precioEntradaIvaRapida = precios.getPrecioIva() != null
                ? precios.getPrecioIva()
                : BigDecimal.ZERO;

        if (txtPrecioEntradaRapida != null) {
            txtPrecioEntradaRapida.setText(formatearDecimal(precioEntradaRapida));
        }

        if (txtPrecioEntradaIvaRapida != null) {
            txtPrecioEntradaIvaRapida.setText(formatearDecimal(precioEntradaIvaRapida));
        }

        if (txtPrecioSalidaRapida != null) {
            txtPrecioSalidaRapida.setText(formatearDecimal(precioEntradaRapida));
        }

        recalcularPreciosRapido();
    }

    private void continuarGuardadoRapido(String clave, String nombre, String descripcion,
                                         String presentacionRapida, int factorRapido, int cantidad,
                                         String precioSalida, String precioIva,
                                         String precioBruto, String precioTotal) {

        if (itemsTraspaso == null) {
            mostrarAlerta("Error", "No se pudo registrar el traspaso en la tabla.");
            return;
        }

        // Obtener disponibilidades rápidas
        List<modelNuevoTraspasoSalida.DisponibilidadRapida> disponibles =
                modelo.obtenerDisponibilidadesRapidas(clave, presentacionRapida, factorRapido);

        if (disponibles == null || disponibles.isEmpty()) {
            mostrarAlerta("Error", "No hay disponibilidad para el producto con las características especificadas.");
            return;
        }

        // Construir asignaciones
        List<AsignacionRapida> asignaciones = construirAsignacionesRapidas(disponibles, cantidad);
        if (asignaciones.isEmpty()) {
            mostrarAlerta("Error", "No se pudo distribuir la cantidad solicitada con la disponibilidad actual.");
            return;
        }

        // Confirmaciones del usuario
        if (!confirmarRevisionUbicacionesRapidas()) {
            return;
        }

        if (!mostrarResumenUbicacionesRapidas(asignaciones)) {
            return;
        }

        mostrarAlerta("Aviso", "Revisión de ubicaciones confirmada.");

        // Construir items de traspaso
        List<traspasoSalida> itemsGenerados = construirItemsRapidosTraspaso(
                clave, nombre, descripcion, asignaciones, presentacionRapida, factorRapido);

        if (itemsGenerados.isEmpty()) {
            mostrarAlerta("Error", "No se pudo distribuir la cantidad solicitada con la disponibilidad actual.");
            return;
        }

        // Procesar cada item generado
        BigDecimal precioSalidaDecimal = parseDecimal(precioSalida);
        BigDecimal precioIvaDecimal = parseDecimal(precioIva);

        for (traspasoSalida item : itemsGenerados) {
            // Verificar si ya existe en el traspaso
            if (existeProductoLoteEnLista(clave, item.getLote(), null)) {
                mostrarAlerta("Advertencia",
                        "Ya se agregó este producto con el mismo lote. Finaliza el traspaso para poder repetirlo.");
                return;
            }

            // Calcular precios para este item
            int cantidadItem = item.getCantidad();
            BigDecimal brutoItem = precioSalidaDecimal.multiply(BigDecimal.valueOf(cantidadItem));
            BigDecimal totalItem = precioIvaDecimal.multiply(BigDecimal.valueOf(cantidadItem));

            // Establecer precios en el item
            item.setPrecioEntrada(formatearDecimal(precioSalidaDecimal));
            item.setPrecioIva(formatearDecimal(precioIvaDecimal));
            item.setPrecioBruto(formatearDecimal(brutoItem));
            item.setPrecioTotal(formatearDecimal(totalItem));

            // Agregar a la lista de traspasos
            itemsTraspaso.add(item);
        }

        // Actualizar interfaz si hay controlador principal
        if (mainController != null) {
            mainController.refrescarTabla();
        }

        // Mostrar mensaje de éxito y limpiar formulario
        mostrarAlertaSinEspera("Éxito", "Producto agregado al traspaso.");
        limpiarFormularioParaNuevo();
    }

    private List<traspasoSalida> construirItemsRapidosTraspaso(
            String clave,
            String nombre,
            String descripcion,
            List<AsignacionRapida> asignaciones,
            String presentacion,
            int factor
    ) {
        return construirItemsRapidos(clave, nombre, descripcion, asignaciones, presentacion, factor);
    }

    private void cargarItemParaEditar() {
        if (itemParaEditar == null) {
            return;
        }

        limpiarFormularioParaNuevo();
        cbClaveProducto.setValue(itemParaEditar.getClaveProducto());
        cbProductoNombre.setValue(itemParaEditar.getProducto());
        txtDescripcion.setText(itemParaEditar.getDescripcion());

        if (txtNota != null) {
            txtNota.setText(itemParaEditar.getNota());
        }

        txtLote.setText(itemParaEditar.getLote());
        configurarCaducidadDesdeTexto(itemParaEditar.getCaducidad());
        loteValidado = true;
        caducidadValidada = true;
        txtCantidad.setText(String.valueOf(itemParaEditar.getCantidad()));
        cbPresentacion.setValue(itemParaEditar.getPresentacion());
        txtFactor.setText(String.valueOf(itemParaEditar.getFactor()));
        presentacionValida = true;
        factorValido = true;
        cargarPreciosDesdeProducto();

        String precioSalida = itemParaEditar.getPrecioEntrada();
        txtPrecioSalida.setText(precioSalida);
        txtPrecioIVA.setText(itemParaEditar.getPrecioIva());
        txtPrecioBruto.setText(itemParaEditar.getPrecioBruto());
        txtPrecioTotal.setText(itemParaEditar.getPrecioTotal());

        cargarUbicacionesParaEdicion(itemParaEditar.getUbicaciones());
        recalcularPrecios();
    }

    private void configurarCaducidadDesdeTexto(String caducidadTexto) {
        if (caducidadTexto == null || caducidadTexto.isBlank()) {
            if (dpCaducidad != null) {
                dpCaducidad.setValue(null);
            }
            return;
        }

        try {
            if (dpCaducidad != null) {
                dpCaducidad.setValue(java.time.LocalDate.parse(caducidadTexto));
            }
        } catch (java.time.format.DateTimeParseException e) {
            if (dpCaducidad != null) {
                dpCaducidad.setValue(null);
            }
        }
    }

    private void cargarUbicacionesParaEdicion(List<UbicacionCompra> ubicacionesLista) {
        limpiarFilasAdicionales();
        ubicacionesCapturadas.clear();
        ultimaCantidadUbicacionValidada.clear();

        if (ubicacionesLista == null || ubicacionesLista.isEmpty()) {
            return;
        }

        for (int i = 0; i < ubicacionesLista.size(); i++) {
            UbicacionCompra ubicacion = ubicacionesLista.get(i);
            if (i > 0) {
                agregarUbicacionCombo();
            }

            HBox fila = (HBox) contenedorUbicaciones.getChildren().get(i);
            VBox contenedorUbicacion = (VBox) fila.getChildren().get(0);
            VBox contenedorCantidad = (VBox) fila.getChildren().get(1);
            ComboBox<String> combo = (ComboBox<String>) contenedorUbicacion.getChildren().get(1);
            TextField campoCantidad = (TextField) contenedorCantidad.getChildren().get(1);

            combo.setValue(ubicacion.getUbicacion());
            if (combo.getEditor() != null) {
                combo.getEditor().setText(ubicacion.getUbicacion());
            }

            campoCantidad.setText(String.valueOf(ubicacion.getCantidad()));
        }
    }

    private void cerrarFormulario() {
        if (btnGuardar != null && btnGuardar.getScene() != null) {
            Stage stage = (Stage) btnGuardar.getScene().getWindow();
            if (stage != null) {
                stage.close();
            }
        }
    }

    // === GETTERS Y SETTERS ESPECÍFICOS ===

    public void setItemsTraspaso(ObservableList<traspasoSalida> itemsTraspaso) {
        this.itemsTraspaso = itemsTraspaso;
    }

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    public void setItemParaEditar(traspasoSalida item) {
        this.itemParaEditar = item;
        if (itemParaEditar != null && inicializado) {
            cargarItemParaEditar();
        }
    }

    public void setTituloFormulario(String tituloFormulario) {
        if (tituloFormulario == null || tituloFormulario.isBlank()) {
            return;
        }

        this.tituloFormulario = tituloFormulario;
        if (lblTitulo != null) {
            lblTitulo.setText(tituloFormulario);
        }
    }

    public void setModoSoloNormal(boolean modoSoloNormal) {
        this.modoSoloNormal = modoSoloNormal;
        aplicarModoSoloNormal();
    }

    public void setModoAjusteInventario(boolean modoAjusteInventario) {
        this.modoAjusteInventario = modoAjusteInventario;
        if (inicializado) {
            aplicarModoAjusteInventario();
        }
    }
}