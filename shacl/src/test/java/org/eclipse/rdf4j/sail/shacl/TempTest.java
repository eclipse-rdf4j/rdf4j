/*******************************************************************************
 * Copyright (c) 2016 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl;

import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.rdf4j.sail.shacl.planNodes.LoggingNode;
import org.junit.Test;

/**
 * @author Håvard Ottestad
 */
public class TempTest {

	{
		LoggingNode.loggingEnabled = true;
	}

	@Test
	public void a() {

		SailRepository shaclSail = new SailRepository(new ShaclSail(new MemoryStore(), Utils.getSailRepository("shacl.ttl")));
		shaclSail.initialize();

		try (SailRepositoryConnection connection = shaclSail.getConnection()) {

			connection.begin();
			connection.add(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE);
//
			connection.add(RDFS.RESOURCE, RDFS.LABEL, connection.getValueFactory().createLiteral("a"));

			connection.add(RDFS.CLASS, RDF.TYPE, RDFS.RESOURCE);

			connection.add(RDFS.CLASS, RDFS.LABEL, connection.getValueFactory().createLiteral("a"));
			connection.add(RDFS.CLASS, RDFS.LABEL, connection.getValueFactory().createLiteral("yay"));
			connection.add(RDFS.CLASS, RDFS.LABEL, connection.getValueFactory().createLiteral("yay2"));


			connection.add(RDFS.SUBCLASSOF, RDFS.LABEL, connection.getValueFactory().createLiteral("b"));
			connection.add(RDFS.SUBCLASSOF, RDFS.LABEL, connection.getValueFactory().createLiteral("c"));

			connection.commit();

			System.out.println("\n\n\n\n\n\n\n\n\n\n");

			connection.begin();

			connection.remove(RDFS.RESOURCE, RDFS.LABEL, connection.getValueFactory().createLiteral("a"));
			connection.add(RDFS.RESOURCE, RDFS.LABEL, connection.getValueFactory().createLiteral("b"));
			connection.add(RDFS.RESOURCE, RDFS.LABEL, connection.getValueFactory().createLiteral("c"));
			connection.remove(RDFS.CLASS, RDFS.LABEL, connection.getValueFactory().createLiteral("a"));
			connection.add(RDFS.SUBCLASSOF, RDF.TYPE, RDFS.RESOURCE);


			connection.commit();

		}


	}

	@Test
	public void b() {

		SailRepository shaclSail = new SailRepository(new ShaclSail(new MemoryStore(), Utils.getSailRepository("shacl.ttl")));
		shaclSail.initialize();

		try (SailRepositoryConnection connection = shaclSail.getConnection()) {

			connection.begin();
//
			connection.add(RDFS.RESOURCE, RDFS.LABEL, connection.getValueFactory().createLiteral("a"));

//			connection.add(RDFS.CLASS, RDFS.LABEL, connection.getValueFactory().createLiteral("a"));
//			connection.add(RDFS.CLASS, RDFS.LABEL, connection.getValueFactory().createLiteral("yay"));
//
			connection.commit();

			System.out.println("\n\n\n\n\n\n\n\n\n\n");

			connection.begin();

			connection.remove(RDFS.RESOURCE, RDFS.LABEL, connection.getValueFactory().createLiteral("a"));
			connection.add(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE);
			connection.add(RDFS.RESOURCE, RDFS.LABEL, connection.getValueFactory().createLiteral("b"));


			connection.commit();

		}


	}


	@Test(expected = RepositoryException.class)
	public void maxCount() {

		SailRepository shaclSail = new SailRepository(new ShaclSail(new MemoryStore(), Utils.getSailRepository("shaclMax.ttl")));
		shaclSail.initialize();

		try (SailRepositoryConnection connection = shaclSail.getConnection()) {

			connection.begin();
//			connection.add(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE);

			connection.add(RDFS.CLASS, RDFS.LABEL, connection.getValueFactory().createLiteral("class1"));
			connection.add(RDFS.CLASS, RDFS.LABEL, connection.getValueFactory().createLiteral("class2"));
			connection.add(RDFS.CLASS, RDFS.LABEL, connection.getValueFactory().createLiteral("class3"));

			connection.commit();

			System.out.println("\n\n\n\n\n\n\n\n\n\n");

			connection.begin();

			connection.add(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE);


			connection.add(RDFS.RESOURCE, RDFS.LABEL, connection.getValueFactory().createLiteral("a"));
			connection.add(RDFS.RESOURCE, RDFS.LABEL, connection.getValueFactory().createLiteral("b"));
			connection.add(RDFS.RESOURCE, RDFS.LABEL, connection.getValueFactory().createLiteral("c"));
			connection.add(RDFS.RESOURCE, RDFS.LABEL, connection.getValueFactory().createLiteral("d"));

			connection.add(RDFS.CLASS, RDF.TYPE, RDFS.RESOURCE);


			connection.commit();

		}


	}


	@Test
	public void minCount() {

		SailRepository shaclSail = new SailRepository(new ShaclSail(new MemoryStore(), Utils.getSailRepository("shacl.ttl")));
		shaclSail.initialize();

		try (SailRepositoryConnection connection = shaclSail.getConnection()) {

			connection.begin();
//			connection.add(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE);
			connection.add(RDFS.RESOURCE, RDFS.LABEL, connection.getValueFactory().createLiteral("a"));
			connection.add(RDFS.RESOURCE, RDFS.LABEL, connection.getValueFactory().createLiteral("b"));
			connection.add(RDFS.RESOURCE, RDFS.LABEL, connection.getValueFactory().createLiteral("c"));

			connection.commit();

			System.out.println("\n\n\n\n\n\n\n\n\n\n");

			connection.begin();

			connection.add(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE);


			connection.commit();

		}


	}


	@Test
	public void leftOuterJoin() {

		SailRepository shaclSail = new SailRepository(new ShaclSail(new MemoryStore(), Utils.getSailRepository("shacl.ttl")));
		shaclSail.initialize();

		try (SailRepositoryConnection connection = shaclSail.getConnection()) {

			connection.begin();
			connection.add(RDFS.RESOURCE, RDF.TYPE, RDFS.CLASS);

			connection.commit();

			connection.begin();

			connection.remove(RDFS.RESOURCE, RDF.TYPE, RDFS.CLASS);

			connection.add(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE);

			connection.add(RDFS.RESOURCE, RDFS.LABEL, connection.getValueFactory().createLiteral("a"));
			connection.add(RDFS.RESOURCE, RDFS.LABEL, connection.getValueFactory().createLiteral("b"));

			connection.add(RDFS.CLASS, RDF.TYPE, RDFS.RESOURCE);

			connection.add(RDFS.CLASS, RDFS.LABEL, connection.getValueFactory().createLiteral("a"));
			connection.add(RDFS.CLASS, RDFS.LABEL, connection.getValueFactory().createLiteral("yay"));
			connection.add(RDFS.CLASS, RDFS.LABEL, connection.getValueFactory().createLiteral("yay2"));


			connection.add(RDFS.SUBCLASSOF, RDFS.LABEL, connection.getValueFactory().createLiteral("b"));
			connection.add(RDFS.SUBCLASSOF, RDFS.LABEL, connection.getValueFactory().createLiteral("c"));

			connection.commit();


		}


	}

	@Test(expected = RepositoryException.class)
	public void testShapeWithoutTargetClassRemove() {

		SailRepository shaclSail = new SailRepository(new ShaclSail(new MemoryStore(), Utils.getSailRepository("shacleNoTargetClass.ttl")));
		shaclSail.initialize();

		try (SailRepositoryConnection connection = shaclSail.getConnection()) {

			connection.begin();
			connection.add(RDFS.CLASS, RDFS.LABEL, connection.getValueFactory().createLiteral("class1"));
			connection.add(RDFS.CLASS, RDF.TYPE, RDFS.RESOURCE);
			connection.commit();

			connection.begin();
			connection.remove(RDFS.CLASS, RDFS.LABEL, connection.getValueFactory().createLiteral("class1"));
			connection.commit();

		}


	}


	@Test(expected = RepositoryException.class)
	public void testShapeWithoutTargetClassAdd() {

		SailRepository shaclSail = new SailRepository(new ShaclSail(new MemoryStore(), Utils.getSailRepository("shacleNoTargetClass.ttl")));
		shaclSail.initialize();

		try (SailRepositoryConnection connection = shaclSail.getConnection()) {

			connection.begin();
			connection.add(RDFS.CLASS, RDFS.LABEL, connection.getValueFactory().createLiteral("class1"));
			connection.add(RDFS.CLASS, RDF.TYPE, RDFS.RESOURCE);
			connection.commit();

			connection.begin();
			connection.add(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE);
			connection.commit();

		}


	}

	@Test
	public void testShapeWithoutTargetClassValid() {

		SailRepository shaclSail = new SailRepository(new ShaclSail(new MemoryStore(), Utils.getSailRepository("shacleNoTargetClass.ttl")));
		shaclSail.initialize();

		try (SailRepositoryConnection connection = shaclSail.getConnection()) {

			connection.begin();
			connection.commit();

			connection.begin();
			connection.add(RDFS.CLASS, RDFS.LABEL, connection.getValueFactory().createLiteral("class1"));
			connection.add(RDFS.CLASS, RDF.TYPE, RDFS.RESOURCE);
			connection.commit();

			connection.begin();
			connection.remove(RDFS.CLASS, RDF.TYPE, RDFS.RESOURCE);
			connection.commit();

			connection.begin();
			connection.add(RDFS.RESOURCE, RDFS.LABEL, connection.getValueFactory().createLiteral("class1"));
			connection.commit();


		}


	}

}
