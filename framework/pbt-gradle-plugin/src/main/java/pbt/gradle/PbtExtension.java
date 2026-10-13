package pbt.gradle;

import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;

/**
 * DSL extension for the {@code pbt} block in a consuming project's build script.
 */
public abstract class PbtExtension {

    /**
     * Directory containing suite descriptor JSON files. Default: {@code src/main/resources/suites}.
     */
    public abstract DirectoryProperty getSuiteDir();

    /**
     * Number of generated inputs per property. Default: 100.
     */
    public abstract Property<Integer> getRunsPerProperty();

    /**
     * Upper bound on candidates evaluated while minimizing one witness. Default: 1000.
     */
    public abstract Property<Integer> getMaxShrinkSteps();

    /**
     * Random seed for input generation. Default: 42.
     */
    public abstract Property<Long> getSeed();
}
