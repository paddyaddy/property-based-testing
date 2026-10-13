/**
 * The collection of pre-built, pluggable components: generators, shrinkers,
 * validators, transforms, applicability filters, SUT runners, value types, and ASP
 * fact generators.
 *
 * <p>Each component is registered through {@link java.util.ServiceLoader} so a suite
 * descriptor can select it by a short name, and a user project can add its own
 * components by putting them on the classpath. This module contributes the builtin
 * implementations.
 */
package pbt.components;
