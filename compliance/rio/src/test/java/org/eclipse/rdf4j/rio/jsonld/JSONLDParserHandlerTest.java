/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.rio.jsonld;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.rio.AbstractParserHandlingTest;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.RDFWriter;

/**
 * Unit tests for {@link JSONLDParser} related to handling of datatypes and languages.
 * 
 * @author Peter Ansell
 */
public class JSONLDParserHandlerTest extends AbstractParserHandlingTest {

	@Override
	protected InputStream getUnknownDatatypeStream(Model unknownDatatypeStatements)
		throws Exception
	{
		return writeJSONLD(unknownDatatypeStatements);
	}

	@Override
	protected InputStream getKnownDatatypeStream(Model knownDatatypeStatements)
		throws Exception
	{
		return writeJSONLD(knownDatatypeStatements);
	}

	@Override
	protected InputStream getUnknownLanguageStream(Model unknownLanguageStatements)
		throws Exception
	{
		return writeJSONLD(unknownLanguageStatements);
	}

	@Override
	protected InputStream getKnownLanguageStream(Model knownLanguageStatements)
		throws Exception
	{
		return writeJSONLD(knownLanguageStatements);
	}

	@Override
	protected RDFParser getParser() {
		return new JSONLDParser();
	}

	/**
	 * Helper method to write the given model to JSON-LD and return an InputStream containing the results.
	 * 
	 * @param statements
	 * @return An {@link InputStream} containing the results.
	 * @throws RDFHandlerException
	 */
	private InputStream writeJSONLD(Model statements)
		throws RDFHandlerException
	{
		final StringWriter writer = new StringWriter();

		final RDFWriter jsonldWriter = new JSONLDWriter(writer);
		jsonldWriter.startRDF();
		for (final Namespace prefix : statements.getNamespaces()) {
			jsonldWriter.handleNamespace(prefix.getPrefix(), prefix.getName());
		}
		for (final Statement nextStatement : statements) {
			jsonldWriter.handleStatement(nextStatement);
		}
		jsonldWriter.endRDF();

		// System.out.println(writer.toString());

		return new ByteArrayInputStream(writer.toString().getBytes(StandardCharsets.UTF_8));
	}

}
