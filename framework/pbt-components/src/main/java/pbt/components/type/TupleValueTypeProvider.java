package pbt.components.type;

import pbt.spi.type.Tuple;
import pbt.spi.type.ValueTypeProvider;

import java.lang.reflect.Type;

/**
 * Value type for {@link Tuple}. Serialized as {@code {"values": [...]}}.
 */
public final class TupleValueTypeProvider implements ValueTypeProvider {

    @Override
    public String name() {
        return "tuple";
    }

    @Override
    public Type javaType() {
        return Tuple.class;
    }

    @Override
    public String describe(Object value) {
        return String.valueOf(value);
    }
}
