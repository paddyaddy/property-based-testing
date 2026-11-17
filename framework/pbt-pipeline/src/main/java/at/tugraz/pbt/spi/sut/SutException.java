package at.tugraz.pbt.spi.sut;

/**
 * Thrown by a {@link SutRunner} when the system under test itself failed: it raised an
 * exception or error, or exceeded its time budget. The pipeline records {@link #exceptionType()}
 * as the outcome of that execution and hands it to the validator. Any other exception from a
 * runner (a wiring problem, a dead worker process) is not a SUT outcome and aborts the run.
 */
public final class SutException extends RuntimeException {

    private final String exceptionType;

    public SutException(String exceptionType, String message) {
        this(exceptionType, message, null);
    }

    /**
     * @param exceptionType name of the exception the SUT raised, for example
     *                      {@code java.lang.NullPointerException}
     */
    public SutException(String exceptionType, String message, Throwable cause) {
        super(message, cause);
        this.exceptionType = exceptionType;
    }

    public String exceptionType() {
        return exceptionType;
    }
}
