package Formularios.utilities;

import Operaciones.compra.model.UbicacionCompra;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

public class UbicacionManager {

    private static final String ESTILO_BOTON_ELIMINAR =
            "-fx-background-color: #d3d3d3; -fx-border-color: #999; -fx-font-weight: bold; "
                    + "-fx-cursor: hand; -fx-border-radius: 5; -fx-max-width: 25; -fx-max-height: 25; "
                    + "-fx-background-radius: 5; -fx-text-fill: black;";

    private final VBox contenedor;
    private final ComboBox<String> comboBase;
    private final TextField cantidadBase;
    private final ObservableList<String> ubicaciones;
    private final int maxFilas;
    private final Consumer<String> onError;
    private final Function<String, String> validadorEnteros;
    private final List<FilaUbicacion> filas = new ArrayList<>();

    public UbicacionManager(VBox contenedor,
                            ComboBox<String> comboBase,
                            TextField cantidadBase,
                            ObservableList<String> ubicaciones,
                            int maxFilas,
                            Consumer<String> onError,
                            Function<String, String> validadorEnteros) {
        this.contenedor = contenedor;
        this.comboBase = comboBase;
        this.cantidadBase = cantidadBase;
        this.ubicaciones = ubicaciones;
        this.maxFilas = maxFilas;
        this.onError = onError;
        this.validadorEnteros = validadorEnteros;
        inicializarBase();
    }

    public void actualizarListaUbicaciones(List<String> nuevas) {
        if (nuevas != null) {
            ubicaciones.setAll(nuevas);
        }
        if (comboBase != null) {
            comboBase.setItems(ubicaciones);
        }
        for (FilaUbicacion fila : filas) {
            fila.combo.setItems(ubicaciones);
        }
    }

    public void agregarFilaUbicacion() {
        if (filas.size() >= maxFilas) {
            notificarError("Solo se pueden agregar hasta " + maxFilas + " ubicaciones.");
            return;
        }

        HBox nuevaFila = new HBox(20);

        VBox vboxUbicacion = new VBox(5);
        ComboBox<String> combo = new ComboBox<>();
        combo.setEditable(true);
        combo.setPromptText("Escribe o selecciona una ubicación");
        combo.setItems(ubicaciones);
        vboxUbicacion.getChildren().addAll(new Label("Ubicación:"), combo);
        HBox.setHgrow(vboxUbicacion, Priority.ALWAYS);

        VBox vboxCantidad = new VBox(5);
        TextField txtCantidad = new TextField();
        vboxCantidad.getChildren().addAll(new Label("Cantidad en ubicación:"), txtCantidad);
        HBox.setHgrow(vboxCantidad, Priority.ALWAYS);

        VBox vboxBoton = new VBox(5);
        Button botonEliminar = new Button("-");
        botonEliminar.setStyle(ESTILO_BOTON_ELIMINAR);
        vboxBoton.setAlignment(Pos.BOTTOM_CENTER);
        vboxBoton.getChildren().addAll(botonEliminar);
        HBox.setHgrow(vboxBoton, Priority.ALWAYS);

        nuevaFila.getChildren().addAll(vboxUbicacion, vboxCantidad, vboxBoton);
        contenedor.getChildren().add(nuevaFila);

        FilaUbicacion fila = new FilaUbicacion(nuevaFila, combo, txtCantidad);
        filas.add(fila);
        configurarFila(fila);

        botonEliminar.setOnAction(event -> eliminarFila(fila));
    }

    public void limpiar() {
        for (int i = filas.size() - 1; i >= 1; i--) {
            contenedor.getChildren().remove(filas.get(i).contenedor);
            filas.remove(i);
        }
        if (!filas.isEmpty()) {
            limpiarFila(filas.get(0));
        }
    }

    public void cargarUbicaciones(List<UbicacionCompra> ubicacionesSeleccionadas) {
        limpiar();
        if (ubicacionesSeleccionadas == null || ubicacionesSeleccionadas.isEmpty()) {
            return;
        }

        for (int i = 0; i < ubicacionesSeleccionadas.size(); i++) {
            UbicacionCompra ubicacion = ubicacionesSeleccionadas.get(i);
            if (ubicacion == null) {
                continue;
            }
            if (i > 0) {
                agregarFilaUbicacion();
            }
            FilaUbicacion fila = filas.get(i);
            fila.combo.setValue(ubicacion.getUbicacion());
            if (fila.combo.getEditor() != null) {
                fila.combo.getEditor().setText(ubicacion.getUbicacion());
            }
            fila.cantidad.setText(String.valueOf(ubicacion.getCantidad()));
        }
    }

    public List<UbicacionCompra> obtenerUbicaciones() {
        List<UbicacionCompra> resultado = new ArrayList<>();
        for (FilaUbicacion fila : filas) {
            String ubicacion = obtenerTextoUbicacion(fila.combo);
            String cantidadTexto = fila.cantidad.getText();
            if (ubicacion.isBlank() || cantidadTexto == null || cantidadTexto.isBlank()) {
                continue;
            }
            try {
                int cantidad = Integer.parseInt(cantidadTexto.trim());
                if (cantidad > 0) {
                    resultado.add(new UbicacionCompra(ubicacion, cantidad));
                }
            } catch (NumberFormatException ignored) {
                // Ignorar cantidades inválidas
            }
        }
        return resultado;
    }

    public helperCompraEmergente.ResultadoValidacion validarUbicaciones(int cantidadTotal) {
        if (filas.isEmpty()) {
            return helperCompraEmergente.ResultadoValidacion.error("Debe capturar al menos una ubicación.");
        }

        int suma = 0;
        for (FilaUbicacion fila : filas) {
            String ubicacion = obtenerTextoUbicacion(fila.combo);
            String cantidadTexto = fila.cantidad.getText();
            if (ubicacion.isBlank() || cantidadTexto == null || cantidadTexto.isBlank()) {
                return helperCompraEmergente.ResultadoValidacion.error("Debe completar todas las ubicaciones y cantidades.");
            }
            try {
                int cantidad = Integer.parseInt(cantidadTexto.trim());
                if (cantidad <= 0) {
                    return helperCompraEmergente.ResultadoValidacion.error("Las cantidades por ubicación deben ser mayores a 0.");
                }
                suma += cantidad;
            } catch (NumberFormatException e) {
                return helperCompraEmergente.ResultadoValidacion.error("La cantidad por ubicación debe ser un número válido.");
            }
        }

        if (cantidadTotal > 0 && suma != cantidadTotal) {
            return helperCompraEmergente.ResultadoValidacion.error(
                    "La suma de cantidades por ubicación debe ser igual a la cantidad total."
            );
        }

        return helperCompraEmergente.ResultadoValidacion.ok();
    }

    public boolean tieneCamposIncompletos() {
        for (FilaUbicacion fila : filas) {
            String ubicacion = obtenerTextoUbicacion(fila.combo);
            String cantidadTexto = fila.cantidad.getText();
            if (ubicacion.isBlank() || cantidadTexto == null || cantidadTexto.isBlank()) {
                return true;
            }
        }
        return false;
    }

    private void inicializarBase() {
        if (contenedor == null || comboBase == null || cantidadBase == null) {
            return;
        }
        if (contenedor.getChildren().isEmpty() || !(contenedor.getChildren().get(0) instanceof HBox)) {
            return;
        }

        FilaUbicacion filaBase = new FilaUbicacion((HBox) contenedor.getChildren().get(0), comboBase, cantidadBase);
        filas.add(filaBase);
        comboBase.setItems(ubicaciones);
        comboBase.setEditable(true);
        configurarFila(filaBase);
    }

    private void configurarFila(FilaUbicacion fila) {
        configurarCombo(fila.combo);
        configurarCantidad(fila.cantidad);
    }

    private void configurarCombo(ComboBox<String> combo) {
        combo.setOnHidden(event -> sincronizarTexto(combo));
        combo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.isBlank()) {
                validarDuplicados(combo);
            }
        });
        if (combo.getEditor() != null) {
            combo.getEditor().focusedProperty().addListener((obs, oldVal, newVal) -> {
                if (!newVal) {
                    sincronizarTexto(combo);
                }
            });
        }
    }

    private void configurarCantidad(TextField campo) {
        campo.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) {
                return;
            }
            String validado = validadorEnteros != null ? validadorEnteros.apply(newVal) : newVal.replaceAll("[^\\d]", "");
            if (!Objects.equals(newVal, validado)) {
                campo.setText(validado);
            }
        });
    }

    private void sincronizarTexto(ComboBox<String> combo) {
        if (combo.getEditor() == null) {
            return;
        }
        String texto = combo.getEditor().getText();
        if (texto == null) {
            return;
        }
        String valor = texto.trim();
        if (valor.isBlank()) {
            combo.setValue(null);
            return;
        }
        combo.setValue(valor);
    }

    private void validarDuplicados(ComboBox<String> comboActual) {
        String valorActual = obtenerTextoUbicacion(comboActual);
        if (valorActual.isBlank()) {
            return;
        }
        for (FilaUbicacion fila : filas) {
            if (fila.combo == comboActual) {
                continue;
            }
            String otroValor = obtenerTextoUbicacion(fila.combo);
            if (valorActual.equalsIgnoreCase(otroValor)) {
                comboActual.setValue(null);
                if (comboActual.getEditor() != null) {
                    comboActual.getEditor().clear();
                }
                notificarError("No se puede seleccionar la misma ubicación más de una vez.");
                return;
            }
        }
    }

    private String obtenerTextoUbicacion(ComboBox<String> combo) {
        if (combo == null) {
            return "";
        }
        String valor = combo.getValue();
        if (valor != null && !valor.isBlank()) {
            return valor.trim();
        }
        if (combo.getEditor() != null && combo.getEditor().getText() != null) {
            return combo.getEditor().getText().trim();
        }
        return "";
    }

    private void eliminarFila(FilaUbicacion fila) {
        if (filas.size() <= 1) {
            return;
        }
        contenedor.getChildren().remove(fila.contenedor);
        filas.remove(fila);
    }

    private void limpiarFila(FilaUbicacion fila) {
        fila.combo.setValue(null);
        if (fila.combo.getEditor() != null) {
            fila.combo.getEditor().clear();
        }
        fila.cantidad.clear();
    }

    private void notificarError(String mensaje) {
        if (onError != null) {
            onError.accept(mensaje);
        }
    }

    private static class FilaUbicacion {
        private final HBox contenedor;
        private final ComboBox<String> combo;
        private final TextField cantidad;

        private FilaUbicacion(HBox contenedor, ComboBox<String> combo, TextField cantidad) {
            this.contenedor = contenedor;
            this.combo = combo;
            this.cantidad = cantidad;
        }
    }
}
