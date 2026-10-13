package pbt.components;

import org.junit.jupiter.api.Test;
import pbt.components.asp.CliAspEngine;
import pbt.descriptor.SuiteDescriptor;
import pbt.descriptor.SuiteLoader;
import pbt.pipeline.PipelineRunner;
import pbt.pipeline.dto.PropertyResult;
import pbt.pipeline.dto.SuiteResult;
import pbt.spi.Registry;
import pbt.spi.generator.GeneratorProvider;
import pbt.spi.shrinker.ShrinkerProvider;
import pbt.spi.transform.TransformProvider;
import pbt.spi.type.ValueTypeProvider;
import pbt.spi.validator.ValidatorProvider;

import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class PipelineEndToEndTest {

    private static PipelineRunner runner(int runs, long seed) {
        return new PipelineRunner(
                Registry.load(GeneratorProvider.class),
                Registry.load(ShrinkerProvider.class),
                Registry.load(ValidatorProvider.class),
                Registry.load(TransformProvider.class),
                Registry.load(ValueTypeProvider.class),
                runs,
                seed);
    }

    @Test
    void all_properties_pass_with_trivial_validator() {
        SuiteDescriptor descriptor = SuiteLoader.parse(new StringReader("""
                {
                  "name": "smoke",
                  "suts": { "ref": { "type": "java" } },
                  "generator": {
                    "kind": "intList",
                    "params": { "minLen": 0, "maxLen": 8, "minValue": -20, "maxValue": 20 }
                  },
                  "properties": [
                    { "id": "P1", "type": "INVARIANT", "validator": { "kind": "trivial" } },
                    { "id": "P2", "type": "INVARIANT", "validator": { "kind": "trivial" } }
                  ]
                }
                """), "inline");

        SuiteResult result = runner(50, 1L).run(descriptor);

        assertTrue(result.passed());
        assertEquals(2, result.properties().size());
        for (PropertyResult p : result.properties()) {
            assertEquals(50, p.runs());
            assertEquals(0, p.failures());
            assertTrue(p.passed());
            assertFalse(p.witness().isPresent());
        }
    }

    @Test
    void int_generator_used_in_pipeline() {
        SuiteDescriptor descriptor = SuiteLoader.parse(new StringReader("""
                {
                  "name": "int-smoke",
                  "suts": { "ref": { "type": "java" } },
                  "generator": {
                    "kind": "int",
                    "params": { "min": -5, "max": 5 }
                  },
                  "properties": [
                    { "id": "P1", "type": "INVARIANT", "validator": { "kind": "trivial" } }
                  ]
                }
                """), "inline");

        SuiteResult result = runner(30, 7L).run(descriptor);

        assertTrue(result.passed());
        assertEquals(30, result.properties().get(0).runs());
    }

    @Test
    void minimizer_shrinks_failing_int_witness() {
        SuiteDescriptor descriptor = SuiteLoader.parse(new StringReader("""
                {
                  "name": "int-minimize",
                  "suts": { "ref": { "type": "java" } },
                  "generator": { "kind": "int", "params": { "min": -100, "max": 100 } },
                  "properties": [
                    { "id": "nonzero", "type": "INVARIANT",
                      "validator": { "kind": "pbt.components.PipelineEndToEndTest$NonZeroValidator" } }
                  ]
                }
                """), "inline");

        SuiteResult result = runner(200, 99L).run(descriptor);

        assertFalse(result.passed());
        PropertyResult p = result.properties().get(0);
        assertTrue(p.failures() > 0);
        assertTrue(p.witness().isPresent());
        int witness = (int) p.witness().get();
        assertTrue(witness == 1 || witness == -1,
                "expected minimized witness ±1, got " + witness);
    }

    @Test
    void asp_invariant_always_pass_program() {
        assumeTrue(new CliAspEngine().isAvailable(), "clingo not available");

        SuiteDescriptor descriptor = SuiteLoader.parse(new StringReader("""
                {
                  "name": "asp-trivial",
                  "suts": { "ref": { "type": "java" } },
                  "generator": {
                    "kind": "intList",
                    "params": { "minLen": 1, "maxLen": 5, "minValue": -10, "maxValue": 10 }
                  },
                  "properties": [
                    { "id": "always-pass",
                      "type": "INVARIANT",
                      "validator": { "kind": "asp", "relation": "asp/always_pass.lp" } }
                  ]
                }
                """), "inline");

        SuiteResult result = runner(20, 3L).run(descriptor);
        assertTrue(result.passed());
    }

    @Test
    void asp_invariant_detects_violation() {
        assumeTrue(new CliAspEngine().isAvailable(), "clingo not available");

        // Validator checks that the list has at least one positive element in "out".
        // Since sutOutput is null, out facts are empty → no positive element → violation fires.
        SuiteDescriptor descriptor = SuiteLoader.parse(new StringReader("""
                {
                  "name": "asp-violation",
                  "suts": { "ref": { "type": "java" } },
                  "generator": {
                    "kind": "intList",
                    "params": { "minLen": 1, "maxLen": 4, "minValue": 1, "maxValue": 10 }
                  },
                  "properties": [
                    { "id": "needs-output",
                      "type": "INVARIANT",
                      "validator": { "kind": "asp", "relation": "asp/needs_output.lp" } }
                  ]
                }
                """), "inline");

        SuiteResult result = runner(10, 5L).run(descriptor);
        assertFalse(result.passed(), "expected violation with no SUT output");
        assertTrue(result.properties().get(0).witness().isPresent());
    }

    @Test
    void boolean_generator_in_pipeline() {
        SuiteDescriptor descriptor = SuiteLoader.parse(new StringReader("""
                {
                  "name": "bool-smoke",
                  "suts": { "ref": { "type": "java" } },
                  "generator": { "kind": "boolean" },
                  "properties": [
                    { "id": "P1", "type": "INVARIANT", "validator": { "kind": "trivial" } }
                  ]
                }
                """), "inline");

        SuiteResult result = runner(30, 2L).run(descriptor);
        assertTrue(result.passed());
    }

    @Test
    void long_generator_in_pipeline() {
        SuiteDescriptor descriptor = SuiteLoader.parse(new StringReader("""
                {
                  "name": "long-smoke",
                  "suts": { "ref": { "type": "java" } },
                  "generator": { "kind": "long", "params": { "min": -50, "max": 50 } },
                  "properties": [
                    { "id": "P1", "type": "INVARIANT", "validator": { "kind": "trivial" } }
                  ]
                }
                """), "inline");

        SuiteResult result = runner(30, 3L).run(descriptor);
        assertTrue(result.passed());
    }

    @Test
    void double_generator_in_pipeline() {
        SuiteDescriptor descriptor = SuiteLoader.parse(new StringReader("""
                {
                  "name": "double-smoke",
                  "suts": { "ref": { "type": "java" } },
                  "generator": { "kind": "double", "params": { "min": -10.0, "max": 10.0 } },
                  "properties": [
                    { "id": "P1", "type": "INVARIANT", "validator": { "kind": "trivial" } }
                  ]
                }
                """), "inline");

        SuiteResult result = runner(30, 4L).run(descriptor);
        assertTrue(result.passed());
    }

    @Test
    void string_generator_in_pipeline() {
        SuiteDescriptor descriptor = SuiteLoader.parse(new StringReader("""
                {
                  "name": "string-smoke",
                  "suts": { "ref": { "type": "java" } },
                  "generator": { "kind": "string", "params": { "minLen": 0, "maxLen": 8 } },
                  "properties": [
                    { "id": "P1", "type": "INVARIANT", "validator": { "kind": "trivial" } }
                  ]
                }
                """), "inline");

        SuiteResult result = runner(30, 5L).run(descriptor);
        assertTrue(result.passed());
    }

    @Test
    void string_minimizer_shrinks_to_single_char() {
        SuiteDescriptor descriptor = SuiteLoader.parse(new StringReader("""
                {
                  "name": "string-minimize",
                  "suts": { "ref": { "type": "java" } },
                  "generator": { "kind": "string", "params": { "minLen": 1, "maxLen": 10 } },
                  "properties": [
                    { "id": "nonempty", "type": "INVARIANT",
                      "validator": { "kind": "pbt.components.PipelineEndToEndTest$NonEmptyStringValidator" } }
                  ]
                }
                """), "inline");

        SuiteResult result = runner(50, 6L).run(descriptor);
        assertFalse(result.passed());
        PropertyResult p = result.properties().get(0);
        assertTrue(p.witness().isPresent());
        assertEquals("a", p.witness().get(), "expected minimized witness to be \"a\"");
    }

    @Test
    void applicability_filter_skips_inputs_below_min_length() {
        assumeTrue(new CliAspEngine().isAvailable(), "clingo not available");
        SuiteDescriptor descriptor = SuiteLoader.parse(new StringReader("""
                {
                  "name": "applicability-test",
                  "suts": { "ref": { "type": "java" } },
                  "generator": {
                    "kind": "intList",
                    "params": { "minLen": 0, "maxLen": 8 }
                  },
                  "properties": [
                    { "id": "P1", "type": "INVARIANT",
                      "applicability": "applicability/at_least_two_elements.lp",
                      "validator": { "kind": "trivial" } }
                  ]
                }
                """), "inline");

        SuiteResult result = runner(100, 10L).run(descriptor);

        assertTrue(result.passed());
        var p = result.properties().get(0);
        assertTrue(p.skipped() > 0, "expected some inputs to be skipped");
        assertTrue(p.runs() < 100, "tested runs should be less than total generated");
        assertEquals(100, p.runs() + p.skipped(), "runs + skipped should equal total generated");
    }

    /**
     * Fails when value == 0; passes otherwise. Used to test minimization toward ±1.
     */
    public static final class NonZeroValidator implements pbt.spi.validator.ValidatorProvider {
        @Override
        public String name() {
            return "nonZero";
        }

        @Override
        public pbt.spi.validator.Validator build(com.google.gson.JsonObject p, String kind) {
            return ctx -> (int) ctx.sourceInput() == 0
                    ? pbt.spi.validator.CheckResult.pass()
                    : pbt.spi.validator.CheckResult.fail("nonzero");
        }
    }

    /**
     * Fails when string is non-empty; used to test minimization toward "a".
     */
    public static final class NonEmptyStringValidator implements pbt.spi.validator.ValidatorProvider {
        @Override
        public String name() {
            return "nonEmptyString";
        }

        @Override
        public pbt.spi.validator.Validator build(com.google.gson.JsonObject p, String kind) {
            return ctx -> ((String) ctx.sourceInput()).isEmpty()
                    ? pbt.spi.validator.CheckResult.pass()
                    : pbt.spi.validator.CheckResult.fail("nonempty");
        }
    }
}
