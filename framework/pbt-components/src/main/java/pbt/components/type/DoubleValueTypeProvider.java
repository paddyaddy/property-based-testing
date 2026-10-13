package pbt.components.type;

import pbt.spi.type.ValueTypeProvider;

import java.lang.reflect.Type;

public final class DoubleValueTypeProvider implements ValueTypeProvider {

    @Override
    public String name() {
        return "double";
    }

    @Override
    public Type javaType() {
        return Double.class;
    }

    @Override
    public String describe(Object value) {
        return String.valueOf(value);
    }
}
