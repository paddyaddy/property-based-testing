package pbt.spi.asp.facts;

import com.google.gson.JsonObject;
import pbt.spi.NamedProvider;

/**
 * Per-type fact renderer: turns a value into ground ASP facts.
 *
 * <p>The short name matches the corresponding generator/shrinker/value-type name
 * (e.g. {@code "int"}, {@code "intList"}). A single method covers all predicate
 * roles (input, output, source_out, followup_out, …) by prefixing each fact with
 * the caller-supplied {@code prefix}.
 *
 * <p>When {@code value} is {@code null} (SUT not yet executed), implementations
 * must return an empty string.
 */
public interface AspFactGeneratorProvider extends NamedProvider {

    /**
     * Renders {@code value} as ground ASP facts. Each fact's predicate is derived
     * from {@code prefix} (e.g. prefix {@code "out"} for a list {@code [1,2]}
     * yields {@code "out(0,1).\nout(1,2).\n"}).
     *
     * @param value  the value to render; {@code null} must return {@code ""}
     * @param prefix predicate prefix
     */
    String toFacts(Object value, String prefix);

    /**
     * Returns a renderer configured by the {@code "validator"} object of a property, for
     * renderers that take parameters (for example a fixed-point scale). The default
     * ignores {@code validatorParams} and returns this instance.
     */
    default AspFactGeneratorProvider withParams(JsonObject validatorParams) {
        return this;
    }
}
