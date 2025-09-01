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

	private final String pathText;

	public IrPathTriple(Var subject, String pathText, Var object, boolean newScope) {
		this(subject, null, pathText, object, null, newScope);
	}

	public IrPathTriple(Var subject, IrNode subjectOverride, String pathText, Var object, IrNode objectOverride,
			boolean newScope) {
		super(subject, subjectOverride, object, objectOverride, newScope);
		this.pathText = pathText;
	}

	public String getPathText() {
		return pathText;
	}

	@Override
	public String getPredicateOrPathText(TupleExprIRRenderer r) {
		return pathText;
	}

	@Override
	public void print(IrPrinter p) {
		p.startLine();
		if (getSubjectOverride() != null) {
			getSubjectOverride().print(p);
		} else {
			p.append(p.convertVarToString(getSubject()));
		}
		// Apply lightweight string-level path simplification at print time for stability/readability
		String simplified = SimplifyPathParensTransform.simplify(pathText);
		p.append(" " + simplified + " ");

		if (getObjectOverride() != null) {
			getObjectOverride().print(p);
		} else {
			p.append(p.convertVarToString(getObject()));
		}

		p.append(" .");
		p.endLine();
	}

}
