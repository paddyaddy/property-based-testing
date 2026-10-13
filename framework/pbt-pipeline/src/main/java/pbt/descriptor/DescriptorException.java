package pbt.descriptor;

/**
 * Raised when a suite descriptor is missing, malformed, or fails validation.
 */
public final class DescriptorException extends RuntimeException {

    public DescriptorException(String message) {
        super(message);
    }

    public DescriptorException(String message, Throwable cause) {
        super(message, cause);
    }
}
