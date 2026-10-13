package demo.math;

public final class BooleanOps {
    public static boolean not(boolean b) {
        return !b;
    }

    /** BUG: returns b unchanged instead of negating it. */
    public static boolean buggyNot(boolean b) {
        return b;
    }
}
