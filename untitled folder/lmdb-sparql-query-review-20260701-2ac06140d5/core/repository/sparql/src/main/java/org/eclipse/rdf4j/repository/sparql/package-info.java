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
/**
 * A {@link org.eclipse.rdf4j.repository.Repository} that serves as a SPARQL endpoint client.
 * <p>
 * A SPARQL endpoint is any web service that implements the <a href="http://www.w3.org/TR/sparql11-protocol/">SPARQL 1.1
 * Protocol</a> - a means of conveying SPARQL queries and updates to SPARQL processors.
 * </p>
 * <p>
 * Since every RDF4J repository running on a RDf4J Server is also a SPARQL endpoint, it is possible to use the
 * SPARQLRepository to access such a repository. However, it is recommended to instead use
 * {@link org.eclipse.rdf4j.repository.http.HTTPRepository}, which has a number of RDF4J-specific optimizations that
 * make client-server communication more scalable, and transaction-safe.
 * </p>
 */
package org.eclipse.rdf4j.repository.sparql;
