package demo.sort;

import java.util.List;

/** BUG: drops the last positional element before sorting. */
public final class BuggySorter {
    public List<Integer> sort(List<Integer> input) {
        List<Integer> truncated = input.subList(0, Math.max(0, input.size() - 1));
        return Sorting.sorted(truncated);
    }
}
