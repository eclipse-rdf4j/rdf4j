package org.eclipse.rdf4j.sail.shacl.planNodes;

import org.apache.commons.text.StringEscapeUtils;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.sail.SailException;

public class DebugPlanNode implements PlanNode {

	private final String message;
	PlanNode parent;
	private boolean printed;
	private ValidationExecutionLogger validationExecutionLogger;

	public DebugPlanNode(PlanNode parent, String message) {
		this.parent = parent;
		this.message = message;
	}

	@Override
	public CloseableIteration<Tuple, SailException> iterator() {

		DebugPlanNode that = this;

		return new CloseableIteration<Tuple, SailException>() {

			final CloseableIteration<Tuple, SailException> iterator = parent.iterator();

			@Override
			public boolean hasNext() throws SailException {
				return iterator.hasNext();
			}

			@Override
			public Tuple next() throws SailException {
				Tuple next = iterator.next();
				validationExecutionLogger.log(depth(), message, next, that, getId());
				return next;
			}

			@Override
			public void remove() throws SailException {
				iterator.remove();
			}

			@Override
			public void close() throws SailException {
				iterator.close();
			}
		};

	}

	@Override
	public int depth() {
		return parent.depth() + 1;
	}

	@Override
	public void getPlanAsGraphvizDot(StringBuilder stringBuilder) {
		if (printed) {
			return;
		}
		printed = true;
		stringBuilder.append(getId() + " [label=\"" + StringEscapeUtils.escapeJava(this.toString()) + "\"];")
				.append("\n");
		stringBuilder.append(parent.getId() + " -> " + getId()).append("\n");
		parent.getPlanAsGraphvizDot(stringBuilder);
	}

	@Override
	public String getId() {
		return System.identityHashCode(this) + "";
	}

	@Override
	public String toString() {
		return "DebugPlanNode";
	}

	@Override
	public IteratorData getIteratorDataType() {
		return parent.getIteratorDataType();
	}

	@Override
	public void receiveLogger(ValidationExecutionLogger validationExecutionLogger) {
		this.validationExecutionLogger = validationExecutionLogger;
		parent.receiveLogger(validationExecutionLogger);
	}
}
