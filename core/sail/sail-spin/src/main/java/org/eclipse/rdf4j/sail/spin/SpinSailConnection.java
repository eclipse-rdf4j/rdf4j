/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.spin;

import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.eclipse.rdf4j.RDF4JException;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.DynamicModelFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.SPIN;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.QueryRoot;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategy;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryContext;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryContextInitializer;
import org.eclipse.rdf4j.query.algebra.evaluation.TripleSource;
import org.eclipse.rdf4j.query.algebra.evaluation.federation.AbstractFederatedServiceResolver;
import org.eclipse.rdf4j.query.algebra.evaluation.federation.FederatedServiceResolver;
import org.eclipse.rdf4j.query.algebra.evaluation.function.Function;
import org.eclipse.rdf4j.query.algebra.evaluation.function.FunctionRegistry;
import org.eclipse.rdf4j.query.algebra.evaluation.function.TupleFunction;
import org.eclipse.rdf4j.query.algebra.evaluation.function.TupleFunctionRegistry;
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
import org.eclipse.rdf4j.query.algebra.evaluation.util.TripleSources;
import org.eclipse.rdf4j.rio.ParserConfig;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandler;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.sail.SailConnectionListener;
import org.eclipse.rdf4j.sail.SailConnectionQueryPreparer;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.evaluation.SailTripleSource;
import org.eclipse.rdf4j.sail.evaluation.TupleFunctionEvaluationMode;
import org.eclipse.rdf4j.sail.inferencer.InferencerConnection;
import org.eclipse.rdf4j.sail.inferencer.fc.AbstractForwardChainingInferencerConnection;
import org.eclipse.rdf4j.sail.inferencer.util.RDFInferencerInserter;
import org.eclipse.rdf4j.spin.ConstraintViolation;
import org.eclipse.rdf4j.spin.RuleProperty;
import org.eclipse.rdf4j.spin.SpinParser;
import org.eclipse.rdf4j.spin.function.TransientFunction;
import org.eclipse.rdf4j.spin.function.TransientTupleFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

class SpinSailConnection extends AbstractForwardChainingInferencerConnection {

	static private final Logger logger = LoggerFactory.getLogger(SpinSailConnection.class);

	private static final IRI EXECUTED = SimpleValueFactory.getInstance()
			.createIRI(
					"http://www.openrdf.org/schema/spin#executed");

	private static final Marker constraintViolationMarker = MarkerFactory.getMarker("ConstraintViolation");

	private static final String CONSTRAINT_VIOLATION_MESSAGE = "Constraint violation: {}: {} {} {}";

	private final TupleFunctionEvaluationMode evaluationMode;

	private final boolean axiomClosureNeeded;

	private final FunctionRegistry functionRegistry;

	private final TupleFunctionRegistry tupleFunctionRegistry;

	private final FederatedServiceResolver serviceResolver;

	private final AbstractFederatedServiceResolver tupleFunctionServiceResolver;

	private final ValueFactory vf;

	private final TripleSource tripleSource;

	private final List<QueryContextInitializer> queryContextInitializers;

	private final SpinParser parser;

	private List<IRI> orderedRuleProperties;

	private Map<IRI, RuleProperty> rulePropertyMap;

	private Map<Resource, Executions> ruleExecutions;

	private Map<IRI, Set<IRI>> classToSuperclassMap;

	private SailConnectionQueryPreparer queryPreparer;

	private SpinSail sail;

	public SpinSailConnection(SpinSail sail, InferencerConnection con) {
		super(sail, con);
		this.sail = sail;
		this.evaluationMode = sail.getEvaluationMode();
		this.axiomClosureNeeded = sail.isAxiomClosureNeeded();
		this.functionRegistry = sail.getFunctionRegistry();
		this.tupleFunctionRegistry = sail.getTupleFunctionRegistry();
		this.vf = sail.getValueFactory();
		this.queryContextInitializers = sail.getQueryContextInitializers();
		this.parser = sail.getSpinParser();
		this.tripleSource = new SailTripleSource(getWrappedConnection(), true, vf);
		this.queryPreparer = new SailConnectionQueryPreparer(this, true, tripleSource);

		this.serviceResolver = sail.getFederatedServiceResolver();
		if (evaluationMode == TupleFunctionEvaluationMode.SERVICE) {
			if (!(serviceResolver instanceof AbstractFederatedServiceResolver)) {
				throw new IllegalArgumentException(
						"SERVICE EvaluationMode requires a FederatedServiceResolver that is an instance of "
								+ AbstractFederatedServiceResolver.class.getName());
			}
			this.tupleFunctionServiceResolver = (AbstractFederatedServiceResolver) serviceResolver;
		} else {
			this.tupleFunctionServiceResolver = null;
		}

		con.addConnectionListener(new SubclassListener());
		con.addConnectionListener(new RulePropertyListener());
		con.addConnectionListener(new InvalidationListener());
	}

	public void setParserConfig(ParserConfig parserConfig) {
		queryPreparer.setParserConfig(parserConfig);
	}

	public ParserConfig getParserConfig() {
		return queryPreparer.getParserConfig();
	}

	@Override
	public CloseableIteration<? extends BindingSet, QueryEvaluationException> evaluate(TupleExpr tupleExpr,
			Dataset dataset, BindingSet bindings, boolean includeInferred)
			throws SailException {
		final CloseableIteration<? extends BindingSet, QueryEvaluationException> iter;
		QueryContext qctx = new QueryContext(queryPreparer);
		qctx.begin();
		try {
			initQueryContext(qctx);
			iter = evaluateInternal(tupleExpr, dataset, bindings, includeInferred);
		} finally {
			try {
				destroyQueryContext(qctx);
			} finally {
				qctx.end();
			}
		}

		// NB: Iteration methods may do on-demand evaluation hence need to wrap
		// these too
		return new QueryContextIteration(iter, qctx);
	}

	private void initQueryContext(QueryContext qctx) {
		for (QueryContextInitializer initializer : queryContextInitializers) {
			initializer.init(qctx);
		}
	}

	private void destroyQueryContext(QueryContext qctx) {
		for (QueryContextInitializer initializer : queryContextInitializers) {
			initializer.destroy(qctx);
		}
	}

	private CloseableIteration<? extends BindingSet, QueryEvaluationException> evaluateInternal(
			TupleExpr tupleExpr, Dataset dataset, BindingSet bindings, boolean includeInferred)
			throws SailException {
		logger.trace("Incoming query model:\n{}", tupleExpr);

		// Clone the tuple expression to allow for more aggresive optimizations
		tupleExpr = tupleExpr.clone();

		if (!(tupleExpr instanceof QueryRoot)) {
			// Add a dummy root node to the tuple expressions to allow the
			// optimizers to modify the actual root node
			tupleExpr = new QueryRoot(tupleExpr);
		}

		new SpinFunctionInterpreter(parser, tripleSource, functionRegistry).optimize(tupleExpr, dataset,
				bindings);
		new SpinMagicPropertyInterpreter(parser, tripleSource, tupleFunctionRegistry,
				tupleFunctionServiceResolver).optimize(tupleExpr, dataset, bindings);

		logger.trace("SPIN query model:\n{}", tupleExpr);

		if (evaluationMode == TupleFunctionEvaluationMode.TRIPLE_SOURCE) {
			EvaluationStrategy strategy = new TupleFunctionEvaluationStrategy(
					new SailTripleSource(this, includeInferred, vf), dataset, serviceResolver,
					tupleFunctionRegistry);

			// do standard optimizations
			new BindingAssigner().optimize(tupleExpr, dataset, bindings);
			new ConstantOptimizer(strategy).optimize(tupleExpr, dataset, bindings);
			new CompareOptimizer().optimize(tupleExpr, dataset, bindings);
			new ConjunctiveConstraintSplitter().optimize(tupleExpr, dataset, bindings);
			new DisjunctiveConstraintOptimizer().optimize(tupleExpr, dataset, bindings);
			new SameTermFilterOptimizer().optimize(tupleExpr, dataset, bindings);
			new QueryModelNormalizer().optimize(tupleExpr, dataset, bindings);
			new QueryJoinOptimizer(new TupleFunctionEvaluationStatistics()).optimize(tupleExpr, dataset,
					bindings);
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

	@Override
	public void close()
			throws SailException {
		super.close();
	}

	private void initRuleProperties()
			throws RDF4JException {
		if (rulePropertyMap != null) {
			return;
		}

		rulePropertyMap = parser.parseRuleProperties(tripleSource);
		// order rules
		Set<IRI> remainingRules = new HashSet<>(rulePropertyMap.keySet());
		List<IRI> reverseOrder = new ArrayList<>(remainingRules.size());
		while (!remainingRules.isEmpty()) {
			for (Iterator<IRI> ruleIter = remainingRules.iterator(); ruleIter.hasNext();) {
				IRI rule = ruleIter.next();
				boolean isTerminal = true;
				RuleProperty ruleProperty = rulePropertyMap.get(rule);
				if (ruleProperty != null) {
					List<IRI> nextRules = ruleProperty.getNextRules();
					for (IRI nextRule : nextRules) {
						if (!nextRule.equals(rule) && remainingRules.contains(nextRule)) {
							isTerminal = false;
							break;
						}
					}
				}
				if (isTerminal) {
					reverseOrder.add(rule);
					ruleIter.remove();
				}
			}
		}
		orderedRuleProperties = Lists.reverse(reverseOrder);
	}

	private void resetRuleProperties() {
		orderedRuleProperties = null;
		rulePropertyMap = null;
	}

	private List<IRI> getRuleProperties()
			throws RDF4JException {
		initRuleProperties();
		return orderedRuleProperties;
	}

	private RuleProperty getRuleProperty(IRI ruleProp)
			throws RDF4JException {
		initRuleProperties();
		return rulePropertyMap.get(ruleProp);
	}

	private void initClasses()
			throws RDF4JException {
		if (classToSuperclassMap != null) {
			return;
		}

		classToSuperclassMap = new HashMap<>();
		try (CloseableIteration<? extends Statement, QueryEvaluationException> stmtIter = tripleSource.getStatements(
				null, RDFS.SUBCLASSOF, null)) {
			while (stmtIter.hasNext()) {
				Statement stmt = stmtIter.next();
				if (stmt.getSubject() instanceof IRI && stmt.getObject() instanceof IRI) {
					IRI cls = (IRI) stmt.getSubject();
					IRI superclass = (IRI) stmt.getObject();
					Set<IRI> superclasses = getSuperclasses(cls);
					if (superclasses == null) {
						superclasses = new HashSet<>();
						classToSuperclassMap.put(cls, superclasses);
					}
					superclasses.add(superclass);
				}
			}
		}
	}

	private void resetClasses() {
		classToSuperclassMap = null;
	}

	private Set<IRI> getSuperclasses(Resource cls)
			throws RDF4JException {
		initClasses();
		return classToSuperclassMap.get(cls);
	}

	@Override
	protected Model createModel() {
		return new DynamicModelFactory().createEmptyModel();
	}

	private final static List<Statement> schemaSp;
	private final static List<Statement> schemaSpin;
	private final static List<Statement> schemaSplSpin;
	private final static List<Statement> schemaSpinFullFC;

	static {
		try {
			schemaSp = getStatementsAsList("/schema/sp.ttl", RDFFormat.TURTLE);
			schemaSpin = getStatementsAsList("/schema/spin.ttl", RDFFormat.TURTLE);
			schemaSplSpin = getStatementsAsList("/schema/spl.spin.ttl", RDFFormat.TURTLE);
			schemaSpinFullFC = getStatementsAsList("/schema/spin-full-forwardchained.ttl", RDFFormat.TURTLE);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	protected void addAxiomStatements()
			throws SailException {
		RDFInferencerInserter inserter = new RDFInferencerInserter(this, vf);
		if (axiomClosureNeeded) {
			schemaSpinFullFC.forEach(inserter::handleStatement);
		} else {
			schemaSp.forEach(inserter::handleStatement);
			schemaSpin.forEach(inserter::handleStatement);
			schemaSplSpin.forEach(inserter::handleStatement);
		}
	}

	private static List<Statement> getStatementsAsList(String resourceName, RDFFormat format) throws IOException {
		RDFParser parser = Rio.createParser(format);
		URL url = SpinSailConnection.class.getResource(resourceName);

		List<Statement> ret = new ArrayList<>();
		parser.setRDFHandler(new RDFHandler() {
			@Override
			public void startRDF() throws RDFHandlerException {

			}

			@Override
			public void endRDF() throws RDFHandlerException {

			}

			@Override
			public void handleNamespace(String s, String s1) throws RDFHandlerException {

			}

			@Override
			public void handleStatement(Statement statement) throws RDFHandlerException {
				ret.add(statement);
			}

			@Override
			public void handleComment(String s) throws RDFHandlerException {

			}
		});

		try (InputStream in = new BufferedInputStream(url.openStream())) {
			parser.parse(in, url.toString());
		}

		return ret;
	}

	@Override
	protected void doInferencing()
			throws SailException {
		if (sail.isInitializing() && sail.isAxiomClosureNeeded()) {
			return;
		}

		ruleExecutions = new HashMap<>();
		super.doInferencing();
		ruleExecutions = null;
	}

	@Override
	protected int applyRules(Model iteration)
			throws SailException {
		try {
			int nofInferred = 0;
			nofInferred += applyRulesInternal(iteration.subjects());
			nofInferred += applyRulesInternal(Iterables.filter(iteration.objects(), Resource.class));
			return nofInferred;
		} catch (SailException e) {
			throw e;
		} catch (RDF4JException e) {
			throw new SailException(e);
		}
	}

	/**
	 * update spin:rules modify existing (non-inferred) statements directly. spin:constructors should be run after
	 * spin:rules for each subject of an RDF.TYPE statement.
	 */
	private int applyRulesInternal(Iterable<Resource> resources)
			throws RDF4JException {
		int nofInferred = 0;
		for (Resource res : resources) {
			logger.debug("building class hierarchy for {}", res);
			// build local class hierarchy
			Collection<IRI> remainingClasses = getClasses(res);
			List<IRI> classHierarchy = new ArrayList<>(remainingClasses.size());
			while (!remainingClasses.isEmpty()) {
				boolean hasCycle = true;
				for (Iterator<IRI> clsIter = remainingClasses.iterator(); clsIter.hasNext();) {
					IRI cls = clsIter.next();
					Set<IRI> superclasses = getSuperclasses(cls);
					boolean isTerminal = true;
					if (superclasses != null) {
						for (IRI superclass : remainingClasses) {
							if (!superclass.equals(cls) && superclasses.contains(superclass)) {
								isTerminal = false;
								break;
							}
						}
					}
					if (isTerminal) {
						classHierarchy.add(cls);
						clsIter.remove();
						hasCycle = false;
					}
				}
				if (hasCycle) {
					logger.warn("Cycle detected in class hierarchy: " + remainingClasses);
					classHierarchy.addAll(remainingClasses);
					break;
				}
			}

			nofInferred += executeRules(res, classHierarchy);

			// flush changes to tripleSource before reading constructors
			flushUpdates();
			nofInferred += executeConstructors(res, classHierarchy);

			// flush changes to tripleSource before reading constraints
			flushUpdates();
			checkConstraints(res, classHierarchy);
		}
		return nofInferred;
	}

	private Collection<IRI> getClasses(Resource subj)
			throws QueryEvaluationException {
		return Iterations.asList(TripleSources.getObjectURIs(subj, RDF.TYPE, tripleSource));
	}

	private int executeConstructors(Resource subj, List<IRI> classHierarchy)
			throws RDF4JException {
		int nofInferred = 0;
		Set<Resource> constructed = Iterations.asSet(TripleSources.getObjectResources(
				subj, EXECUTED, tripleSource));

		for (IRI cls : classHierarchy) {
			List<Resource> constructors = getConstructorsForClass(cls);
			if (!constructors.isEmpty()) {
				logger.trace("executing constructors for resource {} of class {}", subj, cls);

				for (Resource constructor : constructors) {
					if (constructed.add(constructor)) {
						logger.trace("executing constructor {} for resource {}", constructor, subj);
						nofInferred += executeRule(subj, constructor);
						addInferredStatement(subj, EXECUTED, constructor);
					}
				}
			}
		}
		logger.trace("added {} new triples via constructors for resource {}", nofInferred, subj);
		return nofInferred;
	}

	private List<Resource> getConstructorsForClass(IRI cls)
			throws RDF4JException {

		return Iterations.asList(TripleSources.getObjectResources(cls, SPIN.CONSTRUCTOR_PROPERTY, tripleSource));
	}

	private int executeRules(Resource subj, List<IRI> classHierarchy)
			throws RDF4JException {
		int nofInferred = 0;
		// get rule properties
		List<IRI> ruleProps = getRuleProperties();

		// check each class of subj for rule properties
		for (IRI cls : classHierarchy) {
			Map<IRI, List<Resource>> classRulesByProperty = getRulesForClass(cls, ruleProps);
			if (!classRulesByProperty.isEmpty()) {
				logger.debug("executing rules for resource {} of class {}", subj, cls);
				// execute rules
				for (Map.Entry<IRI, List<Resource>> ruleEntry : classRulesByProperty.entrySet()) {
					RuleProperty ruleProperty = getRuleProperty(ruleEntry.getKey());
					int maxCount = ruleProperty.getMaxIterationCount();
					for (Resource rule : ruleEntry.getValue()) {
						Executions executions = null;
						if (maxCount != -1) {
							executions = ruleExecutions.get(rule);
							if (executions == null) {
								executions = new Executions();
								ruleExecutions.put(rule, executions);
							}
							if (executions.count >= maxCount) {
								continue;
							}
						}
						logger.trace("executing rule {} on resource {}", rule, subj);
						nofInferred += executeRule(subj, rule);
						if (executions != null) {
							executions.count++;
						}
					}
				}
			}
		}
		logger.debug("inferred {} new triples for resource {}", nofInferred, subj);
		return nofInferred;
	}

	private int executeRule(Resource subj, Resource rule)
			throws RDF4JException {
		return SpinInferencing.executeRule(subj, rule, queryPreparer, parser, this);
	}

	/**
	 * @return Map with rules in execution order.
	 */
	private Map<IRI, List<Resource>> getRulesForClass(IRI cls, List<IRI> ruleProps)
			throws QueryEvaluationException {
		// NB: preserve ruleProp order!
		Map<IRI, List<Resource>> classRulesByProperty = new HashMap<>(ruleProps.size() * 3);
		for (IRI ruleProp : ruleProps) {
			List<Resource> rules = Iterations.asList(TripleSources.getObjectResources(cls, ruleProp, tripleSource));

			if (!rules.isEmpty()) {
				if (rules.size() > 1) {
					// sort by comments
					final Map<Resource, String> comments = new HashMap<>(rules.size() * 3);
					for (Resource rule : rules) {
						String comment = getHighestComment(rule);
						if (comment != null) {
							comments.put(rule, comment);
						}
					}
					rules.sort((rule1, rule2) -> {
						String comment1 = comments.get(rule1);
						String comment2 = comments.get(rule2);
						if (comment1 != null && comment2 != null) {
							return comment1.compareTo(comment2);
						} else if (comment1 != null && comment2 == null) {
							return 1;
						} else if (comment1 == null && comment2 != null) {
							return -1;
						} else {
							return 0;
						}
					});
				}
				classRulesByProperty.put(ruleProp, rules);
			}
		}
		return classRulesByProperty;
	}

	private String getHighestComment(Resource subj)
			throws QueryEvaluationException {
		String comment = null;
		try (CloseableIteration<? extends Literal, QueryEvaluationException> iter = TripleSources.getObjectLiterals(
				subj, RDFS.COMMENT, tripleSource)) {
			while (iter.hasNext()) {
				Literal l = iter.next();
				String label = l.getLabel();
				if ((comment != null && label.compareTo(comment) > 0) || (comment == null)) {
					comment = label;
				}
			}
		}
		return comment;
	}

	private void checkConstraints(Resource subj, List<IRI> classHierarchy)
			throws RDF4JException {
		if (sail.isInitializing() || !sail.isValidateConstraints()) {
			return;
		}

		Map<IRI, List<Resource>> constraintsByClass = getConstraintsForSubject(subj, classHierarchy);

		if (!constraintsByClass.isEmpty()) {
			// check constraints
			logger.debug("checking constraints for resource {}", subj);
			for (Map.Entry<IRI, List<Resource>> clsEntry : constraintsByClass.entrySet()) {
				List<Resource> constraints = clsEntry.getValue();
				for (Resource constraint : constraints) {
					checkConstraint(subj, constraint);
				}
			}
		}
	}

	private void checkConstraint(Resource subj, Resource constraint)
			throws RDF4JException {
		logger.trace("checking constraint {} on resoure {}", constraint, subj);
		ConstraintViolation violation = SpinInferencing.checkConstraint(subj, constraint, queryPreparer,
				parser);
		if (violation != null) {
			handleConstraintViolation(violation);
		} else {
			logger.trace("no violation detected for resource {}", subj);
		}
	}

	protected void handleConstraintViolation(ConstraintViolation violation)
			throws ConstraintViolationException {
		switch (violation.getLevel()) {
		case INFO:
			logger.info(constraintViolationMarker, CONSTRAINT_VIOLATION_MESSAGE,
					getConstraintViolationLogMessageArgs(violation));
			break;
		case WARNING:
			logger.warn(constraintViolationMarker, CONSTRAINT_VIOLATION_MESSAGE,
					getConstraintViolationLogMessageArgs(violation));
			break;
		case ERROR:
			logger.error(constraintViolationMarker, CONSTRAINT_VIOLATION_MESSAGE,
					getConstraintViolationLogMessageArgs(violation));
			throw new ConstraintViolationException(violation);
		case FATAL:
			logger.error(constraintViolationMarker, CONSTRAINT_VIOLATION_MESSAGE,
					getConstraintViolationLogMessageArgs(violation));
			throw new ConstraintViolationException(violation);
		}
	}

	private Object[] getConstraintViolationLogMessageArgs(ConstraintViolation violation) {
		return new Object[] {
				violation.getMessage() != null ? violation.getMessage() : "No message",
				Strings.nullToEmpty(violation.getRoot()),
				Strings.nullToEmpty(violation.getPath()),
				Strings.nullToEmpty(violation.getValue()) };
	}

	private Map<IRI, List<Resource>> getConstraintsForSubject(Resource subj, List<IRI> classHierarchy)
			throws QueryEvaluationException {
		Map<IRI, List<Resource>> constraintsByClass = new HashMap<>(
				classHierarchy.size() * 3);
		// check each class of subj for constraints
		for (IRI cls : classHierarchy) {
			List<Resource> constraints = getConstraintsForClass(cls);
			if (!constraints.isEmpty()) {
				constraintsByClass.put(cls, constraints);
			}
		}
		return constraintsByClass;
	}

	private List<Resource> getConstraintsForClass(Resource cls)
			throws QueryEvaluationException {
		return Iterations.asList(TripleSources.getObjectResources(cls, SPIN.CONSTRAINT_PROPERTY, tripleSource));

	}

	private class SubclassListener implements SailConnectionListener {

		@Override
		public void statementAdded(Statement st) {
			if (st.getObject() instanceof Resource && RDFS.SUBCLASSOF.equals(st.getPredicate())) {
				resetClasses();
			}
		}

		@Override
		public void statementRemoved(Statement st) {
			if (st.getObject() instanceof Resource && RDFS.SUBCLASSOF.equals(st.getPredicate())) {
				resetClasses();
			}
		}
	}

	private class RulePropertyListener implements SailConnectionListener {

		@Override
		public void statementAdded(Statement st) {
			updateRuleProperties(st);
		}

		@Override
		public void statementRemoved(Statement st) {
			updateRuleProperties(st);
		}

		private void updateRuleProperties(Statement st) {
			boolean changed = false;
			IRI pred = st.getPredicate();
			if (RDFS.SUBPROPERTYOF.equals(pred) && SPIN.RULE_PROPERTY.equals(st.getObject())) {
				changed = true;
			} else if (SPIN.NEXT_RULE_PROPERTY_PROPERTY.equals(pred)) {
				changed = true;
			} else if (SPIN.RULE_PROPERTY_MAX_ITERATION_COUNT_PROPERTY.equals(pred)) {
				changed = true;
			}
			if (changed) {
				resetRuleProperties();
			}
		}
	}

	private class InvalidationListener implements SailConnectionListener {

		@Override
		public void statementAdded(Statement st) {
			invalidate(st.getSubject());
		}

		@Override
		public void statementRemoved(Statement st) {
			invalidate(st.getSubject());
		}

		private void invalidate(Resource subj) {
			if (subj instanceof IRI) {
				parser.reset((IRI) subj);
				String key = subj.stringValue();
				Function func = functionRegistry.get(key).orElse(null);
				if (func instanceof TransientFunction) {
					functionRegistry.remove(func);
				}
				TupleFunction tupleFunc = tupleFunctionRegistry.get(key).orElse(null);
				if (tupleFunc instanceof TransientTupleFunction) {
					tupleFunctionRegistry.remove(tupleFunc);
				}
			}
		}
	}

	private static final class Executions {

		int count;
	}
}
