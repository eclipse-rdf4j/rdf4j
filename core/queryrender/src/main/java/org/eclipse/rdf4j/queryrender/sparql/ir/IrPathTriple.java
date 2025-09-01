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
import org.eclipse.rdf4j.queryrender.sparql.ir.util.transform.SimplifyPathParensTransform;

/**
 * Textual IR node for a property path triple: subject, path expression, object.
 *
 * Path expression is stored as pre-rendered text to allow local string-level rewrites (alternation/sequence grouping,
 * quantifiers) without needing a full AST here. Transforms are responsible for ensuring parentheses are added only when
 * required for correctness; printing strips redundant outermost parentheses for stable output.
 */
public class IrPathTriple extends IrTripleLike {
	private final Var subject;
	private final String pathText;
	private final Var object;

	public IrPathTriple(Var subject, String pathText, Var object, boolean newScope) {
		super(newScope);
		this.subject = subject;
		this.pathText = pathText;
		this.object = object;
	}

	public Var getSubject() {
		return subject;
	}

	public String getPathText() {
		return pathText;
	}

	public Var getObject() {
		return object;
	}

	@Override
	public String getPredicateOrPathText(TupleExprIRRenderer r) {
		return pathText;
	}

	@Override
	public void print(IrPrinter p) {
		final String sTxt = p.renderTermWithOverrides(subject);
		final String oTxt = p.renderTermWithOverrides(object);
		final String path = p.applyOverridesToText(pathText);
		String normalized = SimplifyPathParensTransform.simplify(path);
		// Final local normalization: convert !a|!^b into !(a|^b) for readability
		if (normalized != null) {
			String t = normalized.trim();
			if (t.indexOf('|') >= 0 && t.indexOf('(') < 0 && t.indexOf(')') < 0) {
				String[] segs = t.split("\\|");
				boolean allNeg = segs.length > 1;
				java.util.ArrayList<String> members = new java.util.ArrayList<>();
				for (String seg : segs) {
					String u = seg.trim();
					if (!u.startsWith("!")) {
						allNeg = false;
						break;
					}
					u = u.substring(1).trim();
					if (u.isEmpty()) {
						allNeg = false;
						break;
					}
					members.add(u);
				}
				if (allNeg) {
					normalized = "!(" + String.join("|", members) + ")";
				}
			}
		}
		final String trimmed = TupleExprIRRenderer.stripRedundantOuterParens(normalized);
		p.line(sTxt + " " + trimmed + " " + oTxt + " .");
	}
}
