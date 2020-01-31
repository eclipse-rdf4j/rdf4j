/**
 * A repository that serves as a proxy client for a remote repository on an Rdf4j Server.
 *
 * Note that this proxy implements a <b>rdf4j-specific extension</b> of the basic SPARQL protocol, and therefore should
 * not be used to communicate with non-Sesame SPARQL endpoints. For such endpoints, use
 * {@link org.eclipse.rdf4j.repository.sparql.SPARQLRepository} instead.
 */
package org.eclipse.rdf4j.repository.http;
