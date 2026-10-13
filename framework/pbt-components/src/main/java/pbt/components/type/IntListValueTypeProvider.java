package pbt.components.type;

import com.google.gson.reflect.TypeToken;
import pbt.spi.type.ValueTypeProvider;

import java.lang.reflect.Type;
import java.util.List;

/**
 * Value type for {@code List<Integer>}.
 */
public final class IntListValueTypeProvider implements ValueTypeProvider {

    private static final Type TYPE = new TypeToken<List<Integer>>() {
    }.getType();

    @Override
    public String name() {
        return "intList";
    }

    @Override
    public Type javaType() {
        return TYPE;
    }

    @Override
    public String describe(Object value) {
        return String.valueOf(value);
    }
}
