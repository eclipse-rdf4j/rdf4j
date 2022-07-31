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
package org.eclipse.rdf4j.query.algebra.evaluation.impl;

import static org.assertj.core.api.Assertions.assertThat;

import org.eclipse.rdf4j.query.algebra.EmptySet;
import org.eclipse.rdf4j.query.algebra.Projection;
import org.eclipse.rdf4j.query.algebra.SingletonSet;
import org.eclipse.rdf4j.query.algebra.Union;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryOptimizerTest;
import org.eclipse.rdf4j.query.algebra.evaluation.optimizer.QueryModelNormalizerOptimizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class QueryModelNormalizerTest extends QueryOptimizerTest {

	private QueryModelNormalizerOptimizer subject;

	@BeforeEach
	public void setup() throws Exception {
		subject = getOptimizer();
	}

	@Test
	public void testNormalizeUnionWithEmptyLeft() {
		Projection p = new Projection();
		Union union = new Union();
		SingletonSet s = new SingletonSet();
		union.setLeftArg(new EmptySet());
		union.setRightArg(s);
		p.setArg(union);

		subject.meet(union);

		assertThat(p.getArg()).isEqualTo(s);
	}

	@Test
	public void testNormalizeUnionWithEmptyRight() {
		Projection p = new Projection();
		Union union = new Union();
		SingletonSet s = new SingletonSet();
		union.setRightArg(new EmptySet());
		union.setLeftArg(s);
		p.setArg(union);

		subject.meet(union);

		assertThat(p.getArg()).isEqualTo(s);
	}

	/**
	 * @see <a href="https://github.com/eclipse/rdf4j/issues/1404">GH-1404</a>
	 */
	@Test
	public void testNormalizeUnionWithTwoSingletons() {
		Projection p = new Projection();
		Union union = new Union();
		union.setRightArg(new SingletonSet());
		union.setLeftArg(new SingletonSet());
		p.setArg(union);

		subject.meet(union);

		assertThat(p.getArg()).isEqualTo(union);
	}

	@Override
	public QueryModelNormalizerOptimizer getOptimizer() {
		return new QueryModelNormalizerOptimizer();
	}

}
