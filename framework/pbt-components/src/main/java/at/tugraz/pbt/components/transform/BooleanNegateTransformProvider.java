package at.tugraz.pbt.components.transform;

import at.tugraz.pbt.spi.transform.Transform;
import at.tugraz.pbt.spi.transform.TransformProvider;

/**
 * Negates a {@link Boolean}: {@code true -> false}, {@code false -> true}.
 */
public final class BooleanNegateTransformProvider implements TransformProvider {

    @Override
    public String name() {
        return "booleanNegate";
    }

    @Override
    public Transform get() {
        return (source, rnd) -> !((Boolean) source);
    }
}
