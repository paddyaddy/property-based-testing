package pbt.components.generator;

import com.google.gson.JsonObject;
import pbt.components.tuple.TupleComponent;
import pbt.spi.Registry;
import pbt.spi.generator.Generator;
import pbt.spi.generator.GeneratorProvider;
import pbt.spi.type.Tuple;

import java.util.ArrayList;
import java.util.List;

/**
 * Generates a {@link Tuple} for SUTs with several parameters, one component generator per
 * parameter. Params: {@code components}, a list of {@code {"kind": ..., "params": {...}}}
 * in parameter order.
 */
public final class TupleGeneratorProvider implements GeneratorProvider {

    @Override
    public String name() {
        return "tuple";
    }

    @Override
    @SuppressWarnings("unchecked")
    public Generator<Tuple> build(JsonObject params) {
        Registry<GeneratorProvider> registry = Registry.load(GeneratorProvider.class);
        List<Generator<Object>> parts = new ArrayList<>();
        for (TupleComponent c : TupleComponent.parse(params)) {
            parts.add((Generator<Object>) registry.get(c.kind()).build(c.params()));
        }
        return rnd -> {
            List<Object> values = new ArrayList<>(parts.size());
            for (Generator<Object> part : parts) {
                values.add(part.generate(rnd));
            }
            return new Tuple(values);
        };
    }
}
