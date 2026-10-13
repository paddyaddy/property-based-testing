package pbt.spi.testproviders;

/**
 * Registered in META-INF/services, resolved by short name {@code zero}.
 */
public final class ConstantZeroGenerator implements FakeGeneratorProvider {

    @Override
    public String name() {
        return "zero";
    }

    @Override
    public int generate() {
        return 0;
    }
}
