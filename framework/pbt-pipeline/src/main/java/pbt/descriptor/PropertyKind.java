package pbt.descriptor;

/**
 * The two property families the framework evaluates.
 *
 * <p>An {@link #INVARIANT} is checked against a single execution. A
 * {@link #METAMORPHIC} property derives a follow-up input through a transform and
 * relates the two executions.
 */
public enum PropertyKind {
    INVARIANT,
    METAMORPHIC
}
