package demo.strings;

public final class StringOps {
    public static String reverse(String s) {
        return new StringBuilder(s).reverse().toString();
    }

    /** BUG: returns the string unchanged instead of reversing it. */
    public static String buggyReverse(String s) {
        return s;
    }
}
