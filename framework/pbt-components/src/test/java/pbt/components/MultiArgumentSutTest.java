package pbt.components;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import pbt.components.asp.CliAspEngine;
import pbt.components.asp.facts.TupleAspFactGeneratorProvider;
import pbt.components.generator.TupleGeneratorProvider;
import pbt.components.shrinker.TupleShrinkerProvider;
import pbt.components.sut.JavaSutRunnerProvider;
import pbt.components.sut.PythonSutRunnerProvider;
import pbt.descriptor.SuiteDescriptor;
import pbt.descriptor.SuiteLoader;
import pbt.descriptor.SutSpec;
import pbt.pipeline.PipelineRunner;
import pbt.pipeline.dto.PropertyResult;
import pbt.spi.Registry;
import pbt.spi.generator.GeneratorProvider;
import pbt.spi.shrinker.ShrinkerProvider;
import pbt.spi.sut.SutRunner;
import pbt.spi.transform.TransformProvider;
import pbt.spi.type.Tuple;
import pbt.spi.type.ValueTypeProvider;
import pbt.spi.validator.ValidatorProvider;

import java.io.StringReader;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class MultiArgumentSutTest {

    private static final String TUPLE_PARAMS = """
            { "components": [
                { "kind": "int", "params": { "min": -5, "max": 5 } },
                { "kind": "string", "params": { "maxLen": 3 } } ] }
            """;

    private static JsonObject json(String text) {
        return JsonParser.parseString(text).getAsJsonObject();
    }

    private static SutSpec javaSpec(String method) {
        return new SutSpec("java", Methods.class.getName(), method, true, null, null, null, null, null);
    }

    @Test
    void tuple_generator_builds_one_value_per_component() {
        var generator = new TupleGeneratorProvider().build(json(TUPLE_PARAMS));

        for (int i = 0; i < 20; i++) {
            Tuple t = generator.generate(new Random(i));
            assertEquals(2, t.values().size());
            assertInstanceOf(Integer.class, t.values().get(0));
            assertInstanceOf(String.class, t.values().get(1));
        }
    }

    @Test
    void tuple_generator_requires_components() {
        assertThrows(IllegalArgumentException.class, () -> new TupleGeneratorProvider().build(json("{}")));
        assertThrows(IllegalArgumentException.class,
                () -> new TupleGeneratorProvider().build(json("{\"components\": []}")));
    }

    @Test
    void tuple_shrinker_shrinks_one_component_at_a_time() {
        var shrinker = new TupleShrinkerProvider().build(json(TUPLE_PARAMS));

        List<Tuple> candidates = shrinker.shrink(new Tuple(List.of(4, "ab"))).toList();

        assertTrue(candidates.contains(new Tuple(List.of(0, "ab"))));
        assertTrue(candidates.contains(new Tuple(List.of(4, "a"))));
        assertTrue(candidates.stream().noneMatch(t -> t.equals(new Tuple(List.of(4, "ab")))));
        assertTrue(candidates.stream().allMatch(t ->
                t.values().get(0).equals(4) || t.values().get(1).equals("ab")));
    }

    @Test
    void tuple_facts_prefix_each_component_with_its_position() {
        var fg = new TupleAspFactGeneratorProvider().withGeneratorParams(json(TUPLE_PARAMS));

        assertEquals("in_0(3).\nin_1(\"ab\").\n", fg.toFacts(new Tuple(List.of(3, "ab")), "in"));
        assertEquals("", fg.toFacts(null, "out"));
    }

    @Test
    void a_component_can_name_its_own_fact_generator() {
        var fg = new TupleAspFactGeneratorProvider().withGeneratorParams(json("""
                { "components": [ { "kind": "int" }, { "kind": "string", "factGenerator": "long" } ] }
                """));

        assertEquals("in_0(3).\nin_1(ab).\n", fg.toFacts(new Tuple(List.of(3, "ab")), "in"));
    }

    @Test
    void tuple_facts_reject_a_plain_value() {
        var fg = new TupleAspFactGeneratorProvider().withGeneratorParams(json(TUPLE_PARAMS));

        var e = assertThrows(IllegalArgumentException.class, () -> fg.toFacts(7, "out"));
        assertTrue(e.getMessage().contains("outputKind"), e.getMessage());
    }

    @Test
    void java_runner_spreads_a_tuple_over_the_parameters() {
        var provider = new JavaSutRunnerProvider();

        assertEquals("ab:2", provider.build(javaSpec("describe(String, int)")).run(new Tuple(List.of("ab", 2))));
        assertEquals(7, provider.build(javaSpec("sum(List<Integer>, int)"))
                .run(new Tuple(List.of(List.of(1, 2), 4))));
    }

    @Test
    void java_runner_selects_overloads_by_arity() {
        var provider = new JavaSutRunnerProvider();

        assertEquals("one", provider.build(javaSpec("arity(String)")).run("x"));
        assertEquals("two", provider.build(javaSpec("arity(String, String)")).run(new Tuple(List.of("x", "y"))));
        assertThrows(IllegalArgumentException.class, () -> provider.build(javaSpec("arity")));
    }

    @Test
    void java_runner_rejects_inputs_of_the_wrong_shape() {
        var runner = new JavaSutRunnerProvider().build(javaSpec("describe(String, int)"));

        var plain = assertThrows(IllegalArgumentException.class, () -> runner.run("ab"));
        assertTrue(plain.getMessage().contains("tuple"), plain.getMessage());
        assertThrows(IllegalArgumentException.class, () -> runner.run(new Tuple(List.of("ab"))));
    }

    @Test
    void python_runner_spreads_a_tuple() {
        assumeTrue(pythonAvailable(), "python3 not on PATH");
        SutSpec spec = new SutSpec("python", null, "add(int, int)", false, null,
                "src/test/resources/python/test_add.py", null, null, "int");
        SutRunner runner = new PythonSutRunnerProvider().build(spec);
        try {
            assertEquals(5, runner.run(new Tuple(List.of(2, 3))));
        } finally {
            runner.close();
        }
    }

    @Test
    void pipeline_checks_a_two_argument_sut_and_shrinks_per_component() {
        assumeTrue(new CliAspEngine().isAvailable(), "clingo not available");
        SuiteDescriptor suite = SuiteLoader.parse(new StringReader("""
                {
                  "name": "add",
                  "suts": { "ref": { "type": "java" } },
                  "generator": { "kind": "tuple", "params": { "components": [
                      { "kind": "int", "params": { "min": -20, "max": 20 } },
                      { "kind": "int", "params": { "min": -20, "max": 20 } } ] } },
                  "outputKind": "int",
                  "properties": [
                    { "id": "sum", "type": "INVARIANT",
                      "validator": { "kind": "asp", "relation": "asp/sum_matches.lp" } } ]
                }
                """), "inline");
        var good = new JavaSutRunnerProvider().build(javaSpec("add(int, int)"));
        var bad = new JavaSutRunnerProvider().build(javaSpec("subtract(int, int)"));

        assertTrue(runner().run(suite, good).passed());
        PropertyResult failed = runner().run(suite, bad).properties().get(0);

        assertFalse(failed.passed());
        Tuple witness = assertInstanceOf(Tuple.class, failed.witness().orElseThrow());
        assertEquals(0, witness.values().get(0));
        assertNotEquals(0, witness.values().get(1));
    }

    private static boolean pythonAvailable() {
        try {
            return new ProcessBuilder("python3", "--version").start().waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
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

    public static final class Methods {

        public static String describe(String s, int n) {
            return s + ":" + n;
        }

        public static int sum(List<Integer> xs, int extra) {
            return xs.stream().mapToInt(Integer::intValue).sum() + extra;
        }

        public static String arity(String a) {
            return "one";
        }

        public static String arity(String a, String b) {
            return "two";
        }

        public static int add(int a, int b) {
            return a + b;
        }

        public static int subtract(int a, int b) {
            return a - b;
        }
    }
}
