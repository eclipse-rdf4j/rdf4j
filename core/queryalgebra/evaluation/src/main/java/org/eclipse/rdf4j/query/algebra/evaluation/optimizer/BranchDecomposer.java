/*******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ******************************************************************************/

package org.eclipse.rdf4j.query.algebra.evaluation.optimizer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.rdf4j.query.algebra.Extension;
import org.eclipse.rdf4j.query.algebra.ExtensionElem;
import org.eclipse.rdf4j.query.algebra.Filter;
import org.eclipse.rdf4j.query.algebra.Join;
import org.eclipse.rdf4j.query.algebra.SingletonSet;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.Union;
import org.eclipse.rdf4j.query.algebra.helpers.collectors.VarNameCollector;

/** Flattens a branch (Join/Filter/Extension/StatementPattern) into ordered parts. */
public final class BranchDecomposer {

	public static final class Parts {
		public final List<StatementPattern> triples = new ArrayList<>();
		public final List<Filter> filters = new ArrayList<>(); // inner-first order
		public final List<Extension> extensions = new ArrayList<>(); // inner-first order

		public Set<String> tripleVars() {
			Set<String> vs = new HashSet<>();
			for (StatementPattern sp : triples) {
				vs.addAll(VarNameCollector.process(sp));
			}
			return vs;
		}
	}

	private BranchDecomposer() {
	}

	public static Parts decompose(TupleExpr e) {
		Parts p = new Parts();
		if (!collect(e, p)) {
			return null;
		}
		return p;
	}

	private static boolean collect(TupleExpr e, Parts p) {
		if (e instanceof Join) {
			Join j = (Join) e;
			return collect(j.getLeftArg(), p) && collect(j.getRightArg(), p);
		} else if (e instanceof Filter) {
			var f = (Filter) e;
			if (!collect(f.getArg(), p)) {
				return false;
			}
			p.filters.add(f);
			return true;
		} else if (e instanceof Extension) {
			var ext = (Extension) e;
			if (!collect(ext.getArg(), p)) {
				return false;
			}
			p.extensions.add(ext);
			return true;
		} else if (e instanceof StatementPattern) {
			var sp = (StatementPattern) e;
			p.triples.add(sp);
			return true;
		} else if (e instanceof SingletonSet) {
			return true;
		} else if (e instanceof Union) {
			return false; // union handled one level up
		}
		// Unknown node type => bail (safe)
		return false;
	}

	public static Set<String> extensionDefinedVars(List<Extension> exts) {
		Set<String> out = new HashSet<>();
		for (Extension e : exts) {
			for (ExtensionElem ee : e.getElements()) {
				out.add(ee.getName());
			}
		}
		return out;
	}
}
