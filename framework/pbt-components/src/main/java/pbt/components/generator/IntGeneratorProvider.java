package pbt.components.generator;

import com.google.gson.JsonObject;
import pbt.spi.generator.Generator;
import pbt.spi.generator.GeneratorProvider;

import java.util.Random;

/**
 * Generates bounded {@link Integer} values. Params: {@code min} (default -1000), {@code max} (default 1000).
 */
public final class IntGeneratorProvider implements GeneratorProvider {

    public static int generateInt(Random rnd, int min, int max) {
        long span = (long) max - (long) min + 1L;
        return (int) (min + Math.floorMod(rnd.nextLong(), span));
    }

    public static int getInt(JsonObject params, String key, int defaultValue) {
        if (params == null || !params.has(key) || params.get(key).isJsonNull()) {
            return defaultValue;
        }
        return params.get(key).getAsInt();
    }

    @Override
    public String name() {
        return "int";
    }

    @Override
    public Generator<Integer> build(JsonObject params) {
        int min = getInt(params, "min", -1000);
        int max = getInt(params, "max", 1000);
        if (min > max) {
            throw new IllegalArgumentException("int generator: min > max (" + min + " > " + max + ")");
        }
        return rnd -> generateInt(rnd, min, max);
    }
}
