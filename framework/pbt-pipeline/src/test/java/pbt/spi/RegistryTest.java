package pbt.spi;

import org.junit.jupiter.api.Test;
import pbt.spi.testproviders.FakeGeneratorProvider;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class RegistryTest {

    private Registry<FakeGeneratorProvider> registry() {
        return Registry.load(FakeGeneratorProvider.class);
    }

    @Test
    void discovers_providers_by_short_name() {
        Registry<FakeGeneratorProvider> reg = registry();
        assertEquals(0, reg.get("zero").generate());
        assertEquals(1, reg.get("one").generate());
        assertTrue(reg.names().containsAll(Set.of("zero", "one")));
    }

    @Test
    void falls_back_to_fully_qualified_class_name() {
        Registry<FakeGeneratorProvider> reg = registry();
        // ConstantTwoGenerator is intentionally absent from META-INF/services.
        assertFalse(reg.names().contains("two"));
        FakeGeneratorProvider two = reg.get("pbt.spi.testproviders.ConstantTwoGenerator");
        assertEquals(2, two.generate());
    }

    @Test
    void unknown_short_name_reports_registered_names() {
        Registry<FakeGeneratorProvider> reg = registry();
        ProviderException ex = assertThrows(ProviderException.class, () -> reg.get("intList"));
        assertTrue(ex.getMessage().contains("intList"));
        assertTrue(ex.getMessage().contains("zero"), "error should list registered names");
    }

    @Test
    void class_name_of_wrong_type_is_rejected() {
        Registry<FakeGeneratorProvider> reg = registry();
        assertThrows(ProviderException.class, () -> reg.get("java.lang.String"));
    }

    @Test
    void has_reports_resolvability_without_throwing() {
        Registry<FakeGeneratorProvider> reg = registry();
        assertTrue(reg.has("zero"));
        assertTrue(reg.has("pbt.spi.testproviders.ConstantTwoGenerator"));
        assertFalse(reg.has("nope"));
        assertFalse(reg.has("com.example.DoesNotExist"));
    }

    @Test
    void blank_key_is_rejected() {
        Registry<FakeGeneratorProvider> reg = registry();
        assertThrows(ProviderException.class, () -> reg.get(" "));
    }
}
