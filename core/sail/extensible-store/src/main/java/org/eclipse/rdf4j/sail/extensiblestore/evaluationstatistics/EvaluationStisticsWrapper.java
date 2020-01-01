package org.eclipse.rdf4j.sail.extensiblestore.evaluationstatistics;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.extensiblestore.DataStructureInterface;

public class EvaluationStisticsWrapper implements DataStructureInterface {

	private final DynamicStatistics dynamicStatistics;
	private final boolean inferred;
	private final DataStructureInterface delegate;

	public EvaluationStisticsWrapper(DataStructureInterface delegate, DynamicStatistics dynamicStatistics,
			boolean inferred) {
		this.delegate = delegate;
		this.dynamicStatistics = dynamicStatistics;
		this.inferred = inferred;
	}

	@Override
	public void addStatement(Statement statement) {
		delegate.addStatement(statement);
		dynamicStatistics.add(statement, inferred);
	}

	@Override
	public void removeStatement(Statement statement) {
		delegate.removeStatement(statement);
		dynamicStatistics.remove(statement, inferred);

	}

	@Override
	public CloseableIteration<? extends Statement, SailException> getStatements(Resource subject, IRI predicate,
			Value object, Resource... context) {
		return delegate.getStatements(subject, predicate, object, context);
	}

	@Override
	public void flushForReading() {
		delegate.flushForReading();
	}

	@Override
	public void init() {
		delegate.init();
	}

	@Override
	public void clear(Resource[] contexts) {
		delegate.clear(contexts);
	}

	@Override
	public void flushForCommit() {
		delegate.flushForCommit();
	}

	@Override
	public boolean removeStatementsByQuery(Resource subj, IRI pred, Value obj, Resource[] contexts) {
		dynamicStatistics.removeByQuery(subj, pred, obj, inferred, contexts);
		return delegate.removeStatementsByQuery(subj, pred, obj, contexts);
	}
}
