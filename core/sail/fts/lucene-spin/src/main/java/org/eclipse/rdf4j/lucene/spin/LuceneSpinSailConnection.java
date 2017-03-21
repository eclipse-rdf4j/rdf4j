/*******************************************************************************
 * Copyright (c) 2017 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.lucene.spin;

import java.io.IOException;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.sail.NotifyingSailConnection;
import org.eclipse.rdf4j.sail.SailConnectionListener;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.helpers.NotifyingSailConnectionWrapper;
import org.eclipse.rdf4j.sail.lucene.LuceneSailBuffer.AddRemoveOperation;
import org.eclipse.rdf4j.sail.lucene.LuceneSailBuffer.ClearContextOperation;
import org.eclipse.rdf4j.sail.lucene.LuceneSailBuffer.Operation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.rdf4j.sail.lucene.LuceneSailBuffer;
import org.eclipse.rdf4j.sail.lucene.LuceneSailConnection;
import org.eclipse.rdf4j.sail.lucene.SearchIndex;

/**
 * This connection inherits Lucene index supporting methods from {@link LuceneSailConnection}.
 * 
 * @author sauermann
 * @author christian.huetter
 * @author jacek grzebyta
 */
public class LuceneSpinSailConnection extends NotifyingSailConnectionWrapper {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	private final SearchIndex luceneIndex;

	/**
	 * the buffer that collects operations
	 */
	final private LuceneSailBuffer buffer = new LuceneSailBuffer();

	/**
	 * The listener that listens to the underlying connection. It is disabled during clearContext operations.
	 */
	protected final SailConnectionListener connectionListener = new SailConnectionListener() {

		@Override
		public void statementAdded(Statement statement) {
			// we only consider statements that contain literals
			if (statement.getObject() instanceof Literal) {
				if (statement == null)
					return;
				// we further only index statements where the Literal's datatype is
				// accepted
				Literal literal = (Literal)statement.getObject();
				if (luceneIndex.accept(literal))
					buffer.add(statement);
			}
		}

		@Override
		public void statementRemoved(Statement statement) {
			// we only consider statements that contain literals
			if (statement.getObject() instanceof Literal) {
				if (statement == null)
					return;
				// we further only indexed statements where the Literal's datatype
				// is accepted
				Literal literal = (Literal)statement.getObject();
				if (luceneIndex.accept(literal))
					buffer.remove(statement);
			}
		}
	};

	/**
	 * To remember if the iterator was already closed and only free resources once
	 */
	private final AtomicBoolean closed = new AtomicBoolean(false);

	public LuceneSpinSailConnection(NotifyingSailConnection wrappedConnection, SearchIndex luceneIndex) {
		super(wrappedConnection);
		this.luceneIndex = luceneIndex;

		/*
		 * Using SailConnectionListener, see <a href="#whySailConnectionListener">above</a>
		 */

		wrappedConnection.addConnectionListener(connectionListener);
	}

	@Override
	public synchronized void addStatement(Resource arg0, IRI arg1, Value arg2, Resource... arg3)
		throws SailException
	{
		super.addStatement(arg0, arg1, arg2, arg3);
	}

	@Override
	public void close()
		throws SailException
	{
		if (closed.compareAndSet(false, true)) {
			try {
				super.close();
			}
			finally {
				try {
					luceneIndex.endReading();
				}
				catch (IOException e) {
					logger.warn("could not close IndexReader or IndexSearcher " + e, e);
				}
				// remember if you were closed before, some sloppy programmers
				// may call close() twice.
			}
		}
	}

	// //////////////////////////////// Methods related to indexing

	@Override
	public synchronized void clear(Resource... arg0)
		throws SailException
	{
		// remove the connection listener, this is safe as the changing methods
		// are synchronized
		// during the clear(), no other operation can be invoked
		getWrappedConnection().removeConnectionListener(connectionListener);
		try {
			super.clear(arg0);
			buffer.clear(arg0);
		}
		finally {
			getWrappedConnection().addConnectionListener(connectionListener);
		}
	}

	@Override
	public void begin()
		throws SailException
	{
		super.begin();
		buffer.reset();
		try {
			luceneIndex.begin();
		}
		catch (IOException e) {
			throw new SailException(e);
		}
	}

	@Override
	public void commit()
		throws SailException
	{
		super.commit();

		logger.debug("Committing Lucene transaction with {} operations.", buffer.operations().size());
		try {
			try {
				// preprocess buffer
				buffer.optimize();

				// run operations and remove them from buffer
				for (Iterator<Operation> i = buffer.operations().iterator(); i.hasNext();) {
					Operation op = i.next();
					if (op instanceof LuceneSailBuffer.AddRemoveOperation) {
						AddRemoveOperation addremove = (AddRemoveOperation)op;
						// add/remove in one call
						addRemoveStatements(addremove.getAdded(), addremove.getRemoved());
					}
					else if (op instanceof LuceneSailBuffer.ClearContextOperation) {
						// clear context
						clearContexts(((ClearContextOperation)op).getContexts());
					}
					else if (op instanceof LuceneSailBuffer.ClearOperation) {
						logger.debug("clearing index...");
						luceneIndex.clear();
					}
					else
						throw new RuntimeException(
								"Cannot interpret operation " + op + " of type " + op.getClass().getName());
					i.remove();
				}
			}
			catch (Exception e) {
				logger.error("Committing operations in lucenesail, encountered exception " + e
						+ ". Only some operations were stored, " + buffer.operations().size()
						+ " operations are discarded. Lucene Index is now corrupt.", e);
				throw new SailException(e);
			}
		}
		finally {
			buffer.reset();
		}
	}

	private void addRemoveStatements(Set<Statement> toAdd, Set<Statement> toRemove)
		throws IOException
	{
		logger.debug("indexing {}/removing {} statements...", toAdd.size(), toRemove.size());
		luceneIndex.begin();
		try {
			luceneIndex.addRemoveStatements(toAdd, toRemove);
			luceneIndex.commit();
		}
		catch (IOException e) {
			logger.error("Rolling back", e);
			luceneIndex.rollback();
			throw e;
		}
	}

	private void clearContexts(Resource... contexts)
		throws IOException
	{
		logger.debug("clearing contexts...");
		luceneIndex.begin();
		try {
			luceneIndex.clearContexts(contexts);
			luceneIndex.commit();
		}
		catch (IOException e) {
			logger.error("Rolling back", e);
			luceneIndex.rollback();
			throw e;
		}
	}

	@Override
	public synchronized void removeStatements(Resource arg0, IRI arg1, Value arg2, Resource... arg3)
		throws SailException
	{
		super.removeStatements(arg0, arg1, arg2, arg3);
	}

	@Override
	public void rollback()
		throws SailException
	{
		super.rollback();
		buffer.reset();
		try {
			luceneIndex.rollback();
		}
		catch (IOException e) {
			throw new SailException(e);
		}
	}
}
