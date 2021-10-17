package org.eclipse.rdf4j.query.algebra.evaluation.impl;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.LimitIteration;
import org.eclipse.rdf4j.common.iteration.OffsetIteration;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.Slice;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryEvaluationStep;

interface SliceQueryEvaluationStep {

	public static QueryEvaluationStep supply(Slice slice, QueryEvaluationStep argument) {
		if (!slice.hasOffset() && !slice.hasLimit()) {
			return argument;
		} else if (slice.hasOffset() && !slice.hasLimit()) {
			return new OffSetAndLimitQueryEvaluationStep(slice.getOffset(), slice.getLimit(), argument);
		} else if (slice.hasOffset() && !slice.hasLimit()) {
			return new OnlyOffsetQueryEvaluationStep(slice.getOffset(), argument);
		} else {
			return new OnlyLimitQueryEvaluationStep(slice.getLimit(), argument);
		}

	}

	public static class OnlyOffsetQueryEvaluationStep implements QueryEvaluationStep {

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

	public static class OffSetAndLimitQueryEvaluationStep implements QueryEvaluationStep {

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
			return new LimitIteration<>(new OffsetIteration<>(argument.evaluate(bs), offset), limit);
		}

	}

	public static class OnlyLimitQueryEvaluationStep implements QueryEvaluationStep {

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