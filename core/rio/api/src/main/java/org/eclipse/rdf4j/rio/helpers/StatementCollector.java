/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.rio.helpers;

import java.util.Collection;
import java.util.Map;

import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

/**
 * A RDFHandler that can be used to collect reported statements in collections.
 *
 * @author Arjohn Kampman
 */
public class StatementCollector extends ContextStatementCollector {

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * Creates a new StatementCollector that uses a new ArrayList to store the reported statements and a new
	 * LinkedHashMap to store the reported namespaces.
	 */
	public StatementCollector() {
		super(SimpleValueFactory.getInstance());
	}

	/**
	 * Creates a new StatementCollector that stores reported statements in the supplied collection and that uses a new
	 * LinkedHashMap to store the reported namespaces.
	 */
	public StatementCollector(Collection<Statement> statements) {
		super(statements, SimpleValueFactory.getInstance());
	}

	/**
	 * Creates a new StatementCollector that stores reported statements and namespaces in the supplied containers.
	 */
	public StatementCollector(Collection<Statement> statements, Map<String, String> namespaces) {
		super(statements, namespaces, SimpleValueFactory.getInstance());
	}
}
