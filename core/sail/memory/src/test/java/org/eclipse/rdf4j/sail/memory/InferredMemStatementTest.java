/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Distribution License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/org/documents/edl-v10.php.
 ******************************************************************************/

package org.eclipse.rdf4j.sail.memory;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import java.util.stream.Stream;

import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.sail.memory.model.MemStatement;
import org.junit.Test;

/**
 * This test class should be removed when we remove
 * {@link org.eclipse.rdf4j.sail.memory.model.MemStatement#setExplicit(boolean)}
 */
@Deprecated(since = "4.0.0", forRemoval = true)
public class InferredMemStatementTest {

	@Test
	public void testChangingInferredStatement() {
		MemoryStore memoryStore = new MemoryStore();

		// add our explicit statement
		try (MemoryStoreConnection connection = (MemoryStoreConnection) memoryStore.getConnection()) {
			connection.begin();
			connection.addStatement(RDF.TYPE, RDF.TYPE, RDF.TYPE);
			connection.commit();
		}

		// make the statement inferred
		try (MemoryStoreConnection connection = (MemoryStoreConnection) memoryStore.getConnection()) {
			connection.begin();
			try (Stream<? extends Statement> stream = connection.getStatements(RDF.TYPE, RDF.TYPE, RDF.TYPE, true)
					.stream()) {
				MemStatement statement = stream
						.map(s -> (MemStatement) s)
						.findAny()
						.get();

				assertThat(statement.isExplicit()).isTrue();
				statement.setExplicit(false);
				assertThat(statement.isExplicit()).isFalse();

				connection.commit();
			}

		}

		// we need to add an inferred statement so that the MemorySailStore doesn't assume that the inferred branch is
		// empty
		try (MemoryStoreConnection connection = (MemoryStoreConnection) memoryStore.getConnection()) {
			connection.begin();
			connection.addInferredStatement(RDF.SUBJECT, RDF.PREDICATE, RDF.OBJECT);
			connection.commit();
		}

		// check the statement is still set as inferred
		try (MemoryStoreConnection connection = (MemoryStoreConnection) memoryStore.getConnection()) {
			connection.begin();

			Optional<MemStatement> statement = connection.getStatements(RDF.TYPE, RDF.TYPE, RDF.TYPE, true)
					.stream()
					.map(s -> (MemStatement) s)
					.findAny();

			assertThat(statement).isPresent();
			assertThat(statement.get().isExplicit()).isFalse();

			connection.commit();
		}

	}

}
