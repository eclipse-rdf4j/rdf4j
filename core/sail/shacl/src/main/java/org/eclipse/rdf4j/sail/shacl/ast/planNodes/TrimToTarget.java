package org.eclipse.rdf4j.sail.shacl.ast.planNodes;

import java.util.Objects;

import org.apache.commons.text.StringEscapeUtils;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.sail.SailException;

public class TrimToTarget implements PlanNode {

	private StackTraceElement[] stackTrace;
	PlanNode parent;
	private boolean printed = false;
	private ValidationExecutionLogger validationExecutionLogger;

	boolean keepPath = false;

	public TrimToTarget(PlanNode parent) {
		parent = PlanNodeHelper.handleSorting(this, parent);
		this.parent = parent;
//		this.stackTrace = Thread.currentThread().getStackTrace();
	}

	@Override
	public CloseableIteration<? extends ValidationTuple, SailException> iterator() {
		return new LoggingCloseableIteration(this, validationExecutionLogger) {

			final CloseableIteration<? extends ValidationTuple, SailException> parentIterator = parent.iterator();

			@Override
			public void close() throws SailException {
				parentIterator.close();
			}

			@Override
			protected boolean localHasNext() throws SailException {
				return parentIterator.hasNext();
			}

			@Override
			protected ValidationTuple loggingNext() throws SailException {

				return parentIterator.next().trimToTarget();

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
	public void receiveLogger(ValidationExecutionLogger validationExecutionLogger) {
		this.validationExecutionLogger = validationExecutionLogger;
		parent.receiveLogger(validationExecutionLogger);
	}

	@Override
	public boolean producesSorted() {
		return false;
	}

	@Override
	public boolean requiresSorted() {
		return false;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		TrimToTarget that = (TrimToTarget) o;
		return keepPath == that.keepPath &&
				parent.equals(that.parent);
	}

	@Override
	public int hashCode() {
		return Objects.hash(parent, keepPath);
	}

	@Override
	public String toString() {
		return "TrimToTarget{" +
				"parent=" + parent +
				", keepPath=" + keepPath +
				'}';
	}
}
