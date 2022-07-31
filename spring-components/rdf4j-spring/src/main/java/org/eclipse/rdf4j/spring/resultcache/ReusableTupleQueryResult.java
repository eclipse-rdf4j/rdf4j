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
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.spring.support.query.DelegatingIterator;

/**
 * Wrapper for a TupleQueryResult, allowing the result to be replayed. The wrapper copies the result data, consuming the
 * original result fully on <code>close()</code>.
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
 * TupleQueryResult cached = reuseable.recreateTupleQueryResult();
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
public class ReusableTupleQueryResult implements TupleQueryResult, ThrowableRecorder {
	private TupleQueryResult originalResult;
	private final List<BindingSet> bindingSets;
	private final AtomicBoolean recording = new AtomicBoolean(true);
	private final AtomicBoolean exceptionDuringRecording = new AtomicBoolean(false);
	private final BindingSet queryBindings;
	private List<String> bindingNames;

	public ReusableTupleQueryResult(TupleQueryResult result, BindingSet queryBindings) {
		this.originalResult = result;
		this.queryBindings = queryBindings;
		this.bindingSets = new LinkedList<>();
		this.recording.set(true);
		this.exceptionDuringRecording.set(false);
	}

	public boolean queryBindingsAreIdentical(BindingSet candidate) {
		return queryBindings.equals(candidate);
	}

	public boolean canReuse() {
		return (!recording.get()) && originalResult == null && (!exceptionDuringRecording.get());
	}

	public CachedTupleQueryResult recreateTupleQueryResult() {
		if (recording.get()) {
			throw new IllegalStateException("Cannot reuse yet: still recording");
		}
		return new CachedTupleQueryResult(this.bindingSets, this.bindingNames);
	}

	@Override
	public void recordThrowable(Throwable t) {
		if (recording.get()) {
			this.exceptionDuringRecording.set(true);
		}
	}

	@Override
	public List<String> getBindingNames() throws QueryEvaluationException {
		if (recording.get()) {
			return recordingThrowable(
					originalResult::getBindingNames,
					this);
		} else {
			throw new IllegalStateException("Not open");
		}
	}

	@Override
	public Iterator<BindingSet> iterator() {
		if (recording.get()) {
			return recordingThrowable(
					() -> new DelegatingIterator<BindingSet>(originalResult.iterator()) {
						@Override
						public BindingSet next() {
							BindingSet n = super.next();
							bindingSets.add(n);
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
							this.bindingNames = originalResult.getBindingNames();
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
			throw new IllegalStateException("Not open");
		}
	}

	@Override
	public BindingSet next() throws QueryEvaluationException {
		if (recording.get()) {
			BindingSet n = recordingThrowable(() -> originalResult.next(), this);
			bindingSets.add(n);
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
	public Stream<BindingSet> stream() {
		if (recording.get()) {
			return recordingThrowable(
					() -> originalResult.stream()
							.map(
									bindings -> {
										bindingSets.add(bindings);
										return bindings;
									}),
					this);
		} else {
			throw new IllegalStateException("Not open");
		}
	}

	@Override
	public void forEach(Consumer<? super BindingSet> action) {
		if (recording.get()) {
			recordingThrowable(
					() -> originalResult.forEach(
							bindings -> {
								bindingSets.add(bindings);
								action.accept(bindings);
							}),
					this);
		} else {
			throw new IllegalStateException("Not open");
		}
	}

	@Override
	public Spliterator<BindingSet> spliterator() {
		if (recording.get()) {
			return Spliterators.spliteratorUnknownSize(iterator(), Spliterator.ORDERED);
		} else {
			throw new IllegalStateException("Not open");
		}
	}
}
