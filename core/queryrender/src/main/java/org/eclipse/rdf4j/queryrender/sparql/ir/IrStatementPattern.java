/*******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.queryrender.sparql.ir;

import org.eclipse.rdf4j.query.algebra.Var;

/**
 * Textual IR node for a simple triple pattern line.
 */
public class IrStatementPattern extends IrNode {
	private final Var subject;
	private final Var predicate;
	private final Var object;

	public IrStatementPattern(Var subject, Var predicate, Var object) {
		this.subject = subject;
		this.predicate = predicate;
		this.object = object;
	}

	public Var getSubject() {
		return subject;
	}

	public Var getPredicate() {
		return predicate;
	}

	public Var getObject() {
		return object;
	}
}
