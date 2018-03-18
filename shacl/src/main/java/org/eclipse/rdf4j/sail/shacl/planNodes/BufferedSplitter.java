/*******************************************************************************
 * Copyright (c) 2016 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.planNodes;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.sail.SailException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author Håvard Ottestad
 *
 * Allows the iterator of one planNode to be used by multiple other nodes by buffering all results from the parent iterator.
 * This will potentially take a fair bit of memory, but maybe be useful for perfomance so that we don't query the underlying
 * datastores for the same data multiple times.
 */
public class BufferedSplitter{

	PlanNode parent;
	private List<Tuple> tuplesBuffer = new ArrayList<>();

	public BufferedSplitter(PlanNode planNode) {
		parent = planNode;
		initialize();
	}

	private void initialize(){
		try (CloseableIteration<Tuple, SailException> iterator = parent.iterator()) {

			while (iterator.hasNext()) {
				Tuple next = iterator.next();
				tuplesBuffer.add(next);
			}
		}
	}

	public PlanNode getPlanNode() {

		return new PlanNode() {
			@Override
			public CloseableIteration<Tuple, SailException> iterator() {
				return new CloseableIteration<Tuple, SailException>() {

					Iterator<Tuple> iterator = tuplesBuffer.iterator();


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
				return parent.depth()+1;
			}
		};

	}
}

