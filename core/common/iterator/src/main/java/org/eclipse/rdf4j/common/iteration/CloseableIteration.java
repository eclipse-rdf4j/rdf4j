/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.common.iteration;

import java.util.Iterator;
import java.util.stream.Stream;

import org.eclipse.rdf4j.common.annotation.Experimental;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;

/**
 * An {@link CloseableIteration} that can be closed to free resources that it is holding. CloseableIterations
 * automatically free their resources when exhausted. If not read until exhaustion or if you want to make sure the
 * iteration is properly closed, any code using the iterator should be placed in a try-with-resources block, closing the
 * iteration automatically, e.g.:
 *
 * <pre>
 *
 * try (CloseableIteration&lt;Object, Exception&gt; iter = ...) {
 *    // read objects from the iterator
 * }
 * catch(Exception e) {
 *   // process the exception that can be thrown while processing.
 * }
 * </pre>
 *
 */
public interface CloseableIteration<E> extends Iterator<E>, AutoCloseable {

	EmptyIteration<Statement> EMPTY_STATEMENT_ITERATION = new EmptyIteration<>();

	/**
	 * Convert the results to a Java 8 Stream.
	 *
	 * @return stream
	 */
	default Stream<E> stream() {
		return Iterations.stream(this);
	}

	/**
	 * Closes this iteration, freeing any resources that it is holding. If the iteration has already been closed then
	 * invoking this method has no effect.
	 */
	@Override
	void close();

	/**
	 * Advisory range hint for value-ordered iterations.
	 * <p>
	 * Semantics are only defined when this iteration was produced with a known element order, such as a statement order
	 * component or an ordered join variable agreed between producer and consumer; any other iteration MUST treat this
	 * call as a no-op. The hint is advisory in both directions:
	 * <ul>
	 * <li><b>min</b>: the caller declares that it will not use elements ordered before {@code minValue} (before or
	 * equal to {@code minValue} when {@code minInclusive} is {@code false}). The implementation MAY skip any subset of
	 * such elements, or MAY ignore the hint and keep returning them. Successive minimum targets MUST be non-decreasing
	 * in the agreed order.</li>
	 * <li><b>max</b>: the caller declares that it will not use elements ordered after {@code maxValue} (after or equal
	 * to {@code maxValue} when {@code maxInclusive} is {@code false}). The implementation MAY report exhaustion once
	 * the bound is passed, or MAY keep returning elements beyond it; the caller must tolerate both.</li>
	 * </ul>
	 * A {@code null} bound means unbounded on that end. Implementations MUST NOT throw for hints they cannot honor and
	 * MUST do nothing when in doubt (buffered state, unresolvable bound values). This method does not otherwise change
	 * the contract of {@link #hasNext()} and {@link #next()}.
	 *
	 * @param minValue     lower bound in the agreed order, or {@code null} for unbounded
	 * @param minInclusive whether {@code minValue} itself may still be used by the caller
	 * @param maxValue     upper bound in the agreed order, or {@code null} for unbounded
	 * @param maxInclusive whether {@code maxValue} itself may still be used by the caller
	 */
	@Experimental
	default void seek(Value minValue, boolean minInclusive, Value maxValue, boolean maxInclusive) {
		// advisory: default implementations ignore the hint
	}

	/**
	 * Discovery hint for {@link #seek(Value, boolean, Value, boolean)}: {@code true} means a seek hint MAY be honored
	 * somewhere downstream, so it is worth the caller's time to compute bounds. Wrappers should delegate to their
	 * wrapped iteration.
	 *
	 * @return {@code true} if seek hints may be honored by this iteration
	 */
	@Experimental
	default boolean supportsSeek() {
		return false;
	}

}
