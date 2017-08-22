package org.eclipse.rdf4j.shacl.mock;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.plan.PlanNode;
import org.eclipse.rdf4j.plan.Tuple;
import org.eclipse.rdf4j.sail.SailException;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by havardottestad on 22/08/2017.
 */
public class MockConsumePlanNode {

	PlanNode innerNode;

	public MockConsumePlanNode(PlanNode innerNode) {
		this.innerNode = innerNode;
	}


	public List<Tuple> asList() {

		CloseableIteration<Tuple, SailException> iterator = innerNode.iterator();

		List<Tuple> ret = new ArrayList<>();

		while (iterator.hasNext()) {
			ret.add(iterator.next());
		}

		return ret;

	}
}
