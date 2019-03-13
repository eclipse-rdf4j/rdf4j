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
public class Unique implements PlanNode {
	PlanNode parent;
	private boolean printed = false;

	public Unique(PlanNode parent) {
		this.parent = parent;
	}

	@Override
	public CloseableIteration<Tuple, SailException> iterator() {
		return new CloseableIteration<Tuple, SailException>() {


			CloseableIteration<Tuple, SailException> parentIterator = parent.iterator();

			Tuple next;
			Tuple previous;

			private void calculateNext() {
				if (next != null) {
					return;
				}

				while (next == null && parentIterator.hasNext()) {
					Tuple temp = parentIterator.next();

					if (previous == null) {
						next = temp;
					} else {
						if (!(previous == temp || previous.equals(temp))) {
							next = temp;
						}
					}

					if (next != null) {
						previous = next;
					}

				}


			}

			@Override
			public void close() throws SailException {
				parentIterator.close();
			}

			@Override
			public boolean hasNext() throws SailException {
				calculateNext();
				return next != null;
			}


			@Override
			public Tuple next() throws SailException {
				calculateNext();

				Tuple temp = next;
				next = null;
				return temp;
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
		return "Unique";
	}

	@Override
	public String getId() {
		return System.identityHashCode(this)+"";
	}

	@Override
	public IteratorData getIteratorDataType() {
		return parent.getIteratorDataType();
	}
}
