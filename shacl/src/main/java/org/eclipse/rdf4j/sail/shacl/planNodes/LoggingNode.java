package org.eclipse.rdf4j.sail.shacl.planNodes;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.shacl.plan.PlanNode;
import org.eclipse.rdf4j.sail.shacl.plan.Tuple;

public class LoggingNode implements PlanNode{

	PlanNode parent;

	public LoggingNode(PlanNode parent) {
		this.parent = parent;
	}

	@Override
	public CloseableIteration<Tuple, SailException> iterator() {
		return new CloseableIteration<Tuple, SailException>() {


				CloseableIteration<Tuple, SailException> parentIterator = parent.iterator();


			@Override
			public void close() throws SailException {
				parentIterator.close();
			}

			@Override
			public boolean hasNext() throws SailException {
				boolean hasNext = parentIterator.hasNext();

				System.out.println(parent.getClass().getSimpleName()+".hasNext() : "+hasNext);
				return hasNext;
			}

			@Override
			public Tuple next() throws SailException {
				System.out.println(parent.getClass().getSimpleName()+".next()");

				Tuple next = parentIterator.next();
				System.out.println("\t"+next.toString());
				return next;
			}

			@Override
			public void remove() throws SailException {

			}
		};
	}
}
