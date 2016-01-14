/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.http.client;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.Charset;

import org.eclipse.rdf4j.http.client.BackgroundGraphResult;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.helpers.AbstractRDFParser;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * @author Damyan Ognyanov
 */
public class BackgroundGraphResultHangTest {

	static class DummyParser extends AbstractRDFParser {

		@Override
		public RDFFormat getRDFFormat() {
			return null;
		}

		@Override
		public void parse(InputStream in, String baseURI)
			throws IOException, RDFParseException, RDFHandlerException
		{
			throw new RDFParseException("invalid RDF ");
		}

		@Override
		public void parse(Reader reader, String baseURI)
			throws IOException, RDFParseException, RDFHandlerException
		{
			throw new RDFParseException("invalid RDF ");
		}

	}

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test(timeout = 1000)
	public void testBGRHang()
		throws Exception
	{
		String data = "@not-rdf";

		BackgroundGraphResult gRes = new BackgroundGraphResult(new DummyParser(), new ByteArrayInputStream(
				data.getBytes(Charset.forName("UTF-8"))), Charset.forName("UTF-8"), "http://base.org");

		gRes.run();

		gRes.getNamespaces();

		thrown.expect(QueryEvaluationException.class);
		gRes.hasNext();
	}
}
