package pbt.components.asp;

import pbt.spi.asp.AspEngine;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * {@link AspEngine} that shells out to the {@code clingo} command-line solver.
 * The program is fed on stdin; the query atom is matched against the solver output.
 */
public final class CliAspEngine implements AspEngine {

    private final String command;
    private final long timeoutSeconds;

    public CliAspEngine() {
        this("clingo", 10);
    }

    public CliAspEngine(String command, long timeoutSeconds) {
        this.command = command;
        this.timeoutSeconds = timeoutSeconds;
    }

    private static boolean isSatisfiable(String output) {
        return output.contains("SATISFIABLE") && !output.contains("UNSATISFIABLE");
    }

    private static boolean containsAtom(String output, String atom) {
        for (String line : output.split("\\R")) {
            for (String token : line.trim().split("\\s+")) {
                if (token.equals(atom)) return true;
            }
        }
        return false;
    }

    @Override
    public boolean isAvailable() {
        try {
            Process p = new ProcessBuilder(command, "--version")
                    .redirectErrorStream(true)
                    .start();
            boolean done = p.waitFor(5, TimeUnit.SECONDS);
            if (!done) {
                p.destroyForcibly();
                return false;
            }
            return p.exitValue() == 0;
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            return false;
        }
    }

    @Override
    public boolean entails(String program, String queryAtom) {
        String output = run(program, List.of("--quiet=1", "--verbose=0"));
        return isSatisfiable(output) && containsAtom(output, queryAtom);
    }

    @Override
    public String name() {
        return "clingo";
    }

    private String run(String program, List<String> extraArgs) {
        try {
            List<String> cmd = new ArrayList<>();
            cmd.add(command);
            cmd.addAll(extraArgs);
            Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
            try (OutputStream stdin = p.getOutputStream()) {
                stdin.write(program.getBytes(StandardCharsets.UTF_8));
            }
            String output = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            boolean done = p.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!done) {
                p.destroyForcibly();
                throw new AspException("ASP solver timed out after " + timeoutSeconds + "s");
            }
            return output;
        } catch (IOException e) {
            throw new AspException("Failed to run '" + command + "'", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AspException("Interrupted while running '" + command + "'", e);
        }
    }

    /**
     * Unchecked error raised when the solver cannot be invoked.
     */
    public static final class AspException extends RuntimeException {
        public AspException(String message) {
            super(message);
        }

        public AspException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
