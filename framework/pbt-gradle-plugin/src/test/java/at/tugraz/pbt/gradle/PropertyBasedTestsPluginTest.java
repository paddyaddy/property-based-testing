package at.tugraz.pbt.gradle;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class PropertyBasedTestsPluginTest {

    @TempDir
    Path projectDir;

    private static boolean clingoAvailable() {
        try {
            return new ProcessBuilder("clingo", "--version").start().waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    @BeforeEach
    void createProject() throws IOException {
        write("settings.gradle", "rootProject.name = 'consumer'\n");
        write("build.gradle", """
                plugins {
                    id 'java'
                    id 'at.tugraz.pbt'
                }
                pbt {
                    runsPerProperty = 20
                    maxShrinkSteps = 50
                }
                """);
        write("src/main/java/sut/Numbers.java", """
                package sut;
                
                public final class Numbers {
                    public static int abs(int n) { return Math.abs(n); }
                    public static int buggyAbs(int n) { return n; }
                }
                """);
    }

    private void write(String relative, String content) throws IOException {
        Path file = projectDir.resolve(relative);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
    }

    private void suite(String sutMethod) throws IOException {
        write("src/main/resources/suites/abs.json", """
                {
                  "name": "abs",
                  "suts": {
                    "under_test": { "type": "java", "class": "sut.Numbers", "method": "%s", "static": true }
                  },
                  "generator": { "kind": "int", "params": { "min": -50, "max": 50 } },
                  "properties": [
                    { "id": "always", "type": "INVARIANT", "validator": { "kind": "trivial" } }
                  ]
                }
                """.formatted(sutMethod));
    }

    private GradleRunner runner(String... args) {
        return GradleRunner.create()
                .withProjectDir(projectDir.toFile())
                .withPluginClasspath()
                .withArguments(args);
    }

    @Test
    void passing_suite_succeeds_and_writes_an_artifact() throws IOException {
        suite("abs");

        BuildResult result = runner("propertyBasedTests", "--stacktrace").build();

        assertEquals(TaskOutcome.SUCCESS, result.task(":propertyBasedTests").getOutcome());
        assertTrue(result.getOutput().contains("[abs/under_test] PASSED"), result.getOutput());
        Path artifact = projectDir.resolve("build/reports/pbt/abs/under_test/always.json");
        assertTrue(Files.exists(artifact), "artifact should be written to " + artifact);
        JsonObject json = JsonParser.parseString(Files.readString(artifact)).getAsJsonObject();
        assertTrue(json.get("passed").getAsBoolean());
        assertEquals(20, json.get("generated").getAsInt());
        assertEquals(20, json.get("runs").getAsInt());
        assertEquals(0, json.get("skipped").getAsInt());
    }

    @Test
    void suites_and_components_can_live_in_a_dedicated_source_set() throws IOException {
        write("build.gradle", """
                plugins {
                    id 'java'
                    id 'at.tugraz.pbt'
                }
                sourceSets {
                    pbt {
                        compileClasspath += sourceSets.main.output
                        runtimeClasspath += sourceSets.main.output
                    }
                }
                pbt {
                    sourceSet = 'pbt'
                    runsPerProperty = 10
                }
                """);
        write("src/pbt/resources/suites/abs.json", """
                {
                  "name": "abs",
                  "suts": { "p": { "type": "java", "class": "sut.Numbers", "method": "abs", "static": true } },
                  "generator": { "kind": "int" },
                  "properties": [ { "id": "always", "type": "INVARIANT", "validator": { "kind": "trivial" } } ]
                }
                """);

        BuildResult result = runner("propertyBasedTests").build();

        assertTrue(result.getOutput().contains("[abs/p] PASSED"), result.getOutput());
    }

    @Test
    void task_is_marked_as_a_test_task_for_intellij() throws IOException {
        write("build.gradle", """
                plugins {
                    id 'java'
                    id 'at.tugraz.pbt'
                }
                tasks.register('showFlag') {
                    doLast { println "flag=" + tasks.getByName('propertyBasedTests').extensions.extraProperties['idea.internal.test'] }
                }
                """);

        BuildResult result = runner("showFlag").build();

        assertTrue(result.getOutput().contains("flag=true"), result.getOutput());
    }

    @Test
    void unknown_source_set_is_reported() throws IOException {
        write("build.gradle", """
                plugins {
                    id 'java'
                    id 'at.tugraz.pbt'
                }
                pbt { sourceSet = 'nope' }
                """);

        BuildResult result = runner("propertyBasedTests").buildAndFail();

        assertTrue(result.getOutput().contains("pbt.sourceSet 'nope' does not exist"), result.getOutput());
    }

    @Test
    void failing_property_fails_the_build_and_reports_a_witness() throws IOException {
        assumeTrue(clingoAvailable(), "clingo not on PATH");
        write("src/main/resources/suites/abs.json", """
                {
                  "name": "abs",
                  "suts": {
                    "under_test": { "type": "java", "class": "sut.Numbers", "method": "buggyAbs", "static": true }
                  },
                  "generator": { "kind": "int", "params": { "min": -50, "max": -1 } },
                  "properties": [
                    { "id": "nonneg", "type": "INVARIANT",
                      "validator": { "kind": "asp", "relation": "asp/abs_nonneg.lp" } }
                  ]
                }
                """);
        write("src/main/resources/asp/abs_nonneg.lp", """
                violation :- out(V), V < 0.
                #show violation/0.
                """);

        BuildResult result = runner("propertyBasedTests").buildAndFail();

        assertTrue(result.getOutput().contains("[abs/under_test] FAILED"), result.getOutput());
        assertTrue(result.getOutput().contains("nonneg FAILED"), result.getOutput());
        Path artifact = projectDir.resolve("build/reports/pbt/abs/under_test/nonneg.json");
        JsonObject json = JsonParser.parseString(Files.readString(artifact)).getAsJsonObject();
        assertFalse(json.get("passed").getAsBoolean());
        assertEquals("-1", json.get("witness").getAsString());
    }

    @Test
    void python_module_paths_are_resolved_against_the_project_directory() throws IOException {
        assumeTrue(clingoAvailable(), "clingo not on PATH");
        write("py/lib/pkg/__init__.py", "");
        write("py/lib/pkg/helpers.py", "def identity(n):\n    return n\n");
        write("py/lib/pkg/core.py", """
                from .helpers import identity


                def in_project(n):
                    open("marker.txt").close()
                    return identity(n)
                """);
        write("marker.txt", "");
        write("src/main/resources/asp/pass.lp", "#show violation/0.\n");
        write("src/main/resources/suites/python_module.json", """
                {
                  "name": "python_module",
                  "suts": {
                    "under_test": { "type": "python", "module": "pkg.core", "searchPath": ["py/lib"],
                                    "method": "in_project(int)", "outputType": "int" }
                  },
                  "generator": { "kind": "int", "params": { "min": 0, "max": 5 } },
                  "properties": [
                    { "id": "no_exception", "type": "INVARIANT",
                      "validator": { "kind": "asp", "relation": "asp/pass.lp", "failOnException": true } }
                  ]
                }
                """);

        BuildResult result = runner("propertyBasedTests", "--stacktrace").build();

        assertTrue(result.getOutput().contains("[python_module/under_test] PASSED"), result.getOutput());
    }
}
