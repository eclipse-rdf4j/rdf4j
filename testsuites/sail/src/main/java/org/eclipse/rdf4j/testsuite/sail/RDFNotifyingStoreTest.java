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
package org.eclipse.rdf4j.testsuite.sail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.sail.NotifyingSail;
import org.eclipse.rdf4j.sail.SailChangedEvent;
import org.eclipse.rdf4j.sail.SailChangedListener;
import org.eclipse.rdf4j.sail.SailException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * A JUnit test for testing Sail implementations that store RDF data. This is purely a test for data storage and
 * retrieval which assumes that no inferencing or whatsoever is performed. This is an abstract class that should be
 * extended for specific Sail implementations.
 */
public abstract class RDFNotifyingStoreTest extends RDFStoreTest implements SailChangedListener {

	/*-----------*
	 * Variables *
	 *-----------*/

	private int removeEventCount;

	private int addEventCount;

	/*---------*
	 * Methods *
	 *---------*/

	/**
	 * Gets an instance of the Sail that should be tested. The returned repository should already have been initialized.
	 *
	 * @return an initialized Sail.
	 * @throws SailException If the initialization of the repository failed.
	 */
	@Override
	protected abstract NotifyingSail createSail() throws SailException;

	@BeforeEach
	public void addSailChangedListener() {
		// set self as listener
		((NotifyingSail) sail).addSailChangedListener(this);

	}

	@Test
	public void testNotifyingRemoveAndClear() {
		// Add some data to the repository
		con.begin();
		con.addStatement(painter, RDF.TYPE, RDFS.CLASS);
		con.addStatement(painting, RDF.TYPE, RDFS.CLASS);
		con.addStatement(picasso, RDF.TYPE, painter, context1);
		con.addStatement(guernica, RDF.TYPE, painting, context1);
		con.addStatement(picasso, paints, guernica, context1);
		con.commit();

		// Test removal of statements
		con.begin();
		con.removeStatements(painting, RDF.TYPE, RDFS.CLASS);
		con.commit();

		assertEquals(4, countAllElements(), "Repository should contain 4 statements in total");

		assertEquals(3, countContext1Elements(), "Named context should contain 3 statements");

		assertThat(con.hasStatement(painting, RDF.TYPE, RDFS.CLASS, true)).isFalse();

		con.begin();
		con.removeStatements(null, null, null, context1);
		con.commit();

		assertEquals(1, countAllElements(), "Repository should contain 1 statement in total");

		assertEquals(0, countContext1Elements(), "Named context should be empty");

		con.begin();
		con.clear();
		con.commit();

		assertEquals(0, countAllElements(), "Repository should no longer contain any statements");

		// test if event listener works properly.
		assertEquals(1, addEventCount, "There should have been 1 event in which statements were added");

		assertEquals(3, removeEventCount, "There should have been 3 events in which statements were removed");
	}

	@Override
	public void sailChanged(SailChangedEvent event) {
		if (event.statementsAdded()) {
			addEventCount++;
		}
		if (event.statementsRemoved()) {
			removeEventCount++;
		}
	}
}
