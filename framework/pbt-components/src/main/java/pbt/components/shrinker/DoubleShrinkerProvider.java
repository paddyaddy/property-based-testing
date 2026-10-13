package pbt.components.shrinker;

import com.google.gson.JsonObject;
import pbt.spi.shrinker.Shrinker;
import pbt.spi.shrinker.ShrinkerProvider;

import java.util.stream.Stream;

/**
 * Shrinks doubles toward zero by halving and stepping one unit closer.
 */
public final class DoubleShrinkerProvider implements ShrinkerProvider {

    static Stream<Double> shrinkDouble(double value) {
        if (value == 0.0) {
            return Stream.empty();
        }
        double half = value / 2.0;
        double toward = value > 0.0 ? value - 1.0 : value + 1.0;
        return Stream.of(0.0, half, toward)
                .distinct()
                .filter(v -> v != value && Math.abs(v) < Math.abs(value));
    }

    @Override
    public String name() {
        return "double";
    }

    @Override
    public Shrinker<Double> build(JsonObject params) {
        return DoubleShrinkerProvider::shrinkDouble;
    }
}
