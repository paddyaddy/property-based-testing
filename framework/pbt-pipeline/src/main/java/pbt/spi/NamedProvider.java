package pbt.spi;

/**
 * Base contract for every pluggable component discovered through
 * {@link java.util.ServiceLoader}.
 *
 * <p>{@link #name()} is the short key a descriptor uses to select the component. A
 * {@link Registry} indexes providers by this name and also accepts a fully
 * qualified class name as a fallback, so a user component can be selected either by
 * registering a name or by naming its class directly.
 */
public interface NamedProvider {

    /**
     * The short, descriptor-facing key for this component.
     */
    String name();
}
