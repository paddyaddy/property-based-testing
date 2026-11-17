package at.tugraz.pbt.components.validator;

import com.google.gson.JsonObject;
import at.tugraz.pbt.spi.validator.CheckResult;
import at.tugraz.pbt.spi.validator.Validator;
import at.tugraz.pbt.spi.validator.ValidatorProvider;

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
