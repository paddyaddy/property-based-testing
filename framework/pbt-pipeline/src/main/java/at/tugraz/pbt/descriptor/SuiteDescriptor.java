package at.tugraz.pbt.descriptor;

import java.util.List;
import java.util.Map;

/**
 * A whole suite as parsed from a JSON descriptor: named systems under test, the
 * input generator, and the list of properties.
 *
 * @param factGenerator optional override for the ASP fact generator kind; when
 *                      {@code null} the generator's own {@code kind} is used as the
 *                      lookup key, which works whenever the generator and fact
 *                      generator share the same name (the default convention)
 * @param outputKind    optional ASP fact generator kind for SUT outputs; when
 *                      {@code null} outputs are rendered like inputs, which only fits
 *                      SUTs whose output type equals their input type
 */
public record SuiteDescriptor(
        String name,
        String description,
        Map<String, SutSpec> suts,
        GeneratorSpec generator,
        String factGenerator,
        String outputKind,
        List<PropertyDescriptor> properties) {
}
