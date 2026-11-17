package at.tugraz.pbt.pipeline;

import com.google.gson.JsonObject;
import at.tugraz.pbt.artifact.ArtifactCodec;
import at.tugraz.pbt.descriptor.PropertyDescriptor;
import at.tugraz.pbt.descriptor.PropertyKind;
import at.tugraz.pbt.descriptor.SuiteDescriptor;
import at.tugraz.pbt.pipeline.dto.PropertyResult;
import at.tugraz.pbt.pipeline.dto.PropertyResultArtifact;
import at.tugraz.pbt.pipeline.dto.SuiteResult;
import at.tugraz.pbt.spi.Registry;
import at.tugraz.pbt.spi.asp.AspEngine;
import at.tugraz.pbt.spi.asp.facts.AspFactGeneratorProvider;
import at.tugraz.pbt.spi.generator.Generator;
import at.tugraz.pbt.spi.generator.GeneratorProvider;
import at.tugraz.pbt.spi.shrinker.Shrinker;
import at.tugraz.pbt.spi.shrinker.ShrinkerProvider;
import at.tugraz.pbt.spi.sut.SutException;
import at.tugraz.pbt.spi.sut.SutRunner;
import at.tugraz.pbt.spi.transform.Transform;
import at.tugraz.pbt.spi.transform.TransformProvider;
import at.tugraz.pbt.spi.type.ValueTypeProvider;
import at.tugraz.pbt.spi.validator.CheckContext;
import at.tugraz.pbt.spi.validator.CheckResult;
import at.tugraz.pbt.spi.validator.Validator;
import at.tugraz.pbt.spi.validator.ValidatorProvider;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Runs a suite descriptor: generates inputs, optionally executes a SUT, validates each
 * property, and minimizes any failing witness via the shrinker.
 */
public final class PipelineRunner {

    /**
     * Default upper bound on candidate evaluations while minimizing one witness.
     */
    public static final int DEFAULT_MAX_SHRINK_STEPS = 1000;

    private final Registry<GeneratorProvider> generators;
    private final Registry<ShrinkerProvider> shrinkers;
    private final Registry<ValidatorProvider> validators;
    private final Registry<TransformProvider> transforms;
    private final Registry<ValueTypeProvider> valueTypes;
    private final Registry<AspFactGeneratorProvider> factGenerators;
    private final AspEngine aspEngine;
    private final int runsPerProperty;
    private final long seed;
    private final int maxShrinkSteps;
    private final Path artifactsDir;
    private final ArtifactCodec codec;

    public PipelineRunner(
            Registry<GeneratorProvider> generators,
            Registry<ShrinkerProvider> shrinkers,
            Registry<ValidatorProvider> validators,
            Registry<TransformProvider> transforms,
            Registry<ValueTypeProvider> valueTypes,
            int runsPerProperty,
            long seed) {
        this(generators, shrinkers, validators, transforms, valueTypes, runsPerProperty, seed, null);
    }

    public PipelineRunner(
            Registry<GeneratorProvider> generators,
            Registry<ShrinkerProvider> shrinkers,
            Registry<ValidatorProvider> validators,
            Registry<TransformProvider> transforms,
            Registry<ValueTypeProvider> valueTypes,
            int runsPerProperty,
            long seed,
            Path artifactsDir) {
        this(generators, shrinkers, validators, transforms, valueTypes, runsPerProperty, seed,
                artifactsDir, DEFAULT_MAX_SHRINK_STEPS);
    }

    public PipelineRunner(
            Registry<GeneratorProvider> generators,
            Registry<ShrinkerProvider> shrinkers,
            Registry<ValidatorProvider> validators,
            Registry<TransformProvider> transforms,
            Registry<ValueTypeProvider> valueTypes,
            int runsPerProperty,
            long seed,
            Path artifactsDir,
            int maxShrinkSteps) {
        if (maxShrinkSteps < 0) {
            throw new IllegalArgumentException("maxShrinkSteps must not be negative");
        }
        this.generators = generators;
        this.shrinkers = shrinkers;
        this.validators = validators;
        this.transforms = transforms;
        this.valueTypes = valueTypes;
        this.factGenerators = Registry.load(AspFactGeneratorProvider.class);
        this.aspEngine = ServiceLoader.load(AspEngine.class).findFirst().orElse(null);
        this.runsPerProperty = runsPerProperty;
        this.seed = seed;
        this.maxShrinkSteps = maxShrinkSteps;
        this.artifactsDir = artifactsDir;
        this.codec = artifactsDir != null ? new ArtifactCodec() : null;
    }

    private static String formatRun(int index, CheckContext ctx, CheckResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append("--- Run ").append(index).append(" ---\n");
        if (ctx.followUpInput() != null) {
            sb.append("  sourceInput:    ").append(ctx.sourceInput()).append("\n");
            if (ctx.sourceOutput() != null)
                sb.append("  sourceOutput:   ").append(ctx.sourceOutput()).append("\n");
            if (ctx.sourceException() != null)
                sb.append("  sourceException: ").append(ctx.sourceException()).append("\n");
            sb.append("  followUpInput:  ").append(ctx.followUpInput()).append("\n");
            if (ctx.followUpOutput() != null)
                sb.append("  followUpOutput: ").append(ctx.followUpOutput()).append("\n");
            if (ctx.followUpException() != null)
                sb.append("  followUpException: ").append(ctx.followUpException()).append("\n");
        } else {
            sb.append("  input:  ").append(ctx.sourceInput()).append("\n");
            if (ctx.sourceOutput() != null)
                sb.append("  output: ").append(ctx.sourceOutput()).append("\n");
            if (ctx.sourceException() != null)
                sb.append("  exception: ").append(ctx.sourceException()).append("\n");
        }
        if (!result.diagnostics().isEmpty()) {
            sb.append("facts:\n").append(result.diagnostics().trim()).append("\n");
        }
        sb.append("result: ").append(result.passed() ? "PASS" : "FAIL (" + result.signature() + ")").append("\n");
        return sb.toString();
    }

    private static CheckContext buildContext(
            Object input,
            boolean isMetamorphic,
            Transform transform,
            Random transformRng,
            SutRunner sutRunner) {
        Object sourceOutput = null;
        Object followUpInput = null;
        Object followUpOutput = null;
        String sourceException = null;
        String followUpException = null;

        if (isMetamorphic) {
            followUpInput = transform.apply(input, transformRng);
        }
        if (sutRunner != null) {
            try {
                sourceOutput = sutRunner.run(input);
            } catch (SutException e) {
                sourceException = e.exceptionType();
            }
            if (isMetamorphic) {
                try {
                    followUpOutput = sutRunner.run(followUpInput);
                } catch (SutException e) {
                    followUpException = e.exceptionType();
                }
            }
        }
        return new CheckContext(input, sourceOutput, followUpInput, followUpOutput,
                sourceException, followUpException);
    }

    private static Object minimize(
            Object failing,
            Shrinker<Object> shrinker,
            Transform transform,
            Validator validator,
            boolean isMetamorphic,
            SutRunner sutRunner,
            Predicate<Object> applicability,
            ValueTypeProvider valueType,
            long baseSeed,
            int maxSteps,
            List<String> log) {
        Object current = failing;
        int steps = 0;
        boolean improved = true;
        log.add("=== Shrinking witness: " + valueType.describe(failing) + " ===");
        search:
        while (improved) {
            improved = false;
            for (Object candidate : (Iterable<Object>) shrinker.shrink(current)::iterator) {
                if (applicability != null && !applicability.test(candidate)) {
                    log.add("  " + valueType.describe(candidate) + " -> SKIPPED (not applicable)");
                    continue;
                }
                if (steps >= maxSteps) {
                    log.add("Shrink step cap reached (" + maxSteps + " candidates evaluated)");
                    break search;
                }
                steps++;
                Random rng = new Random(baseSeed ^ (long) Objects.hashCode(candidate));
                CheckContext ctx = buildContext(candidate, isMetamorphic, transform, rng, sutRunner);
                CheckResult result = validator.check(ctx);
                if (!result.passed()) {
                    log.add("  " + valueType.describe(candidate) + " -> FAIL (reduced)");
                    current = candidate;
                    improved = true;
                    break;
                } else {
                    log.add("  " + valueType.describe(candidate) + " -> PASS");
                }
            }
        }
        log.add("Minimal witness: " + valueType.describe(current));
        return current;
    }

    public SuiteResult run(SuiteDescriptor descriptor) {
        return run(descriptor, null);
    }

    @SuppressWarnings("unchecked")
    public SuiteResult run(SuiteDescriptor descriptor, SutRunner sutRunner) {
        String generatorKind = descriptor.generator().kind();
        String factGeneratorKind = descriptor.factGenerator() != null
                ? descriptor.factGenerator() : generatorKind;
        JsonObject genParams = descriptor.generator().params() != null
                ? descriptor.generator().params() : new JsonObject();

        Generator<Object> generator = (Generator<Object>)
                generators.get(generatorKind).build(genParams);
        Shrinker<Object> shrinker = shrinkers.has(generatorKind)
                ? (Shrinker<Object>) shrinkers.get(generatorKind).build(genParams)
                : v -> Stream.empty();

        List<Object> sharedInputs = descriptor.generator().regenerate()
                ? null
                : generateAll(generator, new Random(seed));

        ValueTypeProvider valueType = valueTypes.get(generatorKind);
        List<PropertyResult> results = new ArrayList<>();
        for (PropertyDescriptor pd : descriptor.properties()) {
            List<Object> inputs = sharedInputs != null
                    ? sharedInputs
                    : generateAll(generator, new Random(seed ^ (long) pd.id().hashCode()));
            Predicate<Object> applicability = buildApplicabilityCheck(pd, factGeneratorKind, genParams);
            PropertyResult r = runProperty(pd, inputs, descriptor, shrinker, sutRunner,
                    applicability, valueType);
            if (artifactsDir != null) {
                var artifact = new PropertyResultArtifact(
                        r.propertyId(), r.passed(), r.generated(), r.runs(), r.skipped(), r.failures(),
                        r.witness().map(w -> codec.toCompactJson(w, valueType.javaType())).orElse(null));
                codec.write(artifactsDir.resolve(r.propertyId() + ".json"),
                        artifact, PropertyResultArtifact.class);
            }
            results.add(r);
        }
        return new SuiteResult(descriptor.name(), results);
    }

    private List<Object> generateAll(Generator<Object> generator, Random rng) {
        List<Object> inputs = new ArrayList<>(runsPerProperty);
        for (int i = 0; i < runsPerProperty; i++) {
            inputs.add(generator.generate(rng));
        }
        return inputs;
    }

    private PropertyResult runProperty(
            PropertyDescriptor pd,
            List<Object> inputs,
            SuiteDescriptor descriptor,
            Shrinker<Object> shrinker,
            SutRunner sutRunner,
            Predicate<Object> applicability,
            ValueTypeProvider valueType) {

        Validator validator = validators
                .get(pd.validator().get("kind").getAsString())
                .build(pd.validator(), descriptor);

        boolean isMetamorphic = pd.type() == PropertyKind.METAMORPHIC;
        Transform transform = isMetamorphic ? resolveTransform(pd.transform()) : null;

        int failures = 0;
        int skipped = 0;
        Object firstFailingInput = null;
        Random transformRng = new Random(seed ^ (long) pd.id().hashCode() ^ 0xCAFEL);
        List<String> log = new ArrayList<>();

        for (int i = 0; i < inputs.size(); i++) {
            Object input = inputs.get(i);
            if (applicability != null && !applicability.test(input)) {
                skipped++;
                log.add("--- Run " + (i + 1) + " --- SKIPPED\n  input:  " + input + "\n");
                continue;
            }
            CheckContext ctx = buildContext(input, isMetamorphic, transform, transformRng, sutRunner);
            CheckResult result = validator.check(ctx);
            log.add(formatRun(i + 1, ctx, result));
            if (!result.passed()) {
                failures++;
                if (firstFailingInput == null) {
                    firstFailingInput = input;
                }
            }
        }

        Optional<Object> witness = Optional.empty();
        if (firstFailingInput != null) {
            witness = Optional.of(minimize(firstFailingInput, shrinker, transform, validator,
                    isMetamorphic, sutRunner, applicability, valueType,
                    seed ^ (long) pd.id().hashCode(), maxShrinkSteps, log));
        }
        return new PropertyResult(pd.id(), failures == 0, inputs.size(), inputs.size() - skipped,
                skipped, failures, witness, log);
    }

    private Predicate<Object> buildApplicabilityCheck(
            PropertyDescriptor pd, String factGeneratorKind, JsonObject generatorParams) {
        String path = pd.applicability();
        if (path == null || path.isBlank()) return null;
        String program = AspPrograms.load(path);
        if (aspEngine == null) {
            throw new IllegalStateException("property '" + pd.id() + "' declares applicability '"
                    + path + "' but no AspEngine service is on the classpath");
        }
        if (!aspEngine.isAvailable()) {
            throw new IllegalStateException("property '" + pd.id() + "' declares applicability '"
                    + path + "' but the ASP engine " + aspEngine.name() + " is not available");
        }
        AspFactGeneratorProvider fg = factGenerators.get(factGeneratorKind).withGeneratorParams(generatorParams);
        return input -> !aspEngine.entails(fg.toFacts(input, "in") + "\n" + program, "skip");
    }

    private Transform resolveTransform(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("metamorphic property missing a 'transform'");
        }
        return transforms.get(key).get();
    }
}
