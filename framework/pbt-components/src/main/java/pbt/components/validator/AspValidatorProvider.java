package pbt.components.validator;

import com.google.gson.JsonObject;
import pbt.components.asp.CliAspEngine;
import pbt.spi.Registry;
import pbt.spi.asp.AspEngine;
import pbt.spi.asp.facts.AspFactGeneratorProvider;
import pbt.spi.validator.CheckContext;
import pbt.spi.validator.CheckResult;
import pbt.spi.validator.Validator;
import pbt.spi.validator.ValidatorProvider;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ServiceLoader;

/**
 * Builds ASP-backed validators for both invariant and metamorphic properties.
 *
 * <p>The {@code validator} descriptor object must contain a {@code "relation"} field
 * naming a classpath resource with the ASP rules. An optional {@code "violation"}
 * field overrides the default query atom {@code "violation"}, and
 * {@code "failureSignature"} overrides the default failure label. Other fields are
 * passed to the fact renderer (for example {@code "scale"} for {@code double} values).
 * The solver is the first {@link AspEngine} service found, falling back to
 * {@link CliAspEngine}.
 *
 * <p>For <b>invariant</b> properties the program is:
 * <pre>  toFacts(input, "in") + toFacts(sutOutput, "out") + relationProgram</pre>
 * For <b>metamorphic</b> properties (detected via null/non-null {@code followUpInput}):
 * <pre>  toFacts(sourceInput, "source_in") + toFacts(sourceOutput, "source_out")
 *   + toFacts(followUpInput, "followup_in") + toFacts(followUpOutput, "followup_out")
 *   + relationProgram</pre>
 */
public final class AspValidatorProvider implements ValidatorProvider {

    private final AspEngine engine;
    private final Registry<AspFactGeneratorProvider> factGenerators;

    public AspValidatorProvider() {
        this.engine = ServiceLoader.load(AspEngine.class).findFirst().orElseGet(CliAspEngine::new);
        this.factGenerators = Registry.load(AspFactGeneratorProvider.class);
    }

    private static String buildFacts(CheckContext ctx, AspFactGeneratorProvider fg) {
        if (ctx.followUpInput() != null) {
            return fg.toFacts(ctx.sourceInput(), "source_in")
                    + fg.toFacts(ctx.sourceOutput(), "source_out")
                    + fg.toFacts(ctx.followUpInput(), "followup_in")
                    + fg.toFacts(ctx.followUpOutput(), "followup_out");
        }
        return fg.toFacts(ctx.sourceInput(), "in")
                + fg.toFacts(ctx.sourceOutput(), "out");
    }

    private static String loadRelation(JsonObject params) {
        if (params == null || !params.has("relation") || params.get("relation").isJsonNull()) {
            throw new IllegalArgumentException("ASP validator missing required 'relation' param");
        }
        String path = params.get("relation").getAsString();
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try (InputStream in = cl.getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalArgumentException("ASP relation resource not found: " + path);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read ASP relation: " + path, e);
        }
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
        String relation = loadRelation(params);
        String violationAtom = stringOrDefault(params, "violation", "violation");
        String defaultSignature = stringOrDefault(params, "failureSignature", violationAtom);

        return ctx -> {
            String facts = buildFacts(ctx, fg);
            String program = facts + "\n" + relation;
            if (engine.entails(program, violationAtom)) {
                return CheckResult.fail(defaultSignature, facts);
            }
            return CheckResult.pass(facts);
        };
    }
}
