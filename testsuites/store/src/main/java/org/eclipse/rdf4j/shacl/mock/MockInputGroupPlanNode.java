/*******************************************************************************
 * Copyright (c) 2016 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.shacl.mock;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.plan.GroupPlanNode;
import org.eclipse.rdf4j.plan.Tuple;
import org.eclipse.rdf4j.sail.SailException;

import java.util.Iterator;
import java.util.List;

/**
 * Created by havardottestad on 22/08/2017.
 */
public class MockInputGroupPlanNode implements GroupPlanNode{

	List<List<Tuple>> initialData;

	public MockInputGroupPlanNode(List<List<Tuple>> initialData) {
		this.initialData = initialData;
	}

	@Override
	public boolean validate() {
		return false;
	}

	@Override
	public CloseableIteration<List<Tuple>, SailException> iterator() {
		return new CloseableIteration<List<Tuple>, SailException>() {
			Iterator<List<Tuple>> iterator = initialData.iterator();

			@Override
			public void close() throws SailException {
			}

			@Override
			public boolean hasNext() throws SailException {
				return iterator.hasNext();
			}

			@Override
			public List<Tuple> next() throws SailException {
				return iterator.next();
			}

			@Override
			public void remove() throws SailException {

			}
		};
	}

}
