package at.tugraz.pbt.components.generator;

import com.google.gson.JsonObject;
import at.tugraz.pbt.spi.generator.Generator;
import at.tugraz.pbt.spi.generator.GeneratorProvider;

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
