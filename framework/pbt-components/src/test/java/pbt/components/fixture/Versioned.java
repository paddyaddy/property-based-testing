package pbt.components.fixture;

/**
 * Stand-in for a class that exists in two versions; the test compiles a second copy with
 * different behaviour and loads it through a SUT search path.
 */
public final class Versioned {

    public static String version(String input) {
        return "project";
    }
}
