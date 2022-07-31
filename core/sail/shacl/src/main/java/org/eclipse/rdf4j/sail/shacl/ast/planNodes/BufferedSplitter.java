/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.ast.planNodes;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.text.StringEscapeUtils;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.shacl.ast.ShaclUnsupportedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Håvard Ottestad
 *         <p>
 *         Allows the iterator of one planNode to be used by multiple other nodes by buffering all results from the
 *         parent iterator. This will potentially take a fair bit of memory, but maybe be useful for perfomance so that
 *         we don't query the underlying datastores for the same data multiple times.
 */
public class BufferedSplitter implements PlanNodeProvider {

	private static final AtomicLong idCounter = new AtomicLong();

	private final Logger logger = LoggerFactory.getLogger(getClass());

	private final PlanNode parent;
	private final boolean cached;
	private volatile List<ValidationTuple> tuplesBuffer;
	private long id = -1;

	public BufferedSplitter(PlanNode parent, boolean cached) {
		this.parent = parent;
		this.cached = cached;
	}

	public BufferedSplitter(PlanNode parent, boolean cached, boolean createId) {
		this.parent = parent;
		this.cached = cached;
		id = idCounter.incrementAndGet();
	}

	public BufferedSplitter(PlanNode parent) {
		this(parent, true);
	}

	private synchronized void init() {
		if (tuplesBuffer == null) {
			tuplesBuffer = new ArrayList<>();
			try (CloseableIteration<? extends ValidationTuple, SailException> iterator = parent.iterator()) {
				while (iterator.hasNext()) {
					ValidationTuple next = iterator.next();
					tuplesBuffer.add(next);
				}
			}
		}

	}

	public String getId() {
		int length = (idCounter.get() + "").length();
		return String.format("%0" + length + "d", id);
	}

	@Override
	public PlanNode getPlanNode() {
		return new BufferedSplitterPlaneNode(this, cached);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		BufferedSplitter that = (BufferedSplitter) o;
		return parent.equals(that.parent);
	}

	@Override
	public int hashCode() {
		return Objects.hash(parent);
	}

	public static class BufferedSplitterPlaneNode implements PlanNode {
		private final BufferedSplitter bufferedSplitter;
		public final boolean cached;
		private boolean printed = false;

		private ValidationExecutionLogger validationExecutionLogger;

		public BufferedSplitterPlaneNode(BufferedSplitter bufferedSplitter, boolean cached) {
			this.bufferedSplitter = bufferedSplitter;
			this.cached = cached;
		}

		@Override
		public CloseableIteration<? extends ValidationTuple, SailException> iterator() {

			return new CloseableIteration<>() {

				Iterator<ValidationTuple> iterator;

				private void init() {
					if (iterator == null) {
						bufferedSplitter.init();
						iterator = bufferedSplitter.tuplesBuffer.iterator();
					}
				}

				@Override
				public void close() throws SailException {

				}

				@Override
				public boolean hasNext() throws SailException {
					init();
					return iterator.hasNext();
				}

				@Override
				public ValidationTuple next() throws SailException {
					init();
					ValidationTuple tuple = iterator.next();
					if (validationExecutionLogger.isEnabled()) {
						validationExecutionLogger.log(depth(),
								bufferedSplitter.parent.getClass().getSimpleName() + ":BufferedSplitter.next()", tuple,
								bufferedSplitter.parent, getId(), null);
					}
					return tuple;
				}

				@Override
				public void remove() throws SailException {
					throw new ShaclUnsupportedException();
				}
			};
		}

		@Override
		public int depth() {
			return bufferedSplitter.parent.depth() + 1;
		}

		@Override
		public void getPlanAsGraphvizDot(StringBuilder stringBuilder) {
			if (printed) {
				return;
			}
			printed = true;
			stringBuilder.append(getId() + " [label=\"" + StringEscapeUtils.escapeJava(this.toString()) + "\"];")
					.append("\n");
			stringBuilder.append(bufferedSplitter.parent.getId() + " -> " + getId()).append("\n");
			bufferedSplitter.parent.getPlanAsGraphvizDot(stringBuilder);
		}

		@Override
		public String getId() {
			return System.identityHashCode(bufferedSplitter) + "";
		}

		@Override
		public String toString() {
			return "BufferedSplitter";
		}

		@Override
		public void receiveLogger(ValidationExecutionLogger validationExecutionLogger) {
			this.validationExecutionLogger = validationExecutionLogger;
			bufferedSplitter.parent.receiveLogger(validationExecutionLogger);
		}

		@Override
		public boolean producesSorted() {
			return bufferedSplitter.parent.producesSorted();
		}

		@Override
		public boolean requiresSorted() {
			return bufferedSplitter.parent.requiresSorted();
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			BufferedSplitterPlaneNode that = (BufferedSplitterPlaneNode) o;
			return bufferedSplitter.equals(that.bufferedSplitter);
		}

		@Override
		public int hashCode() {
			return Objects.hash(bufferedSplitter);
		}
	}

}
