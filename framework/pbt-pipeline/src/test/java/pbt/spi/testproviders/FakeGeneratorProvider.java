package pbt.spi.testproviders;

import pbt.spi.NamedProvider;

/**
 * Test-only provider type used to exercise {@link pbt.spi.Registry}.
 */
public interface FakeGeneratorProvider extends NamedProvider {
    int generate();
}
