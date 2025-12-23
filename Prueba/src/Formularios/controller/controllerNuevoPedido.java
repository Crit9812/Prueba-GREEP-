package Formularios.controller;

import Operaciones.pedidos.controller.MainController;
import Compartido.controller.productoCboxController;
import Operaciones.pedidos.model.itemPedido;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.input.KeyCode;

public class controllerNuevoPedido {

    @FXML private ComboBox<String> cbClaveProducto;
    @FXML private ComboBox<String> cbProductoNombre;
    @FXML private TextField txtCantidad;
    @FXML private TextField txtDescripcion;
    @FXML private Button btnGuardar;
    @FXML private Button btnCancelar;
    @FXML private ComboBox<String> cbClaveAlterna;
    @FXML private ComboBox<String> cbPresentacion;
    @FXML private TextField txtFactor;

    // ESTA LISTA VIENE DESDE EL CONTROLLER DE LA TABLA
    private ObservableList<itemPedido> itemsPedido;
    private MainController mainController;
    private itemPedido itemEnEdicion; // 🔥 NUEVO: Para saber si estamos editando
    private boolean modoEdicion = false; // 🔥 NUEVO: Flag para modo edición

    // Controlador reutilizable para productos
    private productoCboxController productoController;

    // Lista de presentaciones disponibles
    private ObservableList<String> presentaciones = FXCollections.observableArrayList(
            "Pieza", "Caja", "Paquete", "Rollo", "Litro", "Kilogramo", "Metro", "Unidad"
    );

    @FXML
    public void initialize() {
        // Inicializar controlador de productos
        productoController = new productoCboxController();
        productoController.inicializar(cbClaveProducto, cbProductoNombre, cbClaveAlterna);

        // Configurar ComboBox de presentación
        configurarPresentaciones();

        // Configurar eventos del formulario
        configurarEventos();
        validarCantidad();
        validarFactor();

        // Configurar tecla Enter en los campos
        configurarManejoEnter();

        // Enfocar el primer campo al iniciar
        Platform.runLater(() -> cbClaveProducto.requestFocus());
    }

    private void configurarPresentaciones() {
        cbPresentacion.setItems(presentaciones);
        cbPresentacion.setValue("Pieza"); // Valor por defecto
    }

    public void setItemsPedido(ObservableList<itemPedido> itemsPedido) {
        this.itemsPedido = itemsPedido;
    }

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    private void configurarEventos() {
        // Evento para cuando cambia la selección (actualizar descripción)
        cbClaveProducto.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                actualizarDescripcionDesdeProducto();
            }
        });

        cbProductoNombre.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                actualizarDescripcionDesdeProducto();
            }
        });

        cbClaveAlterna.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                actualizarDescripcionDesdeProducto();
            }
        });

        btnGuardar.setOnAction(e -> guardarItem());
        btnCancelar.setOnAction(e -> cerrarFormulario());
    }

    /**
     * Configurar manejo de tecla Enter
     */
    private void configurarManejoEnter() {
        // Permitir usar Enter para guardar desde el campo de factor
        txtFactor.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                guardarItem();
                event.consume();
            }
        });

        // Permitir usar Enter para navegar entre campos
        cbClaveProducto.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                cbProductoNombre.requestFocus();
                event.consume();
            }
        });

        cbProductoNombre.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                cbClaveAlterna.requestFocus();
                event.consume();
            }
        });

        cbClaveAlterna.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                cbPresentacion.requestFocus();
                event.consume();
            }
        });

        cbPresentacion.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                txtFactor.requestFocus();
                event.consume();
            }
        });

        txtCantidad.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                guardarItem();
                event.consume();
            }
        });

        // También permitir Enter desde el campo factor
        txtFactor.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                guardarItem();
                event.consume();
            }
        });
    }

    private void actualizarDescripcionDesdeProducto() {
        String descripcion = productoController.getDescripcionSeleccionada();
        txtDescripcion.setText(descripcion);
    }

    private void guardarItem() {
        if (itemsPedido == null) {
            mostrarAlerta("Error", "No se pudo conectar con la tabla principal");
            return;
        }

        String clave = productoController.getIdSeleccionado();
        String nombre = productoController.getNombreSeleccionado();
        String claveAlterna = productoController.getClaveAlternaSeleccionada();
        String cantidadTexto = txtCantidad.getText();
        String presentacion = cbPresentacion.getValue();
        String factor = txtFactor.getText();

        if (clave == null || clave.isEmpty() ||
                nombre == null || nombre.isEmpty() ||
                cantidadTexto == null || cantidadTexto.isBlank()) {
            mostrarAlerta("Advertencia", "Complete todos los campos obligatorios");
            return;
        }

        try {
            int cantidad = Integer.parseInt(cantidadTexto);
            if (cantidad <= 0) {
                mostrarAlerta("Advertencia", "La cantidad debe ser mayor a 0");
                return;
            }

            // Validar factor si se ingresó
            if (factor != null && !factor.isEmpty()) {
                try {
                    double factorNum = Double.parseDouble(factor);
                    if (factorNum <= 0) {
                        mostrarAlerta("Advertencia", "El factor debe ser mayor a 0");
                        return;
                    }
                } catch (NumberFormatException e) {
                    mostrarAlerta("Error", "El factor debe ser un número válido");
                    return;
                }
            }

            if (!productoController.validarSeleccion()) {
                mostrarAlerta("Error", "El ID y el nombre del producto no corresponden.\n" +
                        "Por favor, verifique la selección.");
                return;
            }

            String descripcionCorrecta = productoController.getDescripcionSeleccionada();

            // Si no se seleccionó presentación, usar valor por defecto
            if (presentacion == null || presentacion.isEmpty()) {
                presentacion = "Pieza";
            }

            // Crear item con todos los campos
            itemPedido item = new itemPedido(
                    clave,
                    nombre,
                    descripcionCorrecta,
                    cantidad,
                    claveAlterna != null ? claveAlterna : "",
                    presentacion,
                    factor != null ? factor : ""
            );

            // 🔥 MODIFICADO: Manejar modo edición vs modo nuevo
            if (modoEdicion && itemEnEdicion != null) {
                // Modo edición: reemplazar el item existente
                if (mainController != null) {
                    mainController.actualizarItemEnLista(itemEnEdicion, item);
                }
                mostrarAlertaSinEspera("Éxito", "Producto actualizado correctamente");

                // Cerrar formulario después de editar (opcional, puedes cambiarlo)
                cerrarFormulario();
            } else {
                // Modo nuevo: agregar nuevo item
                itemsPedido.add(item);
                mostrarAlertaSinEspera("Éxito", "Producto agregado al pedido");

                if (mainController != null) {
                    mainController.refrescarTabla();
                }

                // Limpiar formulario para agregar otro
                limpiarFormularioParaNuevo();
            }

        } catch (NumberFormatException e) {
            mostrarAlerta("Error", "La cantidad debe ser un número válido");
        }
    }

    /**
     * 🔥 NUEVO: Método para cargar un item existente para editar
     */
    public void cargarItemParaEditar(itemPedido item) {
        if (item == null) return;

        this.itemEnEdicion = item;
        this.modoEdicion = true;

        // Cargar datos del item en los campos del formulario
        productoController.setSeleccion(item.getClaveProducto(), item.getProducto());
        txtCantidad.setText(String.valueOf(item.getCantidad()));
        txtDescripcion.setText(item.getDescripcion());

        // Cargar clave alterna si existe
        if (item.getClaveAlterna() != null && !item.getClaveAlterna().isEmpty()) {
            cbClaveAlterna.setValue(item.getClaveAlterna());
        }

        // Cargar presentación y factor
        if (item.getPresentacion() != null && !item.getPresentacion().isEmpty()) {
            cbPresentacion.setValue(item.getPresentacion());
        } else {
            cbPresentacion.setValue("Pieza");
        }

        if (item.getFactor() != null && !item.getFactor().isEmpty()) {
            txtFactor.setText(item.getFactor());
        } else {
            txtFactor.clear();
        }

        // 🔥 OPCIONAL: Cambiar texto del botón para indicar modo edición
        btnGuardar.setText("Actualizar");

        // 🔥 OPCIONAL: Agregar botón para eliminar en modo edición
        agregarBotonEliminarEnEdicion();
    }

    /**
     * 🔥 NUEVO: Agregar botón para eliminar cuando se está editando
     */
    private void agregarBotonEliminarEnEdicion() {
        // Puedes implementar esto según tu interfaz
        // Por ejemplo, agregar un botón rojo "Eliminar" al lado del botón Guardar
    }

    /**
     * Limpiar formulario para agregar otro producto
     */
    private void limpiarFormularioParaNuevo() {
        // Resetear modo edición
        modoEdicion = false;
        itemEnEdicion = null;

        // Limpiar campos
        productoController.limpiarSeleccion();
        txtCantidad.clear();
        txtDescripcion.clear();
        cbPresentacion.setValue("Pieza");
        txtFactor.clear();

        // Restaurar texto del botón si estaba en modo edición
        btnGuardar.setText("Guardar");

        // Enfocar el primer campo
        cbClaveProducto.requestFocus();

        // Mostrar mensaje indicativo
        mostrarMensajeStatus("Formulario listo para agregar otro producto");
    }

    private void mostrarMensajeStatus(String mensaje) {
        String textoOriginal = btnGuardar.getText();
        btnGuardar.setText("✓ " + mensaje);

        // Restaurar después de 2 segundos
        new Thread(() -> {
            try {
                Thread.sleep(2000);
                Platform.runLater(() -> btnGuardar.setText(textoOriginal));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    /**
     * Mostrar alerta sin bloquear (no modal)
     */
    private void mostrarAlertaSinEspera(String titulo, String mensaje) {
        Platform.runLater(() -> {
            Alert alert = new Alert(AlertType.INFORMATION);
            alert.setTitle(titulo);
            alert.setHeaderText(null);
            alert.setContentText(mensaje);
            alert.initOwner(btnGuardar.getScene().getWindow());

            // Mostrar y no esperar (non-modal)
            alert.show();

            // Cerrar automáticamente después de 2 segundos
            new Thread(() -> {
                try {
                    Thread.sleep(2000);
                    if (alert.isShowing()) {
                        Platform.runLater(() -> alert.close());
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        });
    }

    private void limpiarFormularioCompleto() {
        modoEdicion = false;
        itemEnEdicion = null;
        productoController.limpiarSeleccion();
        txtCantidad.clear();
        txtDescripcion.clear();
        cbPresentacion.setValue("Pieza");
        txtFactor.clear();
        cbClaveProducto.requestFocus();
        btnGuardar.setText("Guardar");
    }

    private void validarCantidad() {
        txtCantidad.textProperty().addListener((obs, old, val) -> {
            if (!val.matches("\\d*")) {
                txtCantidad.setText(val.replaceAll("[^\\d]", ""));
            }
        });
    }

    private void validarFactor() {
        txtFactor.textProperty().addListener((obs, old, val) -> {
            if (!val.matches("\\d*(\\.\\d*)?")) {
                txtFactor.setText(val.replaceAll("[^\\d.]", ""));

                // Evitar múltiples puntos
                if (val.chars().filter(ch -> ch == '.').count() > 1) {
                    int firstDot = val.indexOf('.');
                    txtFactor.setText(val.substring(0, firstDot + 1) +
                            val.substring(firstDot + 1).replace(".", ""));
                }
            }
        });
    }

    private void mostrarAlerta(String titulo, String mensaje) {
        Platform.runLater(() -> {
            Alert alert = new Alert(AlertType.INFORMATION);
            alert.setTitle(titulo);
            alert.setHeaderText(null);
            alert.setContentText(mensaje);
            alert.showAndWait();
        });
    }

    @FXML
    private void cerrarFormulario() {
        if (mainController != null) {
            mainController.cerrarFormulario();
        }
    }

    // Método para cargar un producto específico (para edición)
    public void cargarProducto(String id, String nombre) {
        if (productoController != null) {
            productoController.setSeleccion(id, nombre);
            actualizarDescripcionDesdeProducto();
        }
    }

    // Cargar por clave alterna
    public void cargarPorClaveAlterna(String idAlterno) {
        if (productoController != null) {
            productoController.setSeleccionPorClaveAlterna(idAlterno);
            actualizarDescripcionDesdeProducto();
        }
    }

    /**
     * Método para limpiar completamente el formulario
     */
    public void resetearFormulario() {
        limpiarFormularioCompleto();
    }
}