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
package org.eclipse.rdf4j.query.resultio.text.tsv;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.TupleQueryResultHandlerException;
import org.eclipse.rdf4j.query.impl.IteratingTupleQueryResult;
import org.eclipse.rdf4j.query.impl.ListBindingSet;
import org.eclipse.rdf4j.query.resultio.QueryResultIO;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultFormat;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Custom tests for the SPARQL TSV writer.
 *
 * @author Peter Ansell
 */
public class SPARQLTSVCustomTest {

	/**
	 * Only Literals with the XML Schema numeric types should be simplified.
	 * <p>
	 * NOTE: This will fail when using RDF-1.1, as the datatype {@link XSD#STRING} is implied and hence is not generally
	 * represented.
	 *
	 * @throws Exception
	 */
	@Ignore("This test does not work with RDF-1.1")
	@Test
	public void testSES2126QuotedLiteralIntegerAsStringExplicitType() throws Exception {
		List<String> bindingNames = List.of("test");
		TupleQueryResult tqr = new IteratingTupleQueryResult(bindingNames,
				List.of(new ListBindingSet(bindingNames,
						SimpleValueFactory.getInstance().createLiteral("1", XSD.STRING))));
		String result = writeTupleResult(tqr);
		assertEquals("?test\n\"1\"^^<http://www.w3.org/2001/XMLSchema#string>\n", result);
	}

	/**
	 * Only Literals with the XML Schema numeric types should be simplified.
	 *
	 * @throws Exception
	 */
	@Test
	public void testSES2126QuotedLiteralIntegerAsStringImplicitType() throws Exception {
		List<String> bindingNames = List.of("test");
		TupleQueryResult tqr = new IteratingTupleQueryResult(bindingNames,
				List.of(new ListBindingSet(bindingNames, SimpleValueFactory.getInstance().createLiteral("1"))));
		String result = writeTupleResult(tqr);
		assertEquals("?test\n\"1\"\n", result);
	}

	private String writeTupleResult(TupleQueryResult tqr)
			throws IOException, TupleQueryResultHandlerException, QueryEvaluationException {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		QueryResultIO.writeTuple(tqr, TupleQueryResultFormat.TSV, output);
		String result = new String(output.toByteArray(), StandardCharsets.UTF_8);
		return result;
	}

}
