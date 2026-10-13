package pbt.spi.generator;

import java.util.Random;

/**
 * Produces test inputs of type {@code T}.
 */
@FunctionalInterface
public interface Generator<T> {

    T generate(Random rnd);

    default String describe(T value) {
        return String.valueOf(value);
    }
}
