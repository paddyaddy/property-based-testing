package pbt.components.validator;

import com.google.gson.JsonObject;
import pbt.components.asp.CliAspEngine;
import pbt.descriptor.SuiteDescriptor;
import pbt.pipeline.AspPrograms;
import pbt.spi.Registry;
import pbt.spi.asp.AspEngine;
import pbt.spi.asp.facts.AspFactGeneratorProvider;
import pbt.spi.validator.CheckContext;
import pbt.spi.validator.CheckResult;
import pbt.spi.validator.Validator;
import pbt.spi.validator.ValidatorProvider;

import java.util.ServiceLoader;

/**
 * Builds ASP-backed validators for both invariant and metamorphic properties.
 *
 * <p>The {@code validator} descriptor object must contain a {@code "relation"} field
 * naming a classpath resource with the ASP rules. The program may {@code #include} other classpath
 * resources, see {@link AspPrograms}. An optional {@code "violation"}
 * field overrides the default query atom {@code "violation"}, and
 * {@code "failureSignature"} overrides the default failure label. Other fields are
 * passed to the fact renderer (for example {@code "scale"} for {@code double} values).
 * With {@code "failOnException": true} an execution in which the SUT raised an exception fails
 * with the signature {@code exception:<type>} without consulting the program.
 * The solver is the first {@link AspEngine} service found, falling back to
 * {@link CliAspEngine}.
 *
 * <p>For <b>invariant</b> properties the program is:
 * <pre>  toFacts(input, "in") + toFacts(sutOutput, "out") + relationProgram</pre>
 * For <b>metamorphic</b> properties (detected via null/non-null {@code followUpInput}):
 * <pre>  toFacts(sourceInput, "source_in") + toFacts(sourceOutput, "source_out")
 *   + toFacts(followUpInput, "followup_in") + toFacts(followUpOutput, "followup_out")
 *   + relationProgram</pre>
 * When the SUT raised an exception its output facts are absent and one fact names the type:
 * {@code exception("java.lang.NullPointerException").}, or {@code source_exception(...)} and
 * {@code followup_exception(...)} for metamorphic properties.
 */
public final class AspValidatorProvider implements ValidatorProvider {

    private final AspEngine engine;
    private final Registry<AspFactGeneratorProvider> factGenerators;

    public AspValidatorProvider() {
        this.engine = ServiceLoader.load(AspEngine.class).findFirst().orElseGet(CliAspEngine::new);
        this.factGenerators = Registry.load(AspFactGeneratorProvider.class);
    }

    private static String exceptionFact(String type, String predicate) {
        if (type == null) return "";
        return predicate + "(\"" + type.replace("\\", "\\\\").replace("\"", "\\\"") + "\").\n";
    }

    private static String buildFacts(CheckContext ctx, AspFactGeneratorProvider in, AspFactGeneratorProvider out) {
        if (ctx.followUpInput() != null) {
            return in.toFacts(ctx.sourceInput(), "source_in")
                    + out.toFacts(ctx.sourceOutput(), "source_out")
                    + exceptionFact(ctx.sourceException(), "source_exception")
                    + in.toFacts(ctx.followUpInput(), "followup_in")
                    + out.toFacts(ctx.followUpOutput(), "followup_out")
                    + exceptionFact(ctx.followUpException(), "followup_exception");
        }
        return in.toFacts(ctx.sourceInput(), "in")
                + out.toFacts(ctx.sourceOutput(), "out")
                + exceptionFact(ctx.sourceException(), "exception");
    }

    private static boolean flag(JsonObject params, String key) {
        return params != null && params.has(key) && params.get(key).getAsBoolean();
    }

    private static String loadRelation(JsonObject params) {
        if (params == null || !params.has("relation") || params.get("relation").isJsonNull()) {
            throw new IllegalArgumentException("ASP validator missing required 'relation' param");
        }
        return AspPrograms.load(params.get("relation").getAsString());
    }

    private static String stringOrDefault(JsonObject params, String key, String defaultValue) {
        if (params == null || !params.has(key) || params.get(key).isJsonNull()) {
            return defaultValue;
        }
        return params.get(key).getAsString();
    }

    @Override
    public String name() {
        return "asp";
    }

    @Override
    public Validator build(JsonObject params, String generatorKind) {
        AspFactGeneratorProvider fg = factGenerators.get(generatorKind).withParams(params);
        return build(params, fg, fg);
    }

    /**
     * Renders inputs with the suite's fact generator (configured by the generator params) and
     * outputs with the {@code outputKind} fact generator, or like inputs when none is set.
     */
    @Override
    public Validator build(JsonObject params, SuiteDescriptor suite) {
        String inputKind = suite.factGenerator() != null ? suite.factGenerator() : suite.generator().kind();
        JsonObject generatorParams = suite.generator().params() != null
                ? suite.generator().params() : new JsonObject();
        AspFactGeneratorProvider in = factGenerators.get(inputKind)
                .withGeneratorParams(generatorParams).withParams(params);
        AspFactGeneratorProvider out = suite.outputKind() != null
                ? factGenerators.get(suite.outputKind()).withParams(params) : in;
        return build(params, in, out);
    }

    private Validator build(JsonObject params, AspFactGeneratorProvider in, AspFactGeneratorProvider out) {
        String relation = loadRelation(params);
        String violationAtom = stringOrDefault(params, "violation", "violation");
        String defaultSignature = stringOrDefault(params, "failureSignature", violationAtom);

        boolean failOnException = flag(params, "failOnException");
        return ctx -> {
            String facts = buildFacts(ctx, in, out);
            String raised = ctx.sourceException() != null ? ctx.sourceException() : ctx.followUpException();
            if (failOnException && raised != null) {
                return CheckResult.fail("exception:" + raised, facts);
            }
            String program = facts + "\n" + relation;
            if (engine.entails(program, violationAtom)) {
                return CheckResult.fail(defaultSignature, facts);
            }
            return CheckResult.pass(facts);
        };
    }
}
