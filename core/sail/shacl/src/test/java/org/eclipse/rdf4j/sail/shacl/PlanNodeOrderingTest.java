/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl;

import org.eclipse.rdf4j.IsolationLevels;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.rdf4j.sail.shacl.mock.MockConsumePlanNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.Select;
import org.eclipse.rdf4j.sail.shacl.planNodes.Tuple;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static junit.framework.TestCase.assertEquals;

/**
 * @author Håvard Ottestad
 */
public class PlanNodeOrderingTest {

	@Test
	public void testSelect() {
		MemoryStore repository = new MemoryStore();
		repository.init();

		try (SailConnection connection = repository.getConnection()) {
			connection.begin(IsolationLevels.NONE);
			connection.addStatement(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE);
			connection.addStatement(RDFS.CLASS, RDF.TYPE, RDFS.RESOURCE);
			connection.addStatement(RDFS.SUBCLASSOF, RDF.TYPE, RDFS.RESOURCE);
			connection.commit();

			Select select = new Select(connection, "?a <" + RDF.TYPE + "> []", "*");
			List<Tuple> tuples = new MockConsumePlanNode(select).asList();

			String actual = Arrays.toString(tuples.toArray());

			Collections.sort(tuples);

			String expected = Arrays.toString(tuples.toArray());

			assertEquals(expected, actual);
		}
	}
}
