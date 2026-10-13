package pbt.components;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pbt.components.asp.CliAspEngine;
import pbt.components.sut.PythonSutRunnerProvider;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class PythonSutRunnerTest {

    private static boolean isPythonAvailable() {
        try {
            return new ProcessBuilder("python3", "--version").start().waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private static SutRunner buildRunner(String method) {
        SutSpec spec = new SutSpec(
                "python", null, method, false, null,
                "src/test/resources/python/test_sort.py", null, null, "intList");
        return new PythonSutRunnerProvider().build(spec);
    }

    @BeforeEach
    void requirePython() {
        assumeTrue(isPythonAvailable(), "python3 not on PATH");
    }

    @Test
    @SuppressWarnings("unchecked")
    void builder_invokes_reference_sort() {
        SutRunner runner = buildRunner("reference_sort(list)");
        List<Integer> result = (List<Integer>) runner.run(List.of(3, 1, 2));
        assertEquals(List.of(1, 2, 3), result);
    }

    @Test
    @SuppressWarnings("unchecked")
    void builder_invokes_buggy_sort() {
        SutRunner runner = buildRunner("buggy_sort(list)");
        List<Integer> result = (List<Integer>) runner.run(List.of(3, 1, 2));
        assertEquals(List.of(1, 3), result);
    }

    @Test
    void pipeline_detects_bug_in_buggy_python_sort() {
        assumeTrue(new CliAspEngine().isAvailable(), "clingo not available");

        SuiteDescriptor descriptor = SuiteLoader.parse(new StringReader("""
                {
                  "name": "sort_python",
                  "suts": { "buggy": { "type": "python", "file": "src/test/resources/python/test_sort.py", "outputType": "intList" } },
                  "generator": { "kind": "intList", "params": { "minLen": 2, "maxLen": 8, "minValue": -10, "maxValue": 10 } },
                  "properties": [
                    { "id": "ordered", "type": "INVARIANT",
                      "validator": { "kind": "asp", "relation": "asp/sort_ordered.lp" } }
                  ]
                }
                """), "inline");

        PipelineRunner runner = new PipelineRunner(
                Registry.load(GeneratorProvider.class),
                Registry.load(ShrinkerProvider.class),
                Registry.load(ValidatorProvider.class),
                Registry.load(TransformProvider.class),
                Registry.load(ValueTypeProvider.class),
                200, 42L);

        SutRunner sutRunner = buildRunner("buggy_sort_reverse(list)");
        SuiteResult result = runner.run(descriptor, sutRunner);

        assertFalse(result.passed(), "buggy sort should fail the ordered property");
        PropertyResult ordered = result.properties().get(0);
        assertTrue(ordered.witness().isPresent(), "a minimized witness should be found");
        @SuppressWarnings("unchecked")
        List<Integer> witness = (List<Integer>) ordered.witness().get();
        assertTrue(witness.size() >= 2, "minimized witness should have at least 2 elements");
    }

    @Test
    @SuppressWarnings("unchecked")
    void call_timeout_kills_the_worker_and_the_next_call_restarts_it() {
        SutSpec spec = new SutSpec(
                "python", null, "sort_unless_negative_first(list)", false, null,
                "src/test/resources/python/test_sort.py", 2L, null, "intList");
        SutRunner runner = new PythonSutRunnerProvider().build(spec);
        try {
            assertEquals(List.of(1, 2, 3), runner.run(List.of(3, 1, 2)));

            RuntimeException e = assertThrows(RuntimeException.class, () -> runner.run(List.of(-1)));
            assertTrue(e.getMessage().contains("timed out"), e.getMessage());

            assertEquals(List.of(1, 2, 3), runner.run(List.of(3, 1, 2)));
        } finally {
            runner.close();
        }
    }

    @Test
    void unknown_interpreter_fails_when_building() {
        SutSpec spec = new SutSpec(
                "python", null, "reference_sort(list)", false, null,
                "src/test/resources/python/test_sort.py", null, "no-such-python-binary", "intList");

        assertThrows(java.io.UncheckedIOException.class, () -> new PythonSutRunnerProvider().build(spec));
    }

    @Test
    void configured_interpreter_is_used() {
        SutSpec spec = new SutSpec(
                "python", null, "reference_sort(list)", false, null,
                "src/test/resources/python/test_sort.py", null, "python3", "intList");
        SutRunner runner = new PythonSutRunnerProvider().build(spec);
        try {
            assertEquals(List.of(1, 2), runner.run(List.of(2, 1)));
        } finally {
            runner.close();
        }
    }
}
