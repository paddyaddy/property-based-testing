package pbt.artifact;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Reads and writes pipeline artifacts as JSON, binding each value to an explicit
 * {@link Type} so generic artifacts (for example a record carrying a
 * {@code List<Integer>}) round-trip with their element types intact rather than as
 * loosely typed maps.
 *
 * <p>Persisting every stage boundary through this codec is what makes a run
 * reproducible and inspectable after the fact.
 */
public final class ArtifactCodec {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Gson COMPACT = new Gson();

    /**
     * Serializes {@code value} as indented JSON using {@code type} as the declared type.
     */
    public String toJson(Object value, Type type) {
        return GSON.toJson(value, type);
    }

    /**
     * Serializes {@code value} as single-line JSON, for values embedded as a string field
     * of another artifact.
     */
    public String toCompactJson(Object value, Type type) {
        return COMPACT.toJson(value, type);
    }

    /**
     * Parses JSON into an artifact of {@code type}.
     */
    public <T> T fromJson(String json, Type type) {
        try {
            return GSON.fromJson(json, type);
        } catch (JsonParseException e) {
            throw new IllegalArgumentException(
                    "cannot parse artifact of type " + type + ": " + e.getMessage(), e);
        }
    }

    /**
     * Writes {@code value} to {@code path}, creating parent directories.
     */
    public void write(Path path, Object value, Type type) {
        try {
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                GSON.toJson(value, type, writer);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("cannot write artifact: " + path, e);
        }
    }

    /**
     * Reads an artifact of {@code type} from {@code path}.
     */
    public <T> T read(Path path, Type type) {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return GSON.fromJson(reader, type);
        } catch (IOException e) {
            throw new UncheckedIOException("cannot read artifact: " + path, e);
        }
    }
}
