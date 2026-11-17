package at.tugraz.pbt.spi.testproviders;

import at.tugraz.pbt.spi.NamedProvider;

/**
 * Test-only provider type used to exercise {@link at.tugraz.pbt.spi.Registry}.
 */
public interface FakeGeneratorProvider extends NamedProvider {
    int generate();
}
