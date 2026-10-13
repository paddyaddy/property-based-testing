package pbt.spi.asp;

/**
 * Minimal interface over an Answer Set Programming solver.
 *
 * <p>Only satisfiability checking ({@link #entails}) is required.
 */
public interface AspEngine {

    /**
     * Whether the engine is usable (e.g. the solver binary is present).
     * Callers should refuse to run ASP-backed properties against an unavailable engine.
     */
    default boolean isAvailable() {
        return true;
    }

    /**
     * Returns {@code true} if the program is satisfiable and some answer set contains
     * {@code queryAtom}.
     *
     * @param program   a complete ASP program (facts + rules + show directives)
     * @param queryAtom a ground atom, e.g. {@code "violation"}
     */
    boolean entails(String program, String queryAtom);

    /**
     * Human-readable name for diagnostics.
     */
    default String name() {
        return getClass().getSimpleName();
    }
}
