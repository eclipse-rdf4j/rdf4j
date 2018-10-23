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
import java.util.Iterator;
import java.util.List;


/**
 * @author Håvard Ottestad
 * <p>
 * Allows the iterator of one planNode to be used by multiple other nodes by buffering all results from the parent iterator.
 * This will potentially take a fair bit of memory, but maybe be useful for perfomance so that we don't query the underlying
 * datastores for the same data multiple times.
 */
public class BufferedSplitter {

	PlanNode parent;
	private List<Tuple> tuplesBuffer;
	private BufferedSplitter that = this;
	boolean printed = false;

	public BufferedSplitter(PlanNode planNode) {
		parent = planNode;
	}

	private void initialize() {
		if (tuplesBuffer == null) {
			tuplesBuffer = new ArrayList<>();
			try (CloseableIteration<Tuple, SailException> iterator = parent.iterator()) {

				while (iterator.hasNext()) {
					Tuple next = iterator.next();
					tuplesBuffer.add(next);
				}
			}
		}

	}

	public PlanNode getPlanNode() {


		return new PlanNode() {
			@Override
			public CloseableIteration<Tuple, SailException> iterator() {

				initialize();
				Iterator<Tuple> iterator = tuplesBuffer.iterator();


				return new CloseableIteration<Tuple, SailException>() {

					@Override
					public void close() throws SailException {

					}

					@Override
					public boolean hasNext() throws SailException {
						return iterator.hasNext();
					}

					@Override
					public Tuple next() throws SailException {
						return iterator.next();
					}

					@Override
					public void remove() throws SailException {
						throw new UnsupportedOperationException();
					}
				};
			}

			@Override
			public int depth() {
				return parent.depth() + 1;
			}

			@Override
			public void printPlan() {
				if(printed) return;
				printed = true;
				System.out.println(getId() + " [label=\"" + StringEscapeUtils.escapeJava(this.toString()) + "\"];");
				System.out.println(parent.getId()+" -> "+getId());
				parent.printPlan();
			}



			@Override
			public String getId() {
				return System.identityHashCode(that)+"";
			}

			@Override
			public String toString() {
				return "BufferedSplitter";
			}
		};

	}
}

