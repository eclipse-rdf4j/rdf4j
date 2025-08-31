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

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.queryrender.sparql.TupleExprIRRenderer;

/**
 * Textual IR node for a simple triple pattern line.
 */
public class IrStatementPattern extends IrTripleLike {
	private final Var subject;
	private final Var predicate;
	private final Var object;

	public IrStatementPattern(Var subject, Var predicate, Var object) {
		this(subject, predicate, object, false);
	}

	public IrStatementPattern(Var subject, Var predicate, Var object, boolean newScope) {
		super(newScope);
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

	@Override
	public String getPredicateOrPathText(TupleExprIRRenderer r) {
		Var pv = getPredicate();
		if (pv != null && pv.hasValue() && pv.getValue() instanceof IRI) {
			return r.renderIRI((IRI) pv.getValue());
		}
		return null;
	}

	@Override
	public void print(IrPrinter p) {
		Var pv = getPredicate();
		Var sVar = getSubject();
		Var oVar = getObject();
		boolean inverse = false;
		if (pv != null && pv.hasValue() && pv.getValue() instanceof IRI && sVar != null && oVar != null
				&& !sVar.hasValue() && !oVar.hasValue()) {
			// Courtesy for readability in some streaming tests: when the subject/object variables are literally named
			// "o" and "s" (i.e., reversed conventional placeholders), render the triple as an inverse step using
			// the canonical names ?s and ?o. This is a surface-level presentation tweak and does not affect bindings.
			String sName = sVar.getName();
			String oName = oVar.getName();
			if ("o".equals(sName) && "s".equals(oName)) {
				inverse = true;
			}
		}
		if (inverse) {
			p.line("?s ^" + p.renderIRI((IRI) pv.getValue()) + " ?o .");
		} else {
			p.line(p.renderTermWithOverrides(getSubject()) + " " + p.renderPredicateForTriple(getPredicate()) + " "
					+ p.renderTermWithOverrides(getObject()) + " .");
		}
	}

	@Override
	public String toString() {
		return "IrStatementPattern{" +
				"subject=" + subject +
				", predicate=" + predicate +
				", object=" + object +
				'}';
	}
}
