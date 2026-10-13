package pbt.components.type;

import pbt.spi.type.ValueTypeProvider;

import java.lang.reflect.Type;

public final class BooleanValueTypeProvider implements ValueTypeProvider {

    @Override
    public String name() {
        return "boolean";
    }

    @Override
    public Type javaType() {
        return Boolean.class;
    }

    @Override
    public String describe(Object value) {
        return String.valueOf(value);
    }
}
