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
	private int newLength;
	private int startIndex;
	private boolean printed = false;

	public TrimTuple(PlanNode parent, int startIndex, int newLength) {
		this.parent = parent;
		this.newLength = newLength;
		this.startIndex = startIndex;
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

				int tempLength = newLength >= 0 ? newLength : next.line.size();
				for (int i = startIndex; i < tempLength && i < next.line.size(); i++) {
					tuple.line.add(next.line.get(i));
				}

				tuple.addHistory(next);
				tuple.addAllCausedByPropertyShape(next.getCausedByPropertyShapes());

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
	public void getPlanAsGraphvizDot(StringBuilder stringBuilder) {
		if(printed) return;
		printed = true;
		stringBuilder.append(getId() + " [label=\"" + StringEscapeUtils.escapeJava(this.toString()) + "\"];").append("\n");
		stringBuilder.append(parent.getId()+" -> "+getId()).append("\n");
		parent.getPlanAsGraphvizDot(stringBuilder);
	}

	@Override
	public String toString() {
		return "TrimTuple{" +
			"parent=" + parent +
			", newLength=" + newLength +
			", startIndex=" + startIndex +
			'}';
	}

	@Override
	public String getId() {
		return System.identityHashCode(this)+"";
	}

	@Override
	public IteratorData getIteratorDataType() {
		if(newLength == 1) return IteratorData.tripleBased;
		return parent.getIteratorDataType();
	}
}
