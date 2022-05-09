/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.sail.NotifyingSailConnection;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.rdf4j.sail.shacl.wrapper.data.RdfsSubClassOfReasoner;
import org.eclipse.rdf4j.sail.shacl.wrapper.data.VerySimpleRdfsBackwardsChainingConnection;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class RdfsShaclConnectionTest {

	SimpleValueFactory vf = SimpleValueFactory.getInstance();
	IRI sup = vf.createIRI("http://example.com/sup");
	IRI sub = vf.createIRI("http://example.com/sub");
	IRI subSub = vf.createIRI("http://example.com/subSub");
	IRI aSubSub = vf.createIRI("http://example.com/aSubSub");
	IRI aSub = vf.createIRI("http://example.com/aSub");
	IRI aSup = vf.createIRI("http://example.com/aSup");

	@Test
	public void testHasStatement() {

		ShaclSail shaclSail = new ShaclSail(new MemoryStore());
		shaclSail.init();

		fill(shaclSail);

		try (NotifyingSailConnection connection = shaclSail.getConnection()) {
			((ShaclSailConnection) connection).rdfsSubClassOfReasoner = RdfsSubClassOfReasoner
					.createReasoner((ShaclSailConnection) connection);
			VerySimpleRdfsBackwardsChainingConnection connection2 = new VerySimpleRdfsBackwardsChainingConnection(
					connection,
					((ShaclSailConnection) connection).getRdfsSubClassOfReasoner());

			Assertions.assertTrue(connection2.hasStatement(aSubSub, RDF.TYPE, sup, true));
		}
		shaclSail.shutDown();

	}

	@Test
	public void testGetStatement() {

		ShaclSail shaclSail = new ShaclSail(new MemoryStore());
		shaclSail.init();

		fill(shaclSail);

		try (NotifyingSailConnection connection = shaclSail.getConnection()) {
			((ShaclSailConnection) connection).rdfsSubClassOfReasoner = RdfsSubClassOfReasoner
					.createReasoner((ShaclSailConnection) connection);

			VerySimpleRdfsBackwardsChainingConnection connection2 = new VerySimpleRdfsBackwardsChainingConnection(
					connection,
					((ShaclSailConnection) connection).getRdfsSubClassOfReasoner());

			try (Stream<? extends Statement> stream = connection2.getStatements(aSubSub, RDF.TYPE, sup, true)
					.stream()) {
				Set<? extends Statement> collect = stream.collect(Collectors.toSet());
				HashSet<Statement> expected = new HashSet<>(
						Collections.singletonList(vf.createStatement(aSubSub, RDF.TYPE, sup)));
				Assertions.assertEquals(expected, collect);
			}

			try (Stream<? extends Statement> stream = connection2.getStatements(aSubSub, RDF.TYPE, sub, true)
					.stream()) {
				Set<? extends Statement> collect = stream.collect(Collectors.toSet());
				HashSet<Statement> expected = new HashSet<>(
						Collections.singletonList(vf.createStatement(aSubSub, RDF.TYPE, sub)));
				Assertions.assertEquals(expected, collect);
			}

			try (Stream<? extends Statement> stream = connection2.getStatements(aSubSub, RDF.TYPE, subSub, true)
					.stream()) {
				Set<? extends Statement> collect = stream.collect(Collectors.toSet());
				HashSet<Statement> expected = new HashSet<>(
						Collections.singletonList(vf.createStatement(aSubSub, RDF.TYPE, subSub)));
				Assertions.assertEquals(expected, collect);
			}
		}

		shaclSail.shutDown();

	}

	@Test
	public void testGetStatementNoDuplicates() {

		ShaclSail shaclSail = new ShaclSail(new MemoryStore());
		shaclSail.init();

		fill(shaclSail);

		try (NotifyingSailConnection connection = shaclSail.getConnection()) {
			connection.begin();
			connection.addStatement(aSubSub, RDF.TYPE, sup);
			connection.addStatement(aSubSub, RDF.TYPE, sub);
			connection.commit();

			((ShaclSailConnection) connection).rdfsSubClassOfReasoner = RdfsSubClassOfReasoner
					.createReasoner((ShaclSailConnection) connection);

			VerySimpleRdfsBackwardsChainingConnection connection2 = new VerySimpleRdfsBackwardsChainingConnection(
					connection,
					((ShaclSailConnection) connection).getRdfsSubClassOfReasoner());

			try (Stream<? extends Statement> stream = connection2.getStatements(aSubSub, RDF.TYPE, sup, true)
					.stream()) {
				List<Statement> collect = stream.collect(Collectors.toList());
				Assertions.assertEquals(new HashSet<>(collect).size(), collect.size());

			}
		}
		shaclSail.shutDown();

	}

	private void fill(ShaclSail shaclSail) {
		try (NotifyingSailConnection connection = shaclSail.getConnection()) {
			connection.begin();
			connection.addStatement(subSub, RDFS.SUBCLASSOF, sub);
			connection.addStatement(sub, RDFS.SUBCLASSOF, sup);
			connection.addStatement(aSubSub, RDF.TYPE, subSub);
			connection.addStatement(aSub, RDF.TYPE, sub);
			connection.addStatement(aSup, RDF.TYPE, sup);
			connection.commit();
		}
	}

}
