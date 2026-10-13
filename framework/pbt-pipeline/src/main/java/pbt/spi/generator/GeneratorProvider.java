package pbt.spi.generator;

import com.google.gson.JsonObject;
import pbt.spi.NamedProvider;

/**
 * SPI for pluggable input generators.
 */
public interface GeneratorProvider extends NamedProvider {

    /**
     * Builds a generator configured by {@code params}.
     * {@code params} may be an empty object when the descriptor omits the field.
     */
    Generator<?> build(JsonObject params);
}
