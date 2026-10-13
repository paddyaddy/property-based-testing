package pbt.spi.testproviders;

/**
 * Registered in META-INF/services, resolved by short name {@code one}.
 */
public final class ConstantOneGenerator implements FakeGeneratorProvider {

    @Override
    public String name() {
        return "one";
    }

    @Override
    public int generate() {
        return 1;
    }
}
