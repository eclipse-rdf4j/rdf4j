/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.base;

import org.eclipse.rdf4j.IsolationLevel;
import org.eclipse.rdf4j.IsolationLevels;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.QueryRoot;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategy;
import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategyFactory;
import org.eclipse.rdf4j.query.algebra.evaluation.TripleSource;
import org.eclipse.rdf4j.query.algebra.evaluation.federation.FederatedServiceResolver;
import org.eclipse.rdf4j.query.algebra.evaluation.federation.FederatedServiceResolverClient;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.StrictEvaluationStrategy;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.StrictEvaluationStrategyFactory;
import org.eclipse.rdf4j.query.impl.EmptyBindingSet;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.UnknownSailTransactionStateException;
import org.eclipse.rdf4j.sail.UpdateContext;
import org.eclipse.rdf4j.sail.helpers.AbstractSail;
import org.eclipse.rdf4j.sail.helpers.NotifyingSailConnectionBase;
import org.eclipse.rdf4j.sail.inferencer.InferencerConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

/**
 * A {@link SailConnection} implementation that is based on an {@link SailStore} .
 *
 * @author James Leigh
 */
public abstract class SailSourceConnection extends NotifyingSailConnectionBase
		implements InferencerConnection, FederatedServiceResolverClient {

	private static final Logger logger = LoggerFactory.getLogger(SailSourceConnection.class);

	/*-----------*
	 * Variables *
	 *-----------*/

	/**
	 * The state of store for outstanding operations.
	 */
	private final Map<UpdateContext, SailDataset> datasets = new HashMap<>();

	/**
	 * Outstanding changes that are underway, but not yet realized, by an active operation.
	 */
	private final Map<UpdateContext, SailSink> explicitSinks = new HashMap<>();

	/**
	 * Set of explicit statements that must not be inferred.
	 */
	private volatile SailDataset explicitOnlyDataset;

	/**
	 * Set of inferred statements that have already been inferred earlier.
	 */
	private volatile SailDataset inferredOnlyDataset;

	/**
	 * Outstanding inferred statements that are not yet flushed by a read operation.
	 */
	private volatile SailSink inferredOnlySink;

	/**
	 * {@link ValueFactory} used by this connection.
	 */
	private final ValueFactory vf;

	/**
	 * The backing {@link SailStore} used to manage the state.
	 */
	private final SailStore store;

	/**
	 * The default {@link IsolationLevel} when not otherwise specified.
	 */
	private final IsolationLevel defaultIsolationLevel;

	/**
	 * An {@link SailSource} of only explicit statements when in an isolated transaction.
	 */
	private volatile SailSource explicitOnlyBranch;

	/**
	 * An {@link SailSource} of only inferred statements when in an isolated transaction.
	 */
	private volatile SailSource inferredOnlyBranch;

	/**
	 * An {@link SailSource} of all statements when in an isolated transaction.
	 */
	private volatile SailSource includeInferredBranch;

	/**
	 * {@link EvaluationStrategyFactory} to use.
	 */
	private final EvaluationStrategyFactory evalStratFactory;

	/**
	 * Connection specific resolver.
	 */
	private volatile FederatedServiceResolver federatedServiceResolver;

	// The context that represents the unnamed graph
	static final Resource[] NULL_CTX = new Resource[] { null };

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * Creates a new {@link SailConnection}, using the given {@link SailStore} to manage the state.
	 *
	 * @param sail
	 * @param store
	 * @param resolver the FederatedServiceResolver to use with the {@link StrictEvaluationStrategy default
	 *                 EvaluationStrategy}.
	 */
	protected SailSourceConnection(AbstractSail sail, SailStore store, FederatedServiceResolver resolver) {
		this(sail, store, new StrictEvaluationStrategyFactory(resolver));
	}

	/**
	 * Creates a new {@link SailConnection}, using the given {@link SailStore} to manage the state.
	 *
	 * @param sail
	 * @param store
	 * @param evalStratFactory the {@link EvaluationStrategyFactory} to use.
	 */
	protected SailSourceConnection(AbstractSail sail, SailStore store, EvaluationStrategyFactory evalStratFactory) {
		super(sail);
		this.vf = sail.getValueFactory();
		this.store = store;
		this.defaultIsolationLevel = sail.getDefaultIsolationLevel();
		this.evalStratFactory = evalStratFactory;
		this.federatedServiceResolver = (evalStratFactory instanceof StrictEvaluationStrategyFactory)
				? ((StrictEvaluationStrategyFactory) evalStratFactory).getFederatedServiceResolver()
				: null;
	}

	/*---------*
	 * Methods *
	 *---------*/

	/**
	 * Returns the {@link FederatedServiceResolver} being used.
	 *
	 * @return null if a custom {@link EvaluationStrategyFactory} is being used.
	 */
	public FederatedServiceResolver getFederatedServiceResolver() {
		return federatedServiceResolver;
	}

	/**
	 * Sets the {@link FederatedServiceResolver} to use. If a custom {@link EvaluationStrategyFactory} is being used
	 * then this only has an effect if it implements {@link FederatedServiceResolverClient}.
	 */
	@Override
	public void setFederatedServiceResolver(FederatedServiceResolver resolver) {
		this.federatedServiceResolver = resolver;
	}

	protected EvaluationStrategy getEvaluationStrategy(Dataset dataset, TripleSource tripleSource) {
		EvaluationStrategy evalStrat = evalStratFactory.createEvaluationStrategy(dataset, tripleSource,
				store.getEvaluationStatistics());
		if (federatedServiceResolver != null && evalStrat instanceof FederatedServiceResolverClient) {
			((FederatedServiceResolverClient) evalStrat).setFederatedServiceResolver(federatedServiceResolver);
		}
		return evalStrat;
	}

	@Override
	protected CloseableIteration<? extends BindingSet, QueryEvaluationException> evaluateInternal(TupleExpr tupleExpr,
			Dataset dataset, BindingSet bindings, boolean includeInferred) throws SailException {
		flush();
		logger.trace("Incoming query model:\n{}", tupleExpr);

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
			branch = branch(IncludeInferred.fromBoolean(includeInferred));
			rdfDataset = branch.dataset(getIsolationLevel());

			TripleSource tripleSource = new SailDatasetTripleSource(vf, rdfDataset);
			EvaluationStrategy strategy = getEvaluationStrategy(dataset, tripleSource);

			tupleExpr = strategy.optimize(tupleExpr, store.getEvaluationStatistics(), bindings);

			logger.trace("Optimized query model:\n{}", tupleExpr);

			iter1 = strategy.evaluate(tupleExpr, EmptyBindingSet.getInstance());
			iter2 = interlock(iter1, rdfDataset, branch);
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

	@Override
	protected void closeInternal() throws SailException {
		// no-op
	}

	@Override
	protected CloseableIteration<? extends Resource, SailException> getContextIDsInternal() throws SailException {
		flush();
		SailSource branch = branch(IncludeInferred.explicitOnly);
		SailDataset snapshot = branch.dataset(getIsolationLevel());
		return SailClosingIteration.makeClosable(snapshot.getContextIDs(), snapshot, branch);
	}

	@Override
	protected CloseableIteration<? extends Statement, SailException> getStatementsInternal(Resource subj, IRI pred,
			Value obj, boolean includeInferred, Resource... contexts) throws SailException {
		flush();
		SailSource branch = branch(IncludeInferred.fromBoolean(includeInferred));
		SailDataset snapshot = branch.dataset(getIsolationLevel());
		return SailClosingIteration.makeClosable(snapshot.getStatements(subj, pred, obj, contexts), snapshot, branch);
	}

	@Override
	protected long sizeInternal(Resource... contexts) throws SailException {

		flush();
		try (Stream<? extends Statement> stream = getStatementsInternal(null, null, null, false, contexts).stream()) {
			return stream.count();
		}
	}

	@Override
	protected CloseableIteration<? extends Namespace, SailException> getNamespacesInternal() throws SailException {
		SailSource branch = branch(IncludeInferred.explicitOnly);
		SailDataset snapshot = branch.dataset(getIsolationLevel());
		return SailClosingIteration.makeClosable(snapshot.getNamespaces(), snapshot, branch);
	}

	@Override
	protected String getNamespaceInternal(String prefix) throws SailException {
		SailSource branch = null;
		SailDataset snapshot = null;
		try {
			branch = branch(IncludeInferred.explicitOnly);
			snapshot = branch.dataset(getIsolationLevel());
			return snapshot.getNamespace(prefix);
		} finally {
			try {
				if (snapshot != null) {
					snapshot.close();
				}
			} finally {
				if (branch != null) {
					branch.close();
				}
			}
		}
	}

	@Override
	protected void startTransactionInternal() throws SailException {
		assert explicitOnlyBranch == null;
		assert inferredOnlyBranch == null;
		assert includeInferredBranch == null;
		IsolationLevel level = getTransactionIsolation();
		if (!IsolationLevels.NONE.isCompatibleWith(level)) {
			// only create transaction branches if transaction is isolated
			explicitOnlyBranch = store.getExplicitSailSource().fork();
			inferredOnlyBranch = store.getInferredSailSource().fork();
			includeInferredBranch = new UnionSailSource(inferredOnlyBranch, explicitOnlyBranch);
		}
	}

	@Override
	protected void prepareInternal() throws SailException {
		SailSource toCheckIncludeInferredBranch = includeInferredBranch;
		if (toCheckIncludeInferredBranch != null) {
			toCheckIncludeInferredBranch.prepare();
		}
	}

	@Override
	protected void commitInternal() throws SailException {
		SailSource toCloseInferredBranch = includeInferredBranch;
		explicitOnlyBranch = null;
		inferredOnlyBranch = null;
		includeInferredBranch = null;
		try {
			if (toCloseInferredBranch != null) {
				toCloseInferredBranch.flush();
			}
		} finally {
			if (toCloseInferredBranch != null) {
				toCloseInferredBranch.close();
			}
		}
	}

	@Override
	protected void rollbackInternal() throws SailException {
		synchronized (datasets) {
			SailDataset toCloseDataset = null;
			SailSink toCloseExplicitSink = null;
			SailDataset toCloseExplicitOnlyDataset = explicitOnlyDataset;
			explicitOnlyDataset = null;
			SailDataset toCloseInferredDataset = inferredOnlyDataset;
			inferredOnlyDataset = null;
			SailSink toCloseInferredSink = inferredOnlySink;
			inferredOnlySink = null;
			SailSource toCloseIncludeInferredBranch = includeInferredBranch;
			includeInferredBranch = null;
			explicitOnlyBranch = null;
			inferredOnlyBranch = null;
			try {
				if (datasets.containsKey(null)) {
					toCloseDataset = datasets.remove(null);
				}
			} finally {
				try {
					if (toCloseDataset != null) {
						toCloseDataset.close();
					}
				} finally {
					try {
						if (explicitSinks.containsKey(null)) {
							toCloseExplicitSink = explicitSinks.remove(null);
						}
					} finally {
						try {
							if (toCloseExplicitSink != null) {
								toCloseExplicitSink.close();
							}
						} finally {
							try {
								if (toCloseExplicitOnlyDataset != null) {
									toCloseExplicitOnlyDataset.close();
								}
							} finally {
								try {
									if (toCloseInferredDataset != null) {
										toCloseInferredDataset.close();
									}
								} finally {
									try {
										if (toCloseInferredSink != null) {
											toCloseInferredSink.close();
										}
									} finally {
										if (toCloseIncludeInferredBranch != null) {
											toCloseIncludeInferredBranch.close();
										}
									}
								}
							}
						}
					}
				}
			}
		}
	}

	@Override
	public void startUpdate(UpdateContext op) throws SailException {
		if (op != null) {
			IsolationLevel level = getIsolationLevel();
			flush();
			synchronized (datasets) {
				assert !datasets.containsKey(op);
				SailSource source;
				if (op.isIncludeInferred() && inferredOnlyBranch == null) {
					// IsolationLevels.NONE
					SailSource explicit = store.getExplicitSailSource();
					SailSource inferred = store.getInferredSailSource();
					source = new UnionSailSource(explicit, inferred);
				} else if (op.isIncludeInferred()) {
					source = new UnionSailSource(explicitOnlyBranch, inferredOnlyBranch);
				} else {
					source = branch(IncludeInferred.explicitOnly);
				}
				datasets.put(op, source.dataset(level));
				explicitSinks.put(op, source.sink(level));
			}
		}
	}

	@Override
	public void addStatement(UpdateContext op, Resource subj, IRI pred, Value obj, Resource... contexts)
			throws SailException {
		verifyIsOpen();
		verifyIsActive();
		synchronized (datasets) {
			if (op == null && !datasets.containsKey(null)) {
				SailSource source = branch(IncludeInferred.explicitOnly);
				datasets.put(null, source.dataset(getIsolationLevel()));
				explicitSinks.put(null, source.sink(getIsolationLevel()));
			}
			assert explicitSinks.containsKey(op);
			add(subj, pred, obj, datasets.get(op), explicitSinks.get(op), contexts);
		}
		addStatementInternal(subj, pred, obj, contexts);

	}

	@Override
	public void removeStatement(UpdateContext op, Resource subj, IRI pred, Value obj, Resource... contexts)
			throws SailException {
		verifyIsOpen();
		verifyIsActive();
		synchronized (datasets) {
			if (op == null && !datasets.containsKey(null)) {
				SailSource source = branch(IncludeInferred.explicitOnly);
				datasets.put(null, source.dataset(getIsolationLevel()));
				explicitSinks.put(null, source.sink(getIsolationLevel()));
			}
			assert explicitSinks.containsKey(op);
			remove(subj, pred, obj, datasets.get(op), explicitSinks.get(op), contexts);
		}
		removeStatementsInternal(subj, pred, obj, contexts);
	}

	@Override
	protected void endUpdateInternal(UpdateContext op) throws SailException {
		synchronized (datasets) {
			SailSink toCloseInferredSink = inferredOnlySink;
			inferredOnlySink = null;
			SailDataset toCloseExplicitOnlyDataset = explicitOnlyDataset;
			explicitOnlyDataset = null;
			SailDataset toCloseInferredDataset = inferredOnlyDataset;
			inferredOnlyDataset = null;
			try {
				if (toCloseInferredSink != null) {
					toCloseInferredSink.flush();
				}
			} finally {
				try {
					if (toCloseInferredSink != null) {
						toCloseInferredSink.close();
					}
				} finally {
					try {
						if (toCloseExplicitOnlyDataset != null) {
							toCloseExplicitOnlyDataset.close();
						}
					} finally {
						try {
							if (toCloseInferredDataset != null) {
								toCloseInferredDataset.close();
							}
						} finally {
							SailSink explicit = null;
							try {
								explicit = explicitSinks.remove(op);
								if (explicit != null) {
									explicit.flush();
								}
							} finally {
								try {
									if (explicit != null) {
										explicit.close();
									}
								} finally {
									SailDataset toCloseDataset = null;
									try {
										toCloseDataset = datasets.remove(op);
									} finally {
										if (toCloseDataset != null) {
											toCloseDataset.close();
										}
									}
								}
							}
						}
					}
				}
			}
		}
	}

	@Override
	public boolean addInferredStatement(Resource subj, IRI pred, Value obj, Resource... contexts) throws SailException {
		verifyIsOpen();
		verifyIsActive();
		IsolationLevel level = getIsolationLevel();
		synchronized (datasets) {
			if (inferredOnlySink == null) {
				SailSource branch = branch(IncludeInferred.inferredOnly);
				inferredOnlyDataset = branch.dataset(level);
				inferredOnlySink = branch.sink(level);
				explicitOnlyDataset = branch(IncludeInferred.explicitOnly).dataset(level);
			}
			boolean modified = false;
			if (contexts.length == 0) {
				if (!hasStatement(explicitOnlyDataset, subj, pred, obj, NULL_CTX)) {
					// only add inferred statements that aren't already explicit
					if (!hasStatement(inferredOnlyDataset, subj, pred, obj, NULL_CTX)) {
						// only report inferred statements that don't already
						// exist
						addStatementInternal(subj, pred, obj, contexts);
						notifyStatementAdded(vf.createStatement(subj, pred, obj));
						modified = true;
					}
					inferredOnlySink.approve(subj, pred, obj, null);
				}
			} else {
				for (Resource ctx : contexts) {
					if (!hasStatement(explicitOnlyDataset, subj, pred, obj, ctx)) {
						// only add inferred statements that aren't already
						// explicit
						if (!hasStatement(inferredOnlyDataset, subj, pred, obj, ctx)) {
							// only report inferred statements that don't
							// already exist
							addStatementInternal(subj, pred, obj, ctx);
							notifyStatementAdded(vf.createStatement(subj, pred, obj, ctx));
							modified = true;
						}
						inferredOnlySink.approve(subj, pred, obj, ctx);
					}
				}
			}
			return modified;
		}
	}

	private void add(Resource subj, IRI pred, Value obj, SailDataset dataset, SailSink sink, Resource... contexts)
			throws SailException {
		if (contexts.length == 0) {
			if (hasConnectionListeners() && !hasStatement(dataset, subj, pred, obj, NULL_CTX)) {
				notifyStatementAdded(vf.createStatement(subj, pred, obj));
			}
			sink.approve(subj, pred, obj, null);
		} else {
			for (Resource ctx : contexts) {
				if (hasConnectionListeners() && !hasStatement(dataset, subj, pred, obj, ctx)) {
					notifyStatementAdded(vf.createStatement(subj, pred, obj, ctx));
				}
				sink.approve(subj, pred, obj, ctx);
			}
		}
	}

	@Override
	public boolean removeInferredStatement(Resource subj, IRI pred, Value obj, Resource... contexts)
			throws SailException {
		verifyIsOpen();
		verifyIsActive();
		synchronized (datasets) {
			IsolationLevel level = getIsolationLevel();
			if (inferredOnlySink == null) {
				SailSource branch = branch(IncludeInferred.inferredOnly);
				inferredOnlyDataset = branch.dataset(level);
				inferredOnlySink = branch.sink(level);
				explicitOnlyDataset = branch(IncludeInferred.explicitOnly).dataset(level);
			}
			removeStatementsInternal(subj, pred, obj, contexts);
			return remove(subj, pred, obj, inferredOnlyDataset, inferredOnlySink, contexts);
		}
	}

	private boolean remove(Resource subj, IRI pred, Value obj, SailDataset dataset, SailSink sink, Resource... contexts)
			throws SailException {

		// Use deprecateByQuery if we don't need to notify anyone of which statements have been deleted.
		if (!hasConnectionListeners() && sink.supportsDeprecateByQuery()) {
			return sink.deprecateByQuery(subj, pred, obj, contexts);
		}

		boolean statementsRemoved = false;

		try (CloseableIteration<? extends Statement, SailException> iter = dataset.getStatements(subj, pred, obj,
				contexts)) {
			while (iter.hasNext()) {
				Statement st = iter.next();
				sink.deprecate(st);

				statementsRemoved = true;
				notifyStatementRemoved(st);
			}
		}
		return statementsRemoved;
	}

	@Override
	protected void clearInternal(Resource... contexts) throws SailException {
		verifyIsOpen();
		verifyIsActive();
		synchronized (datasets) {
			if (!datasets.containsKey(null)) {
				SailSource source = branch(IncludeInferred.explicitOnly);
				datasets.put(null, source.dataset(getIsolationLevel()));
				explicitSinks.put(null, source.sink(getIsolationLevel()));
			}
			assert explicitSinks.containsKey(null);
			if (this.hasConnectionListeners()) {
				remove(null, null, null, datasets.get(null), explicitSinks.get(null), contexts);
			}
			explicitSinks.get(null).clear(contexts);
		}
	}

	@Override
	public void clearInferred(Resource... contexts) throws SailException {
		verifyIsOpen();
		verifyIsActive();
		synchronized (datasets) {
			if (inferredOnlySink == null) {
				IsolationLevel level = getIsolationLevel();
				SailSource branch = branch(IncludeInferred.inferredOnly);
				inferredOnlyDataset = branch.dataset(level);
				inferredOnlySink = branch.sink(level);
				explicitOnlyDataset = branch(IncludeInferred.explicitOnly).dataset(level);
			}
			if (this.hasConnectionListeners()) {
				remove(null, null, null, inferredOnlyDataset, inferredOnlySink, contexts);
			}
			inferredOnlySink.clear(contexts);
		}
	}

	@Override
	public void flushUpdates() throws SailException {
		flush();
	}

	@Override
	protected void setNamespaceInternal(String prefix, String name) throws SailException {
		SailSource branch = null;
		SailSink sink = null;
		try {
			branch = branch(IncludeInferred.explicitOnly);
			sink = branch.sink(getTransactionIsolation());
			sink.setNamespace(prefix, name);
			sink.flush();
		} finally {
			try {
				if (sink != null) {
					sink.close();
				}
			} finally {
				if (branch != null) {
					branch.close();
				}
			}
		}
	}

	@Override
	protected void removeNamespaceInternal(String prefix) throws SailException {
		SailSource branch = null;
		SailSink sink = null;
		try {
			branch = branch(IncludeInferred.explicitOnly);
			sink = branch.sink(getTransactionIsolation());
			sink.removeNamespace(prefix);
			sink.flush();
		} finally {
			try {
				if (sink != null) {
					sink.close();
				}
			} finally {
				if (branch != null) {
					branch.close();
				}
			}
		}
	}

	@Override
	protected void clearNamespacesInternal() throws SailException {
		SailSource branch = null;
		SailSink sink = null;
		try {
			branch = branch(IncludeInferred.explicitOnly);
			sink = branch.sink(getTransactionIsolation());
			sink.clearNamespaces();
			sink.flush();
		} finally {
			try {
				if (sink != null) {
					sink.close();
				}
			} finally {
				if (branch != null) {
					branch.close();
				}
			}
		}
	}

	/*-------------------------------------*
	 * Inner class MemEvaluationStatistics *
	 *-------------------------------------*/

	private IsolationLevel getIsolationLevel() throws UnknownSailTransactionStateException {
		if (isActive()) {
			return getTransactionIsolation();
		} else {
			return defaultIsolationLevel;
		}
	}

	enum IncludeInferred {
		all,
		explicitOnly,
		inferredOnly;

		public static IncludeInferred fromBoolean(boolean includeInferred) {
			return includeInferred ? all : explicitOnly;
		}
	}

	/**
	 * @return read operation {@link SailSource}
	 * @throws SailException
	 */
	private SailSource branch(IncludeInferred includeinferred) throws SailException {
		boolean active = isActive();
		IsolationLevel level = getIsolationLevel();
		boolean isolated = !IsolationLevels.NONE.isCompatibleWith(level);
		if (includeinferred == IncludeInferred.all && active && isolated) {
			// use the transaction branch
			return new DelegatingSailSource(includeInferredBranch, false);
		} else if (includeinferred == IncludeInferred.inferredOnly && active && isolated) {
			// use the transaction branch
			return new DelegatingSailSource(inferredOnlyBranch, false);
		} else if (active && isolated) {
			// use the transaction branch
			return new DelegatingSailSource(explicitOnlyBranch, false);
		} else if (includeinferred == IncludeInferred.all && active) {
			// don't actually branch source
			return new UnionSailSource(store.getInferredSailSource(), store.getExplicitSailSource());
		} else if (includeinferred == IncludeInferred.inferredOnly && active) {
			// don't actually branch source
			return store.getInferredSailSource();
		} else if (active) {
			// don't actually branch source
			return store.getExplicitSailSource();
		} else if (includeinferred == IncludeInferred.all) {
			// create a new branch for read operation
			return new UnionSailSource(store.getInferredSailSource().fork(), store.getExplicitSailSource().fork());
		} else if (includeinferred == IncludeInferred.inferredOnly) {
			// create a new branch for read operation
			return store.getInferredSailSource().fork();
		} else {
			// create a new branch for read operation
			return store.getExplicitSailSource().fork();
		}
	}

	private <T, X extends Exception> CloseableIteration<T, QueryEvaluationException> interlock(
			CloseableIteration<T, QueryEvaluationException> iter, SailClosable... closes) {
		return new SailClosingIteration<T, QueryEvaluationException>(iter, closes) {

			@Override
			protected void handleSailException(SailException e) throws QueryEvaluationException {
				throw new QueryEvaluationException(e);
			}
		};
	}

	private boolean hasStatement(SailDataset dataset, Resource subj, IRI pred, Value obj, Resource... contexts)
			throws SailException {
		try (CloseableIteration<? extends Statement, SailException> iter = dataset.getStatements(subj, pred, obj,
				contexts)) {
			return iter.hasNext();
		}
	}

}
