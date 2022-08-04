/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.federated.optimizer;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.rdf4j.federated.EndpointManager;
import org.eclipse.rdf4j.federated.algebra.ExclusiveGroup;
import org.eclipse.rdf4j.federated.algebra.ExclusiveStatement;
import org.eclipse.rdf4j.federated.algebra.FedXService;
import org.eclipse.rdf4j.federated.algebra.NJoin;
import org.eclipse.rdf4j.federated.algebra.StatementSource;
import org.eclipse.rdf4j.federated.algebra.StatementSource.StatementSourceType;
import org.eclipse.rdf4j.federated.endpoint.Endpoint;
import org.eclipse.rdf4j.federated.exception.FedXRuntimeException;
import org.eclipse.rdf4j.federated.exception.OptimizationException;
import org.eclipse.rdf4j.federated.structures.QueryInfo;
import org.eclipse.rdf4j.query.algebra.Service;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.helpers.AbstractSimpleQueryModelVisitor;

/**
 * Optimizer for SERVICE nodes.
 *
 * @author Andreas Schwarte
 *
 */
public class ServiceOptimizer extends AbstractSimpleQueryModelVisitor<OptimizationException> implements FedXOptimizer {

	protected final QueryInfo queryInfo;

	/**
	 * @param queryInfo
	 */
	public ServiceOptimizer(QueryInfo queryInfo) {
		super(true);
		this.queryInfo = queryInfo;
	}

	@Override
	public void optimize(TupleExpr tupleExpr) {
		try {
			tupleExpr.visit(this);
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			if (e instanceof InterruptedException) {
				Thread.currentThread().interrupt();
			}
			throw new FedXRuntimeException(e);
		}

	}

	@Override
	public void meet(Service service) {
		// create an optimized service, which may wrap the original service
		// the latter is the case, if we cannot optimize the SERVICE node in FedX
		TupleExpr newExpr = optimizeService(service);
		service.replaceWith(newExpr);
	}

	protected TupleExpr optimizeService(Service service) {

		// check if there is a service uri given
		if (service.getServiceRef().hasValue()) {

			String serviceUri = service.getServiceRef().getValue().stringValue();

			GenericInfoOptimizer serviceInfo = new GenericInfoOptimizer(queryInfo);
			serviceInfo.optimize(service.getServiceExpr());

			Endpoint e = getFedXEndpoint(serviceUri);

			// endpoint is not in federation
			if (e == null) {
				// leave service as is, evaluate with Sesame code
				return new FedXService(service, queryInfo);
			}

			StatementSource source = new StatementSource(e.getId(), StatementSourceType.REMOTE);
			List<ExclusiveStatement> stmts = new ArrayList<>();
			// convert all statements to exclusive statements
			for (StatementPattern st : serviceInfo.getStatements()) {
				ExclusiveStatement est = new ExclusiveStatement(st, source, queryInfo);
				st.replaceWith(est);
				stmts.add(est);
			}

			// check if we have a simple subquery now (i.e. only a simple BGP)
			if (service.getArg() instanceof ExclusiveStatement) {
				return service.getArg();
			}
			if (service.getArg() instanceof NJoin) {
				NJoin j = (NJoin) service.getArg();
				boolean simple = true;
				for (TupleExpr t : j.getArgs()) {
					if (!(t instanceof ExclusiveStatement)) {
						simple = false;
						break;
					}
				}

				if (simple) {
					return new ExclusiveGroup(stmts, source, queryInfo);
				}
			}

		}

		return new FedXService(service, queryInfo);
	}

	/**
	 * Return the FedX endpoint corresponding to the given service URI. If there is no such endpoint in FedX, this
	 * method returns null.
	 *
	 * Note that this method compares the endpoint URL first, however, that the name of the endpoint can be used as
	 * identifier as well. Note that the name must be a valid URI, i.e. start with http://
	 *
	 * @param serviceUri
	 * @return
	 */
	private Endpoint getFedXEndpoint(String serviceUri) {
		EndpointManager em = queryInfo.getFederationContext().getEndpointManager();
		Endpoint e = em.getEndpointByUrl(serviceUri);
		if (e != null) {
			return e;
		}
		e = em.getEndpointByName(serviceUri);
		return e;
	}

}
