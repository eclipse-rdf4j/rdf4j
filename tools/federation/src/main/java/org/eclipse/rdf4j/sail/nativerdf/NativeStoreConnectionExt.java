/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.nativerdf;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.eclipse.rdf4j.IsolationLevel;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.QueryRoot;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategy;
import org.eclipse.rdf4j.query.algebra.evaluation.TripleSource;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.BindingAssigner;
import org.eclipse.rdf4j.query.impl.EmptyBindingSet;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.base.SailClosable;
import org.eclipse.rdf4j.sail.base.SailDataset;
import org.eclipse.rdf4j.sail.base.SailDatasetTripleSourceExt;
import org.eclipse.rdf4j.sail.base.SailSource;
import org.eclipse.rdf4j.sail.base.SailSourceConnection;
import org.eclipse.rdf4j.sail.nativerdf.NativeStore;
import org.eclipse.rdf4j.sail.nativerdf.NativeStoreConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Extension to Sesame's {@link NativeStoreConnection} to allow for efficient evaluation of precompiled queries without
 * prior optimizations. When creating the NativeStore the following hook should be used:
 * 
 * <pre>
 * NativeStore ns = new NativeStore(store) {
 * 	{@literal}Override
 *  protected NotifyingSailConnection getConnectionInternal() throws SailException {
 *    	try {
 *    		return new NativeStoreConnectionExt(this);
 *     	} catch (IOException e) {
 *     		throw new SailException(e);
 *     	}‚
 *   }};
 * </pre>
 * 
 * @author Andreas Schwarte
 *
 */
public class NativeStoreConnectionExt extends NativeStoreConnection {

	protected static final Logger log = LoggerFactory.getLogger(NativeStoreConnectionExt.class);

	public NativeStoreConnectionExt(NativeStore nativeStore)
			throws IOException {
		super(nativeStore);
	}

	public final CloseableIteration<? extends BindingSet, QueryEvaluationException> evaluatePrecompiled(
			TupleExpr tupleExpr)
			throws QueryEvaluationException {
		connectionLock.readLock().lock();
		try {
			verifyIsOpen();
			return registerIteration(evaluatePrecompiledInternal(tupleExpr, null, null, true));
		} catch (SailException e) {
			throw new QueryEvaluationException(e);
		} finally {
			connectionLock.readLock().unlock();
		}
	}

	/*
	 * Copy of org.openrdf.sail.base.SailSourceConnection.evaluateInternal(TupleExpr, Dataset, BindingSet, boolean)
	 * without any optimizers
	 */
	protected CloseableIteration<? extends BindingSet, QueryEvaluationException> evaluatePrecompiledInternal(
			TupleExpr tupleExpr, Dataset dataset, BindingSet bindings, boolean includeInferred)
			throws SailException, QueryEvaluationException {

		flush();
		log.trace("Incoming query model:\n{}", tupleExpr);

		// Clone the tuple expression to allow for more aggresive optimizations
		tupleExpr = tupleExpr.clone();

		if (!(tupleExpr instanceof QueryRoot)) {
			// Add a dummy root node to the tuple expressions to allow the
			// optimizers to modify the actual root node
			tupleExpr = new QueryRoot(tupleExpr);
		}

		SailSource branch = null;
		SailDataset rdfDataset = null;
		CloseableIteration<BindingSet, QueryEvaluationException> iter1 = null;
		CloseableIteration<BindingSet, QueryEvaluationException> iter2 = null;

		boolean allGood = false;
		try {
			branch = _branch(includeInferred);
			rdfDataset = branch.dataset(_getIsolationLevel());

			TripleSource tripleSource = new SailDatasetTripleSourceExt(this.nativeStore.getValueFactory(), rdfDataset);
			EvaluationStrategy strategy = getEvaluationStrategy(dataset, tripleSource);

			if (bindings != null) {
				new BindingAssigner().optimize(tupleExpr, dataset, bindings);
			}

			log.trace("Optimized query model:\n{}", tupleExpr);

			iter1 = strategy.evaluate(tupleExpr, EmptyBindingSet.getInstance());
			iter2 = _interlock(iter1, rdfDataset, branch);
			allGood = true;
			return iter2;
		} catch (QueryEvaluationException e) {
			throw new SailException(e);
		} finally {
			if (!allGood) {
				try {
					if (iter2 != null) {
						iter2.close();
					}
				} finally {
					try {
						if (iter1 != null) {
							iter1.close();
						}
					} finally {
						try {
							if (rdfDataset != null) {
								rdfDataset.close();
							}
						} finally {
							if (branch != null) {
								branch.close();
							}
						}
					}
				}
			}
		}
	}

	/*
	 * ATTENTION
	 * 
	 * Below code is used to make functions of SailSourceConnection class accessible for this implementation
	 */

	static final Method BRANCH_METHOD = _branchMethod();

	protected static final Method _branchMethod() {
		try {
			Method m = SailSourceConnection.class.getDeclaredMethod("branch", boolean.class);
			m.setAccessible(true);
			return m;
		} catch (Exception e) {
			throw new Error(e);
		}
	}

	private SailSource _branch(boolean includeInferred) {
		try {
			return (SailSource) BRANCH_METHOD.invoke(this, includeInferred);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			throw new Error(e);
		}
	}

	static final Method GET_ISOLATION_LEVEL_METHOD = _getIsolationLevelMethod();

	protected static final Method _getIsolationLevelMethod() {
		try {
			Method m = SailSourceConnection.class.getDeclaredMethod("getIsolationLevel");
			m.setAccessible(true);
			return m;
		} catch (Exception e) {
			throw new Error(e);
		}
	}

	private IsolationLevel _getIsolationLevel() {
		try {
			return (IsolationLevel) GET_ISOLATION_LEVEL_METHOD.invoke(this);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			throw new Error(e);
		}
	}

	static final Method INTERLOCK_METHOD = _interlockMethod();

	protected static final Method _interlockMethod() {

		try {
			Method m = SailSourceConnection.class.getDeclaredMethod("interlock", CloseableIteration.class,
					SailClosable[].class);
			m.setAccessible(true);
			return m;
		} catch (Exception e) {
			throw new Error(e);
		}
	}

	@SuppressWarnings("unchecked")
	private <T, X extends Exception> CloseableIteration<T, QueryEvaluationException> _interlock(
			CloseableIteration<T, QueryEvaluationException> iter, SailClosable... closes) {
		try {
			return (CloseableIteration<T, QueryEvaluationException>) INTERLOCK_METHOD.invoke(this, iter, closes);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			throw new Error(e);
		}
	}
}
