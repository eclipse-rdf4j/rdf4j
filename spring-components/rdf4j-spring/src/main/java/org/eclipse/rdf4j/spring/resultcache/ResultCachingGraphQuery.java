package org.eclipse.rdf4j.spring.resultcache;

import java.lang.invoke.MethodHandles;
import java.lang.ref.WeakReference;

import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.GraphQueryResult;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.spring.support.query.DelegatingGraphQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResultCachingGraphQuery extends DelegatingGraphQuery {
	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	private WeakReference<ResultCache<Integer, ReusableGraphQueryResult>> localResultCacheRef;
	private ResultCache<Integer, ReusableGraphQueryResult> globalResultCache;
	private ResultCacheProperties properties;

	public ResultCachingGraphQuery(
			GraphQuery delegate,
			ResultCache<Integer, ReusableGraphQueryResult> localResultCache,
			ResultCache<Integer, ReusableGraphQueryResult> globalResultCache,
			ResultCacheProperties properties) {
		super(delegate);
		this.localResultCacheRef = new WeakReference<>(localResultCache);
		this.globalResultCache = globalResultCache;
		this.properties = properties;
	}

	public void renewLocalResultCache(
			ResultCache<Integer, ReusableGraphQueryResult> localGraphQueryResultCache) {
		if (logger.isDebugEnabled()) {
			ResultCache<Integer, ReusableGraphQueryResult> previousCache = localResultCacheRef.get();
			logger.debug(
					"resetting local result cache to {} (was: {})",
					localGraphQueryResultCache.hashCode(),
					previousCache != null ? previousCache.hashCode() : "null");
		}
		this.localResultCacheRef = new WeakReference<>(localGraphQueryResultCache);
	}

	@Override
	public GraphQueryResult evaluate() throws QueryEvaluationException {
		BindingSet currentBindings = getDelegate().getBindings();
		Integer cacheKey = currentBindings.hashCode() + getDelegate().hashCode();
		GraphQueryResult cachedResult;
		logger.debug("Checking global result cache");
		if (properties.isAssumeNoOtherRepositoryClients()) {
			cachedResult = recreateCachedResultIfPossible(currentBindings, cacheKey, globalResultCache);
			if (cachedResult != null) {
				return cachedResult;
			}
		}
		logger.debug("Cehcking local result cache");
		ResultCache<Integer, ReusableGraphQueryResult> localResultCache = localResultCacheRef.get();
		if (localResultCache != null) {
			cachedResult = recreateCachedResultIfPossible(currentBindings, cacheKey, localResultCache);
			if (cachedResult != null) {
				return cachedResult;
			}
		}
		logger.debug("No reusable cached result found, executing query");
		GraphQueryResult delegateResult = getDelegate().evaluate();
		if (delegateResult instanceof ReusableGraphQueryResult) {
			throw new IllegalStateException(
					"Cannot cache an already cached result! This should not happen, the caching layer seems misconfigured.");
		}
		ReusableGraphQueryResult cacheableResult = new ReusableGraphQueryResult(delegateResult, currentBindings);
		if (localResultCache != null) {
			localResultCache.put(cacheKey, cacheableResult);
		}
		return cacheableResult;
	}

	private GraphQueryResult recreateCachedResultIfPossible(
			BindingSet currentBindings,
			Integer cacheKey,
			ResultCache<Integer, ReusableGraphQueryResult> cache) {
		ReusableGraphQueryResult result;
		result = cache.get(cacheKey);
		if (result != null
				&& result.queryBindingsAreIdentical(currentBindings)
				&& result.canReuse()) {
			logger.debug("Reusing previously calculated result");
			return result.recreateGraphQueryResult();
		}
		return null;
	}
}
