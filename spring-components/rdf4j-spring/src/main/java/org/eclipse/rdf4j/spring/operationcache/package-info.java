/**
 *
 *
 * <H1>Rdf4j-Spring OperationCache</H1>
 *
 * Provides connection-level caching of SPARQL operations.
 *
 * <p>
 * To enable, set: <code>rdf4j.spring.operationcache.enabled=true</code>.
 *
 * <p>
 * If enabled, the {@link org.eclipse.rdf4j.spring.support.Rdf4JTemplate Rdf4JTemplate}, set up by
 * {@link org.eclipse.rdf4j.spring.Rdf4JConfig}, will use the
 * {@link org.eclipse.rdf4j.spring.operationcache.CachingOperationInstantiator CachingOperationInstantiator} to generate
 * new SPARQL operations instead of the default implementation.
 */
package org.eclipse.rdf4j.spring.operationcache;
