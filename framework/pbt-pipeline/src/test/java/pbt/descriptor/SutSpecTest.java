package pbt.descriptor;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SutSpecTest {

    private static final Path BASE = Path.of("/project");

    @Test
    void relative_paths_are_resolved_against_the_base_directory() {
        SutSpec spec = new SutSpec("python", null, "m", false, null, "scripts/x.py", null, ".venv/bin/python", "int")
                .withSearchPath(List.of("builds/classes", "/abs/lib.jar"))
                .withWorkingDir("work");

        SutSpec resolved = spec.resolvedAgainst(BASE);

        assertEquals("/project/scripts/x.py", resolved.file());
        assertEquals(List.of("/project/builds/classes", "/abs/lib.jar"), resolved.searchPath());
        assertEquals("/project/.venv/bin/python", resolved.interpreter());
        assertEquals("/project/work", resolved.workingDir());
        assertEquals("int", resolved.outputType());
    }

    @Test
    void an_interpreter_without_a_directory_stays_a_command() {
        SutSpec resolved = new SutSpec("python", null, "m", false, null, "x.py", null, "python3", "int")
                .resolvedAgainst(BASE);

        assertEquals("python3", resolved.interpreter());
    }

    @Test
    void absent_paths_stay_absent() {
        SutSpec resolved = new SutSpec("java", "a.B", "m", true, null, null, null, null, null).resolvedAgainst(BASE);

        assertNull(resolved.file());
        assertNull(resolved.searchPath());
        assertNull(resolved.interpreter());
        assertEquals("/project", resolved.workingDir(), "the working directory defaults to the base directory");
    }
}
