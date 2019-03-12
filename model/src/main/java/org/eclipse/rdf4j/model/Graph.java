/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.model;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;

import org.eclipse.rdf4j.OpenRDFUtil;
import org.eclipse.rdf4j.model.util.GraphUtil;

/**
 * An RDF graph, represented as a collection of {@link Statement}s.
 * 
 * @see GraphUtil
 * @author Arjohn Kampman
 * @deprecated Use {@link Model} instead.
 */
@Deprecated
public interface Graph extends Collection<Statement>, Serializable {

	/**
	 * Gets the value factory for this graph.
	 * 
	 * @deprecated {@link org.eclipse.rdf4j.model.impl.SimpleValueFactory#getInstance} will obtain a default value
	 *             factory implementation. If you are working with the Repository API, then <tt>Repository</tt>
	 *             instances also supply a <tt>getValueFactory()</tt> method.
	 */
	@Deprecated
	public ValueFactory getValueFactory();

	/**
	 * Adds one or more statements to the graph. This method creates a statement for each specified context and adds
	 * those to the graph. If no contexts are specified, a single statement with no associated context is added.
	 * 
	 * @param subj     The statement's subject, must not be <tt>null</tt>.
	 * @param pred     The statement's predicate, must not be <tt>null</tt>.
	 * @param obj      The statement's object, must not be <tt>null</tt>.
	 * @param contexts The contexts to add statements to.
	 */
	public boolean add(Resource subj, IRI pred, Value obj, Resource... contexts);

	/**
	 * Gets the statements with the specified subject, predicate, object and (optionally) context. The <tt>subject</tt>,
	 * <tt>predicate</tt> and <tt>object</tt> parameters can be <tt>null</tt> to indicate wildcards. The
	 * <tt>contexts</tt> parameter is a wildcard and accepts zero or more values. If no contexts are specified,
	 * statements will match disregarding their context. If one or more contexts are specified, statements with a
	 * context matching one of these will match. Note: to match statements without an associated context, specify the
	 * value <tt>null</tt> and explicitly cast it to type <tt>Resource</tt>.
	 * <p>
	 * Examples: <tt>graph.match(s1, null, null)</tt> matches all statements that have subject <tt>s1</tt>, <br>
	 * <tt>graph.match(null, null, null, c1)</tt> matches all statements that have context <tt>c1</tt>,<br>
	 * <tt>graph.match(null, null, null, (Resource)null)</tt> matches all statements that have no associated
	 * context,<br>
	 * <tt>graph.match(null, null, null, c1, c2, c3)</tt> matches all statements that have context <tt>c1</tt> ,
	 * <tt>c2</tt> or <tt>c3</tt>.
	 * 
	 * @deprecated The preferred alternative is {@link org.eclipse.rdf4j.model.Model#filter}.iterator().
	 * @param subj     The subject of the statements to match, <tt>null</tt> to match statements with any subject.
	 * @param pred     The predicate of the statements to match, <tt>null</tt> to match statements with any predicate.
	 * @param obj      The object of the statements to match, <tt>null</tt> to match statements with any object.
	 * @param contexts The contexts of the statements to match. If no contexts are specified, statements will match
	 *                 disregarding their context. If one or more contexts are specified, statements with a context
	 *                 matching one of these will match.
	 * @return The statements that match the specified pattern.
	 * @throws IllegalArgumentException If a <tt>null</tt>-array is specified as the value for <tt>contexts</tt>. See
	 *                                  {@link OpenRDFUtil#verifyContextNotNull(Resource[])} for more info.
	 */
	@Deprecated
	public Iterator<Statement> match(Resource subj, IRI pred, Value obj, Resource... contexts);
}
