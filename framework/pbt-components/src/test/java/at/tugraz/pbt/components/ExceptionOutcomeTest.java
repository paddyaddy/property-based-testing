package at.tugraz.pbt.components;

import org.junit.jupiter.api.Test;
import at.tugraz.pbt.components.asp.CliAspEngine;
import at.tugraz.pbt.components.sut.JavaSutRunnerProvider;
import at.tugraz.pbt.components.sut.PythonSutRunnerProvider;
import at.tugraz.pbt.descriptor.SuiteDescriptor;
import at.tugraz.pbt.descriptor.SuiteLoader;
import at.tugraz.pbt.descriptor.SutSpec;
import at.tugraz.pbt.pipeline.PipelineRunner;
import at.tugraz.pbt.pipeline.dto.PropertyResult;
import at.tugraz.pbt.spi.Registry;
import at.tugraz.pbt.spi.generator.GeneratorProvider;
import at.tugraz.pbt.spi.shrinker.ShrinkerProvider;
import at.tugraz.pbt.spi.sut.SutException;
import at.tugraz.pbt.spi.sut.SutRunner;
import at.tugraz.pbt.spi.transform.TransformProvider;
import at.tugraz.pbt.spi.type.Tuple;
import at.tugraz.pbt.spi.type.ValueTypeProvider;
import at.tugraz.pbt.spi.validator.ValidatorProvider;

import java.io.StringReader;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class ExceptionOutcomeTest {

    private static SutRunner javaRunner(String method) {
        return new JavaSutRunnerProvider().build(new SutSpec("java", Methods.class.getName(), method, true,
                null, null, null, null, null));
    }

    private static SuiteDescriptor suite(String validator) {
        return SuiteLoader.parse(new StringReader("""
                {
                  "name": "exceptions",
                  "suts": { "ref": { "type": "java" } },
                  "generator": { "kind": "int", "params": { "min": -5, "max": 5 } },
                  "outputKind": "int",
                  "properties": [ { "id": "P", "type": "INVARIANT", "validator": %s } ]
                }
                """.formatted(validator)), "inline");
    }

    private static PropertyResult run(SuiteDescriptor suite, SutRunner sut) {
        return new PipelineRunner(
                Registry.load(GeneratorProvider.class),
                Registry.load(ShrinkerProvider.class),
                Registry.load(ValidatorProvider.class),
                Registry.load(TransformProvider.class),
                Registry.load(ValueTypeProvider.class),
                50,
                42L).run(suite, sut).properties().get(0);
    }

    @Test
    void java_runner_reports_the_exception_the_sut_raised() {
        SutException e = assertThrows(SutException.class, () -> javaRunner("parse(Integer)").run(-1));

        assertEquals("java.lang.NumberFormatException", e.exceptionType());
        assertEquals("negative", e.getMessage());
    }

    @Test
    void java_runner_reports_errors_as_well_as_exceptions() {
        SutException e = assertThrows(SutException.class, () -> javaRunner("recurse(Integer)").run(1));

        assertEquals("java.lang.StackOverflowError", e.exceptionType());
    }

    @Test
    void wiring_mistakes_are_not_sut_outcomes() {
        var runner = javaRunner("describe(String, Integer)");

        assertThrows(IllegalArgumentException.class, () -> runner.run("only one value"));
        assertDoesNotThrow(() -> runner.run(new Tuple(List.of("a", 1))));
    }

    @Test
    void wiring_mistakes_abort_the_run() {
        var wrongShape = javaRunner("describe(String, Integer)");

        assertThrows(IllegalArgumentException.class, () -> run(suite("{ \"kind\": \"trivial\" }"), wrongShape));
    }

    @Test
    void oracle_sees_the_exception_as_a_fact() {
        assumeTrue(new CliAspEngine().isAvailable(), "clingo not available");
        var validator = "{ \"kind\": \"asp\", \"relation\": \"asp/expects_number_format_exception.lp\" }";

        assertTrue(run(suite(validator), javaRunner("parse(Integer)")).passed());
        PropertyResult unexpected = run(suite(validator), javaRunner("alwaysFails(Integer)"));

        assertFalse(unexpected.passed());
        assertTrue(unexpected.runLog().stream().anyMatch(l -> l.contains("exception(\"java.lang.IllegalStateException\")")));
    }

    @Test
    void fail_on_exception_fails_without_a_program() {
        var validator = "{ \"kind\": \"asp\", \"relation\": \"asp/always_pass.lp\", \"failOnException\": true }";

        PropertyResult result = run(suite(validator), javaRunner("parse(Integer)"));

        assertFalse(result.passed());
        assertEquals(-1, result.witness().orElseThrow());
        assertTrue(result.runLog().stream().anyMatch(l -> l.contains("exception:java.lang.NumberFormatException")));
    }

    @Test
    void exceptions_are_ignored_unless_the_program_or_the_flag_asks() {
        var validator = "{ \"kind\": \"asp\", \"relation\": \"asp/always_pass.lp\" }";

        assertTrue(run(suite(validator), javaRunner("parse(Integer)")).passed());
    }

    @Test
    void python_runner_reports_the_exception_type() {
        assumeTrue(pythonAvailable(), "python3 not on PATH");
        SutRunner runner = new PythonSutRunnerProvider().build(new SutSpec("python", null, "fail_on_negative(int)",
                false, null, "src/test/resources/python/test_add.py", null, null, "int"));
        try {
            assertEquals(3, runner.run(3));
            SutException e = assertThrows(SutException.class, () -> runner.run(-1));
            assertEquals("ValueError", e.exceptionType());
        } finally {
            runner.close();
        }
    }

    private static boolean pythonAvailable() {
        try {
            return new ProcessBuilder("python3", "--version").start().waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    public static final class Methods {

        public static int parse(Integer n) {
            if (n < 0) throw new NumberFormatException("negative");
            return n;
        }

        public static int alwaysFails(Integer n) {
            throw new IllegalStateException("always");
        }

        public static int recurse(Integer n) {
            return recurse(n + 1);
        }

        public static String describe(String s, Integer n) {
            return s + n;
        }
    }
}
