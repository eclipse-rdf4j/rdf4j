package org.eclipse.rdf4j.spanqit.rdf;

import org.eclipse.rdf4j.spanqit.constraint.SparqlOperator;
import org.eclipse.rdf4j.spanqit.graphpattern.GraphName;

/**
 * Denotes an RDF IRI
 * 
 * @see <a
 *      href="http://www.w3.org/TR/2014/NOTE-rdf11-primer-20140624/#section-IRI">
 *      RDF IRIs</a>
 */
public interface Iri extends RdfResource, RdfPredicate, GraphName, SparqlOperator { }