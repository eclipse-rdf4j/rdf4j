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
package org.eclipse.rdf4j.sail.lmdb;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.sail.NotifyingSail;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.lmdb.config.LmdbStoreConfig;
import org.eclipse.rdf4j.testsuite.sail.RDFNotifyingStoreTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * An extension of RDFStoreTest for testing the class {@link LmdbStore}.
 */
public class LmdbStoreTest extends RDFNotifyingStoreTest {

	/*-----------*
	 * Variables *
	 *-----------*/

	@TempDir
	public File dataDir;

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	protected NotifyingSail createSail() throws SailException {
		NotifyingSail sail = new LmdbStore(dataDir, new LmdbStoreConfig("spoc,posc"));
		sail.init();
		return sail;
	}

	@Override
	protected boolean deleteDataDirAfterShutdown() {
		return true;
	}

	// Test for SES-542
	@Test
	public void testGetNamespacePersistence() {
		con.begin();
		con.setNamespace("rdf", RDF.NAMESPACE);
		con.commit();
		assertEquals(RDF.NAMESPACE, con.getNamespace("rdf"));

		con.close();
		sail.shutDown();
		sail.init();
		con = sail.getConnection();

		assertEquals(RDF.NAMESPACE, con.getNamespace("rdf"));
	}

	@Test
	public void testDirectedLanguageLiteralLongLanguageTagAfterRestart() {
		IRI subject = vf.createIRI("http://example.org/subject");
		IRI predicate = vf.createIRI("http://example.org/predicate");
		String longLanguageTag = "he-" + "abcdefg-".repeat(8) + "abc";
		Literal expected = vf.createLiteral("שלום", longLanguageTag, Literal.BaseDirection.RTL);

		con.begin();
		con.addStatement(subject, predicate, expected);
		con.commit();
		con.close();
		sail.shutDown();

		sail.init();
		con = sail.getConnection();

		try (CloseableIteration<? extends Statement> statements = con.getStatements(subject, predicate, null, false)) {
			assertTrue(statements.hasNext(), "expect directed language literal statement to be present");
			Literal actual = (Literal) statements.next().getObject();
			assertEquals(expected, actual);
			assertEquals(longLanguageTag, actual.getLanguage().orElseThrow());
			assertEquals(Literal.BaseDirection.RTL, actual.getBaseDirection());
			assertEquals(RDF.DIRLANGSTRING, actual.getDatatype());
			assertFalse(statements.hasNext(), "expect single statement");
		}
	}
}
