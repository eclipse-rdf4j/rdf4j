/**
 *
 *
 * <H1>Rdf4j-Spring Pool</H1>
 *
 * Provides pooling of {@link org.eclipse.rdf4j.repository.RepositoryConnection RepositoryConnection}s.
 *
 * <p>
 * Enable via <code>rdf4j.spring.pool.enabled=true</code>.
 *
 * <p>
 * If enabled, the {@link org.eclipse.rdf4j.spring.Rdf4JConfig Rdf4JConfig} will wrap its
 * {@link org.eclipse.rdf4j.spring.support.connectionfactory.RepositoryConnectionFactory RepositoryConnectionFactory} in
 * a {@link org.eclipse.rdf4j.spring.pool.PooledRepositoryConnectionFactory PooledRepositoryConnectionFactory}.
 *
 * <p>
 * For more information on configuration of the pool, see {@link org.eclipse.rdf4j.spring.pool.PoolProperties
 * PoolProperties}.
 */
package org.eclipse.rdf4j.spring.pool;
