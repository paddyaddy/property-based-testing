package at.tugraz.pbt.components.sut;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import at.tugraz.pbt.descriptor.SutSpec;
import at.tugraz.pbt.spi.Registry;
import at.tugraz.pbt.spi.sut.SutException;
import at.tugraz.pbt.spi.sut.SutRunner;
import at.tugraz.pbt.spi.sut.SutRunnerProvider;
import at.tugraz.pbt.spi.type.Tuple;
import at.tugraz.pbt.spi.type.ValueTypeProvider;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Invokes a Python SUT via a persistent worker process using the bundled harness.
 *
 * <p>The SUT is a {@code file} loaded as a module, or a {@code module} imported by its dotted
 * name, so that a package with relative imports works. {@code searchPath} entries are put in
 * front of {@code sys.path} before loading. The worker runs in {@code workingDir}.
 */
public final class PythonSutRunnerProvider implements SutRunnerProvider {

    private static final long DEFAULT_TIMEOUT_SECONDS = 10;

    private static volatile Path harnessPath;

    private final Registry<ValueTypeProvider> valueTypes;
    private final Gson gson;

    public PythonSutRunnerProvider() {
        this.valueTypes = Registry.load(ValueTypeProvider.class);
        this.gson = new Gson();
    }

    private static List<String> buildCommand(SutSpec spec, Path harness) {
        List<String> cmd = new ArrayList<>();
        cmd.add(spec.interpreter() != null && !spec.interpreter().isBlank() ? spec.interpreter() : "python3");
        cmd.add(harness.toString());
        if (spec.module() != null) {
            cmd.add("--module");
            cmd.add(spec.module());
        } else {
            cmd.add("--file");
            cmd.add(spec.file());
        }
        if (spec.searchPath() != null) {
            for (String entry : spec.searchPath()) {
                cmd.add("--path");
                cmd.add(entry);
            }
        }
        if (spec.method() != null && !spec.method().isBlank()) {
            cmd.add("--target");
            cmd.add(spec.method());
        }
        if (spec.isStatic()) {
            cmd.add("--static");
        }
        cmd.add("--server");
        if (spec.args() != null) {
            cmd.addAll(spec.args());
        }
        return cmd;
    }

    private static Path extractHarness() {
        if (harnessPath != null) return harnessPath;
        return doExtractHarness();
    }

    private static synchronized Path doExtractHarness() {
        if (harnessPath != null) return harnessPath;
        try (InputStream in = PythonSutRunnerProvider.class.getClassLoader()
                .getResourceAsStream("python/sut_harness.py")) {
            if (in == null) {
                throw new RuntimeException("python/sut_harness.py not found on classpath");
            }
            Path tmp = Files.createTempFile("sut_harness_", ".py");
            Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
            tmp.toFile().deleteOnExit();
            harnessPath = tmp;
            return tmp;
        } catch (IOException e) {
            throw new UncheckedIOException("failed to extract Python harness", e);
        }
    }

    @Override
    public String name() {
        return "python";
    }

    @Override
    public SutRunner build(SutSpec spec) {
        boolean hasFile = spec.file() != null && !spec.file().isBlank();
        boolean hasModule = spec.module() != null && !spec.module().isBlank();
        if (hasFile == hasModule) {
            throw new IllegalArgumentException("python SUT spec requires either a 'file' or a 'module'");
        }
        if (spec.outputType() == null || spec.outputType().isBlank()) {
            throw new IllegalArgumentException("python SUT spec requires an 'outputType'");
        }
        Type outputType = valueTypes.get(spec.outputType()).javaType();
        Path harness = extractHarness();
        List<String> command = buildCommand(spec, harness);
        long timeout = spec.timeoutSeconds() != null ? spec.timeoutSeconds() : DEFAULT_TIMEOUT_SECONDS;
        File workingDir = new File(spec.workingDir() != null ? spec.workingDir() : System.getProperty("user.dir"));
        return new PersistentWorker(command, workingDir, outputType, timeout, gson);
    }

    private static final class PersistentWorker implements SutRunner {

        private final List<String> command;
        private final File workingDir;
        private final Type outputType;
        private final long timeoutSeconds;
        private final Gson gson;
        private final ExecutorService io = Executors.newSingleThreadExecutor(task -> {
            Thread t = new Thread(task, "python-sut-io");
            t.setDaemon(true);
            return t;
        });

        private Process process;
        private BufferedWriter writer;
        private BufferedReader reader;
        private ByteArrayOutputStream stderrBuf;
        private Thread stderrDrainer;

        PersistentWorker(List<String> command, File workingDir, Type outputType, long timeoutSeconds, Gson gson) {
            this.command = command;
            this.workingDir = workingDir;
            this.outputType = outputType;
            this.timeoutSeconds = timeoutSeconds;
            this.gson = gson;
            start();
        }

        private void start() {
            try {
                ProcessBuilder pb = new ProcessBuilder(command);
                pb.directory(workingDir);
                this.process = pb.start();
            } catch (IOException e) {
                throw new UncheckedIOException("failed to start Python SUT worker", e);
            }
            this.writer = new BufferedWriter(
                    new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));
            this.reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            this.stderrBuf = buf;
            Process started = process;
            this.stderrDrainer = new Thread(() -> {
                try {
                    started.getErrorStream().transferTo(buf);
                } catch (IOException ignored) {
                }
            });
            stderrDrainer.setDaemon(true);
            stderrDrainer.start();
        }

        @Override
        public synchronized Object run(Object input) {
            if (process == null) {
                start();
            }
            String requestJson = input instanceof Tuple tuple
                    ? gson.toJson(Map.of("args", tuple.values()))
                    : gson.toJson(Map.of("input", input));
            BufferedWriter out = writer;
            BufferedReader in = reader;
            Future<String> response = io.submit(() -> {
                out.write(requestJson);
                out.newLine();
                out.flush();
                return in.readLine();
            });
            String responseJson;
            try {
                responseJson = response.get(timeoutSeconds, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                String stderr = stderr();
                kill();
                throw new SutException("Timeout", "Python SUT call timed out after " + timeoutSeconds
                        + "s; the worker was killed and restarts on the next call"
                        + (stderr.isEmpty() ? "" : "; stderr: " + stderr));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                kill();
                throw new RuntimeException("interrupted while waiting for the Python SUT worker", e);
            } catch (ExecutionException e) {
                String stderr = stderrOfExitedWorker();
                kill();
                throw new UncheckedIOException("I/O error communicating with Python SUT worker"
                        + (stderr.isEmpty() ? "" : "; stderr: " + stderr),
                        e.getCause() instanceof IOException io ? io : new IOException(e.getCause()));
            }
            if (responseJson == null) {
                String stderr = stderrOfExitedWorker();
                kill();
                throw new RuntimeException("Python SUT worker closed unexpectedly; stderr: " + stderr);
            }
            JsonObject json = gson.fromJson(responseJson, JsonObject.class);
            if (json.has("error")) {
                String type = json.has("type") ? json.get("type").getAsString() : "Exception";
                throw new SutException(type, "Python SUT error: " + json.get("error").getAsString());
            }
            return gson.fromJson(json.get("output"), outputType);
        }

        private String stderr() {
            return stderrBuf.toString(StandardCharsets.UTF_8).strip();
        }

        /**
         * Stderr of a worker that closed its streams, once it has exited and its stderr has been read
         * to the end, so a startup failure such as an import error is reported in full.
         */
        private String stderrOfExitedWorker() {
            try {
                process.waitFor(1, TimeUnit.SECONDS);
                stderrDrainer.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return stderr();
        }

        /**
         * Destroys the current worker; the next call starts a fresh one.
         */
        private void kill() {
            if (process != null) {
                process.destroyForcibly();
                process = null;
            }
        }

        @Override
        public synchronized void close() {
            io.shutdownNow();
            if (process == null) return;
            try {
                writer.close();
            } catch (IOException ignored) {
            }
            try {
                if (!process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
                    process.destroyForcibly();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                process.destroyForcibly();
            }
            process = null;
        }
    }
}
