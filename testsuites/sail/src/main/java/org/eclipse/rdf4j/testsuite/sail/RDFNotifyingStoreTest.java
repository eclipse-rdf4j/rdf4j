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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.NotifyingSail;
import org.eclipse.rdf4j.sail.NotifyingSailConnection;
import org.eclipse.rdf4j.sail.SailChangedEvent;
import org.eclipse.rdf4j.sail.SailChangedListener;
import org.eclipse.rdf4j.sail.SailConnectionListener;
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
	private SailRepository repo;

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
		removeEventCount = 0;
		addEventCount = 0;
		this.repo = new SailRepository(sail);
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

	@Test
	public void testUpdateQuery() {

		try (SailRepositoryConnection connection = repo.getConnection()) {
			connection.begin();
			connection.add(painter, RDF.TYPE, RDFS.CLASS);
			connection.add(painting, RDF.TYPE, RDFS.CLASS);
			connection.add(picasso, RDF.TYPE, painter);
			connection.add(guernica, RDF.TYPE, painting);
			connection.add(picasso, paints, guernica);
			connection.commit();

		}

		try (SailRepositoryConnection connection = repo.getConnection()) {
			Set<Statement> added = new HashSet<>();
			Set<Statement> removed = new HashSet<>();

			List<Statement> addedRaw = new ArrayList<>();
			List<Statement> removedRaw = new ArrayList<>();

			registerConnectionListener(connection, added, removed, addedRaw, removedRaw);

			connection.prepareUpdate("" +
					"DELETE {?a ?b ?c}" +
					"INSERT {?a ?b ?c}" +
					"WHERE {?a ?b ?c}").execute();

			assertEquals(5, added.size());
			assertEquals(5, removed.size());
			assertEquals(5, addedRaw.size());
			assertEquals(5, removedRaw.size());

			assertEquals(added, removed);

		}

		assertEquals(5, con.size());

	}

	@Test
	public void testUpdateQuery2() {

		try (SailRepositoryConnection connection = repo.getConnection()) {
			connection.begin();
			connection.add(painter, RDF.TYPE, RDFS.CLASS);
			connection.add(painting, RDF.TYPE, RDFS.CLASS);
			connection.commit();

		}

		try (SailRepositoryConnection connection = repo.getConnection()) {
			Set<Statement> added = new HashSet<>();
			Set<Statement> removed = new HashSet<>();

			List<Statement> addedRaw = new ArrayList<>();
			List<Statement> removedRaw = new ArrayList<>();

			registerConnectionListener(connection, added, removed, addedRaw, removedRaw);

			String statement = "<" + painter + "> <" + RDF.TYPE + "> <" + RDFS.CLASS + "> .";

			connection.prepareUpdate("" +
					"DELETE {" + statement + "}" +
					"INSERT {" + statement + "}" +
					"WHERE {?a ?b ?c}").execute();

			assertEquals(added, removed, "Added (expected) is not the same as removed (actual)");

			assertEquals(2, addedRaw.size());
			assertEquals(2, removedRaw.size());

			assertEquals(1, added.size());
			assertEquals(1, removed.size());

		}

		assertEquals(2, con.size());

	}

	private static void registerConnectionListener(SailRepositoryConnection connection, Set<Statement> added,
			Set<Statement> removed, List<Statement> addedRaw, List<Statement> removedRaw) {
		((NotifyingSailConnection) connection.getSailConnection())
				.addConnectionListener(
						new SailConnectionListener() {
							@Override
							public void statementAdded(Statement st) {
								boolean add = added.add(st);
								if (!add) {
									removed.remove(st);
								}

								addedRaw.add(st);
							}

							@Override
							public void statementRemoved(Statement st) {
								boolean add = removed.add(st);
								if (!add) {
									added.remove(st);
								}

								removedRaw.add(st);
							}
						}
				);
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
