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


/**
 * @author Håvard Ottestad
 */
public class TrimTuple implements PlanNode {

	PlanNode parent;
	int newLength;

	public TrimTuple(PlanNode parent, int newLength) {
		this.parent = parent;
		this.newLength = newLength;
	}

	@Override
	public CloseableIteration<Tuple, SailException> iterator() {
		return new CloseableIteration<Tuple, SailException>() {


			CloseableIteration<Tuple, SailException> parentIterator = parent.iterator();


			@Override
			public void close() throws SailException {
				parentIterator.close();
			}

			@Override
			public boolean hasNext() throws SailException {
				return parentIterator.hasNext();
			}

			@Override
			public Tuple next() throws SailException {

				Tuple next = parentIterator.next();

				Tuple tuple = new Tuple();

				for (int i = 0; i < newLength && i < next.line.size(); i++) {
					tuple.line.add(next.line.get(i));
					tuple.addHistory(next);
				}

				return tuple;
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
	public void printPlan() {
		System.out.println(getId() + " [label=\"" + StringEscapeUtils.escapeJava(this.toString()) + "\"];");
		System.out.println(parent.getId()+" -> "+getId());
		parent.printPlan();
	}

	@Override
	public String toString() {
		return "TrimTuple{" +
			"newLength=" + newLength +
			'}';
	}

	@Override
	public String getId() {
		return System.identityHashCode(this)+"";
	}
}
