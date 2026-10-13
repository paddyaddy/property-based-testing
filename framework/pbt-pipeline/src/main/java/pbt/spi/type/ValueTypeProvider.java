package pbt.spi.type;

import pbt.spi.NamedProvider;

import java.lang.reflect.Type;

/**
 * Per-type cross-cutting concern: provides the Java reflection type (for
 * serialization) and a human-readable description of a value.
 *
 * <p>The short name matches the generator/shrinker kind for the same type (for
 * example {@code "int"} or {@code "intList"}).
 */
public interface ValueTypeProvider extends NamedProvider {

    /**
     * The Java {@link Type} used when serializing artifacts of this type.
     */
    Type javaType();

    /**
     * Human-readable rendering of a value for reports and witnesses.
     */
    String describe(Object value);
}
