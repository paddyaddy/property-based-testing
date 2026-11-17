package at.tugraz.pbt.components.type;

import at.tugraz.pbt.spi.type.ValueTypeProvider;

import java.lang.reflect.Type;

public final class LongValueTypeProvider implements ValueTypeProvider {

    @Override
    public String name() {
        return "long";
    }

    @Override
    public Type javaType() {
        return Long.class;
    }

    @Override
    public String describe(Object value) {
        return String.valueOf(value);
    }
}
