package org.eclipse.rdf4j.sail.shacl.planNodes;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.shacl.plan.Tuple;

public interface PushBasedPlanNode {

	 void push(Tuple t);

	 void parentIterator(CloseableIteration<Tuple, SailException> iterator);

}
