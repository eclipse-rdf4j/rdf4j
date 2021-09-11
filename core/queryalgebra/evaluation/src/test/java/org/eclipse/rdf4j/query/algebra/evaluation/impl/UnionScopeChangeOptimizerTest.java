/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.impl;

import static org.assertj.core.api.Assertions.assertThat;

import org.eclipse.rdf4j.query.algebra.BindingSetAssignment;
import org.eclipse.rdf4j.query.algebra.Extension;
import org.eclipse.rdf4j.query.algebra.SingletonSet;
import org.eclipse.rdf4j.query.algebra.Union;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryOptimizerTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class UnionScopeChangeOptimizerTest extends QueryOptimizerTest {

	private UnionScopeChangeOptimizer subject;
	private Union union;

	@BeforeEach
	public void setup() throws Exception {
		subject = getOptimizer();
		union = new Union();
		union.setVariableScopeChange(true);

	}

	@Test
	public void fixesScopeChange() {
		union.setRightArg(new SingletonSet());
		union.setLeftArg(new SingletonSet());

		subject.optimize(union, null, null);
		assertThat(union.isVariableScopeChange()).isFalse();
	}

	@Test
	public void keepsScopeChangeOnBindClauseArg() {
		{
			union.setLeftArg(new Extension(new SingletonSet()));
			union.setRightArg(new SingletonSet());

			subject.optimize(union, null, null);
			assertThat(union.isVariableScopeChange()).isTrue();
		}

		{
			union.setLeftArg(new SingletonSet());
			union.setRightArg(new Extension(new SingletonSet()));

			subject.optimize(union, null, null);
			assertThat(union.isVariableScopeChange()).isTrue();
		}
	}

	@Test
	public void keepsScopeChangeOnValuesClauseArg() {
		{
			union.setLeftArg(new BindingSetAssignment());
			union.setRightArg(new SingletonSet());

			subject.optimize(union, null, null);
			assertThat(union.isVariableScopeChange()).isTrue();
		}

		{
			union.setLeftArg(new SingletonSet());
			union.setRightArg(new BindingSetAssignment());

			subject.optimize(union, null, null);
			assertThat(union.isVariableScopeChange()).isTrue();
		}
	}

	@Override
	public UnionScopeChangeOptimizer getOptimizer() {
		return new UnionScopeChangeOptimizer();
	}

}
