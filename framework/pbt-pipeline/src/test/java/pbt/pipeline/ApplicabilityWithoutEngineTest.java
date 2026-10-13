package pbt.pipeline;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;
import pbt.descriptor.SuiteDescriptor;
import pbt.descriptor.SuiteLoader;
import pbt.spi.Registry;
import pbt.spi.generator.Generator;
import pbt.spi.generator.GeneratorProvider;
import pbt.spi.shrinker.ShrinkerProvider;
import pbt.spi.transform.TransformProvider;
import pbt.spi.type.ValueTypeProvider;
import pbt.spi.validator.CheckResult;
import pbt.spi.validator.Validator;
import pbt.spi.validator.ValidatorProvider;

import java.io.StringReader;
import java.lang.reflect.Type;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * pbt-pipeline has no AspEngine on its classpath, so applicability cannot be evaluated.
 */
class ApplicabilityWithoutEngineTest {

    @Test
    void declared_applicability_without_an_engine_fails_with_a_clear_message() {
        SuiteDescriptor d = SuiteLoader.parse(new StringReader("""
                {
                  "name": "s",
                  "suts": { "ref": { "type": "java" } },
                  "generator": { "kind": "one" },
                  "properties": [ {
                    "id": "P", "type": "INVARIANT",
                    "applicability": "descriptors/sample-suite.json",
                    "validator": { "kind": "pass" }
                  } ]
                }
                """), "inline");
        PipelineRunner runner = new PipelineRunner(
                Registry.load(GeneratorProvider.class),
                Registry.load(ShrinkerProvider.class),
                Registry.load(ValidatorProvider.class),
                Registry.load(TransformProvider.class),
                Registry.load(ValueTypeProvider.class),
                3, 1L);

        IllegalStateException e = assertThrows(IllegalStateException.class, () -> runner.run(d));

        assertTrue(e.getMessage().contains("no AspEngine"), e.getMessage());
    }

    public static final class OneGenerator implements GeneratorProvider {
        @Override
        public String name() {
            return "one";
        }

        @Override
        public Generator<?> build(JsonObject params) {
            return rnd -> 1;
        }
    }

    public static final class IntType implements ValueTypeProvider {
        @Override
        public String name() {
            return "one";
        }

        @Override
        public Type javaType() {
            return Integer.class;
        }

        @Override
        public String describe(Object value) {
            return String.valueOf(value);
        }
    }

    public static final class PassValidator implements ValidatorProvider {
        @Override
        public String name() {
            return "pass";
        }

        @Override
        public Validator build(JsonObject params, String generatorKind) {
            return ctx -> CheckResult.pass();
        }
    }
}
