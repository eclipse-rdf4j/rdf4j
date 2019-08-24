/**
 * The Repository API: the main API for accessing rdf databases and SPARQL endpoints.
 * 
 * The class {@link org.eclipse.rdf4j.repository.Repository} is the main interface for rdf4j repositories. It provides
 * all sorts of operations for manipulating RDF in various ways, through a
 * {@link org.eclipse.rdf4j.repository.RepositoryConnection}.
 * 
 * An important notion in a rdf4j repository is that of <strong>context</strong> . Within one repository, subsets of
 * statements can be identified by their context.
 *
 * @see <a href="https://rdf4j.eclipse.org/documentation/programming/repository/">rdf4j repository API documentation</a>
 */
package org.eclipse.rdf4j.repository;