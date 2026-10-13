package pbt.components;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pbt.components.asp.CliAspEngine;
import pbt.components.sut.JavaSutRunnerProvider;
import pbt.descriptor.SuiteDescriptor;
import pbt.descriptor.SuiteLoader;
import pbt.descriptor.SutSpec;
import pbt.pipeline.PipelineRunner;
import pbt.pipeline.dto.PropertyResult;
import pbt.pipeline.dto.SuiteResult;
import pbt.spi.Registry;
import pbt.spi.generator.GeneratorProvider;
import pbt.spi.shrinker.ShrinkerProvider;
import pbt.spi.sut.SutRunner;
import pbt.spi.transform.TransformProvider;
import pbt.spi.type.ValueTypeProvider;
import pbt.spi.validator.ValidatorProvider;

import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class PipelineInProcessSutTest {

    private static final SuiteDescriptor DESCRIPTOR = SuiteLoader.parse(new StringReader("""
            {
              "name": "sort-suite",
              "suts": { "ref": { "type": "java" } },
              "generator": {
                "kind": "intList",
                "params": { "minLen": 1, "maxLen": 8, "minValue": -10, "maxValue": 10 }
              },
              "properties": [
                { "id": "order",
                  "type": "INVARIANT",
                  "validator": { "kind": "asp", "relation": "asp/sort_ordered.lp" } }
              ]
            }
            """), "inline");

    @SuppressWarnings("unchecked")
    private static List<Integer> referenceSort(Object input) {
        List<Integer> copy = new ArrayList<>((List<Integer>) input);
        Collections.sort(copy);
        return copy;
    }

    @SuppressWarnings("unchecked")
    private static List<Integer> buggySort(Object input) {
        List<Integer> copy = new ArrayList<>((List<Integer>) input);
        copy.sort(Comparator.reverseOrder());
        return copy;
    }

    private static PipelineRunner runner() {
        return new PipelineRunner(
                Registry.load(GeneratorProvider.class),
                Registry.load(ShrinkerProvider.class),
                Registry.load(ValidatorProvider.class),
                Registry.load(TransformProvider.class),
                Registry.load(ValueTypeProvider.class),
                50,
                42L);
    }

    @Test
    void reference_sort_passes_ordered_property() {
        assumeTrue(new CliAspEngine().isAvailable(), "clingo not available");

        SuiteResult result = runner().run(DESCRIPTOR,
                input -> referenceSort(input));

        assertTrue(result.passed());
    }

    @Test
    void buggy_sort_violates_ordered_property() {
        assumeTrue(new CliAspEngine().isAvailable(), "clingo not available");

        SuiteResult result = runner().run(DESCRIPTOR,
                input -> buggySort(input));

        assertFalse(result.passed());
        PropertyResult p = result.properties().get(0);
        assertTrue(p.failures() > 0);
        assertTrue(p.witness().isPresent());
        List<?> witness = assertInstanceOf(List.class, p.witness().get());
        assertTrue(witness.size() >= 2, "minimized witness should have at least 2 elements");
    }

    @Test
    void artifact_persistence_writes_json_files(@TempDir Path tempDir) throws Exception {
        assumeTrue(new CliAspEngine().isAvailable(), "clingo not available");

        PipelineRunner runner = new PipelineRunner(
                Registry.load(GeneratorProvider.class),
                Registry.load(ShrinkerProvider.class),
                Registry.load(ValidatorProvider.class),
                Registry.load(TransformProvider.class),
                Registry.load(ValueTypeProvider.class),
                20,
                1L,
                tempDir);

        runner.run(DESCRIPTOR, input -> referenceSort(input));

        Path artifact = tempDir.resolve("order.json");
        assertTrue(Files.exists(artifact), "artifact file should exist");
        assertTrue(Files.size(artifact) > 0, "artifact file should be non-empty");
    }

    @Test
    void java_sut_runner_invokes_via_reflection() {
        SutSpec spec = new SutSpec("java",
                "pbt.components.PipelineInProcessSutTest$SortAdapter",
                "sort", true, null, null, null, null, null);
        SutRunner runner = new JavaSutRunnerProvider().build(spec);

        @SuppressWarnings("unchecked")
        List<Integer> output = (List<Integer>) runner.run(List.of(3, 1, 2));

        assertEquals(List.of(1, 2, 3), output);
    }

    /**
     * Reflection target for {@link #java_sut_runner_invokes_via_reflection()}.
     */
    public static final class SortAdapter {

        public static List<Integer> sort(List<Integer> xs) {
            List<Integer> copy = new ArrayList<>(xs);
            Collections.sort(copy);
            return copy;
        }
    }
}
