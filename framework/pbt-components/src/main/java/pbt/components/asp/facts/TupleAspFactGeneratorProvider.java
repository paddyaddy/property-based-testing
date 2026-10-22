package pbt.components.asp.facts;

import com.google.gson.JsonObject;
import pbt.components.tuple.TupleComponent;
import pbt.spi.Registry;
import pbt.spi.asp.facts.AspFactGeneratorProvider;
import pbt.spi.type.Tuple;

import java.util.ArrayList;
import java.util.List;

/**
 * Renders a {@link Tuple} by delegating each component to the fact generator of its kind,
 * with the position appended to the prefix: component 1 of an input is rendered with prefix
 * {@code in_1}, so a string there yields {@code in_1("abc").}
 *
 * <p>The component kinds come from the generator params, so an instance obtained from the
 * registry must be configured with {@link #withGeneratorParams} before use.
 */
public final class TupleAspFactGeneratorProvider implements AspFactGeneratorProvider {

    private final List<AspFactGeneratorProvider> components;

    public TupleAspFactGeneratorProvider() {
        this(List.of());
    }

    private TupleAspFactGeneratorProvider(List<AspFactGeneratorProvider> components) {
        this.components = components;
    }

    @Override
    public String name() {
        return "tuple";
    }

    @Override
    public AspFactGeneratorProvider withGeneratorParams(JsonObject generatorParams) {
        Registry<AspFactGeneratorProvider> registry = Registry.load(AspFactGeneratorProvider.class);
        List<AspFactGeneratorProvider> configured = new ArrayList<>();
        for (TupleComponent c : TupleComponent.parse(generatorParams)) {
            configured.add(registry.get(c.kind()).withGeneratorParams(c.params()));
        }
        return new TupleAspFactGeneratorProvider(configured);
    }

    @Override
    public AspFactGeneratorProvider withParams(JsonObject validatorParams) {
        return new TupleAspFactGeneratorProvider(
                components.stream().map(c -> c.withParams(validatorParams)).toList());
    }

    @Override
    public String toFacts(Object value, String prefix) {
        if (value == null) return "";
        if (!(value instanceof Tuple tuple)) {
            throw new IllegalArgumentException("tuple fact generator got a " + value.getClass().getSimpleName()
                    + "; SUT outputs need their own fact generator, set 'outputKind' in the suite");
        }
        if (components.size() != tuple.values().size()) {
            throw new IllegalStateException("tuple has " + tuple.values().size() + " values but "
                    + components.size() + " configured components");
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < components.size(); i++) {
            sb.append(components.get(i).toFacts(tuple.values().get(i), prefix + "_" + i));
        }
        return sb.toString();
    }
}
