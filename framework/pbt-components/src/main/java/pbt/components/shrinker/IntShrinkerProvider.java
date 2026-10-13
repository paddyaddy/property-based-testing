package pbt.components.shrinker;

import com.google.gson.JsonObject;
import pbt.spi.shrinker.Shrinker;
import pbt.spi.shrinker.ShrinkerProvider;

import java.util.stream.Stream;

/**
 * Shrinks integers toward zero. Name matches the {@code int} generator kind.
 */
public final class IntShrinkerProvider implements ShrinkerProvider {

    static Stream<Integer> shrinkInt(int value) {
        if (value == 0) {
            return Stream.empty();
        }
        int half = value / 2;
        int toward = value > 0 ? value - 1 : value + 1;
        return Stream.of(0, half, toward).distinct().filter(v -> v != value);
    }

    @Override
    public String name() {
        return "int";
    }

    @Override
    public Shrinker<Integer> build(JsonObject params) {
        return IntShrinkerProvider::shrinkInt;
    }
}
