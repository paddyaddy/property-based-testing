package pbt.spi.validator;

/**
 * Checks whether a property holds for the data in a {@link CheckContext}.
 */
@FunctionalInterface
public interface Validator {

    CheckResult check(CheckContext ctx);
}
