package pbt.spi.shrinker;

import com.google.gson.JsonObject;
import pbt.spi.NamedProvider;

/**
 * SPI for pluggable shrinkers. The short name matches the corresponding generator kind.
 */
public interface ShrinkerProvider extends NamedProvider {

    /**
     * Builds a shrinker using the same {@code params} as the generator for this type.
     * {@code params} may be an empty object when the descriptor omits the field.
     */
    Shrinker<?> build(JsonObject params);
}
