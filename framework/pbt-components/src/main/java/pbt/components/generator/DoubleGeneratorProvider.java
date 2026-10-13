package pbt.components.generator;

import com.google.gson.JsonObject;
import pbt.spi.generator.Generator;
import pbt.spi.generator.GeneratorProvider;

/**
 * Generates bounded {@link Double} values. Params: {@code min} (default -1000.0), {@code max} (default 1000.0).
 */
public final class DoubleGeneratorProvider implements GeneratorProvider {

    public static double getDouble(JsonObject params, String key, double defaultValue) {
        if (params == null || !params.has(key) || params.get(key).isJsonNull()) {
            return defaultValue;
        }
        return params.get(key).getAsDouble();
    }

    @Override
    public String name() {
        return "double";
    }

    @Override
    public Generator<Double> build(JsonObject params) {
        double min = getDouble(params, "min", -1000.0);
        double max = getDouble(params, "max", 1000.0);
        if (min > max) {
            throw new IllegalArgumentException("double generator: min > max (" + min + " > " + max + ")");
        }
        return rnd -> min + (max - min) * rnd.nextDouble();
    }
}
