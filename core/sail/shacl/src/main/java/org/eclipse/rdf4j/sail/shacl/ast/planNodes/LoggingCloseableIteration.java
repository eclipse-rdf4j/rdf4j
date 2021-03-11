package org.eclipse.rdf4j.sail.shacl.ast.planNodes;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.shacl.GlobalValidationExecutionLogging;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class LoggingCloseableIteration implements CloseableIteration<ValidationTuple, SailException> {

	private final ValidationExecutionLogger validationExecutionLogger;
	private final PlanNode planNode;
	private final Logger logger = LoggerFactory.getLogger(getClass());
	private boolean empty = false;

	public LoggingCloseableIteration(PlanNode planNode, ValidationExecutionLogger validationExecutionLogger) {
		this.planNode = planNode;
		this.validationExecutionLogger = validationExecutionLogger;
	}

	static String leadingSpace(PlanNode planNode) {
		return leadingSpace(planNode.depth());
	}

	static String leadingSpace(int depth) {
		StringBuilder builder = new StringBuilder();
		for (int i = depth; i > 0; i--) {
			builder.append("  ");
		}
		return builder.toString();
	}

	@Override
	public final ValidationTuple next() throws SailException {

		ValidationTuple tuple = loggingNext();

		if (GlobalValidationExecutionLogging.loggingEnabled) {
			validationExecutionLogger.log(planNode.depth(), planNode.getClass().getSimpleName() + ".next()", tuple,
					planNode, planNode.getId(), null);
		}
		return tuple;
	}

	@Override
	public final boolean hasNext() throws SailException {
		boolean hasNext = localHasNext();
		if (!hasNext)
			empty = true;
		assert !hasNext || !empty : this.getClass();
		return hasNext;
	}

	protected abstract ValidationTuple loggingNext() throws SailException;

	protected abstract boolean localHasNext() throws SailException;

	private String leadingSpace() {
		return leadingSpace(planNode);
	}

	/**
	 * A default method since the iterators in the ShaclSail don't support remove.
	 *
	 * @throws SailException
	 */
	@Override
	public void remove() throws SailException {
		throw new UnsupportedOperationException();
	}
}
