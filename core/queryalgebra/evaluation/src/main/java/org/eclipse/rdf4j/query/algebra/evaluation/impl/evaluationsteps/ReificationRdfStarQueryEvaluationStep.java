/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.impl.evaluationsteps;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.ConvertingIteration;
import org.eclipse.rdf4j.common.iteration.LookAheadIteration;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.MutableBindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryEvaluationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.TripleSource;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.QueryEvaluationContext;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.StrictEvaluationStrategy;

public class ReificationRdfStarQueryEvaluationStep implements QueryEvaluationStep {
	private final Var objVar;
	private final Var extVar;
	private final Var subjVar;
	private final Var predVar;
	private final TripleSource tripleSource;
	private final QueryEvaluationContext context;

	public ReificationRdfStarQueryEvaluationStep(Var subjVar, Var predVar, Var objVar, Var extVar,
			TripleSource tripleSource, QueryEvaluationContext context) {
		this.objVar = objVar;
		this.extVar = extVar;
		this.subjVar = subjVar;
		this.predVar = predVar;
		this.tripleSource = tripleSource;
		this.context = context;
	}

	@Override
	public CloseableIteration<BindingSet, QueryEvaluationException> evaluate(BindingSet bindings) {
		final Value subjValue = StrictEvaluationStrategy.getVarValue(subjVar, bindings);
		final Value predValue = StrictEvaluationStrategy.getVarValue(predVar, bindings);
		final Value objValue = StrictEvaluationStrategy.getVarValue(objVar, bindings);
		final Value extValue = StrictEvaluationStrategy.getVarValue(extVar, bindings);
		// standard reification iteration
		// 1. walk over resources used as subjects of (x rdf:type rdf:Statement)
		final CloseableIteration<? extends Resource, QueryEvaluationException> iter = new ConvertingIteration<Statement, Resource, QueryEvaluationException>(
				tripleSource.getStatements((Resource) extValue, RDF.TYPE, RDF.STATEMENT)) {

			@Override
			protected Resource convert(Statement sourceObject)
					throws QueryEvaluationException {
				return sourceObject.getSubject();
			}
		};
		// for each reification node, fetch and check the subject, predicate and object values against
		// the expected values from TripleRef pattern and supplied bindings collection
		return new LookAheadIteration<>() {
			@Override
			protected void handleClose()
					throws QueryEvaluationException {
				super.handleClose();
				iter.close();
			}

			@Override
			protected BindingSet getNextElement()
					throws QueryEvaluationException {
				while (iter.hasNext()) {
					Resource theNode = iter.next();
					MutableBindingSet result = context.createBindingSet(bindings);
					// does it match the subjectValue/subjVar
					if (!matchValue(theNode, subjValue, subjVar, result, RDF.SUBJECT)) {
						continue;
					}
					// the predicate, if not, remove the binding that hass been added
					// when the subjValue has been checked and its value added to the solution
					if (!matchValue(theNode, predValue, predVar, result, RDF.PREDICATE)) {
						continue;
					}
					// check the object, if it do not match
					// remove the bindings added for subj and pred
					if (!matchValue(theNode, objValue, objVar, result, RDF.OBJECT)) {
						continue;
					}
					// add the extVar binding if we do not have a value bound.
					if (extValue == null) {
						result.addBinding(extVar.getName(), theNode);
					} else if (!extValue.equals(theNode)) {
						// the extVar value do not match theNode
						continue;
					}
					return result;
				}
				return null;
			}

			//
			private boolean matchValue(Resource theNode, Value value, Var var, MutableBindingSet result,
					IRI predicate) {
				try (CloseableIteration<? extends Statement, QueryEvaluationException> valueIter = tripleSource
						.getStatements(theNode, predicate, null)) {

					while (valueIter.hasNext()) {
						Statement valueStatement = valueIter.next();
						if (theNode.equals(valueStatement.getSubject())) {
							if (value == null || value.equals(valueStatement.getObject())) {
								if (value == null) {
									result.addBinding(var.getName(), valueStatement.getObject());
								}
								return true;
							}
						}
					}
					return false;
				}
			}
//
		};
	} // else standard reification iteration
}
