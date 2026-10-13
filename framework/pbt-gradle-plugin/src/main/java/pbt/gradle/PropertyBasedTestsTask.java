package pbt.gradle;

import com.google.gson.JsonObject;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;
import org.gradle.api.tasks.testing.GroupTestEventReporter;
import org.gradle.api.tasks.testing.TestEventReporter;
import org.gradle.api.tasks.testing.TestEventReporterFactory;
import org.gradle.api.tasks.testing.TestOutputEvent;
import pbt.descriptor.PropertyDescriptor;
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
import pbt.spi.sut.SutRunnerProvider;
import pbt.spi.transform.TransformProvider;
import pbt.spi.type.ValueTypeProvider;
import pbt.spi.validator.ValidatorProvider;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Discovers suite descriptor JSON files and runs the PBT pipeline against each SUT.
 */
public abstract class PropertyBasedTestsTask extends DefaultTask {

    private static String formatSutInfo(
            SuiteDescriptor descriptor, String sutName, SutSpec spec,
            int runsPerProperty, long seed) {
        StringBuilder sb = new StringBuilder();
        sb.append("Suite:             ").append(descriptor.name()).append("\n");
        if (descriptor.description() != null && !descriptor.description().isBlank()) {
            sb.append("Description:       ").append(descriptor.description()).append("\n");
        }
        sb.append("Generator:         ").append(descriptor.generator().kind());
        if (descriptor.generator().params() != null && descriptor.generator().params().size() > 0) {
            sb.append("  params=").append(descriptor.generator().params());
        }
        sb.append("\n");
        sb.append("Regenerate inputs: ").append(descriptor.generator().regenerate()).append("\n");
        if (descriptor.factGenerator() != null) {
            sb.append("Fact generator:    ").append(descriptor.factGenerator()).append("\n");
        }
        sb.append("SUT \"").append(sutName).append("\":\n");
        sb.append("  type:   ").append(spec.type()).append("\n");
        if (spec.className() != null) sb.append("  class:  ").append(spec.className()).append("\n");
        if (spec.method() != null) sb.append("  method: ").append(spec.method()).append("\n");
        if (spec.file() != null) sb.append("  file:   ").append(spec.file()).append("\n");
        if (spec.outputType() != null) sb.append("  outputType: ").append(spec.outputType()).append("\n");
        if (spec.interpreter() != null) sb.append("  interpreter: ").append(spec.interpreter()).append("\n");
        if (spec.timeoutSeconds() != null) sb.append("  timeout: ").append(spec.timeoutSeconds()).append("s\n");
        sb.append("Runs per property: ").append(runsPerProperty).append("\n");
        sb.append("Seed:              ").append(seed).append("\n");
        return sb.toString();
    }

    private static String formatPropertyHeader(SuiteDescriptor descriptor, String propertyId) {
        PropertyDescriptor pd = descriptor.properties().stream()
                .filter(p -> p.id().equals(propertyId))
                .findFirst().orElse(null);
        if (pd == null) return "";
        StringBuilder sb = new StringBuilder();
        sb.append("Property: ").append(pd.id()).append("\n");
        sb.append("Type:     ").append(pd.type()).append("\n");
        if (pd.description() != null && !pd.description().isBlank()) {
            sb.append("Description: ").append(pd.description()).append("\n");
        }
        sb.append("Validator:  ").append(pd.validator()).append("\n");
        if (pd.transform() != null) sb.append("Transform:  ").append(pd.transform()).append("\n");
        if (pd.applicability() != null) {
            sb.append("Applicability: ").append(pd.applicability()).append("\n");
            String applicabilityProgram = loadResource(pd.applicability());
            if (applicabilityProgram != null) {
                sb.append("Applicability program:\n").append(applicabilityProgram.stripTrailing()).append("\n");
            }
        }
        String relation = loadRelation(pd.validator());
        if (relation != null) {
            sb.append("Relation program:\n").append(relation.stripTrailing()).append("\n");
        }
        sb.append("---\n");
        return sb.toString();
    }

    private static String loadResource(String path) {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try (InputStream in = cl.getResourceAsStream(path)) {
            if (in == null) return null;
            return new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
        }
    }

    private static String loadRelation(JsonObject validator) {
        if (validator == null || !validator.has("relation") || validator.get("relation").isJsonNull()) {
            return null;
        }
        String path = validator.get("relation").getAsString();
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try (InputStream in = cl.getResourceAsStream(path)) {
            if (in == null) return null;
            return new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
        }
    }

    @InputFiles
    public abstract ConfigurableFileCollection getSuiteFiles();

    @Classpath
    public abstract ConfigurableFileCollection getSutClasspath();

    @Input
    public abstract Property<Integer> getRunsPerProperty();

    @Input
    public abstract Property<Long> getSeed();

    @Input
    public abstract Property<Integer> getMaxShrinkSteps();

    @OutputDirectory
    public abstract DirectoryProperty getArtifactsDir();

    /**
     * JUnit XML results, consumed by CI tooling.
     */
    @OutputDirectory
    public abstract DirectoryProperty getTestResultsDir();

    /**
     * Binary results, consumed by the {@code propertyBasedTestsReport} task.
     */
    @OutputDirectory
    public abstract DirectoryProperty getBinaryResultsDir();

    @Inject
    protected abstract TestEventReporterFactory getTestEventReporterFactory();

    @TaskAction
    public void run() throws Exception {
        URL[] urls = getSutClasspath().getFiles().stream()
                .map(f -> {
                    try {
                        return f.toURI().toURL();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .toArray(URL[]::new);

        ClassLoader parent = getClass().getClassLoader();
        URLClassLoader cl = new URLClassLoader(urls, parent);
        ClassLoader previous = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(cl);
        try {
            runWithClassloader();
        } finally {
            Thread.currentThread().setContextClassLoader(previous);
            cl.close();
        }
    }

    private void runWithClassloader() {
        Registry<GeneratorProvider> generators = Registry.load(GeneratorProvider.class);
        Registry<ShrinkerProvider> shrinkers = Registry.load(ShrinkerProvider.class);
        Registry<ValidatorProvider> validators = Registry.load(ValidatorProvider.class);
        Registry<TransformProvider> transforms = Registry.load(TransformProvider.class);
        Registry<ValueTypeProvider> valueTypes = Registry.load(ValueTypeProvider.class);
        Registry<SutRunnerProvider> sutRunners = Registry.load(SutRunnerProvider.class);

        Path baseArtifactsDir = getArtifactsDir().get().getAsFile().toPath();
        List<String> failures = new ArrayList<>();

        try (GroupTestEventReporter rootReporter = getTestEventReporterFactory()
                .createTestEventReporter(
                        "property-based tests",
                        getBinaryResultsDir().get(),
                        getTestResultsDir().get())) {
            rootReporter.started(Instant.now());

            for (File suiteFile : getSuiteFiles().getFiles()) {
                SuiteDescriptor descriptor = SuiteLoader.load(suiteFile.toPath());
                for (Map.Entry<String, SutSpec> entry : descriptor.suts().entrySet()) {
                    String sutName = entry.getKey();
                    String groupName = descriptor.name() + "/" + sutName;

                    try (GroupTestEventReporter suiteReporter = rootReporter.reportTestGroup(groupName)) {
                        suiteReporter.started(Instant.now());
                        suiteReporter.output(Instant.now(),
                                TestOutputEvent.Destination.StdOut,
                                formatSutInfo(descriptor, sutName, entry.getValue(),
                                        getRunsPerProperty().get(), getSeed().get()));

                        Path sutArtifactsDir = baseArtifactsDir.resolve(descriptor.name()).resolve(sutName);
                        ValueTypeProvider valueType = valueTypes.get(descriptor.generator().kind());
                        PipelineRunner runner = new PipelineRunner(
                                generators, shrinkers, validators, transforms, valueTypes,
                                getRunsPerProperty().get(), getSeed().get(), sutArtifactsDir,
                                getMaxShrinkSteps().get());
                        SutRunner sutRunner = sutRunners.get(entry.getValue().type())
                                .build(entry.getValue());
                        SuiteResult result;
                        try {
                            result = runner.run(descriptor, sutRunner);
                        } finally {
                            sutRunner.close();
                        }

                        for (PropertyResult p : result.properties()) {
                            try (TestEventReporter propReporter =
                                         suiteReporter.reportTest(p.propertyId(), p.propertyId())) {
                                propReporter.started(Instant.now());
                                propReporter.output(Instant.now(),
                                        TestOutputEvent.Destination.StdOut,
                                        formatPropertyHeader(descriptor, p.propertyId()));
                                for (String line : p.runLog()) {
                                    propReporter.output(Instant.now(),
                                            TestOutputEvent.Destination.StdOut, line);
                                }
                                if (p.passed()) {
                                    propReporter.succeeded(Instant.now());
                                } else {
                                    String witness = p.witness().map(valueType::describe).orElse("(none)");
                                    propReporter.failed(Instant.now(), "Property violation",
                                            "generated=" + p.generated() + " checked=" + p.runs()
                                                    + " skipped=" + p.skipped() + " failures=" + p.failures()
                                                    + " witness=" + witness);
                                    failures.add(groupName + "/" + p.propertyId());
                                    getLogger().lifecycle("  {} FAILED  witness: {}", p.propertyId(), witness);
                                }
                            }
                        }

                        String status = result.passed() ? "PASSED" : "FAILED";
                        getLogger().lifecycle("[{}] {}", groupName, status);

                        if (result.passed()) {
                            suiteReporter.succeeded(Instant.now());
                        } else {
                            suiteReporter.failed(Instant.now());
                        }
                    }
                }
            }

            if (failures.isEmpty()) {
                rootReporter.succeeded(Instant.now());
            } else {
                rootReporter.failed(Instant.now());
            }
        }

        if (!failures.isEmpty()) {
            throw new GradleException("Property-based tests failed:\n  " +
                    String.join("\n  ", failures));
        }
    }
}
