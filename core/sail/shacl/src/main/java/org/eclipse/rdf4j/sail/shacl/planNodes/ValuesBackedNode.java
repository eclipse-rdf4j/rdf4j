/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.planNodes;

import org.apache.commons.text.StringEscapeUtils;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.memory.MemoryStoreConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Iterator;

/**
 * @author Håvard Ottestad
 */
public class ValuesBackedNode implements PlanNode {

	private static final Logger logger = LoggerFactory.getLogger(ValuesBackedNode.class);
	private final Collection<Value> collection;
	private ValidationExecutionLogger validationExecutionLogger;
	boolean printed = false;

	public ValuesBackedNode(Collection<Value> collection) {
		this.collection = collection;
	}

	@Override
	public CloseableIteration<Tuple, SailException> iterator() {
		return new LoggingCloseableIteration(this, validationExecutionLogger) {

			Iterator<Value> iterator = collection.iterator();

			@Override
			public void close() throws SailException {
			}

			@Override
			public boolean localHasNext() throws SailException {
				return iterator.hasNext();
			}

			@Override
			public Tuple loggingNext() throws SailException {
				return new Tuple(iterator.next());
			}

			@Override
			public void remove() throws SailException {

			}
		};
	}

	@Override
	public int depth() {
		return 0;
	}

	@Override
	public void getPlanAsGraphvizDot(StringBuilder stringBuilder) {
		if (printed) {
			return;
		}
		printed = true;
		stringBuilder.append(getId() + " [label=\"" + StringEscapeUtils.escapeJava(this.toString()) + "\"];")
				.append("\n");

	}

	@Override
	public String getId() {
		return System.identityHashCode(this) + "";
	}

	@Override
	public IteratorData getIteratorDataType() {
		return IteratorData.tripleBased;
	}

	@Override
	public String toString() {
		return "ValuesBackedNode{" +
				"collection=" + collection +
				", validationExecutionLogger=" + validationExecutionLogger +
				", printed=" + printed +
				'}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		ValuesBackedNode that = (ValuesBackedNode) o;
		return collection.equals(that.collection);
	}

	@Override
	public int hashCode() {

		return collection.hashCode();

	}

	@Override
	public void receiveLogger(ValidationExecutionLogger validationExecutionLogger) {
		this.validationExecutionLogger = validationExecutionLogger;
	}
}
