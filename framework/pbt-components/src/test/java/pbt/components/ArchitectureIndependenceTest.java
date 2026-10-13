package pbt.components;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The framework must never depend on the demo build that consumes it.
 */
class ArchitectureIndependenceTest {

    private static final Path FRAMEWORK_ROOT = Path.of("..").toAbsolutePath().normalize();

    @Test
    void demo_classes_are_not_on_the_framework_classpath() {
        assertThrows(ClassNotFoundException.class, () -> Class.forName("demo.math.LongMath"));
        assertThrows(ClassNotFoundException.class, () -> Class.forName("demo.sort.Sorter"));
    }

    @Test
    void framework_build_scripts_do_not_reference_the_demo() throws IOException {
        assertTrue(Files.exists(FRAMEWORK_ROOT.resolve("settings.gradle")),
                "expected the framework root at " + FRAMEWORK_ROOT);
        List<Path> scripts;
        try (Stream<Path> files = Files.walk(FRAMEWORK_ROOT)) {
            scripts = files
                    .filter(p -> !p.toString().contains("/build/") && !p.toString().contains("/.gradle/"))
                    .filter(p -> p.getFileName().toString().matches("(settings|build)\\.gradle(\\.kts)?"))
                    .toList();
        }
        assertFalse(scripts.isEmpty());
        for (Path script : scripts) {
            String content = Files.readString(script);
            assertFalse(content.matches("(?s).*['\"/:]demo['\"/:].*"),
                    script + " must not reference the demo project");
        }
    }

    @Test
    void framework_sources_do_not_import_demo_packages() throws IOException {
        List<Path> sources;
        try (Stream<Path> files = Files.walk(FRAMEWORK_ROOT)) {
            sources = files
                    .filter(p -> p.toString().endsWith(".java"))
                    .filter(p -> p.toString().contains("/src/main/"))
                    .toList();
        }
        assertFalse(sources.isEmpty());
        for (Path source : sources) {
            for (String line : Files.readAllLines(source)) {
                String trimmed = line.trim();
                assertFalse(trimmed.startsWith("import demo.") || trimmed.startsWith("package demo"),
                        source + " must not use demo packages");
            }
        }
    }
}
