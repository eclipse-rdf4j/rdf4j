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
package org.eclipse.rdf4j.testsuite.query.parser.sparql.manifest;

import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.parser.ParsedOperation;

/**
 * A test suite that runs the W3C Approved SPARQL 1.1 Syntax tests.
 *
 * @author Jeen Broekstra
 * @see <a href="https://www.w3.org/2009/sparql/docs/tests/">sparql docs tests</a>
 */

public abstract class SPARQL11SyntaxComplianceTest extends SPARQLSyntaxComplianceTest {

	@Override
	protected abstract ParsedOperation parseOperation(String operation, String fileURL) throws MalformedQueryException;
}
