package org.eclipse.rdf4j.sail.shacl.planNodes;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.shacl.plan.PlanNode;
import org.eclipse.rdf4j.sail.shacl.plan.Tuple;

public abstract class FilterPlanNode <T extends PushBasedPlanNode & SupportsDepthProvider> implements DepthProvider{

	PlanNode parent;

	T trueNode;
	T falseNode;


	abstract boolean checkTuple(Tuple t);

	public FilterPlanNode(PlanNode parent, T trueNode, T falseNode) {
		this.parent = parent;
		this.trueNode = trueNode;
		this.falseNode = falseNode;

		CloseableIteration<Tuple, SailException> iterator = iterator();

		if (trueNode != null) {
			trueNode.parentIterator(iterator);
			trueNode.receiveDepthProvider(this);
		}

		if (falseNode != null) {
			falseNode.parentIterator(iterator);
			falseNode.receiveDepthProvider(this);
		}

	}

	private CloseableIteration<Tuple, SailException> iterator() {
		return new CloseableIteration<Tuple, SailException>() {

			CloseableIteration<Tuple, SailException> parentIterator = parent.iterator();

			Tuple next;

			private void calculateNext() {
				if (next != null) {
					return;
				}

				while (parentIterator.hasNext() && next == null) {
					Tuple temp = parentIterator.next();

					if (checkTuple(temp)) {
						if (trueNode != null) {
							trueNode.push(temp);
						}
					} else {
						if (falseNode != null) {
							falseNode.push(temp);
						}
					}

					next = temp;

				}

			}

			boolean closed = false;

			@Override
			public void close() throws SailException {
				if (closed) {
					return;
				}

				closed = true;
				parentIterator.close();
			}

			@Override
			public boolean hasNext() throws SailException {
				calculateNext();
				if (next == null) {
					close();
				}
				return next != null;
			}

			@Override
			public Tuple next() throws SailException {
				Tuple temp = next;
				next = null;
				return temp;
			}

			@Override
			public void remove() throws SailException {

			}
		};
	}

	public int depth() {
		return parent.depth() + 1;
	}


}
