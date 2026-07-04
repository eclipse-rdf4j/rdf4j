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
 * A repository that serves as a proxy client for a remote repository on an RDF4J Server.
 *
 * Note that this proxy implements a <b>RDF4J-specific extension</b> of the basic SPARQL protocol, and therefore should
 * not be used to communicate with non-RDF4J SPARQL endpoints. For such endpoints, use
 * {@link org.eclipse.rdf4j.repository.sparql.SPARQLRepository} instead.
 */
package org.eclipse.rdf4j.repository.http;
