package at.tugraz.pbt.components.tuple;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

/**
 * One position of a {@code tuple}: the kind of the component's generator, the params that
 * generator, shrinker and fact generator are built with, and the key of the fact generator that
 * renders the component, which is the kind unless the component names another one.
 */
public record TupleComponent(String kind, JsonObject params, String factGenerator) {

    /**
     * Reads the {@code components} array of a tuple's generator params. Each entry is
     * {@code {"kind": "int", "params": {...}, "factGenerator": "..."}}; {@code params} and
     * {@code factGenerator} are optional, like the suite's own {@code factGenerator}.
     */
    public static List<TupleComponent> parse(JsonObject tupleParams) {
        if (tupleParams == null || !tupleParams.has("components") || !tupleParams.get("components").isJsonArray()) {
            throw new IllegalArgumentException("tuple needs a 'components' array in the generator params");
        }
        List<TupleComponent> components = new ArrayList<>();
        for (JsonElement e : tupleParams.getAsJsonArray("components")) {
            if (!e.isJsonObject() || !e.getAsJsonObject().has("kind")) {
                throw new IllegalArgumentException("tuple component must be an object with a 'kind': " + e);
            }
            JsonObject component = e.getAsJsonObject();
            JsonObject params = component.has("params") ? component.getAsJsonObject("params") : new JsonObject();
            String kind = component.get("kind").getAsString();
            String factGenerator = component.has("factGenerator") ? component.get("factGenerator").getAsString() : kind;
            components.add(new TupleComponent(kind, params, factGenerator));
        }
        if (components.isEmpty()) {
            throw new IllegalArgumentException("tuple needs at least one component");
        }
        return components;
    }
}
