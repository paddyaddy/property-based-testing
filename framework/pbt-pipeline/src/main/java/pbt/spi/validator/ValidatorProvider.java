package pbt.spi.validator;

import com.google.gson.JsonObject;
import pbt.spi.NamedProvider;

/**
 * SPI for pluggable property validators.
 */
public interface ValidatorProvider extends NamedProvider {

    /**
     * Builds a validator configured by {@code params} (the full validator JSON object
     * from the descriptor, including the {@code "kind"} field).
     *
     * @param params        full validator JSON object from the descriptor
     * @param generatorKind short name of the suite's generator (e.g. {@code "intList"}),
     *                      used to look up the right fact generator for ASP-backed validators
     */
    Validator build(JsonObject params, String generatorKind);
}
