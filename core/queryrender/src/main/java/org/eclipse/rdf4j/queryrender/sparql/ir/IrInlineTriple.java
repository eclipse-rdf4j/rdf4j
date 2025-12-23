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

/**
 * Inline RDF-star triple term: renders as &lt;&lt; subj pred obj &gt;&gt; inside another triple.
 */
public final class IrInlineTriple extends IrNode {
	private final Var subject;
	private final Var predicate;
	private final Var object;

	public IrInlineTriple(Var subject, Var predicate, Var object) {
		super(false);
		this.subject = subject;
		this.predicate = predicate;
		this.object = object;
	}

	@Override
	public void print(IrPrinter p) {
		p.append("<<");
		p.append(" " + p.convertVarToString(subject));
		p.append(" " + predicateText(p));
		p.append(" " + p.convertVarToString(object) + " >>");
	}

	private String predicateText(IrPrinter p) {
		if (predicate != null && predicate.hasValue() && predicate.getValue() instanceof IRI
				&& RDF.TYPE.equals(predicate.getValue())) {
			return "a";
		}
		return p.convertVarToString(predicate);
	}

	@Override
	public Set<Var> getVars() {
		HashSet<Var> out = new HashSet<>();
		if (subject != null) {
			out.add(subject);
		}
		if (predicate != null) {
			out.add(predicate);
		}
		if (object != null) {
			out.add(object);
		}
		return out;
	}
}
