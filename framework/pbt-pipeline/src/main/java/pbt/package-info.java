/**
 * Core of the property-based testing framework.
 *
 * <p>This module holds the pipeline runner, the descriptor model and its loader, the
 * service-provider contracts that make every component pluggable, and the artifact
 * codec used for traceability. It deliberately contains no concrete component and no
 * oracle implementation;
 * those live in {@code pbt-components} and are discovered at runtime.
 */
package pbt;
