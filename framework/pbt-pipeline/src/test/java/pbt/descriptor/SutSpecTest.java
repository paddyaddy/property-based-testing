package pbt.descriptor;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SutSpecTest {

    private static final Path BASE = Path.of("/project");

    @Test
    void relative_paths_are_resolved_against_the_base_directory() {
        SutSpec spec = new SutSpec("java", "a.B", "m", true, null, "scripts/x.py", null, null, null,
                List.of("builds/classes", "/abs/lib.jar"));

        SutSpec resolved = spec.resolvedAgainst(BASE);

        assertEquals("/project/scripts/x.py", resolved.file());
        assertEquals(List.of("/project/builds/classes", "/abs/lib.jar"), resolved.classpath());
        assertEquals("a.B", resolved.className());
    }

    @Test
    void absent_paths_stay_absent() {
        SutSpec resolved = new SutSpec("java", "a.B", "m", true, null, null, null, null, null).resolvedAgainst(BASE);

        assertNull(resolved.file());
        assertNull(resolved.classpath());
    }
}
