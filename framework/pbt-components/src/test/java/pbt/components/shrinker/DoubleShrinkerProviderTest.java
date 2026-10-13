package pbt.components.shrinker;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;
import pbt.spi.shrinker.Shrinker;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DoubleShrinkerProviderTest {

    @SuppressWarnings("unchecked")
    private Shrinker<Double> build() {
        return (Shrinker<Double>) new DoubleShrinkerProvider().build(new JsonObject());
    }

    @Test
    void offers_zero_for_positive_value() {
        List<Double> candidates = build().shrink(4.0).collect(Collectors.toList());
        assertTrue(candidates.contains(0.0), "zero not offered");
    }

    @Test
    void all_candidates_are_closer_to_zero() {
        List<Double> candidates = build().shrink(8.0).collect(Collectors.toList());
        assertTrue(candidates.stream().allMatch(v -> Math.abs(v) < 8.0),
                "candidate not closer to zero than 8.0");
    }

    @Test
    void zero_has_no_candidates() {
        assertTrue(build().shrink(0.0).collect(Collectors.toList()).isEmpty());
    }

    @Test
    void shrinks_negative_values() {
        List<Double> candidates = build().shrink(-6.0).collect(Collectors.toList());
        assertTrue(candidates.contains(0.0));
        assertTrue(candidates.stream().allMatch(v -> Math.abs(v) < 6.0));
    }
}
