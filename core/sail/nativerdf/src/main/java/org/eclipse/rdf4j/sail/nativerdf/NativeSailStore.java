/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.nativerdf;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.rdf4j.IsolationLevel;
import org.eclipse.rdf4j.OpenRDFUtil;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.CloseableIteratorIteration;
import org.eclipse.rdf4j.common.iteration.ConvertingIteration;
import org.eclipse.rdf4j.common.iteration.DistinctIteration;
import org.eclipse.rdf4j.common.iteration.EmptyIteration;
import org.eclipse.rdf4j.common.iteration.ExceptionConvertingIteration;
import org.eclipse.rdf4j.common.iteration.FilterIteration;
import org.eclipse.rdf4j.common.iteration.ReducedIteration;
import org.eclipse.rdf4j.common.iteration.UnionIteration;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.URI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.EvaluationStatistics;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.base.BackingSailSource;
import org.eclipse.rdf4j.sail.base.SailDataset;
import org.eclipse.rdf4j.sail.base.SailSink;
import org.eclipse.rdf4j.sail.base.SailSource;
import org.eclipse.rdf4j.sail.base.SailStore;
import org.eclipse.rdf4j.sail.nativerdf.btree.RecordIterator;
import org.eclipse.rdf4j.sail.nativerdf.model.NativeValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A disk based {@link SailStore} implementation that keeps committed statements in a {@link TripleStore}.
 *  
 * @author James Leigh
 */
class NativeSailStore implements SailStore {

	final Logger logger = LoggerFactory.getLogger(NativeSailStore.class);

	final TripleStore tripleStore;

	final ValueStore valueStore;

	final NamespaceStore namespaceStore;

	/**
	 * Lock manager used to prevent concurrent transactions.
	 */
	final ReentrantLock txnLockManager = new ReentrantLock();

	/**
	 * Creates a new {@link NativeSailStore} with the default cache sizes.
	 */
	public NativeSailStore(File dataDir, String tripleIndexes)
		throws IOException, SailException
	{
		this(dataDir, tripleIndexes, false, ValueStore.VALUE_CACHE_SIZE,
				ValueStore.VALUE_ID_CACHE_SIZE, ValueStore.NAMESPACE_CACHE_SIZE, ValueStore.NAMESPACE_ID_CACHE_SIZE);
	}

	/**
	 * Creates a new {@link NativeSailStore}.
	 */
	public NativeSailStore(File dataDir, String tripleIndexes, boolean forceSync, int valueCacheSize,
			int valueIDCacheSize, int namespaceCacheSize, int namespaceIDCacheSize)
		throws IOException, SailException
	{
		boolean initialized = false;
		try {
			namespaceStore = new NamespaceStore(dataDir);
			valueStore = new ValueStore(dataDir, forceSync, valueCacheSize, valueIDCacheSize,
					namespaceCacheSize, namespaceIDCacheSize);
			tripleStore = new TripleStore(dataDir, tripleIndexes, forceSync);
			initialized = true;
		}
		finally {
			if (!initialized) {
				close();
			}
		}
	}

	public ValueFactory getValueFactory() {
		return valueStore;
	}

	public void close() {
		if (namespaceStore != null) {
			namespaceStore.close();
		}
		try {
			if (valueStore != null) {
				valueStore.close();
			}
			if (tripleStore != null) {
				tripleStore.close();
			}
		}
		catch (IOException e) {
			logger.warn("Failed to close store", e);
		}
	}

	@Override
	public EvaluationStatistics getEvaluationStatistics() {
		return new NativeEvaluationStatistics(valueStore, tripleStore);
	}

	public SailSource getExplicitSailSource() {
		return new NativeSailSource(true);
	}

	public SailSource getInferredSailSource() {
		return new NativeSailSource(false);
	}

	List<Integer> getContextIDs(Resource... contexts)
		throws IOException
	{
		assert contexts.length > 0 : "contexts must not be empty";

		// Filter duplicates
		LinkedHashSet<Resource> contextSet = new LinkedHashSet<Resource>();
		Collections.addAll(contextSet, contexts);

		// Fetch IDs, filtering unknown resources from the result
		List<Integer> contextIDs = new ArrayList<Integer>(contextSet.size());
		for (Resource context : contextSet) {
			if (context == null) {
				contextIDs.add(0);
			}
			else {
				int contextID = valueStore.getID(context);
				if (contextID != NativeValue.UNKNOWN_ID) {
					contextIDs.add(contextID);
				}
			}
		}

		return contextIDs;
	}

	/**
	 * Creates a statement iterator based on the supplied pattern.
	 * 
	 * @param subj
	 *        The subject of the pattern, or <tt>null</tt> to indicate a
	 *        wildcard.
	 * @param pred
	 *        The predicate of the pattern, or <tt>null</tt> to indicate a
	 *        wildcard.
	 * @param obj
	 *        The object of the pattern, or <tt>null</tt> to indicate a wildcard.
	 * @param contexts
	 *        The context(s) of the pattern. Note that this parameter is a vararg
	 *        and as such is optional. If no contexts are supplied the method
	 *        operates on the entire repository.
	 * @return A StatementIterator that can be used to iterate over the
	 *         statements that match the specified pattern.
	 */
	CloseableIteration<? extends Statement, SailException> createStatementIterator(Resource subj, URI pred, Value obj,
			boolean explicit, Resource... contexts)
		throws IOException
	{
		int subjID = NativeValue.UNKNOWN_ID;
		if (subj != null) {
			subjID = valueStore.getID(subj);
			if (subjID == NativeValue.UNKNOWN_ID) {
				return new EmptyIteration<Statement, SailException>();
			}
		}

		int predID = NativeValue.UNKNOWN_ID;
		if (pred != null) {
			predID = valueStore.getID(pred);
			if (predID == NativeValue.UNKNOWN_ID) {
				return new EmptyIteration<Statement, SailException>();
			}
		}

		int objID = NativeValue.UNKNOWN_ID;
		if (obj != null) {
			objID = valueStore.getID(obj);
			if (objID == NativeValue.UNKNOWN_ID) {
				return new EmptyIteration<Statement, SailException>();
			}
		}

		List<Integer> contextIDList = new ArrayList<Integer>(contexts.length);
		if (contexts.length == 0) {
			contextIDList.add(NativeValue.UNKNOWN_ID);
		}
		else {
			for (Resource context : contexts) {
				if (context == null) {
					contextIDList.add(0);
				}
				else {
					int contextID = valueStore.getID(context);

					if (contextID != NativeValue.UNKNOWN_ID) {
						contextIDList.add(contextID);
					}
				}
			}
		}

		ArrayList<NativeStatementIterator> perContextIterList = new ArrayList<NativeStatementIterator>(
				contextIDList.size());

		for (int contextID : contextIDList) {
			RecordIterator btreeIter = tripleStore.getTriples(subjID, predID, objID, contextID, explicit, false);

			perContextIterList.add(new NativeStatementIterator(btreeIter, valueStore));
		}

		if (perContextIterList.size() == 1) {
			return perContextIterList.get(0);
		}
		else {
			return new UnionIteration<Statement, SailException>(perContextIterList);
		}
	}

	double cardinality(Resource subj, URI pred, Value obj, Resource context)
		throws IOException
	{
		int subjID = NativeValue.UNKNOWN_ID;
		if (subj != null) {
			subjID = valueStore.getID(subj);
			if (subjID == NativeValue.UNKNOWN_ID) {
				return 0;
			}
		}

		int predID = NativeValue.UNKNOWN_ID;
		if (pred != null) {
			predID = valueStore.getID(pred);
			if (predID == NativeValue.UNKNOWN_ID) {
				return 0;
			}
		}

		int objID = NativeValue.UNKNOWN_ID;
		if (obj != null) {
			objID = valueStore.getID(obj);
			if (objID == NativeValue.UNKNOWN_ID) {
				return 0;
			}
		}

		int contextID = NativeValue.UNKNOWN_ID;
		if (context != null) {
			contextID = valueStore.getID(context);
			if (contextID == NativeValue.UNKNOWN_ID) {
				return 0;
			}
		}

		return tripleStore.cardinality(subjID, predID, objID, contextID);
	}

	private final class NativeSailSource extends BackingSailSource {

		private final boolean explicit;

		public NativeSailSource(boolean explicit) {
			this.explicit = explicit;
		}

		@Override
		public SailSource fork() {
			throw new UnsupportedOperationException("This store does not support multiple datasets");
		}

		@Override
		public SailSink sink(IsolationLevel level)
			throws SailException
		{
			return new NativeSailSink(explicit);
		}

		@Override
		public NativeSailDataset dataset(IsolationLevel level)
			throws SailException
		{
			return new NativeSailDataset(explicit);
		}

	}

	private final class NativeSailSink implements SailSink {

		private boolean explicit;

		/**
		 * The exclusive transaction lock held by this connection during
		 * transactions.
		 */
		private volatile boolean txnLockAcquired;

		public NativeSailSink(boolean explicit)
			throws SailException
		{
			this.explicit = explicit;
		}

		@Override
		public synchronized void close() {
			if (txnLockAcquired) {
				txnLockManager.unlock();
				txnLockAcquired = false;
			}
		}

		@Override
		public void prepare()
			throws SailException
		{
			// serializable is not supported at this level
		}

		@Override
		public void flush()
			throws SailException
		{
			// SES-1949 check necessary to avoid empty/read-only transactions
			// messing up concurrent transactions
			if (txnLockAcquired && txnLockManager.getHoldCount() == 1) {
				try {
					valueStore.sync();
					namespaceStore.sync();
					tripleStore.commit();
				}
				catch (IOException e) {
					logger.error("Encountered an unexpected problem while trying to commit", e);
				}
				catch (RuntimeException e) {
					logger.error("Encountered an unexpected problem while trying to commit", e);
					throw e;
				}
			}
		}

		@Override
		public void setNamespace(String prefix, String name)
			throws SailException
		{
			acquireExclusiveTransactionLock();
			namespaceStore.setNamespace(prefix, name);
		}

		@Override
		public void removeNamespace(String prefix)
			throws SailException
		{
			acquireExclusiveTransactionLock();
			namespaceStore.removeNamespace(prefix);
		}

		@Override
		public void clearNamespaces()
			throws SailException
		{
			acquireExclusiveTransactionLock();
			namespaceStore.clear();
		}

		@Override
		public void observe(Resource subj, IRI pred, Value obj, Resource... contexts)
			throws SailException
		{
			// serializable is not supported at this level
		}

		@Override
		public void clear(Resource... contexts)
			throws SailException
		{
			removeStatements(null, null, null, explicit, contexts);
		}

		@Override
		public void approve(Resource subj, IRI pred, Value obj, Resource ctx)
			throws SailException
		{
			addStatement(subj, pred, obj, explicit, ctx);
		}

		@Override
		public void deprecate(Resource subj, IRI pred, Value obj, Resource ctx)
			throws SailException
		{
			removeStatements(subj, pred, obj, explicit, ctx);
		}

		private synchronized void acquireExclusiveTransactionLock()
			throws SailException
		{
			if (!txnLockAcquired) {
				txnLockManager.lock();
				try {
					if (txnLockManager.getHoldCount() == 1) {
						// first object
						tripleStore.startTransaction();
					}
					txnLockAcquired = true;
				}
				catch (IOException e) {
					throw new SailException(e);
				}
				finally {
					if (!txnLockAcquired) {
						txnLockManager.unlock();
					}
				}
			}
		}

		private boolean addStatement(Resource subj, IRI pred, Value obj, boolean explicit, Resource... contexts)
			throws SailException
		{
			acquireExclusiveTransactionLock();

			OpenRDFUtil.verifyContextNotNull(contexts);

			boolean result = false;

			try {
				int subjID = valueStore.storeValue(subj);
				int predID = valueStore.storeValue(pred);
				int objID = valueStore.storeValue(obj);

				if (contexts.length == 0) {
					contexts = new Resource[] { null };
				}

				for (Resource context : contexts) {
					int contextID = 0;
					if (context != null) {
						contextID = valueStore.storeValue(context);
					}

					boolean wasNew = tripleStore.storeTriple(subjID, predID, objID, contextID, explicit);
					result |= wasNew;
				}
			}
			catch (IOException e) {
				throw new SailException(e);
			}
			catch (RuntimeException e) {
				logger.error("Encountered an unexpected problem while trying to add a statement", e);
				throw e;
			}

			return result;
		}

		private int removeStatements(Resource subj, URI pred, Value obj, boolean explicit, Resource... contexts)
			throws SailException
		{
			acquireExclusiveTransactionLock();

			OpenRDFUtil.verifyContextNotNull(contexts);

			try {
				int subjID = NativeValue.UNKNOWN_ID;
				if (subj != null) {
					subjID = valueStore.getID(subj);
					if (subjID == NativeValue.UNKNOWN_ID) {
						return 0;
					}
				}
				int predID = NativeValue.UNKNOWN_ID;
				if (pred != null) {
					predID = valueStore.getID(pred);
					if (predID == NativeValue.UNKNOWN_ID) {
						return 0;
					}
				}
				int objID = NativeValue.UNKNOWN_ID;
				if (obj != null) {
					objID = valueStore.getID(obj);
					if (objID == NativeValue.UNKNOWN_ID) {
						return 0;
					}
				}

				List<Integer> contextIDList = new ArrayList<Integer>(contexts.length);
				if (contexts.length == 0) {
					contextIDList.add(NativeValue.UNKNOWN_ID);
				}
				else {
					for (Resource context : contexts) {
						if (context == null) {
							contextIDList.add(0);
						}
						else {
							int contextID = valueStore.getID(context);
							if (contextID != NativeValue.UNKNOWN_ID) {
								contextIDList.add(contextID);
							}
						}
					}
				}

				int removeCount = 0;

				for (int i = 0; i < contextIDList.size(); i++) {
					int contextID = contextIDList.get(i);

					removeCount += tripleStore.removeTriples(subjID, predID, objID, contextID, explicit);
				}

				return removeCount;
			}
			catch (IOException e) {
				throw new SailException(e);
			}
			catch (RuntimeException e) {
				logger.error("Encountered an unexpected problem while trying to remove statements", e);
				throw e;
			}
		}
	}

	/**
	 * @author James Leigh
	 */
	private final class NativeSailDataset implements SailDataset {

		private final boolean explicit;

		public NativeSailDataset(boolean explicit)
			throws SailException
		{
			this.explicit = explicit;
		}

		@Override
		public void close() {
			// no-op
		}

		@Override
		public String getNamespace(String prefix)
			throws SailException
		{
			return namespaceStore.getNamespace(prefix);
		}

		@Override
		public CloseableIteration<? extends Namespace, SailException> getNamespaces() {
			return new CloseableIteratorIteration<Namespace, SailException>(namespaceStore.iterator());
		}

		@Override
		public CloseableIteration<? extends Resource, SailException> getContextIDs()
			throws SailException
		{
			// Which resources are used as context identifiers is not stored
			// separately. Iterate over all statements and extract their context.
			try {
				CloseableIteration<? extends Statement, SailException> stIter;
				CloseableIteration<Resource, SailException> ctxIter;
				RecordIterator btreeIter;
				btreeIter = tripleStore.getAllTriplesSortedByContext(false);
				if (btreeIter == null) {
					// Iterator over all statements
					stIter = createStatementIterator(null, null, null, explicit);
				}
				else {
					stIter = new NativeStatementIterator(btreeIter, valueStore);
				}
				// Filter statements without context resource
				stIter = new FilterIteration<Statement, SailException>(stIter) {

					@Override
					protected boolean accept(Statement st) {
						return st.getContext() != null;
					}
				};
				// Return the contexts of the statements
				ctxIter = new ConvertingIteration<Statement, Resource, SailException>(stIter) {

					@Override
					protected Resource convert(Statement st) {
						return st.getContext();
					}
				};
				if (btreeIter == null) {
					// Filtering any duplicates
					ctxIter = new DistinctIteration<Resource, SailException>(ctxIter);
				}
				else {
					// Filtering sorted duplicates
					ctxIter = new ReducedIteration<Resource, SailException>(ctxIter);
				}

				return new ExceptionConvertingIteration<Resource, SailException>(ctxIter) {

					@Override
					protected SailException convert(Exception e) {
						if (e instanceof IOException) {
							return new SailException(e);
						}
						else if (e instanceof RuntimeException) {
							throw (RuntimeException)e;
						}
						else if (e == null) {
							throw new IllegalArgumentException("e must not be null");
						}
						else {
							throw new IllegalArgumentException("Unexpected exception type: " + e.getClass());
						}
					}
				};
			}
			catch (IOException e) {
				throw new SailException(e);
			}
		}

		@Override
		public CloseableIteration<? extends Statement, SailException> getStatements(Resource subj, IRI pred,
				Value obj, Resource... contexts)
			throws SailException
		{
			try {
				return createStatementIterator(subj, pred, obj, explicit, contexts);
			}
			catch (IOException e) {
				throw new SailException("Unable to get statements", e);
			}
		}
	}
}
