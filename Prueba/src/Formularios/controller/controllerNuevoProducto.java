package Formularios.controller;

import Consultas.producto.model.producto;
import Consultas.producto.model.etiqueta;
import Consultas.producto.model.marca;
import Consultas.producto.model.model;
import Consultas.clasificacion.model.unidades_Medida;
import Formularios.model.modelNuevoProducto;
import conexion.conexionFTP;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.io.File;

public class controllerNuevoProducto {

    @FXML private TextField txtIdProducto;
    @FXML private TextField txtNombre;
    @FXML private ComboBox<String> cmbCategoria;
    @FXML private ComboBox<marca> cmbMarca;
    @FXML private TextField txtMaterial;
    @FXML private ComboBox<unidades_Medida> cmbUnidadMedida;
    @FXML private TextArea txtDescripcion;
    @FXML private TextField txtInventarioMin;
    @FXML private Button btnGuardar;
    @FXML private ImageView imageView;
    @FXML private Button btnSeleccionarImagen;
    @FXML private ComboBox<etiqueta> cmbEtiqueta;

    private File imagenSeleccionada;
    private boolean modoEdicion = false;
    private String idEdicion = "";
    private String nombreImagenActual = "";
    private Runnable onSaved = null;

    private modelNuevoProducto modeloFormulario;
    private model modeloConsulta;

    private String productoIdCreado = "";
    private String productoNombreCreado = "";

    public void setOnSaved(Runnable r) { this.onSaved = r; }

    @FXML
    public void initialize() {
        // Inicializar modelos
        modeloFormulario = new modelNuevoProducto();
        modeloConsulta = new model();

        // Configurar ImageView
        imageView.setFitWidth(150);
        imageView.setFitHeight(150);
        imageView.setPreserveRatio(true);
        imageView.setStyle("-fx-border-color: #ccc; -fx-border-width: 1px;");

        // Inicializar ComboBoxes
        cmbCategoria.setEditable(false); // solo elegir
        cmbMarca.setEditable(true);      // puede escribir
        cmbEtiqueta.setEditable(true);   // puede escribir
        cmbUnidadMedida.setEditable(true);

        // Cargar datos
        cargarCategorias();
        cargarMarcas();
        cargarEtiquetas();
        cargarUnidadesMedida();

        // Configurar cómo mostrar las etiquetas y marcas en los ComboBox
        configurarComboBoxes();

        // Configurar atajo de teclado ENTER
        btnGuardar.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.setOnKeyPressed(event -> {
                    switch (event.getCode()) {
                        case ENTER -> guardarProducto();
                    }
                });
            }
        });
    }

    private void configurarComboBoxes() {
        // Configurar ComboBox de Marca
        cmbMarca.setConverter(new StringConverter<marca>() {
            @Override
            public String toString(marca marca) {
                return marca == null ? "" : marca.getNombre();
            }

            @Override
            public marca fromString(String string) {
                return cmbMarca.getItems().stream()
                        .filter(m -> m.getNombre().equalsIgnoreCase(string))
                        .findFirst()
                        .orElse(null);
            }
        });

        // Configurar ComboBox de Etiqueta
        cmbEtiqueta.setConverter(new StringConverter<etiqueta>() {
            @Override
            public String toString(etiqueta etiqueta) {
                return etiqueta == null ? "" : etiqueta.getNombre();
            }

            @Override
            public etiqueta fromString(String string) {
                return cmbEtiqueta.getItems().stream()
                        .filter(e -> e.getNombre().equalsIgnoreCase(string))
                        .findFirst()
                        .orElse(null);
            }
        });

        // Configurar ComboBox de Unidad de Medida
        cmbUnidadMedida.setConverter(new StringConverter<unidades_Medida>() {
            @Override
            public String toString(unidades_Medida unidad) {
                return unidad == null ? "" : unidad.getNombre();
            }

            @Override
            public unidades_Medida fromString(String string) {
                return cmbUnidadMedida.getItems().stream()
                        .filter(u -> u.getNombre().equalsIgnoreCase(string))
                        .findFirst()
                        .orElse(null);
            }
        });
    }

    private void cargarCategorias() {
        ObservableList<String> items = FXCollections.observableArrayList(
                "cristaleria",
                "consumibles",
                "equipo",
                "reactivos"
        );
        cmbCategoria.setItems(items);
    }

    private void cargarMarcas() {
        ObservableList<marca> marcas = modeloConsulta.obtenerListaMarcas();
        cmbMarca.setItems(marcas);
    }

    private void cargarEtiquetas() {
        ObservableList<etiqueta> etiquetas = modeloConsulta.obtenerListaEtiquetas();
        cmbEtiqueta.setItems(etiquetas);
    }

    private void cargarUnidadesMedida() {
        ObservableList<unidades_Medida> unidades = FXCollections.observableArrayList(
                modeloFormulario.obtenerUnidadesMedida()
        );
        cmbUnidadMedida.setItems(unidades);
    }

    @FXML
    public void seleccionarImagen() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Seleccionar imagen del producto");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Imágenes", "*.jpg","*.jpeg","*.png","*.gif","*.bmp")
        );

        File archivo = fileChooser.showOpenDialog(null);
        if (archivo != null) {
            if (archivo.length() > 5*1024*1024) {
                mostrarError("La imagen no debe superar los 5MB");
                return;
            }
            imagenSeleccionada = archivo;
            imageView.setImage(new Image(archivo.toURI().toString()));
        }
    }

    public void cargarProducto(producto p) {
        if (p == null) return;
        modoEdicion = true;
        idEdicion = p.getIdProducto();
        nombreImagenActual = p.getUrlImagen();

        txtIdProducto.setText(p.getIdProducto());
        txtIdProducto.setDisable(true); // No permitir editar ID en modo edición

        txtNombre.setText(p.getNombreProducto());
        cmbCategoria.getSelectionModel().select(p.getCategoria());
        txtMaterial.setText(p.getMaterial());
        txtDescripcion.setText(p.getDescripcion());
        txtInventarioMin.setText(String.valueOf(p.getInventarioMin()));

        // Cargar etiqueta
        if (p.getEtiqueta() != null && !p.getEtiqueta().isEmpty()) {
            etiqueta etiqueta = modeloFormulario.obtenerEtiquetaPorId(p.getEtiqueta());
            if (etiqueta != null) {
                cmbEtiqueta.getSelectionModel().select(etiqueta);
            }
        }

        // Cargar marca
        if (p.getMarca() != null && !p.getMarca().isEmpty()) {
            marca marca = modeloFormulario.obtenerMarcaPorId(p.getMarca());
            if (marca != null) {
                cmbMarca.getSelectionModel().select(marca);
            }
        }

        if (p.getUnidadMedida() != null && !p.getUnidadMedida().isEmpty()) {
            unidades_Medida unidad = modeloFormulario.obtenerUnidadMedidaPorId(p.getUnidadMedida());
            if (unidad != null) {
                cmbUnidadMedida.getSelectionModel().select(unidad);
            } else {
                cmbUnidadMedida.getEditor().setText(p.getUnidadMedida());
            }
        }

        // Cargar imagen si existe
        if (nombreImagenActual != null && !nombreImagenActual.isEmpty()) {
            try {
                conexionFTP ftp = new conexionFTP();
                Image imagen = ftp.getImageFromFTP(nombreImagenActual);
                imageView.setImage(imagen);
            } catch (Exception e) {
                System.out.println("Error cargando imagen desde servidor: " + e.getMessage());
            }
        }

        btnGuardar.setText("Actualizar");
    }

    @FXML
    public void guardarProducto() {
        try {
            // Validar campos obligatorios
            if (modoEdicion) {
                // En modo edición, el ID ya está establecido
                if (idEdicion == null || idEdicion.isEmpty()) {
                    mostrarError("Error: No se encontró ID del producto a editar");
                    return;
                }
            } else {
                // En modo nuevo, validar ID
                String idProducto = txtIdProducto.getText().trim();
                if (idProducto.isEmpty()) {
                    mostrarError("El ID es obligatorio");
                    return;
                }

                // Verificar si el ID ya existe
                producto existente = modeloFormulario.buscarProductoPorId(idProducto);
                if (existente != null) {
                    mostrarError("Ya existe un producto con este ID");
                    return;
                }
            }

            if (txtNombre.getText().trim().isEmpty()) {
                mostrarError("El nombre es requerido");
                return;
            }

            // Obtener valores de los campos
            String nombre = txtNombre.getText().trim();
            String categoria = cmbCategoria.getSelectionModel().getSelectedItem();
            if (categoria == null) categoria = "";

            String material = txtMaterial.getText().trim();
            String unidadMedidaId = "";
            String unidadTexto = cmbUnidadMedida.getEditor().getText().trim();
            if (!unidadTexto.isEmpty()) {
                unidadMedidaId = modeloFormulario.crearOActualizarUnidadMedida(unidadTexto);
                if (unidadMedidaId == null) {
                    mostrarError("Error al procesar la unidad de medida");
                    return;
                }
            }
            String descripcion = txtDescripcion.getText().trim();

            int inventarioMin = 0;
            if (!txtInventarioMin.getText().isEmpty()) {
                try {
                    inventarioMin = Integer.parseInt(txtInventarioMin.getText());
                } catch (NumberFormatException e) {
                    mostrarError("El inventario mínimo debe ser un número válido");
                    return;
                }
            }

            // Manejar etiqueta
            String etiquetaId = "";
            String etiquetaTexto = cmbEtiqueta.getEditor().getText().trim();
            if (!etiquetaTexto.isEmpty()) {
                etiquetaId = modeloFormulario.crearOActualizarEtiqueta(etiquetaTexto);
                if (etiquetaId == null) {
                    mostrarError("Error al procesar la etiqueta");
                    return;
                }
            }

            // Manejar marca
            String marcaId = "";
            String marcaTexto = cmbMarca.getEditor().getText().trim();
            if (!marcaTexto.isEmpty()) {
                marcaId = modeloFormulario.crearOActualizarMarca(marcaTexto);
                if (marcaId == null) {
                    mostrarError("Error al procesar la marca");
                    return;
                }
            }

            // Manejar imagen
            String nombreImagen = nombreImagenActual;
            if (imagenSeleccionada != null) {
                String extension = getFileExtension(imagenSeleccionada.getName());
                String nuevoNombre = (modoEdicion ? idEdicion : txtIdProducto.getText().trim()) + extension;

                conexionFTP ftp = new conexionFTP();
                if (ftp.uploadFile(imagenSeleccionada, nuevoNombre)) {
                    nombreImagen = nuevoNombre;
                } else {
                    mostrarError("Error al subir la imagen al servidor");
                    return;
                }
            }

            // Crear objeto producto
            producto p = new producto();

            if (modoEdicion) {
                p.setIdProducto(idEdicion);
            } else {
                p.setIdProducto(txtIdProducto.getText().trim());
            }

            p.setNombreProducto(nombre);
            p.setCategoria(categoria);
            p.setEtiqueta(etiquetaId);
            p.setMarca(marcaId);
            p.setMaterial(material);
            p.setUnidadMedida(unidadMedidaId);
            p.setDescripcion(descripcion);
            p.setInventarioMin(inventarioMin);
            p.setUrlImagen(nombreImagen);

            boolean resultado;
            if (modoEdicion) {
                resultado = modeloFormulario.modificarProducto(p);
            } else {
                resultado = modeloFormulario.guardarProducto(p);
            }

            /*if (resultado) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Éxito");
                alert.setHeaderText(null);
                alert.setContentText(modoEdicion ? "Producto actualizado" : "Producto guardado");
                alert.showAndWait();

                // Refrescar comboboxes si se agregaron nuevas etiquetas/marcas
                cargarMarcas();
                cargarEtiquetas();
                cargarUnidadesMedida();

                // Ejecutar callback para refrescar la tabla principal
                if (onSaved != null) onSaved.run();

                // Cerrar ventana
                Stage stage = (Stage) btnGuardar.getScene().getWindow();
                stage.close();
            } else {
                mostrarError("No se pudo guardar el producto");
            }*/
            if (resultado) {
                // ALMACENAR DATOS DEL PRODUCTO CREADO
                if (!modoEdicion) {  // Solo para nuevos productos, no para ediciones
                    productoIdCreado = p.getIdProducto();
                    productoNombreCreado = p.getNombreProducto();
                }

                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Éxito");
                alert.setHeaderText(null);
                alert.setContentText(modoEdicion ? "Producto actualizado" : "Producto guardado");
                alert.showAndWait();

                // Refrescar comboboxes si se agregaron nuevas etiquetas/marcas
                cargarMarcas();
                cargarEtiquetas();
                cargarUnidadesMedida();

                // Ejecutar callback para refrescar la tabla principal
                if (onSaved != null) onSaved.run();

                // Cerrar ventana
                Stage stage = (Stage) btnGuardar.getScene().getWindow();
                stage.close();
            }

        } catch (Exception e) {
            mostrarError("Error al guardar: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String getFileExtension(String fileName) {
        int idx = fileName.lastIndexOf(".");
        return idx == -1 ? ".jpg" : fileName.substring(idx).toLowerCase();
    }

    private void mostrarError(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    public String getProductoIdCreado() {
        return productoIdCreado;
    }

    public String getProductoNombreCreado() {
        return productoNombreCreado;
    }

    // AÑADIR este método para verificar si se creó un producto
    public boolean isProductoCreado() {
        return !productoIdCreado.isEmpty() && !productoNombreCreado.isEmpty();
    }
}
