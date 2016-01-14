/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.query.resultio.sparqlxml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.InputStream;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.rdf4j.query.AbstractTupleQueryResultHandler;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQueryResultHandlerException;
import org.eclipse.rdf4j.query.resultio.sparqlxml.SPARQLResultsXMLParser;
import org.junit.Test;

/**
 * @author James Leigh
 */
public class SPARQLResultsXMLParserTest {

	@Test
	public void testlocalname()
		throws Exception
	{
		assertEquals(4, countSolutions("localname-result.srx"));
	}

	@Test
	public void testNamespace()
		throws Exception
	{
		assertEquals(4, countSolutions("namespace-result.srx"));
	}

	private int countSolutions(String name)
		throws Exception
	{
		SPARQLResultsXMLParser parser = new SPARQLResultsXMLParser();
		CountingTupleQueryResultHandler countingHandler = new CountingTupleQueryResultHandler();
		parser.setTupleQueryResultHandler(countingHandler);
		InputStream in = SPARQLResultsXMLParserTest.class.getClassLoader().getResourceAsStream(name);
		assertNotNull(name + " is missing", in);
		try {
			parser.parseQueryResult(in);
		} finally {
			in.close();
		}
		return countingHandler.getCount();
	}

	static class CountingTupleQueryResultHandler extends AbstractTupleQueryResultHandler
	{
		private int count;

		public int getCount()
		{
			return count;
		}

		public void handleSolution(BindingSet bindingSet)
			throws TupleQueryResultHandlerException
		{
			count++;
		}
	}
}
