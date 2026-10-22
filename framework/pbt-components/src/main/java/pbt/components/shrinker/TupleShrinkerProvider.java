package pbt.components.shrinker;

import com.google.gson.JsonObject;
import pbt.components.tuple.TupleComponent;
import pbt.spi.Registry;
import pbt.spi.shrinker.Shrinker;
import pbt.spi.shrinker.ShrinkerProvider;
import pbt.spi.type.Tuple;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Shrinks a {@link Tuple} one component at a time with that component's own shrinker,
 * keeping the other components fixed. A component without a registered shrinker stays as is.
 */
public final class TupleShrinkerProvider implements ShrinkerProvider {

    @Override
    public String name() {
        return "tuple";
    }

    @Override
    @SuppressWarnings("unchecked")
    public Shrinker<Tuple> build(JsonObject params) {
        Registry<ShrinkerProvider> registry = Registry.load(ShrinkerProvider.class);
        List<Shrinker<Object>> parts = new ArrayList<>();
        for (TupleComponent c : TupleComponent.parse(params)) {
            parts.add(registry.has(c.kind())
                    ? (Shrinker<Object>) registry.get(c.kind()).build(c.params())
                    : v -> Stream.empty());
        }
        return tuple -> {
            List<Object> values = tuple.values();
            return IntStream.range(0, parts.size()).boxed().flatMap(i ->
                    parts.get(i).shrink(values.get(i)).map(smaller -> {
                        List<Object> copy = new ArrayList<>(values);
                        copy.set(i, smaller);
                        return new Tuple(copy);
                    }));
        };
    }
}
