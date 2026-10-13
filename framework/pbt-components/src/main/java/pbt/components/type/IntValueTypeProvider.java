package pbt.components.type;

import pbt.spi.type.ValueTypeProvider;

import java.lang.reflect.Type;

/**
 * Value type for {@link Integer}.
 */
public final class IntValueTypeProvider implements ValueTypeProvider {

    @Override
    public String name() {
        return "int";
    }

    @Override
    public Type javaType() {
        return Integer.class;
    }

    @Override
    public String describe(Object value) {
        return String.valueOf(value);
    }
}
