/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.resultio.jelly;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.eclipse.rdf4j.query.resultio.BooleanQueryResultFormat;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultFormat;
import org.junit.jupiter.api.Test;

import eu.neverblink.jelly.convert.rdf4j.sparql.JellySparqlFormat;

public class JellySparqlFormatTest {

	@Test
	public void testTupleFormatDefinition() {
		// jelly-rdf4j-sparql also defines JellySparqlFormat.JELLY_SPARQL, which should be identical to
		// TupleQueryResultFormat.JELLY
		TupleQueryResultFormat ours = TupleQueryResultFormat.JELLY;
		TupleQueryResultFormat theirs = JellySparqlFormat.JELLY_SPARQL;
		assertEquals(ours, theirs);
		assertEquals(ours.supportsTripleTerms(), theirs.supportsTripleTerms());
		assertEquals(ours.getFileExtensions(), theirs.getFileExtensions());
		assertEquals(ours.getDefaultFileExtension(), theirs.getDefaultFileExtension());
		assertEquals(ours.getMIMETypes(), theirs.getMIMETypes());
		assertEquals(ours.getDefaultMIMEType(), theirs.getDefaultMIMEType());
		assertEquals(ours.getCharset(), theirs.getCharset());
	}

	@Test
	public void testBooleanFormatDefinition() {
		BooleanQueryResultFormat ours = BooleanQueryResultFormat.JELLY;
		BooleanQueryResultFormat theirs = JellySparqlFormat.JELLY_SPARQL_BOOLEAN;
		assertEquals(ours, theirs);
		assertEquals(ours.getFileExtensions(), theirs.getFileExtensions());
		assertEquals(ours.getDefaultFileExtension(), theirs.getDefaultFileExtension());
		assertEquals(ours.getMIMETypes(), theirs.getMIMETypes());
		assertEquals(ours.getDefaultMIMEType(), theirs.getDefaultMIMEType());
		assertEquals(ours.getCharset(), theirs.getCharset());
	}
}
