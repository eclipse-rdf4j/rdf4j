/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.federated.write;

import java.util.List;

import org.eclipse.rdf4j.federated.FederationContext;
import org.eclipse.rdf4j.federated.endpoint.Endpoint;

/**
 * Factory to create {@link WriteStrategy} instantiations.
 *
 * <p>
 * Implementations must have a default constructor.
 * </p>
 *
 * @author Andreas Schwarte
 *
 */
public interface WriteStrategyFactory {

	/**
	 * Create the {@link WriteStrategy} using the provided context
	 *
	 * @param members           the current federation members
	 * @param federationContext the federation context
	 * @return the {@link WriteStrategy}
	 */
	WriteStrategy create(List<Endpoint> members, FederationContext federationContext);
}
