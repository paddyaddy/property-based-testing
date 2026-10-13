package pbt.components.transform;

import pbt.spi.transform.Transform;
import pbt.spi.transform.TransformProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Produces a random permutation of a {@code List<Integer>} source input.
 */
public final class ShuffleTransformProvider implements TransformProvider {

    private static final Transform INSTANCE = (source, rnd) -> {
        @SuppressWarnings("unchecked")
        List<Integer> copy = new ArrayList<>((List<Integer>) source);
        Collections.shuffle(copy, rnd);
        return copy;
    };

    @Override
    public String name() {
        return "shuffle";
    }

    @Override
    public Transform get() {
        return INSTANCE;
    }
}
