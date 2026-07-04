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
package org.eclipse.rdf4j.federated.evaluation;

import org.eclipse.rdf4j.federated.FederationContext;
import org.eclipse.rdf4j.federated.endpoint.Endpoint;
import org.eclipse.rdf4j.federated.endpoint.EndpointType;

public class TripleSourceFactory {

	public static TripleSource tripleSourceFor(Endpoint e, EndpointType t, FederationContext federationContext) {
		switch (t) {
		case NativeStore:
			return new SailTripleSource(e, federationContext);
		case SparqlEndpoint:
			return new SparqlTripleSource(e, federationContext);
		case RemoteRepository:
			return new SparqlTripleSource(e, federationContext);
		case Other:
			return new SparqlTripleSource(e, federationContext);
		default:
			return new SparqlTripleSource(e, federationContext);
		}
	}
}
