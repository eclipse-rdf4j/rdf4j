/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.planNodes;

import org.apache.commons.lang.StringEscapeUtils;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.sail.SailException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

public class Sort implements PlanNode {

	private final PlanNode parent;
	private boolean printed = false;

	public Sort(PlanNode parent) {
		this.parent = parent;
	}

	@Override
	public CloseableIteration<Tuple, SailException> iterator() {
		return new CloseableIteration<Tuple, SailException>() {

			CloseableIteration<Tuple, SailException> iterator = parent.iterator();

			List<Tuple> sortedTuples;

			Iterator<Tuple> sortedTuplesIterator;


			@Override
			public void close() throws SailException {
				iterator.close();
			}

			@Override
			public boolean hasNext() throws SailException {
				sortTuples();
				return sortedTuplesIterator.hasNext();
			}

			private void sortTuples() {
				if (sortedTuples == null) {
					sortedTuples = new ArrayList<>();
					while (iterator.hasNext()) {
						sortedTuples.add(iterator.next());
					}

					Collections.sort(sortedTuples, Comparator.comparing(a -> a.line.get(0).stringValue()));


					sortedTuplesIterator = sortedTuples.iterator();

				}
			}

			@Override
			public Tuple next() throws SailException {
				sortTuples();

				return sortedTuplesIterator.next();
			}

			@Override
			public void remove() throws SailException {

			}
		};

	}

	@Override
	public int depth() {
		return parent.depth() + 1;
	}

	@Override
	public void getPlanAsGraphvizDot(StringBuilder stringBuilder) {
		if(printed) return;
		printed = true;
		stringBuilder.append(getId() + " [label=\"" + StringEscapeUtils.escapeJava(this.toString()) + "\"];").append("\n");
		stringBuilder.append(parent.getId()+" -> "+getId()).append("\n");
		parent.getPlanAsGraphvizDot(stringBuilder);
	}

	@Override
	public String getId() {
		return System.identityHashCode(this)+"";
	}

	@Override
	public String toString() {
		return "Sort";
	}

	@Override
	public IteratorData getIteratorDataType() {
		return parent.getIteratorDataType();
	}
}
