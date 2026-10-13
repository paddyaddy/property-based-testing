package pbt.pipeline.dto;

import java.util.List;
import java.util.Optional;

/**
 * Outcome of running one property: number of inputs generated, how many were checked
 * ({@code runs}), how many were skipped by the applicability filter, how many failed,
 * the minimized witness, and a per-run log.
 *
 * <p>{@code runs + skipped} equals {@code generated}. {@code failures} counts failing
 * checked inputs. {@code runLog} contains one entry per run plus shrinking steps on failure.
 */
public record PropertyResult(
        String propertyId,
        boolean passed,
        int generated,
        int runs,
        int skipped,
        int failures,
        Optional<Object> witness,
        List<String> runLog) {
}
