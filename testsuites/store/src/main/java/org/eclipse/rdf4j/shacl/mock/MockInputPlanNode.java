package org.eclipse.rdf4j.shacl.mock;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.plan.PlanNode;
import org.eclipse.rdf4j.plan.Tuple;
import org.eclipse.rdf4j.sail.SailException;

import java.util.Iterator;
import java.util.List;

/**
 * Created by havardottestad on 22/08/2017.
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
			public void close() throws SailException {
			}

			@Override
			public boolean hasNext() throws SailException {
				return iterator.hasNext();
			}

			@Override
			public Tuple next() throws SailException {
				return iterator.next();
			}

			@Override
			public void remove() throws SailException {

			}
		};
	}
}
