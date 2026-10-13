package pbt.components.shrinker;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;
import pbt.spi.shrinker.Shrinker;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IntListShrinkerProviderTest {

    @SuppressWarnings("unchecked")
    private Shrinker<List<Integer>> build(int minLen, int minValue, int maxValue) {
        JsonObject params = new JsonObject();
        params.addProperty("minLen", minLen);
        params.addProperty("minValue", minValue);
        params.addProperty("maxValue", maxValue);
        return (Shrinker<List<Integer>>) new IntListShrinkerProvider().build(params);
    }

    @Test
    void shrinks_length_by_removing_elements() {
        Shrinker<List<Integer>> shrinker = build(0, -10, 10);
        List<Integer> xs = List.of(1, 2, 3);
        List<List<Integer>> candidates = shrinker.shrink(xs).collect(Collectors.toList());
        assertTrue(candidates.stream().anyMatch(c -> c.size() < xs.size()), "no shorter candidate");
    }

    @Test
    void shrinks_values_toward_zero() {
        Shrinker<List<Integer>> shrinker = build(0, -10, 10);
        List<Integer> xs = List.of(5);
        List<List<Integer>> candidates = shrinker.shrink(xs).collect(Collectors.toList());
        assertTrue(candidates.stream().anyMatch(c -> c.equals(List.of(0))), "zero not offered");
    }

    @Test
    void does_not_shrink_below_min_length() {
        Shrinker<List<Integer>> shrinker = build(2, -10, 10);
        List<Integer> xs = List.of(1, 2);
        List<List<Integer>> candidates = shrinker.shrink(xs).collect(Collectors.toList());
        assertTrue(candidates.stream().allMatch(c -> c.size() >= 2),
                "shrink produced list shorter than minLen=2");
    }

    @Test
    void empty_list_at_minLen_has_no_length_candidates() {
        Shrinker<List<Integer>> shrinker = build(0, 0, 10);
        List<List<Integer>> candidates = shrinker.shrink(List.of()).collect(Collectors.toList());
        assertFalse(candidates.stream().anyMatch(c -> c.size() < 0), "negative length candidate");
    }
}
