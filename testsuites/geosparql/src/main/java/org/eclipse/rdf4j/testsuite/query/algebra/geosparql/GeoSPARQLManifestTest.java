/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.testsuite.query.algebra.geosparql;

import java.util.Collection;

import org.eclipse.rdf4j.testsuite.query.parser.sparql.manifest.SPARQLQueryComplianceTest;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

public abstract class GeoSPARQLManifestTest extends SPARQLQueryComplianceTest {

	@TestFactory
	public Collection<DynamicTest> tests() {
		return getTestData("testcases-geosparql/manifest-all.ttl", false);
	}
}
