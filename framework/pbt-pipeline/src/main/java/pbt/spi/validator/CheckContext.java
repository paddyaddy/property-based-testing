package pbt.spi.validator;

/**
 * All execution data a validator needs to evaluate one property check.
 *
 * <p>For invariant properties {@code followUpInput} and {@code followUpOutput} are
 * {@code null}. For metamorphic properties both are set. {@code sourceOutput} and
 * {@code followUpOutput} are {@code null} until SUT execution is wired in (step 5);
 * validators that do not yet need SUT output (e.g. {@code trivial}) ignore them.
 */
public record CheckContext(
        Object sourceInput,
        Object sourceOutput,
        Object followUpInput,
        Object followUpOutput) {

    /**
     * Convenience factory for invariant checks before SUT execution.
     */
    public static CheckContext invariant(Object input) {
        return new CheckContext(input, null, null, null);
    }

    /**
     * Convenience factory for metamorphic checks before SUT execution.
     */
    public static CheckContext metamorphic(Object sourceInput, Object followUpInput) {
        return new CheckContext(sourceInput, null, followUpInput, null);
    }
}
