package pbt.artifact;

import com.google.gson.reflect.TypeToken;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ArtifactCodecTest {

    private static final Type SAMPLE = Sample.class;

    @Test
    void round_trips_a_record_through_a_string() {
        ArtifactCodec codec = new ArtifactCodec();
        Sample original = new Sample("xs", List.of(3, 1, 2));
        Sample back = codec.fromJson(codec.toJson(original, SAMPLE), SAMPLE);
        assertEquals(original, back);
    }

    @Test
    void binds_element_types_of_a_parameterized_type() {
        ArtifactCodec codec = new ArtifactCodec();
        Type listOfInts = new TypeToken<List<Integer>>() {
        }.getType();
        List<Integer> original = List.of(5, 6, 7);
        List<Integer> back = codec.fromJson(codec.toJson(original, listOfInts), listOfInts);
        assertEquals(original, back);
        // Without a bound type Gson would decode numbers as Double; the type keeps them Integer.
        assertEquals(Integer.class, back.get(0).getClass());
    }

    @Test
    void compact_json_is_a_single_line_and_round_trips() {
        ArtifactCodec codec = new ArtifactCodec();
        Type listOfInts = new TypeToken<List<Integer>>() {
        }.getType();
        String json = codec.toCompactJson(List.of(0, 1), listOfInts);
        assertEquals("[0,1]", json);
        assertEquals(List.of(0, 1), codec.fromJson(json, listOfInts));
    }

    @Test
    void writes_and_reads_a_file_creating_parent_dirs(@TempDir Path dir) {
        ArtifactCodec codec = new ArtifactCodec();
        Path path = dir.resolve("nested/sample.json");
        Sample original = new Sample("ys", List.of(9));
        codec.write(path, original, SAMPLE);
        assertEquals(original, codec.read(path, SAMPLE));
    }

    record Sample(String label, List<Integer> values) {
    }
}
