package pbt.components.asp.facts;

import pbt.spi.asp.facts.AspFactGeneratorProvider;

/**
 * Emits {@code prefix(1).} for true and {@code prefix(0).} for false.
 */
public final class BooleanAspFactGeneratorProvider implements AspFactGeneratorProvider {

    @Override
    public String name() {
        return "boolean";
    }

    @Override
    public String toFacts(Object value, String prefix) {
        if (value == null) return "";
        return prefix + "(" + (Boolean.TRUE.equals(value) ? 1 : 0) + ").\n";
    }
}
