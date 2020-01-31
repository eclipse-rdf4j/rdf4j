/**
 * A {@link org.eclipse.rdf4j.repository.Repository} that serves as a SPARQL endpoint client.
 * <p>
 * A SPARQL endpoint is any web service that implements the <a href="http://www.w3.org/TR/sparql11-protocol/">SPARQL 1.1
 * Protocol</a> - a means of conveying SPARQL queries and updates to SPARQL processors.
 * </p>
 * <p>
 * Since every rdf4j repository running on a Rdf4j Server is also a SPARQL endpoint, it is possible to use the
 * SPARQLRepository to access such a repository. However, it is recommended to instead use
 * {@link org.eclipse.rdf4j.repository.http.HTTPRepository}, which has a number of rdf4j-specific optimizations that
 * make client-server communication more scalable, and transaction-safe.
 * </p>
 */
package org.eclipse.rdf4j.repository.sparql;