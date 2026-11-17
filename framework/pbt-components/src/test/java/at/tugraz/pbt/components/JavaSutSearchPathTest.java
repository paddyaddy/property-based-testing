package at.tugraz.pbt.components;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import at.tugraz.pbt.components.sut.JavaSutRunnerProvider;
import at.tugraz.pbt.descriptor.SutSpec;
import at.tugraz.pbt.spi.sut.SutRunner;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JavaSutSearchPathTest {

    private static final String CLASS_NAME = "at.tugraz.pbt.components.fixture.Versioned";

    private static Path compileVersion(Path dir, String label) throws IOException {
        Path source = dir.resolve("src/pbt/at/tugraz/pbt/components/fixture/Versioned.java");
        Files.createDirectories(source.getParent());
        Files.writeString(source, """
                package at.tugraz.pbt.components.fixture;
                public final class Versioned {
                    public static String version(String input) { return "%s:" + input; }
                }
                """.formatted(label));
        Path classes = Files.createDirectories(dir.resolve("classes"));
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertEquals(0, compiler.run(null, null, null, "-d", classes.toString(), source.toString()));
        return classes;
    }

    private static SutRunner runner(List<String> searchPath) {
        return new JavaSutRunnerProvider().build(new SutSpec("java", CLASS_NAME, "version(String)", true,
                null, null, null, null, null).withSearchPath(searchPath));
    }

    @Test
    void two_versions_of_the_same_class_run_side_by_side(@TempDir Path tmp) throws IOException {
        Path v1 = compileVersion(Files.createDirectories(tmp.resolve("v1")), "v1");
        Path v2 = compileVersion(Files.createDirectories(tmp.resolve("v2")), "v2");
        SutRunner first = runner(List.of(v1.toString()));
        SutRunner second = runner(List.of(v2.toString()));
        try {
            assertEquals("v1:x", first.run("x"));
            assertEquals("v2:x", second.run("x"));
        } finally {
            first.close();
            second.close();
        }
    }

    @Test
    void search_path_wins_over_the_project_classpath(@TempDir Path tmp) throws IOException {
        SutRunner isolated = runner(List.of(compileVersion(tmp, "other").toString()));
        try {
            assertEquals("other:x", isolated.run("x"));
            assertEquals("project", runner(null).run("x"));
        } finally {
            isolated.close();
        }
    }

    @Test
    void missing_search_path_entry_is_reported() {
        var e = assertThrows(IllegalArgumentException.class, () -> runner(List.of("/does/not/exist")));
        assertTrue(e.getMessage().contains("/does/not/exist"), e.getMessage());
    }

    @Test
    void class_not_on_the_search_path_is_reported(@TempDir Path tmp) {
        var spec = new SutSpec("java", "at.tugraz.pbt.components.fixture.Missing", "version(String)", true,
                null, null, null, null, null).withSearchPath(List.of(tmp.toString()));

        var e = assertThrows(IllegalArgumentException.class, () -> new JavaSutRunnerProvider().build(spec));
        assertTrue(e.getMessage().contains("not found"), e.getMessage());
    }
}
