package pbt.components;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pbt.components.sut.PythonSutRunnerProvider;
import pbt.descriptor.SutSpec;
import pbt.spi.sut.SutException;
import pbt.spi.sut.SutRunner;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Python SUTs imported by module name from a search path, the counterpart of
 * {@link JavaSutSearchPathTest}. Each version under {@code python/versions} is a package whose
 * {@code core} module uses a relative import.
 */
class PythonModuleSutTest {

    private static final String VERSIONS = "src/test/resources/python/versions/";

    private static SutSpec spec(String method, String outputType) {
        return new SutSpec("python", null, method, false, null, null, null, null, outputType)
                .withModule("versioned.core");
    }

    private static SutRunner runner(SutSpec spec) {
        return new PythonSutRunnerProvider().build(spec);
    }

    private static SutRunner version(String version, String method) {
        return runner(spec(method, "string").withSearchPath(List.of(VERSIONS + version)));
    }

    @BeforeEach
    void requirePython() {
        try {
            assumeTrue(new ProcessBuilder("python3", "--version").start().waitFor() == 0, "python3 not on PATH");
        } catch (IOException | InterruptedException e) {
            assumeTrue(false, "python3 not on PATH");
        }
    }

    @Test
    void a_module_with_relative_imports_is_imported_from_the_search_path() {
        SutRunner runner = version("v1", "version(str)");
        try {
            assertEquals("v1:x", runner.run("x"));
        } finally {
            runner.close();
        }
    }

    @Test
    void two_versions_of_the_same_module_run_side_by_side() {
        SutRunner first = version("v1", "version(str)");
        SutRunner second = version("v2", "version(str)");
        try {
            assertEquals("v1:x", first.run("x"));
            assertEquals("v2:x", second.run("x"));
        } finally {
            first.close();
            second.close();
        }
    }

    @Test
    void an_exception_type_is_qualified_by_its_module() {
        SutRunner runner = version("v1", "reject(str)");
        try {
            SutException e = assertThrows(SutException.class, () -> runner.run("x"));
            assertEquals("versioned.core.VersionError", e.exceptionType());
        } finally {
            runner.close();
        }
    }

    @Test
    void a_module_that_cannot_be_imported_aborts_with_the_import_error() {
        SutRunner runner = runner(spec("version(str)", "string").withModule("no_such_package.core"));
        try {
            RuntimeException e = assertThrows(RuntimeException.class, () -> runner.run("x"));
            assertFalse(e instanceof SutException, "an import error is not an outcome of the SUT");
            assertTrue(e.getMessage().contains("ModuleNotFoundError"), e.getMessage());
            assertTrue(e.getMessage().contains("no_such_package"), e.getMessage());
        } finally {
            runner.close();
        }
    }

    @Test
    void a_spec_needs_exactly_one_of_file_and_module() {
        SutSpec neither = new SutSpec("python", null, "version(str)", false, null, null, null, null, "string");
        SutSpec both = new SutSpec("python", null, "version(str)", false, null, "x.py", null, null, "string")
                .withModule("versioned.core");

        assertThrows(IllegalArgumentException.class, () -> runner(neither));
        assertThrows(IllegalArgumentException.class, () -> runner(both));
    }

    @Test
    void the_worker_runs_in_the_working_directory(@TempDir Path tmp) throws IOException {
        Path versions = Path.of(VERSIONS).toAbsolutePath();
        SutRunner runner = runner(spec("working_directory(str)", "string")
                .withSearchPath(List.of(versions.resolve("v1").toString()))
                .withWorkingDir(tmp.toString()));
        try {
            assertEquals(tmp.toRealPath().toString(), runner.run("x"));
        } finally {
            runner.close();
        }
    }
}
