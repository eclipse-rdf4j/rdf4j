/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.spring.resultcache;

import static org.eclipse.rdf4j.spring.resultcache.ThrowableRecorder.recordingThrowable;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.GraphQueryResult;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.spring.support.query.DelegatingIterator;

/**
 * Wrapper for a TupleQueryResult, allowing the result to be replayed. Not thread-safe. The wrapper copies the result
 * data, consuming the original result fully on <code>close()</code>.
 *
 * <p>
 * Example:
 *
 * <pre>
 * TupleQueryResult result = tupleQuery.execute();
 * ReuseableTupleQueryResult reusable = new ReuseableTupleQueryResult(result);
 * while (reusable.hasNext()) {
 * 	reuseable.next();
 * }
 * reuseable.close();
 * GraphQueryResult cached = reuseable.recreateGraphQueryResult();
 * while (cached.hasNext()) {
 * 	cached.next();
 * }
 * cached.close();
 *
 * </pre>
 *
 * @since 4.0.0
 * @author Florian Kleedorfer
 */
public class ReusableGraphQueryResult implements GraphQueryResult, ThrowableRecorder {
	private GraphQueryResult originalResult;
	private final List<Statement> statements;
	private final AtomicBoolean recording = new AtomicBoolean(true);
	private final AtomicBoolean exceptionDuringRecording = new AtomicBoolean(false);
	private final BindingSet queryBindings;
	private Map<String, String> namespaces;

	public ReusableGraphQueryResult(GraphQueryResult result, BindingSet queryBindings) {
		this.originalResult = result;
		this.queryBindings = queryBindings;
		this.statements = new LinkedList<>();
		this.recording.set(true);
		this.exceptionDuringRecording.set(false);
	}

	public boolean queryBindingsAreIdentical(BindingSet candidate) {
		return queryBindings.equals(candidate);
	}

	public boolean canReuse() {
		return (!recording.get()) && originalResult == null && (!exceptionDuringRecording.get());
	}

	public CachedGraphQueryResult recreateGraphQueryResult() {
		if (recording.get()) {
			throw new IllegalStateException("Cannot reuse yet: still recording");
		}
		return new CachedGraphQueryResult(this.statements, this.namespaces);
	}

	@Override
	public void recordThrowable(Throwable t) {
		if (recording.get()) {
			this.exceptionDuringRecording.set(true);
		}
	}

	@Override
	public Map<String, String> getNamespaces() throws QueryEvaluationException {
		if (recording.get()) {
			return this.originalResult.getNamespaces();
		}
		throw new IllegalStateException("Not open");
	}

	@Override
	public Iterator<Statement> iterator() {
		if (recording.get()) {
			return recordingThrowable(
					() -> new DelegatingIterator<Statement>(originalResult.iterator()) {
						@Override
						public Statement next() {
							Statement n = super.next();
							statements.add(n);
							return n;
						}
					},
					this);
		} else {
			throw new IllegalStateException("Not open");
		}
	}

	@Override
	public void close() throws QueryEvaluationException {
		if (recording.get()) {
			recordingThrowable(
					() -> {
						// consume fully if there are more results
						while (hasNext()) {
							next();
						}
						try {
							this.namespaces = originalResult.getNamespaces();
							originalResult.close();
						} finally {
							originalResult = null;
							this.recording.set(false);
						}
					},
					this);
		} else {
			throw new IllegalStateException("Cannot close: not open");
		}
	}

	@Override
	public boolean hasNext() throws QueryEvaluationException {
		if (recording.get()) {
			return recordingThrowable(() -> originalResult.hasNext(), this);
		} else {
			return false;
		}
	}

	@Override
	public Statement next() throws QueryEvaluationException {
		if (recording.get()) {
			Statement n = recordingThrowable(() -> originalResult.next(), this);
			statements.add(n);
			return n;
		} else {
			throw new IllegalStateException("Not open");
		}
	}

	@Override
	public void remove() throws QueryEvaluationException {
		throw new UnsupportedOperationException("Remove is not supported");
	}

	@Override
	public Stream<Statement> stream() {
		if (recording.get()) {
			return recordingThrowable(
					() -> originalResult.stream()
							.map(
									bindings -> {
										statements.add(bindings);
										return bindings;
									}),
					this);
		} else {
			throw new IllegalStateException("Not open");
		}
	}

	@Override
	public void forEach(Consumer<? super Statement> action) {
		if (recording.get()) {
			recordingThrowable(
					() -> originalResult.forEach(
							bindings -> {
								statements.add(bindings);
								action.accept(bindings);
							}),
					this);
		} else {
			throw new IllegalStateException("Not open");
		}
	}

	@Override
	public Spliterator<Statement> spliterator() {
		return Spliterators.spliteratorUnknownSize(iterator(), Spliterator.ORDERED);
	}
}
