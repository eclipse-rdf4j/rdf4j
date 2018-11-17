/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.rio.rdfxml;

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
 * Test for error handling by RDFXML Parser.
 * 
 * @author Peter Ansell
 */
public class RDFXMLHandlingTest extends AbstractParserHandlingTest {

	@Override
	protected InputStream getUnknownDatatypeStream(Model unknownDatatypeStatements)
		throws Exception
	{
		return writeRDFXML(unknownDatatypeStatements);
	}

	@Override
	protected InputStream getKnownDatatypeStream(Model knownDatatypeStatements)
		throws Exception
	{
		return writeRDFXML(knownDatatypeStatements);
	}

	@Override
	protected InputStream getUnknownLanguageStream(Model unknownLanguageStatements)
		throws Exception
	{
		return writeRDFXML(unknownLanguageStatements);
	}

	@Override
	protected InputStream getKnownLanguageStream(Model knownLanguageStatements)
		throws Exception
	{
		return writeRDFXML(knownLanguageStatements);
	}

	@Override
	protected RDFParser getParser() {
		return new RDFXMLParser();
	}

	/**
	 * Helper method to write the given model to RDFXML and return an InputStream containing the results.
	 * 
	 * @param statements
	 * @return An {@link InputStream} containing the results.
	 * @throws RDFHandlerException
	 */
	private InputStream writeRDFXML(Model statements)
		throws RDFHandlerException
	{
		StringWriter writer = new StringWriter();

		RDFWriter rdfxmlWriter = new RDFXMLWriter(writer);
		rdfxmlWriter.startRDF();
		for (Statement nextStatement : statements) {
			rdfxmlWriter.handleStatement(nextStatement);
		}
		rdfxmlWriter.endRDF();

		return new ByteArrayInputStream(writer.toString().getBytes(StandardCharsets.UTF_8));
	}

}
