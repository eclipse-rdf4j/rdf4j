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
package org.eclipse.rdf4j.federated.endpoint.provider;

import org.eclipse.rdf4j.federated.endpoint.Endpoint;
import org.eclipse.rdf4j.federated.exception.FedXException;

/**
 * Generic interface to create {@link Endpoint}s from a repository information.
 *
 * @author Andreas Schwarte
 *
 */
public interface EndpointProvider<T extends RepositoryInformation> {

	Endpoint loadEndpoint(T repoInfo) throws FedXException;

}
