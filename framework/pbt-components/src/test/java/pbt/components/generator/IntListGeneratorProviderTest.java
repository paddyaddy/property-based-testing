package pbt.components.generator;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;
import pbt.spi.generator.Generator;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertTrue;

class IntListGeneratorProviderTest {

    @SuppressWarnings("unchecked")
    private Generator<List<Integer>> build(int minLen, int maxLen, int minValue, int maxValue) {
        JsonObject params = new JsonObject();
        params.addProperty("minLen", minLen);
        params.addProperty("maxLen", maxLen);
        params.addProperty("minValue", minValue);
        params.addProperty("maxValue", maxValue);
        return (Generator<List<Integer>>) new IntListGeneratorProvider().build(params);
    }

    @Test
    void generates_lists_within_length_and_value_bounds() {
        Generator<List<Integer>> gen = build(2, 6, -10, 10);
        Random rnd = new Random(0);
        for (int i = 0; i < 200; i++) {
            List<Integer> xs = gen.generate(rnd);
            assertTrue(xs.size() >= 2 && xs.size() <= 6, "length out of bounds: " + xs.size());
            for (int v : xs) {
                assertTrue(v >= -10 && v <= 10, "element out of bounds: " + v);
            }
        }
    }

    @Test
    void generates_empty_lists_when_minLen_is_zero() {
        Generator<List<Integer>> gen = build(0, 5, 0, 10);
        Random rnd = new Random(42);
        boolean sawEmpty = false;
        for (int i = 0; i < 500; i++) {
            if (gen.generate(rnd).isEmpty()) {
                sawEmpty = true;
                break;
            }
        }
        assertTrue(sawEmpty, "expected at least one empty list");
    }
}
