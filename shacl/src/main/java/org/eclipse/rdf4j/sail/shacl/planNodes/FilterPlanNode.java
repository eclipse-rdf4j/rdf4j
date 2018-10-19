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
public abstract class FilterPlanNode<T extends PushBasedPlanNode & SupportsDepthProvider> implements DepthProvider {

	PlanNode parent;

	T trueNode;
	T falseNode;


	abstract boolean checkTuple(Tuple t);

	public FilterPlanNode(PlanNode parent, T trueNode, T falseNode) {
		this.parent = parent;
		this.trueNode = trueNode;
		this.falseNode = falseNode;

		initialize(trueNode, falseNode);

	}

	private void initialize(T trueNode, T falseNode) {
		CloseableIteration<Tuple, SailException> iterator = iterator();

		if (trueNode != null) {
			trueNode.parentIterator(iterator);
			trueNode.receiveDepthProvider(this);
		}

		if (falseNode != null) {
			falseNode.parentIterator(iterator);
			falseNode.receiveDepthProvider(this);
		}
	}

	private CloseableIteration<Tuple, SailException> iterator() {
		return new CloseableIteration<Tuple, SailException>() {

			CloseableIteration<Tuple, SailException> parentIterator;

			Tuple next;

			private void calculateNext() {
				if(parentIterator == null){
					 parentIterator = parent.iterator();
				}

				if (next != null) {
					return;
				}

				while (parentIterator.hasNext() && next == null) {
					Tuple temp = parentIterator.next();

					if (checkTuple(temp)) {
						if (trueNode != null) {
							trueNode.push(temp);
						}
					} else {
						if (falseNode != null) {
							falseNode.push(temp);
						}
					}

					next = temp;

				}

			}

			boolean closed = false;

			@Override
			public void close() throws SailException {
				if (closed) {
					return;
				}

				closed = true;
				parentIterator.close();
			}

			@Override
			public boolean hasNext() throws SailException {
				calculateNext();
				if (next == null) {
					close();
				}
				return next != null;
			}

			@Override
			public Tuple next() throws SailException {
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


	boolean printed = false;

	public void printPlan() {
		if(printed) return;
		printed = true;
		System.out.println(getId() + " [label=\"" + StringEscapeUtils.escapeJava(this.toString()) + "\"];");
		System.out.println(parent.getId()+" -> "+getId());
		if(trueNode != null){
			String id = getId(trueNode);
			System.out.println(getId()+" -> "+id+ " [label=\"true values\"]");

		}
		if(falseNode != null){
			String id = getId(falseNode);
			System.out.println(getId()+" -> "+id+ " [label=\"false values\"]");

		}

		parent.printPlan();



	}

	private String getId(T trueNode) {
		return System.identityHashCode(trueNode)+"";
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName();
	}

	public String getId() {
		return System.identityHashCode(this)+"";
	}
}
