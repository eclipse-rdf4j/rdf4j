/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.impl.evaluationsteps;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.SingletonIteration;
import org.eclipse.rdf4j.query.Binding;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.Service;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryEvaluationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.federation.FederatedService;
import org.eclipse.rdf4j.query.algebra.evaluation.federation.FederatedServiceResolver;
import org.eclipse.rdf4j.query.algebra.helpers.AbstractSimpleQueryModelVisitor;
import org.eclipse.rdf4j.query.impl.MapBindingSet;

public final class ServiceQueryEvaluationStep implements QueryEvaluationStep {
	private final Service service;
	private final Var serviceRef;
	private final FederatedServiceResolver serviceResolver;

	public ServiceQueryEvaluationStep(Service service, Var serviceRef, FederatedServiceResolver serviceResolver) {
		this.service = service;
		this.serviceRef = serviceRef;
		this.serviceResolver = serviceResolver;
	}

	@Override
	public CloseableIteration<BindingSet, QueryEvaluationException> evaluate(BindingSet bindings) {
		String serviceUri;
		if (serviceRef.hasValue()) {
			serviceUri = serviceRef.getValue().stringValue();
		} else {
			if (bindings != null && bindings.getValue(serviceRef.getName()) != null) {
				serviceUri = bindings.getBinding(serviceRef.getName()).getValue().stringValue();
			} else {
				throw new QueryEvaluationException("SERVICE variables must be bound at evaluation time.");
			}
		}

		try {
			FederatedService fs = serviceResolver.getService(serviceUri);

			// create a copy of the free variables, and remove those for which
			// bindings are available (we can set them as constraints!)
			Set<String> freeVars = new HashSet<>(service.getServiceVars());
			freeVars.removeAll(bindings.getBindingNames());

			// Get bindings from values pre-bound into variables.
			MapBindingSet allBindings = new MapBindingSet();
			for (Binding binding : bindings) {
				allBindings.setBinding(binding.getName(), binding.getValue());
			}

			Set<Var> boundVars = getBoundVariables(service);
			for (Var boundVar : boundVars) {
				freeVars.remove(boundVar.getName());
				allBindings.setBinding(boundVar.getName(), boundVar.getValue());
			}
			bindings = allBindings;

			String baseUri = service.getBaseURI();

			// special case: no free variables => perform ASK query
			if (freeVars.isEmpty()) {
				boolean exists = fs.ask(service, bindings, baseUri);

				// check if triples are available (with inserted bindings)
				if (exists) {
					return new SingletonIteration<>(bindings);
				} else {
					return EMPTY_ITERATION;
				}

			}

			// otherwise: perform a SELECT query
			return fs.select(service, freeVars, bindings,
					baseUri);

		} catch (RuntimeException e) {
			// suppress exceptions if silent
			if (service.isSilent()) {
				return new SingletonIteration<>(bindings);
			} else {
				throw e;
			}
		}
	}

	private Set<Var> getBoundVariables(Service service) {
		BoundVarVisitor visitor = new BoundVarVisitor();
		visitor.meet(service);
		return visitor.boundVars;
	}

	private static class BoundVarVisitor extends AbstractSimpleQueryModelVisitor<RuntimeException> {

		final Set<Var> boundVars = new HashSet<>();

		private BoundVarVisitor() {
			super(true);
		}

		@Override
		public void meet(Var var) {
			if (var.hasValue()) {
				boundVars.add(var);
			}
		}
	}

}
