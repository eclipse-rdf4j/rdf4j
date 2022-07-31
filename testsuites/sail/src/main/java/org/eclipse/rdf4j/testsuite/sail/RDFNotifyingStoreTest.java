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

import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.sail.NotifyingSail;
import org.eclipse.rdf4j.sail.SailChangedEvent;
import org.eclipse.rdf4j.sail.SailChangedListener;
import org.eclipse.rdf4j.sail.SailException;
import org.junit.Assert;
import org.junit.Test;

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

	@Override
	public void setUp() throws Exception {
		super.setUp();

		// set self as listener
		((NotifyingSail) sail).addSailChangedListener(this);

	}

	@Test
	public void testNotifyingRemoveAndClear() throws Exception {
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

		Assert.assertEquals("Repository should contain 4 statements in total", 4, countAllElements());

		Assert.assertEquals("Named context should contain 3 statements", 3, countContext1Elements());

		assertThat(con.hasStatement(painting, RDF.TYPE, RDFS.CLASS, true)).isFalse();

		con.begin();
		con.removeStatements(null, null, null, context1);
		con.commit();

		Assert.assertEquals("Repository should contain 1 statement in total", 1, countAllElements());

		Assert.assertEquals("Named context should be empty", 0, countContext1Elements());

		con.begin();
		con.clear();
		con.commit();

		Assert.assertEquals("Repository should no longer contain any statements", 0, countAllElements());

		// test if event listener works properly.
		Assert.assertEquals("There should have been 1 event in which statements were added", 1, addEventCount);

		Assert.assertEquals("There should have been 3 events in which statements were removed", 3, removeEventCount);
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
