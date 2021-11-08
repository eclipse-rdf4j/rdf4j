/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.iterator;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.LookAheadIteration;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.StatementPattern.Scope;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategy;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryBindingSet;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryEvaluationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.QueryEvaluationContext;

public class ZeroLengthPathIteration extends LookAheadIteration<BindingSet, QueryEvaluationException> {

	private static final String ANON_SUBJECT_VAR = "zero-length-internal-start";

	private static final String ANON_PREDICATE_VAR = "zero-length-internal-pred";

	private static final String ANON_OBJECT_VAR = "zero-length-internal-end";

	private static final String ANON_SEQUENCE_VAR = "zero-length-internal-seq";

	private QueryBindingSet result;

	private Var subjectVar;

	private Var objVar;

	private Value subj;

	private Value obj;

	private BindingSet bindings;

	private CloseableIteration<BindingSet, QueryEvaluationException> iter;

	private Set<Value> reportedValues;

	private Var contextVar;

	private final EvaluationStrategy evaluationStrategy;

	private final QueryEvaluationStep precompile;

	public ZeroLengthPathIteration(EvaluationStrategy evaluationStrategyImpl, Var subjectVar, Var objVar, Value subj,
			Value obj, Var contextVar, BindingSet bindings, QueryEvaluationContext context) {
		this.evaluationStrategy = evaluationStrategyImpl;
		this.result = new QueryBindingSet(bindings);
		this.subjectVar = subjectVar;
		this.objVar = objVar;
		this.contextVar = contextVar;
		this.subj = subj;
		this.obj = obj;
		this.bindings = bindings;
		Var startVar = createAnonVar(ANON_SUBJECT_VAR);
		Var predicate = createAnonVar(ANON_PREDICATE_VAR);
		Var endVar = createAnonVar(ANON_OBJECT_VAR);
		StatementPattern subjects = new StatementPattern(startVar, predicate, endVar);
		if (contextVar != null) {
			subjects.setScope(Scope.NAMED_CONTEXTS);
			subjects.setContextVar(contextVar);
		}
		precompile = evaluationStrategy.precompile(subjects, context);

	}

	@Override
	protected BindingSet getNextElement() throws QueryEvaluationException {
		if (subj == null && obj == null) {
			if (this.reportedValues == null) {
				reportedValues = evaluationStrategy.makeSet();
			}
			if (this.iter == null) {
				// join with a sequence so we iterate over every entry twice
				QueryBindingSet bs1 = new QueryBindingSet(1);
				bs1.addBinding(ANON_SEQUENCE_VAR, SimpleValueFactory.getInstance().createLiteral("subject"));
				QueryBindingSet bs2 = new QueryBindingSet(1);
				bs2.addBinding(ANON_SEQUENCE_VAR, SimpleValueFactory.getInstance().createLiteral("object"));
				List<BindingSet> seqList = Arrays.<BindingSet>asList(bs1, bs2);
				iter = new CrossProductIteration(createIteration(), seqList);
			}

			while (iter.hasNext()) {
				BindingSet bs = iter.next();

				boolean isSubjOrObj = bs.getValue(ANON_SEQUENCE_VAR).stringValue().equals("subject");
				String endpointVarName = isSubjOrObj ? ANON_SUBJECT_VAR : ANON_OBJECT_VAR;
				Value v = bs.getValue(endpointVarName);

				if (reportedValues.add(v)) {
					QueryBindingSet next = new QueryBindingSet(bindings);
					next.addBinding(subjectVar.getName(), v);
					next.addBinding(objVar.getName(), v);
					if (contextVar != null) {
						Value context = bs.getValue(contextVar.getName());
						if (context != null) {
							next.addBinding(contextVar.getName(), context);
						}
					}
					return next;
				}
			}
			iter.close();

			// if we're done, throw away the cached list of values to avoid hogging
			// resources
			reportedValues = null;
			return null;
		} else {
			if (result != null) {
				if (obj == null && subj != null) {
					result.addBinding(objVar.getName(), subj);
				} else if (subj == null && obj != null) {
					result.addBinding(subjectVar.getName(), obj);
				} else if (subj != null && subj.equals(obj)) {
					// empty bindings
					// (result but nothing to bind as subjectVar and objVar are both fixed)
				} else {
					result = null;
				}
			}

			QueryBindingSet next = result;
			result = null;
			return next;
		}
	}

	private CloseableIteration<BindingSet, QueryEvaluationException> createIteration() throws QueryEvaluationException {
		CloseableIteration<BindingSet, QueryEvaluationException> iter = precompile.evaluate(bindings);
		return iter;
	}

	public Var createAnonVar(String varName) {
		Var var = new Var(varName);
		var.setAnonymous(true);
		return var;
	}
}
