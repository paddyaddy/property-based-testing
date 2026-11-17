package at.tugraz.pbt.components.transform;

import at.tugraz.pbt.spi.transform.Transform;
import at.tugraz.pbt.spi.transform.TransformProvider;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * Negates every element of a {@code List<Integer>}: {@code [1, -2, 3] -> [-1, 2, -3]}.
 */
public final class NegateTransformProvider implements TransformProvider {

    @SuppressWarnings("unchecked")
    private static Object negate(Object source, Random rnd) {
        return ((List<Integer>) source).stream()
                .map(x -> -x)
                .collect(Collectors.toList());
    }

    @Override
    public String name() {
        return "negate";
    }

    @Override
    public Transform get() {
        return NegateTransformProvider::negate;
    }
}
