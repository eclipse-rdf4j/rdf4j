/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.federation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.http.client.HttpClient;
import org.eclipse.rdf4j.IsolationLevel;
import org.eclipse.rdf4j.IsolationLevels;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.CloseableIteratorIteration;
import org.eclipse.rdf4j.common.iteration.DistinctIteration;
import org.eclipse.rdf4j.common.iteration.ExceptionConvertingIteration;
import org.eclipse.rdf4j.common.iteration.UnionIteration;
import org.eclipse.rdf4j.http.client.HttpClientDependent;
import org.eclipse.rdf4j.http.client.SesameClient;
import org.eclipse.rdf4j.http.client.SesameClientDependent;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.QueryRoot;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategy;
import org.eclipse.rdf4j.query.algebra.evaluation.TripleSource;
import org.eclipse.rdf4j.query.algebra.evaluation.federation.FederatedServiceResolver;
import org.eclipse.rdf4j.query.algebra.evaluation.federation.FederatedServiceResolverClient;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.BindingAssigner;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.CompareOptimizer;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.ConjunctiveConstraintSplitter;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.ConstantOptimizer;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.DisjunctiveConstraintOptimizer;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.SameTermFilterOptimizer;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.SimpleEvaluationStrategy;
import org.eclipse.rdf4j.query.impl.EmptyBindingSet;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.repository.sail.config.RepositoryResolver;
import org.eclipse.rdf4j.repository.sail.config.RepositoryResolverClient;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.federation.evaluation.FederationStrategy;
import org.eclipse.rdf4j.sail.federation.optimizers.EmptyPatternOptimizer;
import org.eclipse.rdf4j.sail.federation.optimizers.FederationJoinOptimizer;
import org.eclipse.rdf4j.sail.federation.optimizers.OwnedTupleExprPruner;
import org.eclipse.rdf4j.sail.federation.optimizers.PrepareOwnedTupleExpr;
import org.eclipse.rdf4j.sail.federation.optimizers.QueryModelPruner;
import org.eclipse.rdf4j.sail.federation.optimizers.QueryMultiJoinOptimizer;
import org.eclipse.rdf4j.sail.helpers.AbstractSail;
import org.eclipse.rdf4j.sail.helpers.AbstractSailConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Unions the results from multiple {@link RepositoryConnection} into one
 * {@link SailConnection}.
 * 
 * @author James Leigh
 * @author Arjohn Kampman
 */
abstract class AbstractFederationConnection extends AbstractSailConnection implements
		FederatedServiceResolverClient, RepositoryResolverClient, HttpClientDependent, SesameClientDependent
{

	private static final Logger LOGGER = LoggerFactory.getLogger(AbstractFederationConnection.class);

	private final Federation federation;

	private final ValueFactory valueFactory;

	protected final List<RepositoryConnection> members;

	/**
	 * Connection specific resolver.
	 */
	private FederatedServiceResolver federatedServiceResolver;

	public AbstractFederationConnection(Federation federation, List<RepositoryConnection> members) {
		super(new AbstractSail() {

			public boolean isWritable()
				throws SailException
			{
				return false;
			}

			public ValueFactory getValueFactory() {
				return SimpleValueFactory.getInstance();
			}

			@Override
			protected void shutDownInternal()
				throws SailException
			{
				// ignore
			}

			@Override
			protected SailConnection getConnectionInternal()
				throws SailException
			{
				return null;
			}

			@Override
			protected void connectionClosed(SailConnection connection) {
				// ignore
			}

			@Override
			public List<IsolationLevel> getSupportedIsolationLevels() {
				return Arrays.asList(new IsolationLevel[] { IsolationLevels.NONE });
			}
			
			@Override
			public IsolationLevel getDefaultIsolationLevel() {
				return IsolationLevels.NONE;
			}
		});
		this.federation = federation;

		valueFactory = SimpleValueFactory.getInstance();

		this.members = new ArrayList<RepositoryConnection>(members.size());
		for (RepositoryConnection member : members) {
			this.members.add(member);
		}
	}

	public ValueFactory getValueFactory() {
		return valueFactory;
	}

	@Override
	public void closeInternal()
		throws SailException
	{
		excute(new Procedure() {

			public void run(RepositoryConnection con)
				throws RepositoryException
			{
				con.close();
			}
		});
	}

	@Override
	public CloseableIteration<? extends Resource, SailException> getContextIDsInternal()
		throws SailException
	{
		CloseableIteration<? extends Resource, SailException> cursor = union(new Function<Resource>() {

			public CloseableIteration<? extends Resource, RepositoryException> call(RepositoryConnection member)
				throws RepositoryException
			{
				return member.getContextIDs();
			}
		});

		cursor = new DistinctIteration<Resource, SailException>(cursor);

		return cursor;
	}

	public FederatedServiceResolver getFederatedServiceResolver() {
		if (federatedServiceResolver == null)
			return federation.getFederatedServiceResolver();
		return federatedServiceResolver;
	}

	public void setFederatedServiceResolver(FederatedServiceResolver resolver) {
		this.federatedServiceResolver = resolver;
		for (RepositoryConnection member : members) {
			if (member instanceof FederatedServiceResolverClient) {
				((FederatedServiceResolverClient) member).setFederatedServiceResolver(resolver);
			}
		}
	}

	@Override
	public void setRepositoryResolver(RepositoryResolver resolver) {
		for (RepositoryConnection member : members) {
			if (member instanceof RepositoryResolverClient) {
				((RepositoryResolverClient) member).setRepositoryResolver(resolver);
			}
		}
	}

	@Override
	public SesameClient getSesameClient() {
		for (RepositoryConnection member : members) {
			if (member instanceof SesameClientDependent) {
				SesameClient client = ((SesameClientDependent) member).getSesameClient();
				if (client != null) {
					return client;
				}
			}
		}
		return null;
	}

	@Override
	public void setSesameClient(SesameClient client) {
		for (RepositoryConnection member : members) {
			if (member instanceof SesameClientDependent) {
				((SesameClientDependent) member).setSesameClient(client);
			}
		}
	}

	@Override
	public HttpClient getHttpClient() {
		for (RepositoryConnection member : members) {
			if (member instanceof HttpClientDependent) {
				HttpClient client = ((HttpClientDependent) member).getHttpClient();
				if (client != null) {
					return client;
				}
			}
		}
		return null;
	}

	@Override
	public void setHttpClient(HttpClient client) {
		for (RepositoryConnection member : members) {
			if (member instanceof HttpClientDependent) {
				((HttpClientDependent) member).setHttpClient(client);
			}
		}
	}

	@Override
	public String getNamespaceInternal(String prefix)
		throws SailException
	{
		try {
			String namespace = null;
			for (RepositoryConnection member : members) {
				String candidate = member.getNamespace(prefix);
				if (namespace == null) {
					namespace = candidate;
				}
				else if (candidate != null && !candidate.equals(namespace)) {
					namespace = null; // NOPMD
					break;
				}
			}
			return namespace;
		}
		catch (RepositoryException e) {
			throw new SailException(e);
		}
	}

	@Override
	public CloseableIteration<? extends Namespace, SailException> getNamespacesInternal()
		throws SailException
	{
		Map<String, Namespace> namespaces = new HashMap<String, Namespace>();
		Set<String> prefixes = new HashSet<String>();
		Set<String> conflictedPrefixes = new HashSet<String>();

		try {
			for (RepositoryConnection member : members) {
				RepositoryResult<Namespace> memberNamespaces = member.getNamespaces();
				try {
					while (memberNamespaces.hasNext()) {
						Namespace next = memberNamespaces.next();
						String prefix = next.getPrefix();

						if (prefixes.add(prefix)) {
							namespaces.put(prefix, next);
						}
						else if (!next.getName().equals(
								namespaces.get(prefix).getName())) {
							conflictedPrefixes.add(prefix);
						}
					}
				}
				finally {
					memberNamespaces.close();
				}
			}
		}
		catch (RepositoryException e) {
			throw new SailException(e);
		}
		for (String prefix: conflictedPrefixes) {
			namespaces.remove(prefix);
		}
		return new CloseableIteratorIteration<Namespace, SailException>(
				namespaces.values().iterator());
	}

	@Override
	public long sizeInternal(Resource... contexts)
		throws SailException
	{
		try {
			if (federation.isDistinct()) {
				long size = 0;
				for (RepositoryConnection member : members) {
					size += member.size(contexts);
				}
				return size; // NOPMD
			}
			else {
				CloseableIteration<? extends Statement, SailException> cursor = getStatements(null, null, null,
						true, contexts);
				try {
					long size = 0;
					while (cursor.hasNext()) {
						cursor.next();
						size++;
					}
					return size;
				}
				finally {
					cursor.close();
				}
			}
		}
		catch (RepositoryException e) {
			throw new SailException(e);
		}
	}

	@Override
	public CloseableIteration<? extends Statement, SailException> getStatementsInternal(final Resource subj,
			final IRI pred, final Value obj, final boolean includeInferred, final Resource... contexts)
		throws SailException
	{
		CloseableIteration<? extends Statement, SailException> cursor = union(new Function<Statement>() {

			public CloseableIteration<? extends Statement, RepositoryException> call(RepositoryConnection member)
				throws RepositoryException
			{
				return member.getStatements(subj, pred, obj, includeInferred, contexts);
			}
		});

		if (!federation.isDistinct() && !isLocal(pred)) {
			// Filter any duplicates
			cursor = new DistinctIteration<Statement, SailException>(cursor);
		}

		return cursor;
	}

	@Override
	public CloseableIteration<? extends BindingSet, QueryEvaluationException> evaluateInternal(
			TupleExpr query, Dataset dataset, BindingSet bindings, boolean inf)
		throws SailException
	{
		TripleSource tripleSource = new FederationTripleSource(inf);
		EvaluationStrategy strategy = federation.createEvaluationStrategy(tripleSource, dataset,
				getFederatedServiceResolver());
		TupleExpr qry = optimize(query, dataset, bindings, strategy);
		try {
			return strategy.evaluate(qry, EmptyBindingSet.getInstance());
		}
		catch (QueryEvaluationException e) {
			throw new SailException(e);
		}
	}

	private class FederationTripleSource implements TripleSource {

		private final boolean inf;

		public FederationTripleSource(boolean includeInferred) {
			this.inf = includeInferred;
		}

		public CloseableIteration<? extends Statement, QueryEvaluationException> getStatements(Resource subj,
				IRI pred, Value obj, Resource... contexts)
			throws QueryEvaluationException
		{
			try {
				CloseableIteration<? extends Statement, SailException> result = AbstractFederationConnection.this.getStatements(
						subj, pred, obj, inf, contexts);
				return new ExceptionConvertingIteration<Statement, QueryEvaluationException>(result) {

					@Override
					protected QueryEvaluationException convert(Exception e) {
						return new QueryEvaluationException(e);
					}
				};
			}
			catch (SailException e) {
				throw new QueryEvaluationException(e);
			}
		}

		public ValueFactory getValueFactory() {
			return valueFactory;
		}
	}

	private TupleExpr optimize(TupleExpr parsed, Dataset dataset, BindingSet bindings,
			EvaluationStrategy strategy)
		throws SailException
	{
		LOGGER.trace("Incoming query model:\n{}", parsed);

		// Clone the tuple expression to allow for more aggressive optimisations
		TupleExpr query = new QueryRoot(parsed.clone());

		new BindingAssigner().optimize(query, dataset, bindings);
		new ConstantOptimizer(strategy).optimize(query, dataset, bindings);
		new CompareOptimizer().optimize(query, dataset, bindings);
		new ConjunctiveConstraintSplitter().optimize(query, dataset, bindings);
		new DisjunctiveConstraintOptimizer().optimize(query, dataset, bindings);
		new SameTermFilterOptimizer().optimize(query, dataset, bindings);
		new QueryModelPruner().optimize(query, dataset, bindings);

		new QueryMultiJoinOptimizer().optimize(query, dataset, bindings);
		// new FilterOptimizer().optimize(query, dataset, bindings);

		new EmptyPatternOptimizer(members).optimize(query, dataset, bindings);
		boolean distinct = federation.isDistinct();
		PrefixHashSet local = federation.getLocalPropertySpace();
		new FederationJoinOptimizer(members, distinct, local).optimize(query, dataset, bindings);
		new OwnedTupleExprPruner().optimize(query, dataset, bindings);
		new QueryModelPruner().optimize(query, dataset, bindings);
		new QueryMultiJoinOptimizer().optimize(query, dataset, bindings);

		new PrepareOwnedTupleExpr().optimize(query, dataset, bindings);

		LOGGER.trace("Optimized query model:\n{}", query);
		return query;
	}

	interface Procedure {

		void run(RepositoryConnection member)
			throws RepositoryException;
	}

	void excute(Procedure operation)
		throws SailException
	{ // NOPMD
		RepositoryException storeExc = null;
		RuntimeException runtimeExc = null;

		for (RepositoryConnection member : members) {
			try {
				operation.run(member);
			}
			catch (RepositoryException e) {
				LOGGER.error("Failed to execute procedure on federation members", e);
				if (storeExc == null) {
					storeExc = e;
				}
			}
			catch (RuntimeException e) {
				LOGGER.error("Failed to execute procedure on federation members", e);
				if (runtimeExc == null) {
					runtimeExc = e;
				}
			}
		}

		if (storeExc != null) {
			throw new SailException(storeExc);
		}

		if (runtimeExc != null) {
			throw runtimeExc;
		}
	}

	private interface Function<E> {

		CloseableIteration<? extends E, RepositoryException> call(RepositoryConnection member)
			throws RepositoryException;
	}

	private <E> CloseableIteration<? extends E, SailException> union(Function<E> function)
		throws SailException
	{
		List<CloseableIteration<? extends E, RepositoryException>> cursors = new ArrayList<CloseableIteration<? extends E, RepositoryException>>(
				members.size());

		try {
			for (RepositoryConnection member : members) {
				cursors.add(function.call(member));
			}
			UnionIteration<E, RepositoryException> result = new UnionIteration<E, RepositoryException>(cursors);
			return new ExceptionConvertingIteration<E, SailException>(result) {

				@Override
				protected SailException convert(Exception e) {
					return new SailException(e);
				}
			};
		}
		catch (RepositoryException e) {
			closeAll(cursors);
			throw new SailException(e);
		}
		catch (RuntimeException e) {
			closeAll(cursors);
			throw e;
		}
	}

	private boolean isLocal(IRI pred) {
		if (pred == null) {
			return false; // NOPMD
		}

		PrefixHashSet hash = federation.getLocalPropertySpace();
		if (hash == null) {
			return false; // NOPMD
		}

		return hash.match(pred.stringValue());
	}

	private void closeAll(Iterable<? extends CloseableIteration<?, RepositoryException>> cursors) {
		for (CloseableIteration<?, RepositoryException> cursor : cursors) {
			try {
				cursor.close();
			}
			catch (RepositoryException e) {
				LOGGER.error("Failed to close cursor", e);
			}
		}
	}
}
