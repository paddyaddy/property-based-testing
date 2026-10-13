package pbt.spi.shrinker;

import java.util.stream.Stream;

/**
 * Proposes smaller candidates during witness minimization.
 */
@FunctionalInterface
public interface Shrinker<T> {

    /**
     * Returns a stream of candidates smaller than {@code value}.
     * The stream must not contain {@code value} itself.
     */
    Stream<T> shrink(T value);
}
