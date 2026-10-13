package pbt.descriptor;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class SuiteLoaderTest {

    private SuiteDescriptor loadSample() {
        InputStream in = getClass().getClassLoader().getResourceAsStream("descriptors/sample-suite.json");
        assertNotNull(in, "sample descriptor resource is missing");
        Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8);
        return SuiteLoader.parse(reader, "sample-suite.json");
    }

    @Test
    void parses_a_well_formed_descriptor() {
        SuiteDescriptor d = loadSample();
        assertEquals("sort", d.name());
        assertEquals(2, d.suts().size());
        assertEquals("demo.sort.Sorter", d.suts().get("reference").className());
        assertEquals("intList", d.generator().kind());
        assertFalse(d.generator().regenerate());
        assertEquals(2, d.properties().size());

        PropertyDescriptor s1 = d.properties().get(0);
        assertEquals(PropertyKind.INVARIANT, s1.type());
        assertEquals("asp", s1.validator().get("kind").getAsString());
        assertEquals("sort_ordered.lp", s1.validator().get("relation").getAsString());

        PropertyDescriptor s4 = d.properties().get(1);
        assertEquals(PropertyKind.METAMORPHIC, s4.type());
        assertEquals("shuffle", s4.transform());
        assertEquals("at_least_two_elements", s4.applicability());
    }

    @Test
    void rejects_metamorphic_property_without_transform() {
        String json = """
                { "name": "x", "suts": { "ref": { "type": "java" } },
                  "generator": { "kind": "intList" },
                  "properties": [ { "id": "P1", "type": "METAMORPHIC",
                                    "validator": { "kind": "asp" } } ] }
                """;
        DescriptorException ex = assertThrows(DescriptorException.class,
                () -> SuiteLoader.parse(new StringReader(json), "inline"));
        assertTrue(ex.getMessage().contains("transform"));
    }

    @Test
    void rejects_invariant_property_with_transform() {
        String json = """
                { "name": "x", "suts": { "ref": { "type": "java" } },
                  "generator": { "kind": "intList" },
                  "properties": [ { "id": "P1", "type": "INVARIANT", "transform": "shuffle",
                                    "validator": { "kind": "asp" } } ] }
                """;
        DescriptorException ex = assertThrows(DescriptorException.class,
                () -> SuiteLoader.parse(new StringReader(json), "inline"));
        assertTrue(ex.getMessage().contains("transform"));
    }

    @Test
    void rejects_missing_validator_kind() {
        String json = """
                { "name": "x", "suts": { "ref": { "type": "java" } },
                  "generator": { "kind": "intList" },
                  "properties": [ { "id": "P1", "type": "INVARIANT", "validator": {} } ] }
                """;
        DescriptorException ex = assertThrows(DescriptorException.class,
                () -> SuiteLoader.parse(new StringReader(json), "inline"));
        assertTrue(ex.getMessage().contains("validator.kind"));
    }

    @Test
    void rejects_unknown_property_type() {
        String json = """
                { "name": "x", "suts": { "ref": { "type": "java" } },
                  "generator": { "kind": "intList" },
                  "properties": [ { "id": "P1", "type": "WEIRD", "validator": { "kind": "asp" } } ] }
                """;
        DescriptorException ex = assertThrows(DescriptorException.class,
                () -> SuiteLoader.parse(new StringReader(json), "inline"));
        assertTrue(ex.getMessage().contains("type"));
    }

    @Test
    void rejects_duplicate_property_ids() {
        String json = """
                { "name": "x", "suts": { "ref": { "type": "java" } },
                  "generator": { "kind": "intList" },
                  "properties": [ { "id": "P1", "type": "INVARIANT", "validator": { "kind": "asp" } },
                                  { "id": "P1", "type": "INVARIANT", "validator": { "kind": "asp" } } ] }
                """;
        DescriptorException ex = assertThrows(DescriptorException.class,
                () -> SuiteLoader.parse(new StringReader(json), "inline"));
        assertTrue(ex.getMessage().contains("duplicate"));
    }

    @Test
    void rejects_empty_properties() {
        String json = """
                { "name": "x", "suts": { "ref": { "type": "java" } },
                  "generator": { "kind": "intList" }, "properties": [] }
                """;
        assertThrows(DescriptorException.class,
                () -> SuiteLoader.parse(new StringReader(json), "inline"));
    }

    @Test
    void rejects_reserved_sut_name() {
        String json = """
                { "name": "x", "suts": { "all": { "type": "java" } },
                  "generator": { "kind": "intList" },
                  "properties": [ { "id": "P1", "type": "INVARIANT", "validator": { "kind": "asp" } } ] }
                """;
        DescriptorException ex = assertThrows(DescriptorException.class,
                () -> SuiteLoader.parse(new StringReader(json), "inline"));
        assertTrue(ex.getMessage().contains("all"));
    }
}
