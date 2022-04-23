/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.spring.operationcache;

import static org.eclipse.rdf4j.spring.util.RepositoryConnectionWrappingUtils.findWrapper;

import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import java.util.function.Supplier;

import org.apache.commons.collections4.map.LRUMap;
import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.Operation;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.spring.resultcache.CachingRepositoryConnection;
import org.eclipse.rdf4j.spring.resultcache.ClearableAwareUpdate;
import org.eclipse.rdf4j.spring.resultcache.ResultCachingGraphQuery;
import org.eclipse.rdf4j.spring.resultcache.ResultCachingTupleQuery;
import org.eclipse.rdf4j.spring.support.DirectOperationInstantiator;
import org.eclipse.rdf4j.spring.util.RepositoryConnectionWrappingUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 4.0.0
 * @author Florian Kleedorfer
 */
public class CachingOperationInstantiator extends DirectOperationInstantiator {
	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	private final Map<RepositoryConnection, Map<String, Operation>> cachedOperations = Collections
			.synchronizedMap(new WeakHashMap<>());

	@Override
	public TupleQuery getTupleQuery(
			RepositoryConnection con,
			Class<?> owner,
			String operationName,
			Supplier<String> tupleQueryStringSupplier) {
		return cachedOrNewOp(
				TupleQuery.class,
				con,
				owner,
				operationName,
				() -> getTupleQuery(con, tupleQueryStringSupplier.get()));
	}

	@Override
	public Update getUpdate(
			RepositoryConnection con,
			Class<?> owner,
			String operationName,
			Supplier<String> updateStringSupplier) {
		return cachedOrNewOp(
				Update.class,
				con,
				owner,
				operationName,
				() -> getUpdate(con, updateStringSupplier.get()));
	}

	@Override
	public GraphQuery getGraphQuery(
			RepositoryConnection con,
			Class<?> owner,
			String operationName,
			Supplier<String> graphQueryStringSupplier) {
		return cachedOrNewOp(
				GraphQuery.class,
				con,
				owner,
				operationName,
				() -> getGraphQuery(con, graphQueryStringSupplier.get()));
	}

	private <T extends Operation> T cachedOrNewOp(
			Class<T> type,
			RepositoryConnection con,
			Class<?> owner,
			String operationName,
			Supplier<Operation> operationSupplier) {
		String key = makeOperationCacheKey(type, owner, operationName);
		if (logger.isDebugEnabled()) {
			logger.debug(
					"Obtaining operation of type {} for owner {} with name {}",
					type.getSimpleName(),
					owner,
					operationName);
		}
		RepositoryConnection rootConnection = RepositoryConnectionWrappingUtils.findRoot(con);
		Map<String, Operation> cachedOperationsForConnection = this.cachedOperations.get(rootConnection);
		if (cachedOperationsForConnection == null) {
			if (logger.isDebugEnabled()) {
				logger.debug(
						"No operations cached with connection yet, initializing operation cache for connection {}",
						rootConnection.hashCode());
			}
			cachedOperationsForConnection = new LRUMap<>(200, 10);
			this.cachedOperations.put(rootConnection, cachedOperationsForConnection);
		}
		Operation op = cachedOperationsForConnection.get(key);
		if (op == null) {
			if (logger.isDebugEnabled()) {
				logger.debug("Instantiating operation and caching for future reuse");
			}
			op = operationSupplier.get();
			cachedOperationsForConnection.put(key, op);
		} else {
			renewLocalCacheIfPossible(op, con);
			if (logger.isDebugEnabled()) {
				logger.debug("Reusing cached operation");
			}
		}
		return (T) op;
	}

	private void renewLocalCacheIfPossible(Operation op, RepositoryConnection con) {
		Optional<CachingRepositoryConnection> wrapperOpt = findWrapper(con, CachingRepositoryConnection.class);
		if (wrapperOpt.isPresent()) {
			CachingRepositoryConnection cachingCon = wrapperOpt.get();
			if (op instanceof ResultCachingGraphQuery) {
				cachingCon.renewLocalResultCache((ResultCachingGraphQuery) op);
			} else if (op instanceof ResultCachingTupleQuery) {
				cachingCon.renewLocalResultCache((ResultCachingTupleQuery) op);
			} else if (op instanceof ClearableAwareUpdate) {
				((ClearableAwareUpdate) op).renewClearable(cachingCon);
			}
		}
	}

	private <T extends Operation> String makeOperationCacheKey(
			Class<T> operationType, Class<?> owner, String name) {
		return operationType.getSimpleName() + ":" + owner.getName() + ":" + name;
	}
}
