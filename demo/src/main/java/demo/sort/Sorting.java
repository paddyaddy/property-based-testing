package demo.sort;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Sorting {
    private Sorting() {}

    public static List<Integer> sorted(List<Integer> xs) {
        List<Integer> copy = new ArrayList<>(xs);
        Collections.sort(copy);
        return copy;
    }
}
