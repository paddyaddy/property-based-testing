package pbt.components.asp.facts;

import pbt.spi.asp.facts.AspFactGeneratorProvider;

import java.util.List;

/**
 * Renders a {@code List<Integer>} as positional facts: {@code prefix(index, value).}
 * (0-based index). Example: {@code [1, -2]} with prefix {@code "out"} yields
 * {@code "out(0,1).\nout(1,-2).\n"}.
 */
public final class IntListAspFactGeneratorProvider implements AspFactGeneratorProvider {

    @Override
    public String name() {
        return "intList";
    }

    @Override
    @SuppressWarnings("unchecked")
    public String toFacts(Object value, String prefix) {
        if (value == null) return "";
        List<Integer> xs = (List<Integer>) value;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < xs.size(); i++) {
            sb.append(prefix).append('(').append(i).append(',').append(xs.get(i)).append(").\n");
        }
        return sb.toString();
    }
}
