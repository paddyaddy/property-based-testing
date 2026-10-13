package pbt.descriptor;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

/**
 * Reads a {@link SuiteDescriptor} from JSON and checks it is structurally sound.
 *
 * <p>Validation here is structural only: it confirms required fields are present
 * and internally consistent. Whether a named component actually exists is decided
 * later, when the registries resolve it, so a descriptor can be authored and
 * inspected before the components are on the classpath.
 */
public final class SuiteLoader {

    private static final Gson GSON = new Gson();

    private SuiteLoader() {
    }

    /**
     * Loads and validates the descriptor at {@code path}.
     */
    public static SuiteDescriptor load(Path path) {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return parse(reader, path.toString());
        } catch (IOException e) {
            throw new DescriptorException("cannot read descriptor: " + path, e);
        }
    }

    /**
     * Parses and validates a descriptor from {@code reader}; {@code source} names it in errors.
     */
    public static SuiteDescriptor parse(Reader reader, String source) {
        SuiteDescriptor descriptor;
        try {
            descriptor = GSON.fromJson(reader, SuiteDescriptor.class);
        } catch (JsonParseException e) {
            throw new DescriptorException(source + ": malformed JSON: " + e.getMessage(), e);
        }
        if (descriptor == null) {
            throw new DescriptorException(source + ": descriptor is empty");
        }
        validate(descriptor, source);
        return descriptor;
    }

    private static void validate(SuiteDescriptor d, String source) {
        if (isBlank(d.name())) {
            throw new DescriptorException(source + ": suite is missing a 'name'");
        }
        validateSuts(d, source);
        validateGenerator(d, source);
        validateProperties(d, source);
    }

    private static void validateSuts(SuiteDescriptor d, String source) {
        if (d.suts() == null || d.suts().isEmpty()) {
            throw new DescriptorException(source + ": suite '" + d.name() + "' declares no 'suts'");
        }
        for (var entry : d.suts().entrySet()) {
            String key = entry.getKey();
            if ("all".equals(key)) {
                throw new DescriptorException(source + ": SUT name 'all' is reserved");
            }
            if (key.contains(",")) {
                throw new DescriptorException(source + ": SUT name '" + key + "' must not contain a comma");
            }
            SutSpec sut = entry.getValue();
            if (sut == null || isBlank(sut.type())) {
                throw new DescriptorException(source + ": SUT '" + key + "' is missing a 'type'");
            }
        }
    }

    private static void validateGenerator(SuiteDescriptor d, String source) {
        if (d.generator() == null || isBlank(d.generator().kind())) {
            throw new DescriptorException(source + ": suite '" + d.name() + "' is missing a 'generator.kind'");
        }
    }

    private static void validateProperties(SuiteDescriptor d, String source) {
        if (d.properties() == null || d.properties().isEmpty()) {
            throw new DescriptorException(source + ": suite '" + d.name() + "' declares no 'properties'");
        }
        Set<String> ids = new HashSet<>();
        for (PropertyDescriptor p : d.properties()) {
            if (isBlank(p.id())) {
                throw new DescriptorException(source + ": a property is missing an 'id'");
            }
            if (!ids.add(p.id())) {
                throw new DescriptorException(source + ": duplicate property id '" + p.id() + "'");
            }
            if (p.type() == null) {
                throw new DescriptorException(source + ": property '" + p.id()
                        + "' has a missing or unknown 'type' (expected INVARIANT or METAMORPHIC)");
            }
            validateValidator(p, source);
            validateTransform(p, source);
        }
    }

    private static void validateValidator(PropertyDescriptor p, String source) {
        JsonObject validator = p.validator();
        if (validator == null || isBlank(memberAsString(validator, "kind"))) {
            throw new DescriptorException(source + ": property '" + p.id()
                    + "' is missing a 'validator.kind'");
        }
    }

    private static void validateTransform(PropertyDescriptor p, String source) {
        boolean hasTransform = !isBlank(p.transform());
        if (p.type() == PropertyKind.METAMORPHIC && !hasTransform) {
            throw new DescriptorException(source + ": metamorphic property '" + p.id()
                    + "' must declare a 'transform'");
        }
        if (p.type() == PropertyKind.INVARIANT && hasTransform) {
            throw new DescriptorException(source + ": invariant property '" + p.id()
                    + "' must not declare a 'transform'");
        }
    }

    private static String memberAsString(JsonObject object, String member) {
        JsonElement e = object.get(member);
        if (e == null || e.isJsonNull() || !e.isJsonPrimitive()) {
            return null;
        }
        return e.getAsString();
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
