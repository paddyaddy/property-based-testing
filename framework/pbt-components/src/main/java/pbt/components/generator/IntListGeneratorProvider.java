package pbt.components.generator;

import com.google.gson.JsonObject;
import pbt.spi.generator.Generator;
import pbt.spi.generator.GeneratorProvider;

import java.util.ArrayList;
import java.util.List;

/**
 * Generates {@code List<Integer>} with configurable length and value bounds.
 * Params: {@code minLen} (0), {@code maxLen} (10), {@code minValue} (-100), {@code maxValue} (100).
 */
public final class IntListGeneratorProvider implements GeneratorProvider {

    @Override
    public String name() {
        return "intList";
    }

    @Override
    public Generator<List<Integer>> build(JsonObject params) {
        int minLen = IntGeneratorProvider.getInt(params, "minLen", 0);
        int maxLen = IntGeneratorProvider.getInt(params, "maxLen", 10);
        int minValue = IntGeneratorProvider.getInt(params, "minValue", -100);
        int maxValue = IntGeneratorProvider.getInt(params, "maxValue", 100);
        if (minLen < 0 || minLen > maxLen) {
            throw new IllegalArgumentException("intList: invalid length bounds " + minLen + ".." + maxLen);
        }
        if (minValue > maxValue) {
            throw new IllegalArgumentException("intList: invalid value bounds " + minValue + ".." + maxValue);
        }
        return rnd -> {
            int len = minLen == maxLen ? minLen : minLen + rnd.nextInt(maxLen - minLen + 1);
            List<Integer> xs = new ArrayList<>(len);
            for (int i = 0; i < len; i++) {
                xs.add(IntGeneratorProvider.generateInt(rnd, minValue, maxValue));
            }
            return xs;
        };
    }
}
