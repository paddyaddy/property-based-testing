package pbt.components.type;

import pbt.spi.type.ValueTypeProvider;

import java.lang.reflect.Type;

public final class StringValueTypeProvider implements ValueTypeProvider {

    @Override
    public String name() {
        return "string";
    }

    @Override
    public Type javaType() {
        return String.class;
    }

    @Override
    public String describe(Object value) {
        return "\"" + value + "\"";
    }
}
