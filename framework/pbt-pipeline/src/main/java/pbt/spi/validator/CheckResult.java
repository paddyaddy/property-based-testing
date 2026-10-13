package pbt.spi.validator;

/**
 * Outcome of evaluating one property against one input.
 *
 * <p>{@code diagnostics} carries free-form detail produced by the validator (e.g. the ASP
 * facts that were fed to the solver). Empty string when the validator emits nothing.
 */
public record CheckResult(boolean passed, String signature, String diagnostics) {

    public static CheckResult pass() {
        return new CheckResult(true, "ok", "");
    }

    public static CheckResult pass(String diagnostics) {
        return new CheckResult(true, "ok", diagnostics);
    }

    public static CheckResult fail(String signature) {
        return new CheckResult(false, signature, "");
    }

    public static CheckResult fail(String signature, String diagnostics) {
        return new CheckResult(false, signature, diagnostics);
    }
}
