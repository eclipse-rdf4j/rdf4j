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
package org.eclipse.rdf4j.queryrender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.queryrender.sparql.TupleExprIRRenderer;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrBGP;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrFilter;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrPathTriple;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrStatementPattern;
import org.eclipse.rdf4j.queryrender.sparql.ir.util.transform.ApplyPathsTransform;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Safety checks for ApplyPathsTransform: user-supplied variables that merely share the parser's {@code _anon_path_*}
 * prefix must not be treated as parser-generated bridge vars.
 */
class ApplyPathsTransformSafetyTest {

	private final ValueFactory vf = SimpleValueFactory.getInstance();
	private final TupleExprIRRenderer renderer = new TupleExprIRRenderer();

	@Test
	void userNamedAnonPathVarIsNotFusedIntoPathChain() {
		Var s = Var.of("s");
		Var midUserVar = Var.of("_anon_path_user");
		Var o = Var.of("o");
		Var p1 = Var.of("p1", vf.createIRI("urn:p1"));
		Var p2 = Var.of("p2", vf.createIRI("urn:p2"));

		IrBGP bgp = new IrBGP(false);
		bgp.add(new IrStatementPattern(s, p1, midUserVar, false));
		bgp.add(new IrStatementPattern(midUserVar, p2, o, false));

		assertThrows(AssertionError.class, () -> ApplyPathsTransform.apply(bgp, renderer));
	}

	@Test
	void userNamedAnonPathPredicateIsNotRewrittenIntoNps() {
		Var s = Var.of("s");
		Var predicateVar = Var.of("_anon_path_user_predicate");
		Var o = Var.of("o");

		IrStatementPattern sp = new IrStatementPattern(s, predicateVar, o, false);
		IrFilter filter = new IrFilter("?" + predicateVar.getName() + " != <urn:block>", false);

		IrBGP bgp = new IrBGP(false);
		bgp.add(sp);
		bgp.add(filter);

		assertThrows(AssertionError.class, () -> ApplyPathsTransform.apply(bgp, renderer));

	}
}
