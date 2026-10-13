package demo.math;

public final class LongMath {
    public static long abs(long n) {
        return Math.abs(n);
    }

    /** BUG: returns n unchanged, so negative inputs produce a negative result. */
    public static long buggyAbs(long n) {
        return n;
    }
}
