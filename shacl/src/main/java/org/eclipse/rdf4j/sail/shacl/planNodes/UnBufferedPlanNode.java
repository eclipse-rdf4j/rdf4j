package org.eclipse.rdf4j.sail.shacl.planNodes;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.sail.SailException;

import java.util.ArrayDeque;
import java.util.Queue;

public class UnBufferedPlanNode<T extends PlanNode & MultiStreamPlanNode> implements PushablePlanNode {

	private T parent;

	Tuple next;

	UnBufferedPlanNode(T parent) {
		this.parent = parent;
	}

	@Override
	public CloseableIteration<Tuple, SailException> iterator() {
		return new CloseableIteration<Tuple, SailException>() {

			{
				parent.init();
			}

			@Override
			public void close() throws SailException {
				parent.close();
			}

			@Override
			public boolean hasNext() throws SailException {
				calculateNext();
				return next != null;
			}

			private void calculateNext() {
				while(next == null){
					boolean success = parent.incrementIterator();
					if(!success) break;
				}
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
		return parent.depth();
	}

	@Override
	public void getPlanAsGraphvizDot(StringBuilder stringBuilder) {

	}

	@Override
	public String getId() {
		return parent.getId();
	}

	@Override
	public IteratorData getIteratorDataType() {
		return parent.getIteratorDataType();
	}

	@Override
	public void push(Tuple next) {
		this.next = next;
	}
}
