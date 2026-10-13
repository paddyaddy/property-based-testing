package pbt.spi.transform;

import pbt.spi.NamedProvider;

/**
 * SPI for pluggable metamorphic transforms.
 */
public interface TransformProvider extends NamedProvider {

    Transform get();
}
