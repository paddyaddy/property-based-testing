package pbt.components.asp.facts;

import pbt.spi.asp.facts.AspFactGeneratorProvider;

/**
 * Renders a {@link Long} as a single-argument fact: {@code prefix(value).}
 */
public final class LongAspFactGeneratorProvider implements AspFactGeneratorProvider {

    @Override
    public String name() {
        return "long";
    }

    @Override
    public String toFacts(Object value, String prefix) {
        if (value == null) return "";
        return prefix + "(" + value + ").\n";
    }
}
