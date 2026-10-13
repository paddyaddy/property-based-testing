package pbt.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.testing.TestReport;
import pbt.pipeline.PipelineRunner;

/**
 * Registers the {@code propertyBasedTests} task and wires it to the {@code check} lifecycle.
 */
public class PropertyBasedTestsPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        PbtExtension ext = project.getExtensions().create("pbt", PbtExtension.class);
        ext.getSuiteDir().convention(
                project.getLayout().getProjectDirectory().dir("src/main/resources/suites"));
        ext.getRunsPerProperty().convention(100);
        ext.getSeed().convention(42L);
        ext.getMaxShrinkSteps().convention(PipelineRunner.DEFAULT_MAX_SHRINK_STEPS);

        var pbTask = project.getTasks().register("propertyBasedTests",
                PropertyBasedTestsTask.class, task -> {
                    task.setGroup("verification");
                    task.setDescription("Runs data-driven property-based tests declared in descriptors.");

                    task.getSuiteFiles().from(ext.getSuiteDir().map(dir ->
                            project.fileTree(dir, t -> t.include("**/*.json"))));

                    SourceSetContainer sourceSets = project.getExtensions()
                            .getByType(SourceSetContainer.class);
                    SourceSet main = sourceSets.getByName("main");
                    task.getSutClasspath().from(main.getOutput());
                    task.getSutClasspath().from(project.getConfigurations().named("runtimeClasspath"));
                    task.dependsOn(
                            project.getTasks().named("compileJava"),
                            project.getTasks().named("processResources"));

                    task.getRunsPerProperty().set(ext.getRunsPerProperty());
                    task.getSeed().set(ext.getSeed());
                    task.getMaxShrinkSteps().set(ext.getMaxShrinkSteps());
                    task.getOutputs().upToDateWhen(t -> false);
                    task.getArtifactsDir().convention(
                            project.getLayout().getBuildDirectory().dir("reports/pbt"));
                    task.getTestResultsDir().convention(
                            project.getLayout().getBuildDirectory().dir("test-results/propertyBasedTests"));
                    task.getBinaryResultsDir().convention(
                            project.getLayout().getBuildDirectory().dir("test-results-binary/propertyBasedTests"));
                });

        // HTML report aggregating the binary results
        project.getTasks().register("propertyBasedTestsReport", TestReport.class, report -> {
            report.setGroup("reporting");
            report.setDescription("Generates an HTML report for propertyBasedTests results.");
            report.getDestinationDirectory().convention(
                    project.getLayout().getBuildDirectory().dir("reports/tests/propertyBasedTests"));
            report.getTestResults().from(pbTask.map(t -> t.getBinaryResultsDir()));
        });

        pbTask.configure(t -> t.finalizedBy("propertyBasedTestsReport"));

        project.getTasks().named("test").configure(t -> t.dependsOn(pbTask));
    }
}
