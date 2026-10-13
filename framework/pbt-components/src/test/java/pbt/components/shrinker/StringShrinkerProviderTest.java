package pbt.components.shrinker;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;
import pbt.spi.shrinker.Shrinker;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

class StringShrinkerProviderTest {

    @SuppressWarnings("unchecked")
    private Shrinker<String> build(JsonObject params) {
        return (Shrinker<String>) new StringShrinkerProvider().build(params);
    }

    @Test
    void shrinks_length_by_removing_chars() {
        Shrinker<String> shrinker = build(new JsonObject());
        List<String> candidates = shrinker.shrink("abc").collect(Collectors.toList());
        assertTrue(candidates.stream().anyMatch(s -> s.length() < 3), "no shorter candidate");
    }

    @Test
    void shifts_chars_toward_first_in_set() {
        JsonObject params = new JsonObject();
        params.addProperty("chars", "abcdefghijklmnopqrstuvwxyz");
        Shrinker<String> shrinker = build(params);
        List<String> candidates = shrinker.shrink("z").collect(Collectors.toList());
        assertTrue(candidates.contains("a"), "did not offer 'a' as simpler candidate");
    }

    @Test
    void empty_string_at_minLen_zero_has_no_candidates() {
        Shrinker<String> shrinker = build(new JsonObject());
        List<String> candidates = shrinker.shrink("").collect(Collectors.toList());
        assertTrue(candidates.isEmpty(), "empty string should have no shrink candidates");
    }

    @Test
    void does_not_shrink_below_min_len() {
        JsonObject params = new JsonObject();
        params.addProperty("minLen", 2);
        Shrinker<String> shrinker = build(params);
        List<String> candidates = shrinker.shrink("ab").collect(Collectors.toList());
        assertTrue(candidates.stream().allMatch(s -> s.length() >= 2),
                "shrink produced string shorter than minLen=2");
    }
}
