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
public class LeftOuterJoin implements PlanNode {

	private PlanNode left;
	private PlanNode right;
	private boolean printed = false;

	public LeftOuterJoin(PlanNode left, PlanNode right) {
		this.left = left;
		this.right = right;
	}

	@Override
	public CloseableIteration<Tuple, SailException> iterator() {
		return new CloseableIteration<Tuple, SailException>() {


			CloseableIteration<Tuple, SailException> leftIterator = left.iterator();
			CloseableIteration<Tuple, SailException> rightIterator = right.iterator();

			Tuple next;
			Tuple nextLeft;
			Tuple nextRight;
			Tuple prevLeft;

			void calculateNext() {
				if (next != null) {
					return;
				}

				if (nextLeft == null && leftIterator.hasNext()) {
					nextLeft = leftIterator.next();
				}

				if (nextLeft == null) {
					return;
				}

				if (nextRight == null && rightIterator.hasNext()) {
					nextRight = rightIterator.next();
				}


				while (next == null) {


					if (nextRight != null) {

						if (nextLeft.line.get(0) == nextRight.line.get(0) || nextLeft.line.get(0).equals(nextRight.line.get(0))) {
							next = TupleHelper.join(nextLeft, nextRight);
							prevLeft = nextLeft;
							nextRight = null;
						} else {


							int compareTo = nextLeft.compareTo(nextRight);

							if (compareTo < 0) {
								if (prevLeft != nextLeft) {
									prevLeft = nextLeft;
									next = nextLeft;
								} else if (leftIterator.hasNext()) {
									nextLeft = leftIterator.next();
								} else {
									nextLeft = null;
									break;
								}
							} else {
								if (rightIterator.hasNext()) {
									nextRight = rightIterator.next();
								} else {
									nextRight = null;
									break;
								}
							}

						}
					} else {
						if (prevLeft == nextLeft) {
							if (leftIterator.hasNext()) {
								nextLeft = leftIterator.next();
							} else {
								break;
							}
						} else {
							prevLeft = nextLeft;
							next = nextLeft;
						}

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
		return "LeftOuterJoin";
	}
}
