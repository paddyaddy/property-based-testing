package pbt.components.asp.facts;

import pbt.spi.asp.facts.AspFactGeneratorProvider;

/**
 * Renders an {@link Integer} as a single-argument fact: {@code prefix(value).}
 */
public final class IntAspFactGeneratorProvider implements AspFactGeneratorProvider {

    @Override
    public String name() {
        return "int";
    }

    @Override
    public String toFacts(Object value, String prefix) {
        if (value == null) return "";
        return prefix + "(" + value + ").\n";
    }
}
