/**
 *
 *
 * <H1>Rdf4j-Spring OperationLog</H1>
 *
 * Provides query/update-level logging and timing for SPARQL operations.
 *
 * <ul>
 * <li>Use the property <code>rdf4j.spring.operationlog.enabled=true</code> to enable, in which case each query is
 * logged through slf4j.
 * <li>Use the property <code>rdf4j.spring.operationlog.jmx.enabled=true</code> to replace slf4j logging by logging
 * using a JMX MXBean, <code>
 *     org.eclipse.rdf4j.operationlog.OperationStats</code>
 * </ul>
 *
 * <p>
 * If enabled, bean of type {@link org.eclipse.rdf4j.spring.operationlog.log.OperationLog OperationLog} is instantiated
 * that can be used to create a {@link org.eclipse.rdf4j.spring.operationlog.LoggingRepositoryConnectionFactory
 * LoggingRepositoryConnectionFactory}, wrapping the
 * {@link org.eclipse.rdf4j.spring.support.connectionfactory.RepositoryConnectionFactory RepositoryConnectionFactory}
 * used by the application. This is done using spring-autoconfiguration by {@link org.eclipse.rdf4j.spring.Rdf4JConfig
 * Rdf4JConfig}.
 */
package org.eclipse.rdf4j.spring.operationlog;
