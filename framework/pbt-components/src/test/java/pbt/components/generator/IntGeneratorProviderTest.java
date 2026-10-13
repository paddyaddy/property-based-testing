package pbt.components.generator;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;
import pbt.spi.generator.Generator;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IntGeneratorProviderTest {

    private IntGeneratorProvider provider() {
        return new IntGeneratorProvider();
    }

    @Test
    void generates_within_explicit_bounds() {
        JsonObject params = new JsonObject();
        params.addProperty("min", -5);
        params.addProperty("max", 5);
        Generator<Integer> gen = (Generator<Integer>) provider().build(params);
        Random rnd = new Random(0);
        for (int i = 0; i < 200; i++) {
            int v = gen.generate(rnd);
            assertTrue(v >= -5 && v <= 5, "out of bounds: " + v);
        }
    }

    @Test
    void generates_with_default_bounds_when_params_empty() {
        Generator<Integer> gen = (Generator<Integer>) provider().build(new JsonObject());
        Random rnd = new Random(1);
        for (int i = 0; i < 200; i++) {
            int v = gen.generate(rnd);
            assertTrue(v >= -1000 && v <= 1000, "out of default bounds: " + v);
        }
    }

    @Test
    void rejects_inverted_bounds() {
        JsonObject params = new JsonObject();
        params.addProperty("min", 10);
        params.addProperty("max", 5);
        assertThrows(IllegalArgumentException.class, () -> provider().build(params));
    }
}
