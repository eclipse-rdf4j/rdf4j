package org.eclipse.rdf4j.sail.shacl.planNodes;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.sail.SailException;

import java.util.ArrayDeque;
import java.util.Queue;

public class AutoBufferingPlanNode implements PlanNode {

	private InnerJoin innerJoin;

	Queue<Tuple> buffer = new ArrayDeque<>();


	public AutoBufferingPlanNode(InnerJoin innerJoin) {
		this.innerJoin = innerJoin;
	}

	@Override
	public CloseableIteration<Tuple, SailException> iterator() {
		return new CloseableIteration<Tuple, SailException>() {

			{
				innerJoin.init();
			}

			@Override
			public void close() throws SailException {
				innerJoin.close();
			}

			@Override
			public boolean hasNext() throws SailException {
				calculateNext();
				return !buffer.isEmpty();
			}

			private void calculateNext() {
				while(buffer.isEmpty()){
					boolean success = innerJoin.incrementIterator();
					if(!success) break;
				}
			}

			@Override
			public Tuple next() throws SailException {
				return buffer.remove();
			}

			@Override
			public void remove() throws SailException {

			}
		};
	}

	@Override
	public int depth() {
		return innerJoin.depth();
	}

	@Override
	public void getPlanAsGraphvizDot(StringBuilder stringBuilder) {

	}

	@Override
	public String getId() {
		return innerJoin.getId();
	}

	@Override
	public IteratorData getIteratorDataType() {
		return innerJoin.getIteratorDataType();
	}

	public void push(Tuple next) {
		buffer.add(next);
	}
}
