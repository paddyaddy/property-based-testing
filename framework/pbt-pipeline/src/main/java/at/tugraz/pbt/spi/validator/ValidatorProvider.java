package at.tugraz.pbt.spi.validator;

import com.google.gson.JsonObject;
import at.tugraz.pbt.descriptor.SuiteDescriptor;
import at.tugraz.pbt.spi.NamedProvider;

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

    /**
     * Builds a validator with access to the whole suite, so it can see the generator
     * parameters and the output kind. The default ignores everything but the generator
     * kind and delegates to {@link #build(JsonObject, String)}.
     */
    default Validator build(JsonObject params, SuiteDescriptor suite) {
        return build(params, suite.generator().kind());
    }
}
