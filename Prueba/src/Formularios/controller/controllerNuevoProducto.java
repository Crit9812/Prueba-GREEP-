package Formularios.controller;

import Consultas.producto.model.producto;
import conexion.conexionFTP;
import conexion.Conexion;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.sql.*;

public class controllerNuevoProducto {

    @FXML private TextField txtIdProducto;
    @FXML private TextField txtNombre;
    @FXML private ComboBox<String> cmbCategoria;
    @FXML private ComboBox<String> cmbMarca;
    @FXML private TextField txtMaterial;
    @FXML private TextField txtUnidadMedida;
    @FXML private TextArea txtDescripcion;
    @FXML private TextField txtInventarioMin;
    @FXML private Button btnGuardar;
    @FXML private ImageView imageView;
    @FXML private Button btnSeleccionarImagen;
    @FXML private ComboBox<String> cmbEtiqueta;

    private File imagenSeleccionada;
    private boolean modoEdicion = false;
    private String idEdicion = "";
    private String nombreImagenActual = "";
    private Runnable onSaved = null;

    private Conexion conexionDB = new Conexion();

    public void setOnSaved(Runnable r) { this.onSaved = r; }

    @FXML
    public void initialize() {
        imageView.setFitWidth(150);
        imageView.setFitHeight(150);
        imageView.setPreserveRatio(true);
        imageView.setStyle("-fx-border-color: #ccc; -fx-border-width: 1px;");

        // Inicializar ComboBoxes
        cmbCategoria.setEditable(false); // solo elegir
        cmbMarca.setEditable(true);      // puede escribir
        cmbEtiqueta.setEditable(true);   // puede escribir

        cargarCategorias();
        cargarMarcas();
        cargarEtiquetas();
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
        ObservableList<String> items = FXCollections.observableArrayList();
        String sql = "SELECT nombre FROM marcas ORDER BY nombre";
        try (Connection con = conexionDB.conectar();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                items.add(rs.getString("nombre"));
            }
            cmbMarca.setItems(items);
        } catch (Exception e) {
            System.out.println("Error cargando marcas: " + e.getMessage());
        }
    }

    private void cargarEtiquetas() {
        ObservableList<String> items = FXCollections.observableArrayList();
        String sql = "SELECT nombre FROM etiquetas ORDER BY nombre";
        try (Connection con = conexionDB.conectar();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                items.add(rs.getString("nombre"));
            }
            cmbEtiqueta.setItems(items);
        } catch (Exception e) {
            System.out.println("Error cargando etiquetas: " + e.getMessage());
        }
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
        txtNombre.setText(p.getNombreProducto());

        // categoria es almacenada como nombre en productos -> setea directamente si existe en la lista
        cmbCategoria.getSelectionModel().select(p.getCategoria());

        // Para etiqueta y marca, si en producto se guarda el id, buscamos el nombre
        try {
            String etiquetaId = p.getEtiqueta();
            if (etiquetaId != null && !etiquetaId.isEmpty()) {
                String nombreEtiqueta = getNombreEtiquetaById(etiquetaId);
                if (nombreEtiqueta != null) cmbEtiqueta.getSelectionModel().select(nombreEtiqueta);
            }
            String marcaId = p.getMarca();
            if (marcaId != null && !marcaId.isEmpty()) {
                String nombreMarca = getNombreMarcaById(marcaId);
                if (nombreMarca != null) cmbMarca.getSelectionModel().select(nombreMarca);
            }
        } catch (Exception ex) {
            System.out.println("Error al obtener nombre de marca/etiqueta: " + ex.getMessage());
        }

        txtMaterial.setText(p.getMaterial());
        txtUnidadMedida.setText(p.getUnidadMedida());
        txtDescripcion.setText(p.getDescripcion());
        txtInventarioMin.setText(String.valueOf(p.getInventarioMin()));

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
            String idProducto = txtIdProducto.getText().trim();
            if (idProducto.isEmpty()) { mostrarError("El ID es obligatorio"); return; }

            if (txtNombre.getText().trim().isEmpty()) { mostrarError("El nombre es requerido"); return; }

            String nombre = txtNombre.getText().trim();

            String categoria = cmbCategoria.getSelectionModel().getSelectedItem();
            if (categoria == null) categoria = ""; // si no selecciona nada

            // Para etiqueta y marca: el ComboBox es editable. Tomamos lo que haya (puede ser texto escrito)
            String etiquetaTexto = cmbEtiqueta.getEditor().getText().trim();
            String marcaTexto = cmbMarca.getEditor().getText().trim();

            // Resolver/insertar/actualizar y obtener los ids para guardar en productos
            String etiquetaId = null;
            if (!etiquetaTexto.isEmpty()) {
                etiquetaId = resolveOrCreateEtiqueta(etiquetaTexto); // retorna id como String
            }

            String marcaId = null;
            if (!marcaTexto.isEmpty()) {
                marcaId = resolveOrCreateMarca(marcaTexto); // retorna id como String
            }

            String material = txtMaterial.getText().trim();
            String unidadMedida = txtUnidadMedida.getText().trim();
            String descripcion = txtDescripcion.getText().trim();
            int inventarioMin = txtInventarioMin.getText().isEmpty() ? 0 :
                    Integer.parseInt(txtInventarioMin.getText());

            Formularios.model.modelNuevoProducto modelo = new Formularios.model.modelNuevoProducto();
            boolean resultado;

            String nombreImagen = nombreImagenActual;

            if (imagenSeleccionada != null) {
                String extension = getFileExtension(imagenSeleccionada.getName());
                String nuevoNombre = idProducto + extension;

                conexionFTP ftp = new conexionFTP();
                if (ftp.uploadFile(imagenSeleccionada, nuevoNombre)) {
                    nombreImagen = nuevoNombre;
                }
            }

            // Pasamos los ids (o null/empty) en los parámetros etiqueta y marca
            String etiquetaParaGuardar = etiquetaId == null ? "" : etiquetaId;
            String marcaParaGuardar = marcaId == null ? "" : marcaId;

            if (modoEdicion) {
                resultado = modelo.modificarProducto(idEdicion, nombre, categoria, etiquetaParaGuardar, marcaParaGuardar, material,
                        unidadMedida, descripcion, inventarioMin, nombreImagen);
            } else {
                resultado = modelo.guardarProducto(idProducto, nombre, categoria, etiquetaParaGuardar, marcaParaGuardar, material,
                        unidadMedida, descripcion, inventarioMin, nombreImagen);
            }

            if (resultado) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Éxito");
                alert.setHeaderText(null);
                alert.setContentText(modoEdicion ? "Producto actualizado" : "Producto guardado");
                alert.showAndWait();
                // refrescar listas (en caso de insertar nuevas marcas/etiquetas)
                cargarMarcas();
                cargarEtiquetas();
                if (onSaved != null) onSaved.run();
                Stage stage = (Stage) btnGuardar.getScene().getWindow();
                stage.close();
            } else {
                mostrarError("No se pudo guardar el producto");
            }

        } catch (NumberFormatException e) {
            mostrarError("Inventario mínimo inválido");
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

    // ---------- Helpers DB para Marcas / Etiquetas ----------

    // Resuelve o crea la marca. Retorna el id (String).
    // Si existe por nombre (case-insensitive) -> actualiza su nombre con el valor dado (sobrescribir)
    // Si no existe -> inserta y devuelve id generado.
    private String resolveOrCreateMarca(String nombreEscrito) {
        String id = findIdByName("marcas", nombreEscrito);
        if (id != null) {
            // sobrescribir nombre real con la nueva escritura (para normalizar mayúsc/minúsc)
            updateNameById("marcas", id, nombreEscrito);
            return id;
        } else {
            return insertAndGetId("marcas", nombreEscrito);
        }
    }

    private String resolveOrCreateEtiqueta(String nombreEscrito) {
        String id = findIdByName("etiquetas", nombreEscrito);
        if (id != null) {
            updateNameById("etiquetas", id, nombreEscrito);
            return id;
        } else {
            return insertAndGetId("etiquetas", nombreEscrito);
        }
    }

    // Busca id por nombre (case-insensitive). Retorna id como String o null si no existe.
    private String findIdByName(String tabla, String nombre) {
        String sql = "SELECT id FROM " + tabla + " WHERE LOWER(nombre) = LOWER(?) LIMIT 1";
        try (Connection con = conexionDB.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nombre.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return String.valueOf(rs.getInt("id"));
                }
            }
        } catch (Exception e) {
            System.out.println("Error findIdByName (" + tabla + "): " + e.getMessage());
        }
        return null;
    }

    // Actualiza el nombre por id (sobrescribe)
    private void updateNameById(String tabla, String id, String nuevoNombre) {
        String sql = "UPDATE " + tabla + " SET nombre = ? WHERE id = ?";
        try (Connection con = conexionDB.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nuevoNombre.trim());
            ps.setInt(2, Integer.parseInt(id));
            ps.executeUpdate();
        } catch (Exception e) {
            System.out.println("Error updateNameById (" + tabla + "): " + e.getMessage());
        }
    }

    // Inserta y retorna el id generado (como String).
    private String insertAndGetId(String tabla, String nombre) {
        String sql = "INSERT INTO " + tabla + " (nombre) VALUES (?)";
        try (Connection con = conexionDB.conectar();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, nombre.trim());
            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        return String.valueOf(keys.getInt(1));
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Error insertAndGetId (" + tabla + "): " + e.getMessage());
        }
        return null;
    }

    // Obtiene nombre por id (para mostrar en ComboBox al cargar un producto)
    private String getNombreMarcaById(String id) {
        String sql = "SELECT nombre FROM marcas WHERE id = ? LIMIT 1";
        try (Connection con = conexionDB.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, Integer.parseInt(id));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("nombre");
            }
        } catch (Exception e) {
            System.out.println("Error getNombreMarcaById: " + e.getMessage());
        }
        return null;
    }

    private String getNombreEtiquetaById(String id) {
        String sql = "SELECT nombre FROM etiquetas WHERE id = ? LIMIT 1";
        try (Connection con = conexionDB.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, Integer.parseInt(id));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("nombre");
            }
        } catch (Exception e) {
            System.out.println("Error getNombreEtiquetaById: " + e.getMessage());
        }
        return null;
    }
}
