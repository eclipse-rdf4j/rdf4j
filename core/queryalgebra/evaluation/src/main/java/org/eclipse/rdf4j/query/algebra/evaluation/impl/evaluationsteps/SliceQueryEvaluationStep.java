/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.impl.evaluationsteps;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.LimitIteration;
import org.eclipse.rdf4j.common.iteration.OffsetIteration;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.Slice;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryEvaluationStep;

public interface SliceQueryEvaluationStep extends QueryEvaluationStep {

	public static QueryEvaluationStep supply(Slice slice, QueryEvaluationStep argument) {
		// if there is no offset nor limit then the operator does nothing
		// pass through the argument in one go.
		if (!slice.hasOffset() && !slice.hasLimit()) {
			return argument;
		} else if (slice.hasOffset() && slice.hasLimit()) {
			return new OffSetAndLimitQueryEvaluationStep(slice.getOffset(), slice.getLimit(), argument);
		} else if (slice.hasOffset() && !slice.hasLimit()) {
			return new OnlyOffsetQueryEvaluationStep(slice.getOffset(), argument);
		} else {
			return new OnlyLimitQueryEvaluationStep(slice.getLimit(), argument);
		}
	}

	static class OnlyOffsetQueryEvaluationStep implements SliceQueryEvaluationStep {

		private final long offset;
		private final QueryEvaluationStep argument;

		public OnlyOffsetQueryEvaluationStep(long offset, QueryEvaluationStep argument) {
			this.offset = offset;
			this.argument = argument;
		}

		@Override
		public CloseableIteration<BindingSet, QueryEvaluationException> evaluate(BindingSet bs) {
			return new OffsetIteration<>(argument.evaluate(bs), offset);
		}
	}

	static class OffSetAndLimitQueryEvaluationStep implements SliceQueryEvaluationStep {

		private final long offset;
		private final long limit;
		private final QueryEvaluationStep argument;

		public OffSetAndLimitQueryEvaluationStep(long offset, long limit, QueryEvaluationStep argument) {
			this.offset = offset;
			this.limit = limit;
			this.argument = argument;
		}

		@Override
		public CloseableIteration<BindingSet, QueryEvaluationException> evaluate(BindingSet bs) {
			CloseableIteration<BindingSet, QueryEvaluationException> evaluate = argument.evaluate(bs);
			OffsetIteration<BindingSet, QueryEvaluationException> offsetIter = new OffsetIteration<>(
					evaluate, offset);
			LimitIteration<BindingSet, QueryEvaluationException> limitIter = new LimitIteration<>(offsetIter, limit);
			return limitIter;
		}
	}

	static class OnlyLimitQueryEvaluationStep implements SliceQueryEvaluationStep {

		private final long limit;
		private final QueryEvaluationStep argument;

		public OnlyLimitQueryEvaluationStep(long limit, QueryEvaluationStep argument) {
			this.limit = limit;
			this.argument = argument;
		}

		@Override
		public CloseableIteration<BindingSet, QueryEvaluationException> evaluate(BindingSet bs) {
			return new LimitIteration<>(argument.evaluate(bs), limit);
		}
	}
}