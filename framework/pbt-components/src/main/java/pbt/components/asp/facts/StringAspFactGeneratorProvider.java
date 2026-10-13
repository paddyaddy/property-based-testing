package pbt.components.asp.facts;

import pbt.spi.asp.facts.AspFactGeneratorProvider;

/**
 * Renders a {@link String} as a quoted-string fact: {@code prefix("value").}
 */
public final class StringAspFactGeneratorProvider implements AspFactGeneratorProvider {

    @Override
    public String name() {
        return "string";
    }

    @Override
    public String toFacts(Object value, String prefix) {
        if (value == null) return "";
        String s = (String) value;
        String escaped = s.replace("\\", "\\\\").replace("\"", "\\\"");
        return prefix + "(\"" + escaped + "\").\n";
    }
}
