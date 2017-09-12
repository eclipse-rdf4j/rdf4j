/*******************************************************************************
 * Copyright (c) 2016 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.plan;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.sail.SailException;

import java.util.ArrayList;

/**
 * @author Heshan Jayasinghe
 */
public class OuterLeftJoin implements PlanNode {

	PlanNode left;

	PlanNode right;

	ArrayList<Tuple> tuplelist = new ArrayList<Tuple>();

	public OuterLeftJoin(PlanNode left, PlanNode right) {
		this.left = left;
		this.right = right;
	}

	@Override
	public CloseableIteration<Tuple, SailException> iterator() {
		CloseableIteration<Tuple, SailException> leftIterator = left.iterator();
		while (leftIterator.hasNext()) {
			Tuple leftNext = leftIterator.next();
			CloseableIteration<Tuple, SailException> rightIterator = right.iterator();
			boolean hasProperty = false;
			if (rightIterator.hasNext()) {
				while (rightIterator.hasNext()) {
					Tuple rightNext = rightIterator.next();
					if (leftNext.line.get(0).stringValue().equals(rightNext.line.get(0).stringValue())) {
						Tuple tuple = new Tuple();
						tuple.line.addAll(leftNext.getlist());
						tuple.line.addAll(rightNext.getlist());
						tuplelist.add(tuple);
						hasProperty = true;
					}
					else if (!rightIterator.hasNext() && !hasProperty) {
						Tuple tuple = new Tuple();
						tuple.line.addAll(leftNext.getlist());
						tuplelist.add(tuple);
					}
				}
			}
			else {
				Tuple tuple = new Tuple();
				tuple.line.addAll(leftNext.getlist());
				tuplelist.add(tuple);
			}
		}

		return new CloseableIteration<Tuple, SailException>() {

			int counter = 0;

			@Override
			public void close()
					throws SailException
			{

			}

			@Override
			public boolean hasNext()
					throws SailException
			{
				return tuplelist.size() > counter;
			}

			@Override
			public Tuple next()
					throws SailException
			{
				Tuple tuple = tuplelist.get(counter);
				counter++;
				return tuple;
			}

			@Override
			public void remove()
					throws SailException
			{

			}

		};
	}

	@Override
	public boolean validate() {
		return false;
	}

	@Override
	public int getCardinalityMin() {
		return left.getCardinalityMin();
	}

	@Override
	public int getCardinalityMax() {
		return left.getCardinalityMax() + right.getCardinalityMax();
	}
}
