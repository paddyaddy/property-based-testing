package pbt.components.asp.facts;

import com.google.gson.JsonObject;
import pbt.spi.asp.facts.AspFactGeneratorProvider;

/**
 * Renders a {@link Double} as a fixed-point integer fact: {@code prefix(N).}
 * where N = round(value × scale). Clingo is integer-only, so ASP programs
 * for {@code double} values must interpret facts as value/scale.
 *
 * <p>The scale defaults to {@value #DEFAULT_SCALE} and can be set per property with a
 * {@code "scale"} parameter in the {@code validator} object.
 */
public final class DoubleAspFactGeneratorProvider implements AspFactGeneratorProvider {

    public static final int DEFAULT_SCALE = 1000;

    private final int scale;

    public DoubleAspFactGeneratorProvider() {
        this(DEFAULT_SCALE);
    }

    private DoubleAspFactGeneratorProvider(int scale) {
        this.scale = scale;
    }

    @Override
    public String name() {
        return "double";
    }

    @Override
    public String toFacts(Object value, String prefix) {
        if (value == null) return "";
        long scaled = Math.round(((Double) value) * scale);
        return prefix + "(" + scaled + ").\n";
    }

    @Override
    public AspFactGeneratorProvider withParams(JsonObject validatorParams) {
        if (validatorParams == null || !validatorParams.has("scale")
                || validatorParams.get("scale").isJsonNull()) {
            return this;
        }
        int configured = validatorParams.get("scale").getAsInt();
        if (configured <= 0) {
            throw new IllegalArgumentException("validator 'scale' must be positive, got " + configured);
        }
        return new DoubleAspFactGeneratorProvider(configured);
    }
}
