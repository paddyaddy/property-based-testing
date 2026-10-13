package pbt.components.shrinker;

import com.google.gson.JsonObject;
import pbt.spi.shrinker.Shrinker;
import pbt.spi.shrinker.ShrinkerProvider;

import java.util.stream.Stream;

/**
 * Shrinks longs toward zero. Name matches the {@code long} generator kind.
 */
public final class LongShrinkerProvider implements ShrinkerProvider {

    static Stream<Long> shrinkLong(long value) {
        if (value == 0L) {
            return Stream.empty();
        }
        long half = value / 2L;
        long toward = value > 0L ? value - 1L : value + 1L;
        return Stream.of(0L, half, toward).distinct().filter(v -> v != value);
    }

    @Override
    public String name() {
        return "long";
    }

    @Override
    public Shrinker<Long> build(JsonObject params) {
        return LongShrinkerProvider::shrinkLong;
    }
}
