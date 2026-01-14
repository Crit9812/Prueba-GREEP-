package Formularios.utilities;

import Operaciones.compra.model.UbicacionCompra;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.function.Function;

public class helperCompraEmergente {

    private static final BigDecimal IVA_TASA = new BigDecimal("0.16");

    public static Function<String, String> getValidadorEnteros() {
        return valor -> valor == null ? "" : valor.replaceAll("[^\\d]", "");
    }

    public static Function<String, String> getValidadorDecimales() {
        return valor -> {
            if (valor == null) {
                return "";
            }
            String limpio = valor.replaceAll("[^\\d.]", "");
            int primerPunto = limpio.indexOf('.');
            if (primerPunto == -1) {
                return limpio;
            }
            String entero = limpio.substring(0, primerPunto + 1);
            String decimales = limpio.substring(primerPunto + 1).replace(".", "");
            return entero + decimales;
        };
    }

    public static ResultadoValidacion validarFormularioCompleto(String claveProducto,
                                                                String nombreProducto,
                                                                String cantidadTexto,
                                                                List<UbicacionCompra> ubicaciones,
                                                                String precioEntrada) {
        if (claveProducto == null || claveProducto.isBlank()
                || nombreProducto == null || nombreProducto.isBlank()) {
            return ResultadoValidacion.error("Debe seleccionar un producto válido.");
        }
        int cantidad = parseEntero(cantidadTexto);
        if (cantidad <= 0) {
            return ResultadoValidacion.error("La cantidad debe ser mayor a 0.");
        }
        if (ubicaciones == null || ubicaciones.isEmpty()) {
            return ResultadoValidacion.error("Debe capturar al menos una ubicación.");
        }
        BigDecimal precio = parseDecimal(precioEntrada);
        if (precio.compareTo(BigDecimal.ZERO) <= 0) {
            return ResultadoValidacion.error("El precio de entrada debe ser mayor a 0.");
        }
        return ResultadoValidacion.ok();
    }

    public static ResultadoCalculo calcularPrecios(String cantidadStr, String precioEntradaStr, boolean aplicaIva) {
        int cantidad = parseEntero(cantidadStr);
        if (cantidad <= 0) {
            return ResultadoCalculo.vacio();
        }

        BigDecimal precioEntrada = parseDecimal(precioEntradaStr);
        BigDecimal precioConIva = precioEntrada;
        if (aplicaIva) {
            BigDecimal iva = precioEntrada.multiply(IVA_TASA);
            precioConIva = precioEntrada.add(iva);
        }

        BigDecimal precioBruto = precioEntrada.multiply(BigDecimal.valueOf(cantidad));
        BigDecimal precioTotal = precioConIva.multiply(BigDecimal.valueOf(cantidad));

        return new ResultadoCalculo(precioConIva, precioBruto, precioTotal);
    }

    private static int parseEntero(String valor) {
        if (valor == null || valor.isBlank()) {
            return 0;
        }
        try {
            return Integer.parseInt(valor.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static BigDecimal parseDecimal(String valor) {
        if (valor == null || valor.isBlank()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(valor.trim());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    public static class ResultadoValidacion {
        private final boolean valido;
        private final String mensaje;

        private ResultadoValidacion(boolean valido, String mensaje) {
            this.valido = valido;
            this.mensaje = mensaje;
        }

        public static ResultadoValidacion ok() {
            return new ResultadoValidacion(true, "");
        }

        public static ResultadoValidacion error(String mensaje) {
            return new ResultadoValidacion(false, mensaje);
        }

        public boolean isValido() {
            return valido;
        }

        public String getMensaje() {
            return mensaje;
        }
    }

    public static class ResultadoCalculo {
        private final BigDecimal precioConIva;
        private final BigDecimal precioBruto;
        private final BigDecimal precioTotal;

        private ResultadoCalculo(BigDecimal precioConIva, BigDecimal precioBruto, BigDecimal precioTotal) {
            this.precioConIva = precioConIva;
            this.precioBruto = precioBruto;
            this.precioTotal = precioTotal;
        }

        private static ResultadoCalculo vacio() {
            return new ResultadoCalculo(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }

        public String getPrecioConIvaFormateado() {
            return formatear(precioConIva);
        }

        public String getPrecioBrutoFormateado() {
            return formatear(precioBruto);
        }

        public String getPrecioTotalFormateado() {
            return formatear(precioTotal);
        }
    }

    private static String formatear(BigDecimal valor) {
        if (valor == null) {
            return "0.00";
        }
        return valor.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
