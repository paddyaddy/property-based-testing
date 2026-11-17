package at.tugraz.pbt.descriptor;

import com.google.gson.annotations.SerializedName;

import java.nio.file.Path;
import java.util.List;

/**
 * Declarative description of one system under test, parsed from a descriptor's
 * {@code suts} map.
 *
 * <p>Fields are language specific and nullable; each SUT runner reads only the ones
 * it needs. A {@code java} SUT names a {@code class} and {@code method}; a {@code python} SUT
 * names a {@code file} or a {@code module} and declares its {@code outputType}.
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
 * @param searchPath     where the SUT's code is loaded from, read by each runner in its own way: the directories
 *                       and jars of a classloader of its own for java, entries put in front of {@code sys.path}
 *                       for python; null loads from the runner's default location
 * @param module         dotted module name a python SUT is imported from, instead of a {@code file}
 * @param workingDir     working directory of a SUT that runs in a process of its own; null means the project directory
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
        List<String> searchPath,
        String module,
        String workingDir) {

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
        this(type, className, method, isStatic, args, file, timeoutSeconds, interpreter, outputType,
                null, null, null);
    }

    public SutSpec withSearchPath(List<String> searchPath) {
        return new SutSpec(type, className, method, isStatic, args, file, timeoutSeconds, interpreter, outputType,
                searchPath, module, workingDir);
    }

    public SutSpec withModule(String module) {
        return new SutSpec(type, className, method, isStatic, args, file, timeoutSeconds, interpreter, outputType,
                searchPath, module, workingDir);
    }

    public SutSpec withWorkingDir(String workingDir) {
        return new SutSpec(type, className, method, isStatic, args, file, timeoutSeconds, interpreter, outputType,
                searchPath, module, workingDir);
    }

    /**
     * Returns a copy whose relative paths are resolved against {@code baseDir}, so a descriptor
     * means the same thing wherever the process happens to run: {@code file}, the
     * {@code searchPath} entries, {@code workingDir} (which defaults to {@code baseDir}) and an
     * {@code interpreter} given as a path. An interpreter without a directory part, such as
     * {@code python3}, stays a command looked up on the {@code PATH}.
     */
    public SutSpec resolvedAgainst(Path baseDir) {
        return new SutSpec(type, className, method, isStatic, args,
                file == null ? null : baseDir.resolve(file).toString(),
                timeoutSeconds,
                interpreter == null || !interpreter.contains("/") ? interpreter : baseDir.resolve(interpreter).toString(),
                outputType,
                searchPath == null ? null : searchPath.stream().map(e -> baseDir.resolve(e).toString()).toList(),
                module,
                workingDir == null ? baseDir.toString() : baseDir.resolve(workingDir).toString());
    }
}
