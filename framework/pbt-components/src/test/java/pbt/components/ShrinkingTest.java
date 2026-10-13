package pbt.components;

import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pbt.components.asp.CliAspEngine;
import pbt.descriptor.SuiteDescriptor;
import pbt.descriptor.SuiteLoader;
import pbt.pipeline.PipelineRunner;
import pbt.pipeline.dto.PropertyResult;
import pbt.spi.Registry;
import pbt.spi.generator.Generator;
import pbt.spi.generator.GeneratorProvider;
import pbt.spi.shrinker.Shrinker;
import pbt.spi.shrinker.ShrinkerProvider;
import pbt.spi.transform.TransformProvider;
import pbt.spi.type.ValueTypeProvider;
import pbt.spi.validator.CheckResult;
import pbt.spi.validator.Validator;
import pbt.spi.validator.ValidatorProvider;

import java.io.StringReader;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class ShrinkingTest {

    static final String FAIL_ALWAYS = ShrinkingTest.class.getName() + "$FailAlways";
    static final String RECORDING = ShrinkingTest.class.getName() + "$Recording";

    static final List<List<Integer>> RECORDED_FOLLOW_UPS = new ArrayList<>();

    private static PipelineRunner runner(int runs, Path artifacts, int maxShrinkSteps) {
        return new PipelineRunner(
                Registry.load(GeneratorProvider.class),
                Registry.load(ShrinkerProvider.class),
                Registry.load(ValidatorProvider.class),
                Registry.load(TransformProvider.class),
                Registry.load(ValueTypeProvider.class),
                runs, 7L, artifacts, maxShrinkSteps);
    }

    private static SuiteDescriptor suite(String generator, String property) {
        return SuiteLoader.parse(new StringReader("""
                {
                  "name": "shrink",
                  "suts": { "ref": { "type": "java" } },
                  "generator": %s,
                  "properties": [ %s ]
                }
                """.formatted(generator, property)), "inline");
    }

    @Test
    void shrinks_to_empty_list_when_every_candidate_fails() {
        SuiteDescriptor d = suite(
                "{ \"kind\": \"intList\", \"params\": { \"minLen\": 0, \"maxLen\": 8 } }",
                "{ \"id\": \"P\", \"type\": \"INVARIANT\", \"validator\": { \"kind\": \"" + FAIL_ALWAYS + "\" } }");

        PropertyResult p = runner(5, null, PipelineRunner.DEFAULT_MAX_SHRINK_STEPS)
                .run(d).properties().get(0);

        assertEquals(List.of(), p.witness().orElseThrow());
    }

    @Test
    void shrink_loop_stops_at_the_step_cap() {
        SuiteDescriptor d = suite(
                "{ \"kind\": \"intList\", \"params\": { \"minLen\": 8, \"maxLen\": 8 } }",
                "{ \"id\": \"P\", \"type\": \"INVARIANT\", \"validator\": { \"kind\": \"" + FAIL_ALWAYS + "\" } }");

        PropertyResult p = runner(1, null, 2).run(d).properties().get(0);

        assertTrue(p.runLog().stream().anyMatch(l -> l.contains("Shrink step cap reached (2")),
                "log should record the cap: " + p.runLog());
        assertTrue(((List<?>) p.witness().orElseThrow()).size() > 0);
    }

    @Test
    void follow_up_of_a_candidate_does_not_depend_on_earlier_candidates() {
        RECORDED_FOLLOW_UPS.clear();
        SuiteDescriptor d = suite(
                "{ \"kind\": \"fixedList\" }",
                "{ \"id\": \"P\", \"type\": \"METAMORPHIC\", \"transform\": \"shuffle\","
                        + " \"validator\": { \"kind\": \"" + RECORDING + "\" } }");

        runner(1, null, PipelineRunner.DEFAULT_MAX_SHRINK_STEPS).run(d);

        assertTrue(RECORDED_FOLLOW_UPS.size() >= 2, "the repeated candidate should be evaluated twice");
        assertEquals(RECORDED_FOLLOW_UPS.get(0), RECORDED_FOLLOW_UPS.get(1));
    }

    @Test
    void shrink_candidates_respect_the_applicability_filter() {
        assumeTrue(new CliAspEngine().isAvailable(), "clingo not available");
        SuiteDescriptor d = suite(
                "{ \"kind\": \"intList\", \"params\": { \"minLen\": 2, \"maxLen\": 8 } }",
                "{ \"id\": \"P\", \"type\": \"INVARIANT\","
                        + " \"applicability\": \"applicability/at_least_two_elements.lp\","
                        + " \"validator\": { \"kind\": \"" + FAIL_ALWAYS + "\" } }");

        PropertyResult p = runner(10, null, PipelineRunner.DEFAULT_MAX_SHRINK_STEPS)
                .run(d).properties().get(0);

        assertEquals(2, ((List<?>) p.witness().orElseThrow()).size());
    }

    @Test
    void result_reports_generated_checked_and_skipped(@TempDir Path dir) throws Exception {
        assumeTrue(new CliAspEngine().isAvailable(), "clingo not available");
        SuiteDescriptor d = suite(
                "{ \"kind\": \"intList\", \"params\": { \"minLen\": 0, \"maxLen\": 3 } }",
                "{ \"id\": \"P\", \"type\": \"INVARIANT\","
                        + " \"applicability\": \"applicability/at_least_two_elements.lp\","
                        + " \"validator\": { \"kind\": \"trivial\" } }");

        PropertyResult p = runner(40, dir, PipelineRunner.DEFAULT_MAX_SHRINK_STEPS)
                .run(d).properties().get(0);

        assertEquals(40, p.generated());
        assertTrue(p.skipped() > 0);
        assertEquals(p.generated(), p.runs() + p.skipped());
        String json = Files.readString(dir.resolve("P.json"));
        assertTrue(json.contains("\"generated\": 40") || json.contains("\"generated\":40"), json);
        assertFalse(json.isBlank());
    }

    public static final class FailAlways implements ValidatorProvider {
        @Override
        public String name() {
            return "failAlways";
        }

        @Override
        public Validator build(JsonObject params, String generatorKind) {
            return ctx -> CheckResult.fail("always");
        }
    }

    /**
     * Fails for the seven-element source input and records follow-ups of the six-element candidate.
     */
    public static final class Recording implements ValidatorProvider {
        @Override
        public String name() {
            return "recording";
        }

        @Override
        public Validator build(JsonObject params, String generatorKind) {
            return ctx -> {
                List<?> source = (List<?>) ctx.sourceInput();
                if (source.size() == 7) return CheckResult.fail("seven");
                RECORDED_FOLLOW_UPS.add(new ArrayList<>((List<Integer>) ctx.followUpInput()));
                return CheckResult.pass();
            };
        }
    }

    public static final class FixedListGenerator implements GeneratorProvider {
        @Override
        public String name() {
            return "fixedList";
        }

        @Override
        public Generator<?> build(JsonObject params) {
            return rnd -> List.of(1, 2, 3, 4, 5, 6, 7);
        }
    }

    /**
     * Proposes the same six-element candidate twice.
     */
    public static final class FixedListShrinker implements ShrinkerProvider {
        @Override
        public String name() {
            return "fixedList";
        }

        @Override
        public Shrinker<?> build(JsonObject params) {
            return value -> {
                List<Integer> list = (List<Integer>) value;
                if (list.size() != 7) return Stream.empty();
                List<Integer> candidate = List.of(1, 2, 3, 4, 5, 6);
                return Stream.of(candidate, candidate);
            };
        }
    }

    public static final class FixedListValueType implements ValueTypeProvider {
        @Override
        public String name() {
            return "fixedList";
        }

        @Override
        public Type javaType() {
            return new TypeToken<List<Integer>>() {
            }.getType();
        }

        @Override
        public String describe(Object value) {
            return String.valueOf(value);
        }
    }
}
