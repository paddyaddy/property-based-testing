package demo.search;

import java.util.List;

public final class Search {

    /** Index of the first occurrence of {@code needle} in {@code haystack}, or -1 if absent. */
    public static int findFirst(List<Integer> haystack, Integer needle) {
        for (int i = 0; i < haystack.size(); i++) {
            if (haystack.get(i).equals(needle)) {
                return i;
            }
        }
        return -1;
    }

    /** BUG: scans from the back, so with duplicates it returns the last index instead of the first. */
    public static int buggyFindFirst(List<Integer> haystack, Integer needle) {
        for (int i = haystack.size() - 1; i >= 0; i--) {
            if (haystack.get(i).equals(needle)) {
                return i;
            }
        }
        return -1;
    }
}
