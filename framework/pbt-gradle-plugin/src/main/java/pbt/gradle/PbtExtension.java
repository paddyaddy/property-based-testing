package pbt.gradle;

import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;

/**
 * DSL extension for the {@code pbt} block in a consuming project's build script.
 */
public abstract class PbtExtension {

    /**
     * Name of the source set the suites and custom components live in. Its output and runtime
     * classpath are what the SUTs and components are loaded from, so a library can keep them in a
     * dedicated source set (for example {@code pbt}) next to its own code. The source set must
     * exist when the task runs. Default: {@code main}.
     */
    public abstract Property<String> getSourceSet();

    /**
     * Directory containing suite descriptor JSON files. Default:
     * {@code src/<sourceSet>/resources/suites}.
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
