package at.tugraz.pbt.demo.math;

public final class SafeDivision {

    /** Integer division that yields 0 instead of failing when the divisor is 0. */
    public static int nonThrowingDivide(Integer dividend, Integer divisor) {
        return divisor == 0 ? 0 : dividend / divisor;
    }

    /** Plain integer division: a zero divisor raises ArithmeticException. */
    public static int throwingDivide(Integer dividend, Integer divisor) {
        return dividend / divisor;
    }
}
