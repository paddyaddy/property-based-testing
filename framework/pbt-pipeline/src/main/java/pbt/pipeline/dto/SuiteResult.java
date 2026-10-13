package pbt.pipeline.dto;

import java.util.List;

/**
 * Aggregate result of running all properties in a suite.
 */
public record SuiteResult(String name, List<PropertyResult> properties) {

    public boolean passed() {
        return properties.stream().allMatch(PropertyResult::passed);
    }
}
