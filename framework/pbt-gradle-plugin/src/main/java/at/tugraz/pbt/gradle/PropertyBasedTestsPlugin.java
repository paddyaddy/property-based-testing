package at.tugraz.pbt.gradle;

import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.testing.TestReport;
import at.tugraz.pbt.pipeline.PipelineRunner;

/**
 * Registers the {@code propertyBasedTests} task and wires it to the {@code check} lifecycle.
 */
public class PropertyBasedTestsPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        PbtExtension ext = project.getExtensions().create("pbt", PbtExtension.class);
        ext.getSourceSet().convention("main");
        ext.getSuiteDir().convention(ext.getSourceSet().map(name ->
                project.getLayout().getProjectDirectory().dir("src/" + name + "/resources/suites")));
        ext.getRunsPerProperty().convention(100);
        ext.getSeed().convention(42L);
        ext.getMaxShrinkSteps().convention(PipelineRunner.DEFAULT_MAX_SHRINK_STEPS);

        var pbTask = project.getTasks().register("propertyBasedTests",
                PropertyBasedTestsTask.class, task -> {
                    task.setGroup("verification");
                    // IntelliJ only shows a test tree for tasks it recognizes as test tasks, which for
                    // a task that is not a Gradle Test task means this extra property.
                    task.getExtensions().getExtraProperties().set("idea.internal.test", true);
                    task.setDescription("Runs data-driven property-based tests declared in descriptors.");

                    task.getSuiteFiles().from(ext.getSuiteDir().map(dir ->
                            project.fileTree(dir, t -> t.include("**/*.json"))));

                    SourceSetContainer sourceSets = project.getExtensions()
                            .getByType(SourceSetContainer.class);
                    Provider<SourceSet> sourceSet = ext.getSourceSet().map(name -> {
                        SourceSet found = sourceSets.findByName(name);
                        if (found == null) {
                            throw new GradleException("pbt.sourceSet '" + name + "' does not exist");
                        }
                        return found;
                    });
                    task.getSutClasspath().from(sourceSet.map(SourceSet::getOutput));
                    task.getSutClasspath().from(sourceSet.map(SourceSet::getRuntimeClasspath));
                    task.dependsOn(sourceSet.map(SourceSet::getOutput));

                    task.getBaseDir().set(project.getLayout().getProjectDirectory());
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
