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

/**
 * @author Håvard Ottestad
 *
 * This inner join algorithm is left-based, so it doesn't do a full inner join
 *
 */
public class InnerJoin implements PlanNode {

	PlanNode left;
	PlanNode right;

	PushBasedPlanNode discardedLeft;
	PushBasedPlanNode discardedRight;

	public InnerJoin(PlanNode left, PlanNode right, PushBasedPlanNode discardedLeft, PushBasedPlanNode discardedRight) {
		this.left = left;
		this.right = right;
		this.discardedLeft = discardedLeft;
		this.discardedRight = discardedRight;
		if(discardedLeft instanceof SupportsDepthProvider){
			((SupportsDepthProvider) discardedLeft).receiveDepthProvider(new DepthProvider() {
				@Override
				public int depth() {
					return Math.max(left.depth(), right.depth())+1;
				}
			});
		}
		if(discardedRight instanceof SupportsDepthProvider){
			((SupportsDepthProvider) discardedRight).receiveDepthProvider(new DepthProvider() {
				@Override
				public int depth() {
					return Math.max(left.depth(), right.depth())+1;
				}
			});
		}
	}

	@Override
	public CloseableIteration<Tuple, SailException> iterator() {
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
					if (discardedRight != null && nextRight != null) {
						discardedRight.push(nextRight);
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

}
