/*******************************************************************************
 Copyright (c) 2018 Eclipse RDF4J contributors.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the Eclipse Distribution License v1.0
 which accompanies this distribution, and is available at
 http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sparqlbuilder.core;

import static org.eclipse.rdf4j.sparqlbuilder.rdf.Rdf.iri;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.sparqlbuilder.constraint.Expression;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPattern;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.TriplePattern;
import org.eclipse.rdf4j.sparqlbuilder.rdf.Iri;

/**
 * A class to with static methods to create SPARQL query elements.
 */
public class SparqlBuilder {

	// prevent instantiation of this class
	private SparqlBuilder() {
	}

	/**
	 * Create a SPARQL variable with a specific alias.
	 *
	 * @param varName the alias of the variable
	 * @return a new SPARQL variable
	 */
	public static Variable var(String varName) {
		return new Variable(varName);
	}

	/**
	 * Create a SPARQL assignment
	 *
	 * @param exp the expression to evaluate
	 * @param var the variable to bind the expression value to
	 * @return an Assignment object
	 */
	public static Assignment as(Assignable exp, Variable var) {
		return new Assignment(exp, var);
	}

	/**
	 * Create a SPARQL Base declaration
	 *
	 * @param iri the base iri
	 * @return a Base object
	 */
	public static Base base(Iri iri) {
		return new Base(iri);
	}

	/**
	 * Create a SPARQL Base declaration
	 *
	 * @param iri the base iri
	 * @return a Base object
	 */
	public static Base base(IRI iri) {
		return new Base(iri);
	}

	/**
	 * Create a SPARQL Prefix declaration
	 *
	 * @param alias the alias of the prefix
	 * @param iri   the iri the alias refers to
	 * @return a Prefix object
	 */
	public static Prefix prefix(String alias, Iri iri) {
		return new Prefix(alias, iri);
	}

	/**
	 * Create a SPARQL Prefix declaration
	 *
	 * @param alias the alias of the prefix
	 * @param iri   the iri the alias refers to
	 * @return a Prefix object
	 */
	public static Prefix prefix(String alias, IRI iri) {
		return new Prefix(alias, iri);
	}

	/**
	 * Create a SPARQL default Prefix declaration
	 *
	 * @param iri the default iri prefix
	 * @return a Prefix object
	 */
	public static Prefix prefix(Iri iri) {
		return prefix("", iri);
	}

	/**
	 * Create a SPARQL default Prefix declaration
	 *
	 * @param iri the default iri prefix as an {@link IRI}.
	 * @return a Prefix object
	 */
	public static Prefix prefix(IRI iri) {
		return prefix(iri(iri));
	}

	/**
	 * Create SPARQL Prefix declaration from the given {@link Namespace}.
	 *
	 * @param namespace the {@link Namespace} to convert to a prefix declaration.
	 * @return a Prefix object.
	 */
	public static Prefix prefix(Namespace namespace) {
		return prefix(namespace.getPrefix(), iri(namespace.getName()));
	}

	/**
	 * Create a SPARQL Prefix clause
	 *
	 * @param prefixes prefix declarations to add to this Prefix clause
	 * @return a new
	 */
	public static PrefixDeclarations prefixes(Prefix... prefixes) {
		return new PrefixDeclarations().addPrefix(prefixes);
	}

	/**
	 * Create a default graph reference
	 *
	 * @param iri the source of the graph
	 * @return a From clause
	 * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#rdfDataset"> RDF Datasets</a>
	 */
	public static From from(Iri iri) {
		return new From(iri);
	}

	/**
	 * Create a default graph reference
	 *
	 * @param iri the source of the graph
	 * @return a From clause
	 * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#rdfDataset"> RDF Datasets</a>
	 */
	public static From from(IRI iri) {
		return new From(iri);
	}

	/**
	 * Create a named graph reference
	 *
	 * @param iri the source of the graph
	 * @return a named From clause
	 * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#rdfDataset"> RDF Datasets</a>
	 */
	public static From fromNamed(Iri iri) {
		return new From(iri, true);
	}

	/**
	 * Create a named graph reference
	 *
	 * @param iri the source of the graph
	 * @return a named From clause
	 * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#rdfDataset"> RDF Datasets</a>
	 */
	public static From fromNamed(IRI iri) {
		return new From(iri, true);
	}

	/**
	 * Create a dataset declaration
	 *
	 * @param graphs
	 * @return a new dataset clause
	 * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#rdfDataset"> RDF Datasets</a>
	 */
	public static Dataset dataset(From... graphs) {
		return new Dataset().from(graphs);
	}

	/**
	 * Create a SPARQL projection
	 *
	 * @param projectables projectable elements to add to the projection
	 * @return a Projection
	 */
	public static Projection select(Projectable... projectables) {
		return new Projection().select(projectables);
	}

	/**
	 * Create a SPARQL graph template
	 *
	 * @param triples triples to add to the template
	 * @return a new SPARQL graph template
	 */
	public static GraphTemplate construct(TriplePattern... triples) {
		return new GraphTemplate().construct(triples);
	}

	/**
	 * Create a SPARQL query pattern
	 *
	 * @param patterns graph patterns to add to the query pattern
	 * @return a new Query Pattern
	 */
	public static QueryPattern where(GraphPattern... patterns) {
		return new QueryPattern().where(patterns);
	}

	/**
	 * Create a SPARQL Group By clause
	 *
	 * @param groupables the group conditions
	 * @return a Group By clause
	 */
	public static GroupBy groupBy(Groupable... groupables) {
		return new GroupBy().by(groupables);
	}

	/**
	 * Create a SPARQL Order clause
	 *
	 * @param conditions the order conditions
	 * @return an Order By clause
	 */
	public static OrderBy orderBy(Orderable... conditions) {
		return new OrderBy().by(conditions);
	}

	/**
	 * Create a SPARQL Having clause
	 *
	 * @param expressions the having conditions
	 * @return a Having clause
	 */
	public static Having having(Expression<?>... expressions) {
		return new Having().having(expressions);
	}

	/**
	 * Create an ascending SPARQL order condition
	 *
	 * @param orderOn the order comparator
	 * @return an ASC() order condition
	 */
	public static OrderCondition asc(Orderable orderOn) {
		return new OrderCondition(orderOn, true);
	}

	/**
	 * Create a descending SPARQL order condition
	 *
	 * @param orderOn the order comparator
	 * @return a DESC() order condition
	 */
	public static OrderCondition desc(Orderable orderOn) {
		return new OrderCondition(orderOn, false);
	}

	/**
	 * Create a TriplesTemplate instance, for use with Construct and Update queries
	 *
	 * @param triples the triples to include in the triples template
	 * @return a TriplesTemplate of the given triples
	 */
	public static TriplesTemplate triplesTemplate(TriplePattern... triples) {
		return new TriplesTemplate(triples);
	}
}
