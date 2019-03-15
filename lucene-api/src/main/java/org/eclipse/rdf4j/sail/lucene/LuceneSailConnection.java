/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lucene;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.BindingSetAssignment;
import org.eclipse.rdf4j.query.algebra.Join;
import org.eclipse.rdf4j.query.algebra.LeftJoin;
import org.eclipse.rdf4j.query.algebra.MultiProjection;
import org.eclipse.rdf4j.query.algebra.Projection;
import org.eclipse.rdf4j.query.algebra.QueryModelNode;
import org.eclipse.rdf4j.query.algebra.QueryModelVisitor;
import org.eclipse.rdf4j.query.algebra.QueryRoot;
import org.eclipse.rdf4j.query.algebra.SingletonSet;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.UnaryTupleOperator;
import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategy;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryBindingSet;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryContext;
import org.eclipse.rdf4j.query.algebra.evaluation.federation.AbstractFederatedServiceResolver;
import org.eclipse.rdf4j.query.algebra.evaluation.federation.FederatedServiceResolver;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.BindingAssigner;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.CompareOptimizer;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.ConjunctiveConstraintSplitter;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.ConstantOptimizer;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.DisjunctiveConstraintOptimizer;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.FilterOptimizer;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.IterativeEvaluationOptimizer;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.OrderLimitOptimizer;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.QueryJoinOptimizer;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.QueryModelNormalizer;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.SameTermFilterOptimizer;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.TupleFunctionEvaluationStatistics;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.TupleFunctionEvaluationStrategy;
import org.eclipse.rdf4j.query.algebra.evaluation.iterator.QueryContextIteration;
import org.eclipse.rdf4j.query.algebra.helpers.AbstractQueryModelVisitor;
import org.eclipse.rdf4j.sail.NotifyingSailConnection;
import org.eclipse.rdf4j.sail.SailConnectionListener;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.evaluation.SailTripleSource;
import org.eclipse.rdf4j.sail.evaluation.TupleFunctionEvaluationMode;
import org.eclipse.rdf4j.sail.helpers.NotifyingSailConnectionWrapper;
import org.eclipse.rdf4j.sail.lucene.LuceneSailBuffer.AddRemoveOperation;
import org.eclipse.rdf4j.sail.lucene.LuceneSailBuffer.ClearContextOperation;
import org.eclipse.rdf4j.sail.lucene.LuceneSailBuffer.Operation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

/**
 * <h2><a name="whySailConnectionListener">Sail Connection Listener instead of implementing add/remove</a></h2> Using
 * SailConnectionListener, see <a href="#whySailConnectionListener">above</a> The LuceneIndex is adapted based on events
 * coming from the wrapped Sail, rather than by overriding the addStatement and removeStatements methods. This approach
 * has two benefits: (1) when the wrapped Sail only reports statements that were not stored before, the LuceneIndex does
 * not have to do the check on the skipped statemements and (2) the method for removing Statements from the Lucene index
 * does not have to take wildcards into account, making its implementation simpler.
 * <h2>Synchronized Methods</h2> LuceneSailConnection uses a listener to collect removed statements. The listener should
 * not be active during the removal of contexts, as this is not needed (context removal is implemented differently). To
 * realize this, all methods that can do changes are synchronized and during context removal, the listener is disabled.
 * Thus, all methods of this connection that can change data are synchronized.
 * <h2>Evaluating Queries - possible optimizations</h2> Arjohn has answered this question in the sesame-dev mailinglist
 * on 13.8.2007: <b>Is there a QueryModelNode that can contain a fixed (perhaps very long) list of Query result
 * bindings?</b> There is currently no such node, but there are two options to get similar behaviour: 1) Include the
 * result bindings as OR-ed constraints in the query model. E.g. if you have a result binding like
 * {{x=1,y=1},{x=2,y=2}}, this translates to the constraints (x=1 and y=1) or (x=2 and y=2). 2) The LuceneSail could
 * iterate over the LuceneQueryResult and supply the various results as query input parameters to the underlying Sail.
 * This is similar to using PreparedStatement's in JDBC.
 * 
 * @author sauermann
 * @author christian.huetter
 */
public class LuceneSailConnection extends NotifyingSailConnectionWrapper {

	@SuppressWarnings("unchecked")
	private static final Set<Class<? extends QueryModelNode>> PROJECTION_TYPES = Sets.newHashSet(Projection.class,
			MultiProjection.class);

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	private final SearchIndex luceneIndex;

	@SuppressWarnings("unused")
	private final AbstractFederatedServiceResolver tupleFunctionServiceResolver;

	private final LuceneSail sail;

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
				statement = sail.mapStatement(statement);
				if (statement == null)
					return;
				// we further only index statements where the Literal's datatype is
				// accepted
				Literal literal = (Literal) statement.getObject();
				if (luceneIndex.accept(literal))
					buffer.add(statement);
			}
		}

		@Override
		public void statementRemoved(Statement statement) {
			// we only consider statements that contain literals
			if (statement.getObject() instanceof Literal) {
				statement = sail.mapStatement(statement);
				if (statement == null)
					return;
				// we further only indexed statements where the Literal's datatype
				// is accepted
				Literal literal = (Literal) statement.getObject();
				if (luceneIndex.accept(literal))
					buffer.remove(statement);
			}
		}
	};

	/**
	 * To remember if the iterator was already closed and only free resources once
	 */
	private final AtomicBoolean closed = new AtomicBoolean(false);

	public LuceneSailConnection(NotifyingSailConnection wrappedConnection, SearchIndex luceneIndex, LuceneSail sail) {
		super(wrappedConnection);
		this.luceneIndex = luceneIndex;
		this.sail = sail;

		if (sail.getEvaluationMode() == TupleFunctionEvaluationMode.SERVICE) {
			FederatedServiceResolver resolver = sail.getFederatedServiceResolver();
			if (!(resolver instanceof AbstractFederatedServiceResolver)) {
				throw new IllegalArgumentException(
						"SERVICE EvaluationMode requires a FederatedServiceResolver that is an instance of "
								+ AbstractFederatedServiceResolver.class.getName());
			}
			this.tupleFunctionServiceResolver = (AbstractFederatedServiceResolver) resolver;
		} else {
			this.tupleFunctionServiceResolver = null;
		}

		/*
		 * Using SailConnectionListener, see <a href="#whySailConnectionListener">above</a>
		 */

		wrappedConnection.addConnectionListener(connectionListener);
	}

	@Override
	public synchronized void addStatement(Resource subj, IRI pred, Value obj, Resource... contexts)
			throws SailException {
		super.addStatement(subj, pred, obj, contexts);
	}

	@Override
	public void close() throws SailException {
		if (closed.compareAndSet(false, true)) {
			super.close();
		}
	}

	// //////////////////////////////// Methods related to indexing

	@Override
	public synchronized void clear(Resource... contexts) throws SailException {
		// remove the connection listener, this is safe as the changing methods
		// are synchronized
		// during the clear(), no other operation can be invoked
		getWrappedConnection().removeConnectionListener(connectionListener);
		try {
			super.clear(contexts);
			buffer.clear(contexts);
		} finally {
			getWrappedConnection().addConnectionListener(connectionListener);
		}
	}

	@Override
	public void begin() throws SailException {
		super.begin();
		buffer.reset();
		try {
			luceneIndex.begin();
		} catch (IOException e) {
			throw new SailException(e);
		}
	}

	@Override
	public void commit() throws SailException {
		super.commit();

		logger.debug("Committing Lucene transaction with {} operations.", buffer.operations().size());
		try {
			// preprocess buffer
			buffer.optimize();

			// run operations and remove them from buffer
			for (Iterator<Operation> i = buffer.operations().iterator(); i.hasNext();) {
				Operation op = i.next();
				if (op instanceof LuceneSailBuffer.AddRemoveOperation) {
					AddRemoveOperation addremove = (AddRemoveOperation) op;
					// add/remove in one call
					addRemoveStatements(addremove.getAdded(), addremove.getRemoved());
				} else if (op instanceof LuceneSailBuffer.ClearContextOperation) {
					// clear context
					clearContexts(((ClearContextOperation) op).getContexts());
				} else if (op instanceof LuceneSailBuffer.ClearOperation) {
					logger.debug("clearing index...");
					luceneIndex.clear();
				} else {
					throw new SailException("Cannot interpret operation " + op + " of type " + op.getClass().getName());
				}
				i.remove();
			}
		} catch (Exception e) {
			logger.error("Committing operations in lucenesail, encountered exception " + e
					+ ". Only some operations were stored, " + buffer.operations().size()
					+ " operations are discarded. Lucene Index is now corrupt.", e);
			throw new SailException(e);
		} finally {
			buffer.reset();
		}
	}

	private void addRemoveStatements(Set<Statement> toAdd, Set<Statement> toRemove) throws IOException {
		logger.debug("indexing {}/removing {} statements...", toAdd.size(), toRemove.size());
		luceneIndex.begin();
		try {
			luceneIndex.addRemoveStatements(toAdd, toRemove);
			luceneIndex.commit();
		} catch (IOException e) {
			logger.error("Rolling back", e);
			luceneIndex.rollback();
			throw e;
		}
	}

	private void clearContexts(Resource... contexts) throws IOException {
		logger.debug("clearing contexts...");
		luceneIndex.begin();
		try {
			luceneIndex.clearContexts(contexts);
			luceneIndex.commit();
		} catch (IOException e) {
			logger.error("Rolling back", e);
			luceneIndex.rollback();
			throw e;
		}
	}

	// //////////////////////////////// Methods related to querying

	@Override
	public synchronized CloseableIteration<? extends BindingSet, QueryEvaluationException> evaluate(TupleExpr tupleExpr,
			Dataset dataset, BindingSet bindings, boolean includeInferred) throws SailException {
		QueryContext qctx = new QueryContext();
		SearchIndexQueryContextInitializer.init(qctx, luceneIndex);

		final CloseableIteration<? extends BindingSet, QueryEvaluationException> iter;
		qctx.begin();
		try {
			iter = evaluateInternal(tupleExpr, dataset, bindings, includeInferred);
		} finally {
			qctx.end();
		}

		// NB: Iteration methods may do on-demand evaluation hence need to wrap
		// these too
		return new QueryContextIteration(iter, qctx);
	}

	private CloseableIteration<? extends BindingSet, QueryEvaluationException> evaluateInternal(TupleExpr tupleExpr,
			Dataset dataset, BindingSet bindings, boolean includeInferred) throws SailException {
		// Don't modify the original tuple expression
		tupleExpr = tupleExpr.clone();

		if (!(tupleExpr instanceof QueryRoot)) {
			// Add a dummy root node to the tuple expressions to allow the
			// optimizers to modify the actual root node
			tupleExpr = new QueryRoot(tupleExpr);
		}

		// Inline any externally set bindings, lucene statement patterns can also
		// use externally bound variables
		new BindingAssigner().optimize(tupleExpr, dataset, bindings);

		List<SearchQueryEvaluator> queries = new ArrayList<>();

		for (SearchQueryInterpreter interpreter : sail.getSearchQueryInterpreters()) {
			interpreter.process(tupleExpr, bindings, queries);
		}

		// constant optimizer - evaluate lucene queries
		if (!queries.isEmpty()) {
			evaluateLuceneQueries(queries, tupleExpr);
		}

		if (sail.getEvaluationMode() == TupleFunctionEvaluationMode.TRIPLE_SOURCE) {
			ValueFactory vf = sail.getValueFactory();
			EvaluationStrategy strategy = new TupleFunctionEvaluationStrategy(
					new SailTripleSource(this, includeInferred, vf), dataset, sail.getFederatedServiceResolver(),
					sail.getTupleFunctionRegistry());

			// do standard optimizations
			new BindingAssigner().optimize(tupleExpr, dataset, bindings);
			new ConstantOptimizer(strategy).optimize(tupleExpr, dataset, bindings);
			new CompareOptimizer().optimize(tupleExpr, dataset, bindings);
			new ConjunctiveConstraintSplitter().optimize(tupleExpr, dataset, bindings);
			new DisjunctiveConstraintOptimizer().optimize(tupleExpr, dataset, bindings);
			new SameTermFilterOptimizer().optimize(tupleExpr, dataset, bindings);
			new QueryModelNormalizer().optimize(tupleExpr, dataset, bindings);
			new QueryJoinOptimizer(new TupleFunctionEvaluationStatistics()).optimize(tupleExpr, dataset, bindings);
			// new SubSelectJoinOptimizer().optimize(tupleExpr, dataset,
			// bindings);
			new IterativeEvaluationOptimizer().optimize(tupleExpr, dataset, bindings);
			new FilterOptimizer().optimize(tupleExpr, dataset, bindings);
			new OrderLimitOptimizer().optimize(tupleExpr, dataset, bindings);

			logger.trace("Optimized query model:\n{}", tupleExpr);

			try {
				return strategy.evaluate(tupleExpr, bindings);
			} catch (QueryEvaluationException e) {
				throw new SailException(e);
			}
		} else {
			return super.evaluate(tupleExpr, dataset, bindings, includeInferred);
		}
	}

	/**
	 * Evaluate the given Lucene queries, generate bindings from the query result, add the bindings to the query tree,
	 * and remove the Lucene queries from the given query tree.
	 * 
	 * @param queries
	 * @param tupleExpr
	 * @throws SailException
	 */
	private void evaluateLuceneQueries(Collection<SearchQueryEvaluator> queries, TupleExpr tupleExpr)
			throws SailException {
		// TODO: optimize lucene queries here
		// - if they refer to the same subject, merge them into one lucene query
		// - multiple different property constraints can be put into the lucene
		// query string (escape colons here)

		if (closed.get()) {
			throw new SailException("Sail has been closed already");
		}

		// evaluate queries, generate binding sets, and remove queries
		for (SearchQueryEvaluator query : queries) {
			// evaluate the Lucene query and generate bindings
			Collection<BindingSet> bindingSets = luceneIndex.evaluate(query);

			boolean hasResult = bindingSets != null && !bindingSets.isEmpty();

			// found something?
			if (hasResult) {
				// add bindings to the query tree
				BindingSetAssignment bsa = new BindingSetAssignment();
				bsa.setBindingSets(bindingSets);
				if (bindingSets instanceof BindingSetCollection) {
					bsa.setBindingNames(((BindingSetCollection) bindingSets).getBindingNames());
				}
				addBindingSets(query, bsa);
			}

			// remove the evaluated lucene query from the query tree
			query.updateQueryModelNodes(hasResult);
		}
	}

	/**
	 * Join the given bindings and add them to the given query tree.
	 * 
	 * @param bindingSets bindings for the search query
	 * @param tupleExpr   query tree
	 * @param query       the search query to which the bindings belong
	 */
	private void addBindingSets(SearchQueryEvaluator query, BindingSetAssignment bindingSets) {

		// find projection for the given query
		QueryModelNode principalNode = query.getParentQueryModelNode();
		final UnaryTupleOperator projection = (UnaryTupleOperator) getParentNodeOfTypes(principalNode,
				PROJECTION_TYPES);
		if (projection == null) {
			logger.error(
					"Could not add bindings to the query tree because no projection was found for the query node: {}",
					principalNode);
			return;
		}

		// find existing bindings within the given (sub-)query
		final List<BindingSetAssignment> assignments = new ArrayList<>();
		QueryModelVisitor<RuntimeException> assignmentVisitor = new AbstractQueryModelVisitor<RuntimeException>() {

			@Override
			public void meet(BindingSetAssignment node) throws RuntimeException {
				// does the node belong to the same (sub-)query?
				QueryModelNode parent = getParentNodeOfTypes(node, PROJECTION_TYPES);
				if (parent != null && parent.equals(projection))
					assignments.add(node);
			}
		};
		projection.visit(assignmentVisitor);

		// construct a list of binding sets
		List<BindingSetAssignment> bindingSetsList = new ArrayList<>();
		bindingSetsList.add(bindingSets);

		// add existing bindings to the list of binding sets and remove them from
		// the query tree
		for (BindingSetAssignment assignment : assignments) {
			bindingSetsList.add(assignment);
			assignment.replaceWith(new SingletonSet());
		}

		// join binding sets
		BindingSetAssignment bindings = joinBindingSets(bindingSetsList.iterator());

		// add bindings to the projection
		TupleExpr arg = projection.getArg();

		// required to support OPTIONAL patterns (which are represented as
		// LeftJoin)
		if (arg instanceof LeftJoin) {
			LeftJoin binary = (LeftJoin) arg;
			Join join = new Join(bindings, binary.getLeftArg());
			binary.setLeftArg(join);
		} else {
			Join join = new Join(bindings, arg);
			projection.setArg(join);
		}
	}

	/**
	 * Returns the closest parent node of the given type.
	 */
	private QueryModelNode getParentNodeOfTypes(QueryModelNode node, Set<Class<? extends QueryModelNode>> types) {
		QueryModelNode parent = node.getParentNode();
		if (parent == null) {
			return null;
		} else if (types.contains(parent.getClass())) {
			return parent;
		} else {
			return getParentNodeOfTypes(parent, types);
		}
	}

	/**
	 * Recursively join the given binding sets.
	 * 
	 * @param iterator for the binding sets to join
	 * @return joined binding sets
	 */
	private BindingSetAssignment joinBindingSets(Iterator<BindingSetAssignment> iterator) {
		if (iterator.hasNext()) {
			BindingSetAssignment left = iterator.next();
			BindingSetAssignment right = joinBindingSets(iterator);
			if (right != null) {
				return crossJoin(left, right);
			} else {
				return left;
			}
		} else {
			return null;
		}
	}

	/**
	 * Computes the Cartesian product of the given binding sets.
	 * 
	 * @param left  binding sets
	 * @param right binding sets
	 * @return Cartesian product TODO: implement as sort-merge join
	 */
	private BindingSetAssignment crossJoin(BindingSetAssignment left, BindingSetAssignment right) {
		Iterable<BindingSet> leftIter = left.getBindingSets();
		Iterable<BindingSet> rightIter = right.getBindingSets();
		int leftSize = size(leftIter, 16);
		int rightSize = size(rightIter, 16);
		List<BindingSet> output = new ArrayList<>(leftSize * rightSize);

		for (BindingSet l : leftIter) {
			for (BindingSet r : rightIter) {
				QueryBindingSet bs = new QueryBindingSet();
				bs.addAll(l);
				bs.addAll(r);
				output.add(bs);
			}
		}

		Set<String> bindingNames = new HashSet<>(left.getBindingNames());
		bindingNames.addAll(right.getBindingNames());

		BindingSetAssignment bindings = new BindingSetAssignment();
		bindings.setBindingSets(output);
		bindings.setBindingNames(bindingNames);
		return bindings;
	}

	private static int size(Iterable<?> iter, int defaultSize) {
		return (iter instanceof Collection<?>) ? ((Collection<?>) iter).size() : defaultSize;
	}

	@Override
	public synchronized void removeStatements(Resource subj, IRI pred, Value obj, Resource... contexts)
			throws SailException {
		super.removeStatements(subj, pred, obj, contexts);
	}

	@Override
	public void rollback() throws SailException {
		super.rollback();
		buffer.reset();
		try {
			luceneIndex.rollback();
		} catch (IOException e) {
			throw new SailException(e);
		}
	}
}
