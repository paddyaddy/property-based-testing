package pbt.components.generator;

import com.google.gson.JsonObject;
import pbt.spi.generator.Generator;
import pbt.spi.generator.GeneratorProvider;

public final class BooleanGeneratorProvider implements GeneratorProvider {

    @Override
    public String name() {
        return "boolean";
    }

    @Override
    public Generator<Boolean> build(JsonObject params) {
        return rnd -> rnd.nextBoolean();
    }
}
