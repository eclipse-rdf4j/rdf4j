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
import java.util.function.BiConsumer;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.LookAheadIteration;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.MutableBindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.StatementPattern.Scope;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategy;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryBindingSet;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryEvaluationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.QueryEvaluationContext;

public class ZeroLengthPathIteration extends LookAheadIteration<BindingSet, QueryEvaluationException> {

	private static final Literal OBJECT = SimpleValueFactory.getInstance().createLiteral("object");

	private static final Literal SUBJECT = SimpleValueFactory.getInstance().createLiteral("subject");

	public static final String ANON_SUBJECT_VAR = "zero-length-internal-start";

	public static final String ANON_PREDICATE_VAR = "zero-length-internal-pred";

	public static final String ANON_OBJECT_VAR = "zero-length-internal-end";

	public static final String ANON_SEQUENCE_VAR = "zero-length-internal-seq";

	private QueryBindingSet result;

	private final Value subj;

	private final Value obj;

	private final BindingSet bindings;

	private CloseableIteration<BindingSet, QueryEvaluationException> iter;

	private Set<Value> reportedValues;

	private final Var contextVar;

	private final EvaluationStrategy evaluationStrategy;

	private final QueryEvaluationStep precompile;

	private final QueryEvaluationContext context;

	private final BiConsumer<Value, MutableBindingSet> setSubject;

	private final BiConsumer<Value, MutableBindingSet> setObject;

	private final BiConsumer<Value, MutableBindingSet> setContext;

	public ZeroLengthPathIteration(EvaluationStrategy evaluationStrategyImpl, Var subjectVar, Var objVar, Value subj,
			Value obj, Var contextVar, BindingSet bindings, QueryEvaluationContext context) {
		this.evaluationStrategy = evaluationStrategyImpl;
		this.context = context;
		this.result = new QueryBindingSet(bindings);
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
		setSubject = context.addBinding(subjectVar.getName());
		setObject = context.addBinding(objVar.getName());
		if (contextVar != null) {
			setContext = context.addBinding(contextVar.getName());
		} else {
			setContext = null;
		}

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
				bs1.addBinding(ANON_SEQUENCE_VAR, SUBJECT);
				QueryBindingSet bs2 = new QueryBindingSet(1);
				bs2.addBinding(ANON_SEQUENCE_VAR, OBJECT);
				List<BindingSet> seqList = Arrays.<BindingSet>asList(bs1, bs2);
				iter = new CrossProductIteration(createIteration(), seqList);
			}

			while (iter.hasNext()) {
				BindingSet bs = iter.next();

				boolean isSubjOrObj = bs.getValue(ANON_SEQUENCE_VAR).stringValue().equals("subject");
				String endpointVarName = isSubjOrObj ? ANON_SUBJECT_VAR : ANON_OBJECT_VAR;
				Value v = bs.getValue(endpointVarName);

				if (reportedValues.add(v)) {
					MutableBindingSet next = context.createBindingSet(bindings);
					setSubject.accept(v, next);
					setObject.accept(v, next);
					if (setContext != null) {
						Value context = bs.getValue(contextVar.getName());
						if (context != null) {
							setContext.accept(context, next);
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
					setObject.accept(subj, result);
				} else if (subj == null && obj != null) {
					setSubject.accept(obj, result);
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
