package demo.math;

public final class DoubleMath {
    public static double abs(double d) {
        return Math.abs(d);
    }

    /** BUG: returns d unchanged, so negative inputs produce a negative result. */
    public static double buggyAbs(double d) {
        return d;
    }
}
