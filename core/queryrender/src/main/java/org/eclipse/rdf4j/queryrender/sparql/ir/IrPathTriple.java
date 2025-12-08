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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.queryrender.sparql.TupleExprIRRenderer;
import org.eclipse.rdf4j.queryrender.sparql.ir.util.transform.SimplifyPathParensTransform;
import org.eclipse.rdf4j.queryrender.sparql.util.VarUtils;

/**
 * Textual IR node for a property path triple: subject, path expression, object.
 *
 * Path expression is stored as pre-rendered text to allow local string-level rewrites (alternation/sequence grouping,
 * quantifiers) without needing a full AST here. Transforms are responsible for ensuring parentheses are added only when
 * required for correctness; printing strips redundant outermost parentheses for stable output.
 */
public class IrPathTriple extends IrTripleLike {

	private final String pathText;
	private Set<Var> pathVars; // vars that were part of the path before fusing (e.g., anon bridge vars)

	public IrPathTriple(Var subject, String pathText, Var object, boolean newScope, Set<Var> pathVars) {
		this(subject, null, pathText, object, null, pathVars, newScope);
	}

	public IrPathTriple(Var subject, IrNode subjectOverride, String pathText, Var object, IrNode objectOverride,
			Set<Var> pathVars, boolean newScope) {
		super(subject, subjectOverride, object, objectOverride, newScope);
		this.pathText = pathText;
		this.pathVars = Set.copyOf(pathVars);
	}

	public String getPathText() {
		return pathText;
	}

	@Override
	public String getPredicateOrPathText(TupleExprIRRenderer r) {
		return pathText;
	}

	/** Returns the set of variables that contributed to this path during fusing (e.g., anon _anon_path_* bridges). */
	public Set<Var> getPathVars() {
		return pathVars;
	}

	/** Assign the set of variables that contributed to this path during fusing. */
	public void setPathVars(Set<Var> vars) {
		if (vars.isEmpty()) {
			this.pathVars = Collections.emptySet();
		} else {
			this.pathVars = Set.copyOf(vars);
		}
	}

	/** Merge pathVars from 2+ IrPathTriples into a new unmodifiable set. */
	public static Set<Var> mergePathVars(IrPathTriple... pts) {
		if (pts == null || pts.length == 0) {
			return Collections.emptySet();
		}
		HashSet<Var> out = new HashSet<>();
		for (IrPathTriple pt : pts) {
			if (pt == null) {
				continue;
			}
			if (pt.getPathVars() != null) {
				out.addAll(pt.getPathVars());
			}
		}
		return out.isEmpty() ? Collections.emptySet() : Collections.unmodifiableSet(out);
	}

	/**
	 * Create a set of pathVars from one or more IrStatementPattern by collecting any parser bridge variables
	 * (subject/object with names starting with _anon_path_ or _anon_path_inverse_) and anonymous predicate vars.
	 */
	public static Set<Var> fromStatementPatterns(IrStatementPattern... sps) {
		if (sps == null || sps.length == 0) {
			return Collections.emptySet();
		}
		HashSet<Var> out = new HashSet<>();
		for (IrStatementPattern sp : sps) {
			if (sp == null) {
				continue;
			}
			Var s = sp.getSubject();
			Var o = sp.getObject();
			Var p = sp.getPredicate();
			if (isAnonBridgeVar(s)) {
				out.add(s);
			}
			if (isAnonBridgeVar(o)) {
				out.add(o);
			}
			if (isAnonBridgeVar(p)) {
				out.add(p);
			}
		}
		return out.isEmpty() ? Collections.emptySet() : Collections.unmodifiableSet(out);
	}

	private static boolean isAnonBridgeVar(Var v) {
		return VarUtils.isAnonPathVar(v) || VarUtils.isAnonPathInverseVar(v);
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

	@Override
	public String toString() {
		return "IrPathTriple{" +
				"pathText='" + pathText + '\'' +
				", pathVars=" + Arrays.toString(pathVars.toArray()) +
				", subject=" + subject +
				", subjectOverride=" + subjectOverride +
				", object=" + object +
				", objectOverride=" + objectOverride +
				'}';
	}

	@Override
	public Set<Var> getVars() {
		HashSet<Var> out = new HashSet<>(super.getVars());
		if (pathVars != null) {
			out.addAll(pathVars);
		}
		return out;
	}
}
