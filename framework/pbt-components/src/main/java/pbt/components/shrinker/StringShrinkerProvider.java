package pbt.components.shrinker;

import com.google.gson.JsonObject;
import pbt.components.generator.IntGeneratorProvider;
import pbt.components.generator.StringGeneratorProvider;
import pbt.spi.shrinker.Shrinker;
import pbt.spi.shrinker.ShrinkerProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Shrinks strings by removing characters and shifting each character toward the
 * first character in the configured set (default: 'a').
 */
public final class StringShrinkerProvider implements ShrinkerProvider {

    private static Stream<String> shrink(String value, int minLen, char target) {
        List<String> candidates = new ArrayList<>();
        int n = value.length();

        if (n > minLen) {
            for (int i = 0; i < n; i++) {
                candidates.add(value.substring(0, i) + value.substring(i + 1));
            }
        }

        for (int i = 0; i < n; i++) {
            if (value.charAt(i) != target) {
                candidates.add(value.substring(0, i) + target + value.substring(i + 1));
            }
        }

        return candidates.stream().distinct().filter(s -> !s.equals(value));
    }

    @Override
    public String name() {
        return "string";
    }

    @Override
    public Shrinker<String> build(JsonObject params) {
        int minLen = IntGeneratorProvider.getInt(params, "minLen", 0);
        char target = StringGeneratorProvider.getChars(params).charAt(0);
        return value -> shrink(value, minLen, target);
    }
}
