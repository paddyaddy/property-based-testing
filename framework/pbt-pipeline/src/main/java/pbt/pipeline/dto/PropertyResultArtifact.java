package pbt.pipeline.dto;

/**
 * Serialization-friendly projection of {@link PropertyResult} for artifact persistence.
 */
public record PropertyResultArtifact(
        String propertyId,
        boolean passed,
        int generated,
        int runs,
        int skipped,
        int failures,
        String witness) {
}
