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
package org.eclipse.rdf4j.http.client;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.impl.BackgroundGraphResult;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.helpers.AbstractRDFParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

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
		public void parse(InputStream in, String baseURI) throws IOException, RDFParseException, RDFHandlerException {
			throw new RDFParseException("invalid RDF ");
		}

		@Override
		public void parse(Reader reader, String baseURI) throws IOException, RDFParseException, RDFHandlerException {
			throw new RDFParseException("invalid RDF ");
		}

	}

	@Test
	@Timeout(value = 1, unit = TimeUnit.SECONDS)
	public void testBGRHang() {
		String data = "@not-rdf";
		Exception exception = assertThrows(QueryEvaluationException.class, () -> {
			BackgroundGraphResult gRes = new BackgroundGraphResult(new DummyParser(),
					new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8,
					"http://example.org", null);
			gRes.run();
			gRes.getNamespaces();
			gRes.hasNext();
		});
	}

}
