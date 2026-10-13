package pbt.descriptor;

import com.google.gson.JsonObject;

/**
 * Selects and configures the input generator for a suite.
 *
 * @param kind       registry key of the generator component
 * @param regenerate when true, inputs are drawn fresh for each property; when false
 *                   they are generated once and shared across every property in the
 *                   suite
 * @param params     generator-specific parameters, passed through to the component
 */
public record GeneratorSpec(String kind, boolean regenerate, JsonObject params) {
}
