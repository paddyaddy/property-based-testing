package pbt.spi;

/**
 * Raised when a component key cannot be resolved to a provider.
 */
public final class ProviderException extends RuntimeException {

    public ProviderException(String message) {
        super(message);
    }

    public ProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}
