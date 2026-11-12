package pbt.components.validator;

import com.google.gson.JsonObject;
import pbt.spi.validator.CheckResult;
import pbt.spi.validator.Validator;
import pbt.spi.validator.ValidatorProvider;

/**
 * A validator that accepts every execution. It exercises the pipeline wiring (generation,
 * SUT execution, reporting) without an oracle.
 */
public final class TrivialValidatorProvider implements ValidatorProvider {

    @Override
    public String name() {
        return "trivial";
    }

    @Override
    public Validator build(JsonObject params, String generatorKind) {
        return ctx -> CheckResult.pass();
    }
}
