package pbt.components.transform;

import pbt.spi.transform.Transform;
import pbt.spi.transform.TransformProvider;

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
