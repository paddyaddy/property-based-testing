package pbt.spi.sut;

/**
 * Executes one call to a system under test.
 *
 * <p>Implementations that hold external resources (e.g. a persistent subprocess) should
 * override {@link #close()} to release them. The pipeline calls {@code close()} when it
 * is done with a runner; the default is a no-op for stateless runners.
 */
@FunctionalInterface
public interface SutRunner {

    Object run(Object input);

    default void close() {
    }
}
