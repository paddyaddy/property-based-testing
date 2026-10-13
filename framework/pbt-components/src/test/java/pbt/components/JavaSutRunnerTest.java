package pbt.components;

import org.junit.jupiter.api.Test;
import pbt.components.sut.JavaSutRunnerProvider;
import pbt.descriptor.SutSpec;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JavaSutRunnerTest {

    private static SutSpec spec(String method, boolean isStatic) {
        return new SutSpec("java", Overloaded.class.getName(), method, isStatic,
                null, null, null, null, null);
    }

    @Test
    void parameter_type_selects_among_overloads() {
        var provider = new JavaSutRunnerProvider();

        assertEquals("list:2", provider.build(spec("size(List)", true)).run(List.of(1, 2)));
        assertEquals("list:2", provider.build(spec("size(java.util.List<java.lang.Integer>)", true))
                .run(List.of(1, 2)));
        assertEquals("string:3", provider.build(spec("size(String)", true)).run("abc"));
    }

    @Test
    void unnamed_overload_is_ambiguous() {
        var e = assertThrows(IllegalArgumentException.class,
                () -> new JavaSutRunnerProvider().build(spec("size", true)));
        assertTrue(e.getMessage().contains("ambiguous"), e.getMessage());
    }

    @Test
    void single_method_needs_no_parameter_type() {
        assertEquals("unique:1", new JavaSutRunnerProvider().build(spec("unique", true)).run(List.of(9)));
    }

    @Test
    void missing_class_or_method_fails_when_building() {
        var provider = new JavaSutRunnerProvider();
        assertThrows(IllegalArgumentException.class, () -> provider.build(spec("nope", true)));
        assertThrows(IllegalArgumentException.class, () -> provider.build(
                new SutSpec("java", "does.not.Exist", "size", true, null, null, null, null, null)));
    }

    @Test
    void instance_methods_run_on_a_fresh_instance_per_call() {
        var runner = new JavaSutRunnerProvider().build(spec("counter", false));

        assertEquals("fresh:1", runner.run(0));
        assertEquals("fresh:1", runner.run(0));
    }

    public static final class Overloaded {
        private int count;

        public static String size(List<Integer> xs) {
            return "list:" + xs.size();
        }

        public static String size(String s) {
            return "string:" + s.length();
        }

        public static String unique(List<Integer> xs) {
            return "unique:" + xs.size();
        }

        public String counter(Integer n) {
            return "fresh:" + (++count);
        }
    }
}
