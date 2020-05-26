/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.federated.endpoint;

import org.eclipse.rdf4j.federated.evaluation.SparqlTripleSource;

/**
 * Additional marker interface for Endpoint Configurations.
 *
 * An {@link EndpointConfiguration} may bring additional configuration settings for an {@link Endpoint}, e.g. in the
 * case of a {@link SparqlTripleSource} it may decide whether ASK or SELECT queries shall be used for source selection.
 *
 * @author Andreas Schwarte
 *
 */
public interface EndpointConfiguration {

}
