package Compartido.config;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.prefs.Preferences;

public final class ConfiguracionFiscal {

    private static final String PREF_NODE = "greep/configuracion";
    private static final String PREF_IVA_PORCENTAJE = "ivaPorcentaje";
    private static final double IVA_PORCENTAJE_DEFAULT = 16.0;

    private static final Preferences PREFERENCES = Preferences.userRoot().node(PREF_NODE);
    private static final DoubleProperty ivaPorcentaje =
            new SimpleDoubleProperty(validarPorcentaje(PREFERENCES.getDouble(PREF_IVA_PORCENTAJE, IVA_PORCENTAJE_DEFAULT)));

    static {
        ivaPorcentaje.addListener((obs, oldVal, newVal) ->
                PREFERENCES.putDouble(PREF_IVA_PORCENTAJE, validarPorcentaje(newVal.doubleValue())));
    }

    private ConfiguracionFiscal() {
    }

    public static DoubleProperty ivaPorcentajeProperty() {
        return ivaPorcentaje;
    }

    public static double getIvaPorcentaje() {
        return ivaPorcentaje.get();
    }

    public static BigDecimal getIvaTasaDecimal() {
        return BigDecimal.valueOf(getIvaPorcentaje())
                .divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);
    }

    public static void setIvaPorcentaje(double porcentaje) {
        ivaPorcentaje.set(validarPorcentaje(porcentaje));
    }

    private static double validarPorcentaje(double porcentaje) {
        if (Double.isNaN(porcentaje) || Double.isInfinite(porcentaje)) {
            return IVA_PORCENTAJE_DEFAULT;
        }
        if (porcentaje < 0) {
            return 0;
        }
        if (porcentaje > 100) {
            return 100;
        }
        return porcentaje;
    }
}
