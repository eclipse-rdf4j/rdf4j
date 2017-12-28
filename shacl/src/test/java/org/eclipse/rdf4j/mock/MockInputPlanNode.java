/*******************************************************************************
 * Copyright (c) 2016 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.mock;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.plan.PlanNode;
import org.eclipse.rdf4j.plan.Tuple;
import org.eclipse.rdf4j.sail.SailException;

import java.util.Iterator;
import java.util.List;

/**
 * @author Håvard Ottestad
 */
public class MockInputPlanNode implements PlanNode {

	List<Tuple> initialData;

	public MockInputPlanNode(List<Tuple> initialData) {
		this.initialData = initialData;
	}

	@Override
	public boolean validate() {
		return false;
	}

	@Override
	public CloseableIteration<Tuple, SailException> iterator() {
		return new CloseableIteration<Tuple, SailException>() {

			Iterator<Tuple> iterator = initialData.iterator();

			@Override
			public void close()
					throws SailException
			{
			}

			@Override
			public boolean hasNext()
					throws SailException
			{
				return iterator.hasNext();
			}

			@Override
			public Tuple next()
					throws SailException
			{
				return iterator.next();
			}

			@Override
			public void remove()
					throws SailException
			{

			}
		};
	}

	@Override
	public int getCardinalityMin() {
		return 0;
	}

	@Override
	public int getCardinalityMax() {
		return 0;
	}
}
