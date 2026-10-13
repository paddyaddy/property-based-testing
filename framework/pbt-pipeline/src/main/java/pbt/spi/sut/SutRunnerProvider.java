package pbt.spi.sut;

import pbt.descriptor.SutSpec;
import pbt.spi.NamedProvider;

/**
 * SPI for pluggable SUT runners.
 */
public interface SutRunnerProvider extends NamedProvider {

    SutRunner build(SutSpec spec);
}
