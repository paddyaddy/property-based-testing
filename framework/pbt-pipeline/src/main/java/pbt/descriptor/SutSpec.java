package pbt.descriptor;

import com.google.gson.annotations.SerializedName;

import java.nio.file.Path;
import java.util.List;

/**
 * Declarative description of one system under test, parsed from a descriptor's
 * {@code suts} map.
 *
 * <p>Fields are language specific and nullable; each SUT runner reads only the ones
 * it needs. A {@code java} SUT names a {@code class} and {@code method}; an external
 * SUT names a {@code file} and declares its {@code outputType}.
 *
 * @param type           language discriminator selecting the runner (for example {@code java} or {@code python})
 * @param className      fully qualified class name for a java SUT (JSON key {@code class})
 * @param method         method or function signature to invoke
 * @param isStatic       whether a java method is static (JSON key {@code static})
 * @param args           extra harness arguments for a python SUT
 * @param file           script path for a python SUT
 * @param timeoutSeconds per-call wall-clock budget, or null for the runner default
 * @param interpreter    interpreter executable for a python SUT (for example a venv's {@code python}); null means {@code python3}
 * @param outputType     output value-type key for a non-java SUT; null for java, whose output type is reflected
 * @param classpath      directories and jars a java SUT's classes are loaded from, in a classloader of its own
 *                       that prefers them over the project's classpath; null loads from the project's classpath
 */
public record SutSpec(
        String type,
        @SerializedName("class") String className,
        String method,
        @SerializedName("static") boolean isStatic,
        List<String> args,
        String file,
        Long timeoutSeconds,
        String interpreter,
        String outputType,
        List<String> classpath) {

    public SutSpec(
            String type,
            String className,
            String method,
            boolean isStatic,
            List<String> args,
            String file,
            Long timeoutSeconds,
            String interpreter,
            String outputType) {
        this(type, className, method, isStatic, args, file, timeoutSeconds, interpreter, outputType, null);
    }

    /**
     * Returns a copy whose relative {@code file} and {@code classpath} entries are resolved against
     * {@code baseDir}, so a descriptor means the same thing wherever the process happens to run.
     */
    public SutSpec resolvedAgainst(Path baseDir) {
        return new SutSpec(type, className, method, isStatic, args,
                file == null ? null : baseDir.resolve(file).toString(),
                timeoutSeconds, interpreter, outputType,
                classpath == null ? null : classpath.stream().map(e -> baseDir.resolve(e).toString()).toList());
    }
}
