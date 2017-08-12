package org.eclipse.rdf4j.repository.sail;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.rdf4j.IsolationLevel;
import org.eclipse.rdf4j.common.iteration.AbstractCloseableIteration;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.CloseableIterationBase;
import org.eclipse.rdf4j.common.iteration.CloseableIteratorIteration;
import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
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

	private final RepositoryConnection conn;

	private final ValueFactory vf;

	public RepositorySailConnection(final RepositoryConnection conn) {
		this.conn = conn;
		this.vf = conn.getValueFactory();
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
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CloseableIteration<? extends Resource, SailException> getContextIDs()
		throws SailException
	{
		//		ResourceIterator<Resource> it = new ResourceIterator<>();
		//		RepositoryResult<Resource> ids = conn.getContextIDs();
		//		while (ids.hasNext()) {
		//			it.add(ids.next());
		//		}
		//		return it;
		return null;
	}

	@Override
	public CloseableIteration<? extends Statement, SailException> getStatements(Resource subj, IRI pred,
			Value obj, boolean includeInferred, Resource... contexts)
		throws SailException
	{
		// TODO Auto-generated method stub
		return null;
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
		// TODO Auto-generated method stub
	}

	@Override
	public void prepare()
		throws SailException
	{
		// TODO Auto-generated method stub
	}

	@Override
	public void commit()
		throws SailException
	{
		// TODO Auto-generated method stub
		conn.commit();
	}

	@Override
	public void rollback()
		throws SailException
	{
		// TODO Auto-generated method stub
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
		//		final Statement statement = vf.createStatement(subj, pred, obj);
		//		conn.add(statement, contexts);
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
	public void startUpdate(UpdateContext op)
		throws SailException
	{
		// TODO Auto-generated method stub
	}

	@Override
	public void addStatement(UpdateContext op, Resource subj, IRI pred, Value obj, Resource... contexts)
		throws SailException
	{
		// FIXME
		this.addStatement(subj, pred, obj, contexts);
	}

	@Override
	public void removeStatement(UpdateContext op, Resource subj, IRI pred, Value obj, Resource... contexts)
		throws SailException
	{
		// FIXME
		this.removeStatements(subj, pred, obj, contexts);
	}

	@Override
	public void endUpdate(UpdateContext op)
		throws SailException
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void clear(final Resource... contexts)
		throws SailException
	{
		conn.clear(contexts);
	}

	@Override
	public CloseableIteration<? extends Namespace, SailException> getNamespaces()
		throws SailException
	{
		// TODO Auto-generated method stub
		return null;
		//		conn.getNamespaces()
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
	public void addConnectionListener(SailConnectionListener listener) {
		// TODO Auto-generated method stub

	}

	@Override
	public void removeConnectionListener(SailConnectionListener listener) {
		// TODO Auto-generated method stub

	}

}

class ResourceIterator<E> extends AbstractCloseableIteration<E, Exception> {

	private final List<E> list = new ArrayList<>();

	private Iterator<E> it;

	public ResourceIterator() {
		it = list.iterator();
	}

	public void add(final E element) {
		list.add(element);
		it = list.iterator();
	}

	@Override
	public boolean hasNext()
		throws Exception
	{
		return it.hasNext();
	}

	@Override
	public E next()
		throws Exception
	{
		return it.next();
	}

	@Override
	public void remove()
		throws Exception
	{
		it.remove();
	}

}
