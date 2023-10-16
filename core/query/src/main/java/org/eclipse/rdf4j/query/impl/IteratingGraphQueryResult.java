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
package org.eclipse.rdf4j.query.impl;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.CloseableIteratorIteration;
import org.eclipse.rdf4j.common.iteration.IterationWrapper;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.query.GraphQueryResult;

/**
 * An iterating implementation of the {@link GraphQueryResult} interface.
 *
 * @author Arjohn Kampman
 * @author Jeen Broekstra
 */
public class IteratingGraphQueryResult extends IterationWrapper<Statement>
		implements GraphQueryResult {

	/*-----------*
	 * Variables *
	 *-----------*/

	private final Map<String, String> namespaces;

	/*--------------*
	 * Constructors *
	 *--------------*/

	public IteratingGraphQueryResult(Map<String, String> namespaces, Iterable<? extends Statement> statements) {
		this(namespaces, statements.iterator());
	}

	public IteratingGraphQueryResult(Map<String, String> namespaces, Iterator<? extends Statement> statementIter) {
		this(namespaces, new CloseableIteratorIteration<Statement>(statementIter));
	}

	public IteratingGraphQueryResult(Map<String, String> namespaces,
			CloseableIteration<? extends Statement> statementIter) {
		super(statementIter);
		this.namespaces = Collections.unmodifiableMap(namespaces);
	}

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	public Map<String, String> getNamespaces() {
		return namespaces;
	}
}
