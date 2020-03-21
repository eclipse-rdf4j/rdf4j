/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.federated;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.DistinctIteration;
import org.eclipse.rdf4j.common.iteration.EmptyIteration;
import org.eclipse.rdf4j.common.iteration.ExceptionConvertingIteration;
import org.eclipse.rdf4j.federated.endpoint.Endpoint;
import org.eclipse.rdf4j.federated.evaluation.FederationEvalStrategy;
import org.eclipse.rdf4j.federated.evaluation.FederationEvaluationStatistics;
import org.eclipse.rdf4j.federated.evaluation.concurrent.ParallelExecutor;
import org.eclipse.rdf4j.federated.evaluation.concurrent.ParallelTask;
import org.eclipse.rdf4j.federated.evaluation.iterator.RepositoryExceptionConvertingIteration;
import org.eclipse.rdf4j.federated.evaluation.iterator.StopRemainingExecutionsOnCloseIteration;
import org.eclipse.rdf4j.federated.evaluation.union.SynchronousWorkerUnion;
import org.eclipse.rdf4j.federated.evaluation.union.WorkerUnionBase;
import org.eclipse.rdf4j.federated.repository.FedXRepositoryConnection;
import org.eclipse.rdf4j.federated.structures.QueryInfo;
import org.eclipse.rdf4j.federated.structures.QueryType;
import org.eclipse.rdf4j.federated.util.FedXUtil;
import org.eclipse.rdf4j.federated.write.WriteStrategy;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.Operation;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.impl.EmptyBindingSet;
import org.eclipse.rdf4j.query.impl.MapBindingSet;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.helpers.AbstractSail;
import org.eclipse.rdf4j.sail.helpers.AbstractSailConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An implementation of RepositoryConnection that uses {@link FederationEvalStrategy} to evaluate provided queries.
 * Prior to evaluation various optimizations are performed, see
 * {@link org.eclipse.rdf4j.federated.optimizer.FedXOptimizer} for further details.
 * <p>
 * 
 * Since 4.0 FedX supports write operations using the supplied {@link WriteStrategy}, e.g. by writing to a designated
 * federation member. Note: the {@link WriteStrategy} is initialized lazily upon first access to a write operation, see
 * {@link #getWriteStrategyInternal()}.
 * 
 * Implementation notes: - not all methods are implemented as of now
 * 
 * @author Andreas Schwarte
 * @see FederationEvalStrategy
 * @see WriteStrategy
 */
public class FedXConnection extends AbstractSailConnection {

	private static final Logger log = LoggerFactory.getLogger(FedXConnection.class);
	protected final FedX federation;
	protected final FederationContext federationContext;

	/**
	 * If set, contains the initialized write strategy. Always access via {@link #getWriteStrategyInternal()}
	 */
	private WriteStrategy writeStrategy;

	public FedXConnection(FedX federation, FederationContext federationContext)
			throws SailException {
		super(federation);
		this.federation = federation;
		this.federationContext = federationContext;
	}

	@Override
	protected CloseableIteration<? extends BindingSet, QueryEvaluationException> evaluateInternal(
			TupleExpr query, Dataset dataset, BindingSet bindings,
			boolean includeInferred) throws SailException {

		FederationEvalStrategy strategy = federationContext.getStrategy();

		long start = 0;
		QueryInfo queryInfo = null;
		if (true) {
			String queryString = getOriginalQueryString(bindings);
			if (queryString == null)
				log.warn("Query string is null. Please check your FedX setup.");
			queryInfo = new QueryInfo(queryString, getOriginalQueryType(bindings),
					getOriginalMaxExecutionTime(bindings), includeInferred, federationContext);

			if (log.isDebugEnabled()) {
				log.debug("Optimization start (Query: " + queryInfo.getQueryID() + ")");
				start = System.currentTimeMillis();
			}
			try {
				federationContext.getMonitoringService().monitorQuery(queryInfo);
				FederationEvaluationStatistics stats = new FederationEvaluationStatistics(queryInfo, dataset);
				query = strategy.optimize(query, stats, bindings);
			} catch (Exception e) {
				log.warn("Exception occured during optimization (Query: " + queryInfo.getQueryID() + "): "
						+ e.getMessage());
				log.debug("Details: ", e);
				throw new SailException(e);
			}
			if (log.isDebugEnabled())
				log.debug(("Optimization duration: " + ((System.currentTimeMillis() - start))) + " (Query: "
						+ queryInfo.getQueryID() + ")");
		}

		// log the optimized query plan, if Config#isLogQueryPlan(), otherwise void operation
		federationContext.getMonitoringService().logQueryPlan(query);

		if (federationContext.getConfig().isDebugQueryPlan()) {
			System.out.println("Optimized query execution plan: \n" + query);
		}

		if (log.isDebugEnabled()) {
			log.debug("Optimized query execution plan (Query: " + queryInfo.getQueryID() + ");" + query);
		}

		try {
			// make sure to apply any external bindings
			BindingSet queryBindings = EmptyBindingSet.getInstance();
			if (bindings.size() > FedXRepositoryConnection.FEDX_BINDINGS.size()) {
				MapBindingSet actualQueryBindings = new MapBindingSet();
				bindings.forEach(binding -> {
					if (!FedXRepositoryConnection.FEDX_BINDINGS.contains(binding.getName())) {
						actualQueryBindings.addBinding(binding);
					}
				});
				queryBindings = actualQueryBindings;
			}
			CloseableIteration<? extends BindingSet, QueryEvaluationException> res = strategy.evaluate(query,
					queryBindings);
			res = new StopRemainingExecutionsOnCloseIteration(res, queryInfo);
			return res;
		} catch (QueryEvaluationException e) {
			throw new SailException(e);
		}
	}

	@Override
	protected void clearInternal(Resource... contexts) throws SailException {
		throw new UnsupportedOperationException("Operation is not yet supported.");
	}

	@Override
	protected void clearNamespacesInternal() throws SailException {
		throw new UnsupportedOperationException("Operation is not yet supported.");
	}

	@Override
	protected void closeInternal() throws SailException {

		/*
		 * think about it: the federation connection should remain open until the federation is shutdown. we use a
		 * singleton connection!!
		 */

		// the write strategy needs to be closed
		try {
			// Note: access the field directly to not initialize write connection
			if (this.writeStrategy != null) {
				this.writeStrategy.close();
			}
		} catch (RepositoryException e) {
			throw new SailException(e);
		}
	}

	@Override
	protected void commitInternal() throws SailException {
		try {
			getWriteStrategyInternal().commit();
		} catch (RepositoryException e) {
			throw new SailException(e);
		}
	}

	@Override
	protected CloseableIteration<? extends Resource, SailException> getContextIDsInternal() throws SailException {

		FederationEvalStrategy strategy = federationContext.getStrategy();
		final WorkerUnionBase<Resource> union = new SynchronousWorkerUnion<>(strategy,
				new QueryInfo("getContextIDsInternal", QueryType.UNKNOWN, 0,
						federationContext.getConfig().getIncludeInferredDefault(), federationContext));

		for (final Endpoint e : federation.getMembers()) {
			union.addTask(new ParallelTask<Resource>() {
				@Override
				public CloseableIteration<Resource, QueryEvaluationException> performTask() throws Exception {
					try (RepositoryConnection conn = e.getConnection()) {
						return new RepositoryExceptionConvertingIteration<>(conn.getContextIDs());
					}
				}

				@Override
				public ParallelExecutor<Resource> getControl() {
					return union;
				}

				@Override
				public void cancel() {
				}
			});
		}

		// execute the union in a separate thread
		federationContext.getManager().getExecutor().execute(union);

		return new DistinctIteration<Resource, SailException>(
				new ExceptionConvertingIteration<Resource, SailException>(union) {
					@Override
					protected SailException convert(Exception e) {
						return new SailException(e);
					}
				});
	}

	@Override
	protected String getNamespaceInternal(String prefix) throws SailException {
		// do not support this feature, but also do not throw an exception
		// as this method is expected for the RDF4J workbench to work
		return null;
	}

	@Override
	protected CloseableIteration<? extends Namespace, SailException> getNamespacesInternal()
			throws SailException {
		// do not support this feature, but also do not throw an exception
		// as this method is expected for the RDF4J workbench to work
		return new EmptyIteration<>();
	}

	@Override
	protected CloseableIteration<? extends Statement, SailException> getStatementsInternal(
			Resource subj, IRI pred, Value obj, final boolean includeInferred,
			final Resource... contexts) throws SailException {

		try {
			FederationEvalStrategy strategy = federationContext.getStrategy();
			QueryInfo queryInfo = new QueryInfo(subj, pred, obj, includeInferred, federationContext);
			federationContext.getMonitoringService().monitorQuery(queryInfo);
			CloseableIteration<Statement, QueryEvaluationException> res = strategy.getStatements(queryInfo, subj, pred,
					obj, contexts);
			return new ExceptionConvertingIteration<Statement, SailException>(res) {
				@Override
				protected SailException convert(Exception e) {
					return new SailException(e);
				}
			};
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new SailException(e);
		}
	}

	@Override
	protected void addStatementInternal(Resource subj, IRI pred, Value obj,
			Resource... contexts) throws SailException {
		try {
			getWriteStrategyInternal().addStatement(subj, pred, obj, contexts);
		} catch (RepositoryException e) {
			throw new SailException(e);
		}
	}

	@Override
	protected void removeNamespaceInternal(String prefix) throws SailException {
		// do not support this feature, but also do not throw an exception
	}

	@Override
	protected void removeStatementsInternal(Resource subj, IRI pred, Value obj,
			Resource... contexts) throws SailException {
		try {
			getWriteStrategyInternal().removeStatement(subj, pred, obj, contexts);
		} catch (RepositoryException e) {
			throw new SailException(e);
		}
	}

	@Override
	protected void rollbackInternal() throws SailException {
		try {
			getWriteStrategyInternal().rollback();
		} catch (RepositoryException e) {
			throw new SailException(e);
		}
	}

	@Override
	protected void setNamespaceInternal(String prefix, String name) throws SailException {
		// do not support this feature, but also do not throw an exception
	}

	@Override
	protected long sizeInternal(Resource... contexts) throws SailException {
		if (contexts != null && contexts.length > 0)
			throw new UnsupportedOperationException("Context handling for size() not supported");
		long size = 0;
		List<String> errorEndpoints = new ArrayList<>();
		for (Endpoint e : federation.getMembers()) {
			try {
				size += e.size();
			} catch (RepositoryException e1) {
				errorEndpoints.add(e.getId());
			}
		}
		if (errorEndpoints.size() > 0)
			throw new SailException("Could not determine size for members " + errorEndpoints.toString() +
					"(Supported for NativeStore and RemoteRepository only). Computed size: " + size);
		return size;
	}

	@Override
	protected void startTransactionInternal() throws SailException {
		try {
			getWriteStrategyInternal().begin();
		} catch (RepositoryException e) {
			throw new SailException(e);
		}
	}

	/**
	 * Return the initialized {@link #writeStrategy}. If this has not been done yet, {@link WriteStrategy#initialize()}
	 * is returned. This method guarantees lazy initialization upon the first write operation on this
	 * {@link FedXConnection} instance.
	 * 
	 * @return the {@link WriteStrategy}
	 */
	protected synchronized WriteStrategy getWriteStrategyInternal() throws SailException {

		if (writeStrategy == null) {
			writeStrategy = federation.getWriteStrategy();
			try {
				writeStrategy.initialize();
			} catch (RepositoryException e) {
				throw new SailException(e);
			}
		}

		return writeStrategy;
	}

	private static String getOriginalQueryString(BindingSet b) {
		if (b == null)
			return null;
		Value q = b.getValue(FedXRepositoryConnection.BINDING_ORIGINAL_QUERY);
		if (q != null)
			return q.stringValue();
		return null;
	}

	private static QueryType getOriginalQueryType(BindingSet b) {
		if (b == null)
			return null;
		Value q = b.getValue(FedXRepositoryConnection.BINDING_ORIGINAL_QUERY_TYPE);
		if (q != null)
			return QueryType.valueOf(q.stringValue());
		return null;
	}

	/**
	 * Return the original explicit {@link Operation#getMaxExecutionTime()} in seconds, 0 if
	 * {@link FedXConfig#getEnforceMaxQueryTime()} should be applied.
	 * 
	 * @param b
	 * @return
	 */
	private static int getOriginalMaxExecutionTime(BindingSet b) {
		if (b == null)
			return 0;
		Value q = b.getValue(FedXRepositoryConnection.BINDING_ORIGINAL_MAX_EXECUTION_TIME);
		if (q != null)
			return Integer.parseInt(q.stringValue());
		return 0;
	}

	/**
	 * A default implementation for {@link AbstractSail}. This implementation has no further use, however it is needed
	 * for the constructor call.
	 * 
	 * @author as
	 *
	 */
	protected static class SailBaseDefaultImpl extends AbstractSail {

		@Override
		protected SailConnection getConnectionInternal() throws SailException {
			return null;
		}

		@Override
		protected void shutDownInternal() throws SailException {
		}

		@Override
		public ValueFactory getValueFactory() {
			return FedXUtil.valueFactory();
		}

		@Override
		public boolean isWritable() throws SailException {
			return false;
		}

		@Override
		protected void connectionClosed(SailConnection connection) {
			// we do not need this in FedX
		}
	}

	@Override
	public boolean pendingRemovals() {
		return false;
	}

}
