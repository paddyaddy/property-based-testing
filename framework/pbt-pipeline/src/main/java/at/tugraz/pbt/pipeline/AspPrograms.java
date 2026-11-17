package at.tugraz.pbt.pipeline;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Loads ASP programs from the classpath and resolves their {@code #include} directives.
 *
 * <p>clingo resolves {@code #include} against the working directory, which does not work for
 * programs that live on the classpath (possibly inside a jar) and are piped to the solver. A
 * directive of the form {@code #include "grammar.lp".} on a line of its own is
 * therefore replaced by the content of that classpath resource, resolved recursively. As with
 * clingo, the path is relative to the directory of the including file ({@code ../} is allowed).
 * The path of the top-level program is relative to the classpath root, like the {@code relation}
 * and {@code applicability} paths of a descriptor. A resource that is included more than once is
 * inserted only the first time.
 */
public final class AspPrograms {

    private static final Pattern INCLUDE =
            Pattern.compile("^\\s*#include\\s+\"([^\"]+)\"\\s*\\.\\s*(%.*)?$");

    private AspPrograms() {
    }

    /**
     * Returns the program at {@code path} with all includes expanded.
     *
     * @throws IllegalArgumentException if the program or one of its includes is not on the classpath
     */
    public static String load(String path) {
        StringBuilder out = new StringBuilder();
        append(path, null, new HashSet<>(), out);
        return out.toString();
    }

    private static void append(String path, String includedFrom, Set<String> seen, StringBuilder out) {
        if (!seen.add(path)) {
            return;
        }
        String text = read(path);
        if (text == null) {
            throw new IllegalArgumentException("ASP program not found: " + path
                    + (includedFrom == null ? "" : " (included from " + includedFrom + ")"));
        }
        for (String line : text.split("\\R", -1)) {
            Matcher include = INCLUDE.matcher(line);
            if (include.matches()) {
                append(resolve(path, include.group(1)), path, seen, out);
            } else {
                out.append(line).append('\n');
            }
        }
    }

    private static String resolve(String includingPath, String include) {
        Path dir = Path.of(includingPath).getParent();
        Path resolved = (dir == null ? Path.of(include) : dir.resolve(include)).normalize();
        return resolved.toString().replace('\\', '/');
    }

    private static String read(String path) {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try (InputStream in = cl.getResourceAsStream(path)) {
            return in == null ? null : new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("cannot read ASP program: " + path, e);
        }
    }
}
