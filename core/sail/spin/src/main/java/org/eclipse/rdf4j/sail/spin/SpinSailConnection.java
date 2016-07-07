/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.spin;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.rdf4j.RDF4JException;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.TreeModel;
import org.eclipse.rdf4j.model.impl.ValueFactoryImpl;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.SPIN;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.QueryRoot;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategy;
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
import org.eclipse.rdf4j.query.algebra.evaluation.impl.EvaluationStatistics;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.EvaluationStrategyImpl;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.FilterOptimizer;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.IterativeEvaluationOptimizer;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.OrderLimitOptimizer;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.QueryJoinOptimizer;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.QueryModelNormalizer;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.SameTermFilterOptimizer;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.TupleFunctionEvaluationStrategy;
import org.eclipse.rdf4j.query.algebra.evaluation.util.TripleSources;
import org.eclipse.rdf4j.rio.ParserConfig;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.sail.SailConnectionListener;
import org.eclipse.rdf4j.sail.SailConnectionQueryPreparer;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.evaluation.SailTripleSource;
import org.eclipse.rdf4j.sail.inferencer.InferencerConnection;
import org.eclipse.rdf4j.sail.inferencer.fc.AbstractForwardChainingInferencerConnection;
import org.eclipse.rdf4j.sail.inferencer.util.RDFInferencerInserter;
import org.eclipse.rdf4j.sail.spin.SpinSail.EvaluationMode;
import org.eclipse.rdf4j.spin.ConstraintViolation;
import org.eclipse.rdf4j.spin.QueryContext;
import org.eclipse.rdf4j.spin.RuleProperty;
import org.eclipse.rdf4j.spin.SpinParser;
import org.eclipse.rdf4j.spin.function.TransientFunction;
import org.eclipse.rdf4j.spin.function.TransientTupleFunction;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

class SpinSailConnection extends AbstractForwardChainingInferencerConnection {

	private static final IRI EXECUTED = ValueFactoryImpl.getInstance().createIRI(
			"http://www.openrdf.org/schema/spin#executed");

	private static final Marker constraintViolationMarker = MarkerFactory.getMarker("ConstraintViolation");

	private static final String CONSTRAINT_VIOLATION_MESSAGE = "Constraint violation: {}: {} {} {}";

	private final EvaluationMode evaluationMode;

	private final boolean axiomClosureNeeded;

	private final FunctionRegistry functionRegistry;

	private final TupleFunctionRegistry tupleFunctionRegistry;

	private final AbstractFederatedServiceResolver serviceResolver;

	private final ValueFactory vf;

	private final TripleSource tripleSource;

	private final SpinParser parser;

	private List<IRI> orderedRuleProperties;

	private Map<IRI, RuleProperty> rulePropertyMap;

	private Map<Resource, Executions> ruleExecutions;

	private Map<IRI, Set<IRI>> classToSuperclassMap;

	private SailConnectionQueryPreparer queryPreparer;

	public SpinSailConnection(SpinSail sail, InferencerConnection con) {
		super(sail, con);
		this.evaluationMode = sail.getEvaluationMode();
		this.axiomClosureNeeded = sail.isAxiomClosureNeeded();
		this.functionRegistry = sail.getFunctionRegistry();
		this.tupleFunctionRegistry = sail.getTupleFunctionRegistry();
		this.vf = sail.getValueFactory();
		this.parser = sail.getSpinParser();
		this.tripleSource = new SailTripleSource(getWrappedConnection(), true, vf);
		this.queryPreparer = new SailConnectionQueryPreparer(this, true, tripleSource);

		if (evaluationMode == EvaluationMode.SERVICE) {
			FederatedServiceResolver resolver = sail.getFederatedServiceResolver();
			if (!(resolver instanceof AbstractFederatedServiceResolver)) {
				throw new IllegalArgumentException(
						"SERVICE EvaluationMode requires a FederatedServiceResolver that is an instance of "
								+ AbstractFederatedServiceResolver.class.getName());
			}
			this.serviceResolver = (AbstractFederatedServiceResolver)resolver;
		}
		else {
			this.serviceResolver = null;
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
		throws SailException
	{
		final CloseableIteration<? extends BindingSet, QueryEvaluationException> iter;
		QueryContext qctx = QueryContext.begin(queryPreparer);
		try {
			iter = evaluateInternal(tupleExpr, dataset, bindings, includeInferred);
		}
		finally {
			qctx.end();
		}

		// NB: Iteration methods may do on-demand evaluation hence need to wrap
		// these too
		return new QueryContextIteration(iter, queryPreparer);
	}

	private CloseableIteration<? extends BindingSet, QueryEvaluationException> evaluateInternal(
			TupleExpr tupleExpr, Dataset dataset, BindingSet bindings, boolean includeInferred)
		throws SailException
	{
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
				serviceResolver).optimize(tupleExpr, dataset, bindings);

		logger.trace("SPIN query model:\n{}", tupleExpr);

		if (evaluationMode == EvaluationMode.TRIPLE_SOURCE) {
			EvaluationStrategy strategy = new TupleFunctionEvaluationStrategy(
					new EvaluationStrategyImpl(new SailTripleSource(this, includeInferred, vf), dataset,
							serviceResolver),
					vf, tupleFunctionRegistry);

			// do standard optimizations
			new BindingAssigner().optimize(tupleExpr, dataset, bindings);
			new ConstantOptimizer(strategy).optimize(tupleExpr, dataset, bindings);
			new CompareOptimizer().optimize(tupleExpr, dataset, bindings);
			new ConjunctiveConstraintSplitter().optimize(tupleExpr, dataset, bindings);
			new DisjunctiveConstraintOptimizer().optimize(tupleExpr, dataset, bindings);
			new SameTermFilterOptimizer().optimize(tupleExpr, dataset, bindings);
			new QueryModelNormalizer().optimize(tupleExpr, dataset, bindings);
			new QueryJoinOptimizer(new EvaluationStatistics()).optimize(tupleExpr, dataset, bindings);
			// new SubSelectJoinOptimizer().optimize(tupleExpr, dataset,
			// bindings);
			new IterativeEvaluationOptimizer().optimize(tupleExpr, dataset, bindings);
			new FilterOptimizer().optimize(tupleExpr, dataset, bindings);
			new OrderLimitOptimizer().optimize(tupleExpr, dataset, bindings);

			logger.trace("Optimized query model:\n{}", tupleExpr);

			try {
				return strategy.evaluate(tupleExpr, bindings);
			}
			catch (QueryEvaluationException e) {
				throw new SailException(e);
			}
		}
		else {
			return super.evaluate(tupleExpr, dataset, bindings, includeInferred);
		}
	}

	@Override
	public void close()
		throws SailException
	{
		super.close();
	}

	private void initRuleProperties()
		throws RDF4JException
	{
		if (rulePropertyMap != null) {
			return;
		}

		rulePropertyMap = parser.parseRuleProperties(tripleSource);
		// order rules
		Set<IRI> remainingRules = new HashSet<IRI>(rulePropertyMap.keySet());
		List<IRI> reverseOrder = new ArrayList<IRI>(remainingRules.size());
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
		throws RDF4JException
	{
		initRuleProperties();
		return orderedRuleProperties;
	}

	private RuleProperty getRuleProperty(IRI ruleProp)
		throws RDF4JException
	{
		initRuleProperties();
		return rulePropertyMap.get(ruleProp);
	}

	private void initClasses()
		throws RDF4JException
	{
		if (classToSuperclassMap != null) {
			return;
		}

		classToSuperclassMap = new HashMap<IRI, Set<IRI>>();
		CloseableIteration<? extends Statement, QueryEvaluationException> stmtIter = tripleSource.getStatements(
				null, RDFS.SUBCLASSOF, null);
		try {
			while (stmtIter.hasNext()) {
				Statement stmt = stmtIter.next();
				if (stmt.getSubject() instanceof IRI && stmt.getObject() instanceof IRI) {
					IRI cls = (IRI)stmt.getSubject();
					IRI superclass = (IRI)stmt.getObject();
					Set<IRI> superclasses = getSuperclasses(cls);
					if (superclasses == null) {
						superclasses = new HashSet<IRI>(64);
						classToSuperclassMap.put(cls, superclasses);
					}
					superclasses.add(superclass);
				}
			}
		}
		finally {
			stmtIter.close();
		}
	}

	private void resetClasses() {
		classToSuperclassMap = null;
	}

	private Set<IRI> getSuperclasses(Resource cls)
		throws RDF4JException
	{
		initClasses();
		return classToSuperclassMap.get(cls);
	}

	@Override
	protected Model createModel() {
		return new TreeModel();
	}

	@Override
	protected void addAxiomStatements()
		throws SailException
	{
		RDFParser parser = Rio.createParser(RDFFormat.TURTLE);
		if (axiomClosureNeeded) {
			loadAxiomStatements(parser, "/schema/spin-full.ttl");
		}
		else {
			loadAxiomStatements(parser, "/schema/sp.ttl");
			loadAxiomStatements(parser, "/schema/spin.ttl");
			loadAxiomStatements(parser, "/schema/spl.spin.ttl");
		}
	}

	private void loadAxiomStatements(RDFParser parser, String file)
		throws SailException
	{
		RDFInferencerInserter inserter = new RDFInferencerInserter(this, vf);
		parser.setRDFHandler(inserter);
		URL url = getClass().getResource(file);
		try {
			InputStream in = new BufferedInputStream(url.openStream());
			try {
				parser.parse(in, url.toString());
			}
			finally {
				in.close();
			}
		}
		catch (IOException ioe) {
			throw new SailException(ioe);
		}
		catch (RDF4JException e) {
			throw new SailException(e);
		}
	}

	@Override
	protected void doInferencing()
		throws SailException
	{
		ruleExecutions = new HashMap<Resource, Executions>();
		super.doInferencing();
		ruleExecutions = null;
	}

	@Override
	protected int applyRules(Model iteration)
		throws SailException
	{
		try {
			int nofInferred = 0;
			nofInferred += applyRulesInternal(iteration.subjects());
			nofInferred += applyRulesInternal(Iterables.filter(iteration.objects(), Resource.class));
			return nofInferred;
		}
		catch (SailException e) {
			throw e;
		}
		catch (RDF4JException e) {
			throw new SailException(e);
		}
	}

	/**
	 * update spin:rules modify existing (non-inferred) statements directly. spin:constructors should be run
	 * after spin:rules for each subject of an RDF.TYPE statement.
	 */
	private int applyRulesInternal(Iterable<? extends Resource> resources)
		throws RDF4JException
	{
		int nofInferred = 0;
		for (Resource res : resources) {
			// build local class hierarchy
			Collection<IRI> remainingClasses = getClasses(res);
			List<IRI> classHierarchy = new ArrayList<IRI>(remainingClasses.size());
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
		throws QueryEvaluationException
	{
		List<IRI> classes = new LinkedList<IRI>();
		CloseableIteration<? extends IRI, QueryEvaluationException> classIter = TripleSources.getObjectURIs(subj,
				RDF.TYPE, tripleSource);
		Iterations.addAll(classIter, classes);
		return classes;
	}

	private int executeConstructors(Resource subj, List<IRI> classHierarchy)
		throws RDF4JException
	{
		int nofInferred = 0;
		Set<Resource> constructed = new HashSet<Resource>(classHierarchy.size());
		CloseableIteration<? extends Resource, QueryEvaluationException> classIter = TripleSources.getObjectResources(
				subj, EXECUTED, tripleSource);
		Iterations.addAll(classIter, constructed);

		for (IRI cls : classHierarchy) {
			List<Resource> constructors = getConstructorsForClass(cls);
			for (Resource constructor : constructors) {
				if (constructed.add(constructor)) {
					nofInferred += executeRule(subj, constructor);
					addInferredStatement(subj, EXECUTED, constructor);
				}
			}
		}
		return nofInferred;
	}

	private List<Resource> getConstructorsForClass(IRI cls)
		throws RDF4JException
	{
		List<Resource> constructors = new ArrayList<Resource>(2);
		CloseableIteration<? extends Resource, QueryEvaluationException> constructorIter = TripleSources.getObjectResources(
				cls, SPIN.CONSTRUCTOR_PROPERTY, tripleSource);
		Iterations.addAll(constructorIter, constructors);
		return constructors;
	}

	private int executeRules(Resource subj, List<IRI> classHierarchy)
		throws RDF4JException
	{
		int nofInferred = 0;
		// get rule properties
		List<IRI> ruleProps = getRuleProperties();

		// check each class of subj for rule properties
		for (IRI cls : classHierarchy) {
			Map<IRI, List<Resource>> classRulesByProperty = getRulesForClass(cls, ruleProps);
			if (!classRulesByProperty.isEmpty()) {
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
						nofInferred += executeRule(subj, rule);
						if (executions != null) {
							executions.count++;
						}
					}
				}
			}
		}
		return nofInferred;
	}

	private int executeRule(Resource subj, Resource rule)
		throws RDF4JException
	{
		return SpinInferencing.executeRule(subj, rule, queryPreparer, parser, this);
	}

	/**
	 * @return Map with rules in execution order.
	 */
	private Map<IRI, List<Resource>> getRulesForClass(IRI cls, List<IRI> ruleProps)
		throws QueryEvaluationException
	{
		// NB: preserve ruleProp order!
		Map<IRI, List<Resource>> classRulesByProperty = new LinkedHashMap<IRI, List<Resource>>(
				ruleProps.size());
		for (IRI ruleProp : ruleProps) {
			List<Resource> rules = new ArrayList<Resource>(2);
			CloseableIteration<? extends Resource, QueryEvaluationException> ruleIter = TripleSources.getObjectResources(
					cls, ruleProp, tripleSource);
			Iterations.addAll(ruleIter, rules);
			if (!rules.isEmpty()) {
				if (rules.size() > 1) {
					// sort by comments
					final Map<Resource, String> comments = new HashMap<Resource, String>(rules.size());
					for (Resource rule : rules) {
						String comment = getHighestComment(rule);
						if (comment != null) {
							comments.put(rule, comment);
						}
					}
					Collections.sort(rules, new Comparator<Resource>() {

						@Override
						public int compare(Resource rule1, Resource rule2) {
							String comment1 = comments.get(rule1);
							String comment2 = comments.get(rule2);
							if (comment1 != null && comment2 != null) {
								return comment1.compareTo(comment2);
							}
							else if (comment1 != null && comment2 == null) {
								return 1;
							}
							else if (comment1 == null && comment2 != null) {
								return -1;
							}
							else {
								return 0;
							}
						}
					});
				}
				classRulesByProperty.put(ruleProp, rules);
			}
		}
		return classRulesByProperty;
	}

	private String getHighestComment(Resource subj)
		throws QueryEvaluationException
	{
		String comment = null;
		CloseableIteration<? extends Literal, QueryEvaluationException> iter = TripleSources.getObjectLiterals(
				subj, RDFS.COMMENT, tripleSource);
		try {
			while (iter.hasNext()) {
				Literal l = iter.next();
				String label = l.getLabel();
				if ((comment != null && label.compareTo(comment) > 0) || (comment == null)) {
					comment = label;
				}
			}
		}
		finally {
			iter.close();
		}
		return comment;
	}

	private void checkConstraints(Resource subj, List<IRI> classHierarchy)
		throws RDF4JException
	{
		Map<IRI, List<Resource>> constraintsByClass = getConstraintsForSubject(subj, classHierarchy);

		// check constraints
		for (Map.Entry<IRI, List<Resource>> clsEntry : constraintsByClass.entrySet()) {
			List<Resource> constraints = clsEntry.getValue();
			for (Resource constraint : constraints) {
				checkConstraint(subj, constraint);
			}
		}
	}

	private void checkConstraint(Resource subj, Resource constraint)
		throws RDF4JException
	{
		ConstraintViolation violation = SpinInferencing.checkConstraint(subj, constraint, queryPreparer,
				parser);
		if (violation != null) {
			handleConstraintViolation(violation);
		}
	}

	private void handleConstraintViolation(ConstraintViolation violation)
		throws ConstraintViolationException
	{
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
		throws QueryEvaluationException
	{
		Map<IRI, List<Resource>> constraintsByClass = new LinkedHashMap<IRI, List<Resource>>(
				classHierarchy.size());
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
		throws QueryEvaluationException
	{
		List<Resource> constraints = new ArrayList<Resource>(2);
		CloseableIteration<? extends Resource, QueryEvaluationException> constraintIter = TripleSources.getObjectResources(
				cls, SPIN.CONSTRAINT_PROPERTY, tripleSource);
		Iterations.addAll(constraintIter, constraints);
		return constraints;
	}

	private class SubclassListener implements SailConnectionListener {

		@Override
		public void statementAdded(Statement st) {
			if (RDFS.SUBCLASSOF.equals(st.getPredicate()) && st.getObject() instanceof Resource) {
				resetClasses();
			}
		}

		@Override
		public void statementRemoved(Statement st) {
			if (RDFS.SUBCLASSOF.equals(st.getPredicate())) {
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
			}
			else if (SPIN.NEXT_RULE_PROPERTY_PROPERTY.equals(pred)) {
				changed = true;
			}
			else if (SPIN.RULE_PROPERTY_MAX_ITERATION_COUNT_PROPERTY.equals(pred)) {
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
				parser.reset((IRI)subj);
				String key = subj.stringValue();
				Function func = functionRegistry.get(key);
				if (func instanceof TransientFunction) {
					functionRegistry.remove(func);
				}
				TupleFunction tupleFunc = tupleFunctionRegistry.get(key);
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
