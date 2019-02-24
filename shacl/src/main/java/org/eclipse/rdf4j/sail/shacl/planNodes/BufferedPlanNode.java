package org.eclipse.rdf4j.sail.shacl.planNodes;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.sail.SailException;

import java.util.ArrayDeque;
import java.util.Queue;

public class BufferedPlanNode<T extends MultiStreamPlanNode & PlanNode> implements PushablePlanNode {

	private T parent;

	private Queue<Tuple> buffer = new ArrayDeque<>();


	BufferedPlanNode(T parent) {
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
				return !buffer.isEmpty();
			}

			private void calculateNext() {
				while(buffer.isEmpty()){
					boolean success = parent.incrementIterator();
					if(!success) break;
				}
			}

			@Override
			public Tuple next() throws SailException {
				calculateNext();
				return buffer.remove();
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
		buffer.add(next);
	}
}
