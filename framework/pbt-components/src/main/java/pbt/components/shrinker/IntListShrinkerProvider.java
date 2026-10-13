package pbt.components.shrinker;

import com.google.gson.JsonObject;
import pbt.components.generator.IntGeneratorProvider;
import pbt.spi.shrinker.Shrinker;
import pbt.spi.shrinker.ShrinkerProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Shrinks {@code List<Integer>} by removing contiguous blocks (larger first) and
 * moving individual element values toward zero.
 *
 * <p>Name matches the {@code intList} generator kind. Uses the same bounds params
 * ({@code minLen}, {@code minValue}, {@code maxValue}) as the generator to respect
 * the valid input domain.
 */
public final class IntListShrinkerProvider implements ShrinkerProvider {

    /**
     * Default maximum block size for contiguous removal. Covers parity and small modular constraints.
     */
    private static final int DEFAULT_MAX_BLOCK = 4;

    private static Stream<List<Integer>> shrink(
            List<Integer> value, int minLen, int minValue, int maxValue) {
        List<List<Integer>> candidates = new ArrayList<>();
        int n = value.size();
        int maxBlock = Math.min(DEFAULT_MAX_BLOCK, n - minLen);

        for (int b = maxBlock; b >= 1; b--) {
            for (int i = 0; i + b <= n; i++) {
                List<Integer> copy = new ArrayList<>(value.subList(0, i));
                copy.addAll(value.subList(i + b, n));
                candidates.add(copy);
            }
        }

        for (int i = 0; i < n; i++) {
            int elem = value.get(i);
            Iterable<Integer> smaller = IntShrinkerProvider.shrinkInt(elem)::iterator;
            for (int s : smaller) {
                if (s >= minValue && s <= maxValue) {
                    List<Integer> copy = new ArrayList<>(value);
                    copy.set(i, s);
                    candidates.add(copy);
                }
            }
        }

        return candidates.stream().distinct().filter(c -> !c.equals(value));
    }

    @Override
    public String name() {
        return "intList";
    }

    @Override
    public Shrinker<List<Integer>> build(JsonObject params) {
        int minLen = IntGeneratorProvider.getInt(params, "minLen", 0);
        int minValue = IntGeneratorProvider.getInt(params, "minValue", -100);
        int maxValue = IntGeneratorProvider.getInt(params, "maxValue", 100);
        return value -> shrink(value, minLen, minValue, maxValue);
    }
}
