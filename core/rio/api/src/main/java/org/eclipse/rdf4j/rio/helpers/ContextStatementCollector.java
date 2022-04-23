/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.rio.helpers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.eclipse.rdf4j.model.NamespaceAware;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.util.Namespaces;
import org.eclipse.rdf4j.rio.RDFHandlerException;

/**
 * A RDFHandler that can be used to collect reported statements in collections.
 *
 * @author Arjohn Kampman
 */
public class ContextStatementCollector extends AbstractRDFHandler {

	/*-----------*
	 * Variables *
	 *-----------*/

	private final Collection<Statement> statements;

	private final Map<String, String> namespaces;

	private final Resource[] contexts;

	private final ValueFactory vf;

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * Creates a new StatementCollector that uses a new ArrayList to store the reported statements and a new
	 * LinkedHashMap to store the reported namespaces.
	 */
	public ContextStatementCollector(ValueFactory vf, Resource... contexts) {
		this(new ArrayList<>(), vf, contexts);
	}

	/**
	 * Creates a new StatementCollector that stores reported statements in the supplied collection and that uses a new
	 * LinkedHashMap to store the reported namespaces.
	 */
	public ContextStatementCollector(Collection<Statement> statements, ValueFactory vf, Resource... contexts) {
		Objects.requireNonNull(contexts,
				"contexts argument may not be null; either the value should be cast to Resource or an empty array should be supplied");

		if (statements instanceof NamespaceAware) {
			this.namespaces = Namespaces.wrap(((NamespaceAware) statements).getNamespaces());
		} else {
			this.namespaces = new LinkedHashMap<>();
		}
		this.statements = statements;
		this.vf = vf;
		this.contexts = contexts;
	}

	/**
	 * Creates a new StatementCollector that stores reported statements and namespaces in the supplied containers.
	 */
	public ContextStatementCollector(Collection<Statement> statements, Map<String, String> namespaces, ValueFactory vf,
			Resource... contexts) {
		Objects.requireNonNull(contexts,
				"contexts argument may not be null; either the value should be cast to Resource or an empty array should be supplied");
		this.statements = statements;
		this.namespaces = namespaces;
		this.vf = vf;
		this.contexts = contexts;
	}

	/*---------*
	 * Methods *
	 *---------*/

	/**
	 * Clear the set of collected statements.
	 */
	public void clear() {
		statements.clear();
	}

	/**
	 * Gets the collection that contains the collected statements.
	 */
	public Collection<Statement> getStatements() {
		return statements;
	}

	/**
	 * Gets the map that contains the collected namespaces.
	 */
	public Map<String, String> getNamespaces() {
		return namespaces;
	}

	@Override
	public void handleNamespace(String prefix, String uri) throws RDFHandlerException {
		if (!namespaces.containsKey(prefix)) {
			namespaces.put(prefix, uri);
		}
	}

	@Override
	public void handleStatement(Statement st) {
		if (contexts.length == 0) {
			statements.add(st);
		} else {
			for (Resource nextContext : contexts) {
				statements.add(vf.createStatement(st.getSubject(), st.getPredicate(), st.getObject(), nextContext));
			}
		}
	}
}
