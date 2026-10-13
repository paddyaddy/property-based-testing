package pbt.components.shrinker;

import com.google.gson.JsonObject;
import pbt.spi.shrinker.Shrinker;
import pbt.spi.shrinker.ShrinkerProvider;

import java.util.stream.Stream;

/**
 * Shrinks booleans toward false.
 */
public final class BooleanShrinkerProvider implements ShrinkerProvider {

    @Override
    public String name() {
        return "boolean";
    }

    @Override
    public Shrinker<Boolean> build(JsonObject params) {
        return value -> value ? Stream.of(false) : Stream.empty();
    }
}
