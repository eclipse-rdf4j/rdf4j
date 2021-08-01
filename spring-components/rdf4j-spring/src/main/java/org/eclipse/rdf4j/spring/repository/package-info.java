/**
 *
 *
 * <H1>Rdf4j-Spring Repository</H1>
 *
 * Automatically configures {@link org.eclipse.rdf4j.repository.Repository Repository} beans via
 * {@link org.eclipse.rdf4j.spring.Rdf4JConfig Rdf4JConfig}.
 *
 * <p>
 * To configure a remote repostitory, use
 *
 * <ul>
 * <li><code>rdf4j.spring.repository.remote.enabled=true</code>
 * <li><code>rdf4j.spring.repository.remote.manager-url=[manager-url]</code>
 * <li><code>rdf4j.spring.repository.remote.name=[name]</code>
 * </ul>
 *
 * (see {@link org.eclipse.rdf4j.spring.repository.remote.RemoteRepositoryProperties RemoteRepositoryProperties})
 *
 * <p>
 * To configure an in-memory Repository use <code>rdf4j.spring.repository.inmemory.enabled=true
 * </code> (see {@link org.eclipse.rdf4j.spring.repository.inmemory.InMemoryRepositoryProperties
 * InMemoryRepositoryProperties})
 *
 * <p>
 * <b>Note: Exactly one repository has to be configured.</b>
 */
package org.eclipse.rdf4j.spring.repository;
