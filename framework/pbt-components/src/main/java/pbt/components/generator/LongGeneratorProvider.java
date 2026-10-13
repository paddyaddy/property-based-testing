package pbt.components.generator;

import com.google.gson.JsonObject;
import pbt.spi.generator.Generator;
import pbt.spi.generator.GeneratorProvider;

import java.util.Random;

/**
 * Generates bounded {@link Long} values. Params: {@code min} (default -1000), {@code max} (default 1000).
 */
public final class LongGeneratorProvider implements GeneratorProvider {

    public static long generateLong(Random rnd, long min, long max) {
        long span = max - min + 1L;
        return min + Math.floorMod(rnd.nextLong(), span);
    }

    public static long getLong(JsonObject params, String key, long defaultValue) {
        if (params == null || !params.has(key) || params.get(key).isJsonNull()) {
            return defaultValue;
        }
        return params.get(key).getAsLong();
    }

    @Override
    public String name() {
        return "long";
    }

    @Override
    public Generator<Long> build(JsonObject params) {
        long min = getLong(params, "min", -1000L);
        long max = getLong(params, "max", 1000L);
        if (min > max) {
            throw new IllegalArgumentException("long generator: min > max (" + min + " > " + max + ")");
        }
        return rnd -> generateLong(rnd, min, max);
    }
}
