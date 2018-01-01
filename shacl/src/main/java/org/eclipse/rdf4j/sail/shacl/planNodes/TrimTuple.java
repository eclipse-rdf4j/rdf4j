package org.eclipse.rdf4j.sail.shacl.planNodes;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.shacl.plan.PlanNode;
import org.eclipse.rdf4j.sail.shacl.plan.Tuple;

public class TrimTuple implements PlanNode {

	PlanNode parent;
	int newLength;

	public TrimTuple(PlanNode parent, int newLength) {
		this.parent = parent;
		this.newLength = newLength;
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
				return parentIterator.hasNext();
			}

			@Override
			public Tuple next() throws SailException {

				Tuple next = parentIterator.next();

				Tuple tuple = new Tuple();

				for(int i = 0; i<newLength && i<next.line.size(); i++){
					tuple.line.add(next.line.get(i));
				}

				return tuple;
			}

			@Override
			public void remove() throws SailException {

			}
		};


	}

	@Override
	public int depth() {
		return parent.depth()+1;
	}
}
