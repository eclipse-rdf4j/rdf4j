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
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.sail.SailException;

import java.util.Arrays;
import java.util.Set;

public class SetFilterNode implements PlanNode {

	private Set<Value> targetNodeList;
	private PlanNode parent;
	private int index;
	private boolean returnValid;
	private boolean printed;

	public SetFilterNode(Set<Value> targetNodeList, PlanNode parent, int index, boolean returnValid) {
		this.targetNodeList = targetNodeList;
		this.parent = parent;
		this.index = index;
		this.returnValid = returnValid;
	}

	@Override
	public CloseableIteration<Tuple, SailException> iterator() {
		return new CloseableIteration<Tuple, SailException>() {

			CloseableIteration<Tuple, SailException> iterator = parent.iterator();

			Tuple next;

			private void calulateNext(){
				while(next == null && iterator.hasNext()){
					Tuple temp = iterator.next();
					boolean contains = targetNodeList.contains(temp.getlist().get(index));
					if(returnValid && contains){
						next = temp;
					}else if (!returnValid && !contains){
						next = temp;
					}
				}
			}

			@Override
			public void close() throws SailException {
				iterator.close();
			}

			@Override
			public boolean hasNext() throws SailException {
				calulateNext();
				return next != null;
			}

			@Override
			public Tuple next() throws SailException {
				calulateNext();

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
		if (printed) {
			return;
		}
		printed = true;
		stringBuilder.append(getId() + " [label=\"" + StringEscapeUtils.escapeJava(this.toString()) + "\"];").append("\n");
		stringBuilder.append(parent.getId() + " -> " + getId()).append("\n");
		parent.getPlanAsGraphvizDot(stringBuilder);
	}

	@Override
	public String getId() {
		return System.identityHashCode(this) + "";
	}


	@Override
	public IteratorData getIteratorDataType() {
		return parent.getIteratorDataType();
	}

	@Override
	public String toString() {
		return "SetFilterNode{" +
			"targetNodeList=" + Arrays.toString(targetNodeList.toArray()) +
			", index=" + index +
			", returnValid=" + returnValid +
			'}';
	}
}
