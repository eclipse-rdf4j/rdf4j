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

import java.util.HashSet;
import java.util.Set;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.queryrender.sparql.TupleExprIRRenderer;

/**
 * Textual IR node for a simple triple pattern line.
 */
public class IrStatementPattern extends IrTripleLike {

	private final Var predicate;

	public IrStatementPattern(Var subject, Var predicate, Var object, boolean newScope) {
		super(subject, object, newScope);
		this.predicate = predicate;
	}

	public Var getPredicate() {
		return predicate;
	}

	@Override
	public String getPredicateOrPathText(TupleExprIRRenderer r) {
		Var pv = getPredicate();
		if (isRdfTypePredicate(pv)) {
			return "a";
		}
		return r.convertVarIriToString(pv);
	}

	@Override
	public void print(IrPrinter p) {
		p.startLine();
		if (getSubjectOverride() != null) {
			getSubjectOverride().print(p);
		} else {
			p.append(p.convertVarToString(getSubject()));
		}
		final String predText = isRdfTypePredicate(getPredicate()) ? "a" : p.convertVarToString(getPredicate());
		p.append(" " + predText + " ");

		if (getObjectOverride() != null) {
			getObjectOverride().print(p);
		} else {
			p.append(p.convertVarToString(getObject()));
		}
		p.append(" .");
		p.endLine();
	}

	@Override
	public String toString() {
		return "IrStatementPattern{" +
				"subject=" + subject +
				", subjectOverride=" + subjectOverride +
				", predicate=" + predicate +
				", object=" + object +
				", objectOverride=" + objectOverride +
				'}';
	}

	@Override
	public Set<Var> getVars() {
		HashSet<Var> out = new HashSet<>(super.getVars());
		if (predicate != null) {
			out.add(predicate);
		}
		return out;
	}

	private static boolean isRdfTypePredicate(Var v) {
		return v != null && v.hasValue() && v.getValue() instanceof IRI && RDF.TYPE.equals(v.getValue());
	}
}
