/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.impl;

import static org.assertj.core.api.Assertions.assertThat;

import org.eclipse.rdf4j.query.algebra.EmptySet;
import org.eclipse.rdf4j.query.algebra.Projection;
import org.eclipse.rdf4j.query.algebra.SingletonSet;
import org.eclipse.rdf4j.query.algebra.Union;
import org.junit.BeforeClass;
import org.junit.Test;

public class QueryModelNormalizerTest {

	private static QueryModelNormalizer subject;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		subject = new QueryModelNormalizer();
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
	 * @see https://github.com/eclipse/rdf4j/issues/1404
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

}
