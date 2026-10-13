package pbt.spi.transform;

import java.util.Random;

/**
 * Derives a metamorphic follow-up input from a source input.
 */
@FunctionalInterface
public interface Transform {

    /**
     * Produces the follow-up input from {@code source}.
     * {@code rnd} is seeded per-input so witnesses stay reproducible.
     */
    Object apply(Object source, Random rnd);
}
