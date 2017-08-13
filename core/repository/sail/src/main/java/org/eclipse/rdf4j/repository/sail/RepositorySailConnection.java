package org.eclipse.rdf4j.repository.sail;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.rdf4j.IsolationLevel;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.sail.NotifyingSailConnection;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.SailConnectionListener;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.UnknownSailTransactionStateException;
import org.eclipse.rdf4j.sail.UpdateContext;

public class RepositorySailConnection implements SailConnection, NotifyingSailConnection {

	private final List<SailConnectionListener> listeners = new ArrayList<SailConnectionListener>();

	private final RepositoryConnection conn;

	public RepositorySailConnection(final RepositoryConnection conn) {
		this.conn = conn;
	}

	@Override
	public boolean isOpen()
		throws SailException
	{
		return conn.isOpen();
	}

	@Override
	public void close()
		throws SailException
	{
		if (this.isOpen())
			conn.close();
	}

	@Override
	public CloseableIteration<? extends BindingSet, QueryEvaluationException> evaluate(TupleExpr tupleExpr,
			Dataset dataset, BindingSet bindings, boolean includeInferred)
		throws SailException
	{
		// FIXME: check if this method has meaning here
		throw new SailException("not supported method!");
	}

	@Override
	public CloseableIteration<? extends Resource, SailException> getContextIDs()
		throws SailException
	{
		final SailResult<Resource> it = new SailResult<>();
		final RepositoryResult<Resource> ids = conn.getContextIDs();
		while (ids.hasNext()) {
			it.add(ids.next());
		}
		return it;
	}

	@Override
	public CloseableIteration<? extends Statement, SailException> getStatements(Resource subj, IRI pred,
			Value obj, boolean includeInferred, Resource... contexts)
		throws SailException
	{
		final SailResult<Statement> it = new SailResult<>();
		final RepositoryResult<Statement> statements = conn.getStatements(subj, pred, obj, contexts);
		while (statements.hasNext()) {
			it.add(statements.next());
		}
		return it;
	}

	@Override
	public long size(final Resource... contexts)
		throws SailException
	{
		return conn.size(contexts);
	}

	@Override
	public void begin()
		throws SailException
	{
		conn.begin();
	}

	@Override
	public void begin(final IsolationLevel level)
		throws UnknownSailTransactionStateException, SailException
	{
		conn.begin(level);
	}

	@Override
	public void flush()
		throws SailException
	{
		// FIXME: see how to implement this one
		throw new SailException("not implemented");
	}

	@Override
	public void prepare()
		throws SailException
	{
		// FIXME: see how to implement this one
		throw new SailException("not implemented");
	}

	@Override
	public void commit()
		throws SailException
	{
		conn.commit();
	}

	@Override
	public void rollback()
		throws SailException
	{
		conn.rollback();
	}

	@Override
	public boolean isActive()
		throws UnknownSailTransactionStateException
	{
		return conn.isActive();
	}

	@Override
	public void addStatement(final Resource subj, final IRI pred, final Value obj, final Resource... contexts)
		throws SailException
	{
		conn.add(subj, pred, obj, contexts);
	}

	@Override
	public void removeStatements(final Resource subj, final IRI pred, final Value obj,
			final Resource... contexts)
		throws SailException
	{
		conn.remove(subj, pred, obj, contexts);
	}

	@Override
	public void startUpdate(final UpdateContext op)
		throws SailException
	{
		// FIXME: see how to implement this one
		throw new SailException("not implemented");
	}

	@Override
	public void addStatement(final UpdateContext op, final Resource subj, final IRI pred, final Value obj,
			final Resource... contexts)
		throws SailException
	{
		// FIXME: how can we use the UpdateContext here?
		this.addStatement(subj, pred, obj, contexts);
	}

	@Override
	public void removeStatement(final UpdateContext op, final Resource subj, final IRI pred, final Value obj,
			final Resource... contexts)
		throws SailException
	{
		// FIXME: check how UpdateContext should be used here
		this.removeStatements(subj, pred, obj, contexts);
	}

	@Override
	public void endUpdate(UpdateContext op)
		throws SailException
	{
		// FIXME: see how to implement this one
		throw new SailException("not implemented");
	}

	@Override
	public void clear(final Resource... contexts)
		throws SailException
	{
		conn.clear(contexts);
	}

	@Override
	public CloseableIteration<Namespace, SailException> getNamespaces()
		throws SailException
	{
		final SailResult<Namespace> list = new SailResult<Namespace>();
		final RepositoryResult<Namespace> results = conn.getNamespaces();
		while (results.hasNext()) {
			list.add(results.next());
		}
		return list;
	}

	@Override
	public String getNamespace(final String prefix)
		throws SailException
	{
		return conn.getNamespace(prefix);
	}

	@Override
	public void setNamespace(final String prefix, final String name)
		throws SailException
	{
		conn.setNamespace(prefix, name);
	}

	@Override
	public void removeNamespace(final String prefix)
		throws SailException
	{
		conn.removeNamespace(prefix);
	}

	@Override
	public void clearNamespaces()
		throws SailException
	{
		conn.clearNamespaces();
	}

	@Override
	public void addConnectionListener(final SailConnectionListener listener) {
		listeners.add(listener);
	}

	@Override
	public void removeConnectionListener(final SailConnectionListener listener) {
		listeners.remove(listener);
	}

}
