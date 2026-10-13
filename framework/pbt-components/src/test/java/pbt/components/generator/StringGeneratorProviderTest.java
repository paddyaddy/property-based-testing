package pbt.components.generator;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;
import pbt.spi.generator.Generator;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StringGeneratorProviderTest {

    @SuppressWarnings("unchecked")
    private Generator<String> build(JsonObject params) {
        return (Generator<String>) new StringGeneratorProvider().build(params);
    }

    @Test
    void generates_within_length_bounds() {
        JsonObject params = new JsonObject();
        params.addProperty("minLen", 2);
        params.addProperty("maxLen", 5);
        Generator<String> gen = build(params);
        Random rnd = new Random(0);
        for (int i = 0; i < 200; i++) {
            String s = gen.generate(rnd);
            assertTrue(s.length() >= 2 && s.length() <= 5, "length out of bounds: " + s.length());
        }
    }

    @Test
    void generates_only_chars_from_set() {
        JsonObject params = new JsonObject();
        params.addProperty("chars", "abc");
        params.addProperty("minLen", 1);
        params.addProperty("maxLen", 10);
        Generator<String> gen = build(params);
        Random rnd = new Random(1);
        for (int i = 0; i < 100; i++) {
            for (char c : gen.generate(rnd).toCharArray()) {
                assertTrue(c == 'a' || c == 'b' || c == 'c', "unexpected char: " + c);
            }
        }
    }

    @Test
    void rejects_inverted_length_bounds() {
        JsonObject params = new JsonObject();
        params.addProperty("minLen", 5);
        params.addProperty("maxLen", 2);
        assertThrows(IllegalArgumentException.class, () -> build(params));
    }

    @Test
    void rejects_empty_char_set() {
        JsonObject params = new JsonObject();
        params.addProperty("chars", "");
        assertThrows(IllegalArgumentException.class, () -> build(params));
    }
}
