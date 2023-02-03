/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.rio.ndjsonld;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collection;

import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.junit.jupiter.api.Test;

public class NDJSONLDParserTest {

	private final ValueFactory vf = SimpleValueFactory.getInstance();

	@Test
	public void testNDJSONLDWriter() throws IOException {
		NDJSONLDParser ndjsonldParser = new NDJSONLDParser();
		ndjsonldParser.getParserConfig().set(BasicWriterSettings.BASE_DIRECTIVE, true);
		StatementCollector statementCollector = new StatementCollector();
		ndjsonldParser.setRDFHandler(statementCollector);
		ndjsonldParser.parse(new FileInputStream("src/test/resources/testcases/ndjsonld/mates.ndjsonld"));
		Collection<Statement> statements = statementCollector.getStatements();
		assertTrue(statements.contains(vf.createStatement(vf.createIRI("http://ndjsonld.com/Mate1"), RDF.TYPE,
				vf.createIRI("http://schema.org/Person"))));
		assertTrue(statements.contains(vf.createStatement(vf.createIRI("http://ndjsonld.com/Mate2"), RDF.TYPE,
				vf.createIRI("http://schema.org/Person"))));
		assertTrue(statements.contains(vf.createStatement(vf.createIRI("http://ndjsonld.com/Mate1"),
				vf.createIRI("http://schema.org/givenName"), vf.createLiteral("Mate1"))));
		assertTrue(statements.contains(vf.createStatement(vf.createIRI("http://ndjsonld.com/Mate2"),
				vf.createIRI("http://schema.org/givenName"), vf.createLiteral("Mate2"))));
	}

	@Test
	public void testNDJSONLDParser() throws IOException {
		NDJSONLDParser ndjsonldParser = new NDJSONLDParser();
		ndjsonldParser.getParserConfig().set(BasicWriterSettings.BASE_DIRECTIVE, true);
		StatementCollector statementCollector = new StatementCollector();
		ndjsonldParser.setRDFHandler(statementCollector);
		ndjsonldParser.parse(new FileInputStream("src/test/resources/testcases/ndjsonld/mates.ndjsonld"));
		Collection<Statement> statements = statementCollector.getStatements();
		assertTrue(statements.contains(vf.createStatement(vf.createIRI("http://ndjsonld.com/Mate1"), RDF.TYPE,
				vf.createIRI("http://schema.org/Person"))));
		assertTrue(statements.contains(vf.createStatement(vf.createIRI("http://ndjsonld.com/Mate2"), RDF.TYPE,
				vf.createIRI("http://schema.org/Person"))));
		assertTrue(statements.contains(vf.createStatement(vf.createIRI("http://ndjsonld.com/Mate1"),
				vf.createIRI("http://schema.org/givenName"), vf.createLiteral("Mate1"))));
		assertTrue(statements.contains(vf.createStatement(vf.createIRI("http://ndjsonld.com/Mate2"),
				vf.createIRI("http://schema.org/givenName"), vf.createLiteral("Mate2"))));
	}

}
