package pbt.spi.type;

import java.util.ArrayList;
import java.util.List;

/**
 * The input of a system under test that takes several arguments: one value per parameter,
 * in declaration order. SUT runners spread a {@code Tuple} over the parameters of the
 * method; any other input is passed as the single argument.
 */
public record Tuple(List<Object> values) {

    public Tuple {
        values = List.copyOf(values);
    }

    /**
     * Renders strings quoted and every other value with {@link String#valueOf}.
     */
    public static String describe(Object value) {
        return value instanceof String s ? '"' + s + '"' : String.valueOf(value);
    }

    @Override
    public String toString() {
        List<String> parts = new ArrayList<>(values.size());
        for (Object v : values) {
            parts.add(describe(v));
        }
        return "(" + String.join(", ", parts) + ")";
    }
}
