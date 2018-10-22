/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.rio.binary;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.Charset;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.rio.AbstractParserHandlingTest;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.RDFWriter;

/**
 * Test for error handling by Binary Parser.
 * 
 * @author Peter Ansell
 */
public class BinaryHandlingTest extends AbstractParserHandlingTest {

	@Override
	protected InputStream getUnknownDatatypeStream(Model unknownDatatypeStatements)
		throws Exception
	{
		return writeBinary(unknownDatatypeStatements);
	}

	@Override
	protected InputStream getKnownDatatypeStream(Model knownDatatypeStatements)
		throws Exception
	{
		return writeBinary(knownDatatypeStatements);
	}

	@Override
	protected InputStream getUnknownLanguageStream(Model unknownLanguageStatements)
		throws Exception
	{
		return writeBinary(unknownLanguageStatements);
	}

	@Override
	protected InputStream getKnownLanguageStream(Model knownLanguageStatements)
		throws Exception
	{
		return writeBinary(knownLanguageStatements);
	}

	@Override
	protected RDFParser getParser() {
		return new BinaryRDFParser();
	}

	/**
	 * Helper method to write the given model to N-Triples and return an InputStream containing the results.
	 * 
	 * @param statements
	 * @return An {@link InputStream} containing the results.
	 * @throws RDFHandlerException
	 */
	private InputStream writeBinary(Model statements)
		throws RDFHandlerException
	{
		ByteArrayOutputStream output = new ByteArrayOutputStream(8096);

		RDFWriter binaryWriter = new BinaryRDFWriter(output);
		binaryWriter.startRDF();
		for (Statement nextStatement : statements) {
			binaryWriter.handleStatement(nextStatement);
		}
		binaryWriter.endRDF();

		return new ByteArrayInputStream(output.toByteArray());
	}

}
