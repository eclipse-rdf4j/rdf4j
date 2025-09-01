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
import org.eclipse.rdf4j.queryrender.sparql.TupleExprIRRenderer;

/**
 * Common abstraction for triple-like IR nodes that have subject/object variables and a textual predicate/path
 * representation suitable for alternation merging.
 */
public abstract class IrTripleLike extends IrNode {

	final Var subject;
	IrNode subjectOverride;
	final Var object;
	IrNode objectOverride;

	public IrTripleLike(Var subject, Var object, boolean newScope) {
		super(newScope);
		this.subject = subject;
		this.object = object;
	}

	public IrTripleLike(Var subject, IrNode subjectOverride, Var object, IrNode objectOverride, boolean newScope) {
		super(newScope);
		this.subjectOverride = subjectOverride;
		this.subject = subject;
		this.object = object;
		this.objectOverride = objectOverride;
	}

	public Var getSubject() {
		return subject;
	}

	public Var getObject() {
		return object;
	}

	public IrNode getSubjectOverride() {
		return subjectOverride;
	}

	public void setSubjectOverride(IrNode subjectOverride) {
		this.subjectOverride = subjectOverride;
	}

	public IrNode getObjectOverride() {
		return objectOverride;
	}

	public void setObjectOverride(IrNode objectOverride) {
		this.objectOverride = objectOverride;
	}

	/**
	 * Render the predicate or path as compact textual IR suitable for inclusion in a property path.
	 *
	 * For simple statement patterns this typically returns a compact IRI (possibly prefixed); for path triples it
	 * returns the already-rendered path text.
	 *
	 * Implementations should return null when no safe textual representation exists (e.g., non-constant predicate in a
	 * statement pattern).
	 */
	public abstract String getPredicateOrPathText(TupleExprIRRenderer r);
}
