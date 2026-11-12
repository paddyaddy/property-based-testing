package pbt.components;

import org.junit.jupiter.api.Test;
import pbt.components.asp.CliAspEngine;
import pbt.descriptor.SuiteDescriptor;
import pbt.descriptor.SuiteLoader;
import pbt.pipeline.AspPrograms;
import pbt.pipeline.PipelineRunner;
import pbt.pipeline.dto.PropertyResult;
import pbt.spi.Registry;
import pbt.spi.generator.GeneratorProvider;
import pbt.spi.shrinker.ShrinkerProvider;
import pbt.spi.sut.SutRunner;
import pbt.spi.transform.TransformProvider;
import pbt.spi.type.ValueTypeProvider;
import pbt.spi.validator.ValidatorProvider;

import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class IncludeTest {

    private static SuiteDescriptor suite(String relation, String applicability) {
        return SuiteLoader.parse(new StringReader("""
                {
                  "name": "includes",
                  "suts": { "ref": { "type": "java" } },
                  "generator": { "kind": "int", "params": { "min": 0, "max": 20 } },
                  "properties": [
                    { "id": "P", "type": "INVARIANT",
                      "validator": { "kind": "asp", "relation": "%s" },
                      "applicability": "%s" }
                  ]
                }
                """.formatted(relation, applicability)), "inline");
    }

    private static PropertyResult run(SuiteDescriptor suite, SutRunner sut) {
        return new PipelineRunner(
                Registry.load(GeneratorProvider.class),
                Registry.load(ShrinkerProvider.class),
                Registry.load(ValidatorProvider.class),
                Registry.load(TransformProvider.class),
                Registry.load(ValueTypeProvider.class),
                40,
                42L).run(suite, sut).properties().get(0);
    }

    @Test
    void includes_are_replaced_by_the_included_program() {
        String program = AspPrograms.load("asp/skip_unless_big.lp");

        assertTrue(program.contains("big :- in(V), V > 10."), program);
        assertFalse(program.contains("#include"), program);
        assertTrue(program.contains("skip :- not big."), program);
    }

    @Test
    void a_file_included_twice_is_inserted_once() {
        String program = AspPrograms.load("asp/include_chain.lp");

        assertEquals(1, program.split("big :- in\\(V\\)", -1).length - 1, program);
    }

    @Test
    void include_paths_are_relative_to_the_including_file() {
        String program = AspPrograms.load("asp/nested/parent_include.lp");

        assertTrue(program.contains("big :- in(V), V > 10."), program);
        assertTrue(program.contains("violation :- big."), program);
    }

    @Test
    void a_missing_include_names_the_file_and_where_it_was_included() {
        var e = assertThrows(IllegalArgumentException.class, () -> AspPrograms.load("asp/include_missing.lp"));

        assertTrue(e.getMessage().contains("asp/nope.lp"), e.getMessage());
        assertTrue(e.getMessage().contains("asp/include_missing.lp"), e.getMessage());
    }

    @Test
    void an_include_in_a_comment_is_left_alone() {
        assertDoesNotThrow(() -> AspPrograms.load("asp/commented_include.lp"));
    }

    @Test
    void properties_share_definitions_through_includes() {
        assumeTrue(new CliAspEngine().isAvailable(), "clingo not available");
        var suite = suite("asp/violation_when_big_and_changed.lp", "asp/skip_unless_big.lp");

        PropertyResult identity = run(suite, input -> input);
        PropertyResult clamped = run(suite, input -> Math.min((Integer) input, 10));

        assertTrue(identity.passed());
        assertTrue(identity.skipped() > 0, "inputs that are not big are filtered out");
        assertTrue(identity.runs() > 0, "big inputs are still checked");
        assertFalse(clamped.passed());
        assertEquals(11, clamped.witness().orElseThrow());
    }
}
