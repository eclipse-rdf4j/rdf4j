/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.testsuite.sparql.vocabulary;

import static org.eclipse.rdf4j.model.util.Values.iri;

import org.eclipse.rdf4j.model.IRI;

/**
 * @author jeen
 *
 */
public class EX {

	public static final String NAMESPACE = "http://example.org/";
	public static final String PREFIX = "ex";

	public static final IRI BOB = iri(NAMESPACE, "bob");
	public static final IRI ALICE = iri(NAMESPACE, "alice");
	public static final IRI MARY = iri(NAMESPACE, "mary");

	public static final IRI A = iri(NAMESPACE, "A");
	public static final IRI B = iri(NAMESPACE, "B");
	public static final IRI C = iri(NAMESPACE, "C");
}
