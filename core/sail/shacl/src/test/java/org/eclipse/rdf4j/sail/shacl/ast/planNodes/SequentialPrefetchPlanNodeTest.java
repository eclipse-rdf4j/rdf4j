/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.shacl.ast.planNodes;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.sail.shacl.ast.constraintcomponents.ConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.mock.MockInputPlanNode;
import org.junit.jupiter.api.Test;

/**
 * @author Sava Savov
 */
public class SequentialPrefetchPlanNodeTest {

	private static final Resource[] CONTEXTS = { null };
	private static final SimpleValueFactory VF = SimpleValueFactory.getInstance();

	private static ValidationTuple tuple(String value) {
		return new ValidationTuple(VF.createLiteral(value), ConstraintComponent.Scope.nodeShape, false, CONTEXTS);
	}

	private static List<ValidationTuple> drain(PlanNode node) {
		node.receiveLogger(ValidationExecutionLogger.getInstance(false));
		try (CloseableIteration<? extends ValidationTuple> iterator = node.iterator()) {
			List<ValidationTuple> result = new ArrayList<>();
			while (iterator.hasNext()) {
				result.add(iterator.next());
			}
			return result;
		}
	}

	@Test
	public void mergesAllDelegatesInOrder() {
		MockInputPlanNode first = new MockInputPlanNode(List.of(tuple("a"), tuple("b")));
		MockInputPlanNode second = new MockInputPlanNode(List.of(tuple("c")));

		SequentialPrefetchPlanNode node = new SequentialPrefetchPlanNode(2);
		node.addDelegate(first);
		node.addDelegate(second);

		List<ValidationTuple> result = drain(node);

		assertThat(result).containsExactly(tuple("a"), tuple("b"), tuple("c"));
	}

	@Test
	public void deduplicatesTuplesSeenAcrossDifferentDelegates() {
		// mirrors the case a folded UNION query used to collapse "for free" via a single SELECT DISTINCT:
		// the same (target) pair satisfied by two different branches should only be reported once.
		MockInputPlanNode first = new MockInputPlanNode(List.of(tuple("a"), tuple("shared")));
		MockInputPlanNode second = new MockInputPlanNode(List.of(tuple("shared"), tuple("b")));

		SequentialPrefetchPlanNode node = new SequentialPrefetchPlanNode(2);
		node.addDelegate(first);
		node.addDelegate(second);

		List<ValidationTuple> result = drain(node);

		assertThat(result).containsExactly(tuple("a"), tuple("shared"), tuple("b"));
	}

	@Test
	public void skipsDelegatesThatProduceNoResults() {
		MockInputPlanNode empty = new MockInputPlanNode(List.of());
		MockInputPlanNode withData = new MockInputPlanNode(List.of(tuple("a")));

		SequentialPrefetchPlanNode node = new SequentialPrefetchPlanNode(3);
		node.addDelegate(empty);
		node.addDelegate(withData);
		node.addDelegate(empty);

		List<ValidationTuple> result = drain(node);

		assertThat(result).containsExactly(tuple("a"));
	}

	@Test
	public void emptyWhenThereAreNoDelegates() {
		SequentialPrefetchPlanNode node = new SequentialPrefetchPlanNode(0);

		assertThat(drain(node)).isEmpty();
	}

	@Test
	public void depthIsOneMoreThanTheDeepestDelegate() {
		SequentialPrefetchPlanNode node = new SequentialPrefetchPlanNode(2);
		node.addDelegate(new MockInputPlanNode(List.of(tuple("a"))));
		node.addDelegate(EmptyNode.getInstance());

		// MockInputPlanNode and EmptyNode both report depth() == 0
		assertThat(node.depth()).isEqualTo(1);
	}

	@Test
	public void receiveLoggerPropagatesToEveryDelegate() {
		RecordingPlanNode first = new RecordingPlanNode();
		RecordingPlanNode second = new RecordingPlanNode();

		SequentialPrefetchPlanNode node = new SequentialPrefetchPlanNode(2);
		node.addDelegate(first);
		node.addDelegate(second);

		ValidationExecutionLogger logger = ValidationExecutionLogger.getInstance(true);
		node.receiveLogger(logger);

		assertThat(first.receivedLogger).isSameAs(logger);
		assertThat(second.receivedLogger).isSameAs(logger);
	}

	/**
	 * Minimal delegate that only records whether/what {@link #receiveLogger(ValidationExecutionLogger)} was called with
	 * - {@link MockInputPlanNode} doesn't expose that for assertions.
	 */
	private static class RecordingPlanNode implements PlanNode {
		ValidationExecutionLogger receivedLogger;

		@Override
		public CloseableIteration<? extends ValidationTuple> iterator() {
			return new MockInputPlanNode(List.of()).iterator();
		}

		@Override
		public int depth() {
			return 0;
		}

		@Override
		public void getPlanAsGraphvizDot(StringBuilder stringBuilder) {
		}

		@Override
		public String getId() {
			return System.identityHashCode(this) + "";
		}

		@Override
		public void receiveLogger(ValidationExecutionLogger validationExecutionLogger) {
			this.receivedLogger = validationExecutionLogger;
		}

		@Override
		public boolean producesSorted() {
			return true;
		}

		@Override
		public boolean requiresSorted() {
			return false;
		}
	}
}
