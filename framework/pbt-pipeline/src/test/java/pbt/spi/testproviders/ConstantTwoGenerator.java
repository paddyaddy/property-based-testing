package pbt.spi.testproviders;

/**
 * Deliberately absent from META-INF/services; resolved only via fully qualified class name.
 * Uses a {@code provider()} factory method (private constructor) to exercise that resolution path.
 */
public final class ConstantTwoGenerator implements FakeGeneratorProvider {

    private ConstantTwoGenerator() {
    }

    public static ConstantTwoGenerator provider() {
        return new ConstantTwoGenerator();
    }

    @Override
    public String name() {
        return "two";
    }

    @Override
    public int generate() {
        return 2;
    }
}
