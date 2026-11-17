package at.tugraz.pbt.components.transform;

import at.tugraz.pbt.spi.transform.Transform;
import at.tugraz.pbt.spi.transform.TransformProvider;

/**
 * Reverses a {@link String}: {@code "abc" -> "cba"}.
 */
public final class StringReverseTransformProvider implements TransformProvider {

    @Override
    public String name() {
        return "stringReverse";
    }

    @Override
    public Transform get() {
        return (source, rnd) -> new StringBuilder((String) source).reverse().toString();
    }
}
