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
 * Default implementation of {@link WriteStrategyFactory}.
 *
 * <p>
 * The default implementation uses the {@link RepositoryWriteStrategy} with the first discovered writable
 * {@link Endpoint}. In none is found, the {@link ReadOnlyWriteStrategy} is used.
 * </p>
 *
 * @author Andreas Schwarte
 *
 */
public class DefaultWriteStrategyFactory implements WriteStrategyFactory {

	@Override
	public WriteStrategy create(List<Endpoint> members, FederationContext federationContext) {
		for (Endpoint e : members) {
			if (e.isWritable()) {
				return new RepositoryWriteStrategy(e.getRepository());
			}
		}
		return ReadOnlyWriteStrategy.INSTANCE;
	}
}
