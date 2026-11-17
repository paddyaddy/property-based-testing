package at.tugraz.pbt.spi.transform;

import at.tugraz.pbt.spi.NamedProvider;

/**
 * SPI for pluggable metamorphic transforms.
 */
public interface TransformProvider extends NamedProvider {

    Transform get();
}
