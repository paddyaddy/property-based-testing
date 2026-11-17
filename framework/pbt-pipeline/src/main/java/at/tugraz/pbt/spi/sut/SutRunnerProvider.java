package at.tugraz.pbt.spi.sut;

import at.tugraz.pbt.descriptor.SutSpec;
import at.tugraz.pbt.spi.NamedProvider;

/**
 * SPI for pluggable SUT runners.
 */
public interface SutRunnerProvider extends NamedProvider {

    SutRunner build(SutSpec spec);
}
