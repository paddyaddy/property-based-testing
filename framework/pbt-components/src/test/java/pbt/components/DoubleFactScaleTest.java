package pbt.components;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import pbt.components.asp.facts.DoubleAspFactGeneratorProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DoubleFactScaleTest {

    private static com.google.gson.JsonObject params(String json) {
        return JsonParser.parseString(json).getAsJsonObject();
    }

    @Test
    void default_scale_is_1000() {
        var fg = new DoubleAspFactGeneratorProvider();

        assertEquals("out(1250).\n", fg.toFacts(1.25, "out"));
        assertEquals("out(1250).\n", fg.withParams(params("{}")).toFacts(1.25, "out"));
    }

    @Test
    void scale_comes_from_the_validator_params() {
        var fg = new DoubleAspFactGeneratorProvider().withParams(params("{\"scale\": 10}"));

        assertEquals("out(13).\n", fg.toFacts(1.25, "out"));
    }

    @Test
    void non_positive_scale_is_rejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new DoubleAspFactGeneratorProvider().withParams(params("{\"scale\": 0}")));
    }
}
