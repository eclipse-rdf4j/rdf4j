/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 ******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.ast.planNodes;

import org.eclipse.rdf4j.common.concurrent.locks.diagnostics.ConcurrentCleaner;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;

import java.lang.ref.Cleaner;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ResourceTracker {

	private static final ConcurrentCleaner concurrentCleaner = new ConcurrentCleaner();

	private final static boolean assertEnabled = isAssertEnabled();


	private static boolean isAssertEnabled() {
		boolean assertEnabled = false;
		try {
			assert false;
		} catch (AssertionError e) {
			assertEnabled = true;
		}
		return assertEnabled;
	}

	public static CloseableIteration<? extends BindingSet, QueryEvaluationException> track(CloseableIteration<? extends BindingSet, QueryEvaluationException> closeableIteration) {
		if (assertEnabled) {
			return new TrackedCloseableIteration(closeableIteration);
		}
		return closeableIteration;
	}

	private static class TrackedCloseableIteration implements CloseableIteration<BindingSet, QueryEvaluationException> {

		private final CloseableIteration<? extends BindingSet, QueryEvaluationException> delegate;
		private final Cleaner.Cleanable cleanable;
		private final State closed;

		public TrackedCloseableIteration(CloseableIteration<? extends BindingSet, QueryEvaluationException> delegate) {
			this.delegate = delegate;
			State state = new State();
			cleanable = concurrentCleaner.register(this, state);
			this.closed = state;
		}

		private static class State implements Runnable{
			private boolean closed;

			@Override
			public void run() {
				if(!closed){
					throw new IllegalStateException("Resource was garbage collected before it was closed!");
				}
			}
		}

		@Override
		public void close() throws QueryEvaluationException {
			closed.closed = true;
			delegate.close();
			cleanable.clean();
		}

		@Override
		public boolean hasNext() throws QueryEvaluationException {
			return delegate.hasNext();
		}

		@Override
		public BindingSet next() throws QueryEvaluationException {
			return delegate.next();
		}

		@Override
		public void remove() throws QueryEvaluationException {
			delegate.remove();
		}

		@Override
		public String toString() {
			return delegate.toString();
		}

		@Override
		public boolean equals(Object o) {
			if (o instanceof TrackedCloseableIteration) {
				return delegate.equals(((TrackedCloseableIteration) o).delegate);
			}
			return delegate.equals(o);
		}

		@Override
		public int hashCode() {
			return delegate.hashCode();
		}
	}


}
