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
package org.eclipse.rdf4j.query.algebra.evaluation.impl.evaluationsteps;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.CloseableIteratorIteration;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.MutableBindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.evaluation.TripleSource;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.QueryEvaluationContext;
import org.junit.jupiter.api.Test;

class StatementPatternQueryEvaluationStepTest {

	@Test
	void convertIterationSkipsBindingChecks() {
		InstrumentedQueryEvaluationContext context = new InstrumentedQueryEvaluationContext();
		SingleStatementTripleSource tripleSource = new SingleStatementTripleSource();
		StatementPattern statementPattern = new StatementPattern(new Var("s"), new Var("p"), new Var("o"));
		StatementPatternQueryEvaluationStep evaluationStep = new StatementPatternQueryEvaluationStep(
				statementPattern,
				context,
				tripleSource);

		try (CloseableIteration<BindingSet> iteration = evaluationStep.evaluate(context.createBindingSet())) {
			assertThat(iteration.hasNext()).isTrue();
			BindingSet converted = iteration.next();
			assertThat(converted).isInstanceOf(InstrumentedBindingSet.class);
			InstrumentedBindingSet bindingSet = (InstrumentedBindingSet) converted;
			assertThat(bindingSet.wasIsEmptyInvoked()).isFalse();
			assertThat(bindingSet.getBindingNames())
					.containsExactlyInAnyOrder("s", "p", "o");
			assertThat(bindingSet.getValue("s")).isEqualTo(tripleSource.statement.getSubject());
			assertThat(bindingSet.getValue("p")).isEqualTo(tripleSource.statement.getPredicate());
			assertThat(bindingSet.getValue("o")).isEqualTo(tripleSource.statement.getObject());
			assertThat(iteration.hasNext()).isFalse();
		}

		assertThat(context.bindingChecks.get()).isZero();
	}

	private static final class InstrumentedQueryEvaluationContext implements QueryEvaluationContext {

		private final AtomicInteger bindingChecks = new AtomicInteger();

		@Override
		public Predicate<BindingSet> hasBinding(String variableName) {
			return bindings -> {
				bindingChecks.incrementAndGet();
				return bindings.hasBinding(variableName);
			};
		}

		@Override
		public MutableBindingSet createBindingSet() {
			return new InstrumentedBindingSet();
		}

		@Override
		public Dataset getDataset() {
			return null;
		}

		@Override
		public org.eclipse.rdf4j.model.Literal getNow() {
			return null;
		}
	}

	private static final class InstrumentedBindingSet
			extends org.eclipse.rdf4j.query.algebra.evaluation.QueryBindingSet {

		private boolean isEmptyInvoked = false;

		private InstrumentedBindingSet() {
		}

		@Override
		public boolean isEmpty() {
			isEmptyInvoked = true;
			return super.isEmpty();
		}

		private boolean wasIsEmptyInvoked() {
			return isEmptyInvoked;
		}
	}

	private static final class SingleStatementTripleSource implements TripleSource {

		private final ValueFactory valueFactory = SimpleValueFactory.getInstance();
		private final Statement statement = valueFactory.createStatement(
				valueFactory.createIRI("urn:subj"),
				valueFactory.createIRI("urn:pred"),
				valueFactory.createLiteral("obj"));

		@Override
		public CloseableIteration<? extends Statement> getStatements(Resource subj, IRI pred, Value obj,
				Resource... contexts) throws QueryEvaluationException {
			return new CloseableIteratorIteration<>(List.of(statement).iterator());
		}

		@Override
		public ValueFactory getValueFactory() {
			return valueFactory;
		}
	}
}
