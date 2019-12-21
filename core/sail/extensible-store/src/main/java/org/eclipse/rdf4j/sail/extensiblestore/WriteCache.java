package org.eclipse.rdf4j.sail.extensiblestore;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.sail.SailException;

import javax.swing.plaf.nimbus.State;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class WriteCache implements DataStructureInterface {

	private final DataStructureInterface delegate;

	private List<Statement> added = new ArrayList<>();
	private List<Statement> removed = new ArrayList<>();

	public WriteCache(DataStructureInterface delegate) {
		this.delegate = delegate;
	}

	@Override
	public void addStatement(Statement statement) {
		if (!removed.isEmpty()) {
			flushForReading();
		}
		added.add(statement);
	}

	@Override
	public void removeStatement(Statement statement) {
		if (!added.isEmpty()) {
			flushForReading();
		}
		removed.add(statement);
	}

	@Override
	public void addStatement(Collection<Statement> statements) {
		if (!removed.isEmpty()) {
			flushForReading();
		}
		added.addAll(statements);
	}

	@Override
	public void removeStatement(Collection<Statement> statements) {
		if (!added.isEmpty()) {
			flushForReading();
		}
		removed.addAll(statements);
	}

	@Override
	public CloseableIteration<? extends Statement, SailException> getStatements(Resource subject, IRI predicate,
			Value object, Resource... context) {
		flushForReading();
		return delegate.getStatements(subject, predicate, object, context);
	}

	@Override
	public void flushForReading() {
		if (!added.isEmpty()) {
			delegate.addStatement(added);
		}
		if (!removed.isEmpty()) {
			delegate.addStatement(removed);
		}
		if (!added.isEmpty() || !removed.isEmpty()) {
			delegate.flushForReading();
		}

		if (!added.isEmpty()) {
			added = new ArrayList<>();
		}
		if (!removed.isEmpty()) {
			removed = new ArrayList<>();
		}

	}

	@Override
	public void init() {
		delegate.init();
	}

	@Override
	public void clear(Resource[] contexts) {
		flushForReading();
		delegate.clear(contexts);
	}

	@Override
	public void flushForCommit() {
		flushForReading();
		delegate.flushForCommit();
	}

	@Override
	public boolean removeStatementsByQuery(Resource subj, IRI pred, Value obj, Resource[] contexts) {
		flushForReading();
		return delegate.removeStatementsByQuery(subj, pred, obj, contexts);
	}
}
