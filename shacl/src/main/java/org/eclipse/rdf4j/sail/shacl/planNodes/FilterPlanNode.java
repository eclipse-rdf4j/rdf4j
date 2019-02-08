/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.planNodes;


import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.sail.SailException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;


/**
 * @author Håvard Ottestad
 */
public abstract class FilterPlanNode<T extends PushBasedPlanNode & SupportsParentProvider> implements ParentProvider {

	static private final Logger logger = LoggerFactory.getLogger(FilterPlanNode.class);


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
			trueNode.receiveParentProvider(this);
		}

		if (falseNode != null) {
			falseNode.parentIterator(iterator);
			falseNode.receiveParentProvider(this);
		}
	}

	private CloseableIteration<Tuple, SailException> iterator() {
		FilterPlanNode<T> that = this;
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
							if(LoggingNode.loggingEnabled){
								logger.info(leadingSpace() + that.getClass().getSimpleName() + ";trueNode: " + " " + temp.toString());
							}
							trueNode.push(temp);

						}
					} else {
						if (falseNode != null) {
							if(LoggingNode.loggingEnabled){
								logger.info(leadingSpace() + that.getClass().getSimpleName() + ";falseNode: " + " " + temp.toString());
							}
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
				if (parentIterator != null) {
					parentIterator.close();
				}
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
	public List<PlanNode> parent() {
		return Arrays.asList(parent);
	}


	boolean printed = false;

	public void getPlanAsGraphvizDot(StringBuilder stringBuilder) {
		if(printed) return;
		printed = true;
		stringBuilder.append(getId() + " [label=\"" + StringEscapeUtils.escapeJava(this.toString()) + "\"];").append("\n");
		stringBuilder.append(parent.getId()+" -> "+getId()).append("\n");
		if(trueNode != null){
			String id = getId(trueNode);
			stringBuilder.append(getId()+" -> "+id+ " [label=\"true values\"]").append("\n");

		}
		if(falseNode != null){
			String id = getId(falseNode);
			stringBuilder.append(getId()+" -> "+id+ " [label=\"false values\"]").append("\n");

		}

		parent.getPlanAsGraphvizDot(stringBuilder);



	}

	private String getId(T node) {
		if(node instanceof PlanNode){
			return ((PlanNode) node).getId();
		}

		return System.identityHashCode(node)+"";
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName();
	}

	public String getId() {
		return System.identityHashCode(this)+"";
	}


	private String leadingSpace() {
		return StringUtils.leftPad("", parent.depth()+1, "    ");
	}
}
