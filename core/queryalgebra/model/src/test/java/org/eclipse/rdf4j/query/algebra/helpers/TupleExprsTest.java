/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.helpers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.rdf4j.query.algebra.helpers.TupleExprs.isFilterExistsFunction;

import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.algebra.Compare;
import org.eclipse.rdf4j.query.algebra.Exists;
import org.eclipse.rdf4j.query.algebra.Filter;
import org.eclipse.rdf4j.query.algebra.Not;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.Var;
import org.junit.Test;

public class TupleExprsTest {

	private final ValueFactory f = SimpleValueFactory.getInstance();

	@Test
	public void isFilterExistsFunctionOnEmptyFilter() {
		TupleExpr expr = new Filter();

		assertThat(isFilterExistsFunction(expr)).isFalse();
	}

	@Test
	public void isFilterExistsFunctionOnNormalFilter() {
		Filter expr = new Filter();
		expr.setArg(new StatementPattern());
		expr.setCondition(new Compare(new Var("x", f.createBNode()), new Var("y", f.createBNode())));

		assertThat(isFilterExistsFunction(expr)).isFalse();
	}

	@Test
	public void isFilterExistsFunctionOnNormalNot() {
		Filter expr = new Filter();
		expr.setArg(new StatementPattern());
		expr.setCondition(new Not(new Compare(new Var("x", f.createBNode()), new Var("y", f.createBNode()))));

		assertThat(isFilterExistsFunction(expr)).isFalse();
	}

	@Test
	public void isFilterExistsFunctionOnExists() {
		Filter expr = new Filter();
		expr.setArg(new StatementPattern());
		expr.setCondition(new Exists(new StatementPattern()));

		assertThat(isFilterExistsFunction(expr)).isTrue();

	}

	@Test
	public void isFilterExistsFunctionOnNotExist() {
		Filter expr = new Filter();
		expr.setArg(new StatementPattern());
		expr.setCondition(new Not(new Exists(new StatementPattern())));

		assertThat(isFilterExistsFunction(expr)).isTrue();
	}

}
