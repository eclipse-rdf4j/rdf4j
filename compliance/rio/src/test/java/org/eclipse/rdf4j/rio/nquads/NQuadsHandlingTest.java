/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.rio.nquads;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.rio.AbstractParserHandlingTest;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.RDFWriter;

/**
 * Test for error handling by N-Quads Parser.
 * 
 * @author Peter Ansell
 */
public class NQuadsHandlingTest extends AbstractParserHandlingTest {

	@Override
	protected InputStream getUnknownDatatypeStream(Model unknownDatatypeStatements)
		throws Exception
	{
		return writeNQuads(unknownDatatypeStatements);
	}

	@Override
	protected InputStream getKnownDatatypeStream(Model knownDatatypeStatements)
		throws Exception
	{
		return writeNQuads(knownDatatypeStatements);
	}

	@Override
	protected InputStream getUnknownLanguageStream(Model unknownLanguageStatements)
		throws Exception
	{
		return writeNQuads(unknownLanguageStatements);
	}

	@Override
	protected InputStream getKnownLanguageStream(Model knownLanguageStatements)
		throws Exception
	{
		return writeNQuads(knownLanguageStatements);
	}

	@Override
	protected RDFParser getParser() {
		return new NQuadsParser();
	}

	/**
	 * Helper method to write the given model to N-Triples and return an InputStream containing the results.
	 * 
	 * @param statements
	 * @return An {@link InputStream} containing the results.
	 * @throws RDFHandlerException
	 */
	private InputStream writeNQuads(Model statements)
		throws RDFHandlerException
	{
		StringWriter writer = new StringWriter();

		RDFWriter nQuadsWriter = new NQuadsWriter(writer);
		nQuadsWriter.startRDF();
		for (Statement nextStatement : statements) {
			nQuadsWriter.handleStatement(nextStatement);
		}
		nQuadsWriter.endRDF();

		return new ByteArrayInputStream(writer.toString().getBytes(StandardCharsets.UTF_8));
	}

}
