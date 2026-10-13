package pbt.descriptor;

import com.google.gson.JsonObject;

/**
 * One property entry from a descriptor's {@code properties} list.
 *
 * <p>The oracle is chosen by the {@code validator} object: its {@code kind} names a
 * validator component and its remaining fields are that component's parameters (for
 * example an ASP relation program). Facts are not named here; they follow from the
 * suite's value types and the property {@link PropertyKind}.
 *
 * @param type          invariant or metamorphic
 * @param validator     oracle selection: a {@code kind} plus component parameters
 * @param transform     registry key of the follow-up transform (metamorphic only)
 * @param applicability classpath path of an optional {@code .lp} precondition filter
 */
public record PropertyDescriptor(
        String id,
        PropertyKind type,
        String description,
        JsonObject validator,
        String transform,
        String applicability) {
}
