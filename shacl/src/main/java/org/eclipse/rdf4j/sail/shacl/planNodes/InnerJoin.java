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
 *
 * This inner join algorithm assumes the left iterator is unique for tuple[0], eg. no two tuples have the same value at index 0.
 * The right iterator is allowed to contain duplicates.
 *
 */
public class InnerJoin implements PlanNode, ParentProvider {

	static private final Logger logger = LoggerFactory.getLogger(InnerJoin.class);
	private boolean printed = false;


	private PlanNode left;
	private PlanNode right;

	private PushBasedPlanNode discardedLeft;
	private PushBasedPlanNode discardedRight;

	public InnerJoin(PlanNode left, PlanNode right, PushBasedPlanNode discardedLeft, PushBasedPlanNode discardedRight) {
		this.left = left;
		this.right = right;
		this.discardedLeft = discardedLeft;
		this.discardedRight = discardedRight;
		if(discardedLeft instanceof SupportsParentProvider){
			((SupportsParentProvider) discardedLeft).receiveParentProvider(this);
		}
		if(discardedRight instanceof SupportsParentProvider){
			((SupportsParentProvider) discardedRight).receiveParentProvider(this);
		}
	}

	@Override
	public List<PlanNode> parent() {
		return Arrays.asList(left, right);
	}

	@Override
	public CloseableIteration<Tuple, SailException> iterator() {

		InnerJoin that = this;
		return new CloseableIteration<Tuple, SailException>() {


			CloseableIteration<Tuple, SailException> leftIterator = left.iterator();
			CloseableIteration<Tuple, SailException> rightIterator = right.iterator();

			Tuple next;
			Tuple nextLeft;
			Tuple nextRight;

			void calculateNext() {
				if (next != null) {
					return;
				}

				if (nextLeft == null && leftIterator.hasNext()) {
					nextLeft = leftIterator.next();
				}


				if (nextRight == null && rightIterator.hasNext()) {
					nextRight = rightIterator.next();
				}

				if (nextLeft == null) {
					if (discardedRight != null) {
						while(nextRight != null){
							if(LoggingNode.loggingEnabled){
								logger.info(leadingSpace() + that.getClass().getSimpleName() + ";discardedRight: " + " " + nextRight.toString());
							}
							discardedRight.push(nextRight);
							if(rightIterator.hasNext()){
								nextRight = rightIterator.next();
							}else{
								nextRight = null;
							}
						}
					}
					return;
				}


				while (next == null) {
					if (nextRight != null) {

						if (nextLeft.line.get(0) == nextRight.line.get(0) || nextLeft.line.get(0).equals(nextRight.line.get(0))) {
							next = TupleHelper.join(nextLeft, nextRight);
							nextRight = null;
						} else {


							int compareTo = nextLeft.compareTo(nextRight);

							if (compareTo < 0) {
								if (discardedLeft != null) {
									if(LoggingNode.loggingEnabled){
										logger.info(leadingSpace() + that.getClass().getSimpleName() + ";discardedLeft: " + " " + nextLeft.toString());
									}
									discardedLeft.push(nextLeft);
								}
								if (leftIterator.hasNext()) {
									nextLeft = leftIterator.next();
								} else {
									nextLeft = null;
									break;
								}
							} else {
								if (discardedRight != null) {
									if(LoggingNode.loggingEnabled){
										logger.info(leadingSpace() + that.getClass().getSimpleName() + ";discardedRight: " + " " + nextRight.toString());
									}
									discardedRight.push(nextRight);
								}
								if (rightIterator.hasNext()) {
									nextRight = rightIterator.next();
								} else {
									nextRight = null;
									break;
								}
							}

						}
					} else {
						return;
					}
				}


			}

			@Override
			public void close() throws SailException {
				leftIterator.close();
				rightIterator.close();
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
		return Math.max(left.depth(), right.depth());
	}

	@Override
	public void getPlanAsGraphvizDot(StringBuilder stringBuilder) {
		if(printed) return;
		printed = true;
		left.getPlanAsGraphvizDot(stringBuilder);

		stringBuilder.append(getId() + " [label=\"" + StringEscapeUtils.escapeJava(this.toString()) + "\"];").append("\n");
		stringBuilder.append(left.getId()+" -> "+getId()+ " [label=\"left\"];").append("\n");
		stringBuilder.append(right.getId()+" -> "+getId()+ " [label=\"right\"];").append("\n");
		right.getPlanAsGraphvizDot(stringBuilder);

		if(discardedRight != null){
			if(discardedRight instanceof PlanNode){
				stringBuilder.append(getId()+" -> "+((PlanNode) discardedRight).getId()+ " [label=\"discardedRight\"];").append("\n");
			}

		}
		if(discardedLeft != null){
			if(discardedLeft instanceof PlanNode){
				stringBuilder.append(getId()+" -> "+((PlanNode) discardedLeft).getId()+ " [label=\"discardedLeft\"];").append("\n");
			}


		}
	}

	@Override
	public String getId() {
		return System.identityHashCode(this)+"";
	}

	@Override
	public IteratorData getIteratorDataType() {
		if(left.getIteratorDataType() == right.getIteratorDataType()) return left.getIteratorDataType();

		throw new IllegalStateException("Not implemented support for when left and right have different types of data");

	}

	@Override
	public String toString() {
		return "InnerJoin";
	}

	private String leadingSpace() {
		return StringUtils.leftPad("", depth(), "    ");
	}
}
