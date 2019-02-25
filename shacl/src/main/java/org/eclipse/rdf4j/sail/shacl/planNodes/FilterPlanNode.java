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


/**
 * @author Håvard Ottestad
 */
public abstract class FilterPlanNode implements MultiStreamPlanNode, PlanNode {

	static private final Logger logger = LoggerFactory.getLogger(FilterPlanNode.class);


	PlanNode parent;

	PushablePlanNode trueNode;
	PushablePlanNode falseNode;

	private CloseableIteration<Tuple, SailException> iterator;


	abstract boolean checkTuple(Tuple t);

	public FilterPlanNode(PlanNode parent) {
		this.parent = parent;
	}

	public PlanNode getTrueNode(Class<? extends PushablePlanNode> type) {
		if (trueNode != null) {
			throw new IllegalStateException();
		}
		if (type == BufferedPlanNode.class) {
			trueNode = new BufferedPlanNode<>(this);
		} else {
			trueNode = new UnBufferedPlanNode<>(this);

		}

		return trueNode;
	}

	public PlanNode getFalseNode(Class<? extends PushablePlanNode> type) {
		if (falseNode != null) {
			throw new IllegalStateException();
		}
		if (type == BufferedPlanNode.class) {
			falseNode = new BufferedPlanNode<>(this);
		} else {
			falseNode = new UnBufferedPlanNode<>(this);

		}

		return falseNode;
	}


	public CloseableIteration<Tuple, SailException> iterator() {

		return new CloseableIteration<Tuple, SailException>() {

			CloseableIteration<Tuple, SailException> parentIterator;

			Tuple next;

			private void calculateNext() {
				if (parentIterator == null) {
					parentIterator = parent.iterator();
				}

				if (next != null) {
					return;
				}

				while (parentIterator.hasNext() && next == null) {
					Tuple temp = parentIterator.next();

					if (checkTuple(temp)) {
						if (trueNode != null) {
							if (LoggingNode.loggingEnabled) {
								logger.info(leadingSpace() + this.getClass().getSimpleName() + ";trueNode: " + " " + temp.toString());
							}
							trueNode.push(temp);

						}
					} else {
						if (falseNode != null) {
							if (LoggingNode.loggingEnabled) {
								logger.info(leadingSpace() + this.getClass().getSimpleName() + ";falseNode: " + " " + temp.toString());
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


	boolean printed = false;

	public void getPlanAsGraphvizDot(StringBuilder stringBuilder) {
		if (printed) {
			return;
		}
		printed = true;
		stringBuilder.append(getId() + " [label=\"" + StringEscapeUtils.escapeJava(this.toString()) + "\"];").append("\n");
		stringBuilder.append(parent.getId() + " -> " + getId()).append("\n");
		if(trueNode != null){
			stringBuilder.append(getId()+" -> "+trueNode.getId()+ " [label=\"true values\"]").append("\n");

		}
		if(falseNode != null){
			stringBuilder.append(getId()+" -> "+falseNode.getId()+ " [label=\"false values\"]").append("\n");

		}

		parent.getPlanAsGraphvizDot(stringBuilder);


	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName();
	}

	public String getId() {
		return System.identityHashCode(this) + "";
	}


	private String leadingSpace() {
		return StringUtils.leftPad("", parent.depth() + 1, "    ");
	}

	@Override
	public void init() {
		if (iterator == null) {
			iterator = iterator();
		}
	}


	@Override
	public void close() {
		if (
			(trueNode == null || trueNode.isClosed()) &&
				(falseNode == null || falseNode.isClosed())
		) {
			iterator.close();
			iterator = null;
		}

	}

	@Override
	public boolean incrementIterator() {
		if (iterator.hasNext()) {
			iterator.next();
			return true;
		}
		return false;
	}

	@Override
	public int depth() {
		return parent.depth() + 1;
	}

	@Override
	public IteratorData getIteratorDataType() {
		return parent.getIteratorDataType();
	}
}
